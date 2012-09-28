package com.sketchpunk.ocomicreader;

import java.util.Map;

import sage.data.Sqlite;
import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Toast;

public class ViewActivity extends Activity implements ComicPageView.CallBack,ComicLoader.CallBack{
	private ComicPageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file
	private String mComicID = "";
	private Sqlite mDb = null;
	

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
        
        //.........................................
        Bundle b = this.getIntent().getExtras(); 
        mComicID = b.getString("comicid");
        System.out.println(mComicID);

        //.........................................
        //Get comic information
        mDb = new Sqlite(this);
        mDb.openRead();
        Map<String,String> dbData = mDb.scalarRow("SELECT path,pgCurrent FROM ComicLibrary WHERE comicID = ?",new String[]{mComicID});
        
        //.........................................        
        mImageView = (ComicPageView)this.findViewById(R.id.pageView);
        
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
    }//func


	/*========================================================
	*/
	@Override
	public void onPageLoaded(boolean isSuccess,int currentPage){
		if(isSuccess && mDb != null && mDb.isOpen()){ //Save reading progress.
			String cp = Integer.toString(currentPage);
			String sql = "UPDATE ComicLibrary SET pgCurrent="+cp+", pgRead=CASE WHEN pgRead < "+cp+" THEN "+cp+" ELSE pgRead END WHERE comicID = '" + mComicID + "'"; 
			mDb.execSql(sql,null);
		}//if
	}//func

	@Override
	public void onComicPageGesture(int gestureID){
		switch(gestureID){
			case ComicPageView.FlingRight:
			case ComicPageView.TapLeft:
				if(!mComicLoad.prevPage()) Toast.makeText(this,"First Page",Toast.LENGTH_SHORT).show();
				break;

			case ComicPageView.FlingLeft:
			case ComicPageView.TapRight:
				if(!mComicLoad.nextPage()) Toast.makeText(this,"Last Page",Toast.LENGTH_SHORT).show();
				break;
		}//switch
	}//func	
}//cls
