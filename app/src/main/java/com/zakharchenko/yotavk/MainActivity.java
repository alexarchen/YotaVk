package com.zakharchenko.yotavk;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.methods.VKApiBase;
import com.vk.sdk.util.VKUtil;
import com.yotadevices.sdk.EpdIntentCompat;


// Login activity

public class MainActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private boolean isReceiverRegistered;

    // VK present
    boolean isVK(){

            boolean bMyVK = false;
            try {
                PackageInfo pI = getPackageManager().getPackageInfo("com.vkontakte.android", 0);

                if (pI!=null) bMyVK = true;
            }
            catch (Exception e){}
            return bMyVK;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "MainActivity start");



        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                boolean sentToken = sharedPreferences
                        .getBoolean("SENT_TOKEN_TO_SERVER", false);

              /*  if (sentToken)
                    Log.d(TAG,"GCM Send message");
                else
                    Log.d(TAG,"GCM ERROR message");*/
            }
        };


                // Registering BroadcastReceiver
                registerReceiver();



        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_notif", true)) {

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
        }

        //PreferenceManager.getDefaultSharedPreferences(this).edit().putString("notif_time","10").commit();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Log.d(TAG,"Start registration");
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        else Log.d(TAG, "Play serice is not available");


        if (!VKSdk.isLoggedIn())
        {

            VKSdk.login(this, "messages,notifications,stats,friends,notify");

            setContentView(R.layout.login);

        }
        else
        {

            startService(new Intent(getApplicationContext(), BSWidget.WidgetService.class));

/*           new Handler().postDelayed(new Runnable() {
                                          @Override
                                          public void run() {
                                              startService(new Intent(getApplicationContext(), BSNotifActivity.class));
                                          }
                                      },1000);
*/

           if (!PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("firsttime",true)) {

               new Handler().postDelayed(new Runnable() {
                   @Override
                   public void run() {

                       if ((getIntent().getAction() != null) && (getIntent().getAction().equals(Intent.ACTION_VIEW))) {

                           if ((getIntent().getData() != null) && (getIntent().getData().toString().contains("https://vk.com/im")))
                           {
                               if  ((PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("use_VK", true))
                                   && (isVK())) {
                                   Intent intent1 = new Intent();
                                   intent1.setPackage("com.vkontakte.android");
                                   intent1.setData(getIntent().getData());
                                   intent1.setAction(Intent.ACTION_VIEW);
                                   intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                   startActivity(intent1);
                               }
                               else {
                                   if (getIntent().getData().toString().equals("https://vk.com/im"))
                                    startActivity(new Intent(MainActivity.this, ChatsList.class));
                                   else {
                                   // TODO: call MessageList
                                       startActivity(new Intent(MainActivity.this, ChatsList.class));

                                   }
                               }


                           } else {
                               if (Build.VERSION.SDK_INT>=23)
                                 startActivity(new Intent(MainActivity.this, ChatsList.class));
                               else
                                   startService(new Intent(MainActivity.this, BSChatsList.class));
                           }
                       } else
                           startActivity(new Intent(MainActivity.this, ChatsList.class));

                       finish();

                   }
               }, 100);
           }
            else
               setContentView(R.layout.activity_main);

        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {

            @Override
            public void onResult(VKAccessToken res) {
// Пользователь успешно авторизовался
                MyApplication app = ((MyApplication) getApplication());
                app.vkAccessToken = res;

                MyApplication.GetMessages(app.defListener);
                MyApplication.GetFriends(app.defListener);

                if (!PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("firsttime", true)) {

                    if (getIntent().getAction().equals(Intent.ACTION_VIEW)){
                        if (MyApplication.isYotaphoneSDK2())
                            startService(new Intent(MainActivity.this, BSChatsList.class));
                        else
                        if (MyApplication.isClass("EpdIntentCompat")) {
                            Intent intent = new Intent(MainActivity.this, ChatsList.class);
                            EpdIntentCompat.setEpdFlags(intent, EpdIntentCompat.FLAG_ACTIVITY_KEEP_ON_EPD_SCREEN);
                            startActivity(intent);
                        }
                        else
                            startActivity(new Intent(MainActivity.this, ChatsList.class));

                    } else {
                        startActivity(new Intent(MainActivity.this, ChatsList.class));
                    }
                    finish();
                }
                else setContentView(R.layout.activity_main);


                Log.d(TAG, "Get Access Token!");

            }

            @Override
            public void onError(VKError error) {
// Произошла ошибка авторизации (например, пользователь запретил авторизацию)
                ((MyApplication) getApplication()).vkAccessToken = null;

                setContentView(R.layout.login);
                ((TextView)findViewById(R.id.textView)).setText(getString(R.string.error_login)+" "+error);

            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    public void onClick(View v){

        if (getIntent().getAction().equals(Intent.ACTION_VIEW)){
           if (MyApplication.isYotaphoneSDK2())
             startService(new Intent(MainActivity.this, BSChatsList.class));
           else
            if (MyApplication.isClass("EpdIntentCompat")) {
                Intent intent = new Intent(MainActivity.this, ChatsList.class);
                EpdIntentCompat.setEpdFlags(intent, EpdIntentCompat.FLAG_ACTIVITY_KEEP_ON_EPD_SCREEN);
                startActivity(intent);
            }
            else
                startActivity(new Intent(MainActivity.this, ChatsList.class));

        } else {
            startActivity(new Intent(MainActivity.this, ChatsList.class));
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
        editor.putBoolean("firsttime",false);
        editor.commit();
        finish();
    }
    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter("REGISTRATION_COMPLETE"));
            isReceiverRegistered = true;
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}