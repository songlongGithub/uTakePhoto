package com.sl.utakephoto.manager;

import android.content.Intent;

/**
 * An interface for listener to {@link android.app.Fragment} and {@link android.app.Activity}
 * lifecycle events.
 */
public interface LifecycleListener {

    /**
     * Callback for when {@link android.app.Fragment#onStart()}} or {@link
     * android.app.Activity#onStart()} is called.
     */
    void onCreate();
    /**
     * Callback for when {@link android.app.Fragment#onDestroy()}} or {@link
     * android.app.Activity#onDestroy()} is called.
     */
    void onDestroy();


    void onActivityResult(int requestCode, int resultCode, Intent data);

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

}
