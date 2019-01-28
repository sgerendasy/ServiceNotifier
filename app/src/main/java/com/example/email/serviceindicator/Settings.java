package com.example.email.serviceindicator;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {
    private static final String TAG = "SETTINGS";
    public SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set persistent data values
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        ((CheckBox)findViewById(R.id.previewSoundCheckbox)).setChecked(sharedPreferences.getBoolean(getResources().getString(R.string.PreviewSoundBoolean), true));
        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setChecked(sharedPreferences.getBoolean(getResources().getString(R.string.EnableToastBoolean), false));
        ((CheckBox)findViewById(R.id.PersistVolumeCheckbox)).setChecked(sharedPreferences.getBoolean(getResources().getString(R.string.PersistAlertVolume), false));

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
            case 0:
                ((RadioButton)findViewById(R.id.ZeroLogsRadioButton)).setChecked(true);
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

        String serviceType = sharedPreferences.getString(getResources().getString(R.string.ServiceTypeToCheckFor), getResources().getString(R.string.BothServiceTypes));
        switch (serviceType)
        {
            case "BothServiceTypes":
                ((RadioButton)findViewById(R.id.BothServiceTypes)).setChecked(true);
                break;
            case "MobileOnly":
                ((RadioButton)findViewById(R.id.MobileOnly)).setChecked(true);
                break;
            case "WifiOnly":
                ((RadioButton)findViewById(R.id.WifiOnly)).setChecked(true);
                break;
        }

        // Set Fonts
        Typeface ubuntuLight = Typeface.createFromAsset(getAssets(), "fonts/Ubuntu-L.ttf");
        Typeface ubuntuMedium = Typeface.createFromAsset(getAssets(), "fonts/Ubuntu-M.ttf");

        ((TextView)findViewById(R.id.ServiceTypeLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.BothServiceTypes)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.MobileOnly)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.WifiOnly)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.LogTimeFormatLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.TwelveHour)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.TwentyFourHour)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.LogDateFormatLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.DDMMYYYY)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.MMDDYYYY)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.NumberOfLogsLabel)).setTypeface(ubuntuMedium);
        ((RadioButton)findViewById(R.id.ZeroLogsRadioButton)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.TenLogsRadioButton)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.FiftyLogsRadioButton)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.OneHundredLogsRadioButton)).setTypeface(ubuntuLight);
        ((RadioButton)findViewById(R.id.InfiniteLogsRadioButton)).setTypeface(ubuntuLight);

        ((TextView)findViewById(R.id.OtherSettingsLabel)).setTypeface(ubuntuMedium);
        ((CheckBox)findViewById(R.id.previewSoundCheckbox)).setTypeface(ubuntuLight);
        ((CheckBox)findViewById(R.id.enableToastCheckbox)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.DeleteLogsButton)).setTypeface(ubuntuLight);
        ((Button)findViewById(R.id.PersistVolumeCheckbox)).setTypeface(ubuntuLight);
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
            case R.id.ZeroLogsRadioButton:
                sharedPreferences.edit().putInt(key, 0).apply();
                break;
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

    public void EnablePersistentVolumeChecked(View view)
    {
        boolean value = ((CheckBox)view).isChecked();
        sharedPreferences.edit().putBoolean(getResources().getString(R.string.PersistAlertVolume), value).apply();
    }

    public void DeleteLogsClicked(View view)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage("You sure there pal?")
                .setPositiveButton("I'm not your pal, guy.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int deletedLogsCount = MainActivity.getInstance().ClearLogs();
                        if (MainActivity.getInstance().sharedPref.getBoolean("EnableToastBoolean", true))
                        {
                            String logsStringButMaybePlural = " Logs.";
                            if (deletedLogsCount == 1)
                            {
                                logsStringButMaybePlural = " Log.";
                            }
                            Toast.makeText(Settings.this, "Deleted all " + deletedLogsCount + logsStringButMaybePlural , Toast.LENGTH_SHORT).show();
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

    public void ChangeServiceClicked(View view)
    {
        switch (view.getId())
        {
            case R.id.BothServiceTypes:
                sharedPreferences.edit().putString(getResources().getString(R.string.ServiceTypeToCheckFor), getResources().getString(R.string.BothServiceTypes)).apply();
                break;
            case R.id.MobileOnly:
                sharedPreferences.edit().putString(getResources().getString(R.string.ServiceTypeToCheckFor), getResources().getString(R.string.MobileOnly)).apply();
                break;
            case R.id.WifiOnly:
                sharedPreferences.edit().putString(getResources().getString(R.string.ServiceTypeToCheckFor), getResources().getString(R.string.WifiOnly)).apply();
                break;
        }
    }

    public void HelpButtonPressed(View view)
    {
        int helpButtonId = view.getId();
        String message = "";
        String title = "";
        DialogInterface.OnClickListener neutralButtonListener = null;
        String neutralButtonText = "";
        switch (helpButtonId)
        {
            case R.id.PersistAlertVolumeHelpButton:
                title = "Persist Alert Volume";
                message = "When you adjust the media volume output on your phone by using either the volume buttons on the side of your phone, or through another app, it affects the volume level of the alerts played though this app. By checking this option, the media volume output will play alerts at the volume level set from the home screen regardless of what volume level you or other apps have set.";
                break;
            case R.id.PreviewSoundHelpButton:
                title = "Preview Alert Sound";
                message = "Checking this option has the system preview an alert sound when it is selected from the \"Set Alert Sounds\" menu.";
                break;
            case R.id.EnableToastHelpButton:
                title = "Enable Toast Notifications";
                message = "Checking this option enables Toast notifications for a service change event. To see what a Toast message is, press \"Toast me\".";
                neutralButtonText = "Toast me";
                neutralButtonListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(Settings.this, "This is a Toast message", Toast.LENGTH_SHORT).show();
                    }
                };
                break;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message).setNeutralButton(neutralButtonText, neutralButtonListener)
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
