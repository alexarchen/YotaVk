package com.zakharchenko.yotavk;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zakharchenko on 16.03.2016.
 */
public class MyApplication extends Application {
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


    public static VKUsersArray friends=null;
    public static VKUsersArray userCache=null;

    public static class VKMessage extends VKApiMessage{

        public VKMessage(){super();}

        public VKMessage(VKApiMessage msg,int _chat_id){
            super();

            Parcel in = Parcel.obtain();
            msg.writeToParcel(in,0);

            in.setDataPosition(0);

            this.id = in.readInt();
            this.user_id = in.readInt();
            this.date = in.readLong();
            this.read_state = in.readByte() != 0;
            this.out = in.readByte() != 0;
            this.title = in.readString();
            this.body = in.readString();
            this.attachments = in.readParcelable(VKAttachments.class.getClassLoader());
            this.fwd_messages = in.readParcelable(VKList.class.getClassLoader());
            this.emoji = in.readByte() != 0;
            this.deleted = in.readByte() != 0;

            chat_id = _chat_id;
        }

        public VKMessage(JSONObject from) throws JSONException
        {
            super(from);

            try {
                chat_id = from.getInt("chat_id");
            }
            catch (Exception e){}

            if (chat_id==0) chat_id = user_id;
            else
            chat_id+=CHAT_UID_OFFSET;

            JSON = from.toString();

        }

        public int chat_id=0; // chat id is the same as in dialog.uid user_id or 2000000000+chatid

        public static Creator<VKMessage> CREATOR = new Creator<VKMessage>() {
            public VKMessage createFromParcel(Parcel source) {
                return new VKMessage(source);
            }

            public VKMessage[] newArray(int size) {
                return new VKMessage[size];
            }
        };

        public VKMessage(Parcel p){

            super(p);
            chat_id = p.readInt();
        }
        String JSON;
    }

    public static class VKDialog extends VKApiDialog {
        public VKDialog(){
            super();
        }

        public VKDialog(JSONObject from) throws JSONException
        {
            super(from);

            uid = message.user_id;

            try {
                chat_id = new Integer(from.optJSONObject("message").optString("chat_id"));

            }
            catch (Exception e){chat_id=0;}

            if (chat_id>0) uid = CHAT_UID_OFFSET+chat_id;

            JSON = from.toString();
        }

        public static Creator<VKDialog> CREATOR = new Creator<VKDialog>() {
            public VKDialog createFromParcel(Parcel source) {
                return new VKDialog(source);
            }

            public VKDialog[] newArray(int size) {
                return new VKDialog[size];
            }
        };

        public VKDialog(Parcel in) {
            super (in);
            //this.unread = in.readInt();
            //this.message = in.readParcelable(VKApiMessage.class.getClassLoader());
            this.Messages = new VKList<VKMessage>(in.readArrayList(VKMessage.class.getClassLoader()));
        }

        public boolean isMulti(){return (chat_id>0);}
        public VKList<VKMessage> Messages;
        int uid;
        int chat_id;
        String JSON;
    }

    public static VKList<VKDialog> dialogs=null;

    public static ReentrantLock lock = new ReentrantLock();


