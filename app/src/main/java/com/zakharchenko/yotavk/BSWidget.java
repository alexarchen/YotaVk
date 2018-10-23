package com.zakharchenko.yotavk;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPoll;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.yotadevices.sdk.BackscreenLauncherConstants;
import com.yotadevices.sdk.Drawer;
import com.yotadevices.sdk.EpdIntentCompat;
import com.yotadevices.sdk.utils.EinkUtils;
import com.yotadevices.sdk.utils.RotationAlgorithm;

import org.apache.http.HttpConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zakharchenko on 18.01.2016.
 */


// extra_large widget

public class BSWidget extends AppWidgetProvider {

  // this service updates messages and send broadcast
public static class WidgetService extends NotificationListenerService {
/*
    public WidgetService(){
        super("WigetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction()=="UPDATE_MSG"){

            MyApplication.GetMessages(((MyApplication)getApplication()).defListener);

        }
    }*/

/*

      String LongPollKey;
      String LongPollServer;
      int LongPollTS;
      Thread LongPollThread=null;

      boolean bLongPoll = false;
      void startLongPoll(){
       if (!bLongPoll){

           if (VKSdk.isLoggedIn()){

             bLongPoll = true;

             VKRequest req = new VKRequest("messages.getLongPollServer");
               req.executeWithListener(new VKRequest.VKRequestListener() {
                   @Override
                   public void onComplete(VKResponse response) {
                       super.onComplete(response);

                       try {
                           JSONObject obj = response.json.getJSONObject("response");
                           LongPollKey = obj.getString("key");
                           LongPollServer = obj.getString("server");
                           LongPollTS = obj.getInt("ts");

                           Log.d("LONGPOLL","Get: "+response.json.toString());

                           if (LongPollThread==null)
                           LongPollThread = new Thread(
                           new Runnable(){
                               @Override
                               public void run() {

                                   while (bLongPoll) {
                                       if (LongPollKey.length()>0) {
                                           try {
                                               URL url = new URL("http://" + LongPollServer + "?act=a_check&key="+LongPollKey+"&ts="+LongPollTS+"&wait=25&mode=0");
                                               HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                               try {
                                                   InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                                                   String str = "";
                                                   byte buffer[] = new byte[1024];
                                                   while (in.read(buffer)!=-1){
                                                       str+=buffer;
                                                   }
                                                   str+=buffer;

                                                   Log.d("LONGPOLL","Request: "+str);

                                                   JSONObject obj = new JSONObject(str);
                                                   if (obj.getInt("failed")!=0){
                                                       //
                                                       Log.i("LONGPOLL","Return failed: "+str);
                                                       bLongPoll = false;
                                                       startLongPoll();
                                                   }
                                                   else{

                                                       JSONArray upd = obj.getJSONArray("updates");
                                                       for (int q=0;q<upd.length();q++)
                                                         if (upd.getJSONArray(q).get(0).

                                                   }



                                               } catch (Exception e) {
                                                  wait(1000);
                                               } finally {
                                                   urlConnection.disconnect();
                                               }
                                           } catch (Exception e) {


                                           }
                                       }
                                   }
                               }
                           });

                           LongPollThread.start();


                       }
                       catch (Exception e){}


                   }

                   @Override
                   public void onError(VKError error) {

                       super.onError(error);

                       bLongPoll = false;
                       Log.i("VKWIDGET", "Error longpoll: " + error);
                   }
               });


           }



       }

      }
*/

