package dev.botdroid.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

/** Reads real phone details for /status; no values are invented or cached as defaults. */
final class DeviceProbe {

    private static final long MEGABYTE = 1024L * 1024L;

    private static volatile Context context;

    private DeviceProbe() {
    }

    static void attach(Context value) {
        if (value != null) {
            context = value.getApplicationContext();
        }
    }

    static DeviceInfo capture() {
        DeviceInfo info = new DeviceInfo();
        info.capturedAt = System.currentTimeMillis();
        info.manufacturer = safe(Build.MANUFACTURER);
        info.model = safe(Build.MODEL);
        info.device = safe(Build.DEVICE);
        info.androidVersion = safe(Build.VERSION.RELEASE);
        info.sdk = Build.VERSION.SDK_INT;
        String[] abis = Build.SUPPORTED_ABIS;
        info.abi = abis != null && abis.length > 0 ? abis[0] : "unknown";
        info.cores = Runtime.getRuntime().availableProcessors();
        info.locale = Locale.getDefault().toLanguageTag();
        info.timezone = TimeZone.getDefault().getID();
        info.deviceUptimeSeconds = SystemClock.elapsedRealtime() / 1000L;

        Context ctx = context;
        if (ctx == null) {
            return info;
        }

        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        info.screenWidth = metrics.widthPixels;
        info.screenHeight = metrics.heightPixels;
        info.densityDpi = metrics.densityDpi;

        readBattery(ctx, info);
        readNetwork(ctx, info);
        readMemory(ctx, info);
        readStorage(ctx, info);
        return info;
    }

    private static void readBattery(Context ctx, DeviceInfo info) {
        try {
            Intent battery = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) {
                return;
            }
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                info.batteryPercent = Math.round(level * 100f / scale);
            }
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            String state;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    state = "charging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    state = "full";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    state = "discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    state = "not charging";
                    break;
                default:
                    state = "unknown";
                    break;
            }
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    state += " (USB)";
                } else if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                    state += " (AC)";
                } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    state += " (wireless)";
                }
            }
            info.batteryState = state;
            int temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            if (temperature != Integer.MIN_VALUE) {
                info.batteryTemperature = String.format(Locale.US, "%.1f °C", temperature / 10.0);
            }
        } catch (Exception ignored) {
            // some devices block the sticky battery broadcast; report what we have
        }
    }

    private static void readNetwork(Context ctx, DeviceInfo info) {
        try {
            ConnectivityManager manager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return;
            }
            Network network = manager.getActiveNetwork();
            NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
            if (capabilities == null) {
                info.network = "offline";
                return;
            }
            boolean online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            String transport;
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                transport = "VPN";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                transport = "Wi-Fi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                transport = "mobile data";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                transport = "Ethernet";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                transport = "Bluetooth";
            } else {
                transport = "connected";
            }
            info.network = online ? transport : transport + " (no internet)";
        } catch (Exception ignored) {
            // keep "unknown"
        }
    }

    private static void readMemory(Context ctx, DeviceInfo info) {
        try {
            ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) {
                return;
            }
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            info.ramTotalMb = memory.totalMem / MEGABYTE;
            info.ramFreeMb = memory.availMem / MEGABYTE;
        } catch (Exception ignored) {
            // ignore, the fields stay at 0 and are not shown
        }
    }

    private static void readStorage(Context ctx, DeviceInfo info) {
        try {
            File directory = ctx.getFilesDir();
            if (directory == null) {
                return;
            }
            info.storageFreeMb = directory.getUsableSpace() / MEGABYTE;
            info.storageTotalMb = directory.getTotalSpace() / MEGABYTE;
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }
}
