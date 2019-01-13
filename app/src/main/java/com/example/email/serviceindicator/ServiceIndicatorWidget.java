package com.example.email.serviceindicator;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.Toast;


/**
 * Implementation of App Widget functionality.
 */
public class ServiceIndicatorWidget extends AppWidgetProvider {

    public static final String WidgetButtonTag = "widgetButtonTag";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.service_indicator_widget);

        Intent intentUpdate = new Intent(context, ServiceIndicatorWidget.class);
        intentUpdate.setAction(WidgetButtonTag);
        int[] idArray = new int[]{appWidgetId};
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        PendingIntent pendingUpdate = PendingIntent.getBroadcast(context, appWidgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.button_update, pendingUpdate);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            Toast.makeText(context, "Widget has been updated! ", Toast.LENGTH_SHORT).show();
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }


    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (WidgetButtonTag.equals(intent.getAction())) {
            MainActivity ma = MainActivity.getInstance();
            CheckBox checkBox = ((Activity)ma).findViewById(R.id.checkBox);
            checkBox.setChecked(!checkBox.isChecked());
            ma.toggleService(checkBox.isChecked());

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.service_indicator_widget);
            String buttonText = ((CheckBox)MainActivity.getInstance().findViewById(R.id.checkBox)).isChecked() ?
                    "X" : "0";

            views.setTextViewText(R.id.button_update, buttonText);
            for (int id : AppWidgetManager.getInstance(context).getAppWidgetIds(intent.getComponent()))
            {
                AppWidgetManager.getInstance(context).updateAppWidget(id, views);
            }

        }
    }
}
