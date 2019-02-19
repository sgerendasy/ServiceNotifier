package com.sigkey.ServiceAlert;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import static com.sigkey.ServiceAlert.MainActivity.MOBILE_OFF_STRING;
import static com.sigkey.ServiceAlert.MainActivity.MOBILE_ON_STRING;
import static com.sigkey.ServiceAlert.MainActivity.WIFI_OFF_STRING;
import static com.sigkey.ServiceAlert.MainActivity.WIFI_ON_STRING;
import static com.sigkey.ServiceAlert.MainActivity.AUDIO_STREAM_TYPE;
import static com.sigkey.ServiceAlert.MainActivity.mediaPlayer;
import static com.sigkey.ServiceAlert.MainActivity.mobileConnected;
import static com.sigkey.ServiceAlert.MainActivity.wifiConnected;


public class MainActivity extends AppCompatActivity {

    // For logging purposes
    public static final String TAG = "MainActivity";
    // Whether there is a Wi-Fi connection.
    public static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    public static boolean mobileConnected = false;

    public static ArrayList<LogEntry> logs = new ArrayList<>();
    ArrayAdapter<LogEntry> logsArrayAdapter;
    public static WifiManager wifiManager;
    
    public static ArrayList<SoundInfo> notificationSounds;

    public static final int AUDIO_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    public boolean appIsOn;

    public static MainActivity mainActivityInstance;
    private SlidingUpPanelLayout mLayout;
    // The BroadcastReceiver that tracks network connectivity changes.
    public static NetworkReceiver receiver = new NetworkReceiver();
    public static IntentFilter filter;

    public SharedPreferences sharedPref;

    // Used as temp storage of a new notification sound ID before notification sound change is saved/cancelled
    public int onSoundIdTemp = -1;
    public int offSoundIdTemp = -1;

    public static final String MOBILE_ON_STRING = "Mobile On";
    public static final String MOBILE_OFF_STRING = "Mobile Off";
    public static final String WIFI_ON_STRING = "Wifi On";
    public static final String WIFI_OFF_STRING = "Wifi Off";

    public static AudioManager mAudioManager;
    public static int MaxVolume = 100;
    public static MediaPlayer mediaPlayer = new MediaPlayer();
    private LogEntrySQL logEntryDB;
    public static RadioGroup leftRadioGroupColumn;
    public static RadioGroup rightRadioGroupColumn;


    // Global booleans to help keep track of whether a newly set sound is a wifi/mobile & on/off sound
    public static boolean changeMobileSound = false;
    public static boolean changeOnSound = true;



    public static MainActivity getInstance()
    {
        return mainActivityInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set persistent data values
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Check for permissions. Ask if necessary.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        }

        InitializeMainActivityView();

        wifiManager = (WifiManager) getApplicationContext().getSystemService (Context.WIFI_SERVICE);
        sharedPref.edit().putString("NameOfLastWifiConnection", wifiManager.getConnectionInfo().getSSID()).apply();
        ConnectivityManager conn = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        if (networkInfo != null)
            sharedPref.edit().putString("NameOfLastMobileConnection", networkInfo.getExtraInfo()).apply();


        // Registers BroadcastReceiver to track network connection changes.
        filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (mAudioManager != null)
            MaxVolume = mAudioManager.getStreamMaxVolume(AUDIO_STREAM_TYPE);
        mainActivityInstance = this;

        logEntryDB = new LogEntrySQL(this);
        logs = logEntryDB.getLogs();


        PopulateNotificationSoundsDictionary();
        PopulateRadioGroupLayout();

        // this value is used to make sure notification sounds don't play when programmatically changing a radio group notification sound.
        sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();

