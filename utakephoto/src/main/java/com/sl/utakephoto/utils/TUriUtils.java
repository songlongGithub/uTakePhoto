package com.sl.utakephoto.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.sl.utakephoto.exception.TakeException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * author : Sl
 * createDate   : 2019-10-1711:14
 * desc   :
 */
public class TUriUtils {
    /**
     * uri检查 将scheme为file的转换为FileProvider提供的uri，判断输入的SCHEME_FILE格式的在androidQ上是否能够使用
     *
     * @param context
     * @param uri
     * @param suffix  默认.jpg
     * @return
     */
    public static Uri checkTakePhotoUri(Context context, Uri uri, String suffix) throws TakeException {
        if (uri == null) return getTempSchemeContentUri(context, suffix);
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Environment.isExternalStorageLegacy()) {
                    Log.w(TConstant.TAG, "当前是Legacy View视图，兼容File方式访问");
                    File file = new File(uri.getPath());
                    if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                    return getUriFromFile(context,file);
                } else {
                    if (checkAppSpecific(uri, context)) {
                        File file = new File(uri.getPath());
                        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                        return getUriFromFile(context,file);
                    } else {
                        Log.w(TConstant.TAG, "当前存储空间视图模式是Filtered View，不能直接访问App-specific外的文件");
                        throw new TakeException(TConstant.TYPE_ANDROID_Q_PERMISSION, "当前是Filtered View，不能直接访问App-specific外的文件，Environment.getExternalStorageDirectory()不能使用，请使用MediaStore或者使用getExternalFilesDirs、" +
                                "getExternalCacheDirs等，可查看" + " https://developer.android.google.cn/preview/privacy/scoped-storage");
                    }
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                File file = new File(uri.getPath());
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                return getUriFromFile(context, file);
            }
        }
        return uri;

    }

    /**
     * uri检查 判断裁剪输出的uri 在androidQ上是否正确
     *
     * @param context
     * @param uri
     * @param suffix  默认.jpg
     * @return
     */
    public static Uri checkCropUri(Context context, Uri uri, String suffix) throws TakeException {
        if (uri == null) return getTempSchemeFileUri(context, suffix);
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Environment.isExternalStorageLegacy()) {
                    Log.w(TConstant.TAG, "当前是Legacy View视图，兼容File方式访问");
                    File file = new File(uri.getPath());
                    if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                    return uri;
                } else {
                    if (checkAppSpecific(uri, context)) {
                        File file = new File(uri.getPath());
                        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                        return uri;
                    } else {
                        Log.w(TConstant.TAG, "当前存储空间视图模式是Filtered View，不能直接访问App-specific外的文件");
                        throw new TakeException(TConstant.TYPE_ANDROID_Q_PERMISSION, "当前是Filtered View，不能直接访问App-specific外的文件，Environment.getExternalStorageDirectory()不能使用，请使用MediaStore或者使用getExternalFilesDirs、" +
                                "getExternalCacheDirs等，可查看" + " https://developer.android.google.cn/preview/privacy/scoped-storage");
                    }
                }

            }
        }
        return uri;

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static boolean checkAppSpecific(Uri uri, Context context) {
        if (uri == null || uri.getPath() == null) return false;
        String path = uri.getPath();
        if (context.getExternalMediaDirs().length != 0 && path.startsWith(context.getExternalMediaDirs()[0].getAbsolutePath())) {
            return true;
        } else if (path.startsWith(context.getObbDir().getAbsolutePath())) {
            return true;
        } else if (path.startsWith(context.getExternalCacheDir().getAbsolutePath())) {
            return true;
        } else if (path.startsWith(context.getExternalFilesDir("").getAbsolutePath())) {
            return true;
        }
        return false;

    }

    /**
     * 创建一个用于拍照图片输出路径的Uri (FileProvider)
     *
     * @param context
     * @return
     */
    public static Uri getUriFromFile(Context context, File file) {
        return FileProvider.getUriForFile(context, TConstant.getFileProviderName(context), file);
    }

    /**
     * 创建Scheme为Content临时的uri
     *
     * @param context
     * @param suffix
     * @return
     */
    public static Uri getTempSchemeContentUri(@NonNull Context context, String suffix) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/" + timeStamp + (TextUtils.isEmpty(suffix) ? ".jpg" : suffix));
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        return getUriFromFile(context, file);

    }

    /**
     * 创建Scheme为file临时的uri
     *
     * @param context
     * @return
     */
    public static Uri getTempSchemeFileUri(@NonNull Context context) {
        return getTempSchemeFileUri(context, null);
    }

    /**
     * 创建Scheme为file临时的uri
     *
     * @param context
     * @return
     */
    public static Uri getTempSchemeFileUri(@NonNull Context context, String suffix) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/" + timeStamp + (TextUtils.isEmpty(suffix) ? ".jpg" : suffix));
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        return Uri.fromFile(file);
    }

}
