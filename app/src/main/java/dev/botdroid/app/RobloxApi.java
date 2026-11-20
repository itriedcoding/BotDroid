package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Roblox public API client. No key or login is needed for these endpoints.
 * See https://users.roblox.com, https://games.roblox.com, https://groups.roblox.com
 * and https://thumbnails.roblox.com.
 */
final class RobloxApi {

    static final class ApiException extends Exception {
        final int code;

        ApiException(String message, int code) {
            super(message);
            this.code = code;
        }
    }

    private RobloxApi() {
    }

    static JSONObject get(String host, String path) throws ApiException {
        return call("GET", host, path, null);
    }

    static JSONObject post(String host, String path, String body) throws ApiException {
        return call("POST", host, path, body);
    }

    private static JSONObject call(String method, String host, String path, String body) throws ApiException {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Net.Resp response = Net.request(method, host, path, null, body);
                if (response.code == 429 && attempt == 0) {
                    Thread.sleep(1200);
                    continue;
                }
                if (response.code == 404) {
                    throw new ApiException("Roblox could not find that (404).", 404);
                }
                if (!response.ok()) {
                    throw new ApiException("Roblox API returned HTTP " + response.code + ".", response.code);
                }
                return new JSONObject(response.body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException("Request interrupted.", -1);
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == 1) {
                    throw new ApiException(e.getMessage() == null ? "Network error talking to Roblox." : e.getMessage(), -1);
                }
                try {
                    Thread.sleep(400L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new ApiException("Roblox request failed.", -1);
    }

    static long findUserId(String username) throws ApiException {
        try {
            JSONObject payload = new JSONObject();
            payload.put("usernames", new JSONArray().put(username));
            payload.put("excludeBannedUsers", false);
            JSONObject result = post("users.roblox.com", "/v1/usernames/users", payload.toString());
            JSONArray data = result.optJSONArray("data");
            if (data == null || data.length() == 0) {
                return -1L;
            }
            return data.optJSONObject(0).optLong("id", -1L);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Could not search Roblox for that username.", -1);
        }
    }

    static JSONObject user(long userId) throws ApiException {
        return get("users.roblox.com", "/v1/users/" + userId);
    }

    static int friendsCount(long userId) {
        try {
            return get("friends.roblox.com", "/v1/users/" + userId + "/friends/count").optInt("count", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    static int followersCount(long userId) {
        try {
            return get("friends.roblox.com", "/v1/users/" + userId + "/followers/count").optInt("count", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    static String headshot(long userId) {
        return thumbnail("https://thumbnails.roblox.com/v1/users/avatar-headshot?userIds=" + userId
                + "&size=420x420&format=Png&isCircular=false");
    }

    static String avatar(long userId) {
        return thumbnail("https://thumbnails.roblox.com/v1/users/avatar?userIds=" + userId
                + "&size=420x420&format=Png&isCircular=false");
    }

    static JSONObject game(long universeId) throws ApiException {
        JSONObject result = get("games.roblox.com", "/v1/games?universeIds=" + universeId);
        JSONArray data = result.optJSONArray("data");
        if (data == null || data.length() == 0) {
            return null;
        }
        return data.optJSONObject(0);
    }

    static JSONArray userGames(long userId) throws ApiException {
        JSONObject result = get("games.roblox.com", "/v2/users/" + userId
                + "/games?accessFilter=Public&limit=10&sortOrder=Desc");
        JSONArray data = result.optJSONArray("data");
        return data == null ? new JSONArray() : data;
    }

    static String gameIcon(long universeId) {
        return thumbnail("https://thumbnails.roblox.com/v1/games/icons?universeIds=" + universeId
                + "&size=512x512&format=Png&isCircular=false");
    }

    static JSONObject group(long groupId) throws ApiException {
        return get("groups.roblox.com", "/v1/groups/" + groupId);
    }

    static String groupIcon(long groupId) {
        return thumbnail("https://thumbnails.roblox.com/v1/groups/icons?groupIds=" + groupId
                + "&size=420x420&format=Png&isCircular=false");
    }

    private static String thumbnail(String url) {
        try {
            JSONObject result = get("thumbnails.roblox.com", url.substring("https://thumbnails.roblox.com".length()));
            JSONArray data = result.optJSONArray("data");
            if (data == null || data.length() == 0) {
                return null;
            }
            JSONObject first = data.optJSONObject(0);
            if (first == null) {
                return null;
            }
            String image = first.optString("imageUrl", null);
            return image == null || image.isEmpty() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }
}
