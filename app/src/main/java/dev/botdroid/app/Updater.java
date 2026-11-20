package dev.botdroid.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * Self-update from this repository's GitHub releases. The release is read from
 * the public API, the APK is downloaded over TLS, its signing certificate is
 * compared with the installed app's, and Android's own installer is asked to
 * apply it. Nothing is installed silently and nothing is simulated.
 */
final class Updater {

    static final String RELEASES_URL = "https://github.com/itriedcoding/BotDroid/releases";

    private static final String HOST = "api.github.com";
    private static final String PATH = "/repos/itriedcoding/BotDroid/releases/latest";
    private static final String APK_NAME = "BotDroid.apk";
    private static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Progress {
        void onProgress(String message);
    }

    static final class Release {
        final String tag;
        final int code;
        final String apkUrl;
        final long size;

        Release(String tag, int code, String apkUrl, long size) {
            this.tag = tag;
            this.code = code;
            this.apkUrl = apkUrl;
            this.size = size;
        }

        boolean isNewer() {
            return code > BotState.code();
        }
    }

    /** Last release fetched in this process, kept for the settings screen. */
    static volatile Release latest = null;

    private Updater() {
    }

    /** Reads the newest published release and its APK asset. */
    static Release check() throws Exception {
        Net.Resp resp = Net.request("GET", HOST, PATH, null, null);
        if (resp.code == 404) {
            throw new IOException("no release has been published yet");
        }
        if (resp.code == 403 || resp.code == 429) {
            throw new IOException("GitHub rate limit reached, try again in a few minutes");
        }
        if (!resp.ok()) {
            throw new IOException("GitHub answered HTTP " + resp.code);
        }
        JSONObject json = new JSONObject(resp.body);
        String tag = json.optString("tag_name", "").trim();
        int code = BotState.versionCode(tag);
        if (code <= 0) {
            throw new IOException("release tag \"" + tag + "\" is not a version");
        }
        String url = null;
        long size = 0;
        JSONArray assets = json.optJSONArray("assets");
        for (int i = 0; assets != null && i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null || !APK_NAME.equals(asset.optString("name", ""))) {
                continue;
            }
            url = asset.optString("browser_download_url", "");
            size = asset.optLong("size", 0);
            break;
        }
        if (url == null || url.isEmpty()) {
            throw new IOException("release " + tag + " has no " + APK_NAME);
        }
        Release release = new Release(tag, code, url, size);
        latest = release;
        return release;
    }

    /** Downloads the release APK into the cache and checks who signed it. */
    static File download(Context context, Release release, Progress progress) throws Exception {
        progress.onProgress("Downloading " + release.tag + "…");
        byte[] bytes = Net.get(release.apkUrl);
        if (bytes.length < 4096 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IOException("the download is not an APK");
        }
        if (release.size > 0 && bytes.length != release.size) {
            throw new IOException("downloaded " + bytes.length + " of " + release.size + " bytes");
        }
        File target = new File(context.getCacheDir(), "BotDroid-" + release.tag + ".apk");
        try (OutputStream out = new FileOutputStream(target)) {
            out.write(bytes);
        }
        progress.onProgress("Checking the signature…");
        if (!signatureMatches(context, target)) {
            target.delete();
            throw new IOException("that APK is not signed with this app's key");
        }
        return target;
    }

    /**
     * True when the downloaded APK carries the certificate of the installed
     * app, so Android will accept it as an update and it cannot be swapped for
     * a build signed by someone else.
     */
    static boolean signatureMatches(Context context, File apk) {
        PackageManager pm = context.getPackageManager();
        PackageInfo installed;
        try {
            installed = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
        if (installed == null || candidate == null
                || installed.signatures == null || installed.signatures.length == 0
                || candidate.signatures == null || candidate.signatures.length == 0) {
            return false;
        }
        String expected = fingerprint(installed.signatures[0]);
        for (Signature signature : candidate.signatures) {
            if (expected.equals(fingerprint(signature))) {
                return true;
            }
        }
        return false;
    }

    private static String fingerprint(Signature signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signature.toByteArray());
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Hands the verified APK to Android's package installer. */
    static void install(Context context, File apk, Progress progress) throws Exception {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !pm.canRequestPackageInstalls()) {
            progress.onProgress("Allow BotDroid to install updates, then press again");
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settings);
            throw new IOException("install permission is off in Android settings");
        }

        PackageInstaller installer = pm.getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        int sessionId = installer.createSession(params);
        PackageInstaller.Session session = installer.openSession(sessionId);
        boolean committed = false;
        try {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                session.fsync(out);
            }
            Intent result = new Intent(context, UpdateReceiver.class)
                    .setAction(UpdateReceiver.ACTION_RESULT);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pending = PendingIntent.getBroadcast(context, sessionId, result, flags);
            session.commit(pending.getIntentSender());
            committed = true;
            progress.onProgress("Waiting for Android's installer…");
        } finally {
            if (!committed) {
                session.abandon();
            }
            session.close();
        }
    }

    /** Checks once a day when the app opens, and only speaks up when there is news. */
    static void autoCheck(Context context) {
        if (!Prefs.updateCheckDue(context, CHECK_INTERVAL_MS)) {
            return;
        }
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Release release = check();
                    Prefs.updateChecked(app, release.tag);
                    if (release.isNewer()) {
                        LogBus.log("Update available: " + release.tag + " (Settings → Updates)");
                        final String message = "BotDroid " + release.tag + " is available";
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(app, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    // A failed check is not worth a message: the phone may simply
                    // be offline. Retry on the next launch.
                    Prefs.updateChecked(app, "");
                    LogBus.log("Update check failed: " + e.getMessage());
                }
            }
        }, "botdroid-update").start();
    }
}
