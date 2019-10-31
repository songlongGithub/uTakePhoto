package com.sl.utakephoto_lib.manager;

import android.net.Uri;

import com.sl.utakephoto_lib.exception.TakeException;

import java.util.List;

/**
 * @author : Sl
 * createDate   : 2019-10-16 17:15
 * desc   :返回结果回调
 */
public interface ITakePhotoResult {

    void takeSuccess(List<Uri> uriList);

    void takeFailure(TakeException ex);

    void takeCancel();
}
