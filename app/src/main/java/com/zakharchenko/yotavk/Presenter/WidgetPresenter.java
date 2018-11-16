package com.zakharchenko.yotavk.Presenter;

import com.vk.sdk.api.model.VKApiUserFull;
import com.zakharchenko.yotavk.Data.VKData;

/**
 * Created by zakharchenko on 16.11.2018.
 */
public class WidgetPresenter extends ChatsPresenter {

     public WidgetPresenter(ChatsPresenter.Listener listener,VKData data){
         super(listener,data);
    }

    @Override
    public void OnDataLoaded(Class s, int data) {
        if ((s!=VKApiUserFull.class) || (data==0))
            super.OnDataLoaded(s,data);
    }


}
