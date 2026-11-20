package dev.botdroid.app;

/** Canned device details so /status can be checked on the JVM. */
final class DeviceProbe {

    static DeviceInfo capture() {
        DeviceInfo info = new DeviceInfo();
        info.manufacturer = "Test Corp";
        info.model = "Pixel 7";
        info.device = "panther";
        info.androidVersion = "14";
        info.sdk = 34;
        info.abi = "arm64-v8a";
        info.cores = 8;
        info.batteryPercent = 82;
        info.batteryState = "charging (USB)";
        info.batteryTemperature = "31.0 °C";
        info.network = "Wi-Fi";
        info.ramTotalMb = 7600;
        info.ramFreeMb = 3200;
        info.storageFreeMb = 42000;
        info.storageTotalMb = 118000;
        info.deviceUptimeSeconds = 200000;
        info.screenWidth = 1080;
        info.screenHeight = 2400;
        info.densityDpi = 440;
        info.locale = "en-US";
        info.timezone = "Europe/London";
        info.capturedAt = 1700000000000L;
        return info;
    }

    private DeviceProbe() {
    }
}
