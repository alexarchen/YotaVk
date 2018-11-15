package com.zakharchenko.yotavk.View;

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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.yotadevices.sdk.Epd;
import com.yotadevices.sdk.EpdIntentCompat;
import com.yotadevices.sdk.utils.RotationAlgorithm;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.Presenter.ChatsPresenter;
import com.zakharchenko.yotavk.Presenter.Presenter;
import com.zakharchenko.yotavk.R;
import com.zakharchenko.yotavk.GCM.RegistrationIntentService;
import com.zakharchenko.yotavk.Utils.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatsList extends Activity implements PopupMenu.OnMenuItemClickListener,ChatsPresenter.Listener {

    public final String TAG="ChatList";

    public class P2BReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            if ("yotaphone.intent.action.IS_BS_SUPPORTED".equalsIgnoreCase(intent.getAction())) {
                Bundle b = new Bundle();
                b.putInt("support_bs", 1);
                setResultExtras(b);
            } else if ("yotaphone.intent.action.P2B".equalsIgnoreCase(intent.getAction())) {
                requestRotation(context.getApplicationContext());
            }

        }
    }
        /**
         * Show rotation request for user and launch a back screen activity.
         * */
        public void requestRotation(Context context) {

            // For getting a RotationAlgorithm object, you should use
            // getInstance() method

            RotationAlgorithm rotation = RotationAlgorithm.getInstance(context);



            // This will prompt the user to rotate the phone
            rotation.issueStandardToastAndVibration();


            // Turning off screen after rotation
            rotation.turnScreenOffIfRotated(0, new RotationAlgorithm.OnPhoneRotatedListener() {
                @Override
                public void onPhoneRotatedToFS() {

                }

                @Override
                public void onPhoneRotatedToBS() {
                    finish();

                }

                @Override
                public void onRotataionCancelled() {

                }
            });

            // Launch a back screen activity


            //TODO: SDK2 getApplicationContext().startService(new Intent(getApplicationContext(), BSChatsList.class));
            if (Utils.isClass("com.yotadevices.sdk.EpdIntentCompat")) {
                Intent i = new Intent(getApplicationContext(), ChatsList.class);
                EpdIntentCompat.addEpdFlags(i, EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                getApplicationContext().startActivity(i);
            }



        }


    private P2BReceiver bP2BReceiver = new P2BReceiver();

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(RegistrationIntentService.REGISTRATION_COMPLETE)){

                    boolean sentToken = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("SENT_TOKEN_TO_SERVER", false);
                    if (sentToken){

                        onGCMReady();

                    }
                }

        }
    };


    public static ChatsList Self = null;
    public boolean bPaused = true;

    @Override
    public void onChanged() {
         runOnUiThread(new Runnable() {
            @Override
            public void run() {

                onUpdate();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Self = this;
        bPaused = true;

        setContentView(R.layout.activity_chats_list);

        if (Utils.isYotaphoneSDK() && (Epd.isEpdContext(this))){

            ((View )findViewById(R.id.Header).getParent()).setBackgroundColor(Color.BLACK);

        }

        //this.getActionBar().setTitle("VK Chats");

        boolean sentToken = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("SENT_TOKEN_TO_SERVER", false);
        if (sentToken){

            onGCMReady();

        }

        presenter = new ChatsPresenter(this, MyApplication.dataProvider);
    }

    public static void onGCMReady(){


    }

    ChatsPresenter presenter;

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        bPaused = false;
        presenter.Attach(this, MyApplication.dataProvider);


        IntentFilter ifil2 = new IntentFilter("yotaphone.intent.action.IS_BS_SUPPORTED");
        ifil2.addAction("yotaphone.intent.action.P2B");
        registerReceiver(bP2BReceiver, ifil2);


        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();

        }

        onUpdate();
    }

    @Override
    public void showLoaded(final boolean bLoaded) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {



            if(bLoaded)

            {
                ((AnimationDrawable) ((ImageView) findViewById(R.id.Wait)).getDrawable()).start();
                findViewById(R.id.Wait).setVisibility(View.VISIBLE);
            }

            else

            {
                ((AnimationDrawable) ((ImageView) findViewById(R.id.Wait)).getDrawable()).stop();
                findViewById(R.id.Wait).setVisibility(View.GONE);
            }
        }
      });
    }


    @Override
    protected void onPause(){
        super.onPause();


        bPaused = true;

        presenter.Destroy();


        Log.d(TAG, "onPause");

        unregisterReceiver(bP2BReceiver);
    }



    public class MyAdapter extends ArrayAdapter<VKDialog>{

        public MyAdapter(Context context, int textViewResourceId, List<VKDialog> dlgs) {
            super(context, textViewResourceId,dlgs);
        }

       /* public void refresh(List<VKDialog> dlgs) {
            clear();
            addAll(dlgs);
            notifyDataSetChanged();
        }*/
       public void refresh(List<VKDialog> dlgs){
           clear();
           addAll(dlgs);
           notifyDataSetChanged();
       }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            VKDialog dlg = this.getItem(position);

            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
            final View row ;
            if (Epd.isEpdContext(getContext()))
                row = inflater.inflate(R.layout.dialog_bs, parent, false);
            else
                row = inflater.inflate(R.layout.dialog, parent, false);
            if (dlg!=null) {

                if (dlg.unread>0){
                    ((TextView)row.findViewById(R.id.Unread)).setText(""+dlg.unread);
                    row.findViewById(R.id.Unread).setVisibility(View.VISIBLE);
                }
                else
                row.findViewById(R.id.Unread).setVisibility(View.GONE);
                if (dlg.message!=null) {

                    String date = new SimpleDateFormat("dd.MM.yy").format(new Date(dlg.message.date*1000));
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

                VKApiUser user =  presenter.GetUser(dlg.uid);

                if (user!=null) {
                    try {
                        if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.id + ".jpg").exists())
                            ((ImageView) row.findViewById(R.id.imageView)).setImageBitmap(Utils.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.id + ".jpg", 100));
                        else {

                            if (user.id<0)
                                ((ImageView) row.findViewById(R.id.imageView)).setImageDrawable(getContext().getResources().getDrawable(R.drawable.group));


                        }
                    }
                    catch (Exception e){}

                    ((TextView) row.findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name);
                }
                else {

                    ((TextView) row.findViewById(R.id.Header)).setText("User #" + dlg.uid);
                    // adding user to friends
                 }
                }

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
                        startConversation(((VKDialog)v.getTag()));
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

        if (!bChatMode) {

            try {

                List<VKDialog> dialogs = presenter.GetDialogs();
                if (dialogs != null) {

                    if (((ListView) findViewById(R.id.listView)).getAdapter() == null)
                        ((ListView) findViewById(R.id.listView)).setAdapter(new MyAdapter(ChatsList.this, R.layout.dialog, dialogs));
                    else {
                        ((MyAdapter) ((ListView) findViewById(R.id.listView)).getAdapter()).refresh(dialogs);
                        //findViewById(R.id.listView).invalidate();
                    }

                }
            } finally {
            }
        }




    }

    final int SELECT_COLOR = Color.rgb(168,168,168);
    ArrayList<Integer> ChatIDs = new ArrayList<>();

    @Override
    public void openDialog(int uid) {
        Intent i = new Intent();
        i.setClass(this, MessagesList.class);
        i.putExtra("ID", uid);
        startActivity(i);
    }

    protected void startConversation(VKDialog dlg){

        if (bChatMode){
            Log.d(TAG, "Select participant: " + dlg.uid);


            if ((dlg.chat_id==0) && (dlg.uid>0))
            {
                ListView list = ((ListView) findViewById(R.id.listView));
                int q=0;
                for (;q<list.getCount();q++)
                    if (((VKDialog)list.getChildAt(q).getTag()).uid==dlg.uid)
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
            openDialog(dlg.uid);


        }
    }

    public void OnMenuClick(View v){

        if (ChatDialog!=null) {
            ChatDialog.cancel();
            ChatDialog=null;
        }

        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.chatsmenu, popup.getMenu());
        popup.show();

    }

    public void onChatClick(View v){

        if (v.getId()==R.id.buttonOK){

            bChatMode = false;

            int array[] = new int[ChatIDs.size()];
            for (int i=0; i<ChatIDs.size();i++) array[i]=ChatIDs.get(i);

            presenter.CreateChat(array,createChatName);
        }

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

        }
        else
        {
            bChatMode = false;
            //((ListView)findViewById(R.id.listView)).setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            list.invalidate();
            findViewById(R.id.CreateChat).setVisibility(View.GONE);

        }
    }


    AlertDialog ChatDialog=null;
    String createChatName="";
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_createchat:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.createchat).setView(R.layout.chat_dialog);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        createChatName = ((TextView) ChatDialog.findViewById(R.id.editText2)).getText().toString();
                        if (createChatName.length() > 0) {
                            CreateChatMode(true);
                            dialog.cancel();
                        }
                    }
                });

                ChatDialog = builder.create();
                ChatDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                ChatDialog.show();



                return true;
            case R.id.menu_refresh:
                if (ChatDialog!=null) {
                    ChatDialog.cancel();
                    ChatDialog=null;
                }
                CreateChatMode(false);
                presenter.Refresh();

                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            case R.id.menu_runvk: {
                final String url = "https://vk.com/im";
                Uri uri = Uri.parse(url);
                Intent intent = new Intent();
                intent.setPackage("com.vkontakte.android");
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        Self = null;

        super.onDestroy();

        presenter = null;
    }
}
