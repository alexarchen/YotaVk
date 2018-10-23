package com.zakharchenko.yotavk;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiLink;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKApiPost;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKAttachments;
import com.yotadevices.sdk.BSActivity;
import com.yotadevices.sdk.Constants;
import com.yotadevices.sdk.utils.RotationAlgorithm;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BSMessagesList extends BSActivity {

   public final String TAG="BSMessagesList";

 protected int ConversationID=0;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG,"Event recevied: "+intent.getAction());
            if(intent.getAction().equals(MyApplication.VKRECORDSCHANGED_BROADCAST)) {

                onUpdate();
            }
            else
            if(intent.getAction().equals(MyApplication.VKREADCHANGED_BROADCAST))
            {
                MyApplication.GetMessages(ConversationID, new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        sendBroadcast(new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST));//, null, MessagesList.this, MessagesList.class));
                    }
                });

            }else
            if(intent.getAction().equals(MyApplication.VKUSERSCHANGED_BROADCAST))

            {

                last_message_id =0;
                //TODO: Update only if users in this chat
                onUpdate();
                onUpdateHeader();
            }

            {

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = BSMessagesList.this.registerReceiver(null, ifilter);

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
                    if (PreferenceManager.getDefaultSharedPreferences(BSMessagesList.this).getBoolean("show_time", false))
                        t.setText(new SimpleDateFormat("HH:mm").format(new Date())+"   "+val+"%");
                    else
                        t.setText("");
                }

            }

           }
        };

    boolean SendUnread = false;
