package ru.scorpio92.kmd.View;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import ru.scorpio92.kmd.BuildConfig;
import ru.scorpio92.kmd.R;

/**
 * Created by scorpio92 on 2/9/17.
 */

public class Help extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        //this.setFinishOnTouchOutside(false);\
    }
}
