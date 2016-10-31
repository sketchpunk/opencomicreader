package sage.adapter;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sketchpunk.ocomicreader.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import sage.SageApp;
import sage.data.ResourceUtil;
import sage.data.ViewBindHolder;

public class RecyclerMapAdapter<K extends Comparable<K>,V> extends RecyclerView.Adapter<ViewBindHolder>{

	//region Vars, iCallback and Constructor
	private Map<K,V> mMap = null;
	private ArrayList<K> mKeys = null;
	private int mItemLayout = 0;
	private Callback mCallBack = null;
	private LayoutInflater mInflater = null;

	public static interface Callback{
		public ViewBindHolder onCreateViewHolder(View v);
	}//interface

	public RecyclerMapAdapter(Context context, int itmLayout, boolean init){
		if(init) mMap = new HashMap<K,V>();
		mItemLayout = itmLayout;
		mInflater = mInflater.from(context);
	}//func
	//endregion

	//region setter/getter
	public void put(K key, V val){
		mMap.put(key,val);
		mKeys.clear();
		mKeys = new ArrayList<K>(mMap.keySet());
	}
	public V get(K key){ return mMap.get(key); }
	public V getByPos(int pos){ return mMap.get(mKeys.get(pos)); }
	public K getKey(int pos){ return mKeys.get(pos);}
	public void setCallback(Callback cb){ mCallBack = cb; }
	public void setMap(Map<K,V> map){
		mMap = map;
		mKeys = new ArrayList<K>(map.keySet());
	}
	//endregion

	//region Adapter
	@Override
	public ViewBindHolder onCreateViewHolder(ViewGroup parent, int viewType){
		if(mCallBack == null) return null;

		View view = mInflater.inflate(mItemLayout,parent,false);
		return mCallBack.onCreateViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewBindHolder holder, int position){
		holder.bindData(position);
	}

	@Override public int getItemCount(){ return mMap.size(); }
	//endregion
}
