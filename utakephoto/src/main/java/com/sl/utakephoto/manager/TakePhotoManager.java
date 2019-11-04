package com.sl.utakephoto.manager;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.sl.utakephoto.compress.CompressConfig;
import com.sl.utakephoto.compress.CompressImage;
import com.sl.utakephoto.utils.ImgUtil;
import com.sl.utakephoto.utils.IntentUtils;
import com.sl.utakephoto.utils.TConstant;
import com.sl.utakephoto.compress.CompressImageImpl;
import com.sl.utakephoto.crop.CropActivity;
import com.sl.utakephoto.crop.CropExtras;
import com.sl.utakephoto.crop.CropOptions;
import com.sl.utakephoto.exception.TakeException;
import com.sl.utakephoto.utils.PermissionUtils;
import com.sl.utakephoto.utils.TUriUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

/**
 * author : Sl
 * createDate   : 2019-10-1516:48
 * desc   :
 */
public class TakePhotoManager implements LifecycleListener {
    /**
     * 拍照ResultCode
     */
    private static final int TAKE_PHOTO_RESULT = 1 << 2;
    /**
     * 选择相册ResultCode
     */
    private static final int DIRECTORY_PICTURES_RESULT = 1 << 3;
    /**
     * 裁剪ResultCode
     */
    private static final int PHOTO_WITCH_CROP_RESULT = 1 << 5;
    /**
     * 类型拍照
     */
    private static final int TYPE_TAKE_PHOTO = 1 << 7;
    /**
     * 类型从相册选择
     */
    private static final int TYPE_SELECT_IMAGE = 1 << 8;
    /**
     * 类型
     */
    private int takeType;
    /**
     * 请求权限requestCode
     */
    private static final int PERMISSION_REQUEST_CODE = 1 << 9;

    private UTakePhoto uTakePhoto;
    private final Lifecycle lifecycle;
    private Context mContext;
    private Intent intent;
    private CropOptions cropOptions;
    /**
     * 保存图片的相对路径 androidQ默认插入MediaStore，对应 RELATIVE_PATH
     * 其他默认在Environment.getExternalStorageDirectory()路径下，relativePath为子路径
     */
    private String relativePath;
    private CompressConfig compressConfig;
    private ITakePhotoResult takePhotoResult;

    private Uri outPutUri;
    private Uri tempUri;
    private boolean isInit;
    public static final SparseArray<String> ERROR_ARRAY = new SparseArray<>();
    private static final String[] PERMISSION_CAMERAS = new String[]{Manifest.permission.CAMERA, "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final String[] PERMISSION_STORAGE = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    static {

        ERROR_ARRAY.put(TConstant.TYPE_NOT_IMAGE, "不是图片类型");
        ERROR_ARRAY.put(TConstant.TYPE_WRITE_FAIL, "保存失败");
        ERROR_ARRAY.put(TConstant.TYPE_URI_NULL, "Uri为null");
        ERROR_ARRAY.put(TConstant.TYPE_URI_PARSE_FAIL, "Uri解析错误");
        ERROR_ARRAY.put(TConstant.TYPE_NO_MATCH_PICK_INTENT, "没有找到选择照片的Intent");
        ERROR_ARRAY.put(TConstant.TYPE_NO_MATCH_CROP_INTENT, "没有找到裁剪照片的Intent");
        ERROR_ARRAY.put(TConstant.TYPE_NO_CAMERA, "没有找到拍照的Intent");
        ERROR_ARRAY.put(TConstant.TYPE_NO_FIND, "选择的文件没有找到");
    }


    TakePhotoManager(
            @NonNull UTakePhoto uTakePhoto,
            @NonNull Lifecycle lifecycle,
            @NonNull Context context) {
        this.uTakePhoto = uTakePhoto;
        this.lifecycle = lifecycle;
        this.mContext = context;
        lifecycle.addListener(this);
        this.uTakePhoto.registerRequestManager(this);
    }

