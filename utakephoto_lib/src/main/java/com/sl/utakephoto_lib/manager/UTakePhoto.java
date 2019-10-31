package com.sl.utakephoto_lib.manager;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * author : Sl
 * createDate   : 2019-10-1521:16
 * desc   :
 */
public class UTakePhoto {


    private final List<TakePhotoManager> managers = new ArrayList<>();
    private android.app.Fragment mFragment;
    private Fragment mSupportFragment;

    /**
     * 在FragmentActivity使用
     *
     * @param activity the activity
     */
    public static TakePhotoManager with(@NonNull FragmentActivity activity) {
        return getRetriever().get(activity);
    }

    public static TakePhotoManager with(@NonNull Activity activity) {
        return getRetriever().get(activity);
    }

    /**
     * 在Fragment使用
     *
     * @param fragment the fragment
     */
    public static TakePhotoManager with(@NonNull Fragment fragment) {
        return getRetriever().get(fragment);
    }

    /**
     * 在Fragment使用
     *
     * @param fragment the fragment
     */
    @SuppressWarnings("deprecation")
    public static TakePhotoManager with(@NonNull android.app.Fragment fragment) {
        return getRetriever().get(fragment);
    }


    /**
     * 在SupportFragment里初始化
     * Instantiates a new Immersion bar.
     *
     * @param fragment the fragment
     */
    UTakePhoto(Fragment fragment) {
        mSupportFragment = fragment;
    }

    /**
     * 在Fragment里初始化
     * Instantiates a new Immersion bar.
     *
     * @param fragment the fragment
     */
    UTakePhoto(android.app.Fragment fragment) {
        mFragment = fragment;
    }

    public void onDestroy() {
        mFragment = null;
        mSupportFragment = null;
    }

    public android.app.Fragment getFragment() {
        return mFragment;
    }

    public Fragment getSupportFragment() {
        return mSupportFragment;
    }

    private static RequestManagerRetriever getRetriever() {
        return RequestManagerRetriever.getInstance();
    }


    void registerRequestManager(TakePhotoManager requestManager) {
        synchronized (managers) {
            if (managers.contains(requestManager)) {
                throw new IllegalStateException("Cannot register already registered manager");
            }
            managers.add(requestManager);
        }
    }

    void unregisterRequestManager(TakePhotoManager requestManager) {
        synchronized (managers) {
            if (!managers.contains(requestManager)) {
                throw new IllegalStateException("Cannot unregister not yet registered manager");
            }
            managers.remove(requestManager);
        }
    }
}