        // Finally, turn the network receiver on or off.
        toggleService(appIsOn, false);
    }

    @Override
    public void onStart () {
        super.onStart();
        updateConnectedFlags();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().toString().equals(getResources().getString(R.string.AboutLabel)))
        {
            Intent settingsIntent = new Intent(this, About.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    sharedPref.edit().putString("NameOfLastWifiConnection", wifiManager.getConnectionInfo().getSSID()).apply();
                }
                else
                {
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setTitle("Permission Requested")
                            .setMessage("Permission to access device location is needed only to display the names of wifi networks. Permissions can be changed in your phone's settings.")
                            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) { }}).create();

                    alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface ad) {
                            ((AlertDialog)ad).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.blueText));
                        }
                    });

                    alertDialog.show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void InitializeMainActivityView()
    {
        Point screenDimensions = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenDimensions);

        appIsOn = sharedPref.getBoolean(getResources().getString(R.string.AppOnPref), true);
        int appOnImageId = appIsOn ? R.drawable.connected_logo : R.drawable.disconnected_logo;
        ((ImageButton)findViewById(R.id.appOnButton)).setImageResource(appOnImageId);

        SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeBar);
        volumeSlider.setProgress(sharedPref.getInt(getResources().getString(R.string.VolumePref), 70));
        ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + volumeSlider.getProgress() + "%");
        int realVolume = (int)(MaxVolume * ((float)volumeSlider.getProgress() / 100));
        mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, realVolume, AudioManager.FLAG_VIBRATE);
        // seek bar listener event for getting the volume's int value
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int realVolume = (int)(MaxVolume * ((float)progress / 100));                                         // calculate it
                sharedPref.edit().putInt(getResources().getString(R.string.VolumePref), progress).apply();                                           // persist it
                ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + progress + "%");                   // label it
                mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, realVolume, AudioManager.FLAG_VIBRATE);            // bop it
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Setup the log entries slide-up
        ListView logListView = (ListView)findViewById(R.id.logList);
        logsArrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                logs );
        logListView.setAdapter(logsArrayAdapter);

        mLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
        mLayout.setPanelHeight((int)((double)screenDimensions.y * 0.05));
        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) { }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) { }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.EnableToastBoolean), true));
        ((CheckBox)findViewById(R.id.PersistVolumeCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.PersistAlertVolume), false));

        ((CheckBox)findViewById(R.id.captureMobileCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.ChangeMobileSoundCheckbox), true));
        ((CheckBox)findViewById(R.id.captureWifiCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.ChangeWifiSoundCheckbox), true));
        findViewById(R.id.SetMobileSoundsTextView).setVisibility(((CheckBox)findViewById(R.id.captureMobileCheckbox)).isChecked() ? View.VISIBLE : View.GONE);
        findViewById(R.id.SetWifiSoundsTextView).setVisibility(((CheckBox)findViewById(R.id.captureWifiCheckbox)).isChecked() ? View.VISIBLE : View.GONE);

        ImageButton onOffButton = (ImageButton) findViewById(R.id.appOnButton);
        ViewGroup.LayoutParams onOffButtonParams = onOffButton.getLayoutParams();
        int newWidth = (int)((double)screenDimensions.x / 1.45);
        int newHeight = (int)((double)newWidth * 0.579365);
        onOffButtonParams.height = newHeight;
        onOffButtonParams.width = newWidth;
        onOffButton.setLayoutParams(onOffButtonParams);
        onOffButton.setScaleType(ImageView.ScaleType.FIT_XY);

        boolean is12HourChecked = sharedPref.getBoolean(getResources().getString(R.string.IsTwelveHourBoolean), true);
        if (is12HourChecked)
            ((RadioButton)findViewById(R.id.TwelveHour)).setChecked(true);
        else
            ((RadioButton)findViewById(R.id.TwentyFourHour)).setChecked(true);
        String dateFormatSelected = sharedPref.getString(getResources().getString(R.string.LogDateFormat), "MM/dd/yyyy");
        switch (dateFormatSelected)
        {
            case "MM/dd/yyyy":
                ((RadioButton)findViewById(R.id.MMDDYYYY)).setChecked(true);
                break;
            case "dd/MM/yyyy":
                ((RadioButton)findViewById(R.id.DDMMYYYY)).setChecked(true);
                break;
            case "MMM d, yyyy":
                ((RadioButton)findViewById(R.id.HumanReadable)).setChecked(true);
                break;
        }

        // Set fonts
        Typeface ubuntuLight = Typeface.createFromAsset(getAssets(), "fonts/Ubuntu-L.ttf");
        Typeface ubuntuMedium = Typeface.createFromAsset(getAssets(), "fonts/Ubuntu-M.ttf");
        ((TextView)findViewById(R.id.volumeLabel)).setTypeface(ubuntuLight);

        Button settingsButton = ((Button)findViewById(R.id.settingsButton));
        settingsButton.setTypeface(ubuntuLight);
        boolean settingsDisplayed = sharedPref.getBoolean("SettingsDisplayed", true);
        findViewById(R.id.settingsLinearLayout).setVisibility(settingsDisplayed? View.VISIBLE : View.GONE);

        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        Button saveButton = (Button)findViewById(R.id.saveButton);
        cancelButton.setMinimumWidth((getResources().getDisplayMetrics().widthPixels / 2) - 60);
        cancelButton.setTypeface(ubuntuLight);
        saveButton.setMinimumWidth((getResources().getDisplayMetrics().widthPixels / 2) - 60);
        saveButton.setTypeface(ubuntuLight);


        ((CheckBox)findViewById(R.id.captureMobileCheckbox)).setTypeface(ubuntuLight);
        ((CheckBox)findViewById(R.id.captureWifiCheckbox)).setTypeface(ubuntuLight);
        ((TextView)findViewById(R.id.SetMobileSoundsTextView)).setTypeface(ubuntuLight);
        ((TextView)findViewById(R.id.SetWifiSoundsTextView)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.LogTimeFormatLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.TwelveHour)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.TwentyFourHour)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.LogDateFormatLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.DDMMYYYY)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.MMDDYYYY)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.HumanReadable)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.OtherSettingsLabel)).setTypeface(ubuntuMedium);
        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.DeleteLogsButton)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.PersistVolumeCheckbox)).setTypeface(ubuntuLight);

        TabHost mTabHost = (TabHost) findViewById(R.id.changeSoundTabhost);
        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("ChangeOnSound").setIndicator("Set ON Sound").setContent(R.id.editSoundsLayout));
        mTabHost.addTab(mTabHost.newTabSpec("ChangeOffSound").setIndicator("Set OFF Sound").setContent(R.id.editSoundsLayout));
        mTabHost.setCurrentTab(1);
        mTabHost.setCurrentTab(0);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTypeface(ubuntuMedium);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextSize(15);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setAllCaps(false);
        (mTabHost.getTabWidget().getChildTabViewAt(0)).getLayoutParams().height = (int) (40 * this.getResources().getDisplayMetrics().density);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTypeface(ubuntuMedium);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextSize(15);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setAllCaps(false);
        (mTabHost.getTabWidget().getChildTabViewAt(1)).getLayoutParams().height = (int) (40 * this.getResources().getDisplayMetrics().density);

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                TabHost mTabHost = (TabHost) findViewById(R.id.changeSoundTabhost);
                // Set colors for each tab on tab selection change event
                int otherTab = (mTabHost.getCurrentTab() * -1) + 1;
                mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(getColor(R.color.blueBackground));
                mTabHost.getTabWidget().getChildTabViewAt(otherTab).setBackgroundColor(getColor(R.color.cardview_light_background));
                ((TextView)mTabHost.getTabWidget().getChildTabViewAt(mTabHost.getCurrentTab()).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
                ((TextView)mTabHost.getTabWidget().getChildTabViewAt(otherTab).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));

                String SelectedSoundType;
                int defaultSoundId;
                changeOnSound = tabId.equals("ChangeOnSound");
                // if a mobile notification sound is getting changed
                if (changeMobileSound)
                {
                    SelectedSoundType = changeOnSound? MOBILE_ON_STRING : MOBILE_OFF_STRING;
                    defaultSoundId = changeOnSound? R.raw.guitar_riff : R.raw.guitar_raff;
                }
                // else if a wifi notification sound is getting changed
                else
                {
                    SelectedSoundType = changeOnSound? WIFI_ON_STRING : WIFI_OFF_STRING;
                    defaultSoundId = changeOnSound? R.raw.affirmative : R.raw.negative;
                }

                int selectedSoundId;
                if ((changeOnSound && onSoundIdTemp != -1) || (!changeOnSound && offSoundIdTemp != -1))
                    selectedSoundId = changeOnSound? onSoundIdTemp : offSoundIdTemp;
                else
                    selectedSoundId = sharedPref.getInt(SelectedSoundType, defaultSoundId);

                int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
                if (radioButtonIndex >= notificationSounds.size() / 2)
                {
                    sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
                    rightRadioGroupColumn.check(radioButtonIndex);
                    sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();

                }
                else
                {
                    sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
                    leftRadioGroupColumn.check(radioButtonIndex);
                    sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();
                }
            }
        });
    }

    // Toggle the visibility of app settings. Persist choice.
    public void SettingsButtonClicked(View view)
    {
        boolean settingsDisplayed = !sharedPref.getBoolean("SettingsDisplayed", true);
        findViewById(R.id.settingsLinearLayout).setVisibility(settingsDisplayed? View.VISIBLE : View.GONE);
        if (!settingsDisplayed)
            findViewById(R.id.changeSoundTabhost).setVisibility(View.GONE);
        sharedPref.edit().putBoolean("SettingsDisplayed", settingsDisplayed).apply();
    }

    public void addLog(String dateTime, String connInfo)
    {
        LogEntry newLog = new LogEntry(dateTime, connInfo);
        logs.add(0, newLog);
        logEntryDB.addEntry(newLog);
        logsArrayAdapter.notifyDataSetChanged();
        try
        {
            FileOutputStream logFileStream = this.openFileOutput(getResources().getString(R.string.ServiceLogFilename), Context.MODE_PRIVATE);
            for (LogEntry s : logs)
            {
                logFileStream.write(s.toString().getBytes());
            }
            logFileStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int ClearLogs()
    {
        logs.clear();
        logsArrayAdapter.notifyDataSetChanged();
        return logEntryDB.ClearLogs();
    }

    private RadioGroup.OnCheckedChangeListener leftColumnListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            rightRadioGroupColumn.setOnCheckedChangeListener(null);
            rightRadioGroupColumn.clearCheck();
            rightRadioGroupColumn.setOnCheckedChangeListener(rightColumnListener);

            int selectedSoundId = getSoundInfoByIndex(checkedId).rawSoundID;
            if (sharedPref.getBoolean(getResources().getString(R.string.PreviewSoundBoolean), true))
            {
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(getApplicationContext(), selectedSoundId);
                mediaPlayer.start();
            }
            if (changeOnSound)
                onSoundIdTemp = selectedSoundId;
            else
                offSoundIdTemp = selectedSoundId;
        }
    };

    private RadioGroup.OnCheckedChangeListener rightColumnListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            leftRadioGroupColumn.setOnCheckedChangeListener(null);
            leftRadioGroupColumn.clearCheck();
            leftRadioGroupColumn.setOnCheckedChangeListener(leftColumnListener);

            int selectedSoundId = getSoundInfoByIndex(checkedId).rawSoundID;
            if (sharedPref.getBoolean(getResources().getString(R.string.PreviewSoundBoolean), true))
            {
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(getApplicationContext(), selectedSoundId);
                mediaPlayer.start();
            }
            if (changeOnSound)
                onSoundIdTemp = selectedSoundId;
            else
                offSoundIdTemp = selectedSoundId;
        }
    };

    public void CancelButtonClicked(View view)
    {
        findViewById(R.id.changeSoundTabhost).setVisibility(View.GONE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.VISIBLE);
        onSoundIdTemp = -1;
        offSoundIdTemp = -1;
    }

    public void SaveButtonClicked(View view)
    {
        findViewById(R.id.changeSoundTabhost).setVisibility(View.GONE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.VISIBLE);

        if (onSoundIdTemp != -1)
        {
            String type = changeMobileSound? MOBILE_ON_STRING : WIFI_ON_STRING;
            sharedPref.edit().putInt(type, onSoundIdTemp).apply();
        }
        if (offSoundIdTemp != -1)
        {
            String type = changeMobileSound? MOBILE_OFF_STRING : WIFI_OFF_STRING;
            sharedPref.edit().putInt(type, offSoundIdTemp).apply();
        }
        onSoundIdTemp = -1;
        offSoundIdTemp = -1;
    }

    // Add a radio button for each notification sound into a two-column radio group.
    public void PopulateRadioGroupLayout()
    {
        leftRadioGroupColumn = (RadioGroup)findViewById(R.id.leftSoundsRadioGroup);
        leftRadioGroupColumn.setMinimumWidth(getResources().getDisplayMetrics().widthPixels / 2);
        rightRadioGroupColumn = (RadioGroup)findViewById(R.id.rightSoundsRadioGroup);
        rightRadioGroupColumn.setMinimumWidth(getResources().getDisplayMetrics().widthPixels / 2);

        leftRadioGroupColumn.setOnCheckedChangeListener(leftColumnListener);
        rightRadioGroupColumn.setOnCheckedChangeListener(rightColumnListener);

        ColorStateList radioButtonColorList = new ColorStateList(
                new int[][]{ new int[]{-android.R.attr.state_enabled}, new int[]{android.R.attr.state_enabled}},
                new int[] { getColor(R.color.greyBackground), getColor(R.color.blueText) }
        );

        for (int i = 0; i < notificationSounds.size() / 2; i++)
        {
            RadioButton newRadioButton = new RadioButton(this);
            newRadioButton.setButtonTintList(radioButtonColorList);
            newRadioButton.setText(notificationSounds.get(i).soundName);
            newRadioButton.setId(notificationSounds.get(i).radioButtonIndex);
            leftRadioGroupColumn.addView(newRadioButton);
        }
        for (int i = notificationSounds.size() / 2; i < notificationSounds.size(); i++)
        {
            RadioButton newRadioButton = new RadioButton(this);
            newRadioButton.setButtonTintList(radioButtonColorList);
            newRadioButton.setText(notificationSounds.get(i).soundName);
            newRadioButton.setId(notificationSounds.get(i).radioButtonIndex);
            rightRadioGroupColumn.addView(newRadioButton);
        }
    }


    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    public void updateConnectedFlags()
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        assert connMgr != null;
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            mobileConnected = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }

    public void AppOn(View view)
    {
        // Unregisters BroadcastReceiver when app is destroyed.
        appIsOn = !appIsOn;
        toggleService(appIsOn, true);
        DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
        String eventString = appIsOn ? "Service Alert Turned On" : "Service Alert Turned Off";
        addLog(logDatetime, eventString);
    }

    public void CaptureMobileChecked(View view)
    {
        boolean val = ((CheckBox)view).isChecked();
        sharedPref.edit().putBoolean(getResources().getString(R.string.ChangeMobileSoundCheckbox), val).apply();
        (findViewById(R.id.SetMobileSoundsTextView)).setVisibility(val ? View.VISIBLE : View.GONE);
    }

    public void CaptureWifiChecked(View view)
    {
        boolean val = ((CheckBox)view).isChecked();
        sharedPref.edit().putBoolean(getResources().getString(R.string.ChangeWifiSoundCheckbox), val).apply();
        (findViewById(R.id.SetWifiSoundsTextView)).setVisibility(val ? View.VISIBLE : View.GONE);
    }

    public void toggleService(boolean on, boolean showToast)
    {
        sharedPref.edit().putBoolean(getResources().getString(R.string.AppOnPref), on).apply();
        int appOnImageId = on ? R.drawable.connected_logo : R.drawable.disconnected_logo;
        ((ImageButton)findViewById(R.id.appOnButton)).setImageResource(appOnImageId);
        try
        {
            String serviceChangeString = "Service Alert Turned On";
            if (on)
            {
                this.getApplicationContext().registerReceiver(receiver, filter);
            }
            else
            {
                serviceChangeString = "Service Alert Turned Off";
                this.getApplicationContext().unregisterReceiver(receiver);
            }
            if (showToast && MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
            {
                Toast.makeText(getApplicationContext(), serviceChangeString, Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public void ChangeMobileSoundsTextClicked(View view)
    {
        findViewById(R.id.changeSoundTabhost).setVisibility(View.VISIBLE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.GONE);
        TabHost tabHost = ((TabHost)findViewById(R.id.changeSoundTabhost));
        tabHost.setCurrentTab(0);
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(getColor(R.color.blueBackground));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));
        changeMobileSound = true;
        int selectedSoundId = sharedPref.getInt(MOBILE_ON_STRING, R.raw.affirmative);
        int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
        if (radioButtonIndex >= notificationSounds.size() / 2)
        {
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
            rightRadioGroupColumn.check(radioButtonIndex);
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();
        }
        else
        {
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
            leftRadioGroupColumn.check(radioButtonIndex);
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();
        }
    }

    public void ChangeWifiSoundsTextClicked(View view)
    {
        findViewById(R.id.changeSoundTabhost).setVisibility(View.VISIBLE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.GONE);
        TabHost tabHost = ((TabHost)findViewById(R.id.changeSoundTabhost));
        tabHost.setCurrentTab(0);
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(getColor(R.color.blueBackground));
        changeMobileSound = false;
        int selectedSoundId = sharedPref.getInt(WIFI_ON_STRING, R.raw.affirmative);
        int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
        if (radioButtonIndex >= notificationSounds.size() / 2)
        {
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
            rightRadioGroupColumn.check(radioButtonIndex);
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();
        }
        else
        {
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), false).apply();
            leftRadioGroupColumn.check(radioButtonIndex);
            sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), true).apply();
        }
    }


    public void PopulateNotificationSoundsDictionary()
    {
        notificationSounds = new ArrayList<>();
        notificationSounds.add(new SoundInfo(R.raw.affirmative, 0, getResources().getString(R.string.Affirmative)));
        notificationSounds.add(new SoundInfo(R.raw.negative, 1, getResources().getString(R.string.Negative)));
        notificationSounds.add(new SoundInfo(R.raw.guitar_riff, 2, getResources().getString(R.string.GuitarRiff)));
        notificationSounds.add(new SoundInfo(R.raw.guitar_raff, 3, getResources().getString(R.string.GuitarRaff)));
        notificationSounds.add(new SoundInfo(R.raw.aliens1, 4, getResources().getString(R.string.Aliens1)));
        notificationSounds.add(new SoundInfo(R.raw.aliens2, 5, getResources().getString(R.string.Aliens2)));
        notificationSounds.add(new SoundInfo(R.raw.tropical, 6, getResources().getString(R.string.Tropical)));
        notificationSounds.add(new SoundInfo(R.raw.chime, 7, getResources().getString(R.string.Chime)));
        notificationSounds.add(new SoundInfo(R.raw.bell, 8, getResources().getString(R.string.Bell)));
        notificationSounds.add(new SoundInfo(R.raw.surprised, 9, getResources().getString(R.string.Surprised)));
        notificationSounds.add(new SoundInfo(R.raw.clicks, 10, getResources().getString(R.string.Clicks)));
        notificationSounds.add(new SoundInfo(R.raw.slide_down, 11, getResources().getString(R.string.SlideDown)));
    }

    public SoundInfo getSoundInfo(int soundId)
    {
        for (SoundInfo soundInfo : notificationSounds)
        {
            if (soundInfo.rawSoundID == soundId)
                return soundInfo;
        }
        return null;
    }

    public SoundInfo getSoundInfoByIndex(int radioButtonIndex)
    {
        for (SoundInfo soundInfo : notificationSounds)
        {
            if (soundInfo.radioButtonIndex == radioButtonIndex)
                return soundInfo;
        }
        return null;
    }

    public void TimeFormatClicked(View view)
    {
        String key = getResources().getString(R.string.IsTwelveHourBoolean);
        switch (view.getId())
        {
            case R.id.TwelveHour:
                sharedPref.edit().putBoolean(key, true).apply();
                sharedPref.edit().putBoolean(getResources().getString(R.string.IsTwelveHourBoolean), true).apply();
                break;
            case (R.id.TwentyFourHour):
                sharedPref.edit().putBoolean(key, false).apply();
                sharedPref.edit().putBoolean(getResources().getString(R.string.IsTwelveHourBoolean), false).apply();
                break;
            default:
                Log.d(TAG, "Unknown time format selection made.");
        }
        logsArrayAdapter.notifyDataSetChanged();
    }

    public void DateFormatClicked(View view)
    {
        switch (view.getId())
        {
            case R.id.MMDDYYYY:
                sharedPref.edit().putString(getResources().getString(R.string.LogDateFormat), "MM/dd/yyyy").apply();
                break;
            case (R.id.DDMMYYYY):
                sharedPref.edit().putString(getResources().getString(R.string.LogDateFormat), "dd/MM/yyyy").apply();
                break;
            case (R.id.HumanReadable):
                sharedPref.edit().putString(getResources().getString(R.string.LogDateFormat), "MMM d, yyyy").apply();
                break;
            default:
                Log.d(TAG, "Unknown date format selection made.");
        }
        logsArrayAdapter.notifyDataSetChanged();
    }

    public void EnableToastChecked(View view)
    {
        CheckBox checkBox = (CheckBox)view;
        sharedPref.edit().putBoolean(getResources().getString(R.string.EnableToastBoolean), checkBox.isChecked()).apply();
    }

    public void EnablePersistentVolumeChecked(View view)
    {
        boolean value = ((CheckBox)view).isChecked();
        sharedPref.edit().putBoolean(getResources().getString(R.string.PersistAlertVolume), value).apply();
    }

    public void DeleteLogsClicked(View view)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Delete all logs?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int deletedLogsCount = ClearLogs();
                        if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                        {
                            String logsStringButMaybePlural = " Logs.";
                            if (deletedLogsCount == 1)
                            {
                                logsStringButMaybePlural = " Log.";
                            }
                            Toast.makeText(MainActivity.this, "Deleted all " + deletedLogsCount + logsStringButMaybePlural , Toast.LENGTH_SHORT).show();
                        }

                    }})
                .setNegativeButton("Cancel", null).create();

        alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ad) {
                ((AlertDialog)ad).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.blueText));
                ((AlertDialog)ad).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.greyBackgroundDarkest));
            }
        });

        alertDialog.show();
    }

    public void HelpButtonPressed(View view)
    {
        int helpButtonId = view.getId();
        String message = "";
        String title = "";
        switch (helpButtonId)
        {
            case R.id.PersistAlertVolumeHelpButton:
                title = "Persist Alert Volume";
                message = "When checked, alerts will sound at the volume set by Service Alert, regardless of cell phone volume. When unchecked, cell phone volume may override the volume set by Service Alert.";
                break;
            case R.id.EnableToastHelpButton:
                title = "Enable Pop-up Notifications";
                message = "Checking this option enables pop-up notifications for a service change event.";
                break;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }}).create();

        alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ad) {
                ((AlertDialog)ad).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.blueText));
            }
        });
        alertDialog.show();
    }
}

