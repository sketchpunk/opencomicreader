package sage.data;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

public class SqlCursorLoader extends AsyncTaskLoader<Cursor>{
	//region Interfaces, Variables and Constructors

	public static interface Callback{
		void onSqlCursorLoaderTask(Cursor cursor,Sqlite db);
	}//interface

	private final ForceLoadContentObserver mObserver;
	private String mRawSql;
	private Sqlite mDb;
	private Cursor mCursor;
	private Callback mCallback = null;

	public SqlCursorLoader(Context context,Sqlite db){
		super(context);
		mDb = db;
		mObserver = new ForceLoadContentObserver();
	}//func
	//endregion

	//region setters
	public void setRaw(String sql){ mRawSql = sql; }

	public void setCallback(Callback cb){ mCallback = cb; }
	//endregion

	//region thread
	@Override public Cursor loadInBackground(){
		Cursor cursor = null;
		try{
			if(!mDb.isOpen()) mDb.openRead();
			cursor = mDb.raw(mRawSql,null);

			if(cursor != null){
				cursor.registerContentObserver(mObserver); //setup observer for a signal from the db to when the cursor needs updating.
			}//if

			if(mCallback != null){
				mCallback.onSqlCursorLoaderTask(cursor,mDb);
				mCallback = null;
			}//if
		}catch(Exception e){
			System.out.println("cursorLoader error : " + e.getMessage() );
		}//try

		return cursor;
	}//func
	//endregion

	//region observer
	//Called when there is new data to be delivered to the client, runs on the UI Thread
	@Override public void deliverResult(Cursor cursor){
		if(isReset()){
			//An async query came in while the loader is stopped;
			if(cursor != null) cursor.close();
			return;
		}//if

		Cursor oldCursor = mCursor;
		mCursor = cursor;

		if(isStarted()) super.deliverResult(cursor); //Send the data
		if(oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) oldCursor.close(); //Clean up the old cursor.
	}//func
	//endregion

	//region events
	//Start the thread to load in data from the DB.
	@Override protected void onStartLoading(){
		if(mCursor != null) deliverResult(mCursor); //If the cursor already exists, Send it back.
		if(takeContentChanged() || mCursor == null) forceLoad(); // data has changed or does not exist, start up a thread.
	}//func

	@Override protected void onStopLoading(){
		cancelLoad(); //try to cancel the current task
	}//func

	//Handle a cancel request.
	@Override public void onCanceled(Cursor cursor){
		if(cursor != null && !cursor.isClosed()){
			cursor.close();
			cursor.unregisterContentObserver(mObserver);
		}//if
	}//func

	@Override protected void onReset(){
		super.onReset();
		onStopLoading(); //Make sure the loader stop

		if(mCursor != null){
			if(!mCursor.isClosed()) mCursor.close();
			mCursor.unregisterContentObserver(mObserver);
		}//if

		mCursor = null;
	}//func
	//endregion
}

