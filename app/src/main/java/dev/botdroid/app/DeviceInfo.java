package dev.botdroid.app;

import java.util.Locale;

/**
 * Snapshot of the phone the bot runs on. Plain data with formatting only, so
 * the values can be filled by the Android probe or by a test double.
 */
final class DeviceInfo {

    String manufacturer = "";
    String model = "";
    String device = "";
    String androidVersion = "";
    int sdk = 0;
    String abi = "";
    int cores = 0;
    int batteryPercent = -1;
    String batteryState = "unknown";
    String batteryTemperature = "";
    String network = "unknown";
    long ramTotalMb = 0;
    long ramFreeMb = 0;
    long storageFreeMb = 0;
    long storageTotalMb = 0;
    long deviceUptimeSeconds = 0;
    int screenWidth = 0;
    int screenHeight = 0;
    int densityDpi = 0;
    String locale = "";
    String timezone = "";
    long capturedAt = 0;

    String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append("**Phone:** ").append(clean(manufacturer)).append(' ').append(clean(model)).append('\n');
        builder.append("**Android:** ").append(clean(androidVersion))
                .append(sdk > 0 ? " (API " + sdk + ")" : "")
                .append(" · ").append(clean(abi))
                .append(cores > 0 ? " · " + cores + " cores" : "").append('\n');
        if (screenWidth > 0 && screenHeight > 0) {
            builder.append("**Screen:** ").append(screenWidth).append('×').append(screenHeight)
                    .append(" @ ").append(densityDpi).append(" dpi\n");
        }
        builder.append("**Locale:** ").append(clean(locale))
                .append(" · ").append(clean(timezone)).append('\n');
        if (batteryPercent >= 0) {
            builder.append("**Battery:** ").append(batteryPercent).append("% · ").append(clean(batteryState));
            if (!batteryTemperature.isEmpty()) {
                builder.append(" · ").append(batteryTemperature);
            }
            builder.append('\n');
        }
        builder.append("**Network:** ").append(clean(network)).append('\n');
        if (ramTotalMb > 0) {
            builder.append("**RAM:** ").append(megabytes(ramFreeMb)).append(" free of ")
                    .append(megabytes(ramTotalMb)).append('\n');
        }
        if (storageTotalMb > 0) {
            builder.append("**Storage:** ").append(megabytes(storageFreeMb)).append(" free of ")
                    .append(megabytes(storageTotalMb)).append('\n');
        }
        if (deviceUptimeSeconds > 0) {
            builder.append("**Phone uptime:** ").append(duration(deviceUptimeSeconds));
        }
        return builder.toString().trim();
    }

    /** "Pixel 7", or the model alone when the maker is already in the name. */
    String shortName() {
        String maker = clean(manufacturer);
        String name = clean(model);
        if (maker.isEmpty()) {
            return name.isEmpty() ? "this phone" : name;
        }
        if (name.toLowerCase(Locale.US).startsWith(maker.toLowerCase(Locale.US))) {
            return name;
        }
        return maker + " " + name;
    }

    static String duration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        return builder.toString().trim();
    }

    private static String megabytes(long value) {
        if (value >= 1024) {
            return String.format(Locale.US, "%.1f GB", value / 1024.0);
        }
        return value + " MB";
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }
}
