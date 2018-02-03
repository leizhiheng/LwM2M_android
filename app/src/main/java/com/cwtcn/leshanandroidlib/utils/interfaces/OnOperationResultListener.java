package com.cwtcn.leshanandroidlib.utils.interfaces;

/**
 * Created by leizhiheng on 2018/2/3.
 *
 * 在Client与Service交互的一些操作过程中，通过这些接口将操作情况返回给主线程
 */
public interface OnOperationResultListener {

    /**
     * Client开始向Service执行请求
     */
    void onStartOperate();

    /**
     * Client向Service执行的请求被拒绝
     * @param rejectReason 拒绝原因
     */
    void onOperateReject(String rejectReason);

    /**
     * Client向Service执行请求的结果
     * @param resultCode 结果码
     * @param msg 附加信息
     */
    void onOperateResult(int resultCode, String msg);
}
