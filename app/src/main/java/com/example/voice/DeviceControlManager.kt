package com.example.voice

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.example.data.model.InstalledAppItem
import com.example.data.model.SettingsType
import com.example.data.model.VolumeAction

class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val packageManager = context.packageManager

    private var isTorchOn = false

    /**
     * Lists all installed apps with launch intents
     */
    fun getInstalledApps(): List<InstalledAppItem> {
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(launchIntent, 0)
        return resolveInfoList.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager).toString()
            val isSystem = try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }
            InstalledAppItem(appName = label, packageName = pkg, isSystemApp = isSystem)
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Opens an app by name (fuzzy matching)
     */
    fun openAppByName(nameQuery: String): Result<String> {
        val query = nameQuery.trim().lowercase()
        val installedApps = getInstalledApps()

        // 1. Exact match
        var matched = installedApps.firstOrNull { it.appName.lowercase() == query }

        // 2. Starts with query
        if (matched == null) {
            matched = installedApps.firstOrNull { it.appName.lowercase().startsWith(query) }
        }

        // 3. Contains query
        if (matched == null) {
            matched = installedApps.firstOrNull { it.appName.lowercase().contains(query) }
        }

        // 4. Well-known package fallbacks (e.g. YouTube, WhatsApp, Chrome, Maps, Camera)
        if (matched == null) {
            val knownPackage = when {
                query.contains("youtube") -> "com.google.android.youtube"
                query.contains("whatsapp") -> "com.whatsapp"
                query.contains("chrome") -> "com.android.chrome"
                query.contains("map") -> "com.google.android.apps.maps"
                query.contains("gmail") -> "com.google.android.gm"
                query.contains("play store") -> "com.android.vending"
                query.contains("camera") -> "com.google.android.GoogleCamera"
                query.contains("clock") -> "com.google.android.deskclock"
                query.contains("calculator") -> "com.google.android.calculator"
                else -> null
            }
            if (knownPackage != null) {
                matched = installedApps.firstOrNull { it.packageName == knownPackage }
            }
        }

        if (matched != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Result.success("${matched.appName} खोल रही हूँ।")
            }
        }

        return Result.failure(
            NoSuchElementException(
                "'${nameQuery}' ऐप आपके फोन में नहीं मिला। कृपया चेक करें कि यह इनस्टॉल है या नहीं।"
            )
        )
    }

    /**
     * Closes current app by navigating to the home screen.
     * Explains Android security sandbox restriction.
     */
    fun closeOrExitApp(): Pair<String, String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)

        val spokenResponse = "होम स्क्रीन पर जा रहे हैं।"
        val restrictionNotice = "एंड्रॉयड सुरक्षा प्रतिबंध (सैंडबॉक्सिंग) के कारण कोई ऐप दूसरे ऐप्स को जबरन बंद नहीं कर सकती। हमने होम स्क्रीन खोल दी है।"

        return Pair(spokenResponse, restrictionNotice)
    }

    /**
     * Toggles flashlight torch mode
     */
    fun toggleFlashlight(enable: Boolean): Result<String> {
        if (cameraManager == null) {
            return Result.failure(IllegalStateException("फ्लैशलाइट कैमरा हार्डवेयर उपलब्ध नहीं है।"))
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                return Result.failure(IllegalStateException("फोन में फ्लैशलाइट नहीं मिली।"))
            }

            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            val stateText = if (enable) "टॉर्च चालू कर दी है।" else "टॉर्च बंद कर दी है।"
            Result.success(stateText)
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Flashlight error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Adjusts or sets media/ring volume
     */
    fun adjustVolume(action: VolumeAction, percent: Int? = null): Result<String> {
        if (audioManager == null) {
            return Result.failure(IllegalStateException("ऑडियो मैनेजर उपलब्ध नहीं है।"))
        }

        val stream = AudioManager.STREAM_MUSIC
        val maxVolume = audioManager.getStreamMaxVolume(stream)

        return try {
            when (action) {
                VolumeAction.UP -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val current = audioManager.getStreamVolume(stream)
                    val pct = (current * 100) / maxVolume
                    Result.success("वॉल्यूम बढ़ा दिया है ($pct प्रतिशत)।")
                }
                VolumeAction.DOWN -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val current = audioManager.getStreamVolume(stream)
                    val pct = (current * 100) / maxVolume
                    Result.success("वॉल्यूम कम कर दिया है ($pct प्रतिशत)।")
                }
                VolumeAction.MUTE -> {
                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                    Result.success("आवाज़ म्यूट कर दी गई है।")
                }
                VolumeAction.UNMUTE -> {
                    val half = maxVolume / 2
                    audioManager.setStreamVolume(stream, half, AudioManager.FLAG_SHOW_UI)
                    Result.success("आवाज़ चालू कर दी गई है।")
                }
                VolumeAction.SET -> {
                    val targetPct = (percent ?: 50).coerceIn(0, 100)
                    val targetVolume = (targetPct * maxVolume) / 100
                    audioManager.setStreamVolume(stream, targetVolume, AudioManager.FLAG_SHOW_UI)
                    Result.success("वॉल्यूम $targetPct प्रतिशत पर सेट कर दिया गया है।")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves current battery percentage and charging status
     */
    fun checkBattery(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            100
        }

        val chargingText = if (isCharging) "और फोन चार्ज हो रहा है" else "और फोन चार्ज नहीं हो रहा है"
        return "आपके फोन की बैटरी अभी $batteryPct प्रतिशत है $chargingText।"
    }

    /**
     * Sets an alarm via system AlarmClock intent
     */
    fun setAlarm(hour: Int, minute: Int, label: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val formatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
            Result.success("$formatted बजे का अलार्म लगा दिया गया है।")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Alarm error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sets a countdown timer via system AlarmClock intent
     */
    fun setTimer(durationSeconds: Int, label: String): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val minutes = durationSeconds / 60
            val text = if (minutes > 0) "$minutes मिनट" else "$durationSeconds सेकंड"
            Result.success("$text का टाइमर शुरू कर दिया गया है।")
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Timer error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Opens system settings screens
     */
    fun openSettings(type: SettingsType): Result<String> {
        return try {
            val action = when (type) {
                SettingsType.WIFI -> Settings.ACTION_WIFI_SETTINGS
                SettingsType.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
                SettingsType.SOUND -> Settings.ACTION_SOUND_SETTINGS
                SettingsType.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
                SettingsType.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                SettingsType.GENERAL -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val name = when (type) {
                SettingsType.WIFI -> "वाईफाई"
                SettingsType.BLUETOOTH -> "ब्लूटूथ"
                SettingsType.SOUND -> "आवाज़ और साउंड"
                SettingsType.DISPLAY -> "डिस्प्ले"
                SettingsType.ACCESSIBILITY -> "एक्सेसिबिलिटी"
                SettingsType.GENERAL -> "फोन"
            }
            Result.success("$name सेटिंग्स खोल रही हूँ।")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
