package com.sl.utakephoto.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.sl.utakephoto.crop.CropOptions;

import java.util.List;

/**
 * Intent工具类
 */
public class IntentUtils {
    private static final String TAG = IntentUtils.class.getName();


    /**
     * 获取裁剪照片的Intent
     *
     * @param targetUri 要裁剪的照片
     * @param outPutUri 裁剪完成的照片
     * @param options   裁剪配置
     * @return
     */
    public static Intent getCropIntent(Uri targetUri, Uri outPutUri, CropOptions options) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.setDataAndType(targetUri, "image/*");
        intent.putExtra("crop", "true");
        if (options.getAspectX() * options.getAspectY() > 0) {
            intent.putExtra("aspectX", options.getAspectX());
            intent.putExtra("aspectY", options.getAspectY());
        }
        if (options.getOutputX() * options.getOutputY() > 0) {
            intent.putExtra("outputX", options.getOutputX());
            intent.putExtra("outputY", options.getOutputY());
        }
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        return intent;
    }

    public static boolean intentAvailable(Context context, Intent intent) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 获取拍照的Intent
     *
     * @return
     */
    public static Intent getCaptureIntent(Uri outPutUri) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
        return intent;
    }

    /**
     * 获取选择照片的Intent
     *
     * @return
     */
    public static Intent getAlbumIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        return intent;
    }

    /**
     * 获取从文件中选择照片的Intent
     *
     * @return
     */
    public static Intent getPickIntentWithDocuments() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return intent;
    }
}