      @Override
      public void onNotificationRemoved(StatusBarNotification sbn,RankingMap map) {
          super.onNotificationPosted(sbn, map);

          if (sbn.getPackageName().equals("com.vkontakte.android")) {

              // maybe read
              MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                  @Override
                  public void onComplete(VKResponse response) {

                      Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                      MyApplication.SetIntentExtras(intent);
                      sendBroadcast(intent);
                  }
              });

          }

      }

      @Override
      public void onNotificationRemoved(StatusBarNotification sbn) {

          super.onNotificationRemoved(sbn);

          if (sbn.getPackageName().equals("com.vkontakte.android")) {

              // maybe read
              MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                  @Override
                  public void onComplete(VKResponse response) {

                      Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                      MyApplication.SetIntentExtras(intent);
                      sendBroadcast(intent);
                  }
              });

          }


      }

      final BroadcastReceiver bReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              // if(intent.getAction().equals(MyApplication.VKRECORDSCHANGED_BROADCAST)) {
              ///onUpdate(context,null,null);
              Log.d("WIDSERV", "Recived " + intent.getAction());
              if (intent.getAction().equals("android.intent.action.SCREEN_ON")){
               // detect if VK is in top, so delete system notifications
              /*
               if (MyGcmListenerService.isVKRunning(context))
               {

                   NotificationManager mNotificationManager =
                           (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                   mNotificationManager.cancelAll();

               }*/

              }
              else
              if (intent.getAction().equals("android.intent.action.SCREEN_OFF"))
              {
                  MyApplication.GetMessages(new VKRequest.VKRequestListener() {
                      @Override
                      public void onComplete(VKResponse response) {

                          Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                          MyApplication.SetIntentExtras(intent);
                          sendBroadcast(intent);
                      }
                  });

              }
              else
              UpdateAllWidgets(context);

              // }

          }
      };

      @Override
      public int onStartCommand(Intent intent, int flags, int startId) {
          super.onStartCommand(intent, flags, startId);

         if ((intent!=null) && (intent.getAction()!=null) && (intent.getAction().equals("CANCEL_NOTIF"))){

             new Handler().postDelayed(new Runnable() {
                 @Override
                 public void run() {

                     try {
                         wait(4000);
                     }
                     catch (Exception e){}
                     NotificationManager mNotificationManager =
                             (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                     mNotificationManager.cancel(2);

                 }
             },4000);
         }

          Log.d("WIDSERV", "Started");


          return (START_STICKY);

      }

      @Override
      public void onCreate() {
          super.onCreate();

          if (!RegistrationIntentService.bRegistered) {
              Intent intent = new Intent(getApplicationContext(), RegistrationIntentService.class);
              getApplicationContext().startService(intent);
          }


          IntentFilter ifil = new IntentFilter(MyApplication.VKRECORDSCHANGED_BROADCAST);
          ifil.addAction("android.intent.action.SCREEN_ON");
          ifil.addAction("android.intent.action.SCREEN_OFF");
          ifil.addAction(MyApplication.VKUSERSCHANGED_BROADCAST);
          ifil.addAction(MyApplication.VKREADCHANGED_BROADCAST);

          Log.d("WIDSERV", "Register receiver");

          registerReceiver(bReceiver, ifil);

          MyApplication.GetMessages(new VKRequest.VKRequestListener() {
              @Override
              public void onComplete(VKResponse response) {

                  Intent intent = new Intent(MyApplication.VKRECORDSCHANGED_BROADCAST);
                  MyApplication.SetIntentExtras(intent);
                  sendBroadcast(intent);
              }
          });

          MyApplication.GetFriends(new VKRequest.VKRequestListener() {
              @Override
              public void onComplete(VKResponse response) {

                  Intent intent = new Intent(MyApplication.VKUSERSCHANGED_BROADCAST);
                  MyApplication.SetIntentExtras(intent);
                  sendBroadcast(intent);
              }
          });



      }

      @Override
      public void onDestroy() {

          Log.d("WIDSERV", "UnRegister receiver");

          unregisterReceiver(bReceiver);
          super.onDestroy();


      }


  }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        UpdateAllWidgets(context);



    }

    protected static void UpdateAllWidgets(Context context){

        Log.d("WIDGET", "UpdateAllWidgets");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, BSWidget.class));

        for (int id : appWidgetIds) {

            Bundle opts = appWidgetManager.getAppWidgetOptions(id);
            int size = opts.getInt(BackscreenLauncherConstants.OPTION_WIDGET_SIZE, -1);
            RemoteViews views=null;
            //Log.d("WIDGET","ID: "+id+" size: "+size);

            if (size==-1) size = BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM;

            if (size!=BackscreenLauncherConstants.WIDGET_SIZE_LARGE) {


                switch (size) {
                    case BackscreenLauncherConstants.WIDGET_SIZE_SMALL:     // Small
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_small);

                        break;
                    case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:    // Medium
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_medium);
                        break;
                    default:
                        views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
                }


                int nUnread = 0;
                int nUnreadMsg = 0;
                int nChats = 0;
                String UnreadString = "";
                boolean bLogged = false;
                long msgtime = 0;
                int uid = 0;
                if (VKSdk.isLoggedIn()) {

                    bLogged = true;
                    MyApplication.lock.lock();
                    try {
                        if (MyApplication.dialogs != null) {

                            for (MyApplication.VKDialog dlg : MyApplication.dialogs) {
                                if (dlg.unread > 0) {
                                    uid = dlg.uid;

                                    if (dlg.isMulti()) nChats++;

                                    if ((dlg.message != null) && (dlg.message.date > msgtime))
                                        msgtime = dlg.message.date;

                                    nUnread++;
                                    nUnreadMsg += dlg.unread;

                                    if (dlg.chat_id > 0) {
                                        UnreadString += (UnreadString.length() > 0 ? ", " : "") + dlg.message.title;
                                    } else {
                                        VKApiUserFull user = MyApplication._getUserById(dlg.uid);
                                        if (user != null) {
                                            UnreadString += (UnreadString.length() > 0 ? ", " : "") + user.first_name + " " + user.last_name;
                                        } else
                                            UnreadString += (UnreadString.length() > 0 ? ", " : "") + "user" + dlg.uid;

                                    }

                                }
                            }
                        } else if (!MyApplication.bDoGetMessages) {

                            // start service for update messages

                            //context.startService(new Intent("UPDATE_MSG",null,context,WidgetService.class));

                        }
                    } finally {
                        MyApplication.lock.unlock();
                    }

                    if (nUnread == 1) {

                        PendingIntent pintent = null;
                        if (MyApplication.isYotaphoneSDK2())
                         pintent = PendingIntent.getService(context.getApplicationContext(), 2, new Intent(context, BSMessagesList.class).putExtra("ID", uid), PendingIntent.FLAG_UPDATE_CURRENT);
                        else {
                            Intent intent = new Intent(context, MessagesList.class).putExtra("ID", uid);
                            //if (MyApplication.isClass("EpdIntentCompat"))
                            //EpdIntentCompat.addEpdFlags(intent,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                            pintent = PendingIntent.getActivity(context.getApplicationContext(), 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        }
                        views.setOnClickPendingIntent(R.id.Header, pintent);
                    } else {
                        PendingIntent pintent = null;
                        if (MyApplication.isYotaphoneSDK2())
                            pintent = PendingIntent.getService(context.getApplicationContext(), 2, new Intent(context, BSChatsList.class), PendingIntent.FLAG_UPDATE_CURRENT);
                        else {
                            Intent intent = new Intent(context, ChatsList.class);
                            //if (MyApplication.isClass("EpdIntentCompat"))
                             //EpdIntentCompat.addEpdFlags(intent,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                            pintent = PendingIntent.getActivity(context.getApplicationContext(), 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        }
                        views.setOnClickPendingIntent(R.id.Header, pintent);
                    }

                } else {
                    views.setTextViewText(R.id.textView, context.getString(R.string.notlogged));
                    Intent intent = new Intent(Intent.ACTION_MAIN, null, context, MainActivity.class);
                   // if (MyApplication.isClass("EpdIntentCompat"))
                    //    EpdIntentCompat.addEpdFlags(intent,EpdIntentCompat.FLAG_ACTIVITY_START_ON_EPD_SCREEN);
                    PendingIntent pintent = PendingIntent.getActivity(context.getApplicationContext(), 2,intent , PendingIntent.FLAG_UPDATE_CURRENT);

                    views.setOnClickPendingIntent(R.id.Header, pintent);

                    if (size == BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM) {
                        views.setViewVisibility(R.id.Time, View.GONE);
                        views.setViewVisibility(R.id.textView2, View.GONE);
                    }

                }
                if (bLogged) {
                    switch (size) {
                        case BackscreenLauncherConstants.WIDGET_SIZE_SMALL:     // Small
                        case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:    // Medium
                            String str = "";

                            if (nUnreadMsg == 1) {
                                //if (nChats > 0)
                                //    str = context.getString(R.string.newmsgs1);
                                //else
                                    str = context.getString(R.string.newmessages1);

                            } else if (nUnreadMsg >= 5) {

                                //if (nChats > 1)
                                //    str = String.format(context.getString(R.string.newmsgs5), nUnreadMsg);
                                //else
                                    str = String.format(context.getString(R.string.newmessages5), nUnreadMsg);

                            } else if (nUnreadMsg >= 2) {

                                //if (nChats > 1)
                                //    str = String.format(context.getString(R.string.newmsgs2), nUnreadMsg);
                                //else
                                    str = String.format(context.getString(R.string.newmessages2), nUnreadMsg);

                            } else
                                str = context.getString(R.string.nomessages);

                            /*str += " ";
                            if (nChats > 0) {
                                if (nUnread == 1) {

                                    str += context.getString(R.string.chat);
                                } else if (nUnread > 0) {
                                    str += String.format(context.getString(R.string.chats), nUnread);

                                }
                            }*/

                            views.setTextViewText(R.id.textView, str);

                            if (size == BackscreenLauncherConstants.WIDGET_SIZE_SMALL)
                                break;
                            // case BackscreenLauncherConstants.WIDGET_SIZE_MEDIUM:
                            views.setTextViewText(R.id.textView2, UnreadString);

                            if (nUnread > 0) {
                                views.setViewVisibility(R.id.textView2, View.VISIBLE);
                                views.setViewVisibility(R.id.Time, View.VISIBLE);

                            } else {
                                views.setViewVisibility(R.id.textView2, View.GONE);
                                views.setViewVisibility(R.id.Time, View.GONE);

                            }
                            views.setTextViewText(R.id.Time, new SimpleDateFormat("HH:mm").format(new Date(msgtime * 1000)));

                    }
                }
            }
            else
            {

                int nUnread = 0;
                int nUnreadMsg = 0;
                String UnreadString="";

                SpannableStringBuilder builder = new SpannableStringBuilder();
                views = new RemoteViews(context.getPackageName(), R.layout.widget_large);



                if (VKSdk.isLoggedIn()) {



                    MyApplication.lock.lock();
                    try {
                        if (MyApplication.dialogs != null) {

                            VKList<MyApplication.VKMessage> msgs = new VKList<>();

                            for (MyApplication.VKDialog dlg : MyApplication.dialogs) {
                                if (dlg.unread > 0) {

                                    nUnread++;
                                    nUnreadMsg += dlg.unread;

                                    if (dlg.chat_id > 0) {
                                        UnreadString += (UnreadString.length() > 0 ? ", " : "") + dlg.message.title;
                                    } else {
                                        VKApiUserFull user = MyApplication._getUserById(dlg.uid);
                                        if (user != null) {
                                            UnreadString += (UnreadString.length() > 0 ? ", " : "") + user.first_name + " " + user.last_name;
                                        } else
                                            UnreadString += (UnreadString.length() > 0 ? ", " : "") + "user" + dlg.uid;

                                    }

                                    VKList<MyApplication.VKMessage> mmm = dlg.Messages;
                                    if ((mmm == null) && (dlg.message != null)) {
                                        mmm = new VKList<>();
                                        mmm.add(new MyApplication.VKMessage(dlg.message, dlg.uid));
                                    }


                                    if (mmm != null) {

                                        for (MyApplication.VKMessage msg : mmm) {
                                            if ((!msg.out) && (!msg.read_state)) {
                                                int q = 0;
                                                for (; q < msgs.size(); q++) {
                                                    if (msgs.get(q).date < msg.date) {
                                                        break;
                                                    } else if (msgs.get(q).date == msg.date) {
                                                        if (msgs.get(q).chat_id > msg.chat_id)
                                                            break;

                                                        if (msgs.get(q).chat_id == msg.chat_id) {
                                                            if (msgs.get(q).user_id > msg.user_id)
                                                                break;

                                                        }

                                                    }

                                                }
                                                msgs.add(q, msg);


                                            }
                                        }
                                    }

                                }
                            }


                            List<MyApplication.VKMessage> l = msgs.subList(0, Math.min(2, msgs.size()));
                            // now we have last messages

                            if ((l != null) && (l.size() > 0)) {

                                int lastid = 0;
                                int user_id = 0;

                                Set<Integer> users = new HashSet<Integer>();
                                Set<Integer> dialogs = new HashSet<Integer>();
                                for (MyApplication.VKMessage msg : l) {
                                    user_id = msg.user_id;
                                    users.add(msg.user_id);
                                    dialogs.add(msg.chat_id);
                                }

                                String str = "";
                                if (nUnreadMsg == 1) {
                                    str = context.getString(R.string.newmsgs1);
                                } else if (nUnreadMsg >= 5) {
                                    str = String.format(context.getString(R.string.newmsgs5), nUnreadMsg);

                                } else if (nUnreadMsg >= 2) {
                                    str = String.format(context.getString(R.string.newmsgs2), nUnreadMsg);

                                } else
                                    str = context.getString(R.string.nomessages);

                                boolean bAddUser = true; // add user name to textLines (only true if 1 user)
                                boolean bAddChat = true; // add chat name to textLines


                                if (users.size() == 1) {

                                    //if (users.toArray())
                                    builder.append(str+"\n");

                                    VKApiUserFull user = MyApplication._getUserById(user_id);

                                    if (user != null) {
                                        //                              if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.getId() + ".jpg").exists())
                                        //                                  mBuilder.setLargeIcon(ImageUtil.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.getId() + ".jpg", 50));
                                        builder.append(user.first_name + " " + user.last_name, new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    } else
                                        builder.append("User#" + user_id, new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


                                    //builder.append("\n" + new SimpleDateFormat("HH:mm").format(new Date(l.get(0).date * 1000)) + "\n", new StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                    bAddUser = false;
                                } else {

/*                                    if (nUnread == 1) {

                                        str += context.getString(R.string.chat);
                                    } else if (nUnread > 0) {
                                        str += String.format(context.getString(R.string.chats), nUnread);

                                    }
*/
                                    builder.append(str+"\n");

                                    if (dialogs.size() == 1) {


                                        MyApplication.VKDialog dlg = MyApplication.getDialogById(l.get(0).chat_id);
                                        String chat_name = "";
                                        if ((dlg != null) && (dlg.message != null)) {
                                            chat_name = dlg.message.title;
                                            builder.append(chat_name, new TextAppearanceSpan(null, Typeface.BOLD, 40, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                            bAddChat = false;
                                        }

                                    } else
                                        builder.append(UnreadString);


                                    //builder.append("\n" + new SimpleDateFormat("HH:mm").format(new Date(l.get(0).date * 1000)) + "\n", new StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    views.setTextViewText(R.id.Time, new SimpleDateFormat("HH:mm").format(new Date(l.get(0).date * 1000)));


                                }

                                Log.d("WIDGET","Set header: "+builder.toString());
                                views.setTextViewText(R.id.Header, builder);
                                builder.clear();


                                //if (l.size() > 0)
                                {
                                    Collections.reverse(l);

                                    // Add extra lines
                                    int lastchat = -1;


                                    for (MyApplication.VKMessage msg : l) {

                                        String str1 = "";
                                        if (true) {
                                            if ((lastid != msg.user_id) || (lastchat != msg.chat_id)) {

                                                VKApiUserFull user = MyApplication._getUserById(msg.user_id);

                                                if (bAddUser) {
                                                    if (user != null) {
                                                        str1 += user.first_name + " " + user.last_name + ":\n";
                                                    } else
                                                        str1 += "User#" + msg.user_id + ":\n";


                                                    if (msg.chat_id > MyApplication.CHAT_UID_OFFSET) {
                                                        if (bAddChat) {

                                                            MyApplication.VKDialog dlg = MyApplication.getDialogById(msg.chat_id);
                                                            String chat_name = "";
                                                            if ((dlg != null) && (dlg.message != null))
                                                                chat_name = dlg.message.title;


                                                            //if (user != null) {
                                                            //    str1 += user.first_name + " " + user.last_name + "(" + chat_name + "): ";
                                                            //} else
                                                            str1 += "  " + chat_name + ":\n";
                                                        }
                                                    }

                                                }
                                                lastid = msg.user_id;
                                                lastchat = msg.chat_id;
                                            } else {
                                                //   str += " ";
                                            }
                                            if (str1.length() > 0)
                                                builder.append("\n" + str1, new TextAppearanceSpan(null, Typeface.BOLD, 35, null, null), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        }
                                        builder.append(msg.body + "\n", new LeadingMarginSpan.Standard(10, 10), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }


                                }

                            }


                        }

                    } finally {
                        MyApplication.lock.unlock();
                    }


                    views.setTextViewText(R.id.Text,builder);

                }
                else
                {

                    //views = new RemoteViews(context.getPackageName(), R.layout.widget_small);
                    views.setTextViewText(R.id.Header, context.getString(R.string.notlogged));
                    views.setViewVisibility(R.id.Time, View.GONE);


                } // Logged in

            } // size of widget
            appWidgetManager.updateAppWidget(id, views);

        }

    }



    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        Log.d("WIDGET", "onEnable");

    }
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);


        //context.stopService(new Intent(context, WidgetService.class));
        Log.d("WIDGET", "UnSetReceiver");

    }



    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        context.startService(new Intent("UPDATE", null, context, WidgetService.class));

        Log.d("WIDGET", "onReceive "+intent.getAction());

        if (intent.getAction().equals("com.yotadevices.yotaphone.action.APPWIDGET_VISIBILITY_CHANGED")) {
            //            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            //            appWidgetManager.updateAppWidget(new ComponentName(context, BSWidget.class),views);

        }
        //else
            UpdateAllWidgets(context);

    }

}