    /**
     * 默认储存在getExternalFilesDir(Pictures)目录下
     *
     * @return
     */
    public TakePhotoManager openCamera() {
        return openCamera(null, null, null);
    }

    /**
     * 打开系统相机，输出路径自定义
     *
     * @param outPutUri 拍照路径
     * @return
     */
    public TakePhotoManager openCamera(Uri outPutUri) {
        return openCamera(outPutUri, null, null);
    }

    /**
     * 打开系统相机，输出路径自定义
     * 在androidQ上建议采用这个方法，因为如果采用传入mediaStore的Uri的方式，会在m相册里创建一个空的img
     *
     * @param relativePath androidQ上清单文件中android:requestLegacyExternalStorage="true"
     *                     则relativePath 必须以 Pictures/DCIM 为跟路径；
     *                     Q以下默认根路径是Environment.getExternalStorageDirectory()
     * @return
     */
    public TakePhotoManager openCamera(String relativePath) {
        return openCamera(null, relativePath, null);
    }

    private TakePhotoManager openCamera(Uri outPutUri, Intent intent) {
        return openCamera(outPutUri, null, intent);
    }

    private TakePhotoManager openCamera(String relativePath, Intent intent) {
        return openCamera(null, relativePath, intent);
    }

    /**
     * @param outPutUri    输入路径
     * @param relativePath androidQ设置MediaStore.Images.Media.RELATIVE_PATH
     * @param intent       自定义Intent的时候，outPutUri为输出路径，成功需要返回setResult(RESULT_OK)
     * @return
     */
    private TakePhotoManager openCamera(Uri outPutUri, String relativePath, Intent intent) {
        takeType = TYPE_TAKE_PHOTO;
        this.outPutUri = outPutUri;
        this.intent = intent;
        this.relativePath = relativePath;
        return this;
    }

    /**
     * 打开相册
     *
     * @return
     */
    public TakePhotoManager openAlbum() {
        return openAlbum(null);
    }

    /**
     * 打开指定相册
     *
     * @param intent 通过Intent跳转的时候，需要返回setResult(RESULT_OK,Intent.setData(Uri)))
     * @return this
     */
    public TakePhotoManager openAlbum(Intent intent) {
        takeType = TYPE_SELECT_IMAGE;
        this.intent = intent;
        return this;
    }

    public TakePhotoManager setCrop(CropOptions cropOptions) {
        this.cropOptions = cropOptions;
        return this;
    }

    public TakePhotoManager setCompressConfig(CompressConfig compressConfig) {
        this.compressConfig = compressConfig;
        return this;
    }


    public void build(ITakePhotoResult takePhotoResult) {
        this.takePhotoResult = takePhotoResult;
        if (!isInit) {
            return;
        }
        checkPermission();
    }


