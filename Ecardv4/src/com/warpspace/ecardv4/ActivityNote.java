package com.warpspace.ecardv4;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ActivityNote extends ActionBarActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.design_actionbar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// this function is called when either action bar icon is tapped
		switch (item.getItemId()) {
		case R.id.design_discard:
			Toast.makeText(this, "Cancel changes to Note", Toast.LENGTH_SHORT).show();
			this.finish();
			return true;
		case R.id.design_save:
			Toast.makeText(this, "Saved changes to Note", Toast.LENGTH_SHORT).show();
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
}
