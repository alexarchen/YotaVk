package com.zakharchenko.yotavk.Presenter;

import com.zakharchenko.yotavk.Data.VKData;

/**
 * Created by zakharchenko on 08.11.2018.
 */
public class MainPresenter extends  Presenter{

    public MainPresenter(Listener listener,VKData data){
        super(listener,data);
    }

    void RefreshData(){

        dataProvider.RefreshDialogs();

    }

    @Override
    public void OnDataLoaded(Class s, int data) {
        super.OnDataLoaded(s, data);
    }
}
