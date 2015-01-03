package com.warpspace.ecardv4.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * ScrollView¤ÎÖÐ¤ÎGridView¤Ç¤â¸ß¤µ¤ò¿É‰ä¤Ë¤¹¤ë<br>
 * http://stackoverflow.com/questions/8481844/gridview-height-gets-cut
 */
public class ExpandableHeightGridView extends GridView {

  boolean expanded = false;

  public ExpandableHeightGridView(Context context) {
    super(context);
  }

  public ExpandableHeightGridView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpandableHeightGridView(Context context, AttributeSet attrs,
    int defStyle) {
    super(context, attrs, defStyle);
  }

  public boolean isExpanded() {
    return expanded;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int heightSpec;

    if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
      // The great Android "hackatlon", the love, the magic.
      // The two leftmost bits in the height measure spec have
      // a special meaning, hence we can't use them to describe height.
      heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
        MeasureSpec.AT_MOST);
    } else {
      // Any other height should be respected as is.
      heightSpec = heightMeasureSpec;
    }

    super.onMeasure(widthMeasureSpec, heightSpec);
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }
}
