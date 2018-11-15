package com.zakharchenko.yotavk.View;

import android.app.Activity;
import android.app.Dialog;
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
import com.yotadevices.sdk.EpdIntentCompat;
import com.yotadevices.sdk.utils.RotationAlgorithm;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.Presenter.MessagesPresenter;
import com.zakharchenko.yotavk.Presenter.Presenter;
import com.zakharchenko.yotavk.R;
import com.zakharchenko.yotavk.Utils.Utils;

import junit.framework.Assert;

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

public class MessagesList extends Activity implements MessagesPresenter.MessageListener{

   public final String TAG="MessagesList";

   MessagesPresenter presenter;

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

        // TODO SDK2
        if (Utils.isClass("com.yotadevices.sdk.EpdIntentCompat")) {
            Intent i = new Intent(getApplicationContext(), MessagesList.class).putExtra("ID", ConversationID);
            EpdIntentCompat.addEpdFlags(i,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
            getApplicationContext().startActivity(i);
        }



    }

    private P2BReceiver bP2BReceiver = new P2BReceiver();

/*
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
*/

    @Override
    public void onNewMessage() {

        ListView listView = ((ListView) findViewById(R.id.listViewConv));
        if (listView.getLastVisiblePosition()>=listView.getAdapter().getCount()-2)
         listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

    }

