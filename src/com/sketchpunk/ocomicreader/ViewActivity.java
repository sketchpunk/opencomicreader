 package com.sketchpunk.ocomicreader;

import java.util.Map;

import sage.data.Sqlite;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;
import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class ViewActivity extends Activity implements ComicPageView.CallBack,ComicLoader.CallBack,
	DialogInterface.OnClickListener{
	private ComicPageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file
	private String mComicID = "";
	private Sqlite mDb = null;
	private Toast mToast;
	private Boolean mPref_ShowPgNum = true;
	private Boolean mPref_FlingEnabled = true;

	/*========================================================
	View Events*/
	@Override
	public void onPause(){
		super.onPause();
	}//func

	@Override
	public void onResume(){
		super.onResume();
        if(mDb == null) mDb = new Sqlite(this);
        if(!mDb.isOpen()) mDb.openRead();
	}//func
	
	@Override
	public void onConfigurationChanged(Configuration config){
		super.onConfigurationChanged(config);
		mComicLoad.refreshOrientation();
	}//func

	@Override
	public void onDestroy(){
		if(mDb != null){ mDb.close(); mDb = null; }
		super.onDestroy();
	}//func
		
    @SuppressLint("ShowToast")
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        //........................................
        //Get preferences
      	int winFlags = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	this.mPref_ShowPgNum = prefs.getBoolean("showPageNum",true);
    	this.mPref_FlingEnabled = prefs.getBoolean("flingEnabled",true);
    	
    	//Full screen, force navigation
    	if(prefs.getBoolean("fullScreen",false)){
    		winFlags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

            View rootView = getWindow().getDecorView();
            rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    	}//if
    	
    	//Keep screen on
    	if(prefs.getBoolean("keepScreenOn",true)) winFlags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

    	//Apply Flags
    	if(winFlags != 0) getWindow().addFlags(winFlags);

    	//.........................................
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    	setContentView(R.layout.activity_view);
       
        //.........................................
        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
		mToast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
       
    	//.........................................
    	int currentPage = 1;
    	String filePath = "";
    	
		Intent intent = this.getIntent();
    	Uri uri = intent.getData();
    	if(uri != null){
    		filePath = Uri.decode(uri.toString().replace("file://",""));
    	}else{
    		Bundle b = intent.getExtras(); 
            mComicID = b.getString("comicid");

            mDb = new Sqlite(this);
            mDb.openRead();
            Map<String,String> dbData = mDb.scalarRow("SELECT "+ComicLibrary.DB_COLUMN_NAME_PATH+","+ComicLibrary.DB_COLUMN_NAME_PGCURRENT+" FROM "+ComicLibrary.DB_TABLE_NAME_COMIC+" WHERE "+ComicLibrary.DB_COLUMN_NAME_COMICID+" = ?",new String[]{mComicID});
            
            filePath = dbData.get(ComicLibrary.DB_COLUMN_NAME_PATH);
            currentPage = Math.max(Integer.parseInt(dbData.get(ComicLibrary.DB_COLUMN_NAME_PGCURRENT)),1);
    	}//if
    	
        //.........................................        
        mImageView = (ComicPageView)this.findViewById(R.id.pageView);
        registerForContextMenu(mImageView);

        //.........................................
        mComicLoad = new ComicLoader(this,mImageView);
        if(mComicLoad.loadArchive(filePath)){
        	if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
        	mComicLoad.gotoPage(currentPage); //Continue where user left off. IF 0, Change to 1
        }else{
        	Toast.makeText(this,"Unable to load comic.",Toast.LENGTH_LONG).show();
        }//if
        
        //.........................................
        View root = mImageView.getRootView();
        root.setBackgroundColor(0xFF000000);
    }//func


	/*========================================================
	Menu Events*/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
    	//super.onCreateContextMenu(menu, v, menuInfo);
    	getMenuInflater().inflate(R.menu.activity_view, menu);
    	menu.setHeaderTitle("Options");
    	
    	switch(mImageView.getScaleMode()){
    		case ComicPageView.ScaleNone: menu.findItem(R.id.mnu_scalen).setChecked(true); break;
    		case ComicPageView.ScaleToHeight: menu.findItem(R.id.mnu_scaleh).setChecked(true); break;
    		case ComicPageView.ScaleToWidth: menu.findItem(R.id.mnu_scalew).setChecked(true); break;
    		case ComicPageView.ScaleAuto: menu.findItem(R.id.mnu_scalea).setChecked(true); break;
    	}//switch
    }//func
    
	@Override
	public boolean onContextItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.mnu_scaleh: mImageView.setScaleMode(ComicPageView.ScaleToHeight); break;
			case R.id.mnu_scalew: mImageView.setScaleMode(ComicPageView.ScaleToWidth); break;
			case R.id.mnu_scalen: mImageView.setScaleMode(ComicPageView.ScaleNone); break;
			case R.id.mnu_goto: sage.ui.Dialogs.NumPicker(this,"Goto Page",1,mComicLoad.getPageCount(),mComicLoad.getCurrentPage()+1,this); break;
			case R.id.mnu_exit: this.finish(); break;
		}//switch
		return true;
	}//func
	
	//this is for the goto menu option and user clicks ok.
	@Override
	public void onClick(DialogInterface dialog, int which){
		mComicLoad.gotoPage(which);
	}//func
    
    
	/*========================================================
	*/
	@Override
	public void onPageLoaded(boolean isSuccess,int currentPage){
		if(isSuccess){ //Save reading progress.
			if(mComicID != ""){
				//Make sure database is open
				if(mDb == null) mDb = new Sqlite(this);
				if(!mDb.isOpen()) mDb.openRead();
	
				//Save update
				String cp = Integer.toString(currentPage);
				String sql = "UPDATE "+ComicLibrary.DB_TABLE_NAME_COMIC+" SET "+ComicLibrary.DB_COLUMN_NAME_PGCURRENT+"="+cp+", "+ComicLibrary.DB_COLUMN_NAME_PGREAD+"=CASE WHEN "+ComicLibrary.DB_COLUMN_NAME_PGREAD+" < "+cp+" THEN "+cp+" ELSE "+ComicLibrary.DB_COLUMN_NAME_PGREAD+" END WHERE "+ComicLibrary.DB_COLUMN_NAME_COMICID+" = '" + mComicID + "'"; 
				mDb.execSql(sql,null);
			}//if

			//....................................
			//Display page number
			if(this.mPref_ShowPgNum) showToast(String.format("%d / %d",currentPage,mComicLoad.getPageCount()),0);
		}//if
	}//func

	@Override
	public void onComicPageGesture(int gestureID){
		//Check if fling is allowed.
		if(!this.mPref_FlingEnabled && (gestureID == ComicPageView.FlingRight || gestureID == ComicPageView.FlingLeft)) return;

		//Perform Gesture
		switch(gestureID){
			//.........................................
			case ComicPageView.LongPress:
				this.openContextMenu(mImageView);
			break;
			//.........................................		
			case ComicPageView.FlingRight:
			case ComicPageView.TapLeft:
				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				switch(mComicLoad.prevPage()){
					case 0:
						if(this.mPref_ShowPgNum) showToast("FIRST PAGE",1);
						break;
					case -1: showToast("Still Preloading, Try again in one second",1); break;
				}//switch
				break;
			//.........................................
			case ComicPageView.FlingLeft:
			case ComicPageView.TapRight:
				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				switch(mComicLoad.nextPage()){
					case 0: 
						if(this.mPref_ShowPgNum) showToast("LAST PAGE",1);
						break;
					case -1: showToast("Still Preloading, Try again in one second",1); break;
				}//switch
				break;
		}//switch
	}//func
	
    
	/*========================================================
	functions*/
	private void showToast(String msg,int duration){
		mToast.setText(msg);
		mToast.setDuration(duration);
		mToast.show();
	}//func
	
}//cls
