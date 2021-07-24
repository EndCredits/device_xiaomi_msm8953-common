package com.xiaomi.parts;

import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.xiaomi.parts.DeviceSettings;

public class Dimmer implements OnPreferenceChangeListener {

    private Context mContext;

    public Dimmer(Context context) {
        mContext = context;
    }

    public static String getFile() {
        if (FileUtils.fileWritable(DeviceSettings.BACKLIGHT_DIMMER_PATH)) {
            return DeviceSettings.BACKLIGHT_DIMMER_PATH;
        }
        return null;
    }

    public static boolean isSupported() {
        return FileUtils.fileWritable(getFile());
    }

    public static boolean isCurrentlyEnabled(Context context) {
        return FileUtils.getFileValueAsBoolean(getFile(), false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        switch (key) {
            case DeviceSettings.PREF_BACKLIGHT_DIMMER:
                FileUtils.setValue(DeviceSettings.BACKLIGHT_DIMMER_PATH, (boolean) value);
                break;

            default:
                break;
        }
        return true;
    }
}
