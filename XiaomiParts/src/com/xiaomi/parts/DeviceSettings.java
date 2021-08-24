package com.xiaomi.parts;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SELinux;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import com.xiaomi.parts.fps.FPSInfoService;
import com.xiaomi.parts.kcal.KCalSettingsActivity;
import com.xiaomi.parts.preferences.SecureSettingListPreference;
import com.xiaomi.parts.preferences.SecureSettingSwitchPreference;
import com.xiaomi.parts.preferences.VibrationSeekBarPreference;
import com.xiaomi.parts.preferences.CustomSeekBarPreference;
import com.xiaomi.parts.ambient.AmbientGesturePreferenceActivity;
import com.xiaomi.parts.su.SuShell;
import com.xiaomi.parts.su.SuTask;
import android.util.Log;

import com.xiaomi.parts.R;

public class DeviceSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "DeviceSettings";

    private static final String PREF_DEVICE_KCAL = "device_kcal";
private static final String AMBIENT_DISPLAY = "ambient_display_gestures";
    // vibration override will use bool instead of integer
    public static final String PREF_VIBRATION_OVERRIDE = "vmax_override";
    public static final String PREF_VIBRATION_PATH = "/sys/class/leds/vibrator/vmax_override";

    public static final String PREF_VIBRATION_SYSTEM_STRENGTH = "vibration_system";
    public static final String PREF_VIBRATION_NOTIFICATION_STRENGTH = "vibration_notification";
    public static final String PREF_VIBRATION_CALL_STRENGTH = "vibration_call";
    public static final String VIBRATION_SYSTEM_PATH = "/sys/class/leds/vibrator/vmax_mv_user";
    public static final String VIBRATION_NOTIFICATION_PATH = "/sys/class/leds/vibrator/vmax_mv_strong";
    public static final String VIBRATION_CALL_PATH = "/sys/class/leds/vibrator/vmax_mv_call";

    // value of vtg_min and vtg_max
    public static final int MIN_VIBRATION = 116;
    public static final int MAX_VIBRATION = 3596;

    public static final String PREF_KEY_FPS_INFO = "fps_info";

    private static final String SELINUX_CATEGORY = "selinux";
    private static final String SELINUX_EXPLANATION = "selinux_explanation";
    private static final String PREF_SELINUX_MODE = "selinux_mode";
    private static final String PREF_SELINUX_PERSISTENCE = "selinux_persistence";

    public static final  String PREF_HEADPHONE_GAIN = "headphone_gain";
    public static final  String PREF_MICROPHONE_GAIN = "microphone_gain";
    public static final  String PREF_SPEAKER_GAIN = "speaker_gain";
    public static final  String PREF_EARPIECE_GAIN = "earpiece_gain";
    public static final  String HEADPHONE_GAIN_PATH = "/sys/kernel/sound_control/headphone_gain";
    public static final  String MICROPHONE_GAIN_PATH = "/sys/kernel/sound_control/mic_gain";
    public static final  String SPEAKER_GAIN_PATH = "/sys/kernel/sound_control/speaker_gain";
    public static final  String EARPIECE_GAIN_PATH = "/sys/kernel/sound_control/earpiece_gain";

    public static final  String PREF_BACKLIGHT_DIMMER = "backlight_dimmer";
    public static final  String BACKLIGHT_DIMMER_PATH = "/sys/module/mdss_fb/parameters/backlight_dimmer";

    public static final  String PERF_YELLOW_TORCH_BRIGHTNESS = "yellow_torch_brightness";
    public static final  String TORCH_YELLOW_BRIGHTNESS_PATH = "/sys/class/leds/led:torch_1/max_brightness";

    private static final String PREF_ENABLE_DIRAC = "dirac_enabled";
    private static final String PREF_HEADSET = "dirac_headset_pref";
    private static final String PREF_PRESET = "dirac_preset_pref";

    private static Context mContext;
    private SwitchPreference mSelinuxMode;
    private SwitchPreference mSelinuxPersistence;

    private CustomSeekBarPreference mSpeakerGain;
    private CustomSeekBarPreference mEarpieceGain;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.xiaomi_main, rootKey);
        mContext = this.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        Preference ambientDisplay = findPreference(AMBIENT_DISPLAY);
        ambientDisplay.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getContext(), AmbientGesturePreferenceActivity.class);
            startActivity(intent);
            return true;
        });

        CustomSeekBarPreference torch_yellow = (CustomSeekBarPreference) findPreference(PERF_YELLOW_TORCH_BRIGHTNESS);
        torch_yellow.setEnabled(FileUtils.fileWritable(TORCH_YELLOW_BRIGHTNESS_PATH));
        torch_yellow.setOnPreferenceChangeListener(this);

        SwitchPreference fpsInfo = (SwitchPreference) findPreference(PREF_KEY_FPS_INFO);
        fpsInfo.setChecked(prefs.getBoolean(PREF_KEY_FPS_INFO, false));
        fpsInfo.setOnPreferenceChangeListener(this);

        boolean enhancerEnabled;
        try {
            enhancerEnabled = DiracService.sDiracUtils.isDiracEnabled();
        } catch (java.lang.NullPointerException e) {
            getContext().startService(new Intent(getContext(), DiracService.class));
            try {
                enhancerEnabled = DiracService.sDiracUtils.isDiracEnabled();
            } catch (NullPointerException ne) {
                // Avoid crash
                ne.printStackTrace();
                enhancerEnabled = false;
            }
        }

        SecureSettingSwitchPreference enableDirac = (SecureSettingSwitchPreference) findPreference(PREF_ENABLE_DIRAC);
        enableDirac.setOnPreferenceChangeListener(this);
        enableDirac.setChecked(enhancerEnabled);

        SecureSettingListPreference headsetType = (SecureSettingListPreference) findPreference(PREF_HEADSET);
        headsetType.setOnPreferenceChangeListener(this);

        SecureSettingListPreference preset = (SecureSettingListPreference) findPreference(PREF_PRESET);
        preset.setOnPreferenceChangeListener(this);


        Preference kcal = findPreference(PREF_DEVICE_KCAL);
        kcal.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity().getApplicationContext(), KCalSettingsActivity.class);
            startActivity(intent);
            return true;
        });

        // SELinux
        Preference selinuxCategory = findPreference(SELINUX_CATEGORY);
        Preference selinuxExp = findPreference(SELINUX_EXPLANATION);
        mSelinuxMode = (SwitchPreference) findPreference(PREF_SELINUX_MODE);
        mSelinuxMode.setChecked(SELinux.isSELinuxEnforced());

        mSelinuxPersistence =
            (SwitchPreference) findPreference(PREF_SELINUX_PERSISTENCE);
        mSelinuxPersistence.setChecked(getContext()
            .getSharedPreferences("selinux_pref", Context.MODE_PRIVATE)
            .contains(PREF_SELINUX_MODE));

        // Disabling root required switches if unrooted and letting the user know
        if (!FileUtils.isRooted(getContext())) {
            mSelinuxMode.setEnabled(false);
            mSelinuxPersistence.setEnabled(false);
            mSelinuxPersistence.setChecked(false);
            selinuxExp.setSummary(selinuxExp.getSummary() + "\n" +
                getResources().getString(R.string.selinux_unrooted_summary));
          } else {
            mSelinuxPersistence.setOnPreferenceChangeListener(this);
            mSelinuxMode.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        switch (key) {
            case PERF_YELLOW_TORCH_BRIGHTNESS:
                FileUtils.setValue(TORCH_YELLOW_BRIGHTNESS_PATH, (int) value);
                break;

            case PREF_VIBRATION_SYSTEM_STRENGTH:
                double VibrationSystemValue = (int) value / 100.0 * (MAX_VIBRATION - MIN_VIBRATION) + MIN_VIBRATION;
                FileUtils.setValue(VIBRATION_SYSTEM_PATH, VibrationSystemValue);
                break;

            case PREF_VIBRATION_NOTIFICATION_STRENGTH:
                double VibrationNotificationValue = (int) value / 100.0 * (MAX_VIBRATION - MIN_VIBRATION) + MIN_VIBRATION;
                FileUtils.setValue(VIBRATION_NOTIFICATION_PATH, VibrationNotificationValue);
                break;

            case PREF_VIBRATION_CALL_STRENGTH:
                double VibrationCallValue = (int) value / 100.0 * (MAX_VIBRATION - MIN_VIBRATION) + MIN_VIBRATION;
                FileUtils.setValue(VIBRATION_CALL_PATH, VibrationCallValue);
                break;

            case PREF_HEADPHONE_GAIN:
                FileUtils.setValue(HEADPHONE_GAIN_PATH, value + " " + value);
                break;

            case PREF_MICROPHONE_GAIN:
                FileUtils.setValue(MICROPHONE_GAIN_PATH, (int) value);
                break;

            case PREF_SPEAKER_GAIN:
                 FileUtils.setValue(SPEAKER_GAIN_PATH, (int) value);
                break;

            case PREF_EARPIECE_GAIN:
                FileUtils.setValue(EARPIECE_GAIN_PATH, (int) value);
            case PREF_ENABLE_DIRAC:
                try {
                    DiracService.sDiracUtils.setEnabled((boolean) value);
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setEnabled((boolean) value);
                }
                break;

            case PREF_HEADSET:
                try {
                    DiracService.sDiracUtils.setHeadsetType(Integer.parseInt(value.toString()));
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setHeadsetType(Integer.parseInt(value.toString()));
                }
                break;

            case PREF_PRESET:
                try {
                    DiracService.sDiracUtils.setLevel(String.valueOf(value));
                } catch (java.lang.NullPointerException e) {
                    getContext().startService(new Intent(getContext(), DiracService.class));
                    DiracService.sDiracUtils.setLevel(String.valueOf(value));
                }
                break;

            case PREF_KEY_FPS_INFO:
                boolean enabled = (Boolean) value;
                Intent fpsinfo = new Intent(this.getContext(), FPSInfoService.class);
                if (enabled) {
                    this.getContext().startService(fpsinfo);
                } else {
                    this.getContext().stopService(fpsinfo);
                }
                break;

            case PREF_SELINUX_MODE:
                boolean on = (Boolean) value;
                new SwitchSelinuxTask(getActivity()).execute(on);
                setSelinuxEnabled(on, mSelinuxPersistence.isChecked());
                break;
            case PREF_SELINUX_PERSISTENCE:
                setSelinuxEnabled(mSelinuxMode.isChecked(), (Boolean) value);
                break;
        }
        return true;
    }

    private void setSelinuxEnabled(boolean status, boolean persistent) {
      SharedPreferences.Editor editor = getContext()
        .getSharedPreferences("selinux_pref", Context.MODE_PRIVATE).edit();
      if (persistent) {
        editor.putBoolean(PREF_SELINUX_MODE, status);
      } else {
        editor.remove(PREF_SELINUX_MODE);
      }
        editor.apply();
        mSelinuxMode.setChecked(status);
     }

    private class SwitchSelinuxTask extends SuTask<Boolean> {
      public SwitchSelinuxTask(Context context) {
        super(context);
      }

      @Override
      protected void sudoInBackground(Boolean... params) throws SuShell.SuDeniedException {
        if (params.length != 1) {
            Log.e(TAG, "SwitchSelinuxTask: invalid params count");
            return;
         }
         if (params[0]) {
            SuShell.runWithSuCheck("setenforce 1");
         } else {
            SuShell.runWithSuCheck("setenforce 0");
         }
      }

      @Override
      protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (!result) {
          // Did not work, so restore actual value
          setSelinuxEnabled(SELinux.isSELinuxEnforced(), mSelinuxPersistence.isChecked());
         }
      }
   }
}
