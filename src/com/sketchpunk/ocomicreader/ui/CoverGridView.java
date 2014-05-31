package com.sketchpunk.ocomicreader.ui;

import sage.adapter.SqlCursorAdapter;
import sage.data.SqlCursorLoader;
import sage.data.Sqlite;
import sage.loader.LoadImageView;
import sage.ui.ProgressCircle;

import com.sketchpunk.ocomicreader.R;
import com.sketchpunk.ocomicreader.ViewActivity;
import com.sketchpunk.ocomicreader.lib.ComicLibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import android.support.v4.app.LoaderManager;
import android.support.v4.app.FragmentActivity;

public class CoverGridView extends GridView implements
		SqlCursorAdapter.AdapterCallback, OnItemClickListener,
		LoaderManager.LoaderCallbacks<Cursor>,
		LoadImageView.OnImageLoadedListener {

	public interface iCallback {
		void onDataRefreshComplete();
	}

	private final String TAG = "COVERGRIDVIEW";
	private SqlCursorAdapter mAdapter;
	private Sqlite mDb;

	public int recordCount = 0;
	private int mFilterMode = 0;
	private String mSeriesFilter = "";

	private int mThumbHeight = 180;
	private int mThumbPadding = 60;
	private int mGridPadding = 60;
	private int mGridColNum = 2;

	private boolean mIsFirstRun = true;
	private String mThumbPath;

	public CoverGridView(Context context) {
		super(context);
	}// func

	public CoverGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CoverGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void init() {
		// Get Preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this.getContext());
		try {
			this.mGridColNum = prefs.getInt("libraryColCnt", 2);
			this.mGridPadding = prefs.getInt("libraryPadding", 60);
			this.mThumbPadding = prefs.getInt("libraryCoverPad", 60);
			this.mThumbHeight = prefs.getInt("libraryCoverHeight", 180);
		} catch (Exception e) {
			System.err.println("Error Loading Library Prefs " + e.getMessage());
		}

		// set values
		mThumbPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/OpenComicReader/thumbs/";

		mAdapter = new SqlCursorAdapter(this.getContext());
		mAdapter.setItemLayout(R.layout.listitem_library);
		mAdapter.setCallback(this);

		this.setNumColumns(mGridColNum);
		this.setPadding(mGridPadding, mGridPadding + 40, mGridPadding,
				mGridPadding);
		this.setHorizontalSpacing(mThumbPadding);
		this.setVerticalSpacing(mThumbPadding);
		this.setAdapter(mAdapter);
		this.setOnItemClickListener(this);

		// Start DB and Data Loader
		mDb = new Sqlite(this.getContext());
		mDb.openRead();

		// Handles the CursorLoader
		getLoaderManager().initLoader(0, null, this);
	}

	public void dispose() {
		if (mDb != null) {
			mDb.close();
			mDb = null;
		}
	}

	/*
	 * Getter & Setters
	 */
	public int getFilterMode() {
		return mFilterMode;
	}

	public void setFilterMode(int i) {
		mFilterMode = i;
	}

	public String getSeriesFilter() {
		return mSeriesFilter;
	}

	public void setSeriesFilter(String str) {
		mSeriesFilter = (str == null) ? "" : str;
	}

	/*
	 * misc
	 */
	public boolean isSeriesFiltered() {
		return (mFilterMode == 1);
	}

	@Override
	// ComicCover.onClick
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		AdapterItemRef itmRef = (AdapterItemRef) view.getTag();

		// if series is selected but not filtered yet.
		if (isSeriesFiltered() && mSeriesFilter.isEmpty()) {
			mSeriesFilter = itmRef.series;
			refreshData();
		} else { // Open comic in viewer.
			Intent intent = new Intent(this.getContext(), ViewActivity.class);
			intent.putExtra("comicid", itmRef.id);
			((FragmentActivity) this.getContext()).startActivityForResult(
					intent, 0);
		}
	}

	/*
	 * Cursor Loader : LoaderManager.LoaderCallbacks<Cursor>
	 */
	public void refreshData() {
		if (!mIsFirstRun) {
			if (mDb == null)
				mDb = new Sqlite(this.getContext());
			if (!mDb.isOpen())
				mDb.openRead();

			getLoaderManager().restartLoader(0, null, this);
		} else
			mIsFirstRun = false;
	}

	private LoaderManager getLoaderManager() {
		return ((FragmentActivity) this.getContext()).getSupportLoaderManager();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg) {
		String sql = "";

		if (isSeriesFiltered()) {// Filter by series
			if (mSeriesFilter.isEmpty()) {
				sql = "SELECT min(comicID) [_id],series [title],sum(pgCount) [pgCount],sum(pgRead) [pgRead],min(isCoverExists) [isCoverExists],count(comicID) [cntIssue] FROM ComicLibrary GROUP BY series ORDER BY series";
			} else {
				sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists FROM ComicLibrary WHERE series = '"
						+ mSeriesFilter.replace("'", "''") + "' ORDER BY title";
			}
		} else { // Filter by reading progress.
			sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists FROM ComicLibrary";
			switch (mFilterMode) {
			case 2:
				sql += " WHERE pgRead=0";
				break; // Unread;
			case 3:
				sql += " WHERE pgRead > 0 AND pgRead < pgCount-1";
				break;// Progress
			case 4:
				sql += " WHERE pgRead >= pgCount-1";
				break;// Read
			}
			sql += " ORDER BY title";
		}
		SqlCursorLoader cursorLoader = new SqlCursorLoader(this.getContext(),
				mDb);
		cursorLoader.setRaw(sql);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(
			android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
		this.recordCount = cursor.getCount();
		mAdapter.changeCursor(cursor, true);

		if (this.getContext() instanceof iCallback)
			((iCallback) this.getContext()).onDataRefreshComplete();
	}

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	/*
	 * Adapter Events
	 */
	public class AdapterItemRef {
		public String id = "";
		public TextView lblTitle;
		public ImageView imgCover;
		public ProgressCircle pcProgress;
		public Bitmap bitmap = null;
		public String series = "";
	}

	@Override
	public View onCreateListItem(View v) {
		try {
			AdapterItemRef itmRef = new AdapterItemRef();
			itmRef.lblTitle = (TextView) v.findViewById(R.id.lblTitle);
			itmRef.pcProgress = (ProgressCircle) v
					.findViewById(R.id.pcProgress);
			itmRef.imgCover = (ImageView) v.findViewById(R.id.imgCover);
			itmRef.imgCover.setTag(itmRef);
			itmRef.imgCover.getLayoutParams().height = mThumbHeight;
			v.setTag(itmRef);
		} catch (Exception e) {
			System.out.println("onCreateListItem " + e.getMessage());
		}
		return v;
	}

	@Override
	public void onBindListItem(View v, Cursor c) {
		try {
			AdapterItemRef itmRef = (AdapterItemRef) v.getTag();

			String id = c.getString(mAdapter.getColIndex("_id")), tmp = c
					.getString(mAdapter.getColIndex("title"));

			// Grid view binds more then one time, limit double loading to save
			// on resources used to load images.
			// Need to change ID to uniqueness, but also check title because
			// there is a small bind issue going back/forth between series view
			if (itmRef.id.equals(tmp) && itmRef.lblTitle.getText().equals(tmp)) {
				System.out.println("Repeat");
				return;
			}
			itmRef.id = id;

			if (isSeriesFiltered() && mSeriesFilter.isEmpty()) {
				itmRef.series = tmp;
				tmp += " (" + c.getString(mAdapter.getColIndex("cntIssue"))
						+ ")";
			} else
				itmRef.series = "";
			itmRef.lblTitle.setText(tmp);

			// load Cover Image
			if (c.getString(mAdapter.getColIndex("isCoverExists")).equals("1")) {
				LoadImageView.loadImage(mThumbPath + itmRef.id + ".jpg",
						itmRef.imgCover, this);
			} else {
				// No image, clear out images TODO put a default image for
				// missing covers.
				itmRef.imgCover.setImageBitmap(null);
				if (itmRef.bitmap != null) {
					itmRef.bitmap.recycle();
					itmRef.bitmap = null;
				}
			}

			// display reading progress
			float progress = 0f;
			int pTotal = c.getInt(mAdapter.getColIndex("pgCount"));
			if (pTotal > 0) {
				float pRead = c.getFloat(mAdapter.getColIndex("pgRead"));
				progress = (pRead / ((float) pTotal));
			}

			itmRef.pcProgress.setProgress(progress);
		} catch (Exception e) {
			System.out.println("onBindListItem " + e.getMessage());
		}
	}

	/*
	 * Image Loading
	 */
	@Override
	public void onImageLoaded(boolean isSuccess, Bitmap bmp, View view) {
		if (view == null)
			return;
		ImageView iv = (ImageView) view;

		if (!isSuccess)
			// release reference, if cover didn't load show that it didn't.
			iv.setImageBitmap(null);

		AdapterItemRef itmRef = (AdapterItemRef) iv.getTag();
		if (itmRef.bitmap != null) {
			itmRef.bitmap.recycle();
			itmRef.bitmap = null;
		}

		// keeping reference to make sure to clear it out when its not needed
		itmRef.bitmap = bmp;
	}

	/*
	 * Context Menu
	 */
	public void createContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		AdapterItemRef ref = (AdapterItemRef) info.targetView.getTag();

		menu.setHeaderTitle(ref.lblTitle.getText().toString());
		menu.add(0, 2, 0, "Delete");
		menu.add(0, 1, 1, "Reset Progress");
		menu.add(0, 3, 2, "Mark as Read");
		menu.add(0, 4, 3, "Edit Series Name");
	}

	public boolean contextItemSelected(MenuItem item) {
		int itmID = item.getItemId();

		if (isSeriesFiltered() && mSeriesFilter.isEmpty() && itmID == 2) {
			Toast.makeText(this.getContext(),
					"Can not perform operation on series.", Toast.LENGTH_SHORT)
					.show();
			return false;
		}

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		final AdapterItemRef ref = (AdapterItemRef) info.targetView.getTag();
		final String comicID = ref.id;
		final String seriesName = ref.series;
		final Context context = this.getContext();
		AlertDialog.Builder abBuilder;

		switch (itmID) {
		case 2:// DELETE
			abBuilder = new AlertDialog.Builder(this.getContext());
			abBuilder.setTitle("Delete Comic : "
					+ ref.lblTitle.getText().toString());
			abBuilder
					.setMessage("You are able to remove the selected comic from the library or from the device competely.");
			abBuilder.setCancelable(true);
			abBuilder.setNegativeButton("Cancel", null);
			abBuilder.setPositiveButton("Remove from library",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ComicLibrary.removeComic(context, comicID, false);
							refreshData();
						}
					});
			abBuilder.setNeutralButton("Remove from device",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ComicLibrary.removeComic(context, comicID, true);
							refreshData();
						}
					});
			abBuilder.show();
			break;
		case 1:// Reset Progress
			abBuilder = new AlertDialog.Builder(this.getContext());
			abBuilder.setTitle("Reset Progress : "
					+ ref.lblTitle.getText().toString());
			abBuilder
					.setMessage("Are you sure you want to reset the reading progress?");
			abBuilder.setCancelable(true);
			abBuilder.setNegativeButton("Cancel", null);
			abBuilder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							boolean applySeries = (isSeriesFiltered() && mSeriesFilter
									.isEmpty());
							ComicLibrary.setComicProgress(context, comicID, 0,
									applySeries);
							refreshData();
						}
					});
			abBuilder.show();
			break;
		case 3:// Mark as Read
			abBuilder = new AlertDialog.Builder(this.getContext());
			abBuilder.setTitle("Mark as Read : "
					+ ref.lblTitle.getText().toString());
			abBuilder
					.setMessage("Are you sure you want to change the reading progress?");
			abBuilder.setCancelable(true);
			abBuilder.setNegativeButton("Cancel", null);
			abBuilder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							boolean applySeries = (isSeriesFiltered() && mSeriesFilter
									.isEmpty());
							ComicLibrary.setComicProgress(context, comicID, 1,
									applySeries);
							refreshData();
						}
					});
			abBuilder.show();
			break;
		case 4:// Edit Serial
			String sSeries = "";
			if (seriesName == null || seriesName.isEmpty()) {
				sSeries = Sqlite.scalar(getContext(),
						"SELECT Series FROM ComicLibrary WHERE comicID = ?",
						new String[] { comicID });
			} else
				sSeries = seriesName;

			sage.ui.InputDialog inDialog = new sage.ui.InputDialog(
					this.getContext(), "Edit Series : "
							+ ref.lblTitle.getText().toString(), null, sSeries) {
				@Override
				public boolean onOk(String txt) {
					if (seriesName == txt)
						return true;

					if (seriesName != null && !seriesName.isEmpty())
						ComicLibrary
								.renameSeries(getContext(), seriesName, txt);
					else
						ComicLibrary.setSeriesName(getContext(), comicID, txt);

					refreshData();
					return true;
				}
			};

			inDialog.show();
			break;
		}

		return true;
	}
}
