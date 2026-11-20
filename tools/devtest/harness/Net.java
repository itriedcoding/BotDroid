package dev.botdroid.app;

final class Net {

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
    }

    static Resp request(String method, String host, String path, String token, String body)
            throws java.io.IOException {
        throw new java.io.IOException("offline in tests");
    }

    private Net() {
    }
}
