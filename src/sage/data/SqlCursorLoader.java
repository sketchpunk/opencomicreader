package sage.data;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

/**
 * Static library support version of the framework's
 * {@link android.content.CursorLoader}. Used to write apps that run on
 * platforms prior to Android 3.0. When running on Android 3.0 or above, this
 * implementation is still used; it does not try to switch to the framework's
 * implementation. See the framework SDK documentation for a class overview.
 * 
 * Sketchpunk: Modified CursorLoader to use SqliteDatabase instead of a Content
 * Provider
 */
public class SqlCursorLoader extends AsyncTaskLoader<Cursor> {
	final ForceLoadContentObserver mObserver;
	private String mRawSql;
	Cursor mCursor;
	private Sqlite mDb;

	public SqlCursorLoader(Context context, Sqlite db) {
		super(context);
		mDb = db;
		mObserver = new ForceLoadContentObserver();
	}

	public void setRaw(String sSql) {
		mRawSql = sSql;
	}

	/*
	 * Thread
	 */
	@Override
	public Cursor loadInBackground() {
		Cursor cursor = null;

		try {
			if (!mDb.isOpen())
				mDb.openRead();
			cursor = mDb.raw(mRawSql, null);

			if (cursor != null)
				registerContentObserver(cursor, mObserver);
		} catch (Exception e) {
			System.out.println("cursorLoader error : " + e.getMessage());
		}

		return cursor;
	}

	/*
	 * Registers an observer to get notifications from the content provider when
	 * the cursor needs to be refreshed.
	 */
	void registerContentObserver(Cursor cursor, ContentObserver observer) {
		cursor.registerContentObserver(mObserver);
	}

	/* Runs on the UI thread */
	@Override
	public void deliverResult(Cursor cursor) {
		if (isReset()) {
			// An async query came in while the loader is stopped
			if (cursor != null)
				cursor.close();
			return;
		}

		Cursor oldCursor = mCursor;
		mCursor = cursor;

		if (isStarted())
			super.deliverResult(cursor);
		if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed())
			oldCursor.close();
	}

	/*
	 * Events
	 */

	/*
	 * Starts an asynchronous load of the contacts list data. When the result is
	 * ready the callbacks will be called on the UI thread. If a previous load
	 * has been completed and is still valid the result may be passed to the
	 * callbacks immediately. Must be called from the UI thread
	 */
	@Override
	protected void onStartLoading() {
		if (mCursor != null)
			deliverResult(mCursor);
		if (takeContentChanged() || mCursor == null)
			forceLoad();
	}

	// Must be called from the UI thread
	@Override
	protected void onStopLoading() {
		cancelLoad(); // Attempt to cancel the current load task if possible.
	}

	@Override
	public void onCanceled(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			cursor.unregisterContentObserver(mObserver);
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading(); // Ensure the loader is stopped

		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
			mCursor.unregisterContentObserver(mObserver);
		}

		mCursor = null;
	}
}