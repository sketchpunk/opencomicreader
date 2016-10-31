package com.sketchpunk.ocomicreader.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sketchpunk.ocomicreader.R;
import com.sketchpunk.ocomicreader.ViewActivity;
import com.sketchpunk.ocomicreader.data.MainDB;
import com.sketchpunk.ocomicreader.lib.ComicLibrary;
import com.sketchpunk.ocomicreader.lib.Settings;

import sage.adapter.RecyclerCursorAdapter;
import sage.data.SqlCursorLoader;
import sage.data.Sqlite;
import sage.data.ViewBindHolder;
import sage.loader.LoadImageView;
import sage.ui.MultiSelector;
import sage.ui.ProgressCircle;
import sage.ui.RecyclerSpacingItemDecoration;
import sage.ui.RecyclerViewFragment;

/*TODO

mCrimes.remove(0);
    // Notify the adapter that it was removed
    mRecyclerView.getAdapter().notifyItemRemoved(0);

 */

public class LibraryFragment extends RecyclerViewFragment implements LoaderManager.LoaderCallbacks<Cursor>{
	public static interface Callback{
		void onSelectionUpdate(int cnt);
		void onItemLongPress();
		void onFilterChange(int filterMode,String series);
	}

	//region vars,props,constructor
	private Sqlite mDb = null;
	private RecyclerCursorAdapter mAdapter = null;
	private MultiSelector<String> mSelector = null;
	private Callback mCallback = null;

	private int mFilterMode = 0;
	private String mSeriesFilter = "";
	private String mThumbPath = "";

	private int mThumbHeight = 600;
	private int mGridPadding = 0;
	private int mGridColNum = 2;

	public LibraryFragment(){}
	//endregion

