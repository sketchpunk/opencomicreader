 package com.sketchpunk.ocomicreader;

import java.util.Map;

import sage.data.Sqlite;
import sage.ui.ActivityUtil;

import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.lib.ImgTransform;
import com.sketchpunk.ocomicreader.ui.ComicPageView;
import com.sketchpunk.ocomicreader.ui.GestureImageView;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

//http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#disk-cache
//https://github.com/fhucho/simple-disk-cache/blob/master/SimpleDiskCache.java
//http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
//https://github.com/JakeWharton/DiskLruCache/tree/master/src/main/java/com/jakewharton/disklrucache

public class ViewActivity extends Activity implements
	ComicLoader.ComicLoaderListener
	,GestureImageView.OnImageGestureListener
	,DialogInterface.OnClickListener{
	
	//TODO, Some of these things need to exist in a RetainFragment.
	private GestureImageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file
	private String mComicID = "";
	private Sqlite mDb = null;
	private Toast mToast;
	private Boolean mPref_ShowPgNum = true;
	private Boolean mPref_ReadRight = true;
	private Boolean mPref_FullScreen = true;

	//------------------------------------------------------------------------
	//Activty Events		
    @SuppressLint("ShowToast")
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        //TODO Make sure scale is set from Preferences
        //........................................
        //Get preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	mPref_ShowPgNum =	prefs.getBoolean("showPageNum",true);
    	mPref_FullScreen =	prefs.getBoolean("fullScreen",true);
    	mPref_ReadRight =	prefs.getBoolean("readToRight",true);
    	int scaleMode =		Integer.parseInt(prefs.getString("scaleMode","3")); 
    	
    	//Set activity features
    	int features = 0;
    	if(mPref_FullScreen) features |= ActivityUtil.FEATURE_FULLSCREEN;
    	if(prefs.getBoolean("keepScreenOn",true)) features |= ActivityUtil.FEATURE_KEEPSCREENON;
    	if(features > 0) ActivityUtil.setFeatures(this,features);
    	
    	//Apply preferred orientation
    	int so = Integer.parseInt(prefs.getString("screenOrientation", "0"));
    	if(so != 0) ActivityUtil.setScreenOrientation(this,so);
    	
    	//.........................................
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    	setContentView(R.layout.activity_view);
       
        //.........................................
    	//setup reuseable toast
        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
		mToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
       
    	//.........................................
    	int currentPage = 0;
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
            Map<String,String> dbData = mDb.scalarRow("SELECT path,pgCurrent FROM ComicLibrary WHERE comicID = ?",new String[]{mComicID});
            
            filePath = dbData.get("path");
            currentPage = Math.max(Integer.parseInt(dbData.get("pgCurrent")),0);
    	}//if
    	
        //.........................................        
        mImageView = (GestureImageView)this.findViewById(R.id.pageView);
        mImageView.setPanState( (mPref_ReadRight)?ImgTransform.INITPAN_LEFT:ImgTransform.INITPAN_RIGHT );
        mImageView.setScaleMode(scaleMode);
        registerForContextMenu(mImageView);
        
        //.........................................
        mComicLoad = new ComicLoader(this,mImageView);
        if(mComicLoad.loadArchive(filePath)){
        	if(mPref_ShowPgNum) showToast("Loading Page...",1);
        	mComicLoad.gotoPage(currentPage); //Continue where user left off
        }else{
        	Toast.makeText(this,"Unable to load comic.",Toast.LENGTH_LONG).show();
        }//if
    }//func

	@Override
	public void onDestroy(){
		if(mDb != null){ mDb.close(); mDb = null; }
		mComicLoad.close();
		super.onDestroy();
	}//func
    
	@Override
	public void onPause(){ super.onPause(); }//func

	@Override
	public void onResume(){
		super.onResume();
        if(mDb == null) mDb = new Sqlite(this);
        if(!mDb.isOpen()) mDb.openRead();
        
        if(mPref_FullScreen) ActivityUtil.setImmersiveModeOn(this);
	}//func
	
	@Override
	public void onConfigurationChanged(Configuration config){
		super.onConfigurationChanged(config); 
		
		//The view's new values aren't known yet till it has been redrawn onto the screen.
		//So wait till its ready before appying the change event to the imageview.
	    //ViewTreeObserver observer = mImageView.getViewTreeObserver();
	    //observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
	    //    @Override public void onGlobalLayout() {
	    //    	mImageView.configChange();
	    //    	mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
	    //    }//func
		//});
	}//func


	//------------------------------------------------------------------------
	// Menu Events
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu,v,menuInfo);
    	getMenuInflater().inflate(R.menu.activity_view, menu);
    	menu.setHeaderTitle("Options");

    	menu.findItem(R.id.mnu_readright).setChecked(mPref_ReadRight);
    	
    	switch(mImageView.getScaleMode()){
    		case ImgTransform.SCALE_NONE: menu.findItem(R.id.mnu_scalen).setChecked(true); break;
    		case ImgTransform.SCALE_HEIGHT: menu.findItem(R.id.mnu_scaleh).setChecked(true); break;
    		case ImgTransform.SCALE_WIDTH: menu.findItem(R.id.mnu_scalew).setChecked(true); break;
    		case ImgTransform.SCALE_AUTO: menu.findItem(R.id.mnu_scalea).setChecked(true); break;
    	}//switch
		
    	switch(ActivityUtil.getScreenOrientation()){
			case ActivityUtil.ORIENTATION_DEVICE: menu.findItem(R.id.mnu_orientationd).setChecked(true); break;
    		case ActivityUtil.ORIENTATION_PORTRAIT: menu.findItem(R.id.mnu_orientationp).setChecked(true); break;
    		case ActivityUtil.ORIENTATION_LANDSCAPE: menu.findItem(R.id.mnu_orientationl).setChecked(true); break;
	    }//switch
    }//func
    
	@Override
	public boolean onContextItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.mnu_scaleh: mImageView.setScaleMode(ImgTransform.SCALE_HEIGHT); break;
			case R.id.mnu_scalew: mImageView.setScaleMode(ImgTransform.SCALE_WIDTH); break;
			case R.id.mnu_scalen: mImageView.setScaleMode(ImgTransform.SCALE_NONE); break;
			case R.id.mnu_scalea: mImageView.setScaleMode(ImgTransform.SCALE_AUTO); break;

			case R.id.mnu_orientationd: ActivityUtil.setScreenOrientation(this,ActivityUtil.ORIENTATION_DEVICE); break;
			case R.id.mnu_orientationp: ActivityUtil.setScreenOrientation(this,ActivityUtil.ORIENTATION_PORTRAIT); break;
			case R.id.mnu_orientationl: ActivityUtil.setScreenOrientation(this,ActivityUtil.ORIENTATION_LANDSCAPE); break;
			
			case R.id.mnu_goto: sage.ui.Dialogs.NumPicker(this,"Goto Page",1,mComicLoad.getPageCount(),mComicLoad.getCurrentPage()+1,this); break;
			case R.id.mnu_exit: this.finish(); break;

			case R.id.mnu_readright: 
				mPref_ReadRight = (!mPref_ReadRight); 
				mImageView.setPanState( (mPref_ReadRight)?ImgTransform.INITPAN_LEFT:ImgTransform.INITPAN_RIGHT );
			break;
			
			case R.id.mnu_immersive: ActivityUtil.setImmersiveModeOn(this); return true;
		}//switch
		
		//Popup pulls the activity out of immersive mode, after action, turn it back on.
		if(mPref_FullScreen) ActivityUtil.setImmersiveModeOn(this);
		return true;
	}//func

	//this is for the goto menu option and user clicks ok.
	@Override
	public void onClick(DialogInterface dialog, int which){
		mComicLoad.gotoPage(which-1);

		//Popup pulls the activity out of immersive mode, after action, turn it back on.
		if(mPref_FullScreen) ActivityUtil.setImmersiveModeOn(this);
	}//func
	
	
	//------------------------------------------------------------------------
	// Paging Loading Events
	@Override
	public void onPageLoaded(boolean isSuccess,int currentPage){
		if(isSuccess){ //Save reading progress.
			if(mComicID != ""){
				//Make sure database is open
				if(mDb == null) mDb = new Sqlite(this);
				if(!mDb.isOpen()) mDb.openRead();
	
				//Save update
				String cp = Integer.toString(currentPage);
				String sql = "UPDATE ComicLibrary SET pgCurrent="+cp+", pgRead=CASE WHEN pgRead < "+cp+" THEN "+cp+" ELSE pgRead END WHERE comicID = '" + mComicID + "'"; 
				mDb.execSql(sql,null);
			}//if

			//....................................
			//Display page number
			if(this.mPref_ShowPgNum) showToast(String.format("%d / %d",currentPage+1,mComicLoad.getPageCount()),0);
		}//if
	}//func
	
	@Override
	public void onImageGesture(int gType){
		int status = 0;
		int direction = 0; //0 = left, 1 = right
		boolean shifted;
		
		switch(gType){
			//.....................................
			case GestureImageView.TWOFINGTAP:
				openContextMenu(mImageView);
				return;
			
			//.....................................
			//Tap gesture progresses the panel, then changes pages.
			case GestureImageView.TAPLEFT:
				shifted = (!mPref_ReadRight)? mImageView.shiftLeft() : mImageView.shiftRight_rev();
				if(shifted) return;

				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				status = (mPref_ReadRight)? mComicLoad.prevPage() : mComicLoad.nextPage();
			break;
			
			case GestureImageView.TAPRIGHT:
				shifted = (mPref_ReadRight)? mImageView.shiftRight() : mImageView.shiftLeft_rev();
				if(shifted) return;
				
				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				status = (mPref_ReadRight)? mComicLoad.nextPage() : mComicLoad.prevPage();
				direction = 1;
			break;
			
			//.....................................
			//Flinging Gestures turns pages
			case GestureImageView.FLINGLEFT:
				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				status = (mPref_ReadRight)? mComicLoad.prevPage() : mComicLoad.nextPage(); break;
			case GestureImageView.FLINGRIGHT:
				if(this.mPref_ShowPgNum) showToast("Loading Page...",1);
				status = (mPref_ReadRight)? mComicLoad.nextPage() : mComicLoad.prevPage(); 
				direction = 1;
				break;
			
			default: return; //Any other gestures not handling, exit right away.
		}//switch
		
		if(status == 0){
			String msg = (direction == 0 && mPref_ReadRight || direction == 1 && !mPref_ReadRight)?"FIRST PAGE" : "LAST PAGE";
			showToast(msg,1);
		}else if(status == -1) showToast("Still Preloading, Try again in one second",1);
	}//func

    
	//------------------------------------------------------------------------
	// Helper Functions
	private void showToast(String msg,int duration){
		mToast.setText(msg);
		mToast.setDuration(duration);
		mToast.show();
	}//func
}//cls
