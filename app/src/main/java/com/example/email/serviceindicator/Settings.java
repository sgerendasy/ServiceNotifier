package com.example.email.serviceindicator;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

public class Settings extends AppCompatActivity {
    private static final String TAG = "SETTINGS";
    public SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        CheckBox previewAlertCheckbox = (CheckBox) findViewById(R.id.previewSoundCheckbox);
        previewAlertCheckbox.setChecked(sharedPreferences.getBoolean(getResources().getString(R.string.PreviewSoundBoolean), true));

        boolean is12HourChecked = sharedPreferences.getBoolean(getResources().getString(R.string.IsTwelveHourBoolean), true);
        if (is12HourChecked)
            ((RadioButton)findViewById(R.id.TwelveHour)).setChecked(true);
        else
            ((RadioButton)findViewById(R.id.TwentyFourHour)).setChecked(true);

        boolean isMMDDChecked = sharedPreferences.getBoolean(getResources().getString(R.string.ISMMDDFormatBoolean), true);
        if (isMMDDChecked)
            ((RadioButton)findViewById(R.id.MMDDYYYY)).setChecked(true);
        else
            ((RadioButton)findViewById(R.id.DDMMYYYY)).setChecked(true);

        int numberOfLogsToSave = sharedPreferences.getInt(getResources().getString(R.string.SaveLogsValue), 10);
        switch (numberOfLogsToSave)
        {
            case -1:
                ((RadioButton)findViewById(R.id.InfiniteLogsRadioButton)).setChecked(true);
                break;
            case 10:
                ((RadioButton)findViewById(R.id.TenLogsRadioButton)).setChecked(true);
                break;
            case 50:
                ((RadioButton)findViewById(R.id.FiftyLogsRadioButton)).setChecked(true);
                break;
            case 100:
                ((RadioButton)findViewById(R.id.OneHundredLogsRadioButton)).setChecked(true);
                break;
            default:
                Log.d(TAG, "Unknown selection made for number of logs to save.");
        }
    }

    public void TimeFormatClicked(View view)
    {
        String key = getResources().getString(R.string.IsTwelveHourBoolean);
        switch (view.getId())
        {
            case R.id.TwelveHour:
                sharedPreferences.edit().putBoolean(key, true).apply();
                MainActivity.is12HourFormat = true;
                break;
            case (R.id.TwentyFourHour):
                sharedPreferences.edit().putBoolean(key, false).apply();
                MainActivity.is12HourFormat = false;
                break;
            default:
                Log.d(TAG, "Unknown time format selection made.");
        }
    }

    public void DateFormatClicked(View view)
    {
        String key = getResources().getString(R.string.ISMMDDFormatBoolean);
        switch (view.getId())
        {
            case R.id.MMDDYYYY:
                sharedPreferences.edit().putBoolean(key, true).apply();
                MainActivity.isMMDDFormat = true;
                break;
            case (R.id.DDMMYYYY):
                sharedPreferences.edit().putBoolean(key, false).apply();
                MainActivity.isMMDDFormat = false;
                break;
            default:
                Log.d(TAG, "Unknown date format selection made.");
        }
    }

    public void PreviewSoundChecked(View view)
    {
        CheckBox checkBox = (CheckBox)view;
        sharedPreferences.edit().putBoolean(getResources().getString(R.string.PreviewSoundBoolean), checkBox.isChecked()).apply();
    }

    public void EnableToastChecked(View view)
    {
        CheckBox checkBox = (CheckBox)view;
        sharedPreferences.edit().putBoolean(getResources().getString(R.string.EnableToastBoolean), checkBox.isChecked()).apply();
    }

    public void LogNumberSelected(View view)
    {
        String key = getResources().getString(R.string.SaveLogsValue);
        switch (view.getId())
        {
            case R.id.TenLogsRadioButton:
                sharedPreferences.edit().putInt(key, 10).apply();
                break;
            case (R.id.FiftyLogsRadioButton):
                sharedPreferences.edit().putInt(key, 50).apply();
                break;
            case (R.id.OneHundredLogsRadioButton):
                sharedPreferences.edit().putInt(key, 100).apply();
                break;
            case (R.id.InfiniteLogsRadioButton):
                sharedPreferences.edit().putInt(key, -1).apply();
                break;
            default:
                Log.d(TAG, "Unknown date format selection made");
        }
    }
}
