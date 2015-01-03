package com.warpspace.ecardv4.utils;

import com.warpspace.ecardv4.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class RobotoTextView extends TextView {

  public RobotoTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs);
  }

  public RobotoTextView(Context context) {
    super(context);
    init(null);
  }

  private void init(AttributeSet attrs) {
    if (attrs != null) {
      TypedArray a = getContext().obtainStyledAttributes(attrs,
        R.styleable.RobotoTextView);
      String fontName = a.getString(R.styleable.RobotoTextView_robotoFont);
      if (fontName != null) {
        Typeface myTypeface = Typeface.createFromAsset(
          getContext().getAssets(), "fonts/" + fontName);
        setTypeface(myTypeface);
      }
      a.recycle();
    }

  }

}