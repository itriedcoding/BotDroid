package dev.botdroid.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.net.ssl.SSLSocket;

/** Minimal RFC 6455 WebSocket client (text frames only, no extensions). */
final class GatewaySocket {

    static final class GatewayException extends IOException {
        final int closeCode;

        GatewayException(String message, int closeCode) {
            super(message);
            this.closeCode = closeCode;
        }
    }

    private static final int OP_CONTINUATION = 0x0;
    private static final int OP_TEXT = 0x1;
    private static final int OP_BINARY = 0x2;
    private static final int OP_CLOSE = 0x8;
    private static final int OP_PING = 0x9;
    private static final int OP_PONG = 0xA;

    private final String host;
    private final String path;
    private final SecureRandom random = new SecureRandom();

    private SSLSocket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean closed;

    private ByteArrayOutputStream fragments = new ByteArrayOutputStream();
    private int fragmentOpcode = -1;

    GatewaySocket(String host, String path) {
        this.host = host;
        this.path = path;
    }

    void connect() throws IOException {
        socket = Net.connect(host, 15000);
        socket.setSoTimeout(75000);
        out = socket.getOutputStream();
        in = socket.getInputStream();

        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        String key = Base64.getEncoder().encodeToString(nonce);

        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + key + "\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "User-Agent: " + Net.USER_AGENT + "\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        String headers = readHeaders();
        String statusLine = headers.split("\r\n", 2)[0];
        if (!statusLine.contains(" 101 ")) {
            throw new GatewayException("WebSocket handshake failed: " + statusLine, -1);
        }
        String accept = headerValue(headers, "sec-websocket-accept");
        if (accept == null || !accept.equals(expectedAccept(key))) {
            throw new GatewayException("WebSocket handshake accept mismatch", -1);
        }
    }

    private static String expectedAccept(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                    .getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "-";
        }
    }

    private static String headerValue(String headers, String name) {
        for (String line : headers.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(name)) {
                return line.substring(colon + 1).trim();
            }
        }
        return null;
    }

    private String readHeaders() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        while (buffer.size() < 16384) {
            int b = in.read();
            if (b < 0) {
                throw new GatewayException("Connection closed during handshake", -1);
            }
            buffer.write(b);
            if ((matched == 0 && b == '\r') || (matched == 1 && b == '\n')
                    || (matched == 2 && b == '\r') || (matched == 3 && b == '\n')) {
                matched++;
                if (matched == 4) {
                    break;
                }
            } else {
                matched = (b == '\r') ? 1 : 0;
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    String readMessage() throws IOException {
        while (true) {
            int b0 = readByte();
            int b1 = readByte();
            boolean fin = (b0 & 0x80) != 0;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long length = b1 & 0x7F;

            if (length == 126) {
                length = ((long) readByte() << 8) | readByte();
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | readByte();
                }
            }
            if (length < 0 || length > 8L * 1024 * 1024) {
                throw new GatewayException("Frame too large: " + length, -1);
            }

            byte[] mask = masked ? readFully(4) : null;
            byte[] data = readFully((int) length);
            if (mask != null) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (data[i] ^ mask[i & 3]);
                }
            }

            switch (opcode) {
                case OP_CLOSE: {
                    int code = -1;
                    String reason = "";
                    if (data.length >= 2) {
                        code = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    }
                    if (data.length > 2) {
                        reason = new String(data, 2, data.length - 2, StandardCharsets.UTF_8);
                    }
                    throw new GatewayException("Gateway closed (" + code + ")"
                            + (reason.isEmpty() ? "" : " " + reason), code);
                }
                case OP_PING:
                    sendFrame(OP_PONG, data);
                    continue;
                case OP_PONG:
                    continue;
                case OP_TEXT:
                case OP_BINARY:
                    if (fin) {
                        return new String(data, StandardCharsets.UTF_8);
                    }
                    fragmentOpcode = opcode;
                    fragments.reset();
                    fragments.write(data);
                    continue;
                case OP_CONTINUATION:
                    fragments.write(data);
                    if (fin) {
                        byte[] whole = fragments.toByteArray();
                        fragments.reset();
                        fragmentOpcode = -1;
                        return new String(whole, StandardCharsets.UTF_8);
                    }
                    continue;
                default:
                    continue;
            }
        }
    }

    void send(String text) throws IOException {
        sendFrame(OP_TEXT, text.getBytes(StandardCharsets.UTF_8));
    }

    private synchronized void sendFrame(int opcode, byte[] data) throws IOException {
        if (closed || out == null) {
            throw new GatewayException("Socket is closed", -1);
        }
        ByteArrayOutputStream frame = new ByteArrayOutputStream(data.length + 14);
        frame.write(0x80 | opcode);
        int length = data.length;
        if (length < 126) {
            frame.write(0x80 | length);
        } else if (length <= 0xFFFF) {
            frame.write(0x80 | 126);
            frame.write((length >> 8) & 0xFF);
            frame.write(length & 0xFF);
        } else {
            frame.write(0x80 | 127);
            for (int i = 7; i >= 0; i--) {
                frame.write((int) (((long) length >> (8 * i)) & 0xFF));
            }
        }
        byte[] mask = new byte[4];
        random.nextBytes(mask);
        frame.write(mask, 0, 4);
        byte[] masked = new byte[length];
        for (int i = 0; i < length; i++) {
            masked[i] = (byte) (data[i] ^ mask[i & 3]);
        }
        frame.write(masked, 0, length);
        out.write(frame.toByteArray());
        out.flush();
    }

    private int readByte() throws IOException {
        int b;
        try {
            b = in.read();
        } catch (SocketTimeoutException e) {
            throw new GatewayException("Gateway read timed out", -1);
        }
        if (b < 0) {
            throw new GatewayException("Connection closed", -1);
        }
        return b;
    }

    private byte[] readFully(int length) throws IOException {
        byte[] data = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(data, read, length - read);
            if (n < 0) {
                throw new GatewayException("Connection closed", -1);
            }
            read += n;
        }
        return data;
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        try {
            // Send the close frame before marking the socket closed, otherwise
            // sendFrame refuses and Discord never sees a clean shutdown.
            sendFrame(OP_CLOSE, new byte[]{0x03, (byte) 0xE8});
        } catch (IOException ignored) {
            // best effort
        }
        closed = true;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // best effort
        }
    }
}
