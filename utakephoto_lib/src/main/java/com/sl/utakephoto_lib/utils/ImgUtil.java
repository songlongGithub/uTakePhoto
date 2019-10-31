package com.sl.utakephoto_lib.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;

import static android.media.ExifInterface.ORIENTATION_NORMAL;
import static android.media.ExifInterface.ORIENTATION_ROTATE_180;
import static android.media.ExifInterface.ORIENTATION_ROTATE_270;
import static android.media.ExifInterface.ORIENTATION_ROTATE_90;
import static android.media.ExifInterface.ORIENTATION_UNDEFINED;

/**
 * author : Sl
 * createDate   : 2019-10-3009:46
 * desc   :
 */
public class ImgUtil {
    private static final String TAG = "ImgUtil";


    /**
     * Returns the rotation of image at the given URI as one of 0, 90, 180,
     * 270.  Defaults to 0.
     */
    public static int getMetadataRotation(Context context, Uri uri) {
        int orientation = getMetadataOrientation(context, uri);
        switch (orientation) {
            case ORIENTATION_ROTATE_90:
                return 90;
            case ORIENTATION_ROTATE_180:
                return 180;
            case ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static boolean JPEG_MIME_TYPE(Uri uri) {
        if (!JPEG_MIME_TYPE.equals(getMimeType(uri))) {
            return false;
        }
        return true;
    }

    private static final String JPEG_MIME_TYPE = "image/jpeg";

    private static int getMetadataOrientation(Context context, Uri uri) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }

        // First try to find orientation data in Gallery's ContentProvider.
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
                    null, null, null);
            if (cursor != null && cursor.moveToNext() && cursor.getColumnCount() > 0) {
                int ori = cursor.getInt(0);
                switch (ori) {
                    case 90:
                        return ORIENTATION_ROTATE_90;
                    case 270:
                        return ORIENTATION_ROTATE_270;
                    case 180:
                        return ORIENTATION_ROTATE_180;
                    default:
                        return ORIENTATION_NORMAL;
                }
            }
        } catch (SQLiteException e) {
            // Do nothing
        } catch (IllegalArgumentException e) {
            // Do nothing
        } catch (IllegalStateException e) {
            // Do nothing
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        ExifInterface exif;
        InputStream is = null;
        // Fall back to checking EXIF tags in file or input stream.
        try {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                if (!JPEG_MIME_TYPE(uri)) {
                    return ORIENTATION_NORMAL;
                }
                String path = uri.getPath();
                exif = new ExifInterface(path);
                switch (parseExif(exif)) {
                    case 90:
                        return ORIENTATION_ROTATE_90;
                    case 270:
                        return ORIENTATION_ROTATE_270;
                    case 180:
                        return ORIENTATION_ROTATE_180;
                    default:
                        return ORIENTATION_NORMAL;
                }
            } else {
                is = context.getContentResolver().openInputStream(uri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    exif = new ExifInterface(is);
                } else {
                    exif = new ExifInterface(uri.getPath());
                }
                switch (parseExif(exif)) {
                    case 90:
                        return ORIENTATION_ROTATE_90;
                    case 270:
                        return ORIENTATION_ROTATE_270;
                    case 180:
                        return ORIENTATION_ROTATE_180;
                    default:
                        return ORIENTATION_NORMAL;
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to read EXIF orientation", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to close InputStream", e);
            }
        }
        return ORIENTATION_NORMAL;
    }


    private static int parseExif(ExifInterface exif) {
        switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ORIENTATION_UNDEFINED)) {
            case ORIENTATION_ROTATE_90:
                return 90;
            case ORIENTATION_ROTATE_180:
                return 180;
            case ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }

    }

    public static String getMimeType(Uri src) {
        String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
        String ret = null;
        if (postfix != null) {
            ret = MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
        }
        return ret;
    }

    public static String extSuffix(Uri uri) {
        try {

            return getMimeType(uri).replace("image/", ".");
        } catch (Exception e) {
            return ".jpg";
        }
    }

    public static int computeSize(InputStream inputStream) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;

        BitmapFactory.decodeStream(inputStream, null, options);
        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;
        srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
        srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        float scale = ((float) shortSide / longSide);
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280 == 0 ? 1 : longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale));
        }
    }

    public static Bitmap rotatingImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
