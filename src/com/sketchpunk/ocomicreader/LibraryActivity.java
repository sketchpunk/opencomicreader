package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;
import com.sketchpunk.ocomicreader.ui.CoverGridView;

import android.os.Bundle;
import android.preference.PreferenceManager;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.FragmentActivity;

public class LibraryActivity extends FragmentActivity implements
		ComicLibrary.SyncCallback, View.OnClickListener,
		OnItemSelectedListener, PopupMenu.OnMenuItemClickListener,
		CoverGridView.iCallback {

	private CoverGridView mGridView;
	private Button mBtnSync;
	private Button mBtnMenu;
	private Spinner mSpFilter;
	private SpinnerAdapter mSpinAdapter;
	private TextView mSeriesLbl;
	private ProgressDialog mProgress;

	/*
	 * Main
	 */
	@Override
	public void onDestroy() {
		mGridView.dispose();
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_library);
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// setup background
		RelativeLayout mLayouts = (RelativeLayout) findViewById(R.id.MainLayout);
		switch (Integer.parseInt(prefs.getString("libraryBackground", "0"))) {
		case 0: // Solid
			mLayouts.setBackgroundColor(Color.parseColor("#FF000000"));
			break;
		case 1: // opaque
			mLayouts.setBackgroundColor(Color.parseColor("#BB000000"));
			break;
		case 2: // WallPaper
			final WallpaperManager wm = WallpaperManager.getInstance(this);
			final Drawable wallpaperDrawable = wm.getFastDrawable();
			getWindow().setBackgroundDrawable(wallpaperDrawable);
			break;
		}

		mBtnSync = (Button) findViewById(R.id.btnSync);
		mBtnSync.setOnClickListener(this);
		mBtnMenu = (Button) findViewById(R.id.btnMenu);
		mBtnMenu.setOnClickListener(this);

		mSpinAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item,
				this.getResources().getStringArray(R.array.libraryFilter));
		mSpFilter = (Spinner) findViewById(R.id.spFilter);
		mSpFilter.setOnItemSelectedListener(this);
		mSpFilter.setAdapter(mSpinAdapter);

		mSeriesLbl = (TextView) findViewById(R.id.lblSeries);

		mGridView = (CoverGridView) findViewById(R.id.lvMain);

		// Load state of filter from Bundle
		if (savedInstanceState != null) {
			mGridView.setSeriesFilter(savedInstanceState
					.getString("mSeriesFilter"));
			mGridView.setFilterMode(savedInstanceState.getInt("mFilterMode"));
		} else {// if no state, load in default pref.
			mGridView.setFilterMode(Integer.parseInt(prefs.getString(
					"libraryFilter", "0")));
		}// if

		mSpFilter.setSelection(mGridView.getFilterMode());

		mGridView.init();
		registerForContextMenu(mGridView); // Route event from Activity to View
	}

	@Override
	protected void onSaveInstanceState(Bundle siState) {
		// Save the state of the filters so
		siState.putString("mSeriesFilter", mGridView.getSeriesFilter());
		siState.putInt("mFilterMode", mGridView.getFilterMode());
		super.onSaveInstanceState(siState);
	}

	/*
	 * State
	 */
	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mGridView.refreshData();
	}

	/*
	 * Filter Spinner
	 */
	// @Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		// initially, refreshdata gets
		// called twice,its a waste.
		if (mGridView.getFilterMode() != pos) {
			mGridView.setFilterMode(pos);
			mGridView.refreshData();
		}
	}

	// @Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	// @Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, PrefActivity.class);
			// intent.putExtra("path",(String)view.getTag());
			this.startActivityForResult(intent, 0);
			break;
		case R.id.menu_about:
			sage.ui.Dialogs.About(this, this.getText(R.string.app_about));
			break;
		}
		return true;
	}

	/*
	 * Context menu
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		switch (v.getId()) {
		case R.id.lvMain:
			mGridView.createContextMenu(menu, v, menuInfo);
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		mGridView.contextItemSelected(item);
		return true;
	}

	/*
	 * UI Events
	 */
	@Override
	public void onBackPressed() {
		// Override back press to make it easy to back out of series filter.
		if (mGridView.isSeriesFiltered() && mGridView.getSeriesFilter() != "") {
			mGridView.setSeriesFilter("");
			mGridView.refreshData();
		} else
			super.onBackPressed();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnSync:
			sage.ui.Dialogs.ConfirmBox(this, "Sync Library",
					"Are you sure you want sync the library?",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							startSync();
						}
					});

			break;
		case R.id.btnMenu:
			PopupMenu popup = new PopupMenu(this, v);
			popup.setOnMenuItemClickListener(this);

			MenuInflater inflater = popup.getMenuInflater();
			inflater.inflate(R.menu.activity_main, popup.getMenu());
			popup.show();

			break;
		}
	}

	/*
	 * Sync Library
	 */
	private void startSync() {
		if (ComicLibrary.startSync(this)) {
			if (mProgress != null) {
				if (!mProgress.isShowing()) {
					ProgressDialog.show(this, "Library Syncing", "", true);
					return;
				}// if
			}// if

			mProgress = ProgressDialog.show(this, "Library Syncing", "", true);
		} else {
			Toast.makeText(this, "Sync did not start", Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void onSyncProgress(String txt) {
		if (mProgress != null) {
			if (mProgress.isShowing())
				mProgress.setMessage(txt);
		}
	}

	public void onSyncComplete(int status) {
		try {
			if (mProgress != null) {
				mProgress.dismiss();
				mProgress = null;
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error closing progress dialog",
					Toast.LENGTH_LONG).show();
		}

		switch (status) {
		case ComicLibrary.STATUS_COMPLETE:
			mGridView.refreshData();
			break;
		case ComicLibrary.STATUS_NOSETTINGS:
			Toast.makeText(this,
					"No sync folders have been set. Go to settings.",
					Toast.LENGTH_LONG).show();
			break;
		}
	}

	public void onDataRefreshComplete() {
		if (mSeriesLbl.getVisibility() != View.GONE)
			mSeriesLbl.setVisibility(View.GONE);

		if (mGridView.isSeriesFiltered()) {// Filter by series
			if (!mGridView.getSeriesFilter().isEmpty()) {
				mSeriesLbl.setText(mGridView.getSeriesFilter() + " [ "
						+ Integer.toString(mGridView.recordCount) + " ]");
				mSeriesLbl.setVisibility(View.VISIBLE);
			}
		}
	}

}
