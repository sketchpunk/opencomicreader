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

import java.util.Map;

import sage.adapter.RecyclerArrayListAdapter;
import sage.adapter.RecyclerCursorAdapter;
import sage.adapter.RecyclerMapAdapter;
import sage.data.ResourceUtil;
import sage.data.SqlCursorLoader;
import sage.data.Sqlite;
import sage.data.ViewBindHolder;
import sage.loader.LoadImageView;
import sage.ui.MultiSelector;
import sage.ui.ProgressCircle;
import sage.ui.RecyclerSpacingItemDecoration;
import sage.ui.RecyclerViewFragment;

public class FilterFragment extends RecyclerViewFragment{
	public static interface Callback{
		void onFilterChange(String val);
	}

	//region vars,props,constructor
	private RecyclerMapAdapter<String,String> mAdapter = null;
	private Callback mCallback = null;
	private int mSelectedIndex = 0;

	public FilterFragment(){}
	//endregion

	//region fragment Events
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
		View v = super.onCreateView(inflater,container,savedInstanceState);

		mAdapter = new RecyclerMapAdapter<String,String>(this.getActivity(),R.layout.list_item_selectable,false);
		mAdapter.setMap(ResourceUtil.getMapResource(this.getActivity(),R.xml.libraryfilter));
		mAdapter.setCallback(new RecyclerMapAdapter.Callback(){
			@Override public ViewBindHolder onCreateViewHolder(View v){ return new VHolder(v); }
		});

		setRecyclerAdapter(mAdapter);
		hideTextView();
		//addItemDecoration(new RecyclerSpacingItemDecoration(mGridColNum,mGridPadding));
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
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
		//mCallback = null;
	}
	//endregion

	//region Methods
	public void setCallback(Callback cb){ mCallback = cb; }
	public void setSelectedIndex(int i){ mSelectedIndex = i; }
	//endregion

	//---------------------------------------------------------------
	public class VHolder extends ViewBindHolder implements View.OnClickListener{
		//region variables
		private TextView mLblTitle;
		private String mKey;
		private int mPos;
		//endregion

		//region Create and Binding
		public VHolder(View v){
			super(v);
			v.setOnClickListener(this);

			try{
				mLblTitle = (TextView)v.findViewById(android.R.id.text1);
			}catch(Exception e){ System.out.println("VHolder " + e.getMessage()); }
		}

		@Override
		public void bindData(int pos){
			try{
				mPos = pos;
				mKey = mAdapter.getKey(pos);
				mLblTitle.setText(mAdapter.get(mKey));
				this.itemView.setSelected((pos == mSelectedIndex));
			}catch(Exception e){
				System.out.println("onBind " + e.getMessage());
			}//try
		}
		//endregion

		//region events
		@Override
		public void onClick(View v){
			mSelectedIndex = mPos;
			if(mCallback != null) mCallback.onFilterChange(mKey);
			mAdapter.notifyDataSetChanged();
		}
		//endregion
	}

}
