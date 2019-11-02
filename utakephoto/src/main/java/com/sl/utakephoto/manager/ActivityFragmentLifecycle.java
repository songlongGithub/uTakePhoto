package com.sl.utakephoto.manager;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A {@link Lifecycle} implementation for tracking and notifying
 * listeners of {@link android.app.Fragment} and {@link android.app.Activity} lifecycle events.
 */
class ActivityFragmentLifecycle implements Lifecycle {
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
    private boolean isCreated;
    private boolean isDestroyed;

    /**
     * Adds the given listener to the list of listeners to be notified on each lifecycle event.
     */
    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.add(listener);
        if (isCreated) {
            listener.onCreate();
        }
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    void onCreate() {
        isCreated = true;
        for (LifecycleListener lifecycleListener : getSnapshot(lifecycleListeners)) {
            lifecycleListener.onCreate();
            isCreated = false;
        }
    }

    void onDestroy() {
        isDestroyed = true;
        for (LifecycleListener lifecycleListener : getSnapshot(lifecycleListeners)) {
            lifecycleListener.onDestroy();
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (LifecycleListener lifecycleListener : getSnapshot(lifecycleListeners)) {
            lifecycleListener.onActivityResult(requestCode, resultCode, data);
        }
    }

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (LifecycleListener lifecycleListener : getSnapshot(lifecycleListeners)) {
            lifecycleListener.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Returns a copy of the given list that is safe to iterate over and perform actions that may
     * modify the original list.
     *
     * <p>See #303, #375, #322, #2262.
     */
    @NonNull
    @SuppressWarnings("UseBulkOperation")
    private static <T> List<T> getSnapshot(@NonNull Collection<T> other) {
        // toArray creates a new ArrayList internally and does not guarantee that the values it contains
        // are non-null. Collections.addAll in ArrayList uses toArray internally and therefore also
        // doesn't guarantee that entries are non-null. WeakHashMap's iterator does avoid returning null
        // and is therefore safe to use. See #322, #2262.
        List<T> result = new ArrayList<>(other.size());
        for (T item : other) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

}