    @Override
    public void onChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                onUpdateHeader();
                onUpdate();
            }
        });
    }


    @Override
    public void showLoaded(final boolean bLoaded) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (bLoaded) {
                    ((AnimationDrawable) ((ImageView) findViewById(R.id.Wait)).getDrawable()).start();
                    findViewById(R.id.Wait).setVisibility(View.VISIBLE);
                } else {
                    ((AnimationDrawable) ((ImageView) findViewById(R.id.Wait)).getDrawable()).stop();
                    findViewById(R.id.Wait).setVisibility(View.GONE);
                }
            }
        });


    }

    public boolean bPaused = true;

    boolean SendUnread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        bPaused = true;

        try {
            ConversationID = getIntent().getExtras().getInt("ID");
        }
        catch (Exception e){
            ConversationID=0;
            finish();
            return;
        }



        setContentView(R.layout.activity_messages_list);

        if (Utils.isYotaphoneSDK())
         if (Epd.isEpdContext(this)){
            ((View )findViewById(R.id.Header).getParent()).setBackgroundColor(Color.BLACK);

         }


        //((ListView) findViewById(R.id.listViewConv)).setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        //((ListView) findViewById(R.id.listViewConv)).setStackFromBottom(true);


        ((ListView) findViewById(R.id.listViewConv)).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {


                if (view.getAdapter() != null) {
                    //for (int q=firstVisibleItem;q<firstVisibleItem+visibleItemCount;q++)
                    //    if (!((MyConvAdapter) view.getAdapter()).getItem(q).read_state)
                    if ((totalItemCount == firstVisibleItem + visibleItemCount) && (!SendUnread)) {
                        int nUr = 0;

                        try {


                            for (int q = 0; q < totalItemCount; q++) {
                                VKMessage msg = ((MyConvAdapter) view.getAdapter()).getItem(q);

                                if ((!msg.read_state) && (!msg.out))
                                    nUr++;
                            }
                        } catch (Exception e) {
                        }

                        if (nUr > 0) {
                            presenter.MakeRead();
                            onUpdateHeader();


                        }

                        SendUnread = true;

                        //    break;
                    }
                }

            }
        });


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

        presenter = new MessagesPresenter(this, MyApplication.dataProvider,ConversationID);

    }

    public static MessagesList Self = null;

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        presenter.Attach(this, MyApplication.dataProvider);

        bPaused = false;
        last_message_id = 0;

        Self = this;
        // renew dialogs & messages
        IntentFilter ifil2 = new IntentFilter("yotaphone.intent.action.IS_BS_SUPPORTED");
        ifil2.addAction("yotaphone.intent.action.P2B");
        registerReceiver(bP2BReceiver, ifil2);

        //MyApplication.GetMessages(((MyApplication)getApplication()).defListener);
        findViewById(R.id.Wait).setVisibility(View.VISIBLE);
    }


    @Override
    protected void onPause(){
       super.onPause();

        presenter.Destroy();

        Self = null;

        bPaused = true;

        Log.d(TAG, "UnRegister receiver");
        unregisterReceiver(bP2BReceiver);
    }

    final String DATE_FORMAT = "dd MMMM yyyy";

    public class MyConvAdapter extends ArrayAdapter<VKMessage> {

        protected VKDialog Dialog;

        public MyConvAdapter(Context context, int textViewResourceId, VKDialog dlg) {
            super(context, textViewResourceId, dlg.Messages);
            Dialog = dlg;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        public void refresh(VKDialog dlg){
            Dialog = dlg;
            clear();
            addAll(Dialog.Messages);
            notifyDataSetChanged();
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
                           final ImageView imageView = new ImageView(parent.getContext());
                           imageView.setMinimumWidth(700);
                           imageView.setMinimumHeight(photo.height * 700 / photo.width);
                           //imageView.setImageResource(R.drawable.spin_blue_2);

                           ((ViewGroup) row.findViewById(R.id.Attachments)).addView(imageView);
                           String photourl = "";
                           if (photo.photo_604.length() > 0) photourl = photo.photo_604;
                           else if (photo.photo_807.length() > 0) photourl = photo.photo_807;
                           else
                               photourl  = photo.photo_130;

                           presenter.LoadImage(new MessagesPresenter.ImageLoadTask(photourl) {
                               @Override
                               protected void onPostExecute(Bitmap result) {
                                   super.onPostExecute(result);
                                   imageView.setImageBitmap(result);
                               }
                           });

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

                           final TextView tv = new TextView(parent.getContext());
                           tv.setMovementMethod(LinkMovementMethod.getInstance());

                           SpannableStringBuilder sb = new SpannableStringBuilder(getString(R.string.wall)+"\n");
                           int start = sb.length();


                           for (VKAttachments.VKApiAttachment att1 : post.attachments){
                              if (att1.getType()==VKAttachments.TYPE_PHOTO){

                                  VKApiPhoto photo = (VKApiPhoto) att1;

                                  final LevelListDrawable d = new LevelListDrawable();
                                  //Drawable d = getDrawable(R.drawable.send_grey);
                                  d.setBounds(0, 0, 640, photo.height * 640 / photo.width);
                                  //d.addLevel(0,0,getDrawable(R.drawable.send_grey));

//                                  int w = sb.length();
                                  sb.append(" ", new ImageSpan(d), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 //                                 sb.setSpan(new ImageSpan(d), w, w + 1, 0);

                                  String photourl = "";
                                  if (photo.photo_604.length() > 0) photourl = photo.photo_604;
                                  else if (photo.photo_807.length() > 0) photourl = photo.photo_807;
                                  else
                                      photourl  = photo.photo_130;

                                  presenter.LoadImage(new MessagesPresenter.ImageLoadTask(photourl){
                                      @Override
                                      protected void onPostExecute(Bitmap result) {
                                          super.onPostExecute(result);

                                          BitmapDrawable dr = new BitmapDrawable(result);
                                          d.addLevel(1, 1, dr);
                                          d.setLevel(1);
                                          CharSequence t = tv.getText();
                                          tv.setText(t);

                                      }
                                  });


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

                VKApiUserFull user = presenter.getUser(msg.user_id);
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



    protected int last_message_id = 0;

    protected void onUpdate() {

        SendUnread = false;

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif",true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();

        }

                VKDialog dlg = presenter.GetDialog();

                if (dlg != null) {
                    /*
                    if (dlg.chat_id > 0)
                        ((TextView) findViewById(R.id.Header)).setText(dlg.message.title);
                    else {

                        VKApiUserFull user = presenter.getUser(dlg.uid);
                        Assert.assertNotNull(user);
                        ((TextView) findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name);

                    }*/

                    ListView listView = ((ListView) findViewById(R.id.listViewConv));

                    if ((dlg.Messages != null) && (dlg.message != null)) {

                        if (listView.getAdapter() == null) {
                            listView.setAdapter(new MyConvAdapter(MessagesList.this, R.layout.message_in, dlg));
                            presenter.MakeRead();
                        } else {

                            if ((listView.getLastVisiblePosition() >listView.getCount() - 1) && (dlg.unread>0))
                                presenter.MakeRead();



                            //((ListView) findViewById(R.id.listViewConv)).setAdapter(new MyConvAdapter(MessagesList.this, R.layout.message_in, dlg));

                            ((MyConvAdapter) (listView).getAdapter()).refresh(dlg);//notifyDataSetChanged();
                            //listView.invalidateViews();
                            if (listView.getTranscriptMode()==AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL) {
                                listView.smoothScrollToPosition(listView.getCount() - 1);
                                listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
                            }


                        }
                        ;

                        // last_message_id = hash;
                    } else Log.d(TAG, "No messages in dialog " + ConversationID);

                }

    }


    protected void onUpdateHeader(){


             try {
                 VKDialog dlg = presenter.GetDialog();


                 if ((dlg != null) && (dlg.Messages!=null)) {

                     /*int nUnr = 0;
                     for (VKMessage msg: dlg.Messages){
                         if ((!msg.out) && (!msg.read_state)) nUnr++;
                     }

                     String unread="";
                     if (nUnr>0)
                      unread = " "+nUnr;
*/

                     String nUng = dlg.unread==0?"":""+dlg.unread;
                     Log.d(TAG,"Numer of unread: "+nUng );
                     if (dlg.chat_id>0)
                         ((TextView) findViewById(R.id.Header)).setText(dlg.message.title+" "+nUng );
                     else {

                         VKApiUserFull user = presenter.getUser(dlg.uid);
                         Assert.assertNotNull(user);
                         ((TextView) findViewById(R.id.Header)).setText(user.first_name + " " + user.last_name+" "+nUng );

                         }

                 }

             } catch (Exception e){
                Log.e(TAG,e.toString());
             }

    }

  public void onSendClick(View v) {


      try {

          ((ListView) findViewById(R.id.listViewConv)).setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
          presenter.SendMessage(((TextView) findViewById(R.id.Message)).getText().toString());
          ((EditText)findViewById(R.id.Message)).setText("");
//          ((ListView) findViewById(R.id.listViewConv)).setSelection(((ListView) findViewById(R.id.listViewConv)).getAdapter().getCount() - 1);
          //onUpdate();
      }
      catch (Exception e){}


  }


}
