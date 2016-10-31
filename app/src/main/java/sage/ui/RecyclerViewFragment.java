package sage.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

//http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html

public class RecyclerViewFragment extends Fragment{
	protected RecyclerView mRecyclerView = null;
	protected TextView mTextView = null;
	protected RecyclerView.LayoutManager mLayoutManager = null;
	protected FrameLayout mFrameLayout = null;


	//region Fragment Events
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		FragmentActivity act = getActivity();
		if(mLayoutManager == null) mLayoutManager = new LinearLayoutManager(act);

		FrameLayout.LayoutParams lpFrame = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
		FrameLayout.LayoutParams lpText = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
		lpText.gravity = Gravity.CENTER;

		//Main View Component
		mRecyclerView = new RecyclerView(act);
		mRecyclerView.setLayoutParams(lpFrame);
		mRecyclerView.setLayoutManager(mLayoutManager);

		//Text com to display some text when list isn't loaded or empty.
		mTextView = new TextView(act);
		mTextView.setLayoutParams(lpText);
		mTextView.setText("Loading");
		mTextView.setGravity(Gravity.CENTER);

		//Layout to hold the recycler and text components.
		mFrameLayout = new FrameLayout(act);
		mFrameLayout.setLayoutParams(lpFrame);
		mFrameLayout.addView(mRecyclerView);
		mFrameLayout.addView(mTextView);

		return mFrameLayout;
	}//func
	//endregion

	//region Manage Recycler
	public void setRecyclerAdapter(RecyclerView.Adapter adapter){ mRecyclerView.setAdapter(adapter); }//func
	public void addItemDecoration(RecyclerView.ItemDecoration itm){ mRecyclerView.addItemDecoration(itm); }
	//endregion

	//region Manage Text Area
	public void hideTextView(){ mTextView.setVisibility(View.GONE); }
	public void showTextView(){ mTextView.setVisibility(View.VISIBLE); }
	public void showTextView(String txt){ mTextView.setText(txt); mTextView.setVisibility(View.VISIBLE); }
	public void setTextMsg(String txt){ mTextView.setText(txt); }
	//endregion
}//cls
