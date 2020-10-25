package com.hyh.dialog.core

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

class DragableLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    companion object {

        const val DIRECTION_NONE = 0
        const val DIRECTION_TOP_TO_BOTTOM = 1
        const val DIRECTION_BOTTOM_TO_TOP = 1 shl 1
        const val DIRECTION_LEFT_TO_RIGHT = 1 shl 2
        const val DIRECTION_RIGHT_TO_LEFT = 1 shl 3

    }

    private val mDragHelper: ViewDragHelper by lazy {
        ViewDragHelper.create(this, 0.5f, DragHelperCallback())
    }

    var mTouchSlop: Int = 0
    var mDragSize: Int = 0

    var mDragDirection: Int =/* DIRECTION_TOP_TO_BOTTOM or
            DIRECTION_BOTTOM_TO_TOP or*/
            DIRECTION_LEFT_TO_RIGHT /*or
            DIRECTION_RIGHT_TO_LEFT*/


    var mVelocityThreshold: Int = 1500


    init {
        mTouchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop
        mVelocityThreshold = max(
            ViewConfiguration.get(getContext()).scaledMaximumFlingVelocity / 5,
            mVelocityThreshold
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return mDragHelper.shouldInterceptTouchEvent(ev!!)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mDragHelper.processTouchEvent(event!!)
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }


    override fun computeScroll() {
        super.computeScroll()
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }


    inner class DragHelperCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean = true

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            super.onViewCaptured(capturedChild, activePointerId)
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            invalidate()
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            Log.d("DragableLayout", "xvel = $xvel, yvel = $yvel")

            var finalLeft = 0
            var finalTop = 0
            if (releasedChild.left != 0 && releasedChild.top != 0) {
                var expectedFinalLeft = 0
                var expectedFinalTop = 0
                if ((xvel.pow(2) + yvel.pow(2)).pow(0.5f) > mVelocityThreshold) {
                    expectedFinalLeft = if (isBothHorizontalDirection(mDragDirection)) {
                        if (xvel > 0) {
                            this@DragableLayout.measuredWidth
                        } else {
                            -this@DragableLayout.measuredWidth
                        }
                    } else {
                        if (xvel > 0) {
                            if (releasedChild.left > 0) {
                                this@DragableLayout.measuredWidth
                            } else {
                                0
                            }
                        } else {
                            if (releasedChild.left > 0) {
                                0
                            } else {
                                -this@DragableLayout.measuredWidth
                            }
                        }
                    }
                    expectedFinalTop = if (isBothVerticalDirection(mDragDirection)) {
                        if (yvel > 0) {
                            this@DragableLayout.measuredHeight
                        } else {
                            -this@DragableLayout.measuredHeight
                        }
                    } else {
                        if (yvel > 0) {
                            if (releasedChild.top > 0) {
                                this@DragableLayout.measuredHeight
                            } else {
                                0
                            }
                        } else {
                            if (releasedChild.top > 0) {
                                0
                            } else {
                                -this@DragableLayout.measuredHeight
                            }
                        }
                    }
                } else if (
                    abs(releasedChild.left) > releasedChild.measuredWidth / 2
                    || abs(releasedChild.top) > releasedChild.measuredHeight / 2
                ) {
                    expectedFinalLeft = if (releasedChild.left < 0) {
                        -this@DragableLayout.measuredWidth
                    } else {
                        this@DragableLayout.measuredWidth
                    }
                    expectedFinalTop = if (releasedChild.top < 0) {
                        -this@DragableLayout.measuredHeight
                    } else {
                        this@DragableLayout.measuredHeight
                    }
                }

                if (expectedFinalLeft != 0 && expectedFinalTop != 0) {
                    val movableX = expectedFinalLeft - releasedChild.left
                    val movableY = expectedFinalTop - releasedChild.top

                    if (abs(movableX * yvel) < abs(movableY * xvel)) {
                        finalLeft = expectedFinalLeft
                        finalTop = if (xvel != 0.0f) {
                            releasedChild.top + (movableX * (yvel / xvel)).toInt()
                        } else {
                            expectedFinalTop
                        }
                    } else {
                        finalLeft = if (yvel != 0.0f) {
                            releasedChild.left + (movableY * (xvel / yvel)).toInt()
                        } else {
                            expectedFinalLeft
                        }
                        finalTop = expectedFinalTop
                    }
                }
            } else if (releasedChild.left != 0) {
                finalTop = 0
                finalLeft = if (abs(xvel) > mVelocityThreshold) {
                    if (releasedChild.left < 0) {
                        if (xvel < 0) {
                            -releasedChild.measuredWidth
                        } else {
                            0
                        }
                    } else {
                        if (xvel < 0) {
                            0
                        } else {
                            releasedChild.measuredWidth
                        }
                    }
                } else if (releasedChild.left > releasedChild.measuredWidth / 2) {
                    releasedChild.measuredWidth
                } else if (releasedChild.left < -releasedChild.measuredWidth / 2) {
                    -releasedChild.measuredWidth
                } else {
                    0
                }
            } else if (releasedChild.top != 0) {
                finalLeft = 0
                finalTop = if (abs(yvel) > mVelocityThreshold) {
                    if (releasedChild.top < 0) {
                        if (yvel < 0) {
                            -releasedChild.measuredHeight
                        } else {
                            0
                        }
                    } else {
                        if (yvel < 0) {
                            0
                        } else {
                            releasedChild.measuredHeight
                        }
                    }
                } else if (releasedChild.top > releasedChild.measuredHeight / 2) {
                    releasedChild.measuredHeight
                } else if (releasedChild.top < -releasedChild.measuredHeight / 2) {
                    -releasedChild.measuredHeight
                } else {
                    0
                }
            }
            mDragHelper.smoothSlideViewTo(releasedChild, finalLeft, finalTop)
            ViewCompat.postInvalidateOnAnimation(this@DragableLayout)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return if (isBothHorizontalDirection(mDragDirection)) {
                left
            } else if (mDragDirection and DIRECTION_LEFT_TO_RIGHT != 0) {
                if (left < 0) 0 else left
            } else if (mDragDirection and DIRECTION_RIGHT_TO_LEFT != 0) {
                if (left > 0) 0 else left
            } else {
                0
            }
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return if (isBothVerticalDirection(mDragDirection)) {
                top
            } else if (mDragDirection and DIRECTION_TOP_TO_BOTTOM != 0) {
                if (top < 0) 0 else top
            } else if (mDragDirection and DIRECTION_BOTTOM_TO_TOP != 0) {
                if (top > 0) 0 else top
            } else {
                0
            }
        }

        private fun isBothHorizontalDirection(direction: Int): Boolean =
            (direction and DIRECTION_LEFT_TO_RIGHT != 0) && (direction and DIRECTION_RIGHT_TO_LEFT != 0)

        private fun isBothVerticalDirection(direction: Int): Boolean =
            (direction and DIRECTION_TOP_TO_BOTTOM != 0) && (direction and DIRECTION_BOTTOM_TO_TOP != 0)

        private fun isBothDirection(direction: Int): Boolean =
            isBothHorizontalDirection(direction) && isBothVerticalDirection(direction)
    }
}