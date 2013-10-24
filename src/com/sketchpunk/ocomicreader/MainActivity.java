package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;
import sage.data.Sqlite;
import sage.data.SqlCursorLoader;
import sage.adapter.SqlCursorAdapter;
import sage.loader.LoadImageView;
import sage.ui.ProgressCircle;

import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ActionBar.OnNavigationListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity 
	implements SqlCursorAdapter.AdapterCallback
	,LoaderManager.LoaderCallbacks<Cursor>
	,LoadImageView.OnImageLoadedListener
	,ComicLibrary.SyncCallback
	,OnItemClickListener, OnNavigationListener{

	private int mFilterMode = 0;
	private String mSeriesFilter = "";
	private SpinnerAdapter mSpinAdapter;
	
	private GridView mGridView;
	private SqlCursorAdapter mAdapter;
	private Sqlite mDb;
	private ActionBar mActionBar;
	private TextView mCountLbl;
	private TextView mSeriesLbl;
	
	private String mThumbPath;
	private ProgressDialog mProgress;
	private boolean mIsFirstRun = true;
	
	/*========================================================
	Main*/
	@Override
	public void onDestroy(){
		if(mDb != null){ mDb.close(); mDb = null; }
		
		super.onDestroy();
	}//func
	
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        
        //....................................
        //Load state of filter from Bundle
        if(savedInstanceState != null) {
        	mSeriesFilter = savedInstanceState.getString("mSeriesFilter");
        	mFilterMode = savedInstanceState.getInt("mFilterMode");
        	if(mSeriesFilter == null) mSeriesFilter = ""; // if no filter found
        }else{
        	//if no state, load in default pref.
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        	String tmp = prefs.getString("libraryFilter","0");
        	this.mFilterMode = Integer.parseInt(tmp);
        }//if
        
        //....................................
        mThumbPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/";
        mSpinAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,this.getResources().getStringArray(R.array.libraryFilter));
        
        //....................................
        //Setup Actionbar
        ActionBar mActionBar = this.getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setListNavigationCallbacks(mSpinAdapter,this);
        mActionBar.setSelectedNavigationItem(mFilterMode);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM|ActionBar.DISPLAY_USE_LOGO|ActionBar.DISPLAY_SHOW_HOME);
        mActionBar.setCustomView(R.layout.activity_main_actionbar);
        mCountLbl = (TextView)mActionBar.getCustomView().findViewById(R.id.lblCount);
        mSeriesLbl = (TextView)mActionBar.getCustomView().findViewById(R.id.lblSeries);
        
        //....................................
        //Setup Main View Area
        mAdapter = new SqlCursorAdapter(this);
        mAdapter.setItemLayout(R.layout.listitem_main);
        
        mGridView = (GridView) findViewById(R.id.lvMain);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        
        registerForContextMenu(mGridView); //Route event from Activity to View
        
        //....................................
        //Start DB and Data Loader
        mDb = new Sqlite(this);
        mDb.openRead();
        
        getSupportLoaderManager().initLoader(0,null,this);
    }//func

    @Override
    protected void onSaveInstanceState(Bundle siState){
    	//Save the state of the filters so
    	siState.putString("mSeriesFilter",mSeriesFilter);
    	siState.putInt("mFilterMode",mFilterMode);
    	super.onSaveInstanceState(siState);
    }//func 
    
    
	/*========================================================
	State*/
    @Override
    public void onPause() {
        super.onPause();
        //System.out.println("ON PAUSE");
    }//func
    
    @Override
    public void onResume() {
        super.onResume();
        System.out.println("ON RESUME");
        
        if(mDb == null) mDb = new Sqlite(this);
        if(!mDb.isOpen()) mDb.openRead();
        
        if(!mIsFirstRun) this.refreshData();
        else mIsFirstRun = false;
    }//func
    
    
    /*========================================================
	Action Bar Menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }//func

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        	//................................................
        	case R.id.menu_settings:
        		Intent intent = new Intent(this,PrefActivity.class);
        		//intent.putExtra("path",(String)view.getTag());
        		this.startActivityForResult(intent,0);
        		break;

        	//................................................
        	case R.id.menu_reset:
        		final Context oThis = this;
				sage.ui.Dialogs.ConfirmBox(this,getString(R.string.confirm_title_reset_library),getString(R.string.confirm_question_reset_library),new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){ComicLibrary.clearAll(oThis); refreshData(); }
				});
        		break;
        	
        	//................................................
        	case R.id.menu_import:
				sage.ui.Dialogs.ConfirmBox(this,getString(R.string.confirm_title_sync_library),getString(R.string.confirm_question_sync_library),new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){startSync();}
				});
        		break;
        }//switch
        return true;
    }//cls
    
	@Override
	public boolean onNavigationItemSelected(int index, long id){
		if(mFilterMode != index){//initially, refreshdata gets called twice,its a waste.
			mFilterMode = index;
			this.refreshData();
		}//if
		return false;
	}//func
    
    
    /*========================================================
	Context menu*/
    @Override
	public void onCreateContextMenu(ContextMenu menu,View v,ContextMenuInfo menuInfo){
		switch(v.getId()){
			case R.id.lvMain:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
				AdapterItemRef ref = (AdapterItemRef)info.targetView.getTag();
				
				menu.setHeaderTitle(ref.lblTitle.getText().toString());				
				menu.add(0,2,0,R.string.ma_ctxmenu_delete);
				menu.add(0,1,1,R.string.ma_ctxmenu_reset_progress);
				menu.add(0,3,2,R.string.ma_ctxmenu_mark_as_read);
			break;
		}//switch
	}//func
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
		int itmID = item.getItemId();
		
		if(isSeriesFiltered() && mSeriesFilter.isEmpty() && itmID == 2){
			Toast.makeText(this,R.string.ma_ctxmenu_warning_can_not_perform_on_series,Toast.LENGTH_SHORT).show();
			return false;
		}//if
		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final AdapterItemRef ref = (AdapterItemRef)info.targetView.getTag();
		final String comicID = ref.id;
		final Context context = this;
		AlertDialog.Builder abBuilder;
		
		switch(itmID){
			//...................................
			case 2://DELETE
				abBuilder = new AlertDialog.Builder(this);
				abBuilder.setTitle(R.string.ma_ctxmenu_delete_comic_question_title + ref.lblTitle.getText().toString());
				abBuilder.setMessage(R.string.ma_ctxmenu_delete_comic_question_text);
				abBuilder.setCancelable(true);
				abBuilder.setNegativeButton(R.string.ma_ctxmenu_option_cancel,null);
				abBuilder.setPositiveButton(R.string.ma_ctxmenu_delete_comic_question_option_remove_from_library,new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){ ComicLibrary.removeComic(context,comicID,false); refreshData(); }
				});
				abBuilder.setNeutralButton(R.string.ma_ctxmenu_delete_comic_question_option_remove_from_device,new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){ ComicLibrary.removeComic(context,comicID,true); refreshData(); }
				});
				abBuilder.show();
				break;
			//...................................
			case 1://Reset Progress
				abBuilder = new AlertDialog.Builder(this);
				abBuilder.setTitle(R.string.ma_ctxmenu_reset_progress_question_title + ref.lblTitle.getText().toString());
				abBuilder.setMessage(R.string.ma_ctxmenu_reset_progress_question_text);
				abBuilder.setCancelable(true);
				abBuilder.setNegativeButton(R.string.ma_ctxmenu_option_cancel,null);
				abBuilder.setPositiveButton(R.string.ma_ctxmenu_option_ok,new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){
						boolean applySeries = (isSeriesFiltered() && mSeriesFilter.isEmpty());
						ComicLibrary.setComicProgress(context,comicID,0,applySeries);
						refreshData();
					}
				});
				abBuilder.show();
				break;
			//...................................
			case 3://Mark as Read
				abBuilder = new AlertDialog.Builder(this);
				abBuilder.setTitle(R.string.ma_ctxmenu_mark_read_question_title + ref.lblTitle.getText().toString());
				abBuilder.setMessage(R.string.ma_ctxmenu_mark_read_question_text);
				abBuilder.setCancelable(true);
				abBuilder.setNegativeButton(R.string.ma_ctxmenu_option_cancel,null);
				abBuilder.setPositiveButton(R.string.ma_ctxmenu_option_ok,new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){
						boolean applySeries = (isSeriesFiltered() && mSeriesFilter.isEmpty());
						ComicLibrary.setComicProgress(context,comicID,1,applySeries);
						refreshData();
					}
				});
				abBuilder.show();
				break;
		}//switch

		return true;
	}//func

	
    /*========================================================
	UI Events*/
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
		AdapterItemRef itmRef = (AdapterItemRef)view.getTag();
		
		if(isSeriesFiltered() && mSeriesFilter.isEmpty()){ //if series if selected but not filtered yet.
			mSeriesFilter = itmRef.series;
			refreshData();
		}else{ //Open comic in viewer.
			Intent intent = new Intent(this,ViewActivity.class);
			intent.putExtra("comicid",itmRef.id);
			this.startActivityForResult(intent,0);
		}//if
	}//func
	
	@Override
	public void onBackPressed(){
		//Override back press to make it easy to back out of series filter.
		if(isSeriesFiltered() && !mSeriesFilter.isEmpty()){
			mSeriesFilter = "";
			refreshData();
		}else{
			super.onBackPressed();
		}//if
	}//func


    /*========================================================
	Cursor Loader*/
	private void refreshData(){ if(!mIsFirstRun) getSupportLoaderManager().restartLoader(0,null,this); }//func
	
    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg){
    	String sql = "";
    	if(mSeriesLbl.getVisibility() != View.GONE) mSeriesLbl.setVisibility(View.GONE);
    	
    	if(isSeriesFiltered()){//Filter by series
    		if(mSeriesFilter.isEmpty()){
    			sql = "SELECT min(comicID) [_id],series [title],sum(pgCount) [pgCount],sum(pgRead) [pgRead],min(isCoverExists) [isCoverExists],count(comicID) [cntIssue] FROM ComicLibrary GROUP BY series ORDER BY series";
    		}else{
    			mSeriesLbl.setText("Series > " + mSeriesFilter);
    			mSeriesLbl.setVisibility(View.VISIBLE);
    			sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists FROM ComicLibrary WHERE series = '"+mSeriesFilter.replace("'", "''")+"' ORDER BY title";
    		}//if
    	}else{ //Filter by reading progress.
    		String condWhere = "";
    		switch(mFilterMode){
    			case 2: condWhere = "WHERE pgRead=0"; break; //Unread;
    			case 3: condWhere = "WHERE pgRead > 0 AND pgRead < pgCount-1"; break;//Progress
    			case 4: condWhere = "WHERE pgRead >= pgCount-1"; break;//Read
    		}//switch
    		sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists FROM ComicLibrary "+condWhere+" ORDER BY title";
    	}//if
   
    	//............................................
    	SqlCursorLoader cursorLoader = new SqlCursorLoader(this,mDb);
    	cursorLoader.setRaw(sql);
    	return cursorLoader;
	}//func
    
	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor){
		mCountLbl.setText(Integer.toString(cursor.getCount()));
		mAdapter.changeCursor(cursor,true);
	}//func

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}//func
	
	
    /*========================================================
	Adapter Events*/
	protected class AdapterItemRef{
    	String id;
    	TextView lblTitle;
    	ImageView imgCover;
    	ProgressCircle pcProgress;
    	Bitmap bitmap = null;
    	String series = "";
    }//cls
	
	@Override
	public View onCreateListItem(View v){
    	try{
    		AdapterItemRef itmRef = new AdapterItemRef();
    		itmRef.lblTitle = (TextView)v.findViewById(R.id.lblTitle);
    		itmRef.pcProgress = (ProgressCircle)v.findViewById(R.id.pcProgress);
    		itmRef.imgCover = (ImageView)v.findViewById(R.id.imgCover);
    		itmRef.imgCover.setTag(itmRef);

			v.setTag(itmRef);
    	}catch(Exception e){
    		System.out.println("onCreateListItem " + e.getMessage());
    	}//try
		return v;
	}//func

	@Override
	public void onBindListItem(View v,Cursor c){
		try{
			AdapterItemRef itmRef = (AdapterItemRef)v.getTag();
			
			//..............................................
			String tmp = c.getString(mAdapter.getColIndex("title"));
			if(isSeriesFiltered() && mSeriesFilter.isEmpty()){
				itmRef.series = tmp;
				tmp += " ("+c.getString(mAdapter.getColIndex("cntIssue"))+")";
			}else itmRef.series = "";
			itmRef.lblTitle.setText(tmp);
			
			itmRef.id = c.getString(mAdapter.getColIndex("_id"));

			//..............................................
			//load Cover Image
			if(c.getString(mAdapter.getColIndex("isCoverExists")).equals("1")){
				LoadImageView.loadImage(mThumbPath + itmRef.id + ".jpg",itmRef.imgCover,this);
			}//if
			
			//..............................................
			//display reading progress
			float progress = 0f;
			int pTotal = c.getInt(mAdapter.getColIndex("pgCount"));
			if(pTotal > 0){
				float pRead = c.getFloat(mAdapter.getColIndex("pgRead"));
				progress = (pRead / ((float)pTotal));
			}//if
			
			itmRef.pcProgress.setProgress(progress);
    	}catch(Exception e){
    		System.out.println("onBindListItem " + e.getMessage());
    	}//try
	}//func

	
    /*========================================================
	Image Loading*/
	@Override
	public void onImageLoaded(boolean isSuccess,Bitmap bmp,View view){
		if(view == null) return;
		ImageView iv = (ImageView)view;
		
		if(!isSuccess) iv.setImageBitmap(null); //release reference, if cover didn't load show that it didn't.
		
		AdapterItemRef itmRef = (AdapterItemRef)iv.getTag();
		if(itmRef.bitmap != null){
			itmRef.bitmap.recycle();
			itmRef.bitmap = null;
		}//if
		
		itmRef.bitmap = bmp; //keeping reference to make sure to clear it out when its not needed
	}//func

	
    /*========================================================
	Sync Library*/
	private void startSync(){
		if(ComicLibrary.startSync(this)){
			if(mProgress != null){
				if(!mProgress.isShowing()){
					mProgress = ProgressDialog.show(this,getString(R.string.dialog_title_library_syncing),"",true);
					return;
				}//if
			}//if

			mProgress = ProgressDialog.show(this,getString(R.string.dialog_title_library_syncing),"",true);
		}else{
			Toast.makeText(this,R.string.ma_toast_sync_did_not_start, Toast.LENGTH_SHORT).show();
		}//if
	}//func

	@Override
	public void OnSyncProgress(String txt){
		if(mProgress != null){
			if(mProgress.isShowing())mProgress.setMessage(txt);
		}//if
		
		System.out.println("PROGRESS");
	}//func
	
	@Override
	public void onSyncComplete(int status){
		//............................................
		try{
			if(mProgress != null){ mProgress.dismiss(); mProgress = null; }//if
		}catch(Exception e){
			Toast.makeText(this,R.string.ma_toast_error_closing_progress_dialog,Toast.LENGTH_LONG).show();
		}//try
		System.out.println("onSyncComplete");
		
		//............................................
		switch(status){
			case ComicLibrary.STATUS_COMPLETE: refreshData(); break;
		
			case ComicLibrary.STATUS_NOSETTINGS:
				Toast.makeText(this,R.string.ma_toast_no_sync_folders_have_been_set,Toast.LENGTH_LONG).show();
			break;
		}//switch
	}//func

	/*========================================================
	Sync Library*/
	private boolean isSeriesFiltered(){ return (mFilterMode == 1); }
}//cls
