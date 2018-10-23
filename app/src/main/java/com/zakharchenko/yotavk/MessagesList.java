package com.zakharchenko.yotavk;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.methods.VKApiFriends;
import com.vk.sdk.api.methods.VKApiMessages;
import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiGetDialogResponse;
import com.vk.sdk.api.model.VKApiLink;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKApiPost;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.api.model.VKUsersArray;
import com.yotadevices.sdk.Epd;
import com.yotadevices.sdk.utils.RotationAlgorithm;

import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MessagesList extends Activity {

   public final String TAG="MessagesList";

 protected int ConversationID=0;

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
               //finish();

            }

            @Override
            public void onRotataionCancelled() {

            }
        });

        // Launch a back screen activity

        getApplicationContext().startService(new Intent(getApplicationContext(), BSMessagesList.class).putExtra("ID", ConversationID));



    }


    private P2BReceiver bP2BReceiver = new P2BReceiver();

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Event recevied: "+intent.getAction());

            if(intent.getAction().equals(MyApplication.VKRECORDSCHANGED_BROADCAST)) {

                onUpdate();
            }
            else
            if(intent.getAction().equals(MyApplication.VKREADCHANGED_BROADCAST)) {

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
                Log.d(TAG,"Event recevied: "+intent.getAction());

                last_message_id =0;
                onUpdateHeader();
            }


           }
        };

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

    static MessagesList Self = null;
    public boolean bPaused = true;
    static boolean isRunning(){return ((Self!=null) && (Self.bPaused));}

    boolean SendUnread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        Self = this;
        bPaused = true;

        try {
            ConversationID = getIntent().getExtras().getInt("ID");
        }
        catch (Exception e){
            ConversationID=0;
        }




        setContentView(R.layout.activity_messages_list);

        if (Epd.isEpdContext(this)){
            ((View )findViewById(R.id.Header).getParent()).setBackgroundColor(Color.BLACK);

        }

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

                if (s.length()>0) {findViewById(R.id.Send).setEnabled(true); ((ImageView)findViewById(R.id.Send)).setImageDrawable(getDrawable(R.drawable.send));}
                else {findViewById(R.id.Send).setEnabled(false); ((ImageView)findViewById(R.id.Send)).setImageDrawable(getDrawable(R.drawable.send_grey));}

            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "Register receiver");

        bPaused = false;
        last_message_id = 0;

        IntentFilter ifil = new IntentFilter(MyApplication.VKRECORDSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKUSERSCHANGED_BROADCAST);
        ifil.addAction(MyApplication.VKREADCHANGED_BROADCAST);

        registerReceiver(bReceiver, ifil);
        // renew dialogs & messages
        IntentFilter ifil2 = new IntentFilter("yotaphone.intent.action.IS_BS_SUPPORTED");
        ifil2.addAction("yotaphone.intent.action.P2B");
        registerReceiver(bP2BReceiver, ifil2);

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
    protected void onPause(){
       super.onPause();

        ClearTasks();

        bPaused = true;
        ((AnimationDrawable)((ImageView) findViewById(R.id.Wait)).getDrawable()).stop();


        Log.d(TAG, "UnRegister receiver");
        unregisterReceiver(bReceiver);
        unregisterReceiver(bP2BReceiver);
    }

    final String DATE_FORMAT = "dd MMMM yyyy";

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

            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();


            final View row;
            if (!Epd.isEpdContext(getContext()))
                row = inflater.inflate(msg.out ? R.layout.message_out : R.layout.message_in, parent, false);
            else
                row = inflater.inflate(msg.out ? R.layout.message_out_bs : R.layout.message_in_bs, parent, false);


           if (msg.attachments.size()>0){
               for (VKAttachments.VKApiAttachment att:msg.attachments){

                   if (att.getType().equals(VKAttachments.TYPE_PHOTO)){
                       try {
                           VKApiPhoto photo = (VKApiPhoto) att;
                           ImageView imageView = new ImageView(parent.getContext());
                           imageView.setMinimumWidth(700);
                           imageView.setMinimumHeight(photo.height * 700 / photo.width);
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
                   if (att.getType().equals(VKAttachments.TYPE_POST)) {
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
                                  d.setBounds(0, 0, 640, photo.height * 640 / photo.width);
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
                           sb.append(s.substring(0,Math.min(s.length(),256)));

                           sb.setSpan(new URLSpan("https://vk.com/wall" + post.from_id + "_" + post.id), start,sb.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                           tv.setText(sb);

                           tv.setBackgroundResource(R.drawable.rect);
                           ((ViewGroup) row.findViewById(R.id.Attachments)).addView(tv);
                       }

                   }
                   else
                   if (att.getType().equals(VKAttachments.TYPE_LINK)){

                       VKApiLink link = (VKApiLink) att;

                       TextView tv = new TextView(parent.getContext());
                       tv.setMovementMethod(LinkMovementMethod.getInstance());
                       SpannableStringBuilder sb = new SpannableStringBuilder();
                       sb.append("" + link.title + ":\n", new StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                       sb.append(link.description);
                       sb.setSpan(new URLSpan(link.url),0,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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




/*
    @Override
    public void onBackPressed() {

        if (ConversationID>0) endConversation();
        else
        super.onBackPressed();
    }
*/
    protected int last_message_id = 0;

    protected void onUpdate() {

        SendUnread = false;

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

                    if ((dlg != null) && (dlg.Messages != null) && (dlg.message!=null)) {
                        //MyApplication.MarkAsRead(dlg.uid);

                        /*int hash =dlg.message.id+dlg.chat_id;
                        for (MyApplication.VKMessage msg: dlg.Messages)
                         hash+=msg.id+(msg.read_state?1:0);
*/
                        //if ((last_message_id==0) || (hash!=last_message_id))
                           {

                            if (((ListView) findViewById(R.id.listViewConv)).getAdapter()==null) {
                                ((ListView) findViewById(R.id.listViewConv)).setAdapter(new MyConvAdapter(MessagesList.this, R.layout.message_in, dlg));
                                MyApplication.MarkAsRead(dlg.uid);
                            }
                            else {
                                if (((ListView) findViewById(R.id.listViewConv)).getLastVisiblePosition()>=((ListView) findViewById(R.id.listViewConv)).getCount()-1)
                                    MyApplication.MarkAsRead(dlg.uid);

                                ((MyConvAdapter)((ListView) findViewById(R.id.listViewConv)).getAdapter()).refresh(dlg);


                            };
                        }

                       // last_message_id = hash;
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


                 if ((dlg != null) && (dlg.Messages!=null)) {

                     int nUnr = 0;
                     for (MyApplication.VKMessage msg: dlg.Messages){
                         if ((!msg.out) && (!msg.read_state)) nUnr++;
                     }

                     String unread="";
                     if (nUnr>0)
                      unread = " "+nUnr;

                     Log.d(TAG,"Numer of unread: "+nUnr);
                     if (dlg.chat_id>0)
                         ((TextView) findViewById(R.id.Header)).setText(dlg.message.title+" "+unread);
                     else {

                         VKApiUserFull user = ((MyApplication) getApplication()).getUserById(dlg.uid);
                         if (user != null)
                             ((TextView) findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name+" "+unread);
                         else {
                             ((TextView) findViewById(R.id.Header)).setText("User #" + dlg.uid+" "+unread);
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

    @Override
    protected void onDestroy() {
        Self = null;

        super.onDestroy();

    }

}
