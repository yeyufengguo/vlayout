package com.alibaba.android.vlayout.layout;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.alibaba.android.vlayout.LayoutManagerHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.VirtualLayoutManager.LayoutStateWrapper;

/**
 * {@link com.alibaba.android.vlayout.LayoutHelper} that provides basic methods
 */
public abstract class BaseLayoutHelper extends MarginLayoutHelper {

    private static final String TAG = BaseLayoutHelper.class.getSimpleName();

    public static boolean DEBUG = false;

    protected Rect mLayoutRegion = new Rect();

    View mLayoutView;

    int mBgColor;

    float mAspectRatio = Float.NaN;

    public BaseLayoutHelper() {

    }

    public int getBgColor() {
        return this.mBgColor;
    }

    /**
     * Set backgroundColor for LayoutView
     *
     * @param bgColor
     */
    public void setBgColor(int bgColor) {
        this.mBgColor = bgColor;
    }

    public void setAspectRatio(float aspectRatio) {
        this.mAspectRatio = aspectRatio;
    }

    private int mItemCount = 0;

    /**
     * The number of items in current layout
     *
     * @return the number of child views
     */
    @Override
    public int getItemCount() {
        return mItemCount;
    }

    public void setItemCount(int itemCount) {
        this.mItemCount = itemCount;
    }


    /**
     * Retrieve next view and add it into layout, this is to make sure that view are added by order
     *
     * @param recycler    recycler generate views
     * @param layoutState current layout state
     * @param helper      helper to add views
     * @param result      chunk result to tell layoutManager whether layout process goes end
     * @return next view to render, null if no more view available
     */
    @Nullable
    public final View nextView(RecyclerView.Recycler recycler, LayoutStateWrapper layoutState, LayoutManagerHelper helper, LayoutChunkResult result) {
        View view = layoutState.next(recycler);
        if (view == null) {
            // if we are laying out views in scrap, this may return null which means there is
            // no more items to layout.
            if (DEBUG && !layoutState.hasScrapList()) {
                throw new RuntimeException("received null view when unexpected");
            }
            // if there is no more views can be retrieved, this layout process is finished
            result.mFinished = true;
            return null;
        }

        helper.addChildView(layoutState, view);
        return view;
    }