class NetworkReceiver extends BroadcastReceiver
{
    // used to store phone's volume
    static int oldVolume;

    @Override
    public void onReceive(final Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        boolean wifi = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        boolean mobile = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();


        boolean CheckForMobileConnection = MainActivity.getInstance().sharedPref.getBoolean("ChangeMobileSoundCheckbox", true);
        boolean CheckForWifiConnection = MainActivity.getInstance().sharedPref.getBoolean("ChangeWifiSoundCheckbox", true);

        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
        {
            oldVolume = MainActivity.mAudioManager.getStreamVolume(AUDIO_STREAM_TYPE);
            int volume = MainActivity.getInstance().sharedPref.getInt("VolumePref", 70);
            int realVolume = (int)(MainActivity.MaxVolume * ((float)volume / 100));
            MainActivity.mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, realVolume, AudioManager.FLAG_VIBRATE);
        }

        if(networkInfo != null)
        {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !wifiConnected && CheckForWifiConnection)
            {
                wifiConnected = true;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_ON_STRING, R.raw.affirmative);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                        {
                            MainActivity.mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, oldVolume, AudioManager.FLAG_VIBRATE);
                        }

                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                WifiInfo info = MainActivity.getInstance().wifiManager.getConnectionInfo();
                String ssid  = info.getSSID();
                MainActivity.getInstance().sharedPref.edit().putString("NameOfLastWifiConnection", ssid).apply();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Wifi connected to " + ssid;
                MainActivity.getInstance().addLog(logDatetime, connInfo);
                if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                    Toast.makeText(context, connInfo, Toast.LENGTH_SHORT).show();

            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnected && CheckForMobileConnection)
            {
                mobileConnected = true;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_ON_STRING, R.raw.guitar_riff);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                        {
                            MainActivity.mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, oldVolume, AudioManager.FLAG_VIBRATE);
                        }

                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                MainActivity.getInstance().sharedPref.edit().putString("NameOfLastMobileConnection", (networkInfo.getExtraInfo() == null ? "unknown" : networkInfo.getExtraInfo())).apply();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Mobile connected to " + (networkInfo.getExtraInfo() == null ? "unknown" : networkInfo.getExtraInfo());
                MainActivity.getInstance().addLog(logDatetime, connInfo);
                if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                    Toast.makeText(context, connInfo, Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            if (!wifi && wifiConnected && CheckForWifiConnection)
            {
                wifiConnected = false;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_OFF_STRING, R.raw.negative);

                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                        {
                            MainActivity.mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, oldVolume, AudioManager.FLAG_VIBRATE);
                        }

                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());

                String connInfo = "Wifi disconnected from " + MainActivity.getInstance().sharedPref.getString("NameOfLastWifiConnection", "unknown");
                MainActivity.getInstance().addLog(logDatetime, connInfo);
                if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                    Toast.makeText(context, connInfo, Toast.LENGTH_SHORT).show();
            }
            else if(!mobile && mobileConnected && CheckForMobileConnection)
            {

                mobileConnected = false;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_OFF_STRING, R.raw.guitar_raff);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                        {
                            MainActivity.mAudioManager.setStreamVolume(AUDIO_STREAM_TYPE, oldVolume, AudioManager.FLAG_VIBRATE);
                        }

                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());

                String connInfo = "Mobile disconnected from " + MainActivity.getInstance().sharedPref.getString("NameOfLastMobileConnection", "unknown");
                MainActivity.getInstance().addLog(logDatetime, connInfo);
                if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                    Toast.makeText(context, connInfo, Toast.LENGTH_SHORT).show();
            }
        }
    }
}


