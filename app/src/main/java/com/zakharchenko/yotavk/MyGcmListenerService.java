/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zakharchenko.yotavk;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.yotadevices.sdk.YotaPhone;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [ssage]

    /*
    public static boolean isVKRunning(Context ctx) {

        PowerManager pm = (PowerManager)
                ctx.getSystemService(Context.POWER_SERVICE);
        if (!pm.isInteractive()) return (false);

        final int PROCESS_STATE_TOP = 2;
        ActivityManager.RunningAppProcessInfo currentInfo = null;
        Field field = null;
        try {
            field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
        } catch (Exception ignored) {
        }
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo app : appList) {
            if (app.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && app.importanceReasonCode == ActivityManager.RunningAppProcessInfo.REASON_UNKNOWN) {
                Integer state = null;
                try {
                    state = field.getInt(app);
                } catch (Exception e) {
                }
                if (state != null && state == PROCESS_STATE_TOP) {
                    currentInfo = app;
                    break;
                }
            }
        }


        if (currentInfo!=null) {
            Log.d("YOTAVK","Found foreground process: "+currentInfo.processName);
            for (String pck : currentInfo.pkgList) {
                if (pck.equals("com.vkontakte.android")) return (true);
            }
        }
        return false;

    }
*/

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG,"onMessageReceived");
        String message = data.getString("message");
        //Log.d(TAG, "From: " + from);
        //Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Body: " + data.toString());
        //Body: Bundle[{msg_id=50, uid=2000000001, text=Здрассссьте, type=msg, badge=1, _genSrv=403026, sandbox=0, collapse_key=msg}]


        final Context context = this;

        String suid = data.getString("uid");
        if ((suid!=null) && (data.getString("type")!=null) && (data.getString("type").equals("msg"))){

            final int uid = new Integer(suid);
            MyApplication.GetMessages(uid, new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                    intent.putExtra("bNewMsg", true);

                    MyApplication.SetIntentExtras(intent);

                    if (Build.VERSION.SDK_INT<23) {
                        if ((!BSChatsList.isRunning()) && (!BSMessagesList.isRunning()))
                            // if ((!ChatsList.isRunning()) && (!MessagesList.isRunning()))
                            if ((new Integer(PreferenceManager.getDefaultSharedPreferences(context).getString("notif_time", "10"))) > 0) {
                                Intent intent1 = new Intent(getApplicationContext(), BSNotifActivity.class);
                                getApplicationContext().startService(intent1);

                                //MyApplication.NotifySystem();
                            }
                    }
                    else {
                        if ((!ChatsList.isRunning()) && (!MessagesList.isRunning()))
                            // if ((!ChatsList.isRunning()) && (!MessagesList.isRunning()))
                            if ((new Integer(PreferenceManager.getDefaultSharedPreferences(context).getString("notif_time", "10"))) > 0) {
                                Intent intent1 = new Intent(getApplicationContext(), BSNotifActivity.class);
                                getApplicationContext().startService(intent1);

                                //MyApplication.NotifySystem();
                            }
                    }
                    //if ((!BSChatsList.isRunning()) && (!BSMessagesList.isRunning()))
                    // if ((!ChatsList.isRunning()) && (!MessagesList.isRunning()))
                    {
                        //MyApplication.NotifySystem();
                    }


                    sendBroadcast(intent);
                }
            }, true);


            MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                    MyApplication.SetIntentExtras(intent);
                    sendBroadcast(intent);
                }
            });
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
      //  sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        /*
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 , intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 , notificationBuilder.build());
        */
    }
}
