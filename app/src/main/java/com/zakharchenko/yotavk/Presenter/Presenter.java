package com.zakharchenko.yotavk.Presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.MyApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zakharchenko on 08.11.2018.
 *
 * Base class for all presenters
 */


public class Presenter implements VKData.Notify {
  public Presenter(Listener listener,VKData dataProvider){
      Attach(listener,dataProvider);
  }

  public void Attach(Listener listener,VKData dataProvider){
      this.listener = listener;
      if (dataProvider!=null) {
          this.dataProvider = dataProvider;

          if (listener!=null)
           dataProvider.RegisterListener(this);
      }
  }

  VKData dataProvider;

  // called when corresponding data changed
  public void OnDataLoaded(Class s,int data){
   if (listener!=null) listener.onChanged();
  }

  public interface Listener {
        public void onChanged();
        public void showLoaded(boolean bLoaded);
  }

    public void Destroy(){
        if ((dataProvider!=null) && (listener!=null))
            dataProvider.UnregisterListener(this);
        dataProvider = null;
        listener = null;
    }

  Listener listener;

}