/*
    @Override
    public void onBSSaveInstanceState(Bundle b){
     if (b!=null)
         b.putInt("ID",ConversationID);
    }

    @Override
    public void onBSRestoreInstanceState(Bundle b){
     if (b!=null)
         ConversationID = b.getInt("ID",0);

    }*/

    @Override
    public void onBSCreate() {
        super.onBSCreate();

        Self = this;

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(1);
        }

        if (ConversationID==0) {
            try {
                ConversationID = getIntent().getExtras().getInt("ID");
            } catch (Exception e) {
                ConversationID = 0;
            }
        }



        setBSContentView(R.layout.activity_messages_list_bs);

        boolean bLoaded = false;
        MyApplication.lock.lock();
        try {
          MyApplication.VKDialog dlg = MyApplication.getDialogById(ConversationID);
          if (dlg==null) // Create new user conversation
          {
              dlg = MyApplication.CreateDialog(ConversationID);
          }
          if (dlg.Messages!=null) bLoaded = true;
        }
        finally {
            MyApplication.lock.unlock();
        }

        ((ListView) findViewById(R.id.listViewConv)).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (view.getAdapter()!=null) {
                    //for (int q=firstVisibleItem;q<firstVisibleItem+visibleItemCount;q++)
                    //    if (!((MyConvAdapter) view.getAdapter()).getItem(q).read_state)
                    if ((totalItemCount==firstVisibleItem+visibleItemCount) && (!SendUnread))
                    {
                        int nUr=0;

                        try {


                            for (int q=0;q<totalItemCount;q++){
                                MyApplication.VKMessage msg = ((MyConvAdapter)view.getAdapter()).getItem(q);

                                if ((!msg.read_state) && (!msg.out))
                                    nUr++;
                            }
                        }
                        catch (Exception e){
                        }

                        if (nUr>0) {
                            MyApplication.MarkAsRead(ConversationID);
                            onUpdateHeader();


                        }

                        SendUnread = true;

                        //    break;
                    }
                }

            }
        });

        if (bLoaded)
         onUpdate();



        findViewById(R.id.Send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendClick(v);
            }
        });

        ((EditText)findViewById(R.id.Message)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0) {
                    findViewById(R.id.Send).setEnabled(true);
                    ((ImageView) findViewById(R.id.Send)).setImageDrawable(getDrawable(R.drawable.send));
                } else {
                    findViewById(R.id.Send).setEnabled(false);
                    ((ImageView) findViewById(R.id.Send)).setImageDrawable(getDrawable(R.drawable.send_grey));
                }

            }
        });
    }


    public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
        private ImageView imageView=null;
        private LevelListDrawable  d=null;
        private TextView tv=null;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        public ImageLoadTask(String url, LevelListDrawable  d,TextView tv) {
            this.url = url;
            this.d = d;
            this.tv = tv;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);

                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.setUseCaches(true);

                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);

                String classname = input.toString();
                if(classname.contains("HttpResponseCache")){

                    Log.d("IMAGE","From cache");
                }
                else
                    Log.d("IMAGE","Loaded: "+url);


                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (imageView!=null)
                imageView.setImageBitmap(result);
            if (d!=null) {
                BitmapDrawable dr = new BitmapDrawable(result);


                d.addLevel(1, 1, dr);
                d.setLevel(1);

                CharSequence t = tv.getText();
                tv.setText(t);
            }
        }

    }

    List<ImageLoadTask> imageTasks = new ArrayList<ImageLoadTask>();

    void AddLoadImageTask(String url,ImageView view){
        ImageLoadTask task = new ImageLoadTask(url, view);
        imageTasks.add(task);
        task.execute();
    }

    void AddLoadImageTask(String url,LevelListDrawable d,TextView view){
        ImageLoadTask task = new ImageLoadTask(url, d,view);
        imageTasks.add(task);
        task.execute();
    }

    void ClearTasks(){
        for (ImageLoadTask task:imageTasks){
            task.cancel(false);
        }
        imageTasks.clear();
    }

    protected static BSMessagesList Self = null;
    public static boolean isRunning(){
        return (Self!=null);
    }

    @Override
    protected void onBSDestroy(){

        Self = null;
        super.onBSDestroy();
    }

    @Override
    public void onBSResume(){
       super.onBSResume();

        setFeature(Constants.Feature.FEATURE_OVERRIDE_BACK_PRESS);
        last_message_id =0;

        Log.d(TAG, "Register receiver");

        IntentFilter ifil = new IntentFilter(MyApplication.VKRECORDSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKUSERSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKREADCHANGED_BROADCAST);
        ifil.addAction(Intent.ACTION_TIME_TICK);
        ifil.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        ifil.addAction(Intent.ACTION_TIME_CHANGED);
        registerReceiver(bReceiver, ifil);
        bReceiver.onReceive(this,new Intent(""));
        // renew dialogs & messages

        //MyApplication.GetMessages(((MyApplication)getApplication()).defListener);
        findViewById(R.id.Wait).setVisibility(View.VISIBLE);
        ((AnimationDrawable)((ImageView) findViewById(R.id.Wait)).getDrawable()).start();



        // refresh
        MyApplication.GetMessages(ConversationID, new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                sendBroadcast(new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST));//, null, MessagesList.this, MessagesList.class));
            }
        });


    }


    @Override
    public void onBSPause(){
       super.onBSPause();

        ClearTasks();
        Log.d(TAG, "UnRegister receiver");
        unregisterReceiver(bReceiver);

    }

    final String DATE_FORMAT = "dd MMMM yyyy";


    void OpenLink(final String url){

        PackageManager pm = this.getPackageManager();
        try {
            if (pm.getPackageInfo("com.zakharchenko.yobrowser", 0) != null) {

                Intent i = new Intent().setComponent(new ComponentName("com.zakharchenko.yobrowser", "com.zakharchenko.yobrowser.YoBSBrowser")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse(url));
                startService(i);

                return;
            }
        }
        catch (Exception e){}

        RotationAlgorithm.getInstance(getApplication()).turnScreenOffIfRotated(
                RotationAlgorithm.OPTION_START_WITH_BS /*| RotationAlgorithm.OPTION_NO_UNLOCK*/
                        /*| RotationAlgorithm.OPTION_EXPECT_FIRST_ROTATION_FOR_60SEC*/, new RotationAlgorithm.OnPhoneRotatedListener() {
                    @Override
                    public void onRotataionCancelled() {
                    }

                    @Override
                    public void onPhoneRotatedToFS() {

                        Intent i = new Intent("android.intent.action.VIEW");
                        //i.setComponent(ComponentName.unflattenFromString("com.android.chrome/com.android.chrome.Main"));
                        i.setPackage("com.android.chrome");
                        //i.addCategory("android.intent.category.VIEW");
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setData(Uri.parse(url));
                        startActivity(i);

                    }

                    @Override
                    public void onPhoneRotatedToBS() {

                    }
                });




    }


    public class MyConvAdapter extends ArrayAdapter<MyApplication.VKMessage> {

        protected MyApplication.VKDialog Dialog;

        public MyConvAdapter(Context context, int textViewResourceId, MyApplication.VKDialog dlg) {
            super(context, textViewResourceId, dlg.Messages);
            Dialog = dlg;
        }
        public void refresh(MyApplication.VKDialog dlg) {
            clear();
            addAll(dlg.Messages);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            VKApiMessage msg = this.getItem(position);

            LayoutInflater inflater = ((BSMessagesList)getContext()).getBSDrawer().getBSLayoutInflater();


            final View row  = inflater.inflate(msg.out ? R.layout.message_out_bs : R.layout.message_in_bs, parent, false);

            if (msg.attachments.size()>0){
                for (VKAttachments.VKApiAttachment att:msg.attachments){

                    if (att.getType().equals(VKAttachments.TYPE_PHOTO)){
                        try {
                            VKApiPhoto photo = (VKApiPhoto) att;
                            ImageView imageView = new ImageView(parent.getContext());
                            imageView.setMinimumWidth(400);
                            imageView.setMinimumHeight(photo.height * 400 / photo.width);
                            //imageView.setImageResource(R.drawable.spin_blue_2);

                            ((ViewGroup) row.findViewById(R.id.Attachments)).addView(imageView);
                            if (photo.photo_604.length() > 0)
                                AddLoadImageTask(photo.photo_604, imageView);
                            else if (photo.photo_807.length() > 0)
                                AddLoadImageTask(photo.photo_807, imageView);
                            else
                                AddLoadImageTask(photo.photo_130, imageView);
                        }
                        catch (Exception e){}
                    }
                    else
                    if (att.getType().equals(VKAttachments.TYPE_POST)){
                        VKApiPost post = (VKApiPost) att;

                        if (post.text.length()==0){
                            if (post.copy_history!=null){
                                for (VKApiPost p:post.copy_history){
                                    if (p.text.length()>0)
                                    {
                                        post.text = p.text;
                                        break;
                                    }
                                }
                            }
                            if (post.text.length()==0) post.text = " <> ";
                        }

                        if ((post.from_id != 0) && (post.text.length() > 0)) {

                            TextView tv = new TextView(parent.getContext());
                            tv.setMovementMethod(LinkMovementMethod.getInstance());

                            SpannableStringBuilder sb = new SpannableStringBuilder(getString(R.string.wall)+"\n");
                            int start = sb.length();


                            for (VKAttachments.VKApiAttachment att1 : post.attachments){
                                if (att1.getType()==VKAttachments.TYPE_PHOTO){

                                    VKApiPhoto photo = (VKApiPhoto) att1;

                                    LevelListDrawable d = new LevelListDrawable();
                                    //Drawable d = getDrawable(R.drawable.send_grey);
                                    d.setBounds(0, 0, 360, photo.height * 360 / photo.width);
                                    //d.addLevel(0,0,getDrawable(R.drawable.send_grey));

//                                  int w = sb.length();
                                    sb.append(" ", new ImageSpan(d), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    //                                 sb.setSpan(new ImageSpan(d), w, w + 1, 0);


                                    if (photo.photo_604.length() > 0)
                                        AddLoadImageTask(photo.photo_604, d,tv);
                                    else if (photo.photo_807.length() > 0)
                                        AddLoadImageTask(photo.photo_807, d,tv);
                                    else
                                        AddLoadImageTask(photo.photo_130, d,tv);


                                    break;

                                }
                            }

                            sb.append("\n");
                            String s =(post.text.split("\n")[0]);
                            sb.append(s.substring(0,Math.min(s.length(),128)));

                            final String link = "https://vk.com/wall" + post.from_id + "_" + post.id;
                            sb.setSpan(new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    OpenLink (link);
                                }
                            }, start, sb.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            sb.setSpan(new ForegroundColorSpan(Color.rgb(30,30,30)),0,sb.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            tv.setText(sb);

                            tv.setBackgroundResource(R.drawable.rect);
                            ((ViewGroup) row.findViewById(R.id.Attachments)).addView(tv);
                        }


                    }
                    else
                        if (att.getType().equals(VKAttachments.TYPE_LINK)){

                            final VKApiLink link = (VKApiLink) att;

                            TextView tv = new TextView(parent.getContext());
                            tv.setMovementMethod(LinkMovementMethod.getInstance());
                            SpannableStringBuilder sb = new SpannableStringBuilder();
                            sb.append("" + link.title + ":\n", new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                            sb.append(link.description);
                            ClickableSpan span = new ClickableSpan(){
                                @Override
                                public void onClick(View widget) {

                                    OpenLink (link.url);
                                }
                            };

                            sb.setSpan(span, 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            sb.setSpan(new ForegroundColorSpan(Color.rgb(30,30,30)),0,sb.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            tv.setText(sb);
                            tv.setBackgroundResource(R.drawable.rect);
                            ((ViewGroup) row.findViewById(R.id.Attachments)).addView(tv);

                        }
                    else
                        {
                            ImageView imageView = new ImageView(parent.getContext());
                            imageView.setImageResource(R.drawable.attachment);
                            imageView.setMaxWidth(100);
                            imageView.setMaxHeight(100);
                            ((ViewGroup) row.findViewById(R.id.Attachments)).addView(imageView);

                        }

                }

            }


            String date = new SimpleDateFormat(DATE_FORMAT).format(new Date(msg.date*1000));

            if ((position==0)
                || (!new SimpleDateFormat(DATE_FORMAT).format(new Date(getItem(position-1).date*1000)).equals(date))){

                row.findViewById(R.id.Date).setVisibility(View.VISIBLE);
                ((TextView)row.findViewById(R.id.Date)).setText(date);

            }
            else
                row.findViewById(R.id.Date).setVisibility(View.GONE);



            if ((Dialog.isMulti()) && (!msg.out) && ((position==0) || (getItem(position-1).user_id!=msg.user_id))) {



                VKApiUserFull user = MyApplication._getUserById(msg.user_id);
                if (user!=null)
                    ((TextView) row.findViewById(R.id.Header)).setText(user.first_name+" "+user.last_name);// only for multidialog
                else
                    ((TextView) row.findViewById(R.id.Header)).setText("User#"+msg.user_id);// only for multidialog
                row.findViewById(R.id.Header).setVisibility(View.VISIBLE);
            }
            else
                row.findViewById(R.id.Header).setVisibility(View.GONE);


            ((TextView )row.findViewById(R.id.Text)).setText(msg.body+"     ");// + "&nbsp;&nbsp;&nbsp;&nbsp;"));
            ((TextView) row.findViewById(R.id.Time)).setText(new SimpleDateFormat("HH:mm").format(((long)msg.date)*1000));

            row.setTag(msg);
            return row;

        }
    };





    @Override
    protected boolean onBackPressed() {

        Log.d(TAG, "onBackPressed");
        startBSActivity(new Intent(this, BSChatsList.class));
        finish();
        return (true);
    }


    protected int last_message_id = 0;

    protected void onUpdate() {

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                findViewById(R.id.Wait).setVisibility(View.GONE);
                ((AnimationDrawable)((ImageView) findViewById(R.id.Wait)).getDrawable()).stop();

                int  NeedRequestUser=0;
                MyApplication.lock.lock();
                try {
                    MyApplication.VKDialog dlg = ((MyApplication) getApplication()).getDialogById(ConversationID);


                    if (dlg != null) {
                        if (dlg.chat_id>0)
                            ((TextView) findViewById(R.id.Header)).setText(dlg.message.title);
                        else {

                            VKApiUserFull user = ((MyApplication) getApplication()).getUserById(dlg.uid);
                            if (user != null)
                                ((TextView) findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name);
                            else {
                                ((TextView) findViewById(R.id.Header)).setText("User #" + dlg.getId());
                                NeedRequestUser = dlg.uid;
                            }
                        }
                    }

                    if ((dlg != null) && (dlg.Messages != null)) {
                        //MyApplication.MarkAsRead(dlg.uid);

                        int hash =dlg.message.id+dlg.chat_id;
                        for (MyApplication.VKMessage msg: dlg.Messages)
                            hash+=msg.id+(msg.read_state?1:0);

                        //if ((last_message_id==0) || (hash!=last_message_id))

                            if (((ListView) findViewById(R.id.listViewConv)).getAdapter()==null) {
                                ((ListView) findViewById(R.id.listViewConv)).setAdapter(new MyConvAdapter(BSMessagesList.this, R.layout.message_in, dlg));
                                MyApplication.MarkAsRead(dlg.uid);
                            }
                            else {
                                if (((ListView) findViewById(R.id.listViewConv)).getLastVisiblePosition()>=((ListView) findViewById(R.id.listViewConv)).getCount()-1)
                                    MyApplication.MarkAsRead(dlg.uid);

                                ((MyConvAdapter)((ListView) findViewById(R.id.listViewConv)).getAdapter()).refresh(dlg);

                            }

                        last_message_id = hash;
                    }
                } finally {
                    MyApplication.lock.unlock();
                }

                if (NeedRequestUser>0){

                    MyApplication.GetUser(NeedRequestUser, new VKRequest.VKRequestListener() {
                        @Override
                        public void onComplete(VKResponse response) {
                            super.onComplete(response);
                            sendBroadcast(new Intent(MyApplication.VKUSERSCHANGED_BROADCAST));
                        }
                    });

                }
            }
        });



    }


    protected void onUpdateHeader(){

     runOnUiThread(new Runnable() {
         @Override
         public void run() {

             MyApplication.lock.lock();
             try {
                 MyApplication.VKDialog dlg = ((MyApplication) getApplication()).getDialogById(ConversationID);


                 if (dlg != null) {

                     int nUnr = 0;
                     for (MyApplication.VKMessage msg: dlg.Messages){
                         if ((!msg.out) && (!msg.read_state)) nUnr++;
                     }

                     String unread="";
                     if (nUnr>0)
                         unread = "  "+nUnr;

                     if (dlg.chat_id>0)
                         ((TextView) findViewById(R.id.Header)).setText(dlg.message.title+unread);
                     else {

                         VKApiUserFull user = ((MyApplication) getApplication()).getUserById(dlg.message.user_id);
                         if (user != null)
                             ((TextView) findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name+unread);
                         else {
                             ((TextView) findViewById(R.id.Header)).setText("User #" + dlg.getId()+unread);
                         }
                     }
                 }

             } finally {
                 MyApplication.lock.unlock();
             }

         }
     });
    }

  public void onSendClick(View v){



      try {

          if (MyApplication.SendMessage(ConversationID,((TextView) findViewById(R.id.Message)).getText().toString(), new VKRequest.VKRequestListener() {
              @Override
              public void onComplete(VKResponse response) {
                  super.onComplete(response);

                  onUpdate();
              }

              @Override
              public void onError(VKError error) {
                  super.onError(error);

                  onUpdate();

              }
          })){

              ((EditText)findViewById(R.id.Message)).setText("");
              onUpdate();
          }
      }
      catch (Exception e){}


  }


}