class SoundInfo
{
    public int rawSoundID;
    public int radioButtonIndex;
    public String soundName;
    public SoundInfo(int rawSoundID, int radioButtonIndex, String soundName)
    {
        this.rawSoundID = rawSoundID;
        this.radioButtonIndex = radioButtonIndex;
        this.soundName = soundName;
    }
}


class LogEntry
{
    public String dateTime;
    public String connectionInfo;


    public LogEntry(String datetime, String connInfo)
    {
        this.dateTime = datetime;
        this.connectionInfo = connInfo;
    }

    @Override
    public String toString()
    {
        StringBuilder tempOutputString = new StringBuilder();
        try
        {
            String dateFormat = MainActivity.getInstance().sharedPref.getString("LogDateFormat", "MM/dd/yyyy");
            String timeFormat = MainActivity.getInstance().sharedPref.getBoolean("is12hour", true) ? "hh:mm:ss a" : "HH:mm:ss";

            DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date tempDateTime = datetimeFormat.parse(this.dateTime);

            DateFormat outputDateTime = new SimpleDateFormat(dateFormat + " " + timeFormat);
            tempOutputString.append(outputDateTime.format(tempDateTime)).append("\n");
            tempOutputString.append(this.connectionInfo);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return tempOutputString.toString();
    }
}