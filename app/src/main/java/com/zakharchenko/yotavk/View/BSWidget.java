package com.zakharchenko.yotavk.View;

import android.app.Application;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPoll;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.yotadevices.sdk.BackscreenLauncherConstants;
import com.yotadevices.sdk.Drawer;
import com.yotadevices.sdk.EpdIntentCompat;
import com.yotadevices.sdk.utils.EinkUtils;
import com.yotadevices.sdk.utils.RotationAlgorithm;
import com.zakharchenko.yotavk.GCM.RegistrationIntentService;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.Presenter.ChatsPresenter;
import com.zakharchenko.yotavk.Presenter.NotificationPresenter;
import com.zakharchenko.yotavk.Presenter.Presenter;

import com.zakharchenko.yotavk.R;

import com.zakharchenko.yotavk.Utils.Utils;

import org.apache.http.HttpConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zakharchenko on 18.01.2016.
 */


// extra_large widget

public class BSWidget extends AppWidgetProvider{

  // this service updates messages and send broadcast
 public static class WidgetService extends NotificationListenerService  implements ChatsPresenter.Listener{
/*
    public WidgetService(){
        super("WigetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction()=="UPDATE_MSG"){

            MyApplication.GetMessages(((MyApplication)getApplication()).defListener);

        }
    }*/

/*

      String LongPollKey;
      String LongPollServer;
      int LongPollTS;
      Thread LongPollThread=null;

      boolean bLongPoll = false;
      void startLongPoll(){
       if (!bLongPoll){

           if (VKSdk.isLoggedIn()){

             bLongPoll = true;

             VKRequest req = new VKRequest("messages.getLongPollServer");
               req.executeWithListener(new VKRequest.VKRequestListener() {
                   @Override
                   public void onComplete(VKResponse response) {
                       super.onComplete(response);

                       try {
                           JSONObject obj = response.json.getJSONObject("response");
                           LongPollKey = obj.getString("key");
                           LongPollServer = obj.getString("server");
                           LongPollTS = obj.getInt("ts");

                           Log.d("LONGPOLL","Get: "+response.json.toString());

                           if (LongPollThread==null)
                           LongPollThread = new Thread(
                           new Runnable(){
                               @Override
                               public void run() {

                                   while (bLongPoll) {
                                       if (LongPollKey.length()>0) {
                                           try {
                                               URL url = new URL("http://" + LongPollServer + "?act=a_check&key="+LongPollKey+"&ts="+LongPollTS+"&wait=25&mode=0");
                                               HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                               try {
                                                   InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                                                   String str = "";
                                                   byte buffer[] = new byte[1024];
                                                   while (in.read(buffer)!=-1){
                                                       str+=buffer;
                                                   }
                                                   str+=buffer;

                                                   Log.d("LONGPOLL","Request: "+str);

                                                   JSONObject obj = new JSONObject(str);
                                                   if (obj.getInt("failed")!=0){
                                                       //
                                                       Log.i("LONGPOLL","Return failed: "+str);
                                                       bLongPoll = false;
                                                       startLongPoll();
                                                   }
                                                   else{

                                                       JSONArray upd = obj.getJSONArray("updates");
                                                       for (int q=0;q<upd.length();q++)
                                                         if (upd.getJSONArray(q).get(0).

                                                   }



                                               } catch (Exception e) {
                                                  wait(1000);
                                               } finally {
                                                   urlConnection.disconnect();
                                               }
                                           } catch (Exception e) {


                                           }
                                       }
                                   }
                               }
                           });

                           LongPollThread.start();


                       }
                       catch (Exception e){}


                   }

                   @Override
                   public void onError(VKError error) {

                       super.onError(error);

                       bLongPoll = false;
                       Log.i("VKWIDGET", "Error longpoll: " + error);
                   }
               });


           }



       }

      }
*/

      @Override
      public void onNotificationRemoved(StatusBarNotification sbn,RankingMap map) {
          super.onNotificationPosted(sbn, map);

          if (sbn.getPackageName().equals("com.vkontakte.android")) {

             presenter.Refresh();

          }

      }

      @Override
      public void onNotificationRemoved(StatusBarNotification sbn) {

          super.onNotificationRemoved(sbn);

          if (sbn.getPackageName().equals(getPackageName())){

              MyApplication.NotificationsRemoved(sbn.getId());
          }

          if (sbn.getPackageName().equals("com.vkontakte.android")) {

              // maybe read
              presenter.Refresh();

          }


      }

