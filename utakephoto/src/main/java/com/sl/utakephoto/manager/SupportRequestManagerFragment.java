package com.sl.utakephoto.manager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;


/**
 * @author sl
 *
 */
public final class SupportRequestManagerFragment extends Fragment {

    private static String TAG = "SupportRequestManagerFragment";
    private ActivityFragmentLifecycle lifecycle;



    public SupportRequestManagerFragment() {
        this(new ActivityFragmentLifecycle());
    }

    @VisibleForTesting
    @SuppressLint("ValidFragment")
    private SupportRequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }


    private TakePhotoManager takePhotoManager;
    private UTakePhoto mUTakePhoto;

    public UTakePhoto get() {
        if (mUTakePhoto == null) {
            mUTakePhoto = new UTakePhoto(this);
        }
        return mUTakePhoto;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycle.onCreate();
        Log.d(TAG, "onCreate " + this);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();
        Log.d(TAG, "onDestroy " + this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        lifecycle.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        lifecycle.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    public TakePhotoManager getTakePhotoManager() {
        return takePhotoManager;
    }

    public void setTakePhotoManager(TakePhotoManager takePhotoManager) {
        this.takePhotoManager = takePhotoManager;
    }

    @NonNull
    public ActivityFragmentLifecycle getTakePhotoLifecycle() {
        return lifecycle;
    }


}
