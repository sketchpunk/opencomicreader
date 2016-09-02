package sage.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MapAdapter<K extends Comparable<K>,V> extends BaseAdapter{
	public static interface AdapterCallback {
		public View onDisplayListItem(View v,Object key);
	}//interface

	/*========================================================
	*/
	private LayoutInflater mInflater;
	private List<K> mKeys; //used for position int of Map data
	private Map<K,V> mMap;
	private AdapterCallback mCallback;
	private int mListItemLayout;
	private boolean mSortEnabled = false;


	/*========================================================
	*/
	//public MapAdapter(){}//func
	
	//public MapAdapter(Map<K,V> map){
	//	mMap = map;
	//	mKeys = new ArrayList<K>(map.keySet());
	//}//try

	public MapAdapter(Context context){
		mInflater = LayoutInflater.from(context); //Going to be using this alot, just safe ref
		if(context instanceof AdapterCallback) mCallback = (AdapterCallback) context;
	}//func
	

	/*========================================================
	Setters*/	
	public void setData(Map<K,V> map){
		mMap = map;
		if(map != null){
			mKeys = new ArrayList<K>(map.keySet());
			if(mSortEnabled) Collections.sort(mKeys);
		}else{
			if(mKeys != null){
				mKeys.clear();
				mKeys = null;
			}//if
		}//if
	}//func

	public void setListItemLayout(int i){
		this.mListItemLayout = i;
	}//func

	public void setCallback(AdapterCallback callback){
		mCallback = callback;
	}//func

	public void setSortEnabled(boolean isOn){
		mSortEnabled = isOn;
	}//func
	

	/*========================================================
	Methods*/
	public void refresh(){
		if(mMap != null){
			mKeys = new ArrayList<K>(mMap.keySet());
			if(mSortEnabled) Collections.sort(mKeys);
		}else{
			if(mKeys != null){
				mKeys.clear();
				mKeys = null;
			}//if
		}//if
		this.notifyDataSetChanged();
	}//func
	
	@Override
	public int getCount(){
		if(mMap != null) return mMap.size(); 
		return 0;
	}//func

	
	/*========================================================
	Rendering*/
	@Override
	public Object getItem(int position) {
		if(mMap == null) return null;
		return mMap.get(mKeys.get(position));
	}//func

	@Override
	public long getItemId(int position) {
		return position;
	}//func
	
	@Override
	public View getView(int position, View view, ViewGroup parent){
		if(view == null) view = mInflater.inflate(mListItemLayout,parent,false);
		if(mCallback != null && mKeys != null) view = mCallback.onDisplayListItem(view,mKeys.get(position));
		return view;
	}//func
}//cls
