package com.hyh.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.hyh.demo.R;

public class FuckAppBarLayoutBehaviorV2 extends AppBarLayout.Behavior {

    private static final String TAG = "FuckAppBarLayoutBehavio";

    private boolean startNestedScrollByTouchEvent = false;

    private int lastTouchX;
    private int lastTouchY;

    private final int[] scrollOffset = new int[2];
    private final int[] scrollConsumed = new int[2];


    private final BottomSheetViewInfo bottomSheetViewInfoForTouchEvent = new BottomSheetViewInfo();
    private final BottomSheetViewInfo bottomSheetViewInfoForNestedScroll = new BottomSheetViewInfo();

    public FuckAppBarLayoutBehaviorV2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent: " + ev.getAction());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        return super.onInterceptTouchEvent(parent, child, ev);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        Log.d(TAG, "onTouchEvent: " + ev.getAction());
        if (child.getTop() == 0 || startNestedScrollByTouchEvent) {
            int action = ev.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    fillBottomSheetViewInfo(parent, bottomSheetViewInfoForTouchEvent);
                    if (bottomSheetViewInfoForTouchEvent.isValid()) {
                        startNestedScrollByTouchEvent = true;
                        boolean nestedScroll = bottomSheetViewInfoForTouchEvent
                                .bottomSheetBehavior
                                .onStartNestedScroll(
                                        bottomSheetViewInfoForTouchEvent.bottomSheetParent,
                                        bottomSheetViewInfoForTouchEvent.bottomSheet,
                                        bottomSheetViewInfoForTouchEvent.bottomSheet,
                                        bottomSheetViewInfoForTouchEvent.scrollingChild,
                                        NestedScrollView.SCROLL_AXIS_VERTICAL,
                                        ViewCompat.TYPE_TOUCH);
                        if (nestedScroll) {
                            bottomSheetViewInfoForTouchEvent
                                    .bottomSheetBehavior
                                    .onNestedScrollAccepted(
                                            bottomSheetViewInfoForTouchEvent.bottomSheetParent,
                                            bottomSheetViewInfoForTouchEvent.bottomSheet,
                                            bottomSheetViewInfoForTouchEvent.bottomSheet,
                                            bottomSheetViewInfoForTouchEvent.scrollingChild,
                                            NestedScrollView.SCROLL_AXIS_VERTICAL,
                                            ViewCompat.TYPE_TOUCH);
                        }
                        lastTouchX = Math.round(ev.getX());
                        lastTouchY = Math.round(ev.getY());
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (startNestedScrollByTouchEvent
                            && bottomSheetViewInfoForTouchEvent.isValid()) {
                        int x = Math.round(ev.getX());
                        int y = Math.round(ev.getY());
                        int dx = lastTouchX - x;
                        int dy = lastTouchY - y;
                        lastTouchX = x;
                        lastTouchY = y;
                        scrollConsumed[0] = 0;
                        scrollConsumed[1] = 0;

                        bottomSheetViewInfoForTouchEvent.bottomSheet.getLocationInWindow(scrollOffset);
                        int startScrollOffsetX = scrollOffset[0];
                        int startScrollOffsetY = scrollOffset[1];

                        if (child.getTop() == 0) {
                            bottomSheetViewInfoForTouchEvent.
                                    bottomSheetBehavior
                                    .onNestedPreScroll(
                                            bottomSheetViewInfoForTouchEvent.bottomSheetParent,
                                            bottomSheetViewInfoForTouchEvent.bottomSheet,
                                            bottomSheetViewInfoForTouchEvent.scrollingChild,
                                            dx,
                                            dy,
                                            scrollConsumed,
                                            ViewCompat.TYPE_TOUCH);
                        }


                        bottomSheetViewInfoForTouchEvent.bottomSheet.getLocationInWindow(scrollOffset);
                        scrollOffset[0] -= startScrollOffsetX;
                        scrollOffset[1] -= startScrollOffsetY;

                        if (scrollConsumed[0] != 0 || scrollConsumed[1] != 0) {
                            lastTouchX = x - scrollOffset[0];
                            lastTouchY = y - scrollOffset[1];
                            return true;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    if (startNestedScrollByTouchEvent
                            && bottomSheetViewInfoForTouchEvent.isValid()) {
                        bottomSheetViewInfoForTouchEvent.
                                bottomSheetBehavior
                                .onStopNestedScroll(
                                        bottomSheetViewInfoForTouchEvent.bottomSheetParent,
                                        bottomSheetViewInfoForTouchEvent.bottomSheet,
                                        bottomSheetViewInfoForTouchEvent.scrollingChild,
                                        ViewCompat.TYPE_TOUCH
                                );
                    }
                    bottomSheetViewInfoForTouchEvent.reset();
                    startNestedScrollByTouchEvent = false;
                }
                break;
            }
        }
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


    private static class BottomSheetViewInfo {

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