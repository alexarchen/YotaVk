package com.zakharchenko.yotavk.Presenter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.vk.sdk.api.model.VKApiUserFull;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;
import com.zakharchenko.yotavk.MyApplication;

import junit.framework.Assert;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zakharchenko on 08.11.2018.
 *
 * Presenter for message list (open dialog)
 *
 * constructor refreshed dialog data, call it on onResume
 *
 * call Destroy() on Activity onPause
 */
public class MessagesPresenter extends Presenter {

    int uid;
    public interface MessageListener extends Listener{
        public void onNewMessage();
    }

    public MessagesPresenter(MessageListener l,VKData prov,int uid){
        super(l,prov);
        this.uid = uid;

        // start loading dialog messages
        if (dataProvider!=null)
          dataProvider.getDialogByIdWithMessages(uid);
        l.showLoaded(true);
    }

    @Override
    public void Attach(@NonNull Listener listener, VKData dataProvider) {
        super.Attach(listener, dataProvider);

        if ((dataProvider!=null) && (uid!=0))
            dataProvider.getDialogByIdWithMessages(uid);

         listener.showLoaded(true);

    }

    // Set read status of all messages of current dialog
    public void MakeRead(){
        if (dataProvider!=null)
         dataProvider.MarkAsRead(uid);
    }
    // Return current dialog
    public VKDialog GetDialog() {

       if (dataProvider==null) return null;

        VKDialog dlg = VKDialog.COPY(dataProvider.getDialog(uid));
        Collections.reverse(dlg.Messages);
        return dlg;
    }

    public void SendMessage(String text){
        if (dataProvider!=null)
            dataProvider.SendMessage(uid, text);
    }

    @Override
    public void OnDataLoaded(Class s, int data) {
        // call change only if current user or current dialog was changed

        if ((s== VKMessage.class) && (data==uid)){
            // new message

            if (listener!=null)
                ((MessageListener)listener).onNewMessage();

            if (dataProvider!=null)
             dataProvider.getDialogByIdWithMessages(uid);

        }

        if ((s==VKDialog.class) && ((data==uid)))
         if (listener!=null) listener.showLoaded(false);

        if ((s==VKDialog.class) && ((data==uid) || (data==0)))
            if (listener!=null) listener.onChanged();
        if ((s==VKApiUserFull.class) && ((data==uid) || (data==0)))
            if (listener!=null) listener.onChanged();

    }


    // ImageLoadTask class to call LoadImage
    public static class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
       /* private ImageView imageView=null;
        private LevelListDrawable d=null;
        private TextView tv=null;
*/

        public ImageLoadTask(String url) {
            this.url = url;
        //    this.imageView = imageView;
        }

        /*public ImageLoadTask(String url, LevelListDrawable  d,TextView tv) {
            this.url = url;
            this.d = d;
            this.tv = tv;
        }
*/
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

                    Log.d("IMAGE", "From cache");
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
            /*
            if (imageView!=null)
                imageView.setImageBitmap(result);
            if (d!=null) {
                BitmapDrawable dr = new BitmapDrawable(result);
                d.addLevel(1, 1, dr);
                d.setLevel(1);
                CharSequence t = tv.getText();
                tv.setText(t);
            }
            */
        }

    }

    public VKApiUserFull getUser(int id){
        return dataProvider!=null?dataProvider.getUserById(id):null;

    }

    List<ImageLoadTask> imageTasks = new ArrayList<ImageLoadTask>();

    // Create new ImageLoadTask and override onPostExecute method before pass to LoadImage
    public void LoadImage(ImageLoadTask task){
        imageTasks.add(task);
        task.execute();
    }


    void ClearTasks(){
        for (ImageLoadTask task:imageTasks){
            task.cancel(false);
        }
        for (ImageLoadTask task : imageTasks)
         task.cancel(true);
        imageTasks.clear();
    }

    @Override
    public void Destroy() {
        super.Destroy();

        ClearTasks();
    }
}