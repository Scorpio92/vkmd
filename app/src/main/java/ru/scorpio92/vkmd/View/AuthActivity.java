package ru.scorpio92.vkmd.View;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import ru.scorpio92.vkmd.Constants;
import ru.scorpio92.vkmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.vkmd.Operations.GetToken;
import ru.scorpio92.vkmd.Operations.GetTrackListByOwnerID;
import ru.scorpio92.vkmd.Operations.GetTrackListFromResponseOrDB;
import ru.scorpio92.vkmd.R;
import ru.scorpio92.vkmd.Services.AudioService;
import ru.scorpio92.vkmd.Types.Track;
import ru.scorpio92.vkmd.Types.TrackList;
import ru.scorpio92.vkmd.Utils.CommonUtils;
import ru.scorpio92.vkmd.Utils.VKMDUtils;


public class AuthActivity extends Activity implements OperationsCallbacks {

    String LOG_TAG = "AuthActivity";

    public static final int GET_TRACK_LIST_METHOD_BY_UID = 0;
    public static final int GET_TRACK_LIST_METHOD_BY_GID = 1;
    public static final int GET_TRACK_LIST_METHOD_BY_LP = 2;
    private final int GET_TRACK_LIST_METHOD_OFFLINE = 3;
    private int GET_TRACK_LIST_METHOD;

    TableRow uidTableRow, passwordTableRow;
    Spinner method_selector;
    AutoCompleteTextView uid_login;
    EditText password;
    Button go_button;
    CheckBox autoEnter;
    ProgressBar auth_progress;

    private String USER_ID;

    ServiceConnection sConn;
    AudioService audioService;


    void initGUI() {

        uidTableRow = (TableRow) findViewById(R.id.uidTableRow);

        passwordTableRow = (TableRow) findViewById(R.id.passwordTableRow);

        method_selector = (Spinner) findViewById(R.id.method_selector);
        method_selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_BY_UID;
                        uidTableRow.setVisibility(View.VISIBLE);
                        uid_login.setInputType(InputType.TYPE_CLASS_NUMBER);
                        uid_login.setHint(getString(R.string.user_id_hint));
                        passwordTableRow.setVisibility(View.GONE);
                        break;
                    case 1:
                        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_BY_GID;
                        uidTableRow.setVisibility(View.VISIBLE);
                        uid_login.setInputType(InputType.TYPE_CLASS_NUMBER);
                        uid_login.setHint(getString(R.string.group_id_hint));
                        passwordTableRow.setVisibility(View.GONE);
                        break;
                    case 2:
                        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_BY_LP;
                        uidTableRow.setVisibility(View.VISIBLE);
                        uid_login.setInputType(InputType.TYPE_CLASS_TEXT);
                        uid_login.setHint(getString(R.string.login_hint));
                        passwordTableRow.setVisibility(View.VISIBLE);
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        password.setHint(getString(R.string.password_hint));
                        break;
                    case 3:
                        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_OFFLINE;
                        uidTableRow.setVisibility(View.GONE);
                        passwordTableRow.setVisibility(View.GONE);
                        break;
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, VKMDUtils.loadLastLogins(AuthActivity.this));

