package com.warpspace.ecardv4.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLayout extends LinearLayout {
  public SquareLayout(Context context) {
    super(context);
  }

  public SquareLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    // int orientation = getResources().getConfiguration().orientation;
    // int width;
    // int height;
    // int scale = 1;
    // width = MeasureSpec.getSize(widthMeasureSpec);
    // height = (int) (width* scale);
    // super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
    // MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

  }
}
