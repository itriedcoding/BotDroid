package dev.botdroid.app;

import java.util.ArrayList;
import java.util.List;

class DiscordRest {

    interface Responder {
        Net.Resp respond(String method, String path, String body);
    }

    static class Call {
        final String method;
        final String path;
        final String body;

        Call(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body;
        }
    }

    final List<Call> calls = new ArrayList<>();
    Responder responder;

    DiscordRest() {
    }

    DiscordRest(String token) {
    }

    Net.Resp get(String path) {
        return record("GET", path, null);
    }

    Net.Resp post(String path, String body) {
        return record("POST", path, body);
    }

    Net.Resp put(String path, String body) {
        return record("PUT", path, body);
    }

    Net.Resp patch(String path, String body) {
        return record("PATCH", path, body);
    }

    Net.Resp delete(String path) {
        return record("DELETE", path, null);
    }

    private Net.Resp record(String method, String path, String body) {
        calls.add(new Call(method, path, body));
        if (responder != null) {
            Net.Resp response = responder.respond(method, path, body);
            if (response != null) {
                return response;
            }
        }
        return new Net.Resp(204, "");
    }

    Call lastNonCallback() {
        for (int i = calls.size() - 1; i >= 0; i--) {
            if (!calls.get(i).path.endsWith("/callback")) {
                return calls.get(i);
            }
        }
        return null;
    }
}
