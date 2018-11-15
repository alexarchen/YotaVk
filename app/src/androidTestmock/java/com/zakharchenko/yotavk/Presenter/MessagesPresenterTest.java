package com.zakharchenko.yotavk.Presenter;

import android.os.Looper;
import android.util.Log;

import com.zakharchenko.yotavk.Data.VKDataMock;
import com.zakharchenko.yotavk.Model.VKDialog;

import junit.framework.Assert;
import junit.framework.TestCase;

import static org.junit.Assert.*;

/**
 * Created by zakharchenko on 09.11.2018.
 */
public class MessagesPresenterTest extends TestCase implements MessagesPresenter.MessageListener{


    MessagesPresenter presenter;
    VKDataMock vk;

    @org.junit.Before
    public void setUp() throws Exception {
       vk = new VKDataMock();
       vk.init();

       changes =0;
    }

    int changes =0;
    @Override
    public void onChanged() {
     changes++;
    }

    @Override
    public void showLoaded(boolean bLoaded) {

    }

    int newmesgcalls = 0;
    @Override
    public void onNewMessage() {
      newmesgcalls++;
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void testMakeRead() throws Exception {

    }

    @org.junit.Test
    public void testGetDialog() throws Exception {

        presenter = new  MessagesPresenter(this,vk,vk.getDialogs().get(0).uid);
        Assert.assertEquals(vk.getDialogs().get(0).uid, presenter.GetDialog().uid);
        VKDialog dlg = presenter.GetDialog();
        Assert.assertNotSame(dlg, presenter.GetDialog());
        Assert.assertNotNull(dlg.Messages);
        presenter.Destroy();


        presenter.Attach(this,vk);
        Assert.assertEquals(vk.getDialogs().get(0).uid, presenter.GetDialog().uid);
        dlg = presenter.GetDialog();
        Assert.assertNotSame(dlg, presenter.GetDialog());
        Assert.assertNotNull(dlg.Messages);
        presenter.Destroy();

        presenter = new  MessagesPresenter(this,vk,vk.getDialogs().get(1).uid);
        Assert.assertEquals(vk.getDialogs().get(1).uid, presenter.GetDialog().uid);
        dlg = presenter.GetDialog();
        Assert.assertNotSame(dlg, presenter.GetDialog());
        Assert.assertNotNull(dlg.Messages);
        presenter.Destroy();

    }

    @org.junit.Test
    public synchronized void testSendMessage() throws Exception {

        int uid = vk.getDialogs().get(1).uid;
        presenter = new  MessagesPresenter(this,vk,uid);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                presenter.SendMessage("AAAA");
                Looper.loop();
            }
        }).start();

        try {
            wait(VKDataMock.WAIT_DELAY*3 + 500);
        }
        catch (Exception e){
            Log.d("TEST", "Wait interrupted: " + e.toString());}


        // New message should arrive
        Assert.assertEquals(1, newmesgcalls);
        // dialog must blow up
        Assert.assertEquals(presenter.GetDialog().uid, vk.getDialogs().get(0).uid);
        Assert.assertTrue(presenter.GetDialog().unread > 0);
        int nmsgs = presenter.GetDialog().getMessages().size();
        Assert.assertTrue(nmsgs>=2);
        Assert.assertEquals(presenter.GetDialog().getMessages().get(nmsgs-1).body, "AAAA");
        Assert.assertEquals(presenter.GetDialog().getMessages().get(nmsgs-2).body,"AAAA");
        Assert.assertFalse(presenter.GetDialog().getMessages().get(nmsgs - 1).out);
        Assert.assertTrue(presenter.GetDialog().getMessages().get(nmsgs - 2).out);


    }

    @org.junit.Test
    public void testGetUser() throws Exception {

    }

    @org.junit.Test
    public void testLoadImage() throws Exception {

    }

    @org.junit.Test
    public void testClearTasks() throws Exception {

    }

    public void testDestroy() throws  Exception{

        // Test call after Destroy, need for calling after onPause
        presenter = new  MessagesPresenter(this,vk,1);
        presenter.Destroy();
        Assert.assertNull(presenter.GetDialog());
    }

}