   public static boolean isClass(String className) {

        try  {
            Class.forName(className);
            Class c= BSActivity.class;
            return true;
        }  catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isYotaphoneSDK2(){

        return (isClass("com.yotadevices.sdk.BSActivity"));
    }

    public static VKList<VKDialog> dialogs_ex(){

     VKList<VKDialog> dlgs = null;
     lock.lock();
        try {
            if (dialogs != null) {
                dlgs = new VKList<>(dialogs);
                if (friends != null) {
                    for (VKApiUserFull friend  : friends){
                        boolean found = false;
                        for (VKDialog d:dialogs){
                            if (d.uid==friend.getId()) {found = true; break;}
                        }

                        if (!found) {
                            VKDialog dlg = new VKDialog();
                            dlg.uid = friend.getId();
                            dlg.message = null;
                            dlgs.add(dlg);
                        }
                    }
                }
            }
        }
        finally {
            lock.unlock();
        }

        return (dlgs);
    }
    public static void GetFriends(final VKRequest.VKRequestListener listener){

        VKApi.friends().get(VKParameters.from("order", "hints", "count", "500", "fields", "photo_50,photo_100,nickname,last_seen")).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);


                Log.d("FRIENDS", "Resp: " + response.responseString);
                SaveCache();
/*
                JSONArray array = response.json.optJSONObject("response").optJSONArray("items");
                String user_str = "";
                if (array != null) {
                    //int[] result = new int[array.length()];
                    for (int i = 0; i < array.length(); i++) {
                        if (i > 0) user_str += ',';
                        user_str += array.optInt(i);
                        //Log.d("FRIENDS", "Got ID: " + result[i]);
                    }


                    VKParameters params = VKParameters.from(VKApiConst.USER_IDS, user_str, VKApiConst.FIELDS, "photo_id,photo_50,is_friend");
                    Log.d("FRIENDS", "Params:" + params.toString());
                    VKApi.users().get(params).executeWithListener(new VKRequest.VKRequestListener() {
                        @Override
                        public void onError(VKError error) {
                            super.onError(error);

                            Log.d("FRIENDS", "Users Error: " + error);

                            if (listener != null)
                                listener.onError(error);

                        }

                        @Override
                        public void onComplete(VKResponse response) {
                            super.onComplete(response);

                            Log.d("FRIENDS", "Fill: " + response.responseString);
*/
                            lock.lock();
                            try {

                                friends = new VKUsersArray();
                                friends.parse(response.json);

                            } catch (Exception e) {
                            } finally {
                                lock.unlock();
                            }

                            SaveCache();

                            // parse data
                            // preload images for 100 usersa

                          Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        int c = 0;

                                        List<Integer> strs = new ArrayList<Integer>();

                                        MyApplication.lock.lock();
                                        try {
                                            for (VKApiUser user : friends) {
                                                strs.add(user.getId());
                                            }
                                        } finally {
                                            MyApplication.lock.unlock();
                                        }

                                        for (Integer i : strs) {

                                            String name = "";
                                            VKApiUserFull user = MyApplication._getUserById(i);
                                            if (user != null) {
                                                int id = user.getId();
                                                name = user.photo_100;

                                                try {
                                                    URL url = new URL(name);
                                                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                                    connection.setDoInput(true);
                                                    connection.connect();
                                                    InputStream input = connection.getInputStream();

                                                    ImageUtil.copyInputStreamToFile(input, new File(CacheDir, "img_cache/img_user_" + id + ".jpg"));


                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                c++;

                                            }

                                            if (c > 500) break;
                                        }

                                        NotifySystem(false);

                                        if (listener != null)
                                            listener.onComplete(null);


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            thread.start();

               // NotifySystem();

                            if (listener != null)
                                listener.onComplete(null);

                        }
            /*
                    });

                } else
                    Log.d("FRIENDS", "Can't get IDs");

            }*/

            @Override
            public void onError(VKError error) {
                super.onError(error);

                Log.d("FRIENDS", "Error: " + error);

                if (listener != null)
                    listener.onError(error);
            }
        });

    }

    static public VKDialog getDialogById(int uid){
        VKDialog dlg = null;
        lock.lock();
        try {
            if (dialogs != null) {

                for (VKDialog d:dialogs){
                    if (d.uid==uid)
                    {dlg = d; break;}
                }

            }
        }
        finally {
            lock.unlock();
        }
        return (dlg);
    }

    static public VKApiUserFull getUserById(int uid){return (getUserById(uid,true));}
    static public VKApiUserFull _getUserById(int uid){return (getUserById(uid, true));}


    static public VKApiUserFull getUserById(final int uid,boolean bAdd){
        VKApiUserFull user = null;
        lock.lock();
        try {
            boolean bFromCache = false;

            if (friends!=null) {
                user = friends.getById(uid);
            }
            if ((user==null) && (userCache!=null))
            {

                user = userCache.getById(uid);
                bFromCache = true;
            }

        if ((friends!=null) && ((user==null) || ((user.last_seen==0) && bFromCache)) && (bAdd)){

            if (user==null) {
                user = new VKApiUserFull();
                user.id = uid;
                user.first_name = "User#" + uid;
                user.last_name = "";
                if (userCache == null) userCache = new VKUsersArray();
                userCache.add(user);
            }
            Log.d("YOTAVK","Getting user "+uid);

            VKApi.users().get(VKParameters.from(VKApiConst.USER_ID, "" + uid, VKApiConst.FIELDS, "photo_id,photo_100,photo_50,is_friend,last_seen")).executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    try {
                        Log.d("USER", response.json.toString());

                        String user_photo_100="";

                        lock.lock();
                        try {
                            VKApiUserFull user=null;
                             user = getUserById(uid, false);
                            if (user!=null) {


                                user.parse(response.json.getJSONArray("response").getJSONObject(0));

                                user_photo_100 = user.photo_100;
                            }
                        }
                        catch(Exception e){

                        }
                        finally {
                            lock.unlock();
                        }

                        SaveCache();
                        // getting photo

                        if (user_photo_100.length()>0) {

                            Log.d("USER","Loading photo "+user_photo_100+"...");

                            final String urlstr = user_photo_100;

                           new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        URL url = new URL(urlstr);
                                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                        connection.setDoInput(true);
                                        connection.setUseCaches(true);
                                        connection.connect();
                                        InputStream input = connection.getInputStream();
                                        ImageUtil.copyInputStreamToFile(input, new File(CacheDir, "img_cache/img_user_" + uid + ".jpg"));
                                        Log.d("USER", "Loaded");

                                    } catch (IOException e) {
                                        Log.i("YOTAVK", "HTTP error " + e);
                                    }

                                    if (AppContext!=null)
                                        AppContext.sendBroadcast(new Intent(VKUSERSCHANGED_BROADCAST));


                                 }
                                }).start();
                        }

                        NotifySystem(false);


                        //if (AppContext!=null)
                        // AppContext.sendBroadcast(new Intent(VKUSERSCHANGED_BROADCAST));

                    } catch (Exception e) {
                    }
                }
            });


        }

        }
        finally {
            lock.unlock();
        }


        return  user;
    }

    public static void AddUserToCache(VKApiUserFull user){

     VKApiUserFull old = userCache.getById(user.getId());
        if (old!=null) userCache.remove(old);

      userCache.add(user);
    }

    public static void GetUser(final int uid,final VKRequest.VKRequestListener listener){

        VKApi.users().get(VKParameters.from(VKApiConst.USER_ID, "" + uid, VKApiConst.FIELDS, "photo_id,photo_50,is_friend")).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    VKApiUserFull user = new VKApiUserFull(response.json);
                    AddUserToCache(user);
                    listener.onComplete(response);
                } catch (Exception e) {
                }
            }
        });

    }
    public static void GetMessages(final int uid,final VKRequest.VKRequestListener listener){
        GetMessages(uid, listener, false);
    }

    public static void GetMessages(final int uid,final VKRequest.VKRequestListener listener, final boolean bNewMsg){

        Log.d("MESSAGES", "Get for " + uid);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final VKRequest request = new VKRequest("messages.getHistory", VKParameters.from("user_id",uid<2000000000?""+uid:"","peer_id",""+uid,VKApiConst.COUNT,"200","v","5.50"));

                VKHttpClient.VKHTTPRequest hrequest = VKHttpClient.requestWithVkRequest(request);
                for (Pair<String, String> param:hrequest.parameters){
                    if (param.first.equals("v")) {
                        hrequest.parameters.remove(param);
                        break;
                    }
                }
                hrequest.parameters.add(new Pair<String, String>("v", "5.50"));

                try {
                    VKHttpClient.VKHttpResponse response = VKHttpClient.execute(hrequest);
                    Log.d("MESSAGES", "Messages: " + new String(response.responseBytes));
                    JSONObject obj = new JSONObject(new String(response.responseBytes));
                    if(obj.toString().substring(2, 7).toLowerCase().equals("error"))
                    {


                        if (listener != null)
                            listener.onError(new VKError(obj));

                    }
                    else {

                        lock.lock();
                        try {
                            VKDialog dlg = getDialogById(uid);
                            dlg.Messages = new VKList<VKMessage>(obj, VKMessage.class);
                            Collections.reverse(dlg.Messages);

                            int nunread=0;
                            for (VKApiMessage msg : dlg.Messages){
                                if ((!msg.out) && (!msg.read_state))
                                    nunread++;
                            }
                            dlg.unread = nunread;

                            if (dlg.Messages.size()>0) {
                                String title="";
                                if (dlg.message!=null) {
                                   title=dlg.message.title;
                                }
                                    dlg.message = dlg.Messages.get(0);
                                dlg.message.title = title;


                            }
                        }
                        catch(Exception e){

                        }
                        finally {
                            lock.unlock();
                        }

                        NotifySystem(bNewMsg);

                        if (listener != null)
                            listener.onComplete(null);


                    }

                }
                catch (Exception e){}

            }
        }).start();


        /*
        VKHttpOperation oper = new VKHttpOperation(hrequest);
        oper.setHttpOperationListener(new VKAbstractOperation.VKAbstractCompleteListener<VKHttpOperation, VKHttpClient.VKHttpResponse>() {
            @Override
            public void onComplete(VKHttpOperation op, VKHttpClient.VKHttpResponse response) {
                //super.onComplete(response);

                Log.d("MESSAGES", "Messages: " + response.toString());


/*
                lock.lock();
                try {
                    VKDialog dlg = getDialogById(uid);
                    dlg.Messages = new VKList<VKApiMessage>(response.json, VKApiMessage.class);
                    Collections.reverse(dlg.Messages);
                } finally {
                    lock.unlock();
                }
*/
/*
            }

            @Override
            public void onError(VKHttpOperation o, VKError error) {
                //super.onError(o,error);

                Log.d("MESSAGES", "Error " + error);
            }
        });

//        Log.d("MESSAGES",request.toString());
        VKHttpClient.enqueueOperation(oper);
*/

    }

    public static boolean bDoGetMessages = false;

    public static void GetMessages(final VKRequest.VKRequestListener listener){

        Log.d("DIALOGS","Gettings dialogs");

        bDoGetMessages = true;

        VKApi.messages().getDialogs(VKParameters.from(VKApiConst.COUNT, "200")).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
//Do complete stuff
                VKApiGetDialogResponse dialogResponse = new VKApiGetDialogResponse(response.json);
                String text = "";

                Log.d("DIALOGS", response.responseString);
                text += "Dialogs: " + dialogResponse.count + " unread: " + dialogResponse.unread_dialogs + "\n";
                int nUnread =0;

                //this.items = new VKList<>(response.json.optJSONArray("items"), VKMyApiDialog.class);
                lock.lock();
                try {


                    VKList<VKDialog> _dialogs = new VKList<VKDialog>(response.json.optJSONObject("response").optJSONArray("items"), VKDialog.class);
                    // add message information
                    for (VKDialog dlg : _dialogs){

                     VKDialog olddlg = getDialogById(dlg.uid);
                        if ((olddlg!=null) && (olddlg.Messages!=null))
                        {
                            dlg.Messages = olddlg.Messages;
                        }


                        nUnread+=dlg.unread;
                    }

                    dialogs = _dialogs;


                }
                finally {

                    lock.unlock();
                }


                bDoGetMessages = false;

                NotifySystem(false);

                if (nUnread==0){
                    NotificationManager mNotificationManager =
                            (NotificationManager) MyApplication.AppContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancelAll();

                }

                if (listener!=null)
                    listener.onComplete(null);


            }

            @Override
            public void onError(VKError error) {
//Do error stuff
                bDoGetMessages = false;

            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
//I don't really believe in progress

                bDoGetMessages = false;

            }
        });

    }

    public static int CHAT_UID_OFFSET = 2000000000;
    public static boolean SendMessage(final int uid,final String text,final VKRequest.VKRequestListener listener){
        if (!VKSdk.isLoggedIn()){
           return (false);
        }

        // search for dialog
        VKDialog dlg = null;
        lock.lock();
        try {
            dlg = getDialogById(uid);

            if (dlg==null){
                if (uid<CHAT_UID_OFFSET) {

                    dlg = new VKDialog();
                    dlg.uid = uid;
                    dlg.chat_id = 0;
                    dlg.Messages = new VKList<>();


                }
            }

            if (dlg!=null){

                VKMessage msg = new VKMessage();
                msg.user_id = uid;
                if (dlg.message!=null)
                 msg.title = dlg.message.title;
                msg.body = text;
                msg.date = new Date().getTime()/1000;
                msg.out = true;
                msg.deleted = false;
                msg.read_state = true;

                dlg.Messages.add(msg);
                dlg.message = msg;

                //VKRequest req = new VKRequest("messages.send",VKParameters.from(VKApiConst.USER_ID,));


            }
        }
        finally {
            lock.unlock();
        }

       if (dlg!=null){

           final VKRequest request = new VKRequest("messages.send", VKParameters.from("user_id",uid<2000000000?""+uid:"","peer_id",""+uid,VKApiConst.MESSAGE,text,"random_id",Math.round((Math.random()*100000))+""));
           Log.d("MESSAGES","Send message: "+request.toString());
           request.executeWithListener(new VKRequest.VKRequestListener() {
               @Override
               public void onComplete(VKResponse response) {
                   super.onComplete(response);

                   Log.d("MESSAGES", "Complete " + response.json.toString());

                   lock.lock();
                   try {
                       VKDialog dlg = getDialogById(uid);
                       //dlg.Messages = new VKList<VKApiMessage>(obj, VKApiMessage.class);
                       //Collections.reverse(dlg.Messages);
                       if ((dlg.Messages != null) && (dlg.Messages.size()>0)){
                           VKMessage msg = dlg.Messages.get(dlg.Messages.size()-1);
                           if (msg.body.equals(text)) {

                               msg.id = response.json.getInt("response");
                           }
                       }

                   } catch (Exception e) {
                   } finally {
                       lock.unlock();
                   }


                   listener.onComplete(response);
               }

               @Override
               public void onError(VKError error) {
                   super.onError(error);

                   Log.d("MESSAGES", "Error " + error.toString());

                   lock.lock();
                   try {
                       VKDialog dlg = getDialogById(uid);
                       //dlg.Messages = new VKList<VKApiMessage>(obj, VKApiMessage.class);
                       //Collections.reverse(dlg.Messages);
                       if ((dlg.Messages != null) && (dlg.Messages.size()>0)){
                           VKMessage msg = dlg.Messages.get(dlg.Messages.size()-1);
                           if (msg.body.equals(text)) {

                               dlg.Messages.remove(dlg.Messages.size()-1);
                               if (dlg.Messages.size() > 0)
                                   dlg.message = dlg.Messages.get(dlg.Messages.size()-1);
                               else
                                   dialogs.remove(dlg);
                           }
                       }
                   } finally {
                       lock.unlock();
                   }

                   listener.onError(error);
               }
           });


       }


       return (dlg!=null);
    }

