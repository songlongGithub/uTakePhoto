package com.sl.utakephoto_lib.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.sl.utakephoto_lib.crop.CropOptions;

import java.util.List;

/**
 * Intent工具类用于生成拍照、
 * 从相册选择照片，裁剪照片所需的Intent
 * Author: JPH
 * Date: 2016/6/7 0007 13:41
 */
public class IntentUtils {
    private static final String TAG = IntentUtils.class.getName();

    /**
     * 获取图片多选的Intent
     *
     * @param limit 最多选择图片张数的限制
     */
//    public static Intent getPickMultipleIntent(TContextWrap contextWrap, int limit) {
//        Intent intent = new Intent(contextWrap.getActivity(), AlbumSelectActivity.class);
//        intent.putExtra(Constants.INTENT_EXTRA_LIMIT, limit > 0 ? limit : 1);
//        return intent;
//    }

    /**
     * 获取裁剪照片的Intent
     *
     * @param targetUri 要裁剪的照片
     * @param outPutUri 裁剪完成的照片
     * @param options   裁剪配置
     * @return
     */
    public static Intent getCropIntentWithOtherApp(Uri targetUri, Uri outPutUri, CropOptions options) {
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
//
//    public static Uri changeUri(Uri targetUri) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            ParcelFileDescriptor pfd = null;
//            try {
//                pfd = Utils.getContext().getContentResolver().openFileDescriptor(targetUri, "r");
//                if (pfd != null) {
//                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
//                    File file = new File(Utils.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/bodivis/" + System.currentTimeMillis() + ".jpg");
//                    Uri uri = Uri.fromFile(file);
//                    TImageFiles.writeToFile(bitmap, uri);
//                    return TUriParse.checkUri(Utils.getContext(), uri);
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } finally {
//                if (pfd != null) {
//                    try {
//                        pfd.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//
//        return targetUri;
//    }

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
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//设置Action为拍照
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);//将拍取的照片保存到指定URI
        return intent;
    }

    /**
     * 获取选择照片的Intent
     *
     * @return
     */
    public static Intent getPickIntentWithGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);//Pick an item from the data
        intent.setType("image/*");//从所有图片中进行选择
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
