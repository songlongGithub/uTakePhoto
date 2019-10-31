package com.sl.utakephoto_lib.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;


import java.util.HashMap;
import java.util.Map;

/**
 * @author sl
 * @date 2019/4/12 4:21 PM
 * creating new {@link TakePhotoManager}s or
 * * retrieving existing ones from activities and fragment.
 */
class RequestManagerRetriever implements Handler.Callback {

    private final RequestManagerFactory factory;
    private String mTag = UTakePhoto.class.getName();
    @VisibleForTesting
    static final String FRAGMENT_TAG = "com.sl.utakephoto_lib.manager";

    private Handler mHandler;

    private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

    private static class Holder {
        private static final RequestManagerRetriever INSTANCE = new RequestManagerRetriever();
    }

    static RequestManagerRetriever getInstance() {
        return Holder.INSTANCE;
    }

    private RequestManagerRetriever() {
        this.factory = DEFAULT_FACTORY;
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    private final Map<android.app.FragmentManager, RequestManagerFragment> mPendingFragments = new HashMap<>();
    private final Map<FragmentManager, SupportRequestManagerFragment> mPendingSupportFragments = new HashMap<>();


    @NonNull
    public TakePhotoManager get(@NonNull FragmentActivity activity) {
        assertNotDestroyed(activity);
        FragmentManager fm = activity.getSupportFragmentManager();
        return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }

    @NonNull
    public TakePhotoManager get(@NonNull Fragment fragment) {
        Preconditions.checkNotNull(
                fragment.getContext(),
                "You cannot start a load on a fragment before it is attached or after it is destroyed");
        FragmentManager fm = fragment.getChildFragmentManager();
        return supportFragmentGet(fragment.getContext(), fm, fragment, fragment.isVisible());
    }

    @SuppressWarnings("deprecation")
    @NonNull
    public TakePhotoManager get(@NonNull Activity activity) {
        assertNotDestroyed(activity);
        android.app.FragmentManager fm = activity.getFragmentManager();
        return fragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }

    @SuppressWarnings("deprecation")
    public TakePhotoManager get(android.app.Fragment fragment) {
        checkNotNull(fragment, "fragment is null");
        checkNotNull(fragment.getActivity(), "fragment.getActivity() is null");

        android.app.FragmentManager fm = fragment.getChildFragmentManager();

        return fragmentGet(fragment.getActivity(), fm, /*parentHint=*/ fragment, fragment.isVisible());
    }


    private static boolean isActivityVisible(Context context) {
        // This is a poor heuristic, but it's about all we have. We'd rather err on the side of visible
        // and start requests than on the side of invisible and ignore valid requests.
        Activity activity = findActivity(context);
        return activity == null || !activity.isFinishing();
    }

    @Nullable
    private static Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }


    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    @Deprecated
    @NonNull
    private TakePhotoManager fragmentGet(
            @NonNull Context context,
            @NonNull android.app.FragmentManager fm,
            @Nullable android.app.Fragment parentHint,
            boolean isParentVisible) {
        RequestManagerFragment current = getRequestManagerFragment(fm, parentHint, isParentVisible);
        TakePhotoManager requestManager = current.getTakePhotoManager();
        if (requestManager == null) {
            requestManager = factory.build(current.get(), current.getTakePhotoLifecycle(),
                    context);
            current.setTakePhotoManager(requestManager);
        }
        return requestManager;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private RequestManagerFragment getRequestManagerFragment(
            @NonNull final android.app.FragmentManager fm,
            @Nullable android.app.Fragment parentHint,
            boolean isParentVisible) {
        RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (current == null) {
            current = mPendingFragments.get(fm);
            if (current == null) {
                current = new RequestManagerFragment();
//                if (isParentVisible) {
//                    current.getTakePhotoLifecycle().onCreate();
//                }
                mPendingFragments.put(fm, current);
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
                mHandler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        } else {

        }
        return current;
    }


    @NonNull
    private TakePhotoManager supportFragmentGet(
            @NonNull Context context,
            @NonNull FragmentManager fm,
            @Nullable Fragment parentHint,
            boolean isParentVisible) {
        SupportRequestManagerFragment supportFragment =
                getSupportRequestManagerFragment(fm, parentHint, isParentVisible);
        TakePhotoManager takePhotoManager = supportFragment.getTakePhotoManager();
        if (takePhotoManager == null) {
            takePhotoManager = factory.build(supportFragment.get(), supportFragment.getTakePhotoLifecycle(), context);
            supportFragment.setTakePhotoManager(takePhotoManager);
        }
        return takePhotoManager;
    }

    @NonNull
    private SupportRequestManagerFragment getSupportRequestManagerFragment(
            @NonNull final FragmentManager fm, @Nullable Fragment parentHint, boolean isParentVisible) {

        SupportRequestManagerFragment fragment = (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = mPendingSupportFragments.get(fm);
            if (fragment == null) {
                fragment = new SupportRequestManagerFragment();
//                if (isParentVisible) {
//                    fragment.getTakePhotoLifecycle().onCreate();
//                }
                mPendingSupportFragments.put(fm, fragment);
                fm.beginTransaction().add(fragment, FRAGMENT_TAG).commitAllowingStateLoss();
                mHandler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        } else {
//            if (isParentVisible) {
//                fragment.getTakePhotoLifecycle().onCreate();
//            }
        }
        return fragment;


    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static void assertNotDestroyed(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean handled = true;
        switch (msg.what) {
            case ID_REMOVE_FRAGMENT_MANAGER:
                android.app.FragmentManager fm = (android.app.FragmentManager) msg.obj;
                mPendingFragments.remove(fm);
                break;
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
                FragmentManager supportFm = (FragmentManager) msg.obj;
                mPendingSupportFragments.remove(supportFm);
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }


    private static <T> void checkNotNull(@Nullable T arg, @NonNull String message) {
        if (arg == null) {
            throw new NullPointerException(message);
        }
    }

    public interface RequestManagerFactory {
        @NonNull
        TakePhotoManager build(
                @NonNull UTakePhoto UTakePhoto,
                @NonNull Lifecycle lifecycle,
                @NonNull Context context);

    }

    private static final RequestManagerFactory DEFAULT_FACTORY =
            new RequestManagerFactory() {
                @NonNull
                @Override
                public TakePhotoManager build(
                        @NonNull UTakePhoto UTakePhoto,
                        @NonNull Lifecycle lifecycle,
                        @NonNull Context context) {
                    return new TakePhotoManager(UTakePhoto, lifecycle, context);
                }


            };
}
