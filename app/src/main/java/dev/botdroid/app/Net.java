package dev.botdroid.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Dependency-free HTTPS client. One request per connection, no redirects, no
 * compression negotiation, so the wire format stays predictable on every
 * Android version (and it supports PATCH, which HttpURLConnection does not).
 */
final class Net {

    static final String USER_AGENT = "BotDroid (https://github.com/itriedcoding/BotDroid, 1.0)";

    private Net() {
    }

    static final class Resp {
        final int code;
        final String body;

        Resp(int code, String body) {
            this.code = code;
            this.body = body;
        }

        boolean ok() {
            return code >= 200 && code < 300;
        }

        @Override
        public String toString() {
            return code + " " + body;
        }
    }

    static SSLSocket connect(String host, int timeoutMs) throws IOException {
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        socket.connect(new InetSocketAddress(host, 443), timeoutMs);
        socket.setSoTimeout(Math.max(timeoutMs, 60000));
        SSLParameters params = socket.getSSLParameters();
        List<javax.net.ssl.SNIServerName> names = new ArrayList<>(1);
        names.add(new SNIHostName(host));
        params.setServerNames(names);
        socket.setSSLParameters(params);
        socket.startHandshake();
        return socket;
    }

    static Resp request(String method, String host, String path, String token, String json) throws IOException {
        SSLSocket socket = connect(host, 15000);
        try {
            byte[] payload = json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
            StringBuilder head = new StringBuilder();
            head.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
            head.append("Host: ").append(host).append("\r\n");
            head.append("User-Agent: ").append(USER_AGENT).append("\r\n");
            if (token != null) {
                head.append("Authorization: Bot ").append(token).append("\r\n");
            }
            head.append("Accept: application/json\r\n");
            head.append("Accept-Encoding: identity\r\n");
            if (payload.length > 0) {
                head.append("Content-Type: application/json\r\n");
            }
            head.append("Content-Length: ").append(payload.length).append("\r\n");
            head.append("Connection: close\r\n\r\n");

            OutputStream out = socket.getOutputStream();
            out.write(head.toString().getBytes(StandardCharsets.UTF_8));
            out.write(payload);
            out.flush();

            InputStream in = socket.getInputStream();
            String responseHead = readHead(in);
            int code = parseStatus(responseHead);
            String lower = responseHead.toLowerCase();
            boolean chunked = lower.contains("transfer-encoding: chunked");
            int length = contentLength(responseHead);

            String body;
            if (chunked) {
                body = new String(readChunked(in), StandardCharsets.UTF_8);
            } else if (length >= 0) {
                body = new String(readExactly(in, length), StandardCharsets.UTF_8);
            } else {
                body = new String(readToEnd(in), StandardCharsets.UTF_8);
            }
            return new Resp(code, body);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // closing a broken socket is not interesting
            }
        }
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        while (buffer.size() < 65536) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("Connection closed before response headers");
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

    private static int parseStatus(String head) {
        int end = head.indexOf("\r\n");
        String first = end < 0 ? head : head.substring(0, end);
        String[] parts = first.split(" ");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static int contentLength(String head) {
        for (String line : head.split("\r\n")) {
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                try {
                    return Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static byte[] readChunked(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            String line = readLine(in);
            if (line == null) {
                break;
            }
            int semicolon = line.indexOf(';');
            if (semicolon >= 0) {
                line = line.substring(0, semicolon);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int size;
            try {
                size = Integer.parseInt(line, 16);
            } catch (NumberFormatException e) {
                break;
            }
            if (size == 0) {
                break;
            }
            out.write(readExactly(in, size));
            readLine(in);
        }
        return out.toByteArray();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b < 0) {
                return buffer.size() == 0 ? null : new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
            }
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buffer.write(b);
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(data, read, length - read);
            if (n < 0) {
                throw new IOException("Connection closed mid-body");
            }
            read += n;
        }
        return data;
    }

    private static byte[] readToEnd(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
