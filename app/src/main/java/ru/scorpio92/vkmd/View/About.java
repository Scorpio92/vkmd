package ru.scorpio92.vkmd.View;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import ru.scorpio92.vkmd.BuildConfig;
import ru.scorpio92.vkmd.R;

/**
 * Created by scorpio92 on 03.01.17.
 */

public class About extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView about_current_version = (TextView) findViewById(R.id.about_current_version);
        about_current_version.setText(about_current_version.getText() + " " + BuildConfig.VERSION_NAME);
    }
}
