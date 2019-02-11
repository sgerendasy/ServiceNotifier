package com.example.email.serviceindicator;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
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

import static com.example.email.serviceindicator.MainActivity.MOBILE_OFF_TYPE;
import static com.example.email.serviceindicator.MainActivity.MOBILE_ON_TYPE;
import static com.example.email.serviceindicator.MainActivity.WIFI_OFF_TYPE;
import static com.example.email.serviceindicator.MainActivity.WIFI_ON_TYPE;
import static com.example.email.serviceindicator.MainActivity.audioStreamType;
import static com.example.email.serviceindicator.MainActivity.mediaPlayer;
import static com.example.email.serviceindicator.MainActivity.mobileConnected;
import static com.example.email.serviceindicator.MainActivity.oldVolume;
import static com.example.email.serviceindicator.MainActivity.wifiConnected;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    // Whether there is a Wi-Fi connection.
    public static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    public static boolean mobileConnected = false;

    public static ArrayList<LogEntry> logArray = new ArrayList<>();
    ArrayAdapter<LogEntry> arrayAdapter;


    public static WifiManager wifiManager;


    public static ArrayList<SoundInfo> soundsArray;
    public static String SelectedSoundType = "Mobile On";

    public static final int audioStreamType = AudioManager.STREAM_MUSIC;
    public static int oldVolume;
    public boolean appIsOn;

    public static MainActivity mainActivityInstance;
    private SlidingUpPanelLayout mLayout;
    // The BroadcastReceiver that tracks network connectivity changes.
    public static NetworkReceiver receiver = new NetworkReceiver();

    public SharedPreferences sharedPref;
    public static boolean is12HourFormat;
    public static String logDateFormat = "MM/dd/yyyy";

    public static String mobileName;
    public static String wifiName;
    public int onSoundIdTemp = -1;
    public int offSoundIdTemp = -1;

    public static final String MOBILE_ON_TYPE = "Mobile On";
    public static final String MOBILE_OFF_TYPE = "Mobile Off";
    public static final String WIFI_ON_TYPE = "Wifi On";
    public static final String WIFI_OFF_TYPE = "Wifi Off";

    public static IntentFilter filter;
    public static AudioManager mAudioManager;
    public static int MaxVolume = 100;
    public static MediaPlayer mediaPlayer = new MediaPlayer();
    private LogEntrySQL logEntryDB;
    public static RadioGroup leftRadioGroupColumn;
    public static RadioGroup rightRadioGroupColumn;

    public static boolean changeMobileSound = false;
    public static boolean changeOnSound = true;



    public static MainActivity getInstance()
    {
        return mainActivityInstance;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    wifiName = wifiManager.getConnectionInfo().getSSID();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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


        wifiManager = (WifiManager) getApplicationContext().getSystemService (Context.WIFI_SERVICE);
        wifiName = wifiManager.getConnectionInfo().getSSID();
        ConnectivityManager conn = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        mobileName = networkInfo.getExtraInfo();


        // Registers BroadcastReceiver to track network connection changes.
        filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (mAudioManager != null)
            MaxVolume = mAudioManager.getStreamMaxVolume(audioStreamType);
        mainActivityInstance = this;

        // Set persistent data values
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        appIsOn = sharedPref.getBoolean(getResources().getString(R.string.AppOnPref), true);
        int appOnImageId = appIsOn ? R.drawable.connected_logo : R.drawable.disconnected_logo;
        ((ImageButton)findViewById(R.id.appOnButton)).setImageResource(appOnImageId);

        SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeBar);
        volumeSlider.setProgress(sharedPref.getInt(getResources().getString(R.string.VolumePref), 70));
        ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + volumeSlider.getProgress() + "%");
        int realVolume = (int)(MaxVolume * ((float)volumeSlider.getProgress() / 100));
        mAudioManager.setStreamVolume(audioStreamType, realVolume, AudioManager.FLAG_VIBRATE);

        is12HourFormat = sharedPref.getBoolean(getResources().getString(R.string.IsTwelveHourBoolean), true);
        logDateFormat = sharedPref.getString(getResources().getString(R.string.LogDateFormat), "MM/dd/yyyy");
        logEntryDB = new LogEntrySQL(this);
        logArray = logEntryDB.getLogs();


        populateSoundsDict();
        populateRadioGroupLayout();


        ((CheckBox)findViewById(R.id.previewSoundCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.PreviewSoundBoolean), true));
        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.EnableToastBoolean), false));
        ((CheckBox)findViewById(R.id.PersistVolumeCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.PersistAlertVolume), false));

        ((CheckBox)findViewById(R.id.captureMobileCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.ChangeMobileSoundCheckbox), true));
        ((CheckBox)findViewById(R.id.captureWifiCheckbox)).setChecked(sharedPref.getBoolean(getResources().getString(R.string.ChangeWifiSoundCheckbox), true));
        findViewById(R.id.SetMobileSoundsTextView).setVisibility(((CheckBox)findViewById(R.id.captureMobileCheckbox)).isChecked() ? View.VISIBLE : View.GONE);
        findViewById(R.id.SetWifiSoundsTextView).setVisibility(((CheckBox)findViewById(R.id.captureWifiCheckbox)).isChecked() ? View.VISIBLE : View.GONE);





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

        TextView settingsLabel = ((TextView)findViewById(R.id.settingsLabel));
        settingsLabel.setTypeface(ubuntuLight);
        boolean settingsDisplayed = sharedPref.getBoolean("SettingsDisplayed", true);
        findViewById(R.id.settingsLinearLayout).setVisibility(settingsDisplayed? View.VISIBLE : View.GONE);
        settingsLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean settingsDisplayed = !sharedPref.getBoolean("SettingsDisplayed", true);
                findViewById(R.id.settingsLinearLayout).setVisibility(settingsDisplayed? View.VISIBLE : View.GONE);
                if (!settingsDisplayed)
                    findViewById(R.id.distributionTabhost).setVisibility(View.GONE);
                sharedPref.edit().putBoolean("SettingsDisplayed", settingsDisplayed).apply();
            }
        });

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
        ((CheckBox)findViewById(R.id.previewSoundCheckbox)).setTypeface(ubuntuLight);
        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.DeleteLogsButton)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.PersistVolumeCheckbox)).setTypeface(ubuntuLight);


        // seek bar listener event for getting the volume's int value
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int realVolume = (int)(MaxVolume * ((float)progress / 100));                                         // calculate it
                sharedPref.edit().putInt(getResources().getString(R.string.VolumePref), progress).apply();                                           // persist it
                ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + progress + "%");                   // label it
                mAudioManager.setStreamVolume(audioStreamType, realVolume, AudioManager.FLAG_VIBRATE);            // bop it
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Setup the log entries slide-up
        ListView logListView = (ListView)findViewById(R.id.logList);
        arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                logArray );
        logListView.setAdapter(arrayAdapter);

        mLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
        mLayout.setPanelHeight(80);
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


        toggleService(appIsOn, false);
        TabHost mTabHost = (TabHost) findViewById(R.id.distributionTabhost);
        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("ChangeOnSound").setIndicator("Change On Sound").setContent(R.id.editSoundsLayout));
        mTabHost.addTab(mTabHost.newTabSpec("ChangeOffSound").setIndicator("Change Off Sound").setContent(R.id.editSoundsLayout));
        mTabHost.setCurrentTab(1);
        mTabHost.setCurrentTab(0);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTypeface(ubuntuMedium);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextSize(15);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTypeface(ubuntuMedium);
        ((TextView)mTabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextSize(15);


        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                TabHost mTabHost = (TabHost) findViewById(R.id.distributionTabhost);
                int otherTab = (mTabHost.getCurrentTab() * -1) + 1;
                mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(getColor(R.color.blueBackground));
                mTabHost.getTabWidget().getChildTabViewAt(otherTab).setBackgroundColor(getColor(R.color.cardview_light_background));
                ((TextView)mTabHost.getTabWidget().getChildTabViewAt(mTabHost.getCurrentTab()).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
                ((TextView)mTabHost.getTabWidget().getChildTabViewAt(otherTab).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));

                int defaultSoundId;
                changeOnSound = tabId.equals("ChangeOnSound");
                if (changeMobileSound)
                {
                    if (changeOnSound)
                    {
                        SelectedSoundType = MOBILE_ON_TYPE;
                        defaultSoundId = R.raw.guitar_riff;
                    }
                    else
                    {
                        SelectedSoundType = MOBILE_OFF_TYPE;
                        defaultSoundId = R.raw.guitar_raff;
                    }

                }
                else
                {
                    if (changeOnSound)
                    {
                        SelectedSoundType = WIFI_ON_TYPE;
                        defaultSoundId = R.raw.affirmative;
                    }
                    else
                    {
                        SelectedSoundType = WIFI_OFF_TYPE;
                        defaultSoundId = R.raw.negative;
                    }
                }
                int selectedSoundId;
                if ((changeOnSound && onSoundIdTemp != -1) || (!changeOnSound && offSoundIdTemp != -1))
                {
                    selectedSoundId = changeOnSound? onSoundIdTemp : offSoundIdTemp;
                }
                else
                {
                    selectedSoundId = sharedPref.getInt(SelectedSoundType, defaultSoundId);
                }


                int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
                if (radioButtonIndex >= soundsArray.size() / 2)
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


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart () {
        super.onStart();
        updateConnectedFlags();
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


    public void setMobileName(String name)
    {
        mobileName = name;
    }
    public String getMobileName()
    {
        String tempName = mobileName;
        mobileName = "unknown";
        return tempName;
    }

    public void setWifiName(String name)
    {
        wifiName = name;
    }
    public String getWifiName()
    {
        String tempName = wifiName;
        wifiName = "unknown";
        return tempName;
    }

    public void addLog(String dateTime, String connInfo)
    {
        LogEntry newLog = new LogEntry(dateTime, connInfo);
        logArray.add(0, newLog);
        logEntryDB.addEntry(newLog);
        arrayAdapter.notifyDataSetChanged();
        try
        {
            FileOutputStream logFileStream = this.openFileOutput(getResources().getString(R.string.ServiceLogFilename), Context.MODE_PRIVATE);
            for (LogEntry s : logArray)
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
        logArray.clear();
        arrayAdapter.notifyDataSetChanged();
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
        // TODO: save changes, close radiogroup linear layout
        findViewById(R.id.distributionTabhost).setVisibility(View.GONE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.VISIBLE);
        onSoundIdTemp = -1;
        offSoundIdTemp = -1;
    }

    public void SaveButtonClicked(View view)
    {
        // TODO: save changes, close radiogroup linear layout
        findViewById(R.id.distributionTabhost).setVisibility(View.GONE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.VISIBLE);

        if (onSoundIdTemp != -1)
        {
            String type = changeMobileSound? MOBILE_ON_TYPE : WIFI_ON_TYPE;
            sharedPref.edit().putInt(type, onSoundIdTemp).apply();
        }
        if (offSoundIdTemp != -1)
        {
            String type = changeMobileSound? MOBILE_OFF_TYPE : WIFI_OFF_TYPE;
            sharedPref.edit().putInt(type, offSoundIdTemp).apply();
        }

        onSoundIdTemp = -1;
        offSoundIdTemp = -1;

    }

    public void ManuallyUnregister(View view)
    {
        Toast toast = new Toast(this);
        toast.makeText(this, "Unregistering...", Toast.LENGTH_SHORT).show();
        try {
            getApplicationContext().unregisterReceiver(receiver);
        }
        catch (Exception e)
        {
            toast.cancel();
            toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void populateRadioGroupLayout()
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


        for (int i = 0; i < soundsArray.size() / 2; i++)
        {
            RadioButton newRadioButton = new RadioButton(this);
            newRadioButton.setButtonTintList(radioButtonColorList);
            newRadioButton.setText(soundsArray.get(i).soundName);
            newRadioButton.setId(soundsArray.get(i).radioButtonIndex);
            leftRadioGroupColumn.addView(newRadioButton);
        }
        for (int i = soundsArray.size() / 2; i < soundsArray.size(); i++)
        {
            RadioButton newRadioButton = new RadioButton(this);
            newRadioButton.setButtonTintList(radioButtonColorList);
            newRadioButton.setText(soundsArray.get(i).soundName);
            newRadioButton.setId(soundsArray.get(i).radioButtonIndex);
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
        if (!val && !((CheckBox)findViewById(R.id.captureWifiCheckbox)).isChecked() && appIsOn)
        {
            appIsOn = false;
            toggleService(false, true);
        }
        else if (!appIsOn && val)
        {
            toggleService(true, true);
        }
    }

    public void CaptureWifiChecked(View view)
    {
        boolean val = ((CheckBox)view).isChecked();
        sharedPref.edit().putBoolean(getResources().getString(R.string.ChangeWifiSoundCheckbox), val).apply();
        (findViewById(R.id.SetWifiSoundsTextView)).setVisibility(val ? View.VISIBLE : View.GONE);
        if (!val && !((CheckBox)findViewById(R.id.captureMobileCheckbox)).isChecked() && appIsOn)
        {
            appIsOn = false;
            toggleService(false, true);
        }
        else if (!appIsOn && val)
        {
            toggleService(true, true);
        }
    }

    public void ChangeMobileSoundsTextClicked(View view)
    {
        findViewById(R.id.distributionTabhost).setVisibility(View.VISIBLE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.GONE);
        TabHost tabHost = ((TabHost)findViewById(R.id.distributionTabhost));
        tabHost.setCurrentTab(0);
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(getColor(R.color.blueBackground));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));
        changeMobileSound = true;
        int selectedSoundId = sharedPref.getInt(MOBILE_ON_TYPE, R.raw.affirmative);
        int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
        if (radioButtonIndex >= soundsArray.size() / 2)
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
        findViewById(R.id.distributionTabhost).setVisibility(View.VISIBLE);
        findViewById(R.id.settingsLinearLayout).setVisibility(View.GONE);
        TabHost tabHost = ((TabHost)findViewById(R.id.distributionTabhost));
        tabHost.setCurrentTab(0);
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(0).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_light_background));
        ((TextView)tabHost.getTabWidget().getChildTabViewAt(1).findViewById(android.R.id.title)).setTextColor(getColor(R.color.cardview_dark_background));
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(getColor(R.color.blueBackground));
        changeMobileSound = false;
        int selectedSoundId = sharedPref.getInt(WIFI_ON_TYPE, R.raw.affirmative);
        int radioButtonIndex = getSoundInfo(selectedSoundId).radioButtonIndex;
        if (radioButtonIndex >= soundsArray.size() / 2)
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


    public void populateSoundsDict()
    {
        soundsArray = new ArrayList<>();
        soundsArray.add(new SoundInfo(R.raw.affirmative, 0, getResources().getString(R.string.Affirmative)));
        soundsArray.add(new SoundInfo(R.raw.negative, 1, getResources().getString(R.string.Negative)));
        soundsArray.add(new SoundInfo(R.raw.guitar_riff, 2, getResources().getString(R.string.GuitarRiff)));
        soundsArray.add(new SoundInfo(R.raw.guitar_raff, 3, getResources().getString(R.string.GuitarRaff)));
        soundsArray.add(new SoundInfo(R.raw.aliens1, 4, getResources().getString(R.string.Aliens1)));
        soundsArray.add(new SoundInfo(R.raw.aliens2, 5, getResources().getString(R.string.Aliens2)));
        soundsArray.add(new SoundInfo(R.raw.tropical, 6, getResources().getString(R.string.Tropical)));
        soundsArray.add(new SoundInfo(R.raw.chime, 7, getResources().getString(R.string.Chime)));
        soundsArray.add(new SoundInfo(R.raw.bell, 8, getResources().getString(R.string.Bell)));
        soundsArray.add(new SoundInfo(R.raw.surprised, 9, getResources().getString(R.string.Surprised)));
        soundsArray.add(new SoundInfo(R.raw.clicks, 10, getResources().getString(R.string.Clicks)));
        soundsArray.add(new SoundInfo(R.raw.slide_down, 11, getResources().getString(R.string.SlideDown)));
    }

    public SoundInfo getSoundInfo(int soundId)
    {
        for (SoundInfo soundInfo : soundsArray)
        {
            if (soundInfo.rawSoundID == soundId)
                return soundInfo;
        }
        return null;
    }

    public SoundInfo getSoundInfoByIndex(int radioButtonIndex)
    {
        for (SoundInfo soundInfo : soundsArray)
        {
            if (soundInfo.radioButtonIndex == radioButtonIndex)
                return soundInfo;
        }
        return null;
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

    public void TimeFormatClicked(View view)
    {
        String key = getResources().getString(R.string.IsTwelveHourBoolean);
        switch (view.getId())
        {
            case R.id.TwelveHour:
                sharedPref.edit().putBoolean(key, true).apply();
                MainActivity.is12HourFormat = true;
                break;
            case (R.id.TwentyFourHour):
                sharedPref.edit().putBoolean(key, false).apply();
                MainActivity.is12HourFormat = false;
                break;
            default:
                Log.d(TAG, "Unknown time format selection made.");
        }
        arrayAdapter.notifyDataSetChanged();
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
        arrayAdapter.notifyDataSetChanged();
    }

    public void PreviewSoundChecked(View view)
    {
        CheckBox checkBox = (CheckBox)view;
        sharedPref.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), checkBox.isChecked()).apply();
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
            case R.id.PreviewSoundHelpButton:
                title = "Preview Alert Sound";
                message = "When changing an alert sound, the selected sound will play once if this option is checked.";
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


    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        boolean wifi = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        boolean mobile = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();


        boolean CheckForMobileConnection = MainActivity.getInstance().sharedPref.getBoolean("ChangeMobileSoundCheckbox", true);
        boolean CheckForWifiConnection = MainActivity.getInstance().sharedPref.getBoolean("ChangeWifiSoundCheckbox", true);

        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
        {
            oldVolume = MainActivity.mAudioManager.getStreamVolume(audioStreamType);
            int volume = MainActivity.getInstance().sharedPref.getInt("VolumePref", 70);
            int realVolume = (int)(MainActivity.MaxVolume * ((float)volume / 100));
            MainActivity.mAudioManager.setStreamVolume(audioStreamType, realVolume, AudioManager.FLAG_VIBRATE);
        }

        if(networkInfo != null)
        {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !wifiConnected && CheckForWifiConnection)
            {

                wifiConnected = true;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_ON_TYPE, R.raw.affirmative);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                            MainActivity.mAudioManager.setStreamVolume(audioStreamType, oldVolume, AudioManager.FLAG_VIBRATE);
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                WifiInfo info = MainActivity.getInstance().wifiManager.getConnectionInfo();
                String ssid  = info.getSSID();
                MainActivity.getInstance().setWifiName(ssid);

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

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_ON_TYPE, R.raw.guitar_riff);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                            MainActivity.mAudioManager.setStreamVolume(audioStreamType, oldVolume, AudioManager.FLAG_VIBRATE);
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                MainActivity.getInstance().setMobileName((networkInfo.getExtraInfo() == null ? "unknown" : networkInfo.getExtraInfo()));

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

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_OFF_TYPE, R.raw.negative);

                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                            MainActivity.mAudioManager.setStreamVolume(audioStreamType, oldVolume, AudioManager.FLAG_VIBRATE);
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());

                String wifiName = MainActivity.getInstance().getWifiName();
                String connInfo = "Wifi disconnected from " + wifiName;
                MainActivity.getInstance().addLog(logDatetime, connInfo);
                if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                    Toast.makeText(context, connInfo, Toast.LENGTH_SHORT).show();
            }
            else if(!mobile && mobileConnected && CheckForMobileConnection)
            {

                mobileConnected = false;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_OFF_TYPE, R.raw.guitar_raff);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (MainActivity.getInstance().sharedPref.getBoolean("PersistAlertVolume", false))
                            MainActivity.mAudioManager.setStreamVolume(audioStreamType, oldVolume, AudioManager.FLAG_VIBRATE);
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());

                String mobileName = MainActivity.getInstance().getMobileName();
                String connInfo = "Mobile disconnected from " + mobileName;
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
            String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";


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