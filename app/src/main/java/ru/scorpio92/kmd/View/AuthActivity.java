package ru.scorpio92.kmd.View;

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

import java.util.ArrayList;

import ru.scorpio92.kmd.Constants;
import ru.scorpio92.kmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.kmd.Operations.GetToken;
import ru.scorpio92.kmd.Operations.GetTrackCount;
import ru.scorpio92.kmd.Operations.GetTrackListByOwnerID;
import ru.scorpio92.kmd.Operations.GetTrackListFromResponseOrDB;
import ru.scorpio92.kmd.Operations.GetUserIdByUserName;
import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Services.AudioService;
import ru.scorpio92.kmd.Services.StoreService;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.CommonUtils;
import ru.scorpio92.kmd.Utils.KMDUtils;


public class AuthActivity extends Activity implements OperationsCallbacks, GetUserIdByUserName.GetUserIdByUserNameCallback, GetTrackCount.GetTrackCountCallback {

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

    ServiceConnection sConnStoreService;
    StoreService storeService;

    private String token;
    private int tracksCount;
    private int currentOffset;
    private TrackList generalTrackList;


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
                        //uid_login.setInputType(InputType.TYPE_CLASS_NUMBER);
                        uid_login.setInputType(InputType.TYPE_CLASS_TEXT);
                        uid_login.setHint(getString(R.string.user_id_hint));
                        passwordTableRow.setVisibility(View.GONE);
                        break;
                    case 1:
                        GET_TRACK_LIST_METHOD = GET_TRACK_LIST_METHOD_BY_GID;
                        uidTableRow.setVisibility(View.VISIBLE);
                        //uid_login.setInputType(InputType.TYPE_CLASS_NUMBER);
                        uid_login.setInputType(InputType.TYPE_CLASS_TEXT);
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


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, KMDUtils.loadLastLogins(AuthActivity.this));

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

                    boolean getByNumericID = false;
                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_UID || GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID) {
                        try {
                            Integer.valueOf(USER_ID);
                            getByNumericID = true;
                        } catch (Exception e) {
                            //Toast.makeText(AuthActivity.this, R.string.string_not_is_id, Toast.LENGTH_SHORT).show();
                            //return;
                        }
                    }

                    if(GET_TRACK_LIST_METHOD == GET_TRACK_LIST_METHOD_BY_GID) //если получаем список по id группы передаем отрицательное значение
                        USER_ID = "-" + USER_ID;

                    lock_unlock_GUI(true);

                    switch (GET_TRACK_LIST_METHOD) {
                        case GET_TRACK_LIST_METHOD_BY_UID:
                        case GET_TRACK_LIST_METHOD_BY_GID:
                            token = Constants.ACCESS_TOKEN_PUBLIC;
                            if(getByNumericID) {
                                //new GetTrackListByOwnerID(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                                new GetTrackCount(AuthActivity.this, USER_ID, token);
                            } else {
                                new GetUserIdByUserName(AuthActivity.this, USER_ID, token);
                            }
                            break;
                        case GET_TRACK_LIST_METHOD_BY_LP:
                            new GetToken(AuthActivity.this, USER_ID, password.getText().toString().trim());
                            break;
                        case GET_TRACK_LIST_METHOD_OFFLINE:
                            if (!KMDUtils.checkMediaLibrary(AuthActivity.this)) {
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
                        if (!KMDUtils.checkMediaLibrary(AuthActivity.this)) {
                            needForCheckAutoEnter = true;
                        } else {
                            new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_SAVED_TRACKLIST_FROM_DB, AuthActivity.this, null);
                        }
                    } else {
                        needForCheckAutoEnter = true;
                    }

                    if(needForCheckAutoEnter && !isRelogin) {
                        USER_ID = KMDUtils.checkAutoEnter(AuthActivity.this);
                        if(USER_ID != null) {
                            uid_login.setText(USER_ID);
                            autoEnter.setChecked(true);
                            //new GetTrackListByOwnerID(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                            token = Constants.ACCESS_TOKEN_PUBLIC;
                            new GetTrackCount(AuthActivity.this, USER_ID, token);
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

    void showMainActivity(final TrackList tracks) {

        startService(new Intent(AuthActivity.this, StoreService.class));

        sConnStoreService = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "StoreService onServiceConnected");
                storeService = ((StoreService.MyBinder) binder).getService();
                storeService.setTrackList(tracks);

                try {
                    Log.w(LOG_TAG, "unbindService");
                    unbindService(sConnStoreService);
                    sConnStoreService = null;
                    storeService = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "StoreService onServiceDisconnected");
            }
        };

        bindService(new Intent(AuthActivity.this, StoreService.class), sConnStoreService, BIND_AUTO_CREATE);
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
                //new GetTrackListByOwnerID(AuthActivity.this, userID, token);
                this.token = token;
                new GetTrackCount(AuthActivity.this, USER_ID, token);
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
                    KMDUtils.writeCurrentLogin(AuthActivity.this, USER_ID, autoEnter.isChecked(), GET_TRACK_LIST_METHOD); //записываем в БД введенный логин
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
        generalTrackList.getAllTracks().addAll(tracks.getAllTracks());
        if(tracks.getAllTracks().isEmpty() && currentOffset == 0) {
            lock_unlock_GUI(false);
            Toast.makeText(this, R.string.problems_with_parsing_response, Toast.LENGTH_SHORT).show();
        } else {
            currentOffset = currentOffset + GetTrackListByOwnerID.DEFAULT_TRACKS_COUNT;
            if(currentOffset < tracksCount) {
                Log.w(LOG_TAG, "GetTrackListByOwnerID, currentOffset: " + currentOffset);
                new GetTrackListByOwnerID(AuthActivity.this, USER_ID, token, currentOffset);
            }  else {
                showMainActivity(generalTrackList);
            }
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

    @Override
    public void onGetUserIDByUserName(String id, int responseCode) {
        switch (responseCode) {
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_OK:
                Log.w(LOG_TAG, "user id: " + id);
                USER_ID = id;
                Toast.makeText(this, getString(R.string.get_user_id_ok) + " " + USER_ID, Toast.LENGTH_SHORT).show();
                //new GetTrackListByOwnerID(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                new GetTrackCount(AuthActivity.this, USER_ID, Constants.ACCESS_TOKEN_PUBLIC);
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_FAIL:
                Log.w(LOG_TAG, "user id: fail");
                lock_unlock_GUI(false);
                Toast.makeText(this, getString(R.string.get_user_id_fail), Toast.LENGTH_SHORT).show();
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_NO_INTERNET:
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onGetTrackCount(int count, int responseCode) {
        switch (responseCode) {
            case GetTrackCount.GET_TRACKS_COUNT_STATUS_OK:
                Log.w(LOG_TAG, "count tracks: " + count);
                tracksCount = count;
                currentOffset = 0;
                generalTrackList = new TrackList();
                Log.w(LOG_TAG, "GetTrackListByOwnerID, currentOffset: " + currentOffset);
                new GetTrackListByOwnerID(AuthActivity.this, USER_ID, token, currentOffset);
                break;
            case GetTrackCount.GET_TRACKS_COUNT_STATUS_FAIL:
                Log.w(LOG_TAG, "user id: fail");
                lock_unlock_GUI(false);
                Toast.makeText(this, getString(R.string.problems_with_parsing_response), Toast.LENGTH_SHORT).show();
                break;
            case GetTrackCount.GET_TRACKS_COUNT_NO_INTERNET:
                lock_unlock_GUI(false);
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }

    }
}
