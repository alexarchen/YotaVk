package com.zakharchenko.yotavk.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.vk.sdk.api.model.VKApiDialog;
import com.vk.sdk.api.model.VKApiGetMessagesResponse;
import com.vk.sdk.api.model.VKList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by zakharchenko on 08.11.2018.
 */
    /* Class extends VKApi dialog and add Messages list to it */
public class VKDialog extends VKApiDialog implements Comparable<VKDialog> {
    public VKDialog(){
        super();
    }
    public static VKDialog COPY (@NonNull VKDialog dlg){
        try {

            Parcel p = Parcel.obtain();
            dlg.writeToParcel(p, 0);
            p.setDataPosition(0);
            VKDialog newdlg = new VKDialog(p);
            return newdlg;
        }
        catch (Exception e){
            Log.e("VKDialog","Copy error! "+e.toString());
        }
        return  new VKDialog();
    }

    @Override
    public int compareTo(@NonNull VKDialog vkDialog){
        if ((message==null) && (vkDialog.message==null))
        {
            //no messages in both, compare by user
            // chat
            if ((uid>VKDialog.CHAT_UID_OFFSET) && (vkDialog.uid>VKDialog.CHAT_UID_OFFSET)) return Integer.valueOf(vkDialog.uid).compareTo(uid);
            if (uid>VKDialog.CHAT_UID_OFFSET) return 1;
            if (vkDialog.uid>VKDialog.CHAT_UID_OFFSET) return -1;
            // not chat
            // group
            return Integer.valueOf(vkDialog.uid).compareTo(uid);
        }
        if (message==null) return 1;
        if (vkDialog.message==null) return -1;
        return Long.valueOf(vkDialog.message.date).compareTo(message.date);
    }

    final public static int CHAT_UID_OFFSET = 2000000000;
    public VKDialog(JSONObject from) throws JSONException
    {
        super(from);

        uid = message.user_id;

        try {
            chat_id = new Integer(from.optJSONObject("message").optString("chat_id"));

        }
        catch (Exception e){chat_id=0;}

        if (chat_id>0) uid = CHAT_UID_OFFSET+chat_id;

        JSON = from.toString();

        JSONArray msgs = from.optJSONArray("messages");
        if (msgs!=null) {
            Messages = new VKList<VKMessage>();
            for (int i=0;i<msgs.length();i++)
             Messages.add (new VKMessage(msgs.getJSONObject(i)));
        }

    }


    public static Creator<VKDialog> CREATOR = new Creator<VKDialog>() {
        public VKDialog createFromParcel(Parcel source) {
            VKDialog dlg = new VKDialog(source);
            return (dlg);
        }

        public VKDialog[] newArray(int size) {
            return new VKDialog[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, 0);
        dest.writeInt(uid);
        dest.writeInt(chat_id);
        dest.writeString(JSON);
        if (Messages==null) dest.writeInt(0);
        else {
            dest.writeInt(Messages.size());
            for (VKMessage message : Messages)
                message.writeToParcel(dest, 0);

        }
    }

    public VKDialog(Parcel in) {
        super(in);
        uid = in.readInt();
        chat_id = in.readInt();
        JSON = in.readString();
        int n = in.readInt();
        Messages = new VKList<>();
        for (int q=0;q<n;q++)
           Messages.add(new VKMessage(in));

         //Messages = new VKList<VKMessage>(in.readArrayList(VKList.class.getClassLoader()));
    }

    public boolean isMulti(){return (chat_id>0);}
    public VKList<VKMessage> Messages = new VKList<>();
    public List<VKMessage> getMessages() {return Messages;}

    public int uid;
    public int chat_id;
    public String JSON;
}
