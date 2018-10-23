package com.zakharchenko.yotavk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.yotadevices.sdk.BSActivity;
import com.yotadevices.sdk.Constants;
import com.yotadevices.sdk.utils.RotationAlgorithm;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BSChatsList extends BSActivity {

    public final String TAG="ChatList";

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RegistrationIntentService.REGISTRATION_COMPLETE)){

                    boolean sentToken = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("SENT_TOKEN_TO_SERVER", false);
                    if (sentToken){

                        onGCMReady();

                    }

                }
            else
                    onUpdate();

            {

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = BSChatsList.this.registerReceiver(null, ifilter);

                // Are we charging / charged?
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int val = new Double(100. * level / (float) scale).intValue();

                TextView t = (TextView )findViewById(R.id.Time);
                if (t!=null)
                {
                    if (PreferenceManager.getDefaultSharedPreferences(BSChatsList.this).getBoolean("show_time", false))
                     t.setText(new SimpleDateFormat("HH:mm").format(new Date())+"    "+val+"%");
                    else t.setText("");
                }

            }

        }
    };

    public class P2BReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            if ("yotaphone.intent.action.IS_BS_SUPPORTED".equalsIgnoreCase(intent.getAction())) {
                Bundle b = new Bundle();
                b.putInt("support_bs", 1);
                setResultExtras(b);
            } else if ("yotaphone.intent.action.P2B".equalsIgnoreCase(intent.getAction())) {


                startActivity(new Intent(context.getApplicationContext(),ChatsList.class));
                finish();

            }

        }
    }

    private P2BReceiver b2BReceiver = new P2BReceiver();

    protected static BSChatsList Self = null;
    public static boolean isRunning(){
        return (Self!=null);
    }

    @Override
    protected void onBSStop(){

        Self = null;
        super.onBSStop();
    }

    @Override
    protected void onBSCreate() {
        super.onBSCreate();

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
        }

        Self = this;


        /* if (!VKSdk.isLoggedIn())
        {
         TextView view = new TextView(this);
          view.setText(R.string.err_notlogged);
          view.setTextSize(30);
          setBSContentView(view);

        }
        else {*/
            setBSContentView(R.layout.activity_chats_list_bs);

            //this.getActionBar().setTitle("VK Chats");

            boolean sentToken = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("SENT_TOKEN_TO_SERVER", false);
            if (sentToken) {


                onGCMReady();

            }

            findViewById(R.id.Title).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnMenuClick(v);
                }
            });

            findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onChatClick(v);
                }
            });

            findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onChatClick(v);
                }
            });

            View.OnClickListener l = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMenuItemClick(v);
                }
            };
            findViewById(R.id.menu_createchat).setOnClickListener(l);
            findViewById(R.id.menu_refresh).setOnClickListener(l);
            findViewById(R.id.menu_settings).setOnClickListener(l);
            findViewById(R.id.menu_runvk).setOnClickListener(l);
            findViewById(R.id.menu_mainscreen).setOnClickListener(l);
        //}
    }

    public static void onGCMReady(){


    }

    @Override
    protected void onBSResume(){
        super.onBSResume();

        Log.d(TAG, "onResume");

        setFeature(Constants.Feature.FEATURE_OVERRIDE_BACK_PRESS);

        findViewById(R.id.Wait).setVisibility(View.VISIBLE);
        ((AnimationDrawable)((ImageView) findViewById(R.id.Wait)).getDrawable()).start();

        IntentFilter ifil = new IntentFilter(MyApplication.VKRECORDSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKUSERSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKREADCHANGED_BROADCAST);
        ifil.addAction(Intent.ACTION_TIME_TICK);
        ifil.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        ifil.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(bReceiver, ifil);
        bReceiver.onReceive(this, new Intent(""));

      //  IntentFilter ifil2 = new IntentFilter("yotaphone.intent.action.IS_BS_SUPPORTED");
      //  ifil2.addAction("yotaphone.intent.action.P2B");
      //  registerReceiver(b2BReceiver, ifil2);


            MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {

                    Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                    MyApplication.SetIntentExtras(intent);
                    sendBroadcast(intent);

                    onUpdate();
                }
            });



    }


    @Override
    protected void onBSPause(){
        super.onBSPause();

        Log.d(TAG, "onPause");

        unregisterReceiver(bReceiver);
     //   unregisterReceiver(b2BReceiver);
    }

    public class MyAdapter extends ArrayAdapter<MyApplication.VKDialog>{

        public MyAdapter(Context context, int textViewResourceId, List<MyApplication.VKDialog> dlgs) {
            super(context, textViewResourceId,dlgs);
        }

        public void refresh(List<MyApplication.VKDialog> dlgs) {
            clear();
            addAll(dlgs);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            MyApplication.VKDialog dlg = this.getItem(position);


            LayoutInflater inflater = ((BSChatsList)getContext()).getBSDrawer().getBSLayoutInflater();
            final View row  = inflater.inflate(R.layout.dialog_bs, parent, false);
            if (dlg!=null) {

                if (dlg.unread>0){
                    ((TextView)row.findViewById(R.id.Unread)).setText(""+dlg.unread);
                    row.findViewById(R.id.Unread).setVisibility(View.VISIBLE);
                }
                else
                    row.findViewById(R.id.Unread).setVisibility(View.GONE);

                if (dlg.message!=null) {
                    String date = new SimpleDateFormat("dd.MM.yy").format(new Date(dlg.message.date * 1000));
                    String dateNow = new SimpleDateFormat("dd.MM.yy").format(new Date());
                    if (date.equals(dateNow))
                        ((TextView) row.findViewById(R.id.Time)).setText(new SimpleDateFormat("HH:mm").format(((long) dlg.message.date) * 1000));
                    else
                        ((TextView) row.findViewById(R.id.Time)).setText(date);
                }
                    else
                   row.findViewById(R.id.Time).setVisibility(View.GONE);

                if (dlg.chat_id>0)
                {
                    ((TextView) row.findViewById(R.id.Header)).setText(dlg.message.title);
                    ((ImageView) row.findViewById(R.id.imageView)).setImageDrawable(getContext().getResources().getDrawable(R.drawable.group));

                }
                else{

                VKApiUser user =  ((MyApplication)getApplication()).getUserById(dlg.uid);

                if (user!=null) {
                    try {
                        if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
                            ((ImageView) row.findViewById(R.id.imageView)).setImageBitmap(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 100));
                    }
                    catch (Exception e){}
                    ((TextView) row.findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name);
                }
                else {

                    ((TextView) row.findViewById(R.id.Header)).setText("User #" + dlg.uid);
                    // adding user to friends
                }}
                if (dlg.message!=null) {
                    String s = dlg.message.body;
                    ((TextView) row.findViewById(R.id.Text)).setText(s.substring(0, Math.min(50, s.length())));
                }
                else
                    row.findViewById(R.id.Text).setVisibility(View.GONE);


                row.setTag(dlg);

                row.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        //Dirty hack
                        startConversation(((MyApplication.VKDialog)v.getTag()));
                            /*
                            Intent intent = new Intent(getContext(), ConversationActivity.class);
                            if(((DialogWrapper)v.getTag()).chatId>0)
                                intent.putExtra(ConversationActivity.EXTRA_CHATID, -((DialogWrapper)v.getTag()).chatId);
                            else
                                intent.putExtra(ConversationActivity.EXTRA_USERID, ((DialogWrapper)v.getTag()).userId);
                            getContext().startActivity(intent);
*/


                    }
                });

                if ((bChatMode) && (ChatIDs.contains(dlg.uid)))
                    row.setBackgroundColor(SELECT_COLOR);
                return row;
            }

            return null;

        }
    };




    void onUpdate(){


        Log.d(TAG, "onUpdate");

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
        }
        if (!bChatMode)
        {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    MyApplication.lock.lock();
                    try {

                        if (MyApplication.dialogs != null) {


                            if (Self!=null) {
                                Self.findViewById(R.id.Wait).setVisibility(View.GONE);
                                ((AnimationDrawable)((ImageView) Self.findViewById(R.id.Wait)).getDrawable()).stop();

                                if (((ListView) findViewById(R.id.listView)).getAdapter()==null)
                                    ((ListView) findViewById(R.id.listView)).setAdapter(new MyAdapter(BSChatsList.this, R.layout.dialog, MyApplication.dialogs_ex()));
                                else
                                    ((MyAdapter)((ListView) findViewById(R.id.listView)).getAdapter()).refresh(MyApplication.dialogs_ex());
                            }
                        }
                    }
                    catch (Exception e){}
                    finally {
                        MyApplication.lock.unlock();
                    }
                }
            });

        }



    }

    final int SELECT_COLOR = Color.rgb(168,168,168);
    ArrayList<Integer> ChatIDs = new ArrayList<>();

    protected void startConversation(MyApplication.VKDialog dlg){

        if (bChatMode){
            Log.d(TAG, "Select participant: " + dlg.uid);


            if (dlg.chat_id==0) {
                ListView list = ((ListView) findViewById(R.id.listView));
                int q=0;
                for (;q<list.getCount();q++)
                    if (((MyApplication.VKDialog)list.getChildAt(q).getTag()).uid==dlg.uid)
                        break;

                if (q<list.getCount()){
                    if (ChatIDs.contains(dlg.uid)) {
                        list.getChildAt(q).setBackgroundColor(Color.TRANSPARENT);
                    }
                    else {
                        list.getChildAt(q).setBackgroundColor(SELECT_COLOR);
                    }

                }
                if (ChatIDs.contains(dlg.uid))
                    ChatIDs.remove((Object)dlg.uid);
                else
                    ChatIDs.add(dlg.uid);

                //list.setItemChecked(1,true);



            }

        }
        else {
            Log.d(TAG, "Start conversation: " + dlg.uid);


            Intent i = new Intent();
            i.setClass(this, BSMessagesList.class);
            i.putExtra("ID", dlg.uid);
            startBSActivity(i);
        }
    }

    public void OnMenuClick(View v){
/*
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.chatsmenu, popup.getMenu());
        popup.show();
  */

      //  View view = getBSDrawer().getBSLayoutInflater().inflate(R.layout.dialog,(ViewGroup)findViewById(R.id.Top));

        if (findViewById(R.id.Menu).getVisibility()==View.VISIBLE)
            findViewById(R.id.Menu).setVisibility(View.GONE);
         else
           findViewById(R.id.Menu).setVisibility(View.VISIBLE);



   }

    public void onChatClick(View v){

        if (v.getId()==R.id.buttonOK) {

            createChatName = ((TextView) findViewById(R.id.ChatName)).getText().toString();

            if ((createChatName.length() > 0) && (ChatIDs.size()>0)){

                int array[] = new int[ChatIDs.size()];
                for (int i = 0; i < ChatIDs.size(); i++) array[i] = ChatIDs.get(i);

                MyApplication.CreateChat(createChatName, array, new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        try {
                            final int ChatID = response.json.getInt("response");

                            MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                                @Override
                                public void onComplete(VKResponse response) {
                                    super.onComplete(response);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            startConversation(MyApplication.getDialogById(MyApplication.CHAT_UID_OFFSET + ChatID));
                                        }
                                    });

                                }
                            });
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onError(VKError error) {
                        super.onError(error);

                        Log.i(TAG, "Error chat " + error.toString());
                    }
                });

                CreateChatMode(false);

            }

        }
        else
            CreateChatMode(false);
    }

    public boolean bChatMode = false;

    public void CreateChatMode(boolean bMode){

        ListView list = ((ListView)findViewById(R.id.listView));

        if (bMode){
            bChatMode = true;
            ChatIDs.clear();

            //list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            /*
            for (int q=0;q<list.getAdapter().getCount();q++){
                if (((MyApplication.VKDialog)list.getAdapter().getItem(q)).chat_id>0)
                    list.getChildAt(q).setEnabled(false);
            }*/

            findViewById(R.id.CreateChat).setVisibility(View.VISIBLE);
            findViewById(R.id.ChatName).setVisibility(View.VISIBLE);

            findViewById(R.id.ChatName).callOnClick();



        }
        else
        {
            bChatMode = false;
            //((ListView)findViewById(R.id.listView)).setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            list.invalidate();
            findViewById(R.id.CreateChat).setVisibility(View.GONE);

        }
    }


    AlertDialog ChatDialog;
    String createChatName="";

    public void Toast (String s){
        LayoutInflater inflater = getBSDrawer().getBSLayoutInflater();
        final View v = inflater.inflate(R.layout.toast,null);
        ((TextView)v.findViewById(R.id.textView)).setText(s);
        ((ViewGroup)findViewById(R.id.Top)).addView(v);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                ((ViewGroup)findViewById(R.id.Top)).removeView(v);

            }
        },5000);

    }



    @Override
    protected boolean onBackPressed(){

     if (bChatMode)
     {
         CreateChatMode(false);
         return (true);
     }
        finish();
        return (true);
    }

    public boolean onMenuItemClick(View v) {

        switch (v.getId()) {
            case R.id.menu_createchat:


              CreateChatMode(true);
              findViewById(R.id.Menu).setVisibility(View.GONE);


                return true;
            case R.id.menu_refresh:
                CreateChatMode(false);
                findViewById(R.id.Wait).setVisibility(View.VISIBLE);
                ((AnimationDrawable)((ImageView) findViewById(R.id.Wait)).getDrawable()).start();
                MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                        MyApplication.SetIntentExtras(intent);
                        sendBroadcast(intent);

                        onUpdate();
                    }
                });
                findViewById(R.id.Menu).setVisibility(View.GONE);
                return true;
            case R.id.menu_settings: {
                Intent i = new Intent(this, SettingsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                Toast(getString(R.string.turn_screen_front));
                findViewById(R.id.Menu).setVisibility(View.GONE);
                RotationAlgorithm rot = RotationAlgorithm.getInstance(this);
                rot.turnScreenOffIfRotated(RotationAlgorithm.OPTION_START_WITH_BS);
            }
                return true;
            case R.id.menu_mainscreen: {
                Intent i = new Intent(this, ChatsList.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                Toast(getString(R.string.turn_screen_front));
                findViewById(R.id.Menu).setVisibility(View.GONE);
                RotationAlgorithm rot = RotationAlgorithm.getInstance(this);
                rot.turnScreenOffIfRotated(RotationAlgorithm.OPTION_START_WITH_BS);
                return true;
            }
            case R.id.menu_runvk: {
                final String url = "https://vk.com/im";
                Uri uri = Uri.parse(url);
                Intent intent = new Intent();
                intent.setPackage("com.vkontakte.android");
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                Toast(getString(R.string.turn_screen_front));
                findViewById(R.id.Menu).setVisibility(View.GONE);
                RotationAlgorithm rot = RotationAlgorithm.getInstance(this);
                rot.turnScreenOffIfRotated(RotationAlgorithm.OPTION_START_WITH_BS);

            }
                return true;

            default:
                return false;
        }
    }



}


