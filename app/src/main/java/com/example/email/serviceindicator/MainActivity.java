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
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.email.serviceindicator.MainActivity.logArray;
import static com.example.email.serviceindicator.MainActivity.mediaPlayer;
import static com.example.email.serviceindicator.MainActivity.mobileConnected;


import static com.example.email.serviceindicator.MainActivity.wifiConnected;
import static com.example.email.serviceindicator.ServiceIndicatorWidget.WidgetButtonTag;

class LogEntry
{
    boolean is12HourFormat;
    boolean isMMDDFormat;
    public String date;
    public String time;
    public String connectionInfo;

    public LogEntry(String date, boolean isMMDD, String time, boolean is12Hour, String connInfo)
    {
        this.date = date;
        this.time = time;
        this.connectionInfo = connInfo;
        this.isMMDDFormat = isMMDD;
        this.is12HourFormat = is12Hour;
    }

    @Override
    public String toString()
    {
        StringBuilder tempOutputString = new StringBuilder();
        try
        {
            String dateFormat = isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
            DateFormat df = new SimpleDateFormat(dateFormat);

            String timeFormat = is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
            DateFormat tf = new SimpleDateFormat(timeFormat);

            Date tempDate = df.parse(this.date);
            Date tempTime = tf.parse(this.time);

            String outputDateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
            DateFormat outputDate = new SimpleDateFormat(outputDateFormat);

            String outputTimeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
            DateFormat outputTime = new SimpleDateFormat(outputTimeFormat);

            tempOutputString.append(outputDate.format(tempDate)).append(" - ");
            tempOutputString.append(outputTime.format(tempTime)).append("\n");
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

    // Whether there is a Wi-Fi connection.
    public static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    public static boolean mobileConnected = false;

    public static ArrayList<LogEntry> logArray = new ArrayList<>();
    ArrayAdapter<LogEntry> arrayAdapter;
    public static MainActivity MA;
    private SlidingUpPanelLayout mLayout;
    // The BroadcastReceiver that tracks network connectivity changes.
    public static NetworkReceiver receiver = new NetworkReceiver();

    public static boolean is12HourFormat;
    public static boolean isMMDDFormat;

    public static String mobileName = "unknown";
    public static String wifiName = "unknown";

    public static IntentFilter filter;
    private AudioManager mAudioManager;
    public static MediaPlayer mediaPlayer = new MediaPlayer();

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

    public void addLog(String date, boolean isMMDD, String time, boolean is12Hour, String connInfo)
    {
        LogEntry newLog = new LogEntry(date, isMMDD, time, is12Hour, connInfo);
        logArray.add(newLog);
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

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        CheckBox appOnCheckbox = (CheckBox) findViewById(R.id.checkBox);
        appOnCheckbox.setChecked(sharedPref.getBoolean(getResources().getString(R.string.AppOnPref), true));
        SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeBar);
        volumeSlider.setProgress(sharedPref.getInt(getResources().getString(R.string.VolumePref), 70));
        ((TextView)findViewById(R.id.volumeLabel)).setText("Volume: " + volumeSlider.getProgress() + "%");
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeSlider.getProgress(), AudioManager.FLAG_VIBRATE);
        is12HourFormat = sharedPref.getBoolean(getResources().getString(R.string.HourFormatPref), true);
        isMMDDFormat = sharedPref.getBoolean(getResources().getString(R.string.DateFormatPref), true);



        try
        {
            // clear logs
//            FileOutputStream temp = this.openFileOutput(getResources().getString(R.string.ServiceLogFilename), Context.MODE_PRIVATE);
//            temp.write("".getBytes());
//            temp.close();

            FileInputStream logFileStream = this.openFileInput(getResources().getString(R.string.ServiceLogFilename));
            BufferedReader reader = new BufferedReader(new InputStreamReader(logFileStream));
            String logDateTimeLine = reader.readLine();
            while (logDateTimeLine != null)
            {
//                String logContentLine = reader.readLine();
//                logArray.add(logDateTimeLine + "\n" + logContentLine);
                System.out.println(logDateTimeLine);
                logDateTimeLine = reader.readLine();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        // perform seek bar change listener event used for getting the progress value
        volumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
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
                Toast.makeText(MainActivity.this, "onItemClick", Toast.LENGTH_SHORT).show();
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
        switch (item.getItemId()){

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
        boolean isOn = ((CheckBox)view).isChecked();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean("AppOn", isOn).apply();
        toggleService(isOn);
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
                mediaPlayer = MediaPlayer.create(context, R.raw.wifion1);
                mediaPlayer.start();
                MainActivity.getInstance().setWifiName(networkInfo.getExtraInfo());
                String dateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
                DateFormat date = new SimpleDateFormat(dateFormat);

                String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
                DateFormat time = new SimpleDateFormat(timeFormat);

                String logDate = date.format(Calendar.getInstance().getTime());
                String logTime = time.format(Calendar.getInstance().getTime());
                String connInfo = "Wifi connected to " + networkInfo.getExtraInfo();
                MainActivity.getInstance().addLog(logDate, MainActivity.isMMDDFormat, logTime, MainActivity.is12HourFormat, connInfo);

            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnected) {
                mobileConnected = true;
                Toast.makeText(context, "Mobile Connected", Toast.LENGTH_SHORT).show();
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.mobileon1);
                mediaPlayer.start();
                MainActivity.getInstance().setMobileName(networkInfo.getExtraInfo());

                String dateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
                DateFormat date = new SimpleDateFormat(dateFormat);

                String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
                DateFormat time = new SimpleDateFormat(timeFormat);

                String logDate = date.format(Calendar.getInstance().getTime());
                String logTime = time.format(Calendar.getInstance().getTime());
                String connInfo = "Mobile connected to " + networkInfo.getExtraInfo();
                MainActivity.getInstance().addLog(logDate, MainActivity.isMMDDFormat, logTime, MainActivity.is12HourFormat, connInfo);
            }
        }
        else
        {
            if (!wifi && wifiConnected)
            {
                Toast.makeText(context, "WIFI Disconnected", Toast.LENGTH_SHORT).show();
                wifiConnected = false;
                mediaPlayer = MediaPlayer.create(context, R.raw.wifioff1);
                mediaPlayer.start();
                String dateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
                DateFormat date = new SimpleDateFormat(dateFormat);

                String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
                DateFormat time = new SimpleDateFormat(timeFormat);

                String logDate = date.format(Calendar.getInstance().getTime());
                String logTime = time.format(Calendar.getInstance().getTime());
                String connInfo = "Wifi disconnected from " + MainActivity.getInstance().getWifiName();
                MainActivity.getInstance().addLog(logDate, MainActivity.isMMDDFormat, logTime, MainActivity.is12HourFormat, connInfo);
            }
            else if(!mobile && mobileConnected)
            {
                Toast.makeText(context, "Mobile Disconnected", Toast.LENGTH_SHORT).show();
                mobileConnected = false;
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.mobileoff1);
                mediaPlayer.start();
                String dateFormat = MainActivity.isMMDDFormat ? "MM/dd/yyyy" : "dd/MM/yyyy";
                DateFormat date = new SimpleDateFormat(dateFormat);

                String timeFormat = MainActivity.is12HourFormat ? "hh:mm:ss a" : "HH:mm:ss";
                DateFormat time = new SimpleDateFormat(timeFormat);

                String logDate = date.format(Calendar.getInstance().getTime());
                String logTime = time.format(Calendar.getInstance().getTime());
                String connInfo = "Mobile disconnected from " + MainActivity.getInstance().getMobileName();
                MainActivity.getInstance().addLog(logDate, MainActivity.isMMDDFormat, logTime, MainActivity.is12HourFormat, connInfo);
            }
        }

    }
}
