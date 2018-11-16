package com.zakharchenko.yotavk.Data;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Observable;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.httpClient.VKHttpClient;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKUsersArray;
import com.zakharchenko.yotavk.BuildConfig;
import com.zakharchenko.yotavk.GCM.RegistrationIntentService;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.Presenter.Presenter;
import com.zakharchenko.yotavk.R;
import com.zakharchenko.yotavk.Utils.Utils;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zakharchenko on 08.11.2018.
 */
public class VKData {

    final String TAG = "VKDATA";

    public VKData(String CacheDir){
        this.CacheDir = CacheDir;




        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {

                    try {

                        final ServerRequest req = queue.poll();

                        if (req != null) {

                            Log.d(TAG, "Poll Request: " + req.methodName + " with " + req.listeners.size() + " listners");



                            req.executeSyncWithListener(new VKRequest.VKRequestListener() {
                                @Override
                                public void onComplete(VKResponse response) {
                                    super.onComplete(response);
                                    for (VKRequest.VKRequestListener listener : req.listeners)
                                        listener.onComplete(response);
                                }

                                @Override
                                public void onError(VKError error) {
                                    super.onError(error);
                                    if (error.apiError.errorCode == 6) {
                                        req.startAt = System.currentTimeMillis() + REQUEST_DELAY;
                                        queue.add(req);
                                    } else
                                        for (VKRequest.VKRequestListener listener : req.listeners)
                                            listener.onError(error);

                                }
                            });

                        }

                        Thread.sleep(REQUEST_DELAY / 4);


                    } catch (Exception e)
                    {

                    }
                }
            }
        }).start();
    }


    /* Public Memers */

    public boolean IsLogged(){
        return VKSdk.isLoggedIn();
    }


    public synchronized List<VKDialog> getDialogs(){

        if (ThreadID!=Thread.currentThread().getId())
        {
            ArrayList<VKDialog> ret = new ArrayList<>();
            for (VKDialog dlg : dialogs)
                ret.add(VKDialog.COPY(dlg));

            return ret;

        }

        return  dialogs;
        /*
        ArrayList<VKDialog> ret = new ArrayList<>();
        try {
            lock.lock();
            for (VKDialog dlg : dialogs)
             ret.add(VKDialog.COPY(dlg));
         }
        finally {
            lock.unlock();
        }

        return ret;
        */
    }

    // return friends from cache
    public  List<VKApiUserFull> getFriends(){
        /*
        try {
            lock.lock();
            return new ArrayList<VKApiUserFull>(friends.values());
        }
        finally {
            lock.unlock();
        }*/
        ArrayList<VKApiUserFull> arr = new ArrayList<VKApiUserFull>();
        for (int i: friends){
            arr.add(getUserById(i));
        }

        return arr;

    }

    public synchronized void RegisterListener(Notify listener){
        listeners.add(listener);
    }

    public synchronized void UnregisterListener(Notify listener){
        listeners.remove(listener);
    }


    final long ThreadID = Thread.currentThread().getId();
    // return dialog by id from cache
    public synchronized VKDialog getDialog(int uid){
            VKDialog dlg = getDialogById(uid);
            if (dlg==null) return null;
            if (Thread.currentThread().getId() != ThreadID)
                return VKDialog.COPY(dlg);
            return dlg;
        //return (dlg!=null?VKDialog.COPY(dlg):null);
    }

    // starts reloading messages for dialog and returns dialog instance (not jet refreshed)
    public synchronized VKDialog getDialogByIdWithMessages(int uid){

        VKDialog dlg = getDialogById(uid);
        if (dlg==null)
            dlg = CreateDialog(uid);
        GetMessages(uid, null);
/*
        VKDialog ret=null;
        lock.lock();
        try {
            ret = VKDialog.COPY(dlg);
        }
        finally {
            lock.unlock();
        }
        return ret;
  */
        if (ThreadID!=Thread.currentThread().getId())
             return VKDialog.COPY(dlg);
        return dlg;
    }


    // Load user information from cache
    public VKApiUserFull getUserById(int uid){return (getUserById(uid,true));}


    public void RefreshDialogs(){

        Log.d("DIALOGS","Gettings dialogs");

        final VKParameters req = VKParameters.from(VKApiConst.COUNT, ""+ MAX_DIALOGS);
        AddCallRequest(VKApi.messages().getDialogs(req).methodName, req, _DialogsRefreshListener);
    }





    /* Protected members */



    String CacheDir;
    /* class VKApiUserFullCache ads LastLoaded to VKApiUserFull */
    public static class VKApiUserFullCache extends VKApiUserFull
    {
        public VKApiUserFullCache(JSONObject from) throws JSONException {
            super(from);
            LastLoaded = System.currentTimeMillis();
        }
        public VKApiUserFullCache(VKApiUserFull user){

        }
        public static VKApiUserFullCache FromUser(VKApiUserFull user){
            Parcel p = Parcel.obtain();
            user.writeToParcel(p, 0);
            p.setDataPosition(0);
            VKApiUserFullCache retuser =  new VKApiUserFullCache(p);
            return retuser;
        }

        public VKApiUserFullCache(Parcel p){
            super(p);
        }
        @Override
        public int hashCode(){return this.id;}
        public long LastLoaded; // last loaded from server time stamp
    }



    // Cache of users VKApiUserFullCache
    class UsersCache extends ConcurrentHashMap<Integer,VKApiUserFullCache> {

        public VKApiUserFull getById(int id) {return this.get(id);}
        public void parse(JSONObject obj){
            VKUsersArray arr = new VKUsersArray();
            try {
                arr.parse(obj);
                for (VKApiUserFull user: arr){
                    put(user.id,VKApiUserFullCache.FromUser(user));
                }
            }
            catch (JSONException e){
                Log.e("USERS", e.toString());
            }
        }
        public void add(VKApiUserFullCache user){
            put(user.id,user);
        }
        public void addAll(UsersCache cache){
            for (int i: cache.keySet()){
                put(i,cache.get(i));
               //VKApiUserFullCache user = userCache.get(i);
               //if (user!=null) put(i,cache.get(i));
            }
        }
    }


    /* Friend id's cache */
    @NonNull ArrayList<Integer> friends=new ArrayList<>();

    /* Other users cache */
    @NonNull UsersCache userCache= new UsersCache();


    /* Cached dialogs list */
    @NonNull VKList<VKDialog> dialogs=new VKList<>() ;

    ReentrantLock lock = new ReentrantLock();