    private void checkPermission() {
        if (takePhotoResult == null) {
            return;
        }
        if (takeType == 0) {
            takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_OTHER, "You have to make sure you call openCamera or openAlbum"));
            return;
        }
        if (uTakePhoto.getSupportFragment() != null) {
            supportFragmentPermissionCheck(uTakePhoto.getSupportFragment());
        } else if (uTakePhoto.getFragment() != null) {
            fragmentPermissionCheck(uTakePhoto.getFragment());
        }

    }

    private void supportFragmentPermissionCheck(Fragment fragment) {
        if (PermissionUtils.hasSelfPermissions(fragment.getContext(), takeType == TYPE_TAKE_PHOTO ? PERMISSION_CAMERAS : PERMISSION_STORAGE)) {
            permissionGranted();
        } else {
            fragment.requestPermissions(takeType == TYPE_TAKE_PHOTO ? PERMISSION_CAMERAS : PERMISSION_STORAGE, PERMISSION_REQUEST_CODE);
        }
    }

    private void fragmentPermissionCheck(android.app.Fragment fragment) {
        if (PermissionUtils.hasSelfPermissions(fragment.getActivity(), takeType == TYPE_TAKE_PHOTO ? PERMISSION_CAMERAS : PERMISSION_STORAGE)) {
            permissionGranted();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fragment.requestPermissions(takeType == TYPE_TAKE_PHOTO ? PERMISSION_CAMERAS : PERMISSION_STORAGE, PERMISSION_REQUEST_CODE);
            } else {
                if (takePhotoResult != null) {
                    //"请检查权限是否在manifest里注册：" +
                    takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_NO_PERMISSION, (takeType == TYPE_TAKE_PHOTO ? Arrays.toString(PERMISSION_CAMERAS) : Arrays.toString(PERMISSION_STORAGE))));
                }
            }
        }
    }


    @Override
    public void onCreate() {
        isInit = true;
        checkPermission();
    }

    @Override
    public void onDestroy() {
        lifecycle.removeListener(this);
        uTakePhoto.unregisterRequestManager(this);
        takePhotoResult = null;
        isInit = false;
        uTakePhoto.onDestroy();
        ERROR_ARRAY.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PHOTO_RESULT) {
            if (resultCode == RESULT_OK) {
                if (cropOptions != null) {
                    crop(outPutUri);
                } else if (compressConfig != null) {
                    compress(outPutUri);
                } else {
                    if (relativePath == null || relativePath.length() == 0) {
                        if (takePhotoResult != null) {
                            takePhotoResult.takeSuccess(Collections.singletonList(outPutUri));
                        }
                    }
                }
                //拍完照 如果设置的是相对路径，需要把图片储存在这个路径下
                if (relativePath != null && relativePath.length() != 0) {
                    SaveSourceImgTask saveSourceImgTask = new SaveSourceImgTask();
                    saveSourceImgTask.execute(outPutUri);
                }


            } else {
                takeCancel();
            }
        } else if (requestCode == DIRECTORY_PICTURES_RESULT) {
            if (resultCode == RESULT_OK) {
                if (cropOptions != null) {
                    crop(data.getData());
                } else {
                    compress(data.getData());
                }
            } else {
                takeCancel();
            }
        } else if (requestCode == PHOTO_WITCH_CROP_RESULT) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    compress(tempUri);
                }
            } else {
                takeCancel();
            }
        }

    }

    /**
     * 保存原图
     */
    private class SaveSourceImgTask extends AsyncTask<Uri, Void, Uri> {

        @Override
        protected Uri doInBackground(Uri... params) {
            return preserveOriginalImag(params[0]);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (cropOptions == null) {
                compress(uri);
            }
        }
    }

    /**
     * Preserve the original image
     *
     * @param outPutUri
     * @return
     */
    private Uri preserveOriginalImag(Uri outPutUri) {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        FileOutputStream fos = null;

        try {
            inputStream = mContext.getContentResolver().openInputStream(outPutUri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.MIME_TYPE, ImgUtil.getMimeType(outPutUri));
                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                values.put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp + ImgUtil.extSuffix(outPutUri));
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
                String status = Environment.getExternalStorageState();
                ContentResolver contentResolver = mContext.getContentResolver();
                Uri insert;
                // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
                if (Environment.MEDIA_MOUNTED.equals(status)) {
                    insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                } else {
                    insert = contentResolver.insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);
                }
                if (insert != null) {
                    outputStream = contentResolver.openOutputStream(insert);
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inSampleSize = ImgUtil.computeSize(inputStream);
                    Bitmap tagBitmap = BitmapFactory.decodeStream(
                            mContext.getContentResolver().openInputStream(outPutUri));
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    if (ImgUtil.JPEG_MIME_TYPE(outPutUri)) {
                        tagBitmap = ImgUtil.rotatingImage(tagBitmap, ImgUtil.getMetadataRotation(mContext, outPutUri));
                    }
                    tagBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    tagBitmap.recycle();
                    if (outputStream != null) {
                        outputStream.write(stream.toByteArray());
                    }
                    Log.d(TConstant.TAG, "原图路径 :" + insert);

                }

                return insert;

            } else {
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inSampleSize = ImgUtil.computeSize(inputStream);
                Bitmap tagBitmap = BitmapFactory.decodeStream(
                        mContext.getContentResolver().openInputStream(outPutUri));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (ImgUtil.JPEG_MIME_TYPE(outPutUri)) {
                    tagBitmap = ImgUtil.rotatingImage(tagBitmap, ImgUtil.getMetadataRotation(mContext, outPutUri));
                }
                tagBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                tagBitmap.recycle();

                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                File outputFile = new File(Environment.getExternalStorageDirectory(),
                        relativePath + "/" + timeStamp + ImgUtil.extSuffix(outPutUri));
                if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
                Log.d(TConstant.TAG, "原图路径 :" + outputFile.getPath());
                fos = new FileOutputStream(outputFile);
                fos.write(stream.toByteArray());
                Uri uri = Uri.fromFile(outputFile);
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                return uri;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(fos);
            close(outputStream);
            close(inputStream);
        }
        return outPutUri;
    }

    private void close(Closeable fos) {
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void compress(final Uri outPutUri) {
        if (compressConfig == null) {
            if (takePhotoResult != null) {
                takePhotoResult.takeSuccess(Collections.singletonList(outPutUri));
            }
        } else {
            CompressImageImpl.of(mContext, compressConfig, Collections.singletonList(outPutUri), new CompressImage.CompressListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess(Uri images) {
                    if (takePhotoResult != null) {
                        Log.d(TConstant.TAG, "压缩成功 uri：" + images);
                        takePhotoResult.takeSuccess(Collections.singletonList(images));
                    }
                }

                @Override
                public void onError(Throwable obj) {
                    obj.printStackTrace();
                    Log.d(TConstant.TAG, "压缩失败");
                    if (takePhotoResult != null) {
                        if (obj instanceof TakeException) {
                            takePhotoResult.takeFailure((TakeException) obj);
                        } else {
                            takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_OTHER, obj.getMessage()));
                        }
                    }
                }
            }).compress();
        }


    }

    private void takeCancel() {
        if (takePhotoResult != null) {
            takePhotoResult.takeCancel();
        }
        Log.d(TConstant.TAG, "操作取消");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            ArrayList<String> deniedList = new ArrayList<>();
            ArrayList<String> neverAskAgainList = new ArrayList<>();
            for (int i = 0, j = permissions.length; i < j; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {

                    if (uTakePhoto.getSupportFragment() != null) {
                        if (!PermissionUtils.shouldShowRequestPermissionRationale(uTakePhoto.getSupportFragment(), permissions[i])) {
                            neverAskAgainList.add(permissions[i]);
                        } else {
                            deniedList.add(permissions[i]);
                        }
                    } else if (uTakePhoto.getFragment() != null) {
                        if (!PermissionUtils.shouldShowRequestPermissionRationale(uTakePhoto.getFragment(), permissions[i])) {
                            neverAskAgainList.add(permissions[i]);
                        } else {
                            deniedList.add(permissions[i]);
                        }
                    }
                }
            }
            if (deniedList.isEmpty() && neverAskAgainList.isEmpty()) {
                permissionGranted();
            } else {
                if (!deniedList.isEmpty()) {
                    permissionDenied(deniedList);
                }
                if (!neverAskAgainList.isEmpty()) {
                    permissionNeverAskAgain(neverAskAgainList);

                }
            }
        }
    }


    private void permissionGranted() {
        if (takeType == TYPE_TAKE_PHOTO) {
            try {
                //TODO 在androidQ上 如果outPutUri是MediaStore创建的Uri，图片未保存的时候成功的时候，会留下一个空的img
                this.outPutUri = TUriUtils.checkTakePhotoUri(mContext, outPutUri, ImgUtil.extSuffix(outPutUri));
                this.intent = intent == null ? IntentUtils.getCaptureIntent(this.outPutUri) : intent;
            } catch (TakeException e) {
                e.printStackTrace();
                if (takePhotoResult != null) {
                    takePhotoResult.takeFailure(e);
                }
                return;
            }
        } else {
            this.intent = intent == null ? IntentUtils.getAlbumIntent() : intent;
        }

        if (IntentUtils.intentAvailable(mContext, intent)) {
            try {
                startActivityForResult(intent, takeType == TYPE_TAKE_PHOTO ? TAKE_PHOTO_RESULT : DIRECTORY_PICTURES_RESULT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (takePhotoResult != null) {
                takePhotoResult.takeFailure(new TakeException(takeType == TYPE_TAKE_PHOTO ? TConstant.TYPE_NO_CAMERA : TConstant.TYPE_NO_MATCH_PICK_INTENT));
            }
        }
    }

    private void permissionDenied(ArrayList<String> permissions) {
        if (takePhotoResult != null) {
            //拒绝的权限
            takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_DENIED_PERMISSION, permissions.toString()));
        }
    }

    private void permissionNeverAskAgain(ArrayList<String> permissions) {
        if (takePhotoResult != null) {
            //"以下权限不再询问，请去设置里开启："
            takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_NEVER_ASK_PERMISSION, permissions.toString()));
        }
    }


    private void startActivityForResult(Intent intent, int requestCode) {
        if (uTakePhoto.getSupportFragment() != null) {
            (uTakePhoto.getSupportFragment()).startActivityForResult(intent, requestCode);
        } else if (uTakePhoto.getFragment() != null) {
            (uTakePhoto.getFragment()).startActivityForResult(intent, requestCode);
        }
    }

    private void startCropActivityForResult(Intent intent, int requestCode) {
        if (uTakePhoto.getSupportFragment() != null) {
            intent.setClass(uTakePhoto.getSupportFragment().getContext(), CropActivity.class);
            uTakePhoto.getSupportFragment().startActivityForResult(intent, requestCode);
        } else if (uTakePhoto.getFragment() != null) {
            intent.setClass(uTakePhoto.getFragment().getActivity(), CropActivity.class);
            uTakePhoto.getFragment().startActivityForResult(intent, requestCode);
        }
    }


    private void crop(Uri takePhotoUri) {
        tempUri = TUriUtils.getTempSchemeFileUri(mContext);
        Log.d(TConstant.TAG, "tempUri :" + tempUri);
        if (cropOptions.isUseOwnCrop()) {
            Intent cropIntent = new Intent();
            cropIntent.setData(takePhotoUri);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
            if (cropOptions.getAspectX() * cropOptions.getAspectY() > 0) {
                cropIntent.putExtra(CropExtras.KEY_ASPECT_X, cropOptions.getAspectX());
                cropIntent.putExtra(CropExtras.KEY_ASPECT_Y, cropOptions.getAspectY());
            } else if (cropOptions.getOutputX() * cropOptions.getOutputY() > 0) {
                cropIntent.putExtra(CropExtras.KEY_OUTPUT_X, cropOptions.getOutputX());
                cropIntent.putExtra(CropExtras.KEY_OUTPUT_Y, cropOptions.getOutputY());

            } else {
                cropIntent.putExtra(CropExtras.KEY_ASPECT_X, 1);
                cropIntent.putExtra(CropExtras.KEY_ASPECT_Y, 1);
            }
            if (IntentUtils.intentAvailable(mContext, cropIntent)) {
                startCropActivityForResult(cropIntent, PHOTO_WITCH_CROP_RESULT);
            } else {
                if (takePhotoResult != null) {
                    takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_NO_MATCH_CROP_INTENT));
                }
            }
        } else {
            Intent cropIntentWithOtherApp = IntentUtils.getCropIntent(takePhotoUri, tempUri, cropOptions);

            if (IntentUtils.intentAvailable(mContext, cropIntentWithOtherApp)) {
                startActivityForResult(cropIntentWithOtherApp, PHOTO_WITCH_CROP_RESULT);
            } else {
                if (takePhotoResult != null) {
                    takePhotoResult.takeFailure(new TakeException(TConstant.TYPE_NO_MATCH_CROP_INTENT));
                }
            }
        }


    }
}
