package sage.adapter;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import sage.data.ViewBindHolder;

public class RecyclerCursorAdapter extends RecyclerView.Adapter<ViewBindHolder>{
	private LayoutInflater mInflater;
	private Context mContext;
	private int mItemLayout = 0;
	private HashMap<String,Integer> mCursorMap = new HashMap<String,Integer>();
	private Cursor mCursor = null;
	private Callback mCallBack = null;
	private int mRowIDCol = 0;

	public static interface Callback{
		public ViewBindHolder onCreateViewHolder(View v);
	}//interface

	public RecyclerCursorAdapter(Context context,int itmLayout){
		mInflater = LayoutInflater.from(context); //Going to be using this a lot, just save ref
		mContext = context; //Reference back to activity
		mItemLayout = itmLayout;
	}//func

	public void setCallback(Callback cb){ mCallBack = cb; }

	//region Apadter Events
	@Override
	public ViewBindHolder onCreateViewHolder(ViewGroup parent, int viewType){
		if(mCallBack == null) return null;
		View view = mInflater.inflate(mItemLayout,parent,false);

		return mCallBack.onCreateViewHolder(view);
	}//func

	@Override
	public void onBindViewHolder(ViewBindHolder holder, int position){
		if(!mCursor.moveToPosition(position)) throw new IllegalStateException("couldn't move cursor to position " + position);

		holder.bindData(position,mCursor);
	}//func
	//endregion


	private void mapCursor(Cursor c){ //Save reference to the columns in the cursor
		if(c == null) return;

		mCursorMap.clear();
		try{
			mRowIDCol = c.getColumnIndexOrThrow("_id");
			for(int i = 0; i < c.getColumnCount(); i++) mCursorMap.put(c.getColumnName(i),i);
		}catch(Exception e){
			System.out.println("error mapping cursor :" + e.getMessage());
		}//try
	}//func

	public int getColIndex(String key){
		if(mCursorMap.containsKey(key)) return mCursorMap.get(key).intValue();
		return 0;
	}//func

	public String getString(String key){
		int pos = getColIndex(key);
		return mCursor.getString(pos);
	}//func

	public int getInt(String key){
		int pos = getColIndex(key);
		return mCursor.getInt(pos);
	}//func

	public float getFloat(String key){
		int pos = getColIndex(key);
		return mCursor.getFloat(pos);
	}//func

	//region Adapter Methods
	@Override
	public int getItemCount(){ return (mCursor == null)?0 : mCursor.getCount(); }

	@Override
	public long getItemId(int pos){
		if(mCursor != null){
			return (mCursor.moveToPosition(pos))? mCursor.getLong(mRowIDCol) : 0;
		}//if
		return 0;
	}//func

	public String getItemId(){
		if(mCursor == null) return "";
		return mCursor.getString(mRowIDCol);
	}//func

	public void changeCursor(Cursor cursor){
		if(mCursor == cursor) return;

		if(mCursor != null && !mCursor.isClosed()) mCursor.close();

		mCursor = cursor;
		mapCursor(mCursor);

		notifyDataSetChanged();
	}//func
	//endregion

}
