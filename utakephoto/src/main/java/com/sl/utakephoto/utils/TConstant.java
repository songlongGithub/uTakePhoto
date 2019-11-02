package com.sl.utakephoto.utils;

import android.content.Context;

/**
 * author : Sl
 * createDate   : 2019-10-1711:16
 * desc   :
 */
public class TConstant {

    public static final String TAG = "UTakePhoto";


    /**
     * ("选择的文件不是图片")
     */
    public static final int TYPE_NOT_IMAGE = 1;
    /**
     * ("保存选择的的文件失败")
     */
    public static final int TYPE_WRITE_FAIL = 2;
    /**
     * ("所选照片的Uri 为null")
     */
    public static final int TYPE_URI_NULL = 3;
    /**
     * ("从Uri中获取文件路径失败")
     */
    public static final int TYPE_URI_PARSE_FAIL = 4;
    /**
     * ("没有匹配到选择图片的Intent")
     */
    public static final int TYPE_NO_MATCH_PICK_INTENT = 5;
    /**
     * ("没有匹配到裁切图片的Intent")
     */
    public static final int TYPE_NO_MATCH_CROP_INTENT = 6;
    /**
     * ("没有相机")
     */
    public static final int TYPE_NO_CAMERA = 7;
    /**
     * ("选择的文件没有找到")
     */
    public static final int TYPE_NO_FIND = 8;
    /**
     * 没有权限
     */
    public static final int TYPE_NO_PERMISSION = 9;
    /**
     * 没有赋予权限
     */
    public static final int TYPE_DENIED_PERMISSION = 10;
    /**
     * 不再询问权限
     */
    public static final int TYPE_NEVER_ASK_PERMISSION = 11;
    /**
     * AndroidQ 权限未适配
     */
    public static final int TYPE_ANDROID_Q_PERMISSION = 12;
    /**
     * 其他
     */
    public static final int TYPE_OTHER = 13;

    public static String getFileProviderName(Context context) {
        return context.getPackageName() + ".fileProvider";
    }

}
