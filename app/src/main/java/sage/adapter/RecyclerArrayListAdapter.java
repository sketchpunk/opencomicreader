package sage.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import sage.data.ViewBindHolder;

/*
adapter = RecyclerArrayListAdapter<String>(android.R.layout.simple_list_item_1);
adapter.setCallback(new RecyclerArrayListAdapter.Callback(){
    @Override public ViewBindHolder onCreateViewHolder(View v){ return new MyViewHolder(view); }
});
*/

public class RecyclerArrayListAdapter<T> extends RecyclerView.Adapter<ViewBindHolder>{
	//region Vars, iCallback and Constructor
	private ArrayList<T> mArray = null;
	private int mItemLayout = 0;
	private Callback mCallBack = null;
	private LayoutInflater mInflater = null;

	public static interface Callback{
		public ViewBindHolder onCreateViewHolder(View v);
	}//interface

	public RecyclerArrayListAdapter(Context context, int itmLayout){
		mArray = new ArrayList<T>();
		mItemLayout = itmLayout;
		mInflater = mInflater.from(context);
	}//func
	//endregion

	//region setter/getter
	public void add(T obj){ mArray.add(obj); }
	public T get(int pos){ return mArray.get(pos); }
	public void setCallback(Callback cb){ mCallBack = cb; }
	//endregion

	//region Adapter
	@Override
	public ViewBindHolder onCreateViewHolder(ViewGroup parent, int viewType){
		if(mCallBack == null) return null;

		View view = mInflater.inflate(mItemLayout,parent,false);
		return mCallBack.onCreateViewHolder(view);
	}//func

	@Override
	public void onBindViewHolder(ViewBindHolder holder, int position){ holder.bindData(position); }

	@Override public int getItemCount(){ return mArray.size(); }
	//endregion
}