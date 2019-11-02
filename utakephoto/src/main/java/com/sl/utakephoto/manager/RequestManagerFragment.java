package com.sl.utakephoto.manager;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * @author sl
 * @date 2019/4/11 6:43 PM
 */
public final class RequestManagerFragment extends Fragment {

    private UTakePhoto mUTakePhoto;
    private TakePhotoManager takePhotoManager;
    private ActivityFragmentLifecycle lifecycle;


    public RequestManagerFragment() {
        this(new ActivityFragmentLifecycle());
    }


    @VisibleForTesting
    @SuppressLint("ValidFragment")
    private RequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public UTakePhoto get() {
        if (mUTakePhoto == null) {
            mUTakePhoto = new UTakePhoto(this);
        }
        return mUTakePhoto;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("RequestManagerFragment", " onCreate savedInstanceState" + savedInstanceState + " lifecycle == null" + lifecycle);
        lifecycle.onCreate();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        lifecycle.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        lifecycle.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    TakePhotoManager getTakePhotoManager() {
        return takePhotoManager;
    }

    void setTakePhotoManager(TakePhotoManager takePhotoManager) {
        this.takePhotoManager = takePhotoManager;
    }

    @NonNull
    ActivityFragmentLifecycle getTakePhotoLifecycle() {
        return lifecycle;
    }


}
