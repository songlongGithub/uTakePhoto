package com.sl.utakephoto.exception;

import androidx.annotation.IntDef;

import com.sl.utakephoto.utils.TConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author : Sl
 * createDate   : 2019-10-1713:09
 * desc   :错误类型
 */
@IntDef({TConstant.TYPE_NOT_IMAGE, TConstant.TYPE_WRITE_FAIL, TConstant.TYPE_URI_NULL, TConstant.TYPE_URI_PARSE_FAIL, TConstant.TYPE_NO_MATCH_PICK_INTENT, TConstant.TYPE_NO_MATCH_CROP_INTENT,
        TConstant.TYPE_NO_CAMERA, TConstant.TYPE_NO_FIND, TConstant.TYPE_NO_PERMISSION,TConstant.TYPE_DENIED_PERMISSION, TConstant.TYPE_NEVER_ASK_PERMISSION, TConstant.TYPE_ANDROID_Q_PERMISSION, TConstant.TYPE_OTHER})
@Retention(RetentionPolicy.SOURCE)
@interface TakeExceptionType {


}
