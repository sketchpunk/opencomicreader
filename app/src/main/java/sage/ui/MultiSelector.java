package sage.ui;

import java.util.ArrayList;

public class MultiSelector<T>{
	private boolean mIsActive = false;
	private ArrayList<T> mItmList = null;

	public MultiSelector(){
		mItmList = new ArrayList<T>();
	}//func

	public void enable(){ mIsActive = true; mItmList.clear(); }
	public void disable(){ mIsActive = false; }
	public boolean isActive(){ return mIsActive; }

	public void select(T itm){
		if(isSelected(itm)) return;
		mItmList.add(itm);
	}//func

	public void unselect(T itm){
		int i = mItmList.indexOf(itm);
		if(i >= 0) mItmList.remove(i);
	}//func

	public boolean toggle(T itm){
		int i = mItmList.indexOf(itm);

		if(i >= 0){ //Is Selected
			mItmList.remove(i);
			return false;
		}//if

		mItmList.add(itm);
		return true;
	}//func

	public int count(){ return mItmList.size(); }

	public void clear(){ mItmList.clear(); }

	public boolean isSelected(T itm){
		if(!mIsActive) return false;
		return mItmList.indexOf(itm) >= 0;
	}//if

	public boolean getArray(T[] ary){
		if(mItmList.size() == 0) return false;
		mItmList.toArray(ary);
		return true;
	}//func
}//cls