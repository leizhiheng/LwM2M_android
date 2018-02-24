package com.cwtcn.leshanandroidlib.clientmanage;

import android.graphics.Bitmap;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IMainPresenter {
    void register();
    void destroyClient();
    Bitmap encodeQRCode(String content, int width, int height);
    void checkRegistrationId();
}
