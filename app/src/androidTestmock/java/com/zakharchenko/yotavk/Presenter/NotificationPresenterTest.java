package com.zakharchenko.yotavk.Presenter;

import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import com.zakharchenko.yotavk.Data.VKDataMock;
import com.zakharchenko.yotavk.Model.VKDialog;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by zakharchenko on 14.11.2018.
 */
public class NotificationPresenterTest extends TestCase implements NotificationPresenter.Listener {

    NotificationPresenter presenter;
    VKDataMock mock;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mock = new VKDataMock();
        mock.init();
        presenter = new NotificationPresenter(this,mock);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        presenter.Destroy();
        presenter = null;
    }

    public void testGetNotificationInformation() throws Exception {

        Map<String,Object> map = presenter.GetNotificationInformation();

        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey("nUnreadMsg"));
        Assert.assertTrue(map.containsKey("nUnread"));
        Assert.assertTrue(map.containsKey("Text"));
        Assert.assertTrue(map.containsKey("Title"));
        Assert.assertTrue(map.containsKey("inboxStyle"));
        Assert.assertTrue(map.containsKey("url"));
        Assert.assertFalse(map.containsKey("LargeIcon"));
        Assert.assertTrue(map.containsKey("Date"));

        Assert.assertTrue(map.get("nUnreadMsg").getClass()==Integer.class);
        Assert.assertTrue(map.get("nUnread").getClass()==Integer.class);
        Assert.assertTrue(map.get("Text").getClass()==String.class);
        Assert.assertTrue(map.get("Title").getClass()==String.class);
        Assert.assertTrue(map.get("inboxStyle").getClass()== NotificationCompat.InboxStyle.class);
        Assert.assertTrue(map.get("url").getClass()==String.class);
        Assert.assertTrue(map.get("Date").getClass()==Long.class);

        int nUnreadMsg =0;

        for (VKDialog dlg:mock.getDialogs())
            nUnreadMsg+=dlg.unread;
        Assert.assertEquals(nUnreadMsg,(int)(Integer)map.get("nUnreadMsg"));

    }

    @Override
    public void onNotify(boolean bNewMsg) {

    }

    @Override
    public void onChanged() {

    }

    @Override
    public void showLoaded(boolean bLoaded) {

    }

    public void testGetLastMessage() throws Exception {
        Map<String,Object> map = presenter.GetLastMessage();

        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey("Id"));
        Assert.assertTrue(map.containsKey("Text"));
        Assert.assertTrue(map.containsKey("Title"));
        Assert.assertTrue(map.containsKey("Date"));

        Assert.assertTrue(map.get("Text").getClass()==String.class);
        Assert.assertTrue(map.get("Title").getClass()==String.class);
        Assert.assertTrue(map.get("Date").getClass()==Long.class);
        Assert.assertTrue(map.get("Id").getClass() == Integer.class);

        Assert.assertEquals(mock.getDialogs().get(0).uid,(int)(Integer)map.get("Id"));
    }
}