package sage.ui;

import java.util.HashMap;
import java.util.Map;

public class MultiSelectorMap<K,V>{
	private boolean mIsActive = false;
	private Map<K,V> mItems;

	public MultiSelectorMap(){
		mItems = new HashMap<K,V>();
	}//func

	public void enable(){ mIsActive = true; mItems.clear(); }
	public void disable(){ mIsActive = false; }
	public boolean isActive(){ return mIsActive; }

	public void select(K key, V val){
		if(mItems.containsKey(key)) return;
		mItems.put(key,val);
	}//func

	public boolean toggle(K key,V val){
		if(mItems.containsKey(key)){
			mItems.remove(key);
			return false;
		}
		mItems.put(key,val);
		return true;
	}//func

	public void unselect(K key){ if(mItems.containsKey(key)) mItems.remove(key); }

	public int count(){ return mItems.size(); }

	public void clear(){ mItems.clear(); }

	public boolean isSelected(K key){
		if(!mIsActive) return false;
		return mItems.containsKey(key);
	}
}