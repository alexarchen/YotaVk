package com.zakharchenko.yotavk;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.httpClient.VKAbstractOperation;
import com.vk.sdk.api.httpClient.VKHttpClient;
import com.vk.sdk.api.httpClient.VKHttpOperation;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiGetDialogResponse;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKUsersArray;
import com.vk.sdk.util.VKUtil;
import com.yotadevices.sdk.BSActivity;
import com.yotadevices.sdk.EpdIntentCompat;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.GCM.RegistrationIntentService;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.Presenter.NotificationPresenter;
import com.zakharchenko.yotavk.Presenter.Presenter;
import com.zakharchenko.yotavk.Utils.Utils;
import com.zakharchenko.yotavk.View.BSWidget;
import com.zakharchenko.yotavk.View.ChatsList;
import com.zakharchenko.yotavk.View.MainActivity;
import com.zakharchenko.yotavk.View.MessagesList;
import com.zakharchenko.yotavk.View.NotifActivity;

import org.apache.http.HttpServerConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zakharchenko on 16.03.2016.
 *
 * Copyright (c) GNU 3 licence
 * class MyApplication contains all buisiness logic, requests to VK server and GCM
 * Screen views (Activities,BSActivities, Widgets) receive broadcasts from MyApplication when data changed
 * Most methods and properties are static and order to simplify using them from each part of code
 */


public class MyApplication extends Application implements NotificationPresenter.Listener{


