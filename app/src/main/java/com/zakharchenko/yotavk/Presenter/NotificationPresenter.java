package com.zakharchenko.yotavk.Presenter;

import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;

import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.MyApplication;
import com.zakharchenko.yotavk.R;
import com.zakharchenko.yotavk.Utils.Utils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zakharchenko on 14.11.2018.
 */
public class NotificationPresenter extends Presenter {

    public interface Listener extends Presenter.Listener{
        public void onNotify(boolean bNewMsg);
    }

    public NotificationPresenter(Listener listener,VKData dataProvider){
        super(listener, dataProvider);
    }

    @Override
    public void OnDataLoaded(Class s, int data) {
        super.OnDataLoaded(s, data);

        if (listener!=null) {
            if (s == VKMessage.class) ((Listener) listener).onNotify(true);
            else
              if ((s!=VKApiUserFull.class) || (data==0))
                ((Listener) listener).onNotify(false);
        }

    }


    @Nullable public
    Map<String, Object> GetNotificationInformation() {

        Log.d("NotifPres","Call GetNotificationInformation");

        HashMap<String,Object>  map = new HashMap<String,Object>();

        map.put("Title",MyApplication.AppContext.getString(R.string.nomessages));
        map.put("Text","");
        map.put("Date",(long)0);
        map.put("nUnread",(int)0);
        map.put("nUnreadMsg",(int)0);

        if (dataProvider == null) return null;


        if(!dataProvider.IsLogged()) {
          map.put("Title", MyApplication.AppContext.getString(R.string.notlogged));
          map.put("Text","");
          map.put("nUnreadMsg",(int)0);
          map.put("nUnread",(int)0);
          return map;
        }

        int nUnread = 0;
        int nUnreadMsg = 0;

        String UnreadString = "";

        try {
            List<VKDialog> dialogs = dataProvider.getDialogs();
            if (dialogs != null) {

                VKList<VKMessage> msgs = new VKList<>();

                for (VKDialog dlg : dialogs) {
                    if (dlg.unread > 0) {

                        nUnread++;
                        nUnreadMsg += dlg.unread;

                        if (dlg.chat_id > 0) {
                            UnreadString += (UnreadString.length() > 0 ? ", " : "") + dlg.message.title;
                        } else {
                            VKApiUserFull user = dataProvider.getUserById(dlg.uid);
                            if (user != null) {
                                UnreadString += (UnreadString.length() > 0 ? ", " : "") + user.first_name + " " + user.last_name;
                            } else
                                UnreadString += (UnreadString.length() > 0 ? ", " : "") + "<Unknown>";

                            // RefreshUserCache();

                        }


                        List<VKMessage> mmm = dlg.getMessages();
                        if (((mmm == null) || (mmm.size()==0)) && (dlg.message != null)) {
                            mmm = new VKList<>();
                            mmm.add(new VKMessage(dlg.message, dlg.uid));
                        }

                        if (mmm != null) {

                            for (VKMessage msg : mmm) {
                                msg.chat_id = dlg.uid;
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
                } // for dialogs


                map.put("nUnreadMsg", nUnreadMsg);
                map.put("nUnread",nUnread);

                if (nUnreadMsg > 0) {

                    //nUnreadMsg = msgs.size();
                    // msgs - (0) - newest message


                    List<VKMessage> l = null;


                    if (msgs.size() > 0) {

                        l = msgs.subList(0, Math.min(7, msgs.size()));
                    }
                    // now we have last messages
                    if ((l != null) && (l.size() > 0)) {
                        int lastid = 0;
                        int user_id = 0;

                        Set<Integer> users = new HashSet<Integer>();
                        Set<Integer> chats = new HashSet<Integer>();
                        for (VKMessage msg : l) {
                            user_id = msg.chat_id;
                            users.add(msg.user_id);
                            chats.add(msg.chat_id);
                        }

                        String str = "";
                        if (nUnreadMsg == 1) {
                            str = MyApplication.AppContext.getString(R.string.newmessages1);
                        } else if (nUnreadMsg >= 5) {
                            str = String.format(MyApplication.AppContext.getString(R.string.newmessages5), nUnreadMsg);

                        } else if (nUnreadMsg >= 2) {
                            str = String.format(MyApplication.AppContext.getString(R.string.newmessages2), nUnreadMsg);

                        } else
                            str = MyApplication.AppContext.getString(R.string.nomessages);

                        boolean bAddUser = true; // add user info into textLines
                        boolean bAddChat = true; // add chat  info into textLines

                        if (users.size() == 1) {

                            // one user

                            if ((chats.size() == 1) && (user_id > VKDialog.CHAT_UID_OFFSET)) // one user in one chat
                            {
                                VKApiUserFull user = dataProvider.getUserById(l.get(0).user_id);
                                VKDialog dlg = dataProvider.getDialog(user_id);
                                String chat_name = "";
                                if ((dlg != null) && (dlg.message != null))
                                    chat_name = dlg.message.title;

                                if (user != null) {
                                    if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.id + ".jpg").exists())

                                        map.put("LargeIcon", Utils.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.id + ".jpg", 250));

                                    map.put("Title", user.first_name + " " + user.last_name + " (" + chat_name + ")");
                                } else
                                    map.put("Title", chat_name);

                                bAddChat = false;


                            } else // one user personal message or several chats
                            {
                                VKApiUserFull user = dataProvider.getUserById(l.get(0).user_id);

                                if (user != null) {

                                    if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.id + ".jpg").exists())
                                        map.put("LargeIcon", Utils.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.id + ".jpg", 250));

                                    map.put("Title", user.first_name + " " + user.last_name);
                                } else
                                    map.put("Title", "User#" + user_id);

                            }

                            if (l.size() == 1) {

                                map.put("Text", l.get(0).body);
                            } else
                                map.put("Text", str);

                            bAddUser = false;
                        } else {

                            // several users


                            str += " ";
                            if (nUnread == 1) {

                                str += MyApplication.AppContext.getString(R.string.chat);
                            } else if (nUnread > 0) {
                                str += String.format(MyApplication.AppContext.getString(R.string.chats), nUnread);

                            }

                            //mBuilder.setContentTitle(str); // y mesgs in x chats
                            if (chats.size() == 1) {
                                // several users in one chat
                                map.put("Title", l.get(0).title + ":");
                                map.put("Text", str);
                            } else {
                                map.put("Title", str);
                                map.put("Text", UnreadString); // chat names
                            }

                        }


                        map.put("Date",l.get(0).date);
                        if (l.size() > 1) {

                            // Add extra lines

                            Collections.reverse(l); // now l(0) - oldest


                            NotificationCompat.InboxStyle inboxStyle =
                                    new NotificationCompat.InboxStyle();

                            inboxStyle.setBigContentTitle(str);
                            map.put("BigTitle", str);


                            SpannableStringBuilder sbp = new SpannableStringBuilder();


                            int lastchat = -1;

                            for (VKMessage msg : l) {

                                String str1 = "";
                                if (true) {
                                    if ((lastid != msg.user_id) || (lastchat != msg.chat_id)) {
                                        VKApiUserFull user = dataProvider.getUserById(msg.user_id);

                                        if (bAddUser) {

                                            if (user != null) {
                                                str1 += user.first_name + " " + user.last_name;
                                            } else
                                                str1 += "User#" + msg.user_id;
                                            if ((bAddChat) && (msg.chat_id > VKDialog.CHAT_UID_OFFSET)) {

                                                VKDialog dlg = dataProvider.getDialog(msg.chat_id);
                                                if ((dlg != null) && (dlg.message != null))
                                                    str1 += "(" + dlg.message.title + ")";

                                            }
                                            if (str1.length()>0)
                                             sbp.append(str1,new StyleSpan(Typeface.BOLD), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                            sbp.append(":\n");
                                            str1 += ": ";
                                            inboxStyle.addLine(str1);

                                        } else if (bAddChat) {

                                            if (msg.chat_id > VKDialog.CHAT_UID_OFFSET) {

                                                VKDialog dlg = dataProvider.getDialog(msg.chat_id);
                                                if ((dlg != null) && (dlg.message != null))
                                                    str1 += dlg.message.title + ": ";
                                            }

                                        }


                                        lastid = msg.user_id;
                                        lastchat = msg.chat_id;
                                    } else {
                                        str += " ";
                                    }
                                }
                                inboxStyle.addLine(msg.body);
                                sbp.append(msg.body+"\n");
                            }

                            map.put("inboxStyle", inboxStyle);
                            map.put("BigText",(CharSequence)sbp);
                        }


                        // Notify

                        String url = "https://vk.com/im";
                        if (nUnread == 1) {
                            if (l.get(0).chat_id > VKDialog.CHAT_UID_OFFSET)
                                url += "?sel=c" + (l.get(0).chat_id - VKDialog.CHAT_UID_OFFSET);
                            else
                                url += "?sel=" + l.get(0).chat_id;
                        }

                        map.put("url", url);

                    }
                }

            }
        } catch (Exception e) {
         Log.e("NotifPres",e.toString());
        }

        return map;
    }


    public @Nullable Map<String,Object> GetLastMessage(){

        if (dataProvider==null) return  null;

        HashMap<String,Object>  map = new HashMap<String,Object>();

        VKDialog dlg = dataProvider.getDialogs().get(0);
        VKApiMessage msg = dlg.message;
        VKApiUserFull user = dataProvider.getUserById(msg.user_id);

        map.put("Id",dlg.uid);


        String title = "";

        if (user != null) {

            if (new File(MyApplication.CacheDir, "img_cache/img_user_" + user.id + ".jpg").exists())
                map.put("LargeIcon",
                        Utils.decodeBitmapScaledSquare(MyApplication.CacheDir + "img_cache/img_user_" + user.id + ".jpg", 250));

            title = user.first_name + " " + user.last_name;
        } else {
            title = "User#" + msg.user_id;
        }
        if (dlg.uid > VKDialog.CHAT_UID_OFFSET) {
            if (dlg != null) {

                title += " (" + dlg.message.title + ")";
            }
        }

        map.put("Title",title);
        map.put("Text",msg.body);
        map.put("Date",msg.date);

        return  map;
    }


}