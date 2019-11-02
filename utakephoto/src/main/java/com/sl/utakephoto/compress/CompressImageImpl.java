package com.sl.utakephoto.compress;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.sl.utakephoto.utils.ImgUtil;
import com.sl.utakephoto.exception.TakeException;
import com.sl.utakephoto.utils.TUriUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompressImageImpl implements CompressImage, Handler.Callback {
    private final Handler mHandler;
    private final Context context;
    private List<Uri> images;
    private CompressImage.CompressListener mCompressListener;
    private static final int MSG_COMPRESS_SUCCESS = 0;
    private static final int MSG_COMPRESS_START = 1;
    private static final int MSG_COMPRESS_ERROR = 2;

    private int leastCompressSize;
    private Uri targetUri;
    private boolean focusAlpha;

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCompressListener == null) return false;

        switch (msg.what) {
            case MSG_COMPRESS_START:
                mCompressListener.onStart();
                break;
            case MSG_COMPRESS_SUCCESS:
                mCompressListener.onSuccess((Uri) msg.obj);
                break;
            case MSG_COMPRESS_ERROR:
                mCompressListener.onError((Throwable) msg.obj);
                break;
            default:
                break;
        }
        return false;
    }

    public static CompressImage of(Context context, CompressConfig config, List<Uri> images, CompressImage.CompressListener listener) {
        return new CompressImageImpl(context, config, images, listener);
    }

    private CompressImageImpl(Context context, CompressConfig config, List<Uri> images, CompressImage.CompressListener compressListener) {
        this.context = context;
        this.images = images;
        this.mCompressListener = compressListener;
        mHandler = new Handler(Looper.getMainLooper(), this);
        if (config != null) {
            leastCompressSize = config.getLeastCompressSize();
            targetUri = config.getTargetUri();
            focusAlpha = config.isFocusAlpha();
        }

    }

    @Override
    public void compress() {
        if (images == null || images.size() == 0 && mCompressListener != null) {
            mCompressListener.onError(new NullPointerException("image file cannot be null"));
        }

        Iterator<Uri> iterator = images.iterator();

        while (iterator.hasNext()) {
            final Uri path = iterator.next();

            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_START));
                        Uri result = compress(path);
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_SUCCESS, result));
                    } catch (IOException e) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e));
                    } catch (TakeException e) {
                        e.printStackTrace();
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COMPRESS_ERROR, e));
                    }
                }
            });

//            iterator.remove();
        }
    }

    private Uri compress(final Uri uri) throws IOException, TakeException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        if (needCompress(leastCompressSize, inputStream)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = ImgUtil.computeSize(inputStream);
            Bitmap tagBitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (ImgUtil.JPEG_MIME_TYPE(uri)) {
                tagBitmap = ImgUtil.rotatingImage(tagBitmap, ImgUtil.getMetadataRotation(context, uri));
            }
            tagBitmap.compress(focusAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 60, stream);
            tagBitmap.recycle();
            targetUri = TUriUtils.checkUri(context, targetUri, ImgUtil.extSuffix(uri));

            OutputStream outputStream = context.getContentResolver().openOutputStream(targetUri);
            if (outputStream != null) {
                outputStream.write(stream.toByteArray());
            }
            outputStream.close();
            stream.close();
            inputStream.close();
            return targetUri;
        }
        return uri;

    }

    private Uri getRealUri(Context context) {
        if (targetUri != null) {

            return targetUri;
        }

        return null;
    }

    private boolean needCompress(int leastCompressSize, InputStream path) {
        if (leastCompressSize > 0) {
            try {
                return path.available() > (leastCompressSize << 10);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
