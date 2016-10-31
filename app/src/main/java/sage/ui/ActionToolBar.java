package sage.ui;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

public class ActionToolBar implements ActionMode.Callback{
	//region Interfaces and Variables
	public static interface ActionModeListener{
		void onActionModeClose();
		void onActionModeCreate(Menu menu);
		void onActionModePrepare(Menu menu);
		boolean onActionModeItemClick(MenuItem menuItem);
	}//cls

	public Toolbar Ref = null;

	private Spinner mSpinner = null;
	private SearchView mSearchView = null;
	private MenuItem mSearchItem = null;
	private ActionMode mActionMode = null;
	private ActionBarActivity mAct = null;
	private ArrayAdapter<String> mSpinnerAdapter = null;
	private ActionModeListener mActionListener = null;
	private int mActionMenuRes = 0;
	private Context mContext;
	//endregion

	//region Constructor
	public ActionToolBar(ActionBarActivity act, String title,int toolbarID, boolean isHomeEnabled){
		//Note : Some weird bug where there is an order of how things are set.
		mAct = act;
		Ref = (Toolbar) act.findViewById(toolbarID);

		act.setSupportActionBar(Ref);
		ActionBar bar = act.getSupportActionBar();

		bar.setHomeButtonEnabled(true); //Must be the first initial call if wanting to set NavIcon
		bar.setTitle(title); //For some reason setting title through the Toolbar Object doesn't work. Ref.setTitle(title);
		//bar.setDisplayShowTitleEnabled(false);
		if(isHomeEnabled){
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setHomeButtonEnabled(true);
		}

		//Ref.setNavigationIcon(R.drawable.fab_ic_add);
		//Ref.setLogo(R.drawable.ic_launcher);

		mContext = act;
	}//func
	//endregion

	//region Methods
	public void setTitle(String txt){ this.Ref.setTitle(txt); }
	//endregion

	//region Spinner
	//https://blog.danielbetts.net/2015/01/02/material-design-spinner-toolbar-style-fix/

	public void setupSpinner(ActionBarActivity act, List<String> list, AdapterView.OnItemSelectedListener listener){
		mSpinner = new Spinner(act);
		Ref.addView(mSpinner);
		act.getSupportActionBar().setDisplayShowTitleEnabled(false);

		//mSpinnerAdapter= new ArrayAdapter<String>(act, R.layout.actionbar_spinner_text ,list); //android.R.layout.simple_spinner_item
		//mSpinnerAdapter.setDropDownViewResource(R.layout.actionbar_spinner_itm); //android.R.layout.simple_spinner_dropdown_item
		mSpinner.setAdapter(mSpinnerAdapter);
		mSpinner.setOnItemSelectedListener(listener);
	}//func
	//endregion

	//region SearchView
	public MenuItem initSearchView(Menu menu,int srcRes,SearchView.OnQueryTextListener listener){
		//Get Searchview to add listener
		mSearchItem = menu.findItem(srcRes);
		//mSearchItem.setIcon(SvgUtil.getBitmap(mAct, 50, 50, "svg/trash.svg"));

		mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);   //API10 Fix, Original : mSearchView = (SearchView) mSearchItem.getActionView();
		if(mSearchView != null){
			mSearchView.setOnQueryTextListener(listener);
			mSearchView.setQueryHint("Search");
		}//if

		return mSearchItem;
	}//func

	public void setSearchVisibility(boolean state){ mSearchItem.setVisible(state); }
	public void collapseSearchView(){
		if(mSearchView != null) mSearchView.setIconified(true);
		//if(mSearchItem != null) MenuItemCompat.collapseActionView(mSearchItem);
		//else System.out.println("SearchItem not found, can not collapse");
	}//func

	public void closeKeyboard(){
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
	}//func
	//endregion

	//region Action Mode (CAB)
	public void startActionMode(){
		try{
			System.out.println("Starting Action Mode");
			if(mActionMode == null) mActionMode = mAct.startSupportActionMode(this);
		}catch(Exception e){
			Log.e("startActionMode",e.getMessage());
			System.err.println(e.getMessage());
		}
	}
	public void closeActionMode(){ if(mActionMode != null) mActionMode.finish(); }
	public void setActionModeMenuRes(int res){mActionMenuRes = res;}
	public void setActionModeListener(ActionModeListener listener){ mActionListener = listener; }
	public void setActionTitle(String txt){ if(mActionMode != null) mActionMode.setTitle(txt); }
	public void refreshActionMode(){ if(mActionMode != null) mActionMode.invalidate(); }

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu){
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(mActionMenuRes,menu);

		if(mActionListener != null) mActionListener.onActionModeCreate(menu);
		return true;
	}//func

	//Any time we need to update the action mode items
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu){
		if(mActionListener != null) mActionListener.onActionModePrepare(menu);
		return true;
	}//func

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem){
		if(mActionListener != null) {
			if(!mActionListener.onActionModeItemClick(menuItem)) return false;
		}//if

		mode.finish();
		return true;
	}//func

	@Override
	public void onDestroyActionMode(ActionMode mode){
		mActionMode = null;
		if(mActionListener != null) mActionListener.onActionModeClose();
	}//func
	//endregion
}//cls