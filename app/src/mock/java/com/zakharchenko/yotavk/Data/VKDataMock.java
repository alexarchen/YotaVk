package com.zakharchenko.yotavk.Data;

import android.app.Dialog;
import android.os.Handler;
import android.os.Parcel;
import android.util.Log;
import android.view.Display;

import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zakharchenko on 09.11.2018.
 */
public class VKDataMock extends VKData{

   // ArrayList<VKApiUserFull> users;
   // ArrayList<VKApiUserFull> friends;


    public final static int WAIT_DELAY = 2000;
    public int MSGID = 1;
    public int CHATID = 1;

    public VKDataMock(){
        super("");

    }


    final static String TAG = "VKData";

    @Override
    void LoadCache() {

        Log.d(TAG,"LoadCache");

        VKApiUserFull user = new VKApiUserFull();
        user.first_name = "Friend";
        user.last_name = "Tested";
        user.id = 1;
        userCache.add(VKApiUserFullCache.FromUser(user));
        friends.add(user.id);
        user = new VKApiUserFull();
        user.first_name = "User";
        user.last_name = "Tested";
        user.id = 2;
        userCache.add(VKApiUserFullCache.FromUser(user));
        user = new VKApiUserFull();
        user.first_name = "My Group";
        user.last_name = "";
        user.id = -1;
        userCache.add(VKApiUserFullCache.FromUser(user));
        int me = 1000; // my ID
        // Add some friends
        for (int q=0;q<15;q++) {
            user = new VKApiUserFull();
            user.first_name = "Friend"+q;
            user.last_name = "";
            user.id = 3+q;
            userCache.add(VKApiUserFullCache.FromUser(user));
            friends.add(user.id);
        }


        // simple dialog from 2-nd user
        VKDialog dialog = new VKDialog();
        dialog.uid = 2;
        dialog.chat_id = 0;
        dialog.Messages = new VKList<>();
        VKMessage msg = new VKMessage();
        msg.body="Test";
        msg.id = MSGID++;
        msg.user_id = dialog.uid;
        dialog.Messages.add(msg);
        msg = new VKMessage();
        msg.body="Passed message to test VKData";
        msg.id = MSGID++;
        msg.user_id = me;
        msg.out = true;
        dialog.Messages.addBefore(0,msg);
        msg = new VKMessage();
        msg.body="Test message to test VKData OK";
        msg.id = MSGID++;
        msg.user_id = dialog.uid;
        dialog.Messages.addBefore(0,msg);
        msg = new VKMessage();
        msg.body="Passed message";
        msg.id = MSGID++;
        msg.user_id = me;
        msg.out = true;
        dialog.Messages.addBefore(0,msg);
        msg = new VKMessage();
        msg.body="Tested method done\nOk this is OK OK\nEverything is OK";
        msg.id = MSGID++;
        msg.user_id = dialog.uid;
        dialog.Messages.addBefore(0, msg);
        msg = new VKMessage();
        msg.body="Passed one look up\nDon't miss\ndon't tell\nPlease, try try try...";
        msg.id = MSGID++;
        msg.user_id = me;
        msg.out = true;
        dialog.Messages.addBefore(0,msg);
        dialog.message = dialog.Messages.get(0);
        dialogs.add(dialog);

        // dialog from chat with 2 users
        dialog = new VKDialog();
        dialog.chat_id = CHATID++;
        dialog.uid = VKDialog.CHAT_UID_OFFSET+dialog.chat_id;
        dialog.Messages = new VKList<>();
        msg = new VKMessage();
        msg.body="From 1-st";
        msg.id = MSGID++;
        msg.user_id = 1;
        msg.title = "Test Chat";
        dialog.Messages.add(msg);
        msg = new VKMessage();
        msg.body="From 2-nd";
        msg.id = MSGID++;
        msg.user_id = 2;
        msg.read_state = true;
        msg.title = "Test Chat";
        dialog.Messages.addBefore(0,msg);
        dialog.message = dialog.Messages.get(0);
        dialogs.add(dialog);

        // dialog from Group
        dialog = new VKDialog();
        dialog.chat_id = 0;
        dialog.uid = -1;
        dialog.Messages = new VKList<>();
        msg = new VKMessage();
        msg.body="This is Test";
        msg.id = MSGID++;
        msg.user_id = dialog.uid;
        dialog.Messages.add(msg);
        msg = new VKMessage();
        msg.body="From 2-nd";
        msg.id = MSGID++;
        msg.user_id = -1;
        dialog.Messages.addBefore(0, msg);
        dialog.message = dialog.Messages.get(0);
        dialogs.add(dialog);

        for (VKDialog dlg:dialogs){
            int nR=0;
            for (VKMessage _msg:dlg.Messages){
                if (!_msg.read_state) nR++;
            }
            dlg.unread = nR;
        }
    }

    @Override
    public void SaveCache() {

    }

