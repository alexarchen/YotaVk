package com.zakharchenko.yotavk;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by zakharchenko on 05.04.2016.
 */
public class BSWidgetLarge extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        BSWidget.UpdateAllWidgets(context);



    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        context.startService(new Intent("UPDATE", null, context, BSWidget.WidgetService.class));

        Log.d("WIDGET", "onReceive " + intent.getAction());

        if (intent.getAction().equals("com.yotadevices.yotaphone.action.APPWIDGET_VISIBILITY_CHANGED")) {
            //            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            //            appWidgetManager.updateAppWidget(new ComponentName(context, BSWidget.class),views);

        }
        //else
        BSWidget.UpdateAllWidgets(context);

    }


}
