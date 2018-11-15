package com.zakharchenko.yotavk.Presenter;

import android.os.Looper;
import android.util.Log;

import com.vk.sdk.api.model.VKApiUserFull;
import com.zakharchenko.yotavk.Data.VKData;
import com.zakharchenko.yotavk.Data.VKDataMock;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.MyApplication;

import junit.framework.Assert;
import junit.framework.TestCase;


import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by zakharchenko on 09.11.2018.
 */

public class ChatsPresenterTest extends TestCase implements ChatsPresenter.Listener {

    ChatsPresenter presenter;
    VKData d = new VKDataMock();

    public void setUp() throws Exception {
        super.setUp();

        d.init();

        presenter = new ChatsPresenter(this, d);
        changes =0;
    }

    int changes = 0;
    @Override
    public void onChanged() {
        changes++;
    }

    int opendialog =0;
    @Override
    public void openDialog(int uid) {
        opendialog=uid;
    }

    int loaded_true = 0;
    int loaded_false = 0;
    @Override
    public void showLoaded(boolean bLoaded) {
        if (bLoaded)
         loaded_true++;
        else
            loaded_false++;
    }

    public void tearDown() throws Exception {

        presenter.Destroy();
        presenter = null;
    }

    public void testGetDialog() throws Exception {

       Assert.assertNotNull(presenter.GetDialog(2));
       Assert.assertNotNull(presenter.GetDialog(VKDialog.CHAT_UID_OFFSET + 1));
       Assert.assertNotNull(presenter.GetDialog(-1));
       Assert.assertNull(presenter.GetDialog(-2));

    }

    public void testGetDialogs() throws Exception {

        List<VKDialog> dialogList = presenter.GetDialogs();
        Assert.assertNotNull(dialogList);
        Assert.assertTrue(dialogList.size() >= d.getDialogs().size());
        Assert.assertTrue(dialogList.size() <= d.getDialogs().size()+d.getFriends().size());

        Assert.assertEquals(2, dialogList.get(0).uid);
        Assert.assertNotNull(dialogList.get(0).message);
        Assert.assertNotNull(dialogList.get(1).message);
        Assert.assertNotNull(dialogList.get(2).message);
    }

    public synchronized  void testRefresh() throws Exception {
        int l = changes;
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();
                presenter.Refresh();

                Looper.loop();

            }
        }).start();

        try {
            wait(VKDataMock.WAIT_DELAY + 500);
        }
        catch (Exception e){
            Log.d("TEST","Wait interrupted: "+e.toString());}
        Assert.assertTrue(l < changes);
        Assert.assertEquals(1, loaded_true); // calls 2 times

        testGetDialogs();
    }
/*
    public synchronized void TestConcurrent() throws Exception{

        final int res[] = {0};

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int q=0;q<1000;q++){
                    try {
                        presenter.GetDialogs().clear();
                        presenter.CreateChat(new int[]{1}, "BBB");


                    }
                    catch (Exception e){

                        res[0]++;
                    }
                }
                notify();
            }
        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int q=0; q < 1000; q++) {
                    try {
                        presenter.GetDialogs().clear();
                        presenter.CreateChat(new int[]{1}, "AAA");

                    }
                    catch (Exception e){

                        res[0]++;
                    }

                }

                notify();

            }
        }).start();

        wait();
        wait();
        Assert.assertTrue(res[0] == 0);
        Assert.assertEquals(2000+3,presenter.GetDialogs().size());
    }
*/
    public void testGetUser() throws Exception {

        Assert.assertNotNull(presenter.GetUser(1));
        Assert.assertNotNull(presenter.GetUser(2));
        Assert.assertNotNull(presenter.GetUser(-1));
        VKApiUserFull user = presenter.GetUser(-100);
        Assert.assertNotNull(user);
        Assert.assertEquals(-100,user.id);
    }

    public synchronized void testCreateChat() throws Exception {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();
                presenter.CreateChat(new int[]{1, 2}, "Title");
                Looper.loop();

            }
        }).start();

        try {
            wait(VKDataMock.WAIT_DELAY+100);
        }
        catch (Exception e){
            Log.d("TEST","Wait interrupted: "+e.toString());}

        Assert.assertEquals(((VKDataMock)d).CHATID-1,opendialog-VKDialog.CHAT_UID_OFFSET);
        Assert.assertEquals("Title",presenter.GetDialog(opendialog).message.title);

    }

    public void testOnDataLoaded() throws Exception {

        int l = changes;
        presenter.OnDataLoaded(VKDialog.class, 0);
        presenter.OnDataLoaded(VKDialog.class, 1);
        presenter.OnDataLoaded(VKApiUserFull.class, 1);
        Assert.assertEquals(l + 3, changes);
        Assert.assertEquals(1, loaded_true);
        Assert.assertEquals(3,loaded_false);
    }
}