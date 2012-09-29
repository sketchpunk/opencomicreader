 package com.sketchpunk.ocomicreader;

import java.util.Map;

import sage.data.Sqlite;
import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.os.Bundle;
import android.app.Activity;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

public class ViewActivity extends Activity implements ComicPageView.CallBack,ComicLoader.CallBack{
	private ComicPageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file
	private String mComicID = "";
	private Sqlite mDb = null;
	private Toast mToast;
	

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
	public void onDestroy(){
		if(mDb != null){ mDb.close(); mDb = null; }
		super.onDestroy();
	}//func
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        this.getActionBar().hide();
        this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        
        //.........................................
        Bundle b = this.getIntent().getExtras(); 
        mComicID = b.getString("comicid");

        //.........................................
        //Get comic information
        mDb = new Sqlite(this);
        mDb.openRead();
        Map<String,String> dbData = mDb.scalarRow("SELECT path,pgCurrent FROM ComicLibrary WHERE comicID = ?",new String[]{mComicID});
        
        //.........................................        
        mImageView = (ComicPageView)this.findViewById(R.id.pageView);
        registerForContextMenu(mImageView);
        
        //.........................................
        mComicLoad = new ComicLoader(this,mImageView);
        if(mComicLoad.loadArchive(dbData.get("path"))){
        	mComicLoad.gotoPage(Integer.parseInt(dbData.get("pgCurrent"))); //Continue where user left off
        }else{
        	Toast.makeText(this,"Unable to load comic.",Toast.LENGTH_LONG).show();
        }//if
        
        //.........................................
        View root = mImageView.getRootView();
        root.setBackgroundColor(0xFF000000);

        //.........................................
        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
		mToast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
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
    	}//switch
    }//func
    
	@Override
	public boolean onContextItemSelected(MenuItem item){
		switch(item.getItemId()){
			case R.id.mnu_scaleh: mImageView.setScaleMode(ComicPageView.ScaleToHeight); break;
			case R.id.mnu_scalew: mImageView.setScaleMode(ComicPageView.ScaleToWidth); break;
			case R.id.mnu_scalen: mImageView.setScaleMode(ComicPageView.ScaleNone); break;
		}//switch
		return true;
	}//func
    
    
	/*========================================================
	*/
	@Override
	public void onPageLoaded(boolean isSuccess,int currentPage){
		if(isSuccess && mDb != null && mDb.isOpen()){ //Save reading progress.
			String cp = Integer.toString(currentPage);
			String sql = "UPDATE ComicLibrary SET pgCurrent="+cp+", pgRead=CASE WHEN pgRead < "+cp+" THEN "+cp+" ELSE pgRead END WHERE comicID = '" + mComicID + "'"; 
			mDb.execSql(sql,null);
			
			//....................................
			//Display page number
			mToast.setText(String.format("%d / %d",currentPage+1,mComicLoad.getPageCount()));
			mToast.show();
		}//if
	}//func

	@Override
	public void onComicPageGesture(int gestureID){
		switch(gestureID){
			//.........................................
			case ComicPageView.LongPress:
				this.openContextMenu(mImageView);
				System.out.println("LONG PRESS");
			break;
			//.........................................		
			case ComicPageView.FlingRight:
			case ComicPageView.TapLeft:
				if(!mComicLoad.prevPage()) Toast.makeText(this,"First Page",Toast.LENGTH_SHORT).show();
				break;
			//.........................................
			case ComicPageView.FlingLeft:
			case ComicPageView.TapRight:
				if(!mComicLoad.nextPage()) Toast.makeText(this,"Last Page",Toast.LENGTH_SHORT).show();
				break;
		}//switch
	}//func	
}//cls
