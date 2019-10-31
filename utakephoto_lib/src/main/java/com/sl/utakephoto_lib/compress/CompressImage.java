package com.sl.utakephoto_lib.compress;

import android.net.Uri;


/**
 * 压缩照片2.0
 * <p>
 * Author JPH
 * Date 2015-08-26 下午1:44:26
 */
public interface CompressImage {
    void compress();

    /**
     * 压缩结果监听器
     */
    interface CompressListener {
        void onStart();

        /**
         * 压缩成功
         *
         * @param images 已经压缩图片
         */
        void onSuccess(Uri images);


        void onError(Throwable obj);

    }
}
