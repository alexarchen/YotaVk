package com.zakharchenko.yotavk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.yotadevices.sdk.BSActivity;
import com.yotadevices.sdk.Constants;
import com.yotadevices.sdk.utils.BitmapUtils;
import com.yotadevices.sdk.utils.RotationAlgorithm;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BSNotifActivity extends BSActivity {

    static String TAG="BSNotifActivity";
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MyApplication.VKRECORDSCHANGED_BROADCAST)) {
                onStartCommand(new Intent());
            }
        }
    };

    @Override
    protected void onBSCreate() {
        super.onBSCreate();

        Log.d(TAG, "BSCreate");

        Self = this;

        setBSContentView(R.layout.notification);


        findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancelAll();


                finish();
            }
        });
        findViewById(R.id.buttRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                finish();
            }
        });

        findViewById(R.id.buttRun).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ConvID==0)
                 startBSActivity(new Intent(BSNotifActivity.this,BSChatsList.class));
                else
                    startBSActivity(new Intent(BSNotifActivity.this,BSMessagesList.class).putExtra("ID",ConvID));


            }
        });

        //showShareFlipActivity(this);
        registerReceiver(bReceiver, new IntentFilter(MyApplication.VKRECORDSCHANGED_BROADCAST));

        findViewById(R.id.Text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) {
                    handler.removeCallbacks(runnable);

                }
            }
        });

        setSystemBSUiVisibility(Constants.SystemBSFlags.SYSTEM_BS_UI_FLAG_HIDE_NAVIGATION);
    }

    public void onClick(View v){

           finish();


    }

    static BSNotifActivity Self = null;


    public static void showShareFlipActivity(final Context context) {



        RotationAlgorithm.getInstance(context).turnScreenOffIfRotated(
                RotationAlgorithm.OPTION_START_WITH_BS /*| RotationAlgorithm.OPTION_NO_UNLOCK*/
                        /*| RotationAlgorithm.OPTION_EXPECT_FIRST_ROTATION_FOR_60SEC*/, new RotationAlgorithm.OnPhoneRotatedListener() {
                    @Override
                    public void onRotataionCancelled() {
                        if ((Self != null) && (!Self.isFinishing())){

//                            Self.findViewById(R.id.buttRun).setVisibility(View.VISIBLE);
//                            Self.findViewById(R.id.rotText).setVisibility(View.GONE);

                            //showShareFlipActivity(context);

                        }

                        Log.d(TAG,"Rotation canceled");

                    }

                    @Override
                    public void onPhoneRotatedToFS() {

                        Log.d(TAG,"Rotation");

                        final String url = "https://vk.com/im";

                        Uri uri = Uri.parse(url);
                        Intent intent = new Intent();
                        intent.setPackage("com.vkontakte.android");
                        intent.setData(uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);

                        if (Self != null)
                            Self.onClick(Self.findViewById(R.id.buttonOK));


                    }

                    @Override
                    public void onPhoneRotatedToBS() {
                        Log.d(TAG,"Rotation to BS");


                    }
                });
        //      Intent shareFlipPopup = new Intent(mContext, ShareFlipActivity.class);
        //     mContext.startService(shareFlipPopup);
    }


    public Handler handler=null;
    protected Runnable runnable=null;

    int ConvID=0;

    @Override
    public void onStartCommand(Intent intent) {
        super.onStartCommand(intent);

        Log.d(TAG, "onStartCommand");

        ConvID=0;
        // displaying
        int nUnread = 0;
        int nUnreadMsg = 0;
        SpannableStringBuilder builder = new SpannableStringBuilder();

        final TextView  t= ((TextView )findViewById(R.id.Text));
        final TextView  h= ((TextView )findViewById(R.id.Header));

        MyApplication.lock.lock();
        try {
            if (MyApplication.dialogs!=null) {

                VKList<MyApplication.VKMessage> msgs = new VKList<>();

                for (MyApplication.VKDialog dlg:MyApplication.dialogs){
                    if (dlg.unread>0) {

                        nUnread++;
                        nUnreadMsg+=dlg.unread;


                        VKList<MyApplication.VKMessage> mmm =dlg.Messages;
                        if ((mmm==null) && (dlg.message!=null)) {mmm = new VKList<>(); mmm.add(new MyApplication.VKMessage(dlg.message,dlg.uid));}


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



                List<MyApplication.VKMessage> l = msgs.subList(0,Math.min(7, msgs.size()));
                // now we have last messages

                if ((l!=null) && (l.size()>0)) {

                    if (nUnread==1) {
                        // 1 chat
                        ConvID = l.get(0).chat_id;
                    }


                    int lastid = 0;
                    int user_id = 0;

                    Drawable d = getDrawable(R.drawable.yotavk);
                    d.setBounds(0, 0, h.getLineHeight(), h.getLineHeight());
                    ImageSpan img = new ImageSpan(d);
                    builder.append(" ", img, 0);

                    builder.append(" ");

                    Set<Integer> users = new HashSet<Integer>();
                    Set<Integer> dialogs = new HashSet<Integer>();
                    for (MyApplication.VKMessage msg : l) {
                        user_id = msg.user_id;
                        users.add(msg.user_id);
                        dialogs.add(msg.chat_id);
                    }

                    String str = "";
                    if (nUnreadMsg == 1) {
                        str = getString(R.string.newmessages1);
                    } else if (nUnreadMsg >= 5) {
                        str = String.format(getString(R.string.newmessages5), nUnreadMsg);

                    } else if (nUnreadMsg >= 2) {
                        str = String.format(getString(R.string.newmessages2), nUnreadMsg);

                    } else
                        str = getString(R.string.nomessages);

                    boolean bAddUser = true; // add user name to textLines (only true if 1 user)
                    boolean bAddChat = true; // add chat name to textLines


                    if (users.size() == 1) {

                        //if (users.toArray())

                                VKApiUserFull user = MyApplication._getUserById(user_id);

                            if (user != null) {
  //                              if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
  //                                  mBuilder.setLargeIcon(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 50));
                                builder.append(user.first_name + " " + user.last_name,new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else
                                builder.append("User#" + user_id,new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                        builder.append("\n"+new SimpleDateFormat("HH:mm").format(new Date(l.get(0).date*1000)) + "\n", new StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(str);

                        bAddUser = false;
                    }
                    else {


                        if (dialogs.size()==1){


                            MyApplication.VKDialog dlg = MyApplication.getDialogById(l.get(0).chat_id);
                            String chat_name = "";
                            if ((dlg != null) && (dlg.message != null)) {
                                chat_name = dlg.message.title;
                                builder.append(chat_name, new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                bAddChat = false;
                            }

                        }
                        else
                         builder.append(getString(R.string.app_name),new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                        builder.append("\n"+new SimpleDateFormat("HH:mm").format(new Date(l.get(0).date*1000)) + "\n", new StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        str += " ";
                        if (nUnread == 1) {

                            str += getString(R.string.chat);
                        } else if (nUnread > 0) {
                            str += String.format(getString(R.string.chats), nUnread);

                        }

                        //mBuilder.setContentTitle(str); // y mesgs in x chats
                        //mBuilder.setContentText(UnreadString); // y mesgs in x chats
                        builder.append(str);

                    }

                    h.setText(builder);
                    builder.clear();


                    //if (l.size() > 0)
                    {
                        Collections.reverse(l);

                        // Add extra lines
                        int lastchat =-1;


                        for (MyApplication.VKMessage msg : l) {

                            String str1 = "";
                            if (true) {
                                if ((lastid != msg.user_id) || (lastchat!=msg.chat_id)){

                                    VKApiUserFull user = MyApplication._getUserById(msg.user_id);

                                    if (bAddUser){
                                        if (user != null) {
                                            str1 += user.first_name + " " + user.last_name + ":\n";
                                        } else
                                            str1 += "User#" + msg.user_id + ":\n";



                                        if (msg.chat_id > MyApplication.CHAT_UID_OFFSET) {
                                            if  (bAddChat) {

                                                MyApplication.VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                                                String chat_name = "";
                                                if ((dlg != null) && (dlg.message != null))
                                                    chat_name = dlg.message.title;


                                                //if (user != null) {
                                                //    str1 += user.first_name + " " + user.last_name + "(" + chat_name + "): ";
                                                //} else
                                                str1 += "  "+chat_name + ":\n";
                                            }
                                        }

                                    }
                                    lastid = msg.user_id;
                                    lastchat = msg.chat_id;
                                }
                                 else {
                                 //   str += " ";
                                }
                                if (str1.length()>0)
                                 builder.append("\n"+str1,new TextAppearanceSpan(null, Typeface.BOLD, 35, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            builder.append(msg.body+"\n",new LeadingMarginSpan.Standard(10, 10), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }



                    }

                }

/*
                    int lastid =0;

                for (MyApplication.VKMessage msg:l) {

                    if (lastid!=msg.user_id) {
                        if (msg.user_id > MyApplication.CHAT_UID_OFFSET) {
                            builder.append(msg.title + ":\n", new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            VKApiUserFull user = MyApplication._getUserById(msg.user_id);
                            if (user != null) {
                                builder.append(user.first_name + " " + user.last_name + ":", new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else
                                builder.append("User#" + msg.user_id + ":", new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                        }
                        lastid = msg.user_id;
                    }
                   builder.append(msg.body);
                }
*/



            }
            else
             finish();

        }
        finally {
            MyApplication.lock.unlock();
        }


        if (builder.length()==0) {finish();return;};
        if (isFinishing()) return;

       // showShareFlipActivity(this);

        t.setText(builder);
        //((TextView)findView
        //
        // ById(R.id.Header)).setText(String.format(""));


        t.setMovementMethod(new ScrollingMovementMethod());

        t.post(new Runnable() {
            public void run() {

                int scrollAmount = t.getLayout().getLineTop(t.getLineCount()) - t.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    t.scrollTo(0, scrollAmount);
                else
                    t.scrollTo(0, 0);


                // t.scrollTo(0, Math.max(0,t.getBottom()-t.getHeight()));
            }
        });

        int time = 10;

        try{
            time = new Integer(PreferenceManager.getDefaultSharedPreferences(this).getString("notif_time","10"));
        }
        catch (Exception e){}

        if (runnable == null)
            runnable = new Runnable() {
                public void run() {


                    Log.d("ACTIVITY", "Finish on timer");


                    finish();
                }
            };


        if (handler == null)
            handler = new Handler();
        else
            handler.removeCallbacks(runnable);

        handler.postDelayed(runnable,
                time * 1000);

    }




    @Override
    protected void onBSStop() {
        unregisterReceiver(bReceiver);

        Self = null;

        super.onBSStop();

    }
}
