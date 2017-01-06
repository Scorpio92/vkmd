package ru.scorpio92.kmd.View;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import ru.scorpio92.kmd.R;

/**
 * Created by scorpio92 on 02.01.17.
 */

public class Settings extends PreferenceActivity {

    public static final String SETTING_KMD_FOLDER_KEY = "settings_kmd_dir";
    public static final String SETTING_AUTO_OPEN_SAVED_KEY = "settings_auto_open_saved";
    public static final String SETTING_AUTO_SCAN_SAVED_KEY = "settings_auto_scan_saved";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
