package com.sl.utakephoto_lib.exception;

import androidx.annotation.IntDef;

import com.sl.utakephoto_lib.utils.TConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.sl.utakephoto_lib.utils.TConstant.TYPE_ANDROID_Q_PERMISSION;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NEVER_ASK_PERMISSION;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NOT_IMAGE;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NO_CAMERA;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NO_FIND;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NO_MATCH_CROP_INTENT;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NO_MATCH_PICK_INTENT;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_NO_PERMISSION;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_OTHER;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_URI_NULL;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_URI_PARSE_FAIL;
import static com.sl.utakephoto_lib.utils.TConstant.TYPE_WRITE_FAIL;

/**
 * @author : Sl
 * createDate   : 2019-10-1713:09
 * desc   :错误类型
 */
@IntDef({TYPE_NOT_IMAGE, TYPE_WRITE_FAIL, TYPE_URI_NULL, TYPE_URI_PARSE_FAIL, TYPE_NO_MATCH_PICK_INTENT, TYPE_NO_MATCH_CROP_INTENT,
        TYPE_NO_CAMERA, TYPE_NO_FIND, TYPE_NO_PERMISSION,TConstant.TYPE_DENIED_PERMISSION, TYPE_NEVER_ASK_PERMISSION, TYPE_ANDROID_Q_PERMISSION, TYPE_OTHER})
@Retention(RetentionPolicy.SOURCE)
@interface TakeExceptionType {


}
