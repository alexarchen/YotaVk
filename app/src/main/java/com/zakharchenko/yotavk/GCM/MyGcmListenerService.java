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

package com.zakharchenko.yotavk.GCM;

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
import com.yotadevices.sdk.EpdIntentCompat;
import com.yotadevices.sdk.YotaPhone;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.Utils.Utils;
import com.zakharchenko.yotavk.View.ChatsList;
import com.zakharchenko.yotavk.View.MessagesList;
import com.zakharchenko.yotavk.View.NotifActivity;

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

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG,"onMessageReceived");
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        //Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Body: " + data.toString());
        //Body: Bundle[{msg_id=50, uid=2000000001, text=Здрассссьте, type=msg, badge=1, _genSrv=403026, sandbox=0, collapse_key=msg}]


        final Context context = this;

        String suid = data.getString("uid");
        if ((suid!=null) && (data.getString("type")!=null) && (data.getString("type").equals("msg"))){


            MyApplication.dataProvider.getDialogByIdWithMessages(Integer.decode(suid));
            //MyApplication.dataProvider.RefreshDialogs(); //.AddNewMessage(data.getInt("uid"),data.getInt("msg_id"),data.getString("text"));
            //MyApplication.NotifySystem(true);
        }

    }
    // [END receive_message]

}
