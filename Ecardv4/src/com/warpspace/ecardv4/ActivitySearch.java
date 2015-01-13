package com.warpspace.ecardv4;

import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.warpspace.ecardv4.infrastructure.SearchListNameAdapter;

import android.app.Activity;
import android.os.Bundle;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ActivitySearch extends Activity {
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);

    StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);

    SearchListNameAdapter adapter = new SearchListNameAdapter(this);
    AlphaInAnimationAdapter animationAdapter = new AlphaInAnimationAdapter(
      adapter);
    StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(
      animationAdapter);
    stickyListHeadersAdapterDecorator
      .setListViewWrapper(new StickyListHeadersListViewWrapper(listView));

    assert animationAdapter.getViewAnimator() != null;
    animationAdapter.getViewAnimator().setInitialDelayMillis(500);

    assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
    stickyListHeadersAdapterDecorator.getViewAnimator().setInitialDelayMillis(
      500);

    listView.setAdapter(stickyListHeadersAdapterDecorator);
  }
}
