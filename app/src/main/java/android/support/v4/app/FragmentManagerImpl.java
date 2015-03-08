package android.support.v4.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import com.valvesoftware.android.steam.community.SettingInfoDB;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

final class FragmentManagerImpl extends FragmentManager {
    static final Interpolator ACCELERATE_CUBIC;
    static final Interpolator ACCELERATE_QUINT;
    static final int ANIM_DUR = 220;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;
    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    static boolean DEBUG = false;
    static final Interpolator DECELERATE_CUBIC;
    static final Interpolator DECELERATE_QUINT;
    static final boolean HONEYCOMB;
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    ArrayList<Fragment> mActive;
    FragmentActivity mActivity;
    ArrayList<Fragment> mAdded;
    ArrayList<Integer> mAvailBackStackIndices;
    ArrayList<Integer> mAvailIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    ArrayList<BackStackRecord> mBackStackIndices;
    ArrayList<Fragment> mCreatedMenus;
    int mCurState;
    boolean mDestroyed;
    Runnable mExecCommit;
    boolean mExecutingActions;
    boolean mHavePendingDeferredStart;
    boolean mNeedMenuInvalidate;
    String mNoTransactionsBecause;
    ArrayList<Runnable> mPendingActions;
    SparseArray<Parcelable> mStateArray;
    Bundle mStateBundle;
    boolean mStateSaved;
    Runnable[] mTmpActions;

    class AnonymousClass_3 implements Runnable {
        final /* synthetic */ int val$flags;
        final /* synthetic */ String val$name;

        AnonymousClass_3(String str, int i) {
            this.val$name = str;
            this.val$flags = i;
        }

