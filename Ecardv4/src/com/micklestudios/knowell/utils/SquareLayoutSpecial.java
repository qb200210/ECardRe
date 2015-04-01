package com.micklestudios.knowell.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLayoutSpecial extends LinearLayout {
  public SquareLayoutSpecial(Context context) {
    super(context);
  }

  public SquareLayoutSpecial(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (widthMeasureSpec >= heightMeasureSpec)
      super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    else
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