      final BroadcastReceiver bReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              // if(intent.getAction().equals(MyApplication.VKRECORDSCHANGED_BROADCAST)) {
              ///onUpdate(context,null,null);
              Log.d("WIDSERV", "Recived " + intent.getAction());
              if (intent.getAction().equals("android.intent.action.SCREEN_ON")){
               // detect if VK is in top, so delete system notifications
              /*
               if (MyGcmListenerService.isVKRunning(context))
               {

                   NotificationManager mNotificationManager =
                           (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                   mNotificationManager.cancelAll();

               }*/

              }
              else
              if (intent.getAction().equals("android.intent.action.SCREEN_OFF"))
              {
                //  presenter.Refresh();

              }
              else
              UpdateAllWidgets(context);

              // }

          }
      };

      @Override
      public int onStartCommand(Intent intent, int flags, int startId) {
          super.onStartCommand(intent, flags, startId);

         if ((intent!=null) && (intent.getAction()!=null) && (intent.getAction().equals("CANCEL_NOTIF"))){

             new Handler().postDelayed(new Runnable() {
                 @Override
                 public void run() {

                     try {
                         wait(4000);
                     }
                     catch (Exception e){}
                     NotificationManager mNotificationManager =
                             (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                     mNotificationManager.cancel(2);

                 }
             },4000);
         }

          Log.d("WIDSERV", "Started");


          return (START_STICKY);

      }


      ChatsPresenter presenter;

      @Override
      public void onCreate() {
          super.onCreate();


          IntentFilter ifil = new IntentFilter();//MyApplication.VKRECORDSCHANGED_BROADCAST);
          ifil.addAction("android.intent.action.SCREEN_ON");
          ifil.addAction("android.intent.action.SCREEN_OFF");
//          ifil.addAction(MyApplication.VKUSERSCHANGED_BROADCAST);
//          ifil.addAction(MyApplication.VKREADCHANGED_BROADCAST);

          Log.d("WIDSERV", "Register receiver");

          registerReceiver(bReceiver, ifil);

          presenter = new ChatsPresenter(this,MyApplication.dataProvider);


      }

      @Override
      public void onDestroy() {

          Log.d("WIDSERV", "UnRegister receiver");

          unregisterReceiver(bReceiver);

          presenter.Destroy();


          super.onDestroy();


      }


      @Override
      public void onChanged() {
          // when data changed
          UpdateAllWidgets(this.getApplicationContext());
      }

      @Override
      public void showLoaded(boolean bLoaded) {

      }

      @Override
      public void openDialog(int uid) {

      }
  }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        UpdateAllWidgets(context);

    }

    public class BackscreenLauncherConstants {
        public static final int WIDGET_SIZE_EXTRA_LARGE = 8;
        public static final int WIDGET_SIZE_FULL_SCREEN = 16;
        public static final int WIDGET_SIZE_LARGE = 0;
        public static final int WIDGET_SIZE_LARGE_PLUS = 5;
        public static final int WIDGET_SIZE_MEDIUM = 1;
        public static final int WIDGET_SIZE_MEDIUM_HALF = 2;
        public static final int WIDGET_SIZE_SMALL = 3;
        public static final int WIDGET_SIZE_TINY = 4;
        public static final int WIDGET_THEME_BLACK = 0;
        public static final int WIDGET_THEME_WHITE = 1;
    }

    protected static void UpdateAllWidgets(Context context){

        Log.d("WIDGET", "UpdateAllWidgets");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, BSWidget.class));

        NotificationPresenter presenter = new NotificationPresenter(null, MyApplication.dataProvider);
        Map<String, Object> map = presenter.GetNotificationInformation();
        int nUnreadMsg = (Integer) map.get("nUnreadMsg");

        for (int id : appWidgetIds)
        {

            Bundle opts = appWidgetManager.getAppWidgetOptions(id);
//            for (String key: opts.keySet())
 //            Log.d("WIDGET",key+"="+opts.get(key).toString());

            int size = opts.getInt("epdlauncher.widget.size", -1);
            if (size==-1) size = opts.getInt("bslauncher.widget.size",-1);

            RemoteViews views = null;
            //Log.d("WIDGET","ID: "+id+" size: "+size);

            if (size == -1) size = BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM;

            if ((size != BackscreenLauncherConstants.WIDGET_SIZE_LARGE) && (size!=BackscreenLauncherConstants.WIDGET_SIZE_LARGE_PLUS)
            && (size != BackscreenLauncherConstants.WIDGET_SIZE_EXTRA_LARGE)
                    && (size != BackscreenLauncherConstants.WIDGET_SIZE_FULL_SCREEN))
            {


                switch (size) {
                    case BackscreenLauncherConstants.WIDGET_SIZE_SMALL:     // Small
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_small);

                        break;
                    case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:    // Medium
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_medium);
                        break;
                    default:
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
                }


                PendingIntent pintent = null;
                if (Utils.isYotaphoneSDK2()) {
                    // TODO: SDK2 pintent = PendingIntent.getService(context.getApplicationContext(), 2, new Intent(context, BSChatsList.class), PendingIntent.FLAG_UPDATE_CURRENT);
                } else {
                    Intent intent = new Intent(context, ChatsList.class);
                    //if (MyApplication.isClass("EpdIntentCompat"))
                    //EpdIntentCompat.addEpdFlags(intent,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                    pintent = PendingIntent.getActivity(context.getApplicationContext(), 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
                views.setOnClickPendingIntent(R.id.Header, pintent);


            switch (size) {
                case BackscreenLauncherConstants.WIDGET_SIZE_SMALL:     // Small
                case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:    // Medium
                    views.setTextViewText(R.id.textView, map.get("Title").toString());
                    if (size == BackscreenLauncherConstants.WIDGET_SIZE_SMALL)
                        break;
                    // case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:
                    views.setTextViewText(R.id.textView2, map.get("Text").toString());

                    if (nUnreadMsg > 0) {
                        views.setTextViewText(R.id.Time, new SimpleDateFormat("HH:mm").format(new Date(((Long) map.get("Date")) * 1000)));
                        views.setViewVisibility(R.id.textView2, View.VISIBLE);
                        views.setViewVisibility(R.id.Time, View.VISIBLE);

                    } else {
                        views.setViewVisibility(R.id.textView2, View.GONE);
                        views.setViewVisibility(R.id.Time, View.GONE);

                    }

            }
        }
       else { // large

                views = new RemoteViews(context.getPackageName(), R.layout.widget_large);

                if (nUnreadMsg>0) {
                    views.setTextViewText(R.id.Time, new SimpleDateFormat("HH:mm").format(new Date((Long) map.get("Date") * 1000)));

                    if (map.containsKey("BigTitle"))
                     views.setTextViewText(R.id.Header, map.get("BigTitle").toString());
                    else
                        views.setTextViewText(R.id.Header, map.get("Title").toString());

                    if (map.containsKey("BigText")) {

                        views.setTextViewText(R.id.Text, (CharSequence) map.get("BigText"));
                    }
                    else
                        views.setTextViewText(R.id.Text, map.get("Text").toString());

                    //views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
                    views.setViewVisibility(R.id.Time, View.VISIBLE);
                }
                else {
                    views.setViewVisibility(R.id.Time, View.GONE);

                }
            }
            PendingIntent pintent = null;
            if (Utils.isYotaphoneSDK2()) {
                // TODO: SDK2 pintent = PendingIntent.getService(context.getApplicationContext(), 2, new Intent(context, BSChatsList.class), PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                Intent intent = new Intent(context, ChatsList.class);
                //if (MyApplication.isClass("EpdIntentCompat"))
                //EpdIntentCompat.addEpdFlags(intent,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                pintent = PendingIntent.getActivity(context.getApplicationContext(), 3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            views.setOnClickPendingIntent(R.id.Text, pintent);



            appWidgetManager.updateAppWidget(id, views);

        }

    }



    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        Log.d("WIDGET", "onEnable");

    }
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);


        //context.stopService(new Intent(context, WidgetService.class));
        Log.d("WIDGET", "UnSetReceiver");

    }



    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        context.startService(new Intent("UPDATE", null, context, WidgetService.class));

        Log.d("WIDGET", "onReceive "+intent.getAction());

        if (intent.getAction().equals("com.yotadevices.yotaphone.action.APPWIDGET_VISIBILITY_CHANGED")) {
            //            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            //            appWidgetManager.updateAppWidget(new ComponentName(context, BSWidget.class),views);

        }
        //else
            UpdateAllWidgets(context);

    }

}