package com.warpspace.ecardv4.utils;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class MyViewPager extends ViewPager {

  public MyViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    // TODO Auto-generated constructor stub
  }

  public MyViewPager(Context context) {
    super(context);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
  }

}