        uid_login = (AutoCompleteTextView) findViewById(R.id.uid_login);
        uid_login.setAdapter(adapter);
        uid_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uid_login.showDropDown();
            }
        });

        password = (EditText) findViewById(R.id.password);

        go_button = (Button) findViewById(R.id.go_button);
        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    USER_ID = uid_login.getText().toString().trim();

                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_UID || GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID || GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_LP) {
                        if (USER_ID.isEmpty()) {
                            Toast.makeText(AuthActivity.this, R.string.empty_field_warning, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_LP) {
                        if (password.getText().toString().isEmpty()) {
                            Toast.makeText(AuthActivity.this, R.string.empty_field_warning, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_UID || GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID) {
                        try {
                            Integer.valueOf(USER_ID);
                        } catch (Exception e) {
                            Toast.makeText(AuthActivity.this, R.string.string_not_is_id, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID) //если получаем список по id группы передаем отрицательное значение
                        USER_ID = "-" + USER_ID;

                    lock_unlock_GUI(true);

                    switch (GET_TRACK_LIST_METHOD) {
                        case GET_TRACK_LIST_METHOD_BY_UID:
                        case GET_TRACK_LIST_METHOD_BY_GID:
                            new GetTrackListByOwnerID(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                            break;
                        case GET_TRACK_LIST_METHOD_BY_LP:
                            new GetToken(AuthActivity.this, USER_ID, password.getText().toString().trim());
                            break;
                        case GET_TRACK_LIST_METHOD_OFFLINE:
                            if (!VKMDUtils.checkMediaLibrary(AuthActivity.this)) {
                                lock_unlock_GUI(false);
                                Toast.makeText(getApplicationContext(), R.string.none_tracks_in_local_db, Toast.LENGTH_SHORT).show();
                            } else {
                                new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_SAVED_TRACKLIST_FROM_DB, AuthActivity.this, null);
                            }
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    lock_unlock_GUI(false);
                }
            }
        });

        autoEnter = (CheckBox) findViewById(R.id.autoEnter);

        auth_progress = (ProgressBar) findViewById(R.id.auth_progress);
    }

    void initAndStartBindingWithAudioService() {

        final boolean autoCheckMediaLibrary = getIntent().getBooleanExtra("autoCheckMediaLibrary", true);
        final boolean settings_auto_open_saved = CommonUtils.getBooleanSetting(AuthActivity.this, Settings.SETTING_AUTO_OPEN_SAVED_KEY, false);
        final boolean isRelogin = getIntent().getBooleanExtra("isRelogin", false);

        initGUI();

        //при первом запуске блокируем гуи и проверяем, есть ли в БД инф о полученных ранее аудио
        lock_unlock_GUI(true);

        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();
                if(audioService.isStarted()) {
                    Log.w(LOG_TAG, "AudioService is running, start MainActivity");
                    showMainActivity(null);
                } else {
                    Log.w(LOG_TAG, "AudioService is not running, standard init");
                    boolean needForCheckAutoEnter = false;
                    if(autoCheckMediaLibrary && settings_auto_open_saved) {
                        if (!VKMDUtils.checkMediaLibrary(AuthActivity.this)) {
                            needForCheckAutoEnter = true;
                        } else {
                            new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_SAVED_TRACKLIST_FROM_DB, AuthActivity.this, null);
                        }
                    } else {
                        needForCheckAutoEnter = true;
                    }

                    if(needForCheckAutoEnter && !isRelogin) {
                        USER_ID = VKMDUtils.checkAutoEnter(AuthActivity.this);
                        if(USER_ID != null) {
                            uid_login.setText(USER_ID);
                            autoEnter.setChecked(true);
                            new GetTrackListByOwnerID(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                        } else {
                            lock_unlock_GUI(false);
                        }
                    } else {
                        lock_unlock_GUI(false);
                    }
                }

                try {
                    Log.w(LOG_TAG, "unbindService");
                    unbindService(sConn);
                    sConn = null;
                    audioService = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "AudioService onServiceDisconnected");
            }
        };

        bindService(new Intent(this, AudioService.class), sConn, BIND_AUTO_CREATE);
    }

    void lock_unlock_GUI(boolean lock) {
        if (lock) {
            method_selector.setEnabled(false);
            uid_login.setEnabled(false);
            password.setEnabled(false);
            autoEnter.setEnabled(false);
            go_button.setEnabled(false);
            auth_progress.setVisibility(View.VISIBLE);
        } else {
            method_selector.setEnabled(true);
            uid_login.setEnabled(true);
            password.setEnabled(true);
            autoEnter.setEnabled(true);
            go_button.setEnabled(true);
            auth_progress.setVisibility(View.INVISIBLE);
        }
    }

    void showMainActivity(TrackList tracks) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("TrackList", tracks);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_BY_UID; //по-умолчанию получаем записи по uid

        initAndStartBindingWithAudioService();
    }

    @Override
    public void onGetTokenComplete(int status, String token, String userID) {
        switch (status) {
            case GetToken.GET_TOKEN_STATUS_OK:
                Log.w(LOG_TAG, "token is " + token + " user_id is " + userID);
                new GetTrackListByOwnerID(AuthActivity.this, userID, token);
                break;
            case GetToken.GET_TOKEN_STATUS_FAIL:
                Log.w(LOG_TAG, "onGetTokenFailed");
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_auth, Toast.LENGTH_SHORT).show();
                break;
            case GetToken.GET_TOKEN_NO_INTERNET:
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onGetTrackListComplete(int status, String response) {
        switch (status) {
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_OK:
                if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_UID ||
                        GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID ||
                        GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_LP) {
                    VKMDUtils.writeCurrentLogin(AuthActivity.this, USER_ID, autoEnter.isChecked(), GET_TRACK_LIST_METHOD); //записываем в БД введенный логин
                }
                new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FROM_RESPONSE, AuthActivity.this, response);
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_FAIL:
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_get_music_list, Toast.LENGTH_SHORT).show();
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_NO_INTERNET:
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onResponseParseComplete(TrackList tracks) {
        if(!tracks.getAllTracks().isEmpty()) {
            showMainActivity(tracks);
        } else {
            lock_unlock_GUI(false);
            Toast.makeText(this, R.string.problems_with_parsing_response, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGetSavedTracksComplete(TrackList tracks) {
        showMainActivity(tracks);
    }

    @Override
    public void onWriteTrackListToDBComplete(int count) {
        if(count < 0) {
            Toast.makeText(getApplicationContext(), R.string.write_track_to_db_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.w(LOG_TAG, "onWriteTrackListToDBComplete. Writed " + Integer.toString(count) + " tracks");
    }

    @Override
    public void onUpdateDownloadInfoComplete(int count, int action) {

    }

    @Override
    public void onScanTaskComplete(ArrayList<Track> tracks) {

    }
}