    @Override
    public void CreateChat(String name, int[] userIds, final Notify notify)
    {

        Log.d(TAG, "CreateChat " + name);

        final VKDialog dialog = new VKDialog();

        lock.lock();

        try {
            dialog.Messages = new VKList<>();
            dialog.message = new VKApiMessage();
            dialog.message.title = name;
            dialog.chat_id = CHATID++;
            dialog.uid = dialog.chat_id+VKDialog.CHAT_UID_OFFSET;
            dialogs.addBefore(0, dialog);
        }
        catch (Exception e){}
        finally {
            lock.unlock();
        }

      new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {

              notify.OnDataLoaded(VKDialog.class,dialog.chat_id);
          }
      },WAIT_DELAY/2);
     }

    @Override
    public VKDialog getDialogByIdWithMessages(int uid) {

        Log.d(TAG,"getDialogByIdWithMessages "+uid);

        // emuilate empty messages before loaded
        final VKDialog dialog;
        if (getDialogById(uid)==null)
            dialog = CreateDialog(uid);
        else dialog =  getDialogById(uid);
        final VKList<VKMessage> messages = dialog.Messages;



        // emulate messages request
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                // emulate receiving new messages
                dialog.Messages = messages;

                Collections.sort(dialogs);
                DetectNewMessage();

                NotifySystem(VKDialog.class, dialog.uid);
            }
        }, WAIT_DELAY);

        return dialog;
    }

    @Override
    public void init() {

        Log.d(TAG, "init()");

        LoadCache();
    }

    @Override
    public boolean IsLogged() {
        return true;
    }

    @Override
    public void MarkAsRead(final int DialogId) {

        Log.d(TAG, "MarkAsRead");

        if (getDialogById(DialogId).unread>0) {

            VKDialog dlg = getDialogById(DialogId);
            dlg.unread = 0;
            String ids = "";

            for (VKMessage msg : dlg.Messages){
                if (!msg.read_state){
                    msg.read_state = true;
                    ids+=(ids.length()==0?"":",")+msg.getId();
                }
            }

            NotifySystem(VKDialog.class, DialogId);


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    lock.lock();
                    try {
                        if (getDialogById(DialogId) == null) {
                            Log.e(TAG, "No such dialog: " + DialogId);
                            return;
                        }

                        if (getDialogById(DialogId).Messages != null) {
                            for (VKMessage msg : getDialogById(DialogId).Messages)
                                msg.read_state = true;
                        }
                        getDialogById(DialogId).unread = 0;
                    } finally {
                        lock.unlock();

                    }

                    NotifySystem(VKDialog.class, DialogId);

                }
            }, WAIT_DELAY);
        }
    }

    @Override
    public void RefreshDialogs() {

        Log.d(TAG, "RefreshDialogs");

        super.RefreshDialogs();

    }

    @Override
    void RefreshDialogs(final VKRequest.VKRequestListener listener) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                listener.onComplete(new VKResponse());

                NotifySystem(VKDialog.class, 0);
            }
        }, WAIT_DELAY);

    }

    @Override
    void NotifySystem(Class s, int data) {
        Log.d(TAG,"Notify "+s+" "+data);
        super.NotifySystem(s, data);
    }

    @Override
    void RefreshUserCache() {

        Log.d(TAG, "RefreshUserCache");
    }

    @Override
    public void SendMessage(final int uid, final String text) {

        Log.d("VKData","Send "+uid+" "+text);
        VKDialog dlg = null;
        lock.lock();
        try {
            dlg = getDialogById(uid);

            if (dlg==null){
                if (uid<VKDialog.CHAT_UID_OFFSET) {

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
                msg.id = MSGID++;

                if (dlg.Messages.size()>0)
                    dlg.Messages.addBefore(0,msg);
                else
                 dlg.Messages.add(msg);
                dlg.message = msg;

                //VKRequest req = new VKRequest("messages.send",VKParameters.from(VKApiConst.USER_ID,));
            }
        }
        finally {
            lock.unlock();
        }

        NotifySystem(VKDialog.class, uid);




        // emulate receive answer
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                lock.lock();
                // modify data on "server"
                try {
                    VKDialog dlg = getDialogById(uid);

                    Log.d("VKData",dlg.toString());

                    VKMessage msg = new VKMessage();
                    msg.user_id = 1;
                    msg.body = text;
                    if (dlg.chat_id > 0)
                        msg.title = dlg.message.title;
                    msg.date = new Date().getTime() / 1000;
                    msg.out = false;
                    msg.read_state = false;
                    msg.id = MSGID++;
                    if (dlg.Messages.size()>0)
                     dlg.Messages.addBefore(0,msg);
                    else
                        dlg.Messages.add(msg);
                    dlg.message = msg;
                    dlg.unread++;
                    if (dialogs.size() > 1) {
                        dialogs.remove(dlg);
                        dialogs.addBefore(0, dlg);
                    }
                }
                finally {
                    lock.unlock();
                }
                Log.d("VKData", "New message " + (MSGID - 1) + " arrived to "+uid);
                        // emulate GCM call
                        getDialogByIdWithMessages(uid);

            }
        },WAIT_DELAY);
    }
}