        public void run() {
            FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mActivity.mHandler, this.val$name, -1, this.val$flags);
        }
    }

    class AnonymousClass_4 implements Runnable {
        final /* synthetic */ int val$flags;
        final /* synthetic */ int val$id;

        AnonymousClass_4(int i, int i2) {
            this.val$id = i;
            this.val$flags = i2;
        }

        public void run() {
            FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mActivity.mHandler, null, this.val$id, this.val$flags);
        }
    }

    class AnonymousClass_5 implements AnimationListener {
        final /* synthetic */ Fragment val$fragment;

        AnonymousClass_5(Fragment fragment) {
            this.val$fragment = fragment;
        }

        public void onAnimationEnd(Animation animation) {
            if (this.val$fragment.mAnimatingAway != null) {
                this.val$fragment.mAnimatingAway = null;
                FragmentManagerImpl.this.moveToState(this.val$fragment, this.val$fragment.mStateAfterAnimating, 0, 0);
            }
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationStart(Animation animation) {
        }
    }

    FragmentManagerImpl() {
        this.mCurState = 0;
        this.mStateBundle = null;
        this.mStateArray = null;
        this.mExecCommit = new Runnable() {
            public void run() {
                FragmentManagerImpl.this.execPendingActions();
            }
        };
    }

    static {
        boolean z = HONEYCOMB;
        DEBUG = false;
        if (VERSION.SDK_INT >= 11) {
            z = true;
        }
        HONEYCOMB = z;
        DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
        DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
        ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
        ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);
    }

    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    public boolean executePendingTransactions() {
        return execPendingActions();
    }

    public void popBackStack() {
        enqueueAction(new Runnable() {
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mActivity.mHandler, null, -1, 0);
            }
        }, HONEYCOMB);
    }

    public boolean popBackStackImmediate() {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mActivity.mHandler, null, -1, 0);
    }

    public void popBackStack(String name, int flags) {
        enqueueAction(new AnonymousClass_3(name, flags), HONEYCOMB);
    }

    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mActivity.mHandler, name, -1, flags);
    }

    public void popBackStack(int id, int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new AnonymousClass_4(id, flags), HONEYCOMB);
    }

    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        executePendingTransactions();
        if (id >= 0) {
            return popBackStackState(this.mActivity.mHandler, null, id, flags);
        }
        throw new IllegalArgumentException("Bad id: " + id);
    }

    public int getBackStackEntryCount() {
        return this.mBackStack != null ? this.mBackStack.size() : 0;
    }

    public BackStackEntry getBackStackEntryAt(int index) {
        return (BackStackEntry) this.mBackStack.get(index);
    }

    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            this.mBackStackChangeListeners = new ArrayList();
        }
        this.mBackStackChangeListeners.add(listener);
    }

    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners != null) {
            this.mBackStackChangeListeners.remove(listener);
        }
    }

    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            throw new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager");
        }
        bundle.putInt(key, fragment.mIndex);
    }

    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= this.mActive.size()) {
            throw new IllegalStateException("Fragement no longer exists for key " + key + ": index " + index);
        }
        Fragment f = (Fragment) this.mActive.get(index);
        if (f != null) {
            return f;
        }
        throw new IllegalStateException("Fragement no longer exists for key " + key + ": index " + index);
    }

    public SavedState saveFragmentInstanceState(Fragment fragment) {
        if (fragment.mIndex < 0) {
            throw new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager");
        } else if (fragment.mState <= 0) {
            return null;
        } else {
            Bundle result = saveFragmentBasicState(fragment);
            return result != null ? new SavedState(result) : null;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(this.mActivity, sb);
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        int N;
        int i;
        Fragment f;
        String innerPrefix = prefix + "    ";
        if (this.mActive != null) {
            N = this.mActive.size();
            if (N > 0) {
                writer.print(prefix);
                writer.print("Active Fragments in ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(":");
                for (i = 0; i < N; i++) {
                    f = (Fragment) this.mActive.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f);
                    if (f != null) {
                        f.dump(innerPrefix, fd, writer, args);
                    }
                }
            }
        }
        if (this.mAdded != null) {
            N = this.mAdded.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Added Fragments:");
                for (i = 0; i < N; i++) {
                    f = (Fragment) this.mAdded.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }
        if (this.mCreatedMenus != null) {
            N = this.mCreatedMenus.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Fragments Created Menus:");
                for (i = 0; i < N; i++) {
                    f = (Fragment) this.mCreatedMenus.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }
        if (this.mBackStack != null) {
            N = this.mBackStack.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Back Stack:");
                for (i = 0; i < N; i++) {
                    BackStackRecord bs = (BackStackRecord) this.mBackStack.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, fd, writer, args);
                }
            }
        }
        synchronized (this) {
            if (this.mBackStackIndices != null) {
                N = this.mBackStackIndices.size();
                if (N > 0) {
                    writer.print(prefix);
                    writer.println("Back Stack Indices:");
                    for (i = 0; i < N; i++) {
                        bs = (BackStackRecord) this.mBackStackIndices.get(i);
                        writer.print(prefix);
                        writer.print("  #");
                        writer.print(i);
                        writer.print(": ");
                        writer.println(bs);
                    }
                }
            }
            if (this.mAvailBackStackIndices != null && this.mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(this.mAvailBackStackIndices.toArray()));
            }
        }
        if (this.mPendingActions != null) {
            N = this.mPendingActions.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Pending Actions:");
                for (i = 0; i < N; i++) {
                    Runnable r = (Runnable) this.mPendingActions.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(r);
                }
            }
        }
        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(this.mCurState);
        writer.print(" mStateSaved=");
        writer.print(this.mStateSaved);
        writer.print(" mDestroyed=");
        writer.println(this.mDestroyed);
        if (this.mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(this.mNeedMenuInvalidate);
        }
        if (this.mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(this.mNoTransactionsBecause);
        }
        if (this.mAvailIndices != null && this.mAvailIndices.size() > 0) {
            writer.print(prefix);
            writer.print("  mAvailIndices: ");
            writer.println(Arrays.toString(this.mAvailIndices.toArray()));
        }
    }

    static Animation makeOpenCloseAnimation(Context context, float startScale, float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale, 1, 0.5f, 1, 0.5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(220);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(220);
        set.addAnimation(alpha);
        return set;
    }

    static Animation makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(220);
        return anim;
    }

    Animation loadAnimation(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        Animation animObj = fragment.onCreateAnimation(transit, enter, fragment.mNextAnim);
        if (animObj != null) {
            return animObj;
        }
        if (fragment.mNextAnim != 0) {
            Animation anim = AnimationUtils.loadAnimation(this.mActivity, fragment.mNextAnim);
            if (anim != null) {
                return anim;
            }
        }
        if (transit == 0) {
            return null;
        }
        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }
        switch (styleIndex) {
            case ANIM_STYLE_OPEN_ENTER:
                return makeOpenCloseAnimation(this.mActivity, 1.125f, 1.0f, 0.0f, 1.0f);
            case ANIM_STYLE_OPEN_EXIT:
                return makeOpenCloseAnimation(this.mActivity, 1.0f, 0.975f, 1.0f, 0.0f);
            case ANIM_STYLE_CLOSE_ENTER:
                return makeOpenCloseAnimation(this.mActivity, 0.975f, 1.0f, 0.0f, 1.0f);
            case ANIM_STYLE_CLOSE_EXIT:
                return makeOpenCloseAnimation(this.mActivity, 1.0f, 1.075f, 1.0f, 0.0f);
            case ANIM_STYLE_FADE_ENTER:
                return makeFadeAnimation(this.mActivity, 0.0f, 1.0f);
            case ANIM_STYLE_FADE_EXIT:
                return makeFadeAnimation(this.mActivity, 1.0f, 0.0f);
            default:
                if (transitionStyle == 0 && this.mActivity.getWindow() != null) {
                    transitionStyle = this.mActivity.getWindow().getAttributes().windowAnimations;
                }
                return transitionStyle == 0 ? null : null;
        }
    }

    public void performPendingDeferredStart(Fragment f) {
        if (!f.mDeferStart) {
            return;
        }
        if (this.mExecutingActions) {
            this.mHavePendingDeferredStart = true;
            return;
        }
        f.mDeferStart = false;
        moveToState(f, this.mCurState, 0, 0);
    }

    void moveToState(Fragment f, int newState, int transit, int transitionStyle) {
        if (!f.mAdded && newState > 1) {
            newState = ANIM_STYLE_OPEN_ENTER;
        }
        if (f.mRemoving && newState > f.mState) {
            newState = f.mState;
        }
        if (f.mDeferStart && f.mState < 4 && newState > 3) {
            newState = ANIM_STYLE_CLOSE_ENTER;
        }
        Animation anim;
        if (f.mState < newState) {
            if (!f.mFromLayout || f.mInLayout) {
                if (f.mAnimatingAway != null) {
                    f.mAnimatingAway = null;
                    moveToState(f, f.mStateAfterAnimating, 0, 0);
                }
                ViewGroup container;
                switch (f.mState) {
                    case SettingInfoDB.SETTING_NOTIFY_FIRST:
                        if (DEBUG) {
                            Log.v(TAG, "moveto CREATED: " + f);
                        }
                        if (f.mSavedFragmentState != null) {
                            f.mSavedViewState = f.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            f.mTarget = getFragment(f.mSavedFragmentState, TARGET_STATE_TAG);
                            if (f.mTarget != null) {
                                f.mTargetRequestCode = f.mSavedFragmentState.getInt(TARGET_REQUEST_CODE_STATE_TAG, 0);
                            }
                            f.mUserVisibleHint = f.mSavedFragmentState.getBoolean(USER_VISIBLE_HINT_TAG, true);
                            if (!f.mUserVisibleHint) {
                                f.mDeferStart = true;
                                if (newState > 3) {
                                    newState = ANIM_STYLE_CLOSE_ENTER;
                                }
                            }
                        }
                        f.mActivity = this.mActivity;
                        f.mFragmentManager = this.mActivity.mFragments;
                        f.mCalled = false;
                        f.onAttach(this.mActivity);
                        if (f.mCalled) {
                            this.mActivity.onAttachFragment(f);
                            if (!f.mRetaining) {
                                f.mCalled = false;
                                f.onCreate(f.mSavedFragmentState);
                                if (!f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onCreate()");
                                }
                            }
                            f.mRetaining = false;
                            if (f.mFromLayout) {
                                f.mView = f.onCreateView(f.getLayoutInflater(f.mSavedFragmentState), null, f.mSavedFragmentState);
                                if (f.mView != null) {
                                    f.mInnerView = f.mView;
                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                    if (f.mHidden) {
                                        f.mView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                                    }
                                    f.onViewCreated(f.mView, f.mSavedFragmentState);
                                } else {
                                    f.mInnerView = null;
                                }
                            }
                            if (newState > 1) {
                                if (DEBUG) {
                                    Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                                }
                                if (!f.mFromLayout) {
                                    container = null;
                                    if (f.mContainerId != 0) {
                                        container = this.mActivity.findViewById(f.mContainerId);
                                        if (container == null && !f.mRestored) {
                                            throw new IllegalArgumentException("No view found for id 0x" + Integer.toHexString(f.mContainerId) + " for fragment " + f);
                                        }
                                    }
                                    f.mContainer = container;
                                    f.mView = f.onCreateView(f.getLayoutInflater(f.mSavedFragmentState), container, f.mSavedFragmentState);
                                    if (f.mView == null) {
                                        f.mInnerView = f.mView;
                                        f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                        if (container != null) {
                                            anim = loadAnimation(f, transit, true, transitionStyle);
                                            if (anim != null) {
                                                f.mView.startAnimation(anim);
                                            }
                                            container.addView(f.mView);
                                        }
                                        if (f.mHidden) {
                                            f.mView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                                        }
                                        f.onViewCreated(f.mView, f.mSavedFragmentState);
                                    } else {
                                        f.mInnerView = null;
                                    }
                                }
                                f.mCalled = false;
                                f.onActivityCreated(f.mSavedFragmentState);
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onActivityCreated()");
                                }
                                if (f.mView != null) {
                                    f.restoreViewState();
                                }
                                f.mSavedFragmentState = null;
                            }
                            if (newState > 3) {
                                if (DEBUG) {
                                    Log.v(TAG, "moveto STARTED: " + f);
                                }
                                f.mCalled = false;
                                f.performStart();
                                if (!f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onStart()");
                                }
                            }
                            if (newState > 4) {
                                if (DEBUG) {
                                    Log.v(TAG, "moveto RESUMED: " + f);
                                }
                                f.mCalled = false;
                                f.mResumed = true;
                                f.onResume();
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onResume()");
                                }
                                f.mSavedFragmentState = null;
                                f.mSavedViewState = null;
                            }
                        } else {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onAttach()");
                        }
                    case ANIM_STYLE_OPEN_ENTER:
                        if (newState > 1) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                            }
                            if (f.mFromLayout) {
                                container = null;
                                if (f.mContainerId != 0) {
                                    container = this.mActivity.findViewById(f.mContainerId);
                                    throw new IllegalArgumentException("No view found for id 0x" + Integer.toHexString(f.mContainerId) + " for fragment " + f);
                                }
                                f.mContainer = container;
                                f.mView = f.onCreateView(f.getLayoutInflater(f.mSavedFragmentState), container, f.mSavedFragmentState);
                                if (f.mView == null) {
                                    f.mInnerView = null;
                                } else {
                                    f.mInnerView = f.mView;
                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                    if (container != null) {
                                        anim = loadAnimation(f, transit, true, transitionStyle);
                                        if (anim != null) {
                                            f.mView.startAnimation(anim);
                                        }
                                        container.addView(f.mView);
                                    }
                                    if (f.mHidden) {
                                        f.mView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                                    }
                                    f.onViewCreated(f.mView, f.mSavedFragmentState);
                                }
                            }
                            f.mCalled = false;
                            f.onActivityCreated(f.mSavedFragmentState);
                            if (f.mCalled) {
                                if (f.mView != null) {
                                    f.restoreViewState();
                                }
                                f.mSavedFragmentState = null;
                            } else {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onActivityCreated()");
                            }
                        }
                        if (newState > 3) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto STARTED: " + f);
                            }
                            f.mCalled = false;
                            f.performStart();
                            if (f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onStart()");
                            }
                        }
                        if (newState > 4) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto RESUMED: " + f);
                            }
                            f.mCalled = false;
                            f.mResumed = true;
                            f.onResume();
                            if (f.mCalled) {
                                f.mSavedFragmentState = null;
                                f.mSavedViewState = null;
                            } else {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onResume()");
                            }
                        }
                    case ANIM_STYLE_OPEN_EXIT:
                    case ANIM_STYLE_CLOSE_ENTER:
                        if (newState > 3) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto STARTED: " + f);
                            }
                            f.mCalled = false;
                            f.performStart();
                            if (f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onStart()");
                            }
                        }
                        if (newState > 4) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto RESUMED: " + f);
                            }
                            f.mCalled = false;
                            f.mResumed = true;
                            f.onResume();
                            if (f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onResume()");
                            }
                            f.mSavedFragmentState = null;
                            f.mSavedViewState = null;
                        }
                        break;
                    case ANIM_STYLE_CLOSE_EXIT:
                        if (newState > 4) {
                            if (DEBUG) {
                                Log.v(TAG, "moveto RESUMED: " + f);
                            }
                            f.mCalled = false;
                            f.mResumed = true;
                            f.onResume();
                            if (f.mCalled) {
                                f.mSavedFragmentState = null;
                                f.mSavedViewState = null;
                            } else {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onResume()");
                            }
                        }
                }
            }
            return;
        } else if (f.mState > newState) {
            View v;
            Fragment fragment;
            switch (f.mState) {
                case ANIM_STYLE_OPEN_ENTER:
                    if (newState < 1) {
                        if (this.mDestroyed && f.mAnimatingAway != null) {
                            v = f.mAnimatingAway;
                            f.mAnimatingAway = null;
                            v.clearAnimation();
                        }
                        if (f.mAnimatingAway != null) {
                            f.mStateAfterAnimating = newState;
                            newState = ANIM_STYLE_OPEN_ENTER;
                        } else {
                            if (DEBUG) {
                                Log.v(TAG, "movefrom CREATED: " + f);
                            }
                            if (!f.mRetaining) {
                                f.mCalled = false;
                                f.onDestroy();
                                if (!f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroy()");
                                }
                            }
                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDetach()");
                            } else if (f.mRetaining) {
                                f.mActivity = null;
                                f.mFragmentManager = null;
                            } else {
                                makeInactive(f);
                            }
                        }
                    }
                    break;
                case ANIM_STYLE_OPEN_EXIT:
                    if (newState < 2) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        }
                        if (!(f.mView == null || this.mActivity.isFinishing() || f.mSavedViewState != null)) {
                            saveFragmentViewState(f);
                        }
                        f.mCalled = false;
                        f.performDestroyView();
                        if (f.mCalled) {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroyView()");
                        }
                        if (!(f.mView == null || f.mContainer == null)) {
                            anim = null;
                            if (this.mCurState > 0 && !this.mDestroyed) {
                                anim = loadAnimation(f, transit, HONEYCOMB, transitionStyle);
                            }
                            if (anim != null) {
                                fragment = f;
                                f.mAnimatingAway = f.mView;
                                f.mStateAfterAnimating = newState;
                                anim.setAnimationListener(new AnonymousClass_5(fragment));
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                    if (newState < 1) {
                        v = f.mAnimatingAway;
                        f.mAnimatingAway = null;
                        v.clearAnimation();
                        if (f.mAnimatingAway != null) {
                            if (DEBUG) {
                                Log.v(TAG, "movefrom CREATED: " + f);
                            }
                            if (f.mRetaining) {
                                f.mCalled = false;
                                f.onDestroy();
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroy()");
                                }
                            }
                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDetach()");
                            } else if (f.mRetaining) {
                                f.mActivity = null;
                                f.mFragmentManager = null;
                            } else {
                                makeInactive(f);
                            }
                        } else {
                            f.mStateAfterAnimating = newState;
                            newState = ANIM_STYLE_OPEN_ENTER;
                        }
                    }
                    break;
                case ANIM_STYLE_CLOSE_ENTER:
                    if (newState < 3) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom STOPPED: " + f);
                        }
                        f.performReallyStop();
                    }
                    if (newState < 2) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        }
                        saveFragmentViewState(f);
                        f.mCalled = false;
                        f.performDestroyView();
                        if (f.mCalled) {
                            anim = null;
                            anim = loadAnimation(f, transit, HONEYCOMB, transitionStyle);
                            if (anim != null) {
                                fragment = f;
                                f.mAnimatingAway = f.mView;
                                f.mStateAfterAnimating = newState;
                                anim.setAnimationListener(new AnonymousClass_5(fragment));
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                            f.mContainer = null;
                            f.mView = null;
                            f.mInnerView = null;
                        } else {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroyView()");
                        }
                    }
                    if (newState < 1) {
                        v = f.mAnimatingAway;
                        f.mAnimatingAway = null;
                        v.clearAnimation();
                        if (f.mAnimatingAway != null) {
                            f.mStateAfterAnimating = newState;
                            newState = ANIM_STYLE_OPEN_ENTER;
                        } else {
                            if (DEBUG) {
                                Log.v(TAG, "movefrom CREATED: " + f);
                            }
                            if (f.mRetaining) {
                                f.mCalled = false;
                                f.onDestroy();
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroy()");
                                }
                            }
                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDetach()");
                            } else if (f.mRetaining) {
                                makeInactive(f);
                            } else {
                                f.mActivity = null;
                                f.mFragmentManager = null;
                            }
                        }
                    }
                    break;
                case ANIM_STYLE_CLOSE_EXIT:
                    if (newState < 4) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom STARTED: " + f);
                        }
                        f.mCalled = false;
                        f.performStop();
                        if (!f.mCalled) {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onStop()");
                        }
                    }
                    if (newState < 3) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom STOPPED: " + f);
                        }
                        f.performReallyStop();
                    }
                    if (newState < 2) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        }
                        saveFragmentViewState(f);
                        f.mCalled = false;
                        f.performDestroyView();
                        if (f.mCalled) {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroyView()");
                        }
                        anim = null;
                        anim = loadAnimation(f, transit, HONEYCOMB, transitionStyle);
                        if (anim != null) {
                            fragment = f;
                            f.mAnimatingAway = f.mView;
                            f.mStateAfterAnimating = newState;
                            anim.setAnimationListener(new AnonymousClass_5(fragment));
                            f.mView.startAnimation(anim);
                        }
                        f.mContainer.removeView(f.mView);
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                    if (newState < 1) {
                        v = f.mAnimatingAway;
                        f.mAnimatingAway = null;
                        v.clearAnimation();
                        if (f.mAnimatingAway != null) {
                            if (DEBUG) {
                                Log.v(TAG, "movefrom CREATED: " + f);
                            }
                            if (f.mRetaining) {
                                f.mCalled = false;
                                f.onDestroy();
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroy()");
                                }
                            }
                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDetach()");
                            } else if (f.mRetaining) {
                                f.mActivity = null;
                                f.mFragmentManager = null;
                            } else {
                                makeInactive(f);
                            }
                        } else {
                            f.mStateAfterAnimating = newState;
                            newState = ANIM_STYLE_OPEN_ENTER;
                        }
                    }
                    break;
                case ANIM_STYLE_FADE_ENTER:
                    if (newState < 5) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom RESUMED: " + f);
                        }
                        f.mCalled = false;
                        f.onPause();
                        if (f.mCalled) {
                            f.mResumed = false;
                        } else {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onPause()");
                        }
                    }
                    if (newState < 4) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom STARTED: " + f);
                        }
                        f.mCalled = false;
                        f.performStop();
                        if (f.mCalled) {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onStop()");
                        }
                    }
                    if (newState < 3) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom STOPPED: " + f);
                        }
                        f.performReallyStop();
                    }
                    if (newState < 2) {
                        if (DEBUG) {
                            Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        }
                        saveFragmentViewState(f);
                        f.mCalled = false;
                        f.performDestroyView();
                        if (f.mCalled) {
                            anim = null;
                            anim = loadAnimation(f, transit, HONEYCOMB, transitionStyle);
                            if (anim != null) {
                                fragment = f;
                                f.mAnimatingAway = f.mView;
                                f.mStateAfterAnimating = newState;
                                anim.setAnimationListener(new AnonymousClass_5(fragment));
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                            f.mContainer = null;
                            f.mView = null;
                            f.mInnerView = null;
                        } else {
                            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroyView()");
                        }
                    }
                    if (newState < 1) {
                        v = f.mAnimatingAway;
                        f.mAnimatingAway = null;
                        v.clearAnimation();
                        if (f.mAnimatingAway != null) {
                            f.mStateAfterAnimating = newState;
                            newState = ANIM_STYLE_OPEN_ENTER;
                        } else {
                            if (DEBUG) {
                                Log.v(TAG, "movefrom CREATED: " + f);
                            }
                            if (f.mRetaining) {
                                f.mCalled = false;
                                f.onDestroy();
                                if (f.mCalled) {
                                    throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDestroy()");
                                }
                            }
                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onDetach()");
                            } else if (f.mRetaining) {
                                makeInactive(f);
                            } else {
                                f.mActivity = null;
                                f.mFragmentManager = null;
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        f.mState = newState;
    }

    void moveToState(Fragment f) {
        moveToState(f, this.mCurState, 0, 0);
    }

    void moveToState(int newState, boolean always) {
        moveToState(newState, 0, 0, always);
    }

    void moveToState(int newState, int transit, int transitStyle, boolean always) {
        if (this.mActivity == null && newState != 0) {
            throw new IllegalStateException("No activity");
        } else if (always || this.mCurState != newState) {
            this.mCurState = newState;
            if (this.mActive != null) {
                boolean loadersRunning = HONEYCOMB;
                for (int i = 0; i < this.mActive.size(); i++) {
                    Fragment f = (Fragment) this.mActive.get(i);
                    if (f != null) {
                        moveToState(f, newState, transit, transitStyle);
                        if (f.mLoaderManager != null) {
                            loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                        }
                    }
                }
                if (!loadersRunning) {
                    startPendingDeferredFragments();
                }
                if (this.mNeedMenuInvalidate && this.mActivity != null && this.mCurState == 5) {
                    this.mActivity.supportInvalidateOptionsMenu();
                    this.mNeedMenuInvalidate = false;
                }
            }
        }
    }

    void startPendingDeferredFragments() {
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.get(i);
                if (f != null) {
                    performPendingDeferredStart(f);
                }
            }
        }
    }

    void makeActive(Fragment f) {
        if (f.mIndex < 0) {
            if (this.mAvailIndices == null || this.mAvailIndices.size() <= 0) {
                if (this.mActive == null) {
                    this.mActive = new ArrayList();
                }
                f.setIndex(this.mActive.size());
                this.mActive.add(f);
                return;
            }
            f.setIndex(((Integer) this.mAvailIndices.remove(this.mAvailIndices.size() - 1)).intValue());
            this.mActive.set(f.mIndex, f);
        }
    }

    void makeInactive(Fragment f) {
        if (f.mIndex >= 0) {
            if (DEBUG) {
                Log.v(TAG, "Freeing fragment index " + f.mIndex);
            }
            this.mActive.set(f.mIndex, null);
            if (this.mAvailIndices == null) {
                this.mAvailIndices = new ArrayList();
            }
            this.mAvailIndices.add(Integer.valueOf(f.mIndex));
            this.mActivity.invalidateSupportFragmentIndex(f.mIndex);
            f.initState();
        }
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        if (this.mAdded == null) {
            this.mAdded = new ArrayList();
        }
        if (DEBUG) {
            Log.v(TAG, "add: " + fragment);
        }
        makeActive(fragment);
        if (!fragment.mDetached) {
            this.mAdded.add(fragment);
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
            }
        }
    }

    public void removeFragment(Fragment fragment, int transition, int transitionStyle) {
        boolean inactive;
        int i = 0;
        if (DEBUG) {
            Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        }
        if (fragment.isInBackStack()) {
            inactive = false;
        } else {
            inactive = true;
        }
        if (!fragment.mDetached || inactive) {
            this.mAdded.remove(fragment);
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
            if (!inactive) {
                i = 1;
            }
            moveToState(fragment, i, transition, transitionStyle);
        }
    }

    public void hideFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "hide: " + fragment);
        }
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            if (fragment.mView != null) {
                Animation anim = loadAnimation(fragment, transition, true, transitionStyle);
                if (anim != null) {
                    fragment.mView.startAnimation(anim);
                }
                fragment.mView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
            }
            if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.onHiddenChanged(true);
        }
    }

    public void showFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "show: " + fragment);
        }
        if (fragment.mHidden) {
            fragment.mHidden = false;
            if (fragment.mView != null) {
                Animation anim = loadAnimation(fragment, transition, true, transitionStyle);
                if (anim != null) {
                    fragment.mView.startAnimation(anim);
                }
                fragment.mView.setVisibility(0);
            }
            if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.onHiddenChanged(HONEYCOMB);
        }
    }

    public void detachFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "detach: " + fragment);
        }
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                this.mAdded.remove(fragment);
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
                moveToState(fragment, (int) ANIM_STYLE_OPEN_ENTER, transition, transitionStyle);
            }
        }
    }

    public void attachFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "attach: " + fragment);
        }
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                this.mAdded.add(fragment);
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                moveToState(fragment, this.mCurState, transition, transitionStyle);
            }
        }
    }

    public Fragment findFragmentById(int id) {
        if (this.mActive != null) {
            int i;
            Fragment f;
            for (i = this.mAdded.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mAdded.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
            for (i = this.mActive.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mActive.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByTag(String tag) {
        if (!(this.mActive == null || tag == null)) {
            int i;
            Fragment f;
            for (i = this.mAdded.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
            for (i = this.mActive.size() - 1; i >= 0; i--) {
                f = (Fragment) this.mActive.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        if (!(this.mActive == null || who == null)) {
            for (int i = this.mActive.size() - 1; i >= 0; i--) {
                Fragment f = (Fragment) this.mActive.get(i);
                if (f != null && who.equals(f.mWho)) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (this.mStateSaved) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        } else if (this.mNoTransactionsBecause != null) {
            throw new IllegalStateException("Can not perform this action inside of " + this.mNoTransactionsBecause);
        }
    }

    public void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (this.mActivity == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (this.mPendingActions == null) {
                this.mPendingActions = new ArrayList();
            }
            this.mPendingActions.add(action);
            if (this.mPendingActions.size() == 1) {
                this.mActivity.mHandler.removeCallbacks(this.mExecCommit);
                this.mActivity.mHandler.post(this.mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            int index;
            if (this.mAvailBackStackIndices == null || this.mAvailBackStackIndices.size() <= 0) {
                if (this.mBackStackIndices == null) {
                    this.mBackStackIndices = new ArrayList();
                }
                index = this.mBackStackIndices.size();
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.add(bse);
                return index;
            }
            index = ((Integer) this.mAvailBackStackIndices.remove(this.mAvailBackStackIndices.size() - 1)).intValue();
            if (DEBUG) {
                Log.v(TAG, "Adding back stack index " + index + " with " + bse);
            }
            this.mBackStackIndices.set(index, bse);
            return index;
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList();
            }
            int N = this.mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    this.mBackStackIndices.add(null);
                    if (this.mAvailBackStackIndices == null) {
                        this.mAvailBackStackIndices = new ArrayList();
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Adding available back stack index " + N);
                    }
                    this.mAvailBackStackIndices.add(Integer.valueOf(N));
                    N++;
                }
                if (DEBUG) {
                    Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                }
                this.mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            this.mBackStackIndices.set(index, null);
            if (this.mAvailBackStackIndices == null) {
                this.mAvailBackStackIndices = new ArrayList();
            }
            if (DEBUG) {
                Log.v(TAG, "Freeing back stack index " + index);
            }
            this.mAvailBackStackIndices.add(Integer.valueOf(index));
        }
    }

    public boolean execPendingActions() {
        if (this.mExecutingActions) {
            throw new IllegalStateException("Recursive entry to executePendingTransactions");
        } else if (Looper.myLooper() != this.mActivity.mHandler.getLooper()) {
            throw new IllegalStateException("Must be called from main thread of process");
        } else {
            int i;
            boolean didSomething = HONEYCOMB;
            while (true) {
                synchronized (this) {
                    if (this.mPendingActions == null || this.mPendingActions.size() == 0) {
                        break;
                    }
                    int numActions = this.mPendingActions.size();
                    if (this.mTmpActions == null || this.mTmpActions.length < numActions) {
                        this.mTmpActions = new Runnable[numActions];
                    }
                    this.mPendingActions.toArray(this.mTmpActions);
                    this.mPendingActions.clear();
                    this.mActivity.mHandler.removeCallbacks(this.mExecCommit);
                    this.mExecutingActions = true;
                    for (i = 0; i < numActions; i++) {
                        this.mTmpActions[i].run();
                        this.mTmpActions[i] = null;
                    }
                    this.mExecutingActions = false;
                    didSomething = true;
                }
            }
            if (this.mHavePendingDeferredStart) {
                boolean loadersRunning = HONEYCOMB;
                for (i = 0; i < this.mActive.size(); i++) {
                    Fragment f = (Fragment) this.mActive.get(i);
                    if (f != null && f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
                if (!loadersRunning) {
                    this.mHavePendingDeferredStart = false;
                    startPendingDeferredFragments();
                }
            }
            return didSomething;
        }
    }

    void reportBackStackChanged() {
        if (this.mBackStackChangeListeners != null) {
            for (int i = 0; i < this.mBackStackChangeListeners.size(); i++) {
                ((OnBackStackChangedListener) this.mBackStackChangeListeners.get(i)).onBackStackChanged();
            }
        }
    }

    void addBackStackState(BackStackRecord state) {
        if (this.mBackStack == null) {
            this.mBackStack = new ArrayList();
        }
        this.mBackStack.add(state);
        reportBackStackChanged();
    }

    boolean popBackStackState(Handler handler, String name, int id, int flags) {
        if (this.mBackStack == null) {
            return HONEYCOMB;
        }
        if (name == null && id < 0 && (flags & 1) == 0) {
            int last = this.mBackStack.size() - 1;
            if (last < 0) {
                return HONEYCOMB;
            }
            ((BackStackRecord) this.mBackStack.remove(last)).popFromBackStack(true);
            reportBackStackChanged();
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                BackStackRecord bss;
                index = this.mBackStack.size() - 1;
                while (index >= 0) {
                    bss = (BackStackRecord) this.mBackStack.get(index);
                    if (name != null && name.equals(bss.getName())) {
                        break;
                    }
                    if (id >= 0 && id == bss.mIndex) {
                        break;
                    }
                    index--;
                }
                if (index < 0) {
                    return HONEYCOMB;
                }
                if ((flags & 1) != 0) {
                    index--;
                    while (index >= 0) {
                        bss = (BackStackRecord) this.mBackStack.get(index);
                        if (name == null || !name.equals(bss.getName())) {
                            if (id < 0 || id != bss.mIndex) {
                                break;
                            }
                        }
                        index--;
                    }
                }
            }
            if (index == this.mBackStack.size() - 1) {
                return HONEYCOMB;
            }
            int i;
            ArrayList<BackStackRecord> states = new ArrayList();
            for (i = this.mBackStack.size() - 1; i > index; i--) {
                states.add(this.mBackStack.remove(i));
            }
            int LAST = states.size() - 1;
            for (i = 0; i <= LAST; i++) {
                boolean z;
                if (DEBUG) {
                    Log.v(TAG, "Popping back stack state: " + states.get(i));
                }
                BackStackRecord backStackRecord = (BackStackRecord) states.get(i);
                if (i == LAST) {
                    z = true;
                } else {
                    z = false;
                }
                backStackRecord.popFromBackStack(z);
            }
            reportBackStackChanged();
        }
        return true;
    }

    ArrayList<Fragment> retainNonConfig() {
        ArrayList<Fragment> fragments = null;
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.get(i);
                if (f != null && f.mRetainInstance) {
                    int i2;
                    if (fragments == null) {
                        fragments = new ArrayList();
                    }
                    fragments.add(f);
                    f.mRetaining = true;
                    if (f.mTarget != null) {
                        i2 = f.mTarget.mIndex;
                    } else {
                        i2 = -1;
                    }
                    f.mTargetIndex = i2;
                }
            }
        }
        return fragments;
    }

    void saveFragmentViewState(Fragment f) {
        if (f.mInnerView != null) {
            if (this.mStateArray == null) {
                this.mStateArray = new SparseArray();
            } else {
                this.mStateArray.clear();
            }
            f.mInnerView.saveHierarchyState(this.mStateArray);
            if (this.mStateArray.size() > 0) {
                f.mSavedViewState = this.mStateArray;
                this.mStateArray = null;
            }
        }
    }

    Bundle saveFragmentBasicState(Fragment f) {
        Bundle bundle = null;
        if (this.mStateBundle == null) {
            this.mStateBundle = new Bundle();
        }
        f.onSaveInstanceState(this.mStateBundle);
        if (!this.mStateBundle.isEmpty()) {
            bundle = this.mStateBundle;
            this.mStateBundle = null;
        }
        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putSparseParcelableArray(VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            bundle.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }
        return bundle;
    }

    Parcelable saveAllState() {
        execPendingActions();
        if (HONEYCOMB) {
            this.mStateSaved = true;
        }
        if (this.mActive == null || this.mActive.size() <= 0) {
            return null;
        }
        int i;
        int N = this.mActive.size();
        FragmentState[] active = new FragmentState[N];
        boolean haveFragments = HONEYCOMB;
        for (i = 0; i < N; i++) {
            Fragment f = (Fragment) this.mActive.get(i);
            if (f != null) {
                haveFragments = true;
                FragmentState fs = new FragmentState(f);
                active[i] = fs;
                if (f.mState <= 0 || fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                } else {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);
                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            String msg = "Failure saving state: " + f + " has target not in fragment manager: " + f.mTarget;
                            Log.e(TAG, msg);
                            dump("  ", null, new PrintWriter(new LogWriter(TAG)), new String[0]);
                            throw new IllegalStateException(msg);
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState, TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
                        }
                    }
                }
                if (DEBUG) {
                    Log.v(TAG, "Saved state of " + f + ": " + fs.mSavedFragmentState);
                }
            }
        }
        if (haveFragments) {
            int[] added = null;
            BackStackState[] backStack = null;
            if (this.mAdded != null) {
                N = this.mAdded.size();
                if (N > 0) {
                    added = new int[N];
                    for (i = 0; i < N; i++) {
                        added[i] = ((Fragment) this.mAdded.get(i)).mIndex;
                        if (DEBUG) {
                            Log.v(TAG, "saveAllState: adding fragment #" + i + ": " + this.mAdded.get(i));
                        }
                    }
                }
            }
            if (this.mBackStack != null) {
                N = this.mBackStack.size();
                if (N > 0) {
                    backStack = new BackStackState[N];
                    for (i = 0; i < N; i++) {
                        backStack[i] = new BackStackState(this, (BackStackRecord) this.mBackStack.get(i));
                        if (DEBUG) {
                            Log.v(TAG, "saveAllState: adding back stack #" + i + ": " + this.mBackStack.get(i));
                        }
                    }
                }
            }
            Parcelable fms = new FragmentManagerState();
            fms.mActive = active;
            fms.mAdded = added;
            fms.mBackStack = backStack;
            return fms;
        } else if (!DEBUG) {
            return null;
        } else {
            Log.v(TAG, "saveAllState: no fragments!");
            return null;
        }
    }

    void restoreAllState(Parcelable state, ArrayList<Fragment> nonConfig) {
        if (state != null) {
            FragmentManagerState fms = (FragmentManagerState) state;
            if (fms.mActive != null) {
                int i;
                Fragment f;
                FragmentState fs;
                if (nonConfig != null) {
                    for (i = 0; i < nonConfig.size(); i++) {
                        f = (Fragment) nonConfig.get(i);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                        }
                        fs = fms.mActive[f.mIndex];
                        fs.mInstance = f;
                        f.mSavedViewState = null;
                        f.mBackStackNesting = 0;
                        f.mInLayout = false;
                        f.mAdded = false;
                        f.mTarget = null;
                        if (fs.mSavedFragmentState != null) {
                            fs.mSavedFragmentState.setClassLoader(this.mActivity.getClassLoader());
                            f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                        }
                    }
                }
                this.mActive = new ArrayList(fms.mActive.length);
                if (this.mAvailIndices != null) {
                    this.mAvailIndices.clear();
                }
                for (i = 0; i < fms.mActive.length; i++) {
                    fs = fms.mActive[i];
                    if (fs != null) {
                        f = fs.instantiate(this.mActivity);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: adding #" + i + ": " + f);
                        }
                        this.mActive.add(f);
                        fs.mInstance = null;
                    } else {
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: adding #" + i + ": (null)");
                        }
                        this.mActive.add(null);
                        if (this.mAvailIndices == null) {
                            this.mAvailIndices = new ArrayList();
                        }
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: adding avail #" + i);
                        }
                        this.mAvailIndices.add(Integer.valueOf(i));
                    }
                }
                if (nonConfig != null) {
                    for (i = 0; i < nonConfig.size(); i++) {
                        f = (Fragment) nonConfig.get(i);
                        if (f.mTargetIndex >= 0) {
                            if (f.mTargetIndex < this.mActive.size()) {
                                f.mTarget = (Fragment) this.mActive.get(f.mTargetIndex);
                            } else {
                                Log.w(TAG, "Re-attaching retained fragment " + f + " target no longer exists: " + f.mTargetIndex);
                                f.mTarget = null;
                            }
                        }
                    }
                }
                if (fms.mAdded != null) {
                    this.mAdded = new ArrayList(fms.mAdded.length);
                    for (i = 0; i < fms.mAdded.length; i++) {
                        f = (Fragment) this.mActive.get(fms.mAdded[i]);
                        if (f == null) {
                            throw new IllegalStateException("No instantiated fragment for index #" + fms.mAdded[i]);
                        }
                        f.mAdded = true;
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: making added #" + i + ": " + f);
                        }
                        this.mAdded.add(f);
                    }
                } else {
                    this.mAdded = null;
                }
                if (fms.mBackStack != null) {
                    this.mBackStack = new ArrayList(fms.mBackStack.length);
                    for (i = 0; i < fms.mBackStack.length; i++) {
                        BackStackRecord bse = fms.mBackStack[i].instantiate(this);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: adding bse #" + i + " (index " + bse.mIndex + "): " + bse);
                        }
                        this.mBackStack.add(bse);
                        if (bse.mIndex >= 0) {
                            setBackStackIndex(bse.mIndex, bse);
                        }
                    }
                    return;
                }
                this.mBackStack = null;
            }
        }
    }

    public void attachActivity(FragmentActivity activity) {
        if (this.mActivity != null) {
            throw new IllegalStateException();
        }
        this.mActivity = activity;
    }

    public void noteStateNotSaved() {
        this.mStateSaved = false;
    }

    public void dispatchCreate() {
        this.mStateSaved = false;
        moveToState(ANIM_STYLE_OPEN_ENTER, HONEYCOMB);
    }

    public void dispatchActivityCreated() {
        this.mStateSaved = false;
        moveToState(ANIM_STYLE_OPEN_EXIT, HONEYCOMB);
    }

    public void dispatchStart() {
        this.mStateSaved = false;
        moveToState(ANIM_STYLE_CLOSE_EXIT, HONEYCOMB);
    }

    public void dispatchResume() {
        this.mStateSaved = false;
        moveToState(ANIM_STYLE_FADE_ENTER, HONEYCOMB);
    }

    public void dispatchPause() {
        moveToState(ANIM_STYLE_CLOSE_EXIT, HONEYCOMB);
    }

    public void dispatchStop() {
        this.mStateSaved = true;
        moveToState(ANIM_STYLE_CLOSE_ENTER, HONEYCOMB);
    }

    public void dispatchReallyStop() {
        moveToState(ANIM_STYLE_OPEN_EXIT, HONEYCOMB);
    }

    public void dispatchDestroy() {
        this.mDestroyed = true;
        execPendingActions();
        moveToState(0, HONEYCOMB);
        this.mActivity = null;
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null) {
                    f.onConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void dispatchLowMemory() {
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null) {
                    f.onLowMemory();
                }
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int i;
        Fragment f;
        boolean show = HONEYCOMB;
        ArrayList<Fragment> newMenus = null;
        if (this.mActive != null) {
            for (i = 0; i < this.mAdded.size(); i++) {
                f = (Fragment) this.mAdded.get(i);
                if (f != null && !f.mHidden && f.mHasMenu && f.mMenuVisible) {
                    show = true;
                    f.onCreateOptionsMenu(menu, inflater);
                    if (newMenus == null) {
                        newMenus = new ArrayList();
                    }
                    newMenus.add(f);
                }
            }
        }
        if (this.mCreatedMenus != null) {
            for (i = 0; i < this.mCreatedMenus.size(); i++) {
                f = (Fragment) this.mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(f)) {
                    f.onDestroyOptionsMenu();
                }
            }
        }
        this.mCreatedMenus = newMenus;
        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = HONEYCOMB;
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null && !f.mHidden && f.mHasMenu && f.mMenuVisible) {
                    show = true;
                    f.onPrepareOptionsMenu(menu);
                }
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null && !f.mHidden && f.mHasMenu && f.mMenuVisible && f.onOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return HONEYCOMB;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null && !f.mHidden && f.onContextItemSelected(item)) {
                    return true;
                }
            }
        }
        return HONEYCOMB;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (this.mActive != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null && !f.mHidden && f.mHasMenu && f.mMenuVisible) {
                    f.onOptionsMenuClosed(menu);
                }
            }
        }
    }

    public static int reverseTransit(int transit) {
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                return FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                return FragmentTransaction.TRANSIT_FRAGMENT_FADE;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                return FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
            default:
                return 0;
        }
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? ANIM_STYLE_OPEN_ENTER : ANIM_STYLE_OPEN_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? ANIM_STYLE_FADE_ENTER : ANIM_STYLE_FADE_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? ANIM_STYLE_CLOSE_ENTER : ANIM_STYLE_CLOSE_EXIT;
                break;
        }
        return animAttr;
    }
}