    // GCM access token
    VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
            if (newToken == null) {
                Toast.makeText(MyApplication.this, "AccessToken invalidated", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MyApplication.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);            }
        }
    };

    // Utilily function
    public static void SetIntentExtras(Intent intent){

        int nUnread=0;
        int nUnreadMsg=0;

        try{

            List<VKDialog> dialogs = dataProvider.getDialogs();
            if (dialogs!=null) {

                VKList<VKMessage> msgs = new VKList<>();

                for (VKDialog dlg : dialogs) {
                    if (dlg.unread > 0) {
                        nUnread++;
                        nUnreadMsg += dlg.unread;

                    }

                }

               // msgs contains list of non read messages
               // formin brief in the form of
               // Unread %d messages in %d chats\n
               //

               String brief="";



            }
        }
        catch (Exception e ){

        }
        intent.putExtra("UnreadCount", nUnreadMsg);
        intent.putExtra("UnreadChatsCount", nUnread);
    }

    /* Broadcast send when some messages changed state */
    public static String VKREADCHANGED_BROADCAST="com.zakharchenko.yovk.VKREADCHANGED_BROADCAST";


    public static VKData dataProvider;

    @Override
    public void onTerminate() {
        super.onTerminate();

        Log.d("YOVK", "Terminate");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();


        dataProvider.SaveCache();
        dataProvider =null;
        AppContext = null;

    }

   /* Self static context, null if not ran */
   public static Context AppContext=null;

   public static String CacheDir;

    public VKAccessToken vkAccessToken=null;

    @Override
    public void onCreate() {
        super.onCreate();

        AppContext = this;

        CacheDir = getCacheDir().getAbsolutePath()+"/";
        new File(MyApplication.CacheDir+"img_cache/").mkdirs();


        if (BuildConfig.FLAVOR.equals("mock")) {
            try {
                dataProvider = (VKData) Class.forName("com.zakharchenko.yotavk.Data.VKDataMock").getConstructors()[0].newInstance();
            }
            catch ( Exception e){

            }
        }
        else {
            dataProvider = new VKData(CacheDir);

            Log.d("GCM", "GCM AppID: " + R.string.google_app_id);
            vkAccessTokenTracker.startTracking();
            VKSdk.customInitialize(this, getResources().getInteger(R.integer.com_vk_sdk_AppId), "5.50");
            Log.d("VKSDK", "Version: " + VKSdk.getApiVersion());

        }

        dataProvider.init();

        presenter = new NotificationPresenter(this,dataProvider);


        if (!BuildConfig.FLAVOR.equals("mock")) {

            if (VKSdk.isLoggedIn()) {
                Log.d("VK", "Logged");
            }


            if (checkPlayServices()) {
                // Start IntentService to register this application with GCM.
                Log.d("App", "Start registration");
                Intent intent = new Intent(this, RegistrationIntentService.class);
                try {
                    startService(intent);
                } catch (Exception e) {
                    Log.e("App", "Can't start RegistrationIntentService");
                }
            } else Log.d("App", "Play serice is not available");

            try {
                File f = new File(CacheDir+"http_cache/");
                f.mkdirs();
                HttpResponseCache.install(f, 30000000);
            } catch (Exception e) {
            }
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {

            return false;
        }
        return true;
    }


    @Override
    public void onChanged() {
    }

    @Override
    public void onNotify(boolean bNewMsg) {
        NotifySystem(bNewMsg);
    }

    @Override
    public void showLoaded(boolean bLoaded) {

    }

    /* Show or change system notification
   * bNewMsg - if true new message arrived */


    static boolean HasNotifications = false;
    static boolean HasNewMessageNotification = false;
    static int LastNewMessageId;

    public static void NotificationsRemoved(int id){
        Log.d("APP","NotificationsRemoved: "+id);
       if (id==1)
        HasNotifications = false;
       else
         if (id==2)
           HasNewMessageNotification = false;
   }


   NotificationPresenter presenter;
   ScheduledFuture stopNotifTask;
   public void NotifySystem(boolean bNewMsg){

        Context context = AppContext;

       final NotificationManager mNotificationManager =
               (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

       Log.d("APP","NotifySystem");

       if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("keep_notif",true)) {
           // only new messages or change old
          if (!bNewMsg) {
              if ((!HasNotifications) && (!HasNewMessageNotification))
               return;
          }
       }

       if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("post_notif",true)) {
           if ((HasNewMessageNotification) || (HasNotifications))
               mNotificationManager.cancelAll();
           return;
       }

        boolean bOK = true;


       //if (bNewMsg)
       //    mNotificationManager.cancelAll();



        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context);
         mBuilder.setSmallIcon(R.drawable.yotavk);


       Map<String,Object> map = presenter.GetNotificationInformation();
       if (map==null) return;

       int nUnreadMsg = (Integer) map.get("nUnreadMsg");

       if (nUnreadMsg>0) {
           mBuilder.setNumber(nUnreadMsg);
           mBuilder.setGroup("yovk");
           mBuilder.setGroupSummary(true);
           mBuilder.setContentText(map.get("Text").toString());
           mBuilder.setContentTitle(map.get("Title").toString());

           if (map.containsKey("inboxStyle"))
               mBuilder.setStyle((NotificationCompat.InboxStyle)map.get("inboxStyle"));

                        Uri uri = Uri.parse(map.get("url").toString());
                        Intent intent1 = new Intent(context, MainActivity.class);
//                    intent1.setPackage(context.getPackageName());
                        intent1.setData(uri);
                        intent1.setAction(Intent.ACTION_VIEW);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        //(int)(System.currentTimeMillis()/100)
                        PendingIntent p = PendingIntent.getActivity(context.getApplicationContext(), 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

                        mBuilder.setContentIntent(p);

                        if ((PreferenceManager.getDefaultSharedPreferences(context).getBoolean("keep_notif",true))
                            || (!HasNotifications) || (bNewMsg)) {
                            HasNotifications = true;
                            mNotificationManager.notify(1, mBuilder.build());
                            Log.d("YOTAVK", "Add group notifcation");
                        }


                        // send also small notification
                        if (bNewMsg) {
                            mBuilder =
                                    new NotificationCompat.Builder(context);

                            map = presenter.GetLastMessage();

                            mBuilder.setSmallIcon(R.drawable.yotavk);

                            LastNewMessageId = (Integer) map.get("Id");

                            //mBuilder.setGroup("yovk");
                            mBuilder.setContentTitle(map.get("Title").toString() + ":");
                            mBuilder.setContentText(map.get("Text").toString());
                            mBuilder.setContentIntent(p);

                            //mBuilder.setOngoing(true);

                            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_headsup", true)) {

                                mBuilder.setPriority(Notification.PRIORITY_HIGH);
                                //Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.incoming_message);
                                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notif_sound", true)) {
                                    Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    mBuilder.setSound(sound);

                                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notif_vibrate", true))
                                        mBuilder.setVibrate(new long[]{0, 300});

                                } else
                                    mBuilder.setVibrate(new long[]{0, 300});


                            } else {

                                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notif_sound", true)) {
                                    // Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.incoming_message);
                                    Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    mBuilder.setSound(sound);
                                }
                                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notif_vibrate", true)) {
                                    mBuilder.setVibrate(new long[]{0, 300});
                                }
                            }

                            //mBuilder.setContentIntent(p);

                            Log.d("YOTAVK", "Add sound notifcation");
                            HasNewMessageNotification = true;

                            Notification notify = mBuilder.build();
                            mNotificationManager.notify(2, notify);


                            if (stopNotifTask!=null)
                                stopNotifTask.cancel(true);

                            stopNotifTask = Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    mNotificationManager.cancel(2);
                                }
                            }, 4000, TimeUnit.MILLISECONDS);


                            if ((Utils.isYotaphoneSDK3())
                            && (!PreferenceManager.getDefaultSharedPreferences(this).getString("notif_time","0").equals("0")))
                            {
                                if ((ChatsList.Self!=null) && (MessagesList.Self!=null)) {
                                    Intent i = new Intent(this, NotifActivity.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    EpdIntentCompat.addEpdFlags(i, EpdIntentCompat.FLAG_ACTIVITY_KEEP_ON_EPD_SCREEN);
                                    startActivity(i);
                                }
                            }
                        }
                        else
                        if (HasNewMessageNotification){
                            // check if already read
                            if (dataProvider.getDialog(LastNewMessageId).unread==0) {
                                Log.d("APP","Cancel notif 2");
                                mNotificationManager.cancel(2);
                            }
                        }


                        bOK = true;


                    } // nUnreadMsg>0


                    /*
                    Intent resultIntent = new Intent(this, ResultActivity.class);
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addParentStack(ResultActivity.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );*/




/*        if (!bOK){

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.cancel(1);

        }*/


       if (nUnreadMsg==0){
           Log.d("APP","Clear notifications");
           mNotificationManager.cancelAll();

       }

       //context.startService(new Intent("CANCEL_NOTIF", null, context, BSWidget.WidgetService.class));


    }


}