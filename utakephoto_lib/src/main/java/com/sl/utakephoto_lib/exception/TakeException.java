package com.sl.utakephoto_lib.exception;

import com.sl.utakephoto_lib.manager.TakePhotoManager;

/**
 * author : Sl
 * createDate   : 2019-10-1713:15
 * desc   :
 */
public class TakeException extends Exception {
    private @TakeExceptionType
    int exceptopnType;

    public TakeException(@TakeExceptionType int takeExceptionType) {
        super(TakePhotoManager.ERROR_ARRAY.get(takeExceptionType));
        this.exceptopnType = takeExceptionType;
    }

    public TakeException(@TakeExceptionType int takeExceptionType, String msg) {
        super(msg);
        this.exceptopnType = takeExceptionType;
    }

    public int getExceptopnType() {
        return exceptopnType;
    }
}
