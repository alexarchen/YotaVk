package com.zakharchenko.yotavk.Model;

import android.os.Parcel;

import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by zakharchenko on 08.11.2018.
 */
// Adds chat_id and saved JSON string to VKApiMessage
public class VKMessage extends VKApiMessage {

    public VKMessage(){super();date = new Date().getTime()/1000;}

    public VKMessage(VKApiMessage msg,int _chat_id){
        super();

        Parcel in = Parcel.obtain();
        msg.writeToParcel(in,0);

        in.setDataPosition(0);

        this.id = in.readInt();
        this.user_id = in.readInt();
        this.date = in.readLong();
        this.read_state = in.readByte() != 0;
        this.out = in.readByte() != 0;
        this.title = in.readString();
        this.body = in.readString();
        this.attachments = in.readParcelable(VKAttachments.class.getClassLoader());
        this.fwd_messages = in.readParcelable(VKList.class.getClassLoader());
        this.emoji = in.readByte() != 0;
        this.deleted = in.readByte() != 0;

        chat_id = _chat_id;
    }

    public VKMessage(JSONObject from) throws JSONException
    {
        super(from);

        try {
            chat_id = from.getInt("chat_id");
        }
        catch (Exception e){}

        if (chat_id==0) chat_id = user_id;
        else
            chat_id+=VKDialog.CHAT_UID_OFFSET;

        JSON = from.toString();

    }

    public int chat_id=0; // chat id is the same as in dialog.uid user_id or 2000000000+chatid

    public static Creator<VKMessage> CREATOR = new Creator<VKMessage>() {
        public VKMessage createFromParcel(Parcel source) {
            return new VKMessage(source);
        }

        public VKMessage[] newArray(int size) {
            return new VKMessage[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(chat_id);
    }

    public VKMessage(Parcel p){

        super(p);
        chat_id = p.readInt();
    }
    String JSON;
}

