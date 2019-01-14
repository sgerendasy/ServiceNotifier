package com.example.email.serviceindicator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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
import java.util.HashMap;

import static com.example.email.serviceindicator.MainActivity.MOBILE_OFF_TYPE;
import static com.example.email.serviceindicator.MainActivity.MOBILE_ON_TYPE;
import static com.example.email.serviceindicator.MainActivity.WIFI_OFF_TYPE;
import static com.example.email.serviceindicator.MainActivity.WIFI_ON_TYPE;
import static com.example.email.serviceindicator.MainActivity.logEntryDateTimeFormat;
import static com.example.email.serviceindicator.MainActivity.mediaPlayer;
import static com.example.email.serviceindicator.MainActivity.mobileConnected;
import static com.example.email.serviceindicator.MainActivity.wifiConnected;

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
            String dateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
            String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";

            DateFormat datetimeFormat = new SimpleDateFormat(MainActivity.logEntryDateTimeFormat);
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

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MAIN_ACTIVITY";
    // Whether there is a Wi-Fi connection.
    public static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    public static boolean mobileConnected = false;

    public static final String logEntryDateTimeFormat = "yyyy-MM-dd HH:mm:ss";

    static final HashMap<String, Integer> soundTypeTable = new HashMap<>();
    static final HashMap<Integer, Integer> soundIdToButtonId = new HashMap<>();

    public static ArrayList<LogEntry> logArray = new ArrayList<>();
    ArrayAdapter<LogEntry> arrayAdapter;

    public static HashMap<String, Integer> soundsDict;
    public static String currentSoundTypeTab = "Mobile On";

    public static MainActivity MA;
    private SlidingUpPanelLayout mLayout;
    // The BroadcastReceiver that tracks network connectivity changes.
    public static NetworkReceiver receiver = new NetworkReceiver();

    public SharedPreferences sharedPref;
    public static boolean is12HourFormat;
    public static boolean isMMDDFormat;

    public static String mobileName = "unknown";
    public static String wifiName = "unknown";

    public static final String MOBILE_ON_TYPE = "Mobile On";
    public static final String MOBILE_OFF_TYPE = "Mobile Off";
    public static final String WIFI_ON_TYPE = "Wifi On";
    public static final String WIFI_OFF_TYPE = "Wifi Off";

    public static IntentFilter filter;
    private AudioManager mAudioManager;
    public static MediaPlayer mediaPlayer = new MediaPlayer();
    private LogEntrySQL logEntryDB;

    public static MainActivity getInstance()
    {
        return MA;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Registers BroadcastReceiver to track network connection changes.
        filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        MA = this;

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckBox appOnCheckbox = (CheckBox) findViewById(R.id.checkBox);
        appOnCheckbox.setChecked(sharedPref.getBoolean(getResources().getString(R.string.AppOnPref), true));
        SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeBar);
        volumeSlider.setProgress(sharedPref.getInt(getResources().getString(R.string.VolumePref), 70));
        ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + volumeSlider.getProgress() + "%");
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeSlider.getProgress(), AudioManager.FLAG_VIBRATE);
        is12HourFormat = sharedPref.getBoolean(getResources().getString(R.string.IsTwelveHourBoolean), true);
        isMMDDFormat = sharedPref.getBoolean(getResources().getString(R.string.ISMMDDFormatBoolean), true);

        logEntryDB = new LogEntrySQL(this);
        logArray = logEntryDB.getLogs(sharedPref.getInt(getResources().getString(R.string.SaveLogsValue), 10));

        populateSoundsDict();

        // perform seek bar change listener event used for getting the progress value
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                sharedPref.edit().putInt(getResources().getString(R.string.VolumePref), progress).apply();
                ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + progress + "%");
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_VIBRATE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        ListView lv = (ListView) findViewById(R.id.list);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // handle on log item clicked
            }
        });


        arrayAdapter = new ArrayAdapter<LogEntry>(
                this,
                android.R.layout.simple_list_item_1,
                logArray );
        lv.setAdapter(arrayAdapter);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setPanelHeight(100);

        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        TabLayout soundTypeTabLayout = (TabLayout) findViewById(R.id.ChooseSoundTabLayout);
        soundTypeTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setCurrentTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        this.getApplicationContext().registerReceiver(receiver, filter);
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
        if (item.getTitle().toString().equals(getResources().getString(R.string.SettingsLabel)))
        {
            Intent settingsIntent = new Intent(this, Settings.class);
            startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
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
        logEntryDB.getLogs(10);
        boolean isOn = ((CheckBox)view).isChecked();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("AppOn", isOn).apply();
        toggleService(isOn);
    }

    public void populateSoundsDict()
    {
        soundTypeTable.put(MOBILE_ON_TYPE, sharedPref.getInt(MOBILE_ON_TYPE, R.raw.on_mobile));
        soundTypeTable.put(MOBILE_OFF_TYPE, sharedPref.getInt(MOBILE_OFF_TYPE, R.raw.off_mobile));
        soundTypeTable.put(WIFI_ON_TYPE, sharedPref.getInt(WIFI_ON_TYPE, R.raw.on_wifi));
        soundTypeTable.put(WIFI_OFF_TYPE, sharedPref.getInt(WIFI_OFF_TYPE, R.raw.off_wifi));

        soundsDict = new HashMap<>();
        soundsDict.put(getResources().getString(R.string.MobileOnSound), R.raw.on_mobile);
        soundsDict.put(getResources().getString(R.string.MobileOffSound), R.raw.off_mobile);
        soundsDict.put(getResources().getString(R.string.WifiOnSound), R.raw.on_wifi);
        soundsDict.put(getResources().getString(R.string.WifiOffSound), R.raw.off_wifi);
        soundsDict.put(getResources().getString(R.string.BirdChirpsUp), R.raw.bird_chirps_up);
        soundsDict.put(getResources().getString(R.string.BirdChirpsDown), R.raw.bird_chirps_down);
        soundsDict.put(getResources().getString(R.string.HighBlip), R.raw.high_blip);
        soundsDict.put(getResources().getString(R.string.LowBlip), R.raw.low_blip);
        soundsDict.put(getResources().getString(R.string.DoubleHighBlip), R.raw.double_high_blip);
        soundsDict.put(getResources().getString(R.string.DoubleLowBlip), R.raw.double_low_blip);

        soundIdToButtonId.put(R.raw.on_mobile, R.id.SOUNDMobileOnButton);
        soundIdToButtonId.put(R.raw.off_mobile, R.id.SOUNDMobileOffButton);
        soundIdToButtonId.put(R.raw.on_wifi, R.id.SOUNDWifiOnButton);
        soundIdToButtonId.put(R.raw.off_wifi, R.id.SOUNDWifiOffButton);
        soundIdToButtonId.put(R.raw.bird_chirps_up, R.id.SOUNDBirdChirpsUp);
        soundIdToButtonId.put(R.raw.bird_chirps_down, R.id.SOUNDBirdChirpsDown);
        soundIdToButtonId.put(R.raw.high_blip, R.id.SOUNDHighBlip);
        soundIdToButtonId.put(R.raw.low_blip, R.id.SOUNDLowBlip);
        soundIdToButtonId.put(R.raw.double_high_blip, R.id.SOUNDDoubleHighBlip);
        soundIdToButtonId.put(R.raw.double_low_blip, R.id.SOUNDDoubleLowBlip);

        TabLayout soundsTabLayout = (TabLayout) findViewById(R.id.ChooseSoundTabLayout);
        setCurrentTab(soundsTabLayout.getSelectedTabPosition());

    }

    public void setCurrentTab(int index)
    {
        int defaultSound = 0;
        switch(index)
        {
            case 0:
                currentSoundTypeTab = MOBILE_ON_TYPE;
                defaultSound = R.raw.on_mobile;
                break;
            case 1:
                currentSoundTypeTab = MOBILE_OFF_TYPE;
                defaultSound = R.raw.off_mobile;
                break;
            case 2:
                currentSoundTypeTab = WIFI_ON_TYPE;
                defaultSound = R.raw.on_wifi;
                break;
            case 3:
                currentSoundTypeTab = WIFI_OFF_TYPE;
                defaultSound = R.raw.off_wifi;
                break;
        }
        RadioGroup soundsGroup = (RadioGroup) findViewById(R.id.soundSelectionGroup);
        soundsGroup.check(soundIdToButtonId.get(sharedPref.getInt(currentSoundTypeTab, defaultSound)));
    }

    public void SoundSelected(View view)
    {
        String selectedText = ((RadioButton)view).getText().toString();
        int selectedSoundId = soundsDict.get(selectedText);
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
        sharedPref.edit().putInt(currentSoundTypeTab, selectedSoundId).apply();
    }

    public void toggleService(boolean on)
    {
        try
        {
            if (on)
            {
                Toast.makeText(getApplicationContext(), "Service on", Toast.LENGTH_SHORT).show();
                this.getApplicationContext().registerReceiver(receiver, filter);
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Service off", Toast.LENGTH_SHORT).show();
                this.getApplicationContext().unregisterReceiver(receiver);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}


class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        boolean wifi = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        boolean mobile = conn.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();

        if(networkInfo != null)
        {

            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && !wifiConnected)
            {
                Toast.makeText(context, "WIFI Connected", Toast.LENGTH_SHORT).show();
                wifiConnected = true;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_ON_TYPE, R.raw.on_wifi);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.start();
                MainActivity.getInstance().setWifiName(networkInfo.getExtraInfo());

                DateFormat datetimeFormat = new SimpleDateFormat(logEntryDateTimeFormat);
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Wifi connected to " + networkInfo.getExtraInfo();
                MainActivity.getInstance().addLog(logDatetime, connInfo);

            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnected) {
                mobileConnected = true;
                Toast.makeText(context, "Mobile Connected", Toast.LENGTH_SHORT).show();

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_ON_TYPE, R.raw.on_mobile);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.start();
                MainActivity.getInstance().setMobileName(networkInfo.getExtraInfo());

                DateFormat datetimeFormat = new SimpleDateFormat(logEntryDateTimeFormat);
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Mobile connected to " + networkInfo.getExtraInfo();
                MainActivity.getInstance().addLog(logDatetime, connInfo);
            }
        }
        else
        {
            if (!wifi && wifiConnected)
            {
                Toast.makeText(context, "WIFI Disconnected", Toast.LENGTH_SHORT).show();
                wifiConnected = false;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(WIFI_OFF_TYPE, R.raw.off_wifi);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat(logEntryDateTimeFormat);
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Wifi disconnected from " + MainActivity.getInstance().getWifiName();
                MainActivity.getInstance().addLog(logDatetime, connInfo);
            }
            else if(!mobile && mobileConnected)
            {
                Toast.makeText(context, "Mobile Disconnected", Toast.LENGTH_SHORT).show();
                mobileConnected = false;

                int currentSoundId = MainActivity.getInstance().sharedPref.getInt(MOBILE_OFF_TYPE, R.raw.off_mobile);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentSoundId);
                mediaPlayer.start();

                DateFormat datetimeFormat = new SimpleDateFormat(logEntryDateTimeFormat);
                String logDatetime = datetimeFormat.format(Calendar.getInstance().getTime());
                String connInfo = "Mobile disconnected from " + MainActivity.getInstance().getMobileName();
                MainActivity.getInstance().addLog(logDatetime, connInfo);
            }
        }

    }
}