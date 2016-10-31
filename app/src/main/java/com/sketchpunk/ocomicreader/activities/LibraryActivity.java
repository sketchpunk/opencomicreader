package com.sketchpunk.ocomicreader.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sketchpunk.ocomicreader.PrefActivity;
import com.sketchpunk.ocomicreader.R;
import com.sketchpunk.ocomicreader.lib.ComicLibrary;

import sage.ui.ActionToolBar;
import sage.ui.DrawerWrapper;

public class LibraryActivity extends ActionBarActivity implements
		ActionToolBar.ActionModeListener,
		ComicLibrary.SyncCallback,
		LibraryFragment.Callback,
		FilterFragment.Callback{
	//region Properties/vars
	private LibraryFragment mFragment;
	private FilterFragment mFilterFrag;
	private DrawerWrapper mDrawer = null;
	private ProgressDialog mProgress = null;
	private ActionToolBar mToolBar = null;
	//endregion

	//region Activity events
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_library2);

		//------------------------------------------
		mToolBar = new ActionToolBar(this,"Comic Library",R.id.toolbar,true);
		mToolBar.setActionModeMenuRes(R.menu.library_actions);
		mToolBar.setActionModeListener(this);

		//------------------------------------------
		mFragment = (LibraryFragment)getSupportFragmentManager().findFragmentById(R.id.mainFragment);
		mFragment.setCallback(this);

		mFilterFrag = (FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterFragment);
		mFilterFrag.setCallback(this);

		mDrawer = new DrawerWrapper(this,R.id.drawer_layout,R.drawable.drawer_shadow,0);
		mDrawer.configToggle(mToolBar.Ref);

		//------------------------------------------
		//Load state of filter from Bundle
		if(savedInstanceState != null){
			int filterMode = savedInstanceState.getInt("mFilterMode");
			mFragment.setSeriesFilter(savedInstanceState.getString("mSeriesFilter"),false);
			mFragment.setFilterMode(filterMode,false);
			mFilterFrag.setSelectedIndex(filterMode);
		}else{//if no state, load in default pref.
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int filterMode = Integer.parseInt(prefs.getString("libraryFilter","0"));
			mFragment.setFilterMode(filterMode,false);
			mFilterFrag.setSelectedIndex(filterMode);
		}//if
	}

	@Override
	protected void onSaveInstanceState(Bundle siState){
		//Save the state of the filters so we can go back to them.
		siState.putString("mSeriesFilter",mFragment.getSeriesFilter());
		siState.putInt("mFilterMode",mFragment.getFilterMode());
		super.onSaveInstanceState(siState);
	}//func

	@Override public void onResume(){ super.onResume(); }
	@Override public void onPause(){ super.onPause(); }

	@Override
	public void onBackPressed(){
		//Override back press to make it easy to back out of series filter.
		if(mFragment.isSeriesViewActive() == 3) mFragment.setSeriesFilter("",true);
		else super.onBackPressed();
	}
	//endregion

	//region Toolbar Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.menu_settings: this.startActivityForResult( new Intent(this,PrefActivity.class) ,0); break;
			case R.id.menu_about:
				try{
					sage.ui.Dialogs.About(this,this.getText(R.string.app_about));
				}catch(Exception e){ System.err.println(e.getMessage()); }

				break;
			case R.id.menu_sync:
				sage.ui.Dialogs.ConfirmBox(this,"Sync Library","Are you sure you want sync the library?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){startSync();}
				});
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	//endregion

	//region ActionBar Action Mode
	@Override public void onActionModeClose(){ mFragment.closeSelection(); }
	@Override public void onActionModeCreate(Menu menu){}
	@Override public void onActionModePrepare(Menu menu){
		boolean multi = false, single = false;
		MenuItem mnuEditSeries = menu.findItem(R.id.action_editSeries),
				mnuDelete = menu.findItem(R.id.action_delete),
				mnuMarkRead = menu.findItem(R.id.action_markRead),
				mnuResetProg = menu.findItem(R.id.action_resetProg);

		int cnt = mFragment.getSelectedCount();
		if(cnt == 1){ multi = true; single = true; }
		else if(cnt > 1){ multi = true; }

		mnuEditSeries.setVisible(single);
		mnuDelete.setVisible(multi);
		mnuMarkRead.setVisible(multi);
		mnuResetProg.setVisible(multi);
	}

	@Override
	public boolean onActionModeItemClick(MenuItem menuItem){
		switch(menuItem.getItemId()){
			//....................................................
			case R.id.action_delete:
				sage.ui.Dialogs.Confirm3(this,"Delete","You are able to remove the selected comic from the library or from the device competely","Remove From Device","Remove From Library","Cancel"
					,new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which){ System.out.println("From Device"); } }
					,new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which){ System.out.println("From Library"); } }
					,null
				);
						/*
				ComicLibrary.removeComic(context,comicID,false); refreshData(); }
				ComicLibrary.removeComic(context,comicID,true); refreshData(); }

				*/
				return true;
			//....................................................
			case R.id.action_editSeries:
				sage.ui.InputDialog inDialog = new sage.ui.InputDialog(this,"Edit Series : ",null,"default"){
					@Override public boolean onOk(String txt){
						System.out.println(txt);
						/*
						if(seriesName == txt) return true;

						if(seriesName != null && !seriesName.isEmpty()) ComicLibrary.renameSeries(getContext(),seriesName,txt);
						else ComicLibrary.setSeriesName(getContext(), comicID, txt);

						refreshData();*/
						return true;
					}
				};

				inDialog.show();
				return true;
			//....................................................
			case R.id.action_markRead:
				sage.ui.Dialogs.ConfirmBox(this,"Mark as Read","Are you sure you want to change the reading progress?"
					,new DialogInterface.OnClickListener(){
						@Override public void onClick(DialogInterface dialog, int which){
							boolean applySeries = (mFragment.isSeriesViewActive() == 1);
							ComicLibrary.setComicProgress(mFragment.getSelectedItems(), 1, applySeries);
							mFragment.refreshData();
							mToolBar.closeActionMode();
						}}
					,new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which){ mToolBar.closeActionMode(); }}
				);

				return false;
			//....................................................
			case R.id.action_resetProg:
				sage.ui.Dialogs.ConfirmBox(this,"Reset Progress","Are you sure you want to reset the reading progress?"
					,new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which){
						boolean applySeries = (mFragment.isSeriesViewActive() == 1);
						ComicLibrary.setComicProgress(mFragment.getSelectedItems(),0,applySeries);
						mFragment.refreshData();
						mToolBar.closeActionMode();
					}}
					,new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which){ mToolBar.closeActionMode(); }}
				);

				return false;
		}
		return false;
	}
	//endregion

	//region Library Sync
	private void startSync(){
		if(ComicLibrary.startSync(this)){
			if(mProgress != null){
				if(!mProgress.isShowing()){ mProgress.show(this,"Library Syncing","",true); return; }
			}

			mProgress = ProgressDialog.show(this,"Library Syncing","",true);
		}else Toast.makeText(this,"Sync did not start", Toast.LENGTH_SHORT).show();
	}

	public void onSyncProgress(String txt){ if(mProgress != null && mProgress.isShowing()) mProgress.setMessage(txt); }
	public void onSyncComplete(int status){
		//............................................
		try{ if(mProgress != null){ mProgress.dismiss(); mProgress = null; } }
		catch(Exception e){ Toast.makeText(this,"Error closing progress dialog",Toast.LENGTH_LONG).show(); }

		//............................................
		switch(status){
			case ComicLibrary.STATUS_COMPLETE: mFragment.refreshData(); break;
			case ComicLibrary.STATUS_NOSETTINGS: Toast.makeText(this,"No sync folders have been set. Go to settings.",Toast.LENGTH_LONG).show(); break;
		}
	}
	//endregion

	//region Library/Filter Fragment Callback
	@Override public void onItemLongPress(){ mToolBar.startActionMode(); }

	@Override
	public void onFilterChange(int filterMode, String series){
		System.out.println(filterMode);
		switch(filterMode){
			case 0: mToolBar.setTitle("Viewing All"); break;
			case 1: mToolBar.setTitle((series.isEmpty())? "Viewing Series" : series); break;
			case 2: mToolBar.setTitle("Viewing Unread"); break;
			case 3: mToolBar.setTitle("Viewing in Progress"); break;
			case 4: mToolBar.setTitle("Viewing Read"); break;
		}
	}

	@Override public void onSelectionUpdate(int cnt){
		mToolBar.setActionTitle("Items " + Integer.toString(cnt));
		mToolBar.refreshActionMode();
	}

	@Override
	public void onFilterChange(String val){
		mFragment.setFilterMode(Integer.valueOf(val),true);
		mDrawer.closeLeft();
	}
	//endregion
}