	//region fragment Events
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		//Get Preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		try{
			mGridColNum =		prefs.getInt("libColCnt",2);
			mGridPadding =		prefs.getInt("libPadding",0);
			//TODO This setting is no longer needed. mThumbPadding =		prefs.getInt("libCoverPad",3);
			mThumbHeight =		prefs.getInt("libCoverHeight",800);
		}catch(Exception e){ System.err.println("Error Loading Library Prefs " + e.getMessage()); }

		mLayoutManager = new GridLayoutManager(this.getActivity(),mGridColNum); //Override the default layout manager for recycler.
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
		View v = super.onCreateView(inflater,container,savedInstanceState);

		mThumbPath = Settings.AppFld("thumbs");

		mAdapter = new RecyclerCursorAdapter(this.getActivity(), R.layout.listitem_library);//android.R.layout.simple_list_item_1
		mAdapter.setCallback(new RecyclerCursorAdapter.Callback(){
			@Override public ViewBindHolder onCreateViewHolder(View v){ return (ViewBindHolder)new VHolder(v); }
		});

		mSelector = new MultiSelector<String>();

		setRecyclerAdapter(mAdapter);
		addItemDecoration(new RecyclerSpacingItemDecoration(mGridColNum,mGridPadding));
		//addItemDecoration(new RecyclerDivider());
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);

		//Start up DB Connection.
		mDb = new Sqlite(MainDB.get());
		mDb.openRead();

		getLoaderManager().initLoader(0,null,this); //Assign this fragment as the loader callback.
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		//try{
		//	if(activity instanceof Callback) mCallback = (Callback) activity;
		//}catch(ClassCastException e){
		//	throw new ClassCastException(activity.toString() + " must implement Callback");
		//}//try
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mCallback = null;
	}
	//endregion

	//region data loader
	public void refreshData(){
		if(mDb == null) mDb = new Sqlite(MainDB.get());
		if(!mDb.isOpen()) mDb.openRead();

		getLoaderManager().restartLoader(0,null,this);
	}//func

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args){
		String sql = ComicLibrary.getListSql(mFilterMode,mSeriesFilter,-1);
		SqlCursorLoader cursorLoader = new SqlCursorLoader(this.getActivity(),mDb);
		cursorLoader.setRaw(sql);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor){
		if(cursor != null){
			mAdapter.changeCursor(cursor);
			if(cursor.getCount() == 0) showTextView("No items to display");
			else hideTextView();
		}else showTextView("Null Cursor");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader){ mAdapter.changeCursor(null); }
	//endregion

	//region Methods
	public int isSeriesViewActive(){
		int rtn = 0;
		if(mFilterMode == 1) rtn += 1;
		if(!mSeriesFilter.isEmpty()) rtn += 2;
		return rtn;
	}
	public String getSeriesFilter(){ return mSeriesFilter; }
	public void setSeriesFilter(String str, boolean refresh){
		mSeriesFilter = (str == null)?"":str;
		if(refresh) refreshData();

		if(mCallback != null) mCallback.onFilterChange(mFilterMode,mSeriesFilter);
	}

	public int getFilterMode(){ return mFilterMode; }
	public void setFilterMode(int v, boolean refresh){
		mFilterMode = v;
		mSeriesFilter = "";
		if(refresh) refreshData();
		if(mCallback != null) mCallback.onFilterChange(mFilterMode,mSeriesFilter);
	}

	public void setCallback(Callback cb){ mCallback = cb; }

	public int getSelectedCount(){ return mSelector.count(); }
	public String[] getSelectedItems(){
		int cnt = mSelector.count();
		if(cnt == 0) return null;

		String[] ary = new String[cnt];
		mSelector.getArray(ary);
		return ary;
	}//func

	public void closeSelection(){
		mSelector.disable();
		mSelector.clear();
		mAdapter.notifyDataSetChanged();
	}//func
	//endregion

	//---------------------------------------------------------------
	public class VHolder extends ViewBindHolder implements View.OnClickListener,View.OnLongClickListener{
		//region variables
		private String mID = "";
		private TextView mLblTitle;
		private ImageView mImgCover;
		private ProgressCircle mProgress;
		private Bitmap mBitmap = null;
		private String mSeries = "";
		private int mPos = 0;
		//endregion

		//region Create and Binding
		public VHolder(View v){
			super(v);

			v.setOnClickListener(this);
			v.setOnLongClickListener(this);

			try{
				mLblTitle = (TextView)v.findViewById(R.id.lblTitle);
				mProgress = (ProgressCircle)v.findViewById(R.id.pcProgress);
				mImgCover = (ImageView)v.findViewById(R.id.imgCover);
				mImgCover.getLayoutParams().height = mThumbHeight;
			}catch(Exception e){ System.out.println("VHolder " + e.getMessage()); }
		}

		@Override
		public void bindData(int pos,Cursor c){
			try{
				int cntIssues = 1;
				mPos = pos;
				mSeries = mAdapter.getString("series");
				mID = mAdapter.getItemId();

				//..............................................
				String tmp;

				if(mFilterMode == 1 && mSeriesFilter.isEmpty()){
					tmp = mSeries;
					cntIssues = Integer.parseInt(c.getString(mAdapter.getColIndex("cntIssue")));
					tmp += " ("+Integer.toString(cntIssues)+")";
				}else tmp = mAdapter.getString("title");

				mLblTitle.setText(tmp);
				this.itemView.setSelected(mSelector.isSelected(mID));

				//..............................................
				//load Cover Image
				if(mAdapter.getString("isCoverExists").equals("1")){
					LoadImageView.loadImage(mThumbPath + mID + ".jpg",mImgCover,this);
				}else{
					//No image, clear out images TODO put a default image for missing covers.
					mImgCover.setImageBitmap(null);
					if(mBitmap != null){
						mBitmap.recycle();
						mBitmap = null;
					}//if
				}//if

				//..............................................
				//display reading progress
				float progress = 0f;
				int pTotal = mAdapter.getInt("pgCount");
				if(pTotal > 0){
					float pRead = mAdapter.getFloat("pgRead");
					if(pRead > 0) pRead += cntIssues; //Page index start at 0, so if the user has already passed the first page, Add Issue count to it to be able to get 100%, else leave it so it can get 0%
					progress = (pRead / ((float)pTotal));
				}//if

				mProgress.setProgress(progress);
			}catch(Exception e){
				System.out.println("onBind " + e.getMessage());
			}//try
		}
		//endregion

		//region Events
		@Override
		public void onClick(View v){
			if(mSelector.isActive()){
				this.itemView.setSelected(mSelector.toggle(mID));
				if(mCallback != null) mCallback.onSelectionUpdate(mSelector.count());
				return;
			}

			if(mFilterMode == 1 && mSeriesFilter.isEmpty()){ //if series is selected but not filtered yet.
				mSeriesFilter = mSeries;
				setSeriesFilter(mSeries,true);
			}else{ //Open comic in viewer.
				//TODO Make this isn't a static function in the View Activity.
				Intent intent = new Intent(getActivity(),ViewActivity.class);
				intent.putExtra("comicid",mID);
				intent.putExtra("comicpos",mPos);
				intent.putExtra("filtermode",mFilterMode);
				intent.putExtra("seriesname", mSeriesFilter);
				getActivity().startActivityForResult(intent,0);
			}
		}

		@Override
		public boolean onLongClick(View v){
			if(! mSelector.isActive()){
				mSelector.enable();
				if(mCallback != null) mCallback.onItemLongPress();
			}//if

			this.itemView.setSelected(mSelector.toggle(mID));
			if(mCallback != null) mCallback.onSelectionUpdate(mSelector.count());

			return true;
		}
		//endregion
	}

}
