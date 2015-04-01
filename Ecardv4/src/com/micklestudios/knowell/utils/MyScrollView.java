package com.micklestudios.knowell.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

  private Context mContext;
  private boolean mScrollable = true;
  private boolean mTopReached = true;

  public boolean ismTopReached() {
    return mTopReached;
  }

  public void setmTopReached(boolean mTopReached) {
    this.mTopReached = mTopReached;
  }

  public boolean ismScrollable() {
    return mScrollable;
  }

  public void setmScrollable(boolean mScrollable) {
    this.mScrollable = mScrollable;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (mScrollable)
      return super.onTouchEvent(ev);
    else
      return mScrollable;
  }

  public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mContext = context;
    // TODO Auto-generated constructor stub
  }

  public MyScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    // TODO Auto-generated constructor stub
  }

  public MyScrollView(Context context) {
    super(context);
    mContext = context;
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    View view = (View) getChildAt(getChildCount() - 1);
    int diff = (view.getBottom() - (getHeight() + getScrollY() + view.getTop()));// Calculate
                                                                                 // the
                                                                                 // scrolldiff
    if (diff == 0) { // if diff is zero, then the bottom has been reached
      // Toast.makeText(mContext, "Bottom has been reached", Toast.LENGTH_SHORT
      // ).show();
    }
    view = (View) getChildAt(0);
    int diff1 = (getScrollY());// Calculate the scrolldiff
    // Log.d("top", String.valueOf(diff1));
    if (diff1 == 0) { // if diff is zero, then the bottom has been reached
      // Toast.makeText(mContext, "Top has been reached", Toast.LENGTH_SHORT
      // ).show();
      setmTopReached(true);
      // If top reached, set flag so QR container can be shown
    } else {
      setmTopReached(false);
      // If top not reached, do not show QR
    }

    super.onScrollChanged(l, t, oldl, oldt);
  }

}