    @Override
    public void beforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                             LayoutManagerHelper helper) {
        if (DEBUG) {
            Log.d(TAG, "call beforeLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (mLayoutView != null) {
                // TODO: recycle LayoutView
                // helper.detachChildView(mLayoutView);
            }
        } else {
            // if no layoutView is required, remove it
            if (mLayoutView != null) {
                helper.removeChildView(mLayoutView);
                mLayoutView = null;
            }
        }
    }

    /**
     * Tell whether the scrolled value is valid, if not, means it's a layout processing without scrolling
     *
     * @param scrolled value of how many pixels does scrolled
     * @return true means during a scrolling process, false means during a layout process.
     */
    protected boolean isValidScrolled(int scrolled) {
        return scrolled != Integer.MAX_VALUE && scrolled != Integer.MIN_VALUE;
    }


    @Override
    public void afterLayout(RecyclerView.Recycler recycler, RecyclerView.State state,
                            int startPosition, int endPosition, int scrolled,
                            LayoutManagerHelper helper) {
        if (DEBUG) {
            Log.d(TAG, "call afterLayout() on " + this.getClass().getSimpleName());
        }


        if (requireLayoutView()) {
            if (isValidScrolled(scrolled) && mLayoutView != null) {
                // initial layout do reset
                mLayoutRegion.union(mLayoutView.getLeft(), mLayoutView.getTop(), mLayoutView.getRight(), mLayoutView.getBottom());
            }


            if (!mLayoutRegion.isEmpty()) {
                if (isValidScrolled(scrolled)) {
                    if (helper.getOrientation() == VirtualLayoutManager.VERTICAL)
                        mLayoutRegion.offset(0, -scrolled);
                    else {
                        mLayoutRegion.offset(-scrolled, 0);
                    }
                }

                if (mLayoutRegion.intersects(0, 0, helper.getContentWidth(), helper.getContentHeight())) {

                    if (mLayoutView == null) {
                        mLayoutView = helper.generateLayoutView();
                        helper.addOffFlowView(mLayoutView, true);
                    }

                    bindLayoutView(mLayoutView);
                    return;
                } else {
                    mLayoutRegion.set(0, 0, 0, 0);
                    if (mLayoutView != null)
                        mLayoutView.layout(0, 0, 0, 0);
                }
            }
        }

        if (mLayoutView != null) {
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

    }


    /**
     * Called when {@link com.alibaba.android.vlayout.LayoutHelper} get dropped
     * Do default clean jobs defined by framework
     *
     * @param helper LayoutManagerHelper
     */
    @Override
    public final void clear(LayoutManagerHelper helper) {
        // remove LayoutViews if there is one
        if (mLayoutView != null) {
            helper.removeChildView(mLayoutView);
            mLayoutView = null;
        }

        // call user defined
        onClear(helper);
    }

    /**
     * Called when {@link com.alibaba.android.vlayout.LayoutHelper} get dropped, do clean custom jobs
     *
     * @param helper
     */
    protected void onClear(LayoutManagerHelper helper) {

    }

    /**
     * @return
     */
    @Override
    public boolean requireLayoutView() {
        return mBgColor != 0 || mLayoutViewBindListener != null;
    }

    public abstract void layoutViews(RecyclerView.Recycler recycler, RecyclerView.State state,
                                     LayoutStateWrapper layoutState, LayoutChunkResult result,
                                     LayoutManagerHelper helper);


    @Override
    public void doLayout(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutStateWrapper layoutState, LayoutChunkResult result, LayoutManagerHelper helper) {
        layoutViews(recycler, state, layoutState, result, helper);
    }

    /**
     * Helper function which do layout children and also update layoutRegion
     * but it won't consider margin in layout, so you need take care of margin if you apply margin to your layoutView
     *
     * @param child  child that will be laid
     * @param left   left position
     * @param top    top position
     * @param right  right position
     * @param bottom bottom position
     * @param helper layoutManagerHelper, help to lay child
     */
    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper) {
        layoutChild(child, left, top, right, bottom, helper, false);
    }

    protected void layoutChild(final View child, int left, int top, int right, int bottom, @NonNull LayoutManagerHelper helper, boolean addLayoutRegionWithMargin) {
        helper.layoutChild(child, left, top, right, bottom);
        if (requireLayoutView()) {
            if (addLayoutRegionWithMargin) {
                mLayoutRegion.union(left - mMarginLeft, top - mMarginTop, right + mMarginRight, bottom + mMarginBottom);
            } else {
                mLayoutRegion.union(left, top, right, bottom);
            }
        }

    }

    /**
     * Listener to handle LayoutViews, like bgImage
     */
    public interface LayoutViewBindListener {
        void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper);
    }

    private LayoutViewBindListener mLayoutViewBindListener;

    public void setLayoutViewBindListener(LayoutViewBindListener bindListener) {
        mLayoutViewBindListener = bindListener;
    }

    @Override
    public void bindLayoutView(@NonNull final View layoutView) {
        layoutView.measure(View.MeasureSpec.makeMeasureSpec(mLayoutRegion.width(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mLayoutRegion.height(), View.MeasureSpec.EXACTLY));
        layoutView.layout(mLayoutRegion.left, mLayoutRegion.top, mLayoutRegion.right, mLayoutRegion.bottom);
        layoutView.setBackgroundColor(mBgColor);

        if (mLayoutViewBindListener != null) {
            mLayoutViewBindListener.onBind(layoutView, this);
        }

        // reset region rectangle
        mLayoutRegion.set(0, 0, 0, 0);
    }

    /**
     * Helper methods to handle focus states for views
     *
     * @param result
     * @param views
     */
    protected void handleStateOnResult(LayoutChunkResult result, View... views) {
        if (views == null) return;

        for (View view : views) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            // Consume the available space if the view is not removed OR changed
            if (params.isItemRemoved() || params.isItemChanged()) {
                result.mIgnoreConsumed = true;
            }

            // used when search a focusable view
            result.mFocusable = result.mFocusable || view.isFocusable();

            if (result.mFocusable && result.mIgnoreConsumed) {
                break;
            }
        }
    }


    protected void calculateRect(int mainAxisSize, Rect areaRect, LayoutStateWrapper layoutState, LayoutManagerHelper helper) {
        if (helper.getOrientation() == VirtualLayoutManager.VERTICAL) {
            areaRect.left = helper.getPaddingLeft() + mMarginLeft;
            areaRect.right = helper.getContentWidth() - helper.getPaddingRight() - mMarginRight;

            // whether this layout pass is layout to start or to end
            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                // fill start, from bottom to top
                areaRect.bottom = layoutState.getOffset() - mMarginBottom;
                areaRect.top = layoutState.getOffset() - mainAxisSize + mMarginTop;
            } else {
                areaRect.top = layoutState.getOffset() + mMarginTop;
                areaRect.bottom = layoutState.getOffset() + mainAxisSize - mMarginBottom;
            }
        } else {
            areaRect.top = helper.getPaddingTop() + mMarginTop;
            areaRect.bottom = helper.getContentHeight() - helper.getPaddingBottom() - mMarginBottom;

            if (layoutState.getLayoutDirection() == LayoutStateWrapper.LAYOUT_START) {
                areaRect.right = layoutState.getOffset() - mMarginRight;
                areaRect.left = layoutState.getOffset() - mainAxisSize + mMarginLeft;
            } else {
                areaRect.left = layoutState.getOffset() + mMarginLeft;
                areaRect.right = layoutState.getOffset() + mainAxisSize - mMarginRight;
            }
        }

        mLayoutRegion.set(areaRect);
    }

}
