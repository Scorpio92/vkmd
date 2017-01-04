package ru.scorpio92.vkmd.View;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import ru.scorpio92.vkmd.R;

/**
 * Created by scorpio92 on 02.01.17.
 */

public class Settings extends PreferenceActivity {

    public static final String SETTING_VKMD_FOLDER_KEY = "settings_vkmd_dir";
    public static final String SETTING_AUTO_OPEN_SAVED_KEY = "settings_auto_open_saved";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
