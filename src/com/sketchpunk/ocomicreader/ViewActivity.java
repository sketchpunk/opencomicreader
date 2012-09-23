package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLoader;
import com.sketchpunk.ocomicreader.lib.ComicPageView;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Toast;

public class ViewActivity extends Activity implements ComicPageView.CallBack,ComicLoader.CallBack{
	private ComicPageView mImageView; //Main display of image
	private ComicLoader mComicLoad; //Object that will manage streaming and scaling images out of the archive file

	/*========================================================
	View Events*/
	@Override
	public void onDestroy(){
		super.onDestroy();
	}//func
	
	@Override
	public void onResume(){
		super.onResume();
	}//func
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        this.getActionBar().hide();
        
        //.........................................
        Bundle b = this.getIntent().getExtras(); 
        String filePath = b.getString("path");
        
        //.........................................        
        mImageView = (ComicPageView)this.findViewById(R.id.pageView);
        
        //.........................................
        mComicLoad = new ComicLoader(this,mImageView);
        mComicLoad.loadArchive(filePath);
        mComicLoad.gotoPage(0);
        
        //.........................................
        View root = mImageView.getRootView();
        root.setBackgroundColor(0xFF000000);
    }//func


	/*========================================================
	*/
	@Override
	public void onPageLoaded(boolean isSuccess,int pWidth,int pHeight){
	}//func

	@Override
	public void onComicPageGesture(int gestureID) {
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
