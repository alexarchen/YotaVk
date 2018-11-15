package com.zakharchenko.yotavk.Data;

import android.os.Looper;
import android.util.Log;

import com.vk.sdk.api.model.VKList;
import com.zakharchenko.yotavk.Model.VKDialog;
import com.zakharchenko.yotavk.Model.VKMessage;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by zakharchenko on 13.11.2018.
 */
public class VKDataMockTest extends TestCase {


    @org.junit.Test
    public void testLoadCache() throws Exception {

        VKDataMock mock = new VKDataMock();
        mock.init();

        Assert.assertTrue(mock.getDialogs().size() > 0);

        // check message order, from newest to oldest
        Assert.assertTrue(mock.getDialogs().get(0).getMessages().get(0).date>=mock.getDialogs().get(0).getMessages().get(1).date);
    }

    public void testSendMessage() throws Exception{


    }

    public synchronized void testgetDialogByIdWithMessages() throws Exception{

        final VKDataMock mock = new VKDataMock();

        // test sort
        VKDialog dlg = new VKDialog();
        dlg.uid=1;
        dlg.message = new VKMessage();
        dlg.message.date = 1;
        dlg.message.read_state = false;
        mock.dialogs.add(dlg);
        dlg = new VKDialog();
        dlg.uid=2;
        dlg.message = new VKMessage();
        dlg.message.date = 2;
        dlg.message.read_state = false;
        mock.dialogs.add(dlg);
        dlg = new VKDialog();
        dlg.uid=3;
        dlg.message = new VKMessage();
        dlg.message.read_state = false;
        dlg.message.date = 3;
        mock.dialogs.add(dlg);

        List<VKDialog> list = mock.getDialogs();
        Assert.assertEquals(1,list.get(0).uid);
        Assert.assertEquals(3, list.get(2).uid);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                mock.getDialogByIdWithMessages(1);

                Looper.loop();
            }
        }).start();

        try {
            wait(VKDataMock.WAIT_DELAY + 500);
        }
        catch (Exception e){
            Log.d("TEST", "Wait interrupted: " + e.toString());}

        list = mock.getDialogs();

        Assert.assertEquals(3,list.size());
        Assert.assertEquals(3,list.get(0).uid);
        Assert.assertEquals(1,list.get(2).uid);

    }


}