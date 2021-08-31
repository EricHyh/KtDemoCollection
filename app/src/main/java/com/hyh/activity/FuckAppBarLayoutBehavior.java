package com.hyh.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.hyh.demo.R;

public class FuckAppBarLayoutBehavior extends AppBarLayout.Behavior {

    private static final String TAG = "FuckAppBarLayoutBehavio";

    private boolean ignoreTouchEvent = false;
    private boolean redispatchTouchEvent = false;
    private boolean overTouchSlop = false;
    private float initialX = 0;
    private float initialY = 0;
    private float touchSlop;

    private final BottomSheetViewInfo bottomSheetViewInfoForNestedScroll = new BottomSheetViewInfo();

    public FuckAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /*@Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        int action = ev.getActionMasked();
        parent.requestDisallowInterceptTouchEvent(true);
        Log.d(TAG, "onInterceptTouchEvent: " + action);

        return false;
    }*/

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        /*int action = ev.getActionMasked();
        Log.d(TAG, "onTouchEvent: " + action);
        if (redispatchTouchEvent && action != MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (redispatchTouchEvent || ignoreTouchEvent) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    redispatchTouchEvent = false;
                    ignoreTouchEvent = true;
                    parent.requestDisallowInterceptTouchEvent(false);
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    ignoreTouchEvent = false;
                    break;
                }
            }
            return true;
        }
        if (isTransformedTouchPointInView(parent, child, ev)) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    initialX = ev.getX();
                    initialY = ev.getY();
                    overTouchSlop = false;
                    parent.requestDisallowInterceptTouchEvent(true);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    float ty = ev.getY() - initialY;
                    if (!overTouchSlop && Math.abs(ty) > touchSlop) {
                        overTouchSlop = true;
                        if (ty > 0) {
                            redispatchTouchEvent = true;
                            MotionEvent newEvent = MotionEvent.obtain(ev);
                            newEvent.setAction(MotionEvent.ACTION_DOWN);
                            parent.getRootView().dispatchTouchEvent(newEvent);
                            return true;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    overTouchSlop = false;
                    break;
                }
            }
        }*/
        return super.onTouchEvent(parent, child, ev);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        if (!target.canScrollVertically(-1) && child.getTop() == 0) {
            fillBottomSheetViewInfo(parent, bottomSheetViewInfoForNestedScroll);
            if (bottomSheetViewInfoForNestedScroll.isValid()) {
                bottomSheetViewInfoForNestedScroll
                        .bottomSheetBehavior
                        .onStartNestedScroll(
                                bottomSheetViewInfoForNestedScroll.bottomSheetParent,
                                bottomSheetViewInfoForNestedScroll.bottomSheet,
                                bottomSheetViewInfoForNestedScroll.bottomSheet,
                                target,
                                nestedScrollAxes,
                                type);
            }
        }
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (bottomSheet == null
                || bottomSheet.getTop() + bottomSheet.getHeight() == bottomSheetParent.getHeight()) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
        if (child.getTop() == 0 && bottomSheetBehavior != null) {
            int[] newConsumed = new int[2];
            bottomSheetBehavior.onNestedPreScroll(bottomSheetParent, bottomSheet, target, dx, dy, newConsumed, type);
            consumed[0] += newConsumed[0];
            consumed[1] += newConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (bottomSheet == null
                || bottomSheet.getTop() + bottomSheet.getHeight() == bottomSheetParent.getHeight()) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        }
        if (child.getTop() == 0 && bottomSheetBehavior != null) {
            bottomSheetBehavior
                    .onNestedScroll(bottomSheetParent, bottomSheet, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target, int type) {
        super.onStopNestedScroll(coordinatorLayout, abl, target, type);
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.onStopNestedScroll(bottomSheetParent, bottomSheet, target, type);
            bottomSheetViewInfoForNestedScroll.reset();
        }
    }

    @Override
    public void onNestedScrollAccepted(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type);
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.onNestedScrollAccepted(bottomSheetParent, bottomSheet, bottomSheet, target, axes, type);
        }
    }

    @Override
    public boolean onNestedFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout child, @NonNull View target, float velocityX, float velocityY, boolean consumed) {
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (child.getTop() == 0
                && bottomSheetBehavior != null
                && bottomSheetBehavior.onNestedFling(bottomSheetParent, bottomSheet, target, velocityX, velocityY, consumed)) {
            return true;
        }
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout child, @NonNull View target, float velocityX, float velocityY) {
        View bottomSheet = bottomSheetViewInfoForNestedScroll.bottomSheet;
        CoordinatorLayout bottomSheetParent = bottomSheetViewInfoForNestedScroll.bottomSheetParent;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = bottomSheetViewInfoForNestedScroll.bottomSheetBehavior;
        if (child.getTop() == 0
                && bottomSheetBehavior != null
                && bottomSheetBehavior.onNestedPreFling(bottomSheetParent, bottomSheet, target, velocityX, velocityY)) {
            return true;
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    private void fillBottomSheetViewInfo(
            CoordinatorLayout childCoordinatorLayout,
            BottomSheetViewInfo bottomSheetViewInfo
    ) {
        bottomSheetViewInfo.reset();
        View bottomSheet = findBottomSheet(childCoordinatorLayout);
        if (bottomSheet == null) return;
        CoordinatorLayout bottomSheetParent = (CoordinatorLayout) bottomSheet.getParent();
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        if (!(layoutParams instanceof CoordinatorLayout.LayoutParams)) return;
        CoordinatorLayout.Behavior<View> behavior = ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
        if (behavior == null) return;
        View scrollingChild = findScrollingChild(childCoordinatorLayout);
        if (scrollingChild == null) return;
        bottomSheetViewInfo.bottomSheetParent = bottomSheetParent;
        bottomSheetViewInfo.bottomSheet = bottomSheet;
        bottomSheetViewInfo.bottomSheetBehavior = behavior;
        bottomSheetViewInfo.scrollingChild = scrollingChild;
    }

    private View findBottomSheet(CoordinatorLayout childCoordinatorLayout) {
        ViewParent parent = childCoordinatorLayout.getParent();
        while (parent != null) {
            if (parent instanceof View
                    && ((View) parent).getId() == R.id.design_bottom_sheet
                    && parent.getParent() instanceof CoordinatorLayout
            ) {
                return (View) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private boolean isTransformedTouchPointInView(
            View parent, View child, MotionEvent event
    ) {
        Float[] point = new Float[]{event.getX(), event.getY()};
        point[0] += (parent.getScrollX() - child.getLeft());
        point[1] += (parent.getScrollY() - child.getTop());

        point[0] -= child.getTranslationX();
        point[1] -= child.getTranslationY();

        return pointInView(child, point[0], point[1], 0.0F);
    }

    private boolean pointInView(View view, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < view.getRight() - view.getLeft() + slop &&
                localY < view.getBottom() - view.getTop() + slop;
    }

    private class BottomSheetViewInfo {

        CoordinatorLayout bottomSheetParent = null;
        View bottomSheet = null;
        CoordinatorLayout.Behavior<View> bottomSheetBehavior = null;
        View scrollingChild = null;

        boolean isValid() {
            return bottomSheetParent != null
                    && bottomSheet != null
                    && bottomSheetBehavior != null
                    && scrollingChild != null;
        }

        void reset() {
            bottomSheetParent = null;
            bottomSheet = null;
            bottomSheetBehavior = null;
            scrollingChild = null;
        }
    }
}