/*    class StubLock{
    void lock(){}
    void unlock(){}
    };
    StubLock lock = new StubLock();
*/
    public interface Notify{
        public void OnDataLoaded(Class s,int data);
    }
    HashSet<Notify> listeners = new HashSet<>();


    synchronized void  NotifySystem(Class s,int data){
        Log.d(TAG,"NotifySystem "+s.toString()+" "+data+" "+listeners.size()+" listeners");
        // Send notification to Application
        for (Notify list : listeners)
         list.OnDataLoaded(s,data);
    }

    class ServerRequest extends VKRequest implements Delayed{
        long startAt;
         ArrayList<VKRequestListener> listeners = new ArrayList<>();

        public  ServerRequest(String method,VKParameters params, VKRequestListener listener){
            super(method,params);
            startAt = System.currentTimeMillis()+REQUEST_DELAY;
            listeners.add(listener);
        }
        @Override
        public long getDelay(TimeUnit unit) {
            return (unit.convert(startAt-System.currentTimeMillis(),TimeUnit.MILLISECONDS));
        }

        @Override
        public int compareTo(Delayed another) {
            return Long.valueOf(startAt).compareTo(((ServerRequest)another).startAt);
        }

        public void AddListener(VKRequestListener listener){
            if (!listeners.contains(listener))
             listeners.add(listener);
        }

    }
    ConcurrentLinkedDeque<ServerRequest> queue = new ConcurrentLinkedDeque<>();

    /*DelayQueue<ServerRequest> queue = new DelayQueue<ServerRequest>(){

        @Override
        public boolean contains(Object object) {
            for (ServerRequest req : this){
                if (req.methodName.equals(((ServerRequest)object).methodName))
                    return true;
            }

            return false;
        }

    };*/

    boolean IsParamsEqual(VKParameters params1,VKParameters params2){
        /*StringBuilder p1=new StringBuilder(),p2=new StringBuilder();
        for (String key:params1.keySet()){
         p1.append(key+"="+params1.get(key).toString()+";");
        }
        for (String key:params2.keySet()){
            p2.append(key+"="+params2.get(key).toString()+";");
        }
        return p1.toString().equals(p2.toString());
*/
        return params1.toString().equals(params2.toString());
    }
    void AddCallRequest(String methodName,VKParameters params,VKRequest.VKRequestListener listener){

        lock.lock();
        try {
            ServerRequest resreq = null;

            for (ServerRequest req : queue) {
                if ((req.methodName.equals(methodName)) && (IsParamsEqual(params,req.getMethodParameters())))
                    resreq = req;

            }
            if (resreq==null) {
                resreq = new ServerRequest(methodName,params,listener);
                Log.d("VKData","Enqueue Request: "+resreq.methodName);
                queue.add(resreq);
            }
            else {
                Log.d("VKData","Found Request: "+resreq.methodName);
                resreq.AddListener(listener);
            }
        }
        finally {
            lock.unlock();

        }
    }

    /* load list of friends of current user from server */
    void GetFriends(final VKRequest.VKRequestListener listener){

        final VKParameters req = VKParameters.from("order", "hints", "count", "500", "fields", "photo_50,photo_100,nickname,last_seen");


        AddCallRequest(VKApi.friends().get(req).methodName,req,new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);


                Log.d("FRIENDS", "Resp: " + response.responseString);


                lock.lock();
                try {

                    UsersCache _friends = new UsersCache();
                    _friends.parse(response.json);

                    userCache.addAll(_friends);

                    friends = new ArrayList<Integer>(_friends.keySet());

                } catch (Exception e) {
                } finally {
                    lock.unlock();
                }

                SaveCache();

                RefreshUserCache();

                NotifySystem(VKApiUserFull.class, 0);

                if (listener != null)
                    listener.onComplete(null);

            }

            @Override
            public void onError(VKError error) {
                super.onError(error);

                Log.d("FRIENDS", "Error: " + error);

                if (error.apiError.errorCode == 6) {
                    final VKRequest.VKRequestListener listener = this;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            VKApi.friends().get(req).executeWithListener(listener);
                        }
                    }, REQUEST_DELAY);
                }


                if (listener != null)
                    listener.onError(error);
            }
        });

    }


    /* Load user photo async and add it to the image cache */
    /* After loading send VKUSERSCHANGED_BROADCAST broadcast */
    void LoadUserPhoto(final int uid,final String urlstr){

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
                    Utils.copyInputStreamToFile(input, new File(CacheDir, "img_cache/img_user_" + uid + ".jpg"));
                    Log.d("USER", "IMAGE Loaded "+uid+" "+urlstr);

                } catch (Exception e) {
                    Log.i("YOTAVK", "HTTP error " + e);
                }
                Thread thread;


                NotifySystem(VKApiUserFull.class, uid);
                //if (AppContext != null)
                //    AppContext.sendBroadcast(new Intent(VKUSERSCHANGED_BROADCAST));

            }
        }).start();

    }

    final static int CACHE_RELOAD_TIME = 10000000;

    final static int MAX_USERS = 500;      // maximum displayed users in app

    final static int REQUEST_DELAY = 2000; // Delay in ms to repeat request to the server


    /* Reloads users information from server for those users that
     were loaded earlier than CACHE_RELOAD_TIME ms ago
      */

    VKRequest.VKRequestListener _UsersRefreshListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            super.onComplete(response);
            try {
                Log.d("USERS", response.json.toString());

                JSONArray arr = response.json.getJSONArray("response");
                for (int i = 0; i < arr.length(); i++) {
                    String user_photo_100 = "";
                    VKApiUserFullCache _user = new VKApiUserFullCache(arr.getJSONObject(i));
                    lock.lock();
                    try {
                        userCache.add(_user);
                        user_photo_100 = _user.photo_100;
                    } catch (Exception e) {

                    } finally {
                        lock.unlock();
                    }

                    if (user_photo_100.length() > 0) {
                        Log.d("USER", "Loading photo " + user_photo_100 + "...");
                        LoadUserPhoto(_user.id, user_photo_100);
                    }

                }
            } catch (Exception e) {
            }

            SaveCache();
            NotifySystem(VKApiUserFull.class, 0);

        }
    };

    VKRequest.VKRequestListener _GroupsRefreshListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            super.onComplete(response);
            try {
                Log.d("GROUPS", response.json.toString());

                JSONArray arr = response.json.getJSONArray("response");
                for (int i = 0; i < arr.length(); i++) {
                    String user_photo_100 = "";
                    VKApiUserFullCache _user = new VKApiUserFullCache(arr.getJSONObject(i));
                    _user.id *= -1; // for group id is negative
                    lock.lock();
                    try {
                        VKApiUserFull user = null;
                        user = getUserById(_user.id, false);
                        if (user != null) {
                            user.parse(arr.getJSONObject(i));
                            user.id *= -1;
                            user_photo_100 = user.photo_100;
                            user.first_name = arr.getJSONObject(i).optString("name");
                            if (user.getClass() == VKApiUserFullCache.class) {
                                ((VKApiUserFullCache) user).LastLoaded = System.currentTimeMillis();
                            }
                        }
                    } catch (Exception e) {

                    } finally {
                        lock.unlock();
                    }

                    SaveCache();

                    if (user_photo_100.length() > 0) {
                        Log.d("USER", "Loading photo " + user_photo_100 + "...");
                        LoadUserPhoto(_user.id, user_photo_100);
                    }

                    NotifySystem(VKApiUserFull.class, _user.id);
                }
            } catch (Exception e) {
            }
        }
    };

    void RefreshUserCache(){

        final StringBuilder group_ids = new StringBuilder();
        final StringBuilder user_ids = new StringBuilder();

        for(VKApiUserFullCache user : userCache.values()){
            if (System.currentTimeMillis()-user.LastLoaded>CACHE_RELOAD_TIME){
                if (user.id>=0)
                    user_ids.append((user_ids.length()>0?",":"")+user.id);
                else
                    group_ids.append((group_ids.length()>0?",":"")+(-user.id));
            }
        }

        /*for(VKApiUserFullCache user : friends.values()){
            if (System.currentTimeMillis()-user.LastLoaded>CACHE_RELOAD_TIME){
                if (user.id>=0)
                    user_ids.append((user_ids.length()>0?",":"")+user.id);
                else
                    group_ids.append((group_ids.length()>0?",":"")+(-user.id));
            }
        }*/

        if (user_ids.toString().length()>0) {
            final VKParameters req = VKParameters.from(VKApiConst.USER_IDS, "" + user_ids.toString(), VKApiConst.FIELDS, "photo_id,photo_100,photo_50,is_friend,last_seen");

            AddCallRequest(VKApi.users().get(req).methodName, req, _UsersRefreshListener);
        }
        if (group_ids.toString().length()>0) {
            final VKParameters req = VKParameters.from("group_ids", "" + group_ids.toString(), VKApiConst.FIELDS, "photo_id,photo_100,photo_50");
            AddCallRequest(VKApi.groups().getById(req).methodName, req, _GroupsRefreshListener);

        }
    }


    //private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            // Load user information from cache
    VKApiUserFull getUserById(final int uid,boolean bAdd){
        VKApiUserFull user = null;
        lock.lock();
        try {
            boolean bFromCache = false;

            /*if (friends!=null) {
                user = friends.getById(uid);
            }*/

            if (userCache!=null)
            {
                user = userCache.getById(uid);
                bFromCache = true;
            }

            if ((user==null) && (bAdd))//(friends!=null) && ((user==null) || ((user.last_seen==0) && bFromCache)) && (bAdd)){
            {
                //if (user==null) {
                    user = new VKApiUserFull();
                    user.id = uid;
                    user.first_name = "User#" + uid;
                    user.last_name = "";
                    userCache.put(uid, VKApiUserFullCache.FromUser(user));

                    //RefreshUserCache();
               // }

            }

        }
        finally {
            lock.unlock();
        }


        return  user;
    }


    /*
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

    */

    VKDialog getDialogById(int uid){
        VKDialog dlg = null;
        lock.lock();
        try {


                for (VKDialog d:dialogs){
                    if (d.uid==uid)
                    {dlg = d; break;}
                }


        }
        finally {
            lock.unlock();
        }
      return  dlg;
    }


    /* Called after message history updated in order to perform sort on clinet
    instead of usual sort on server
    uid - id of dialog with refreshed messages
    */
    void ArrangeDialogsWithMessages(int uid){

    }
    /* Get messages list for some user with uid*/
    void GetMessages(final int uid,final VKRequest.VKRequestListener listener){

        Log.d("MESSAGES", "Get for " + uid);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final VKRequest request = new VKRequest("messages.getHistory", VKParameters.from("user_id",uid<VKDialog.CHAT_UID_OFFSET?""+uid:"","peer_id",""+uid,VKApiConst.COUNT,"200","v","5.50"));

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

                        if (obj.optJSONObject("error").getInt("error_code")==6){
                            // TRY AGAIN
                            Log.d("MESSAGES", "Too many request per second. waiting...");
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    GetMessages(uid,listener);
                                }
                            },2000);

                        }
                        else {
                            if (listener != null)
                                listener.onError(new VKError(obj));
                        }
                    }
                    else {

                        lock.lock();
                        try {
                            VKDialog dlg = getDialogById(uid);
                            getUserById(uid);

                            dlg.Messages = new VKList<VKMessage>(obj, VKMessage.class);
                            //Collections.reverse(dlg.Messages);

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
                                dlg.message = dlg.Messages.get(0);// newest
                                dlg.message.title = title;


                            }

                            Collections.sort(dialogs);
                            if (HasNewMessageNotifiction)
                             DetectNewMessage();
                            HasNewMessageNotifiction = false;
                        }
                        catch(Exception e){

                        }
                        finally {
                            lock.unlock();
                        }

                        RefreshUserCache();

                        NotifySystem(VKDialog.class,uid);

                        if (listener != null)
                            listener.onComplete(null);


                    }

                }
                catch (Exception e){}

            }
        }).start();

    }

    final static int MAX_DIALOGS = 200; // Maximum number of last dialogs shown in app

    //public static boolean bDoGetMessages = false;

    int LastReceivedMessageId=0;


    VKRequest.VKRequestListener _DialogsRefreshListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            //Do complete stuff
            Log.d("DIALOGS", response.json.toString());

            int nUnread = 0;
            lock.lock();
            try {


                VKList<VKDialog> _dialogs = new VKList<VKDialog>(response.json.optJSONObject("response").optJSONArray("items"), VKDialog.class);
                // add message information
                for (VKDialog dlg : _dialogs) {

                    VKDialog olddlg = getDialogById(dlg.uid);
                    // adding users to cache
                    getUserById(dlg.uid, true);

                    if ((olddlg != null) && (olddlg.Messages != null)) {
                        dlg.Messages = olddlg.Messages;
                    }


                    nUnread += dlg.unread;
                }

                dialogs = _dialogs;


                DetectNewMessage();

            } finally {

                lock.unlock();
            }


            NotifySystem(VKDialog.class, 0);

            RefreshUserCache();


        }

    };

    public void SendMessage(int uid,String text) {
        SendMessage(uid, text, null);
    }


    void DetectNewMessage(){
        // Detect newly received message
        int lastnonread = 0;
        if (dialogs.size()>0) {

            if ((dialogs.get(0).message!=null) && (dialogs.get(0).message.read_state == false) && (dialogs.get(0).message.out == false))
                lastnonread = dialogs.get(0).message.id;

            if (lastnonread > 0) {
                if (LastReceivedMessageId != lastnonread) {

                    // new message received
                    NotifySystem(VKMessage.class, dialogs.get(0).uid);
                }
                LastReceivedMessageId = lastnonread;
            }
        }

    }

    /* Send message to client uid from current user Async*/
    synchronized boolean SendMessage(final int uid,final String text,final VKRequest.VKRequestListener listener){

        if (!VKSdk.isLoggedIn()){
            return (false);
        }

        // search for dialog
        VKDialog dlg = null;
//        lock.lock();
        try {
            dlg = getDialogById(uid);

            if (dlg==null){
                if (uid<VKDialog.CHAT_UID_OFFSET) {

                    //dlg = new VKDialog();
                    //dlg.uid = uid;
                    //dlg.chat_id = 0;
                    //dlg.Messages = new VKList<>();
                    dlg = CreateDialog(uid);


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

                if (dlg.Messages.size()>0)
                 dlg.Messages.addBefore(0, msg);
                else
                 dlg.Messages.add(msg);
                dlg.message = msg;

                //VKRequest req = new VKRequest("messages.send",VKParameters.from(VKApiConst.USER_ID,));
            }
        }
        finally {
  //          lock.unlock();
        }

        if (dlg!=null){

            NotifySystem(VKDialog.class,uid);

            final VKRequest request = new VKRequest("messages.send", VKParameters.from("user_id",((uid<VKDialog.CHAT_UID_OFFSET) && (uid>0))?""+uid:"","peer_id",""+uid,VKApiConst.MESSAGE,text,"random_id",Math.round((Math.random()*100000))+""));
            Log.d("MESSAGES","Send message: "+request.toString());
            AddCallRequest(request.methodName, request.getMethodParameters(), new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);

                    Log.d("MESSAGES", "Complete " + response.json.toString());

                    lock.lock();
                    try {
                        VKDialog dlg = getDialogById(uid);
                        //dlg.Messages = new VKList<VKApiMessage>(obj, VKApiMessage.class);
                        //Collections.reverse(dlg.Messages);
                        if ((dlg.Messages != null) && (dlg.Messages.size() > 0)) {
                            VKMessage msg = dlg.Messages.get(0);
                            if (msg.body.equals(text)) {

                                msg.id = response.json.getInt("response");
                            }
                        }

                    } catch (Exception e) {
                    } finally {
                        lock.unlock();
                    }

                    NotifySystem(VKDialog.class, uid);

                    if (listener != null)
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
                        if ((dlg.Messages != null) && (dlg.Messages.size() > 0)) {
                            VKMessage msg = dlg.Messages.get(0);
                            if (msg.body.equals(text)) {

                                dlg.Messages.remove(0);
                                if (dlg.Messages.size() > 0)
                                    dlg.message = dlg.Messages.get(0);
                                else
                                    dialogs.remove(dlg);
                            }


                        }
                    } finally {
                        lock.unlock();
                    }

                    NotifySystem(VKDialog.class, uid);

                    if (listener != null)
                        listener.onError(error);
                }
            });


        }


        return (dlg!=null);
    }

   /* Add new GCM message to dialog, called from GCM
   public void AddNewMessage(int dialogID,int messageId,String text){
      lock.lock();
      try {
          VKDialog dialog = getDialogById(dialogID);
          if (dialog != null) {

              VKMessage message = new VKMessage();
              message.id = messageId;
              message.out = false;
              message.body = text;
              message.date = new Date().getTime()/1000;
              message.read_state = false;
              dialog.message = message;
              dialog.unread++;
              dialog.Messages.add(message);
              // aa to the top
              if (dialogs.size()>1) {
                  dialogs.remove(dialog);
                  dialogs.addBefore(0,dialog);
              }

          }
      }
      finally {
          lock.unlock();
      }

      // reload messages
    //    getDialogByIdWithMessages(dialogID);
      RefreshDialogs();

      NotifySystem(VKMessage.class,dialogID);

   }
*/

   // Create new chat, waits until it is created and return VKDialog of it or null if not created
    // If chat is not created within REQUEST_DELAY*3 ms returns null but still tries to create
    public void CreateChat(final String name, final int[] userIds, final Notify notify){

        CreateChat(name, userIds, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                try {
                    int ChatID = response.json.getInt("response")+VKDialog.CHAT_UID_OFFSET;
                    CreateDialog(ChatID);
                    notify.OnDataLoaded(VKDialog.class,ChatID);
                }
                catch (Exception e){

                    Log.e("DATA",e.toString());
                }

               RefreshDialogs();

            }

            @Override
            public void onError(VKError error) {
                super.onError(error);

                if (error.apiError.errorCode==6){

                    final VKRequest.VKRequestListener listener = this;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                          CreateChat(name,userIds,listener);
                        }
                    },REQUEST_DELAY);

                }
                notify.OnDataLoaded(VKDialog.class,0);

            }
        });

    }

    /* Start new chat with user(s) */
    void CreateChat(String name,int[] userIds, final VKRequest.VKRequestListener listener){

        Log.d("DIALOGS","Creating chat "+name+" ids: "+userIds);

        String userIDsStr = "";
        int c=0;
        for (int i:userIds){
            userIDsStr+=(c==0?"":",")+i;
            c++;
        }


        AddCallRequest("messages.createChat",VKParameters.from("user_ids",userIDsStr,"title",name),listener);

    }


    // Create new dialog if it doesn't extist
    @Nullable VKDialog CreateDialog(int uid){

        if (uid==0){

            Log.e(TAG,"Create dialog 0!");
            return null;
        }

        Log.d(TAG,"Create dialog "+uid);
        VKDialog dlg = new VKDialog();
        dlg.message = null;
        dlg.uid = uid;
        lock.lock();
        try {

            dialogs.add(dlg);
        }
        finally {

            lock.unlock();
        }
        return (dlg);
    }

    /* Mark all messages of dialog with DialogID read and send broadcast VKREADCHANGED_BROADCAST*/

   public void MarkAsRead(final int DialogId){

        VKDialog dlg = getDialogById(DialogId);
        if ((dlg!=null) && (dlg.unread>0)){

            dlg.unread = 0;
            String ids = "";

            for (VKMessage msg : dlg.Messages){
                if (!msg.read_state){
                    msg.read_state = true;
                    ids+=(ids.length()==0?"":",")+msg.getId();
                }
            }

            NotifySystem(VKDialog.class, DialogId);

            final VKParameters req = VKParameters.from("peer_id", "" + DialogId, "message_ids", ids);

            if (ids.length()>0){

                AddCallRequest("messages.markAsRead", req, new VKRequest.VKRequestListener() {
                    @Override
                    public void onError(VKError error) {
                        if (error.apiError.errorCode == 6) {

                            final VKRequest.VKRequestListener listener = this;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    new VKRequest("messages.markAsRead", req).executeWithListener(listener);

                                }
                            }, REQUEST_DELAY);

                        }

                    }

                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);


                        NotifySystem(VKDialog.class, DialogId);

                    }
                });
            }
        }
    }



    /* Broadcast send when some messages or dialogs were changed */
    public static String VKRECORDSCHANGED_BROADCAST="com.zakharchenko.yotavk.VKRECORDSCHANGED_BROADCAST";
    /* Browadcast send when some user information was changed */
    public static String VKUSERSCHANGED_BROADCAST="com.zakharchenko.yotavk.VKUSERSCHANGED_BROADCAST";


    boolean HasNewMessageNotifiction = false;
    public void notifyNewMessage(int uid){

        HasNewMessageNotifiction = true;
        getDialogByIdWithMessages(uid);
        // DetectNewmMssage() will be called after information is received from server;
    }

    public void init(){


        LoadCache();

        if (VKSdk.isLoggedIn()) {

            RefreshDialogs();

            GetFriends(null);

            RefreshUserCache();
        }
    }

    /* Load users cache from file*/
     void LoadCache(){

         friends = new ArrayList<>();
         userCache = new UsersCache();



        lock.lock();

        try{

            {
                InputStream file = new FileInputStream(CacheDir + "users3.cache");
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput Input = new ObjectInputStream(buffer);

                friends = (ArrayList<Integer>) Input.readObject();


                int nF = (Integer) Input.readObject();
                if (nF > 0) {

                    userCache = new UsersCache();
                    for (int q = 0; q < nF; q++) {
                        String str = (String) Input.readObject();

                        VKApiUserFullCache user = new VKApiUserFullCache(new JSONObject(str));
                        user.LastLoaded = System.currentTimeMillis()-VKData.CACHE_RELOAD_TIME; // new JSONObject(str).getLong("last_loaded");
                        user.last_seen =0;
                        Log.d("CACHE", "User: "+user.last_name);
                        userCache.add(user);
                    }

                }

            }


        }
        catch (Exception e) {
            Log.d("CACHE","Error: "+e);
        }
        finally {
            lock.unlock();
        }
    }

    /* Save users cache to file*/
    public void SaveCache()
    {
        lock.lock();
        try {
            {
                OutputStream file = new FileOutputStream(CacheDir + "users3.cache");
                //OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(file);


                //if (friends != null) {

                    output.writeObject(friends);
                  //  Log.d("YOTAVK","Save cache friedns "+friends.size());
//
                //} else
                 //   output.writeObject(new Integer(0));


                if (userCache != null) {

                    output.writeObject(new Integer(userCache.size()));

                    for (VKApiUserFullCache user : userCache.values()) {

                        String str="{";
                        str+="\"id\":"+user.id+",";
                        str+="\"first_name\":\""+user.first_name+"\",";
                        str+="\"last_name\":\""+user.last_name+"\",";
                        str+="\"photo_100\":\""+user.photo_100+"\",";
                        str+="\"last_loaded\":\""+user.LastLoaded+"\"}";

                        output.writeObject(str);
                    }
                    Log.d("YOTAVK","Save cache users "+userCache.size());

                } else
                    output.writeObject(new Integer(0));


                output.flush();
                file.flush();
                file.close();

            }

        }
        catch (Exception e){}
        finally {
            lock.unlock();
        }


    }


}