//messages.markAsRead


    public static void SetIntentExtras(Intent intent){

        int nUnread=0;
        int nUnreadMsg=0;
        lock.lock();
        try{

            if (dialogs!=null) {

                VKList<VKMessage> msgs = new VKList<>();

                for (VKDialog dlg : dialogs) {
                    if (dlg.unread > 0) {
/*
                        VKList<VKApiMessage> mmm =dlg.Messages;
                        if ((mmm==null) && (dlg.message!=null)) {mmm = new VKList<>(); mmm.add(dlg.message);}

                        if (mmm!=null){

                            for (VKApiMessage msg:mmm){
                               if (!msg.read_state){
                                   int q=0;
                                   for (;q<msgs.size();q++){
                                     if (msgs.get(q).date<msg.date)
                                     {
                                      break;
                                     }else
                                     if (msgs.get(q).date==msg.date)
                                     {
                                       if (msgs.get(q).user_id>=msg.user_id)
                                           break;
                                     }

                                   }
                                   msgs.add(q,msg);



                               }
                            }
                        }
*/
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
        finally {
            lock.unlock();
        }
        intent.putExtra("UnreadCount", nUnreadMsg);
        intent.putExtra("UnreadChatsCount", nUnread);
    }

    public static String VKREADCHANGED_BROADCAST="com.zakharchenko.yovk.VKREADCHANGED_BROADCAST";

    public static void MarkAsRead(final int DialogId){

        VKDialog dlg = getDialogById(DialogId);
        if (dlg!=null){

            String ids = "";

            for (VKMessage msg : dlg.Messages){
                if (!msg.read_state){
                    ids+=(ids.length()==0?"":",")+msg.getId();
                }
            }

            if (ids.length()>0){

                new VKRequest("messages.markAsRead",VKParameters.from("peer_id",""+DialogId,"message_ids",ids)).executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        NotifySystem(false);

                        if (AppContext!=null)
                            AppContext.sendBroadcast(new Intent(VKREADCHANGED_BROADCAST));

/*
                        Intent intent = new Intent();
                        intent.setPackage("com.vkontakte.android");
                        intent.setAction("com.google.android.c2dm.intent.RECEIVE");
                        intent.putExtra("message_type", "gcm");
                        intent.putExtra("from", "yovk");
                        intent.putExtra("msg_id",0);
                        intent.putExtra("uid",DialogId);
                        intent.putExtra("text","");
                        intent.putExtra("type","msg");
                        intent.putExtra("collapse_key","msg");
                        //{msg_id=50, uid=2000000001, text=Здрассссьте, type=msg, badge=1, _genSrv=403026, sandbox=0, collapse_key=msg}
                        AppContext.sendBroadcast(intent);*/
                    }
                });
            }
        }
    }

    public static void CreateChat(String name,int[] userIds, final VKRequest.VKRequestListener listener){

        Log.d("DIALOGS","Creating chat "+name+" ids: "+userIds);

        String userIDsStr = "";
        int c=0;
        for (int i:userIds){
            userIDsStr+=(c==0?"":",")+i;
            c++;
        }

        new VKRequest("messages.createChat",VKParameters.from("user_ids",userIDsStr,"title",name)).executeWithListener(listener);

    }

    public static VKDialog CreateDialog(int uid){

        VKDialog dlg = new VKDialog();
        dlg.message = null;
        dlg.uid = uid;
        dialogs.add(dlg);
        return (dlg);
    }

    static String CacheDir;

    VKRequest.VKRequestListener defListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {

            Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
            MyApplication.SetIntentExtras(intent);
            sendBroadcast(intent);

        }
    };

    public static String VKRECORDSCHANGED_BROADCAST="com.zakharchenko.yotavk.VKRECORDSCHANGED_BROADCAST";
    public static String VKUSERSCHANGED_BROADCAST="com.zakharchenko.yotavk.VKUSERSCHANGED_BROADCAST";

    public VKAccessToken vkAccessToken=null;


    @Override
    public void onCreate() {
        super.onCreate();

        AppContext = this;

        Log.d("GCM", "GCM AppID: " + R.string.google_app_id);
/*
        String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());

        if (fingerprints!=null)
            for (String s: fingerprints)
                Log.i("YotaVK", "FINGERPRINT: " + s);
        else
        Log.i("YotVK","Null fingerprint");
*/
        vkAccessTokenTracker.startTracking();
        //VKSdk.initialize(this);
        VKSdk.customInitialize(this,getResources().getInteger(R.integer.com_vk_sdk_AppId),"5.50");


        Log.d("VKSDK", "Version: " + VKSdk.getApiVersion());


        CacheDir = "mnt/sdcard/Android/data/"+getPackageName()+"/";
        new File(MyApplication.CacheDir+"img_cache/").mkdirs();
        LoadCache();


        if (VKSdk.isLoggedIn()) {
            Log.d("FRIENDS", "Start gettings...");
            GetFriends(defListener);

           // GetMessages(defListener);

        }

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Log.d("App","Start registration");
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        else Log.d("App", "Play serice is not available");

        try {
            File f = new File("mnt/sdcard/Android/data/" + getPackageName() + "/http_cache");
            HttpResponseCache.install(f, 10000000);
        }
        catch (Exception e){}

    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {

            return false;
        }
        return true;
    }

    static public void LoadCache(){

        lock.lock();

        try{

            {
                InputStream file = new FileInputStream(CacheDir + "users.cache");
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput Input = new ObjectInputStream(buffer);

                Integer nF = (Integer) Input.readObject();
                Log.d("CACHE","Found "+nF+ " users");

                if (nF > 0) {

                    friends = new VKUsersArray();
                    for (int q = 0; q < nF; q++) {


                        String str = (String) Input.readObject();

                        VKApiUserFull user = new VKApiUserFull(new JSONObject(str));
                        Log.d("CACHE", "Friend: "+user.last_name);
                        friends.add(user);
                    }

                }



                nF = (Integer) Input.readObject();
                if (nF > 0) {

                    userCache = new VKUsersArray();
                    for (int q = 0; q < nF; q++) {
                        String str = (String) Input.readObject();

                        VKApiUserFull user = new VKApiUserFull(new JSONObject(str));
                        user.last_seen =0;
                        Log.d("CACHE", "User: "+user.last_name);
                        userCache.add(user);
                    }

                }

            }
/*
            {
                InputStream file = new FileInputStream(CacheDir + "dialogs.cache");
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput Input = new ObjectInputStream(buffer);

                Integer nF = (Integer) Input.readObject();
                if (nF > 0) {

                    dialogs = new VKList<VKDialog>();
                    for (int q = 0; q < nF; q++) {
                        dialogs.add((VKDialog) Input.readObject());
                    }

                }
            }
*/


        }
        catch (Exception e) {
            Log.d("CACHE","Error: "+e);
        }
        finally {
            lock.unlock();
        }
    }

    static public void SaveCache(){

        lock.lock();
        try {
            {
                OutputStream file = new FileOutputStream(CacheDir + "users.cache");
                //OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(file);


                if (friends != null) {

                    output.writeObject(new Integer(friends.getCount()));

                    for (VKApiUserFull user : friends) {


                        //output.writeObject(dlg);
                        String str="{";
                        str+="\"id\":"+user.id+",";
                        str+="\"first_name\":\""+user.first_name+"\",";
                        str+="\"last_name\":\""+user.last_name+"\",";
                        str+="\"photo_100\":\""+user.photo_100+"\"}";
                        output.writeObject(str);

                    }
                    Log.d("YOTAVK","Save cache friedns "+friends.size());

                } else
                    output.writeObject(new Integer(0));


                if (userCache != null) {

                    output.writeObject(new Integer(userCache.getCount()));

                    for (VKApiUserFull user : userCache) {

                        String str="{";
                        str+="\"id\":"+user.id+",";
                        str+="\"first_name\":\""+user.first_name+"\",";
                        str+="\"last_name\":\""+user.last_name+"\",";
                        str+="\"photo_100\":\""+user.photo_100+"\"}";

                        output.writeObject(str);
                    }
                    Log.d("YOTAVK","Save cache users "+userCache.size());

                } else
                    output.writeObject(new Integer(0));


                output.flush();
                file.flush();
                file.close();

            }
            /*
            {
                OutputStream file = new FileOutputStream(CacheDir + "dialogs.cache");
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);

                if (dialogs != null) {


                    output.writeObject(dialogs.getCount());

                    for (VKDialog dlg : dialogs) {

                        output.writeObject(dlg.JSON);
                    }
                } else
                    output.writeObject(new Integer(0));

            }*/

        }
        catch (Exception e){}
        finally {
            lock.unlock();
        }


    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        Log.d("YOVK", "Terminate");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();


        SaveCache();

        AppContext = null;

    }

   static Context AppContext=null;

   static void NotifySystem(boolean bNewMsg){

        Context context = AppContext;


       if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("keep_notif",true)) {
          if (!bNewMsg) return;
       }

       if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("post_notif",true))
           return;


        boolean bOK = true;

       final NotificationManager mNotificationManager =
               (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

       //if (bNewMsg)
       //    mNotificationManager.cancelAll();


        int nUnread = 0;
        int nUnreadMsg = 0;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context);


       mBuilder.setSmallIcon(R.drawable.yotavk);

       String UnreadString="";

        MyApplication.lock.lock();
        try {
            if (MyApplication.dialogs!=null) {

                VKList<MyApplication.VKMessage> msgs = new VKList<>();

                for (MyApplication.VKDialog dlg:MyApplication.dialogs){
                    if (dlg.unread>0) {

                        nUnread++;
                        nUnreadMsg+=dlg.unread;

                        if (dlg.chat_id>0) {
                            UnreadString += (UnreadString.length()>0?", ":"")+dlg.message.title;
                        }
                        else
                        {
                            VKApiUserFull user = MyApplication._getUserById(dlg.uid);
                            if (user!=null){
                                UnreadString+=(UnreadString.length()>0?", ":"")+user.first_name+" "+user.last_name;
                            }
                            else
                                UnreadString+=(UnreadString.length()>0?", ":"")+"<Unknown>";

                        }


                        VKList<MyApplication.VKMessage> mmm =dlg.Messages;
                        if ((mmm==null) && (dlg.message!=null)) {mmm = new VKList<>(); mmm.add(new VKMessage(dlg.message,dlg.uid));}

                        if (mmm!=null){

                            for (MyApplication.VKMessage msg:mmm){
                                if ((!msg.out) && (!msg.read_state)){
                                    int q=0;
                                    for (;q<msgs.size();q++){
                                        if (msgs.get(q).date<msg.date)
                                        {
                                            break;
                                        }else
                                        if (msgs.get(q).date==msg.date)
                                        {
                                            if (msgs.get(q).chat_id>msg.chat_id)
                                                break;

                                            if (msgs.get(q).chat_id==msg.chat_id)
                                            {
                                                if (msgs.get(q).user_id>msg.user_id)
                                                    break;

                                            }

                                        }

                                    }
                                    msgs.add(q,msg);



                                }
                            }
                        }

                    }
                }


                //nUnreadMsg = msgs.size();
                // msgs - (0) - newest message

                mBuilder.setNumber(nUnreadMsg);
                mBuilder.setGroup("yovk");
                mBuilder.setGroupSummary(true);

                List<MyApplication.VKMessage> l = null;


                if (msgs.size()>0) {

                    l = msgs.subList(0, Math.min(7, msgs.size()));
                }
                // now we have last messages
                if ((l!=null) && (l.size()>0)){
                    int lastid =0;
                    int user_id =0;

                    Set<Integer> users = new HashSet<Integer>();
                    Set<Integer> chats = new HashSet<Integer>();
                    for (MyApplication.VKMessage msg:l) {
                        user_id =msg.chat_id;
                        users.add(msg.user_id);
                        chats.add(msg.chat_id);
                    }

                    String str="";
                    if (nUnreadMsg==1) {
                        str = context.getString(R.string.newmessages1);
                    }
                    else
                    if (nUnreadMsg>=5) {
                        str = String.format(context.getString(R.string.newmessages5),nUnreadMsg);

                    }
                    else
                    if (nUnreadMsg>=2) {
                        str = String.format(context.getString(R.string.newmessages2),nUnreadMsg);

                    }
                    else
                        str = context.getString(R.string.nomessages);

                    boolean bAddUser = true; // add user info into textLines
                    boolean bAddChat = true; // add chat  info into textLines

                    if (users.size()==1){

                        // one user

                        if ((chats.size()==1) && (user_id>MyApplication.CHAT_UID_OFFSET)) // one user in one chat
                        {
                            VKApiUserFull user = MyApplication._getUserById(l.get(0).user_id);
                            VKDialog dlg = MyApplication.getDialogById(user_id);
                            String chat_name = "";
                            if ((dlg!=null) && (dlg.message!=null)) chat_name = dlg.message.title;

                            if (user!=null) {
                                if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
                                    mBuilder.setLargeIcon(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 250));

                                mBuilder.setContentTitle(user.first_name + " " + user.last_name + " (" + chat_name + ")");
                            }
                            else
                                mBuilder.setContentTitle(chat_name);

                            bAddChat = false;


                        }
                        else // one user personal message or several chats
                        {
                            VKApiUserFull user = MyApplication._getUserById(l.get(0).user_id);

                            if (user != null) {

                                if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
                                    mBuilder.setLargeIcon(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 250));

                                mBuilder.setContentTitle(user.first_name + " " + user.last_name);
                            } else
                                mBuilder.setContentTitle("User#" + user_id);

                        }

                        if (l.size()==1) {

                            mBuilder.setContentText(l.get(0).body);
                        }
                        else
                            mBuilder.setContentText(str);

                        bAddUser = false;
                    }
                    else{

                        // several users


                        str+=" ";
                        if (nUnread==1){

                            str+=context.getString(R.string.chat);
                        }
                        else
                        if (nUnread>0){
                            str+=String.format(context.getString(R.string.chats),nUnread);

                        }

                        //mBuilder.setContentTitle(str); // y mesgs in x chats
                        if (chats.size()==1){
                            // several users in one chat
                            mBuilder.setContentTitle(l.get(0).title+":");
                            mBuilder.setContentText(str);
                        }
                        else {
                            mBuilder.setContentTitle(str);
                            mBuilder.setContentText(UnreadString); // chat names
                        }

                    }


                    if (l.size()>1) {

                        // Add extra lines

                        Collections.reverse(l); // now l(0) - oldest


                        NotificationCompat.InboxStyle inboxStyle =
                                new NotificationCompat.InboxStyle();

                        inboxStyle.setBigContentTitle(str);
                        int lastchat = -1;

                        for (VKMessage msg : l) {

                            String str1="";
                            if (true) {
                                if ((lastid != msg.user_id) || (lastchat!=msg.chat_id)) {
                                VKApiUserFull user = MyApplication._getUserById(msg.user_id);

                                //if (msg.chat_id > MyApplication.CHAT_UID_OFFSET) {
/*
                                    VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                                    String chat_name = "";
                                    if ((dlg!=null) && (dlg.message!=null)) chat_name = dlg.message.title;


                                    if (user != null) {
                                     str1 += user.first_name+" "+user.last_name+"("+chat_name + "): ";}
                                    else
                                     str1 += chat_name + ":";
*/
                                    if (bAddUser) {

                                        if (user != null) {
                                            str1 += user.first_name + " " + user.last_name;
                                        } else
                                            str1 += "User#" + msg.user_id;
                                        if ((bAddChat) && (msg.chat_id > MyApplication.CHAT_UID_OFFSET)){

                                            VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                                            if ((dlg!=null) && (dlg.message!=null)) str1+= "("+dlg.message.title+")";

                                        }
                                        str1+=": ";
                                        inboxStyle.addLine(str1);

                                    }
                                    else
                                    if (bAddChat){

                                        if (msg.chat_id > MyApplication.CHAT_UID_OFFSET){

                                            VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                                            if ((dlg!=null) && (dlg.message!=null)) str1+= dlg.message.title+": ";
                                        }

                                    }



                                   lastid = msg.user_id;
                                    lastchat = msg.chat_id;
                                }else{
                                    str+=" ";
                                }
                            }
                            inboxStyle.addLine(msg.body);
                        }

                        mBuilder.setStyle(inboxStyle);
                    }


                    // Notify

                    String url = "https://vk.com/im";
                    if (nUnread==1){
                        if (l.get(0).chat_id>CHAT_UID_OFFSET)
                         url+="?sel=c"+(l.get(0).chat_id-CHAT_UID_OFFSET);
                        else
                          url+="?sel="+l.get(0).chat_id;
                    }
                    Uri uri = Uri.parse(url);
                    Intent intent1 = new Intent(context,MainActivity.class);
//                    intent1.setPackage(context.getPackageName());
                    intent1.setData(uri);
                    intent1.setAction(Intent.ACTION_VIEW);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    //(int)(System.currentTimeMillis()/100)
                    PendingIntent p = PendingIntent.getActivity(context.getApplicationContext(), 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

                    mBuilder.setContentIntent(p);


                    //mBuilder.addAction(R.drawable.yotavk, "Open", )
                    //intent1.setData(null);
                    //p = PendingIntent.getActivity(context.getApplicationContext(), 1, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

//                    mBuilder.addAction(R.drawable.yotavk,"To E-INK",p);


/*                    Bundle bundle = new Bundle();
                    bundle.putString("android:title",)
                    mBuilder.setExtras(bundle);*/


                   // mBuilder.setOnlyAlertOnce(true);

                   // mBuilder.setPriority(Notification.PRIORITY_HIGH);
                   // mBuilder.setVibrate(new long[]{0, 300});

                    //mBuilder.setOnlyAlertOnce(true);


                    mNotificationManager.notify(1, mBuilder.build());
                    Log.d("YOTAVK", "Add group notifcation");


                    if (bNewMsg) {
                        mBuilder =
                                new NotificationCompat.Builder(context);

                        mBuilder.setSmallIcon(R.drawable.yotavk);

                        VKMessage msg = l.get(l.size() - 1);
                        VKApiUserFull user = MyApplication._getUserById(msg.user_id);
                        VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                        String title = "";

                        if (user != null) {

                            if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
                                mBuilder.setLargeIcon(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 250));

                            title = user.first_name + " " + user.last_name;
                        } else {
                            title = "User#" + msg.user_id;
                        }
                        if (msg.chat_id > CHAT_UID_OFFSET) {
                            if (dlg != null) {

                                title += " (" + dlg.message.title + ")";
                            }
                        }

                        //mBuilder.setGroup("yovk");
                        mBuilder.setContentTitle(title + ":");
                        mBuilder.setContentText(msg.body);

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

                        mNotificationManager.notify(2, mBuilder.build());

                    }


                    bOK = true;


                } // l.size()>0


            }


        }
        finally {
            MyApplication.lock.unlock();
        }








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
           Log.d("YOTAVK","Clear notifcations");
           mNotificationManager.cancelAll();

       }

       context.startService(new Intent("CANCEL_NOTIF", null, context, BSWidget.WidgetService.class));


    }


}