package com.cwtcn.leshanandroidlib.presenter;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by leizhiheng on 2018/1/16.
 */

public interface IMainPresenter {
    void register();
    void destroyClient();
    void updateLocation();
    Bitmap encodeQRCode(String content, int width, int height);
}
