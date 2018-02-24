package com.cwtcn.leshanandroidlib;

import android.app.Application;
import android.content.Intent;

import com.cwtcn.leshanandroidlib.clientmanage.ClientService;

/**
 * Created by leizhiheng on 2018/1/30.
 */

public class LeshanApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        startClientService();
    }

    public void startClientService() {
        Intent intent = new Intent(this, ClientService.class);
        startActivity(intent);
    }
}
