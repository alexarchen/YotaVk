package com.zakharchenko.yotavk.Presenter;

import android.support.annotation.Nullable;
import android.util.Log;

import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKList;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.MyApplication;

import java.lang.annotation.Documented;
import java.util.List;

/**
 * Created by zakharchenko on 08.11.2018.
 */
public class ChatsPresenter  extends  Presenter{


    public interface Listener extends Presenter.Listener{
        public void openDialog(int uid);
    }

    public ChatsPresenter(Listener listener,VKData dataProvider){
        super(listener,dataProvider);

        Refresh();
    }

    @Override
    public void Attach(Presenter.Listener listener, VKData dataProvider) {
        super.Attach(listener, dataProvider);

        Refresh();
    }

    public @Nullable VKDialog GetDialog(int id){
        if (dataProvider!=null)
            return  dataProvider.getDialog(id);

        return null;
    }

    public List<VKDialog> GetDialogs(){

        if (dataProvider==null) return null;

        List<VKDialog> dialogs = new VKList<>(dataProvider.getDialogs());
        List<VKApiUserFull> friends = dataProvider.getFriends();

            if (friends != null) {
                for (VKApiUserFull friend  : friends){
                    boolean found = false;
                    for (VKDialog d:dialogs){
                        if (d.uid==friend.id) {found = true; break;}
                    }

                    if (!found) {
                        VKDialog dlg = new VKDialog();
                        dlg.uid = friend.id;
                        dlg.message = null;
                        dialogs.add(dlg);
                    }
                }
            }

      return dialogs;
    }

    public void Refresh(){

        if (listener!=null) listener.showLoaded(true);
        if (dataProvider!=null)
            dataProvider.RefreshDialogs();
    }

    public @Nullable VKApiUserFull GetUser(int id){

        if (dataProvider!=null)
         return  dataProvider.getUserById(id);

        return  null;

    }

    public void CreateChat(int [] ids,String title){

        if (dataProvider!=null) {
            if (listener!=null) listener.showLoaded(true);

            dataProvider.CreateChat(title, ids, new VKData.Notify() {
                @Override
                public void OnDataLoaded(Class s, int data) {
                    if (listener!=null) {
                        listener.showLoaded(false);
                        ((Listener) listener).openDialog(VKDialog.CHAT_UID_OFFSET+data);
                    }
                }
            });
        }

    }

    @Override
    public void OnDataLoaded(Class s, int data) {
        super.OnDataLoaded(s,data); //call listener
         Log.d("CP","OnDataLoaded");
        if (listener!=null) listener.showLoaded(false);
    }
}
