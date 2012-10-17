package com.sketchpunk.ocomicreader.lib;

import java.util.List;

import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Display;
import android.widget.Toast;

public class ComicLoader implements PageLoader.CallBack{//LoadImageView.OnImageLoadingListener,LoadImageView.OnImageLoadedListener{
	public static interface CallBack{
		public void onPageLoaded(boolean isSuccess,int currentPage);
	}//interface
	
	public static iComicArchive getArchiveInstance(String path){
		String ext = sage.io.Path.getExt(path).toLowerCase();
		iComicArchive o = null;

		if(ext.equals("zip") || ext.equals("cbz")){
			o = new ComicZip();
			if(o.loadFile(path)) return o;
			o.close();
		}else if(ext.equals("rar") || ext.equals("cbr")){
			o = new ComicRar();
			if(o.loadFile(path)) return o;
			o.close();
		}//if
		
		return null;
	}//func

	/*--------------------------------------------------------
	*/
	private int mPageLen, mCurrentPage;
	private float mMaxSize;
	private CallBack mCallBack;
	private boolean mIsPreloading;
	
	private ComicPageView mImageView;
	private PageLoader mPageLoader;
	private iComicArchive mArchive;
	private List<String> mPageList;
	private Bitmap mBitmap = null;
	private Context mContext = null;

	public ComicLoader(Context context,ComicPageView o){
		mImageView = o;
		mContext = context;
		
		//Save Callback
		if(context instanceof CallBack) mCallBack = (CallBack)context;
		
		//............................
		//Get the window size
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mMaxSize = (float)Math.max((float)size.x,(float)size.y);
		
		mPageLoader = new PageLoader();
		
		//Get perferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mIsPreloading = prefs.getBoolean("preLoading",true);
	}//func

	/*--------------------------------------------------------
	Getters*/
	//Since the event has been created, these getters plus the variables won't be needed anymore.
	public int getCurrentPage(){ return mCurrentPage; }
	public int getPageCount(){ return mPageLen; }

	/*--------------------------------------------------------
	Methods*/
	public boolean close(){		
		try{
			mPageLoader.close(); //cancel any tasks that may be running.
			
			if(mArchive != null){ mArchive.close(); mArchive = null; }//if

			if(mBitmap != null){
				mImageView.setImageBitmap(null);
				mBitmap.recycle(); mBitmap = null;
			}//if

			mCallBack = null;
			mImageView = null;
			return true;
		}catch(Exception e){
			System.out.println("Error closing archive " + e.getMessage());
		}//func

		return false;
	}//func
	
	//Load a list of images in the archive file, need path to stream out the file.
	public boolean loadArchive(String path){
		try{
			mArchive = ComicLoader.getArchiveInstance(path);
			if(mArchive == null) return false;
			
			//Get page list
			mPageList = mArchive.getPageList();
			if(mPageList != null){
				mPageLen = mPageList.size();
				return true;
			}//if
			
			//if non found, then just close the archive.
			mArchive.close(); mArchive = null;
		}catch(Exception e){
			System.err.println("LoadArchive " + e.getMessage());
		}//try

		return false;
	}//func
	
	
	/*--------------------------------------------------------
	Paging Methods*/
	public boolean gotoPage(int pos){
		if(pos < 0 || pos >= mPageLen || pos == mCurrentPage) return false;
		
		mCurrentPage = pos;
		mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive);
		return true;
	}//func
	
	public boolean nextPage(){
		if(mCurrentPage+1 >= mPageLen) return false;
		mCurrentPage++;
		
		mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive);
		return true;
	}//func
	
	public boolean prevPage(){
		if(mCurrentPage-1 < 0) return false;
		mCurrentPage--;

		mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive);
		return true;
	}//func

	
	/*--------------------------------------------------------
	Image Loading*/
	@Override
	public void onImageLoaded(String errMsg, Bitmap bmp){
		System.out.println("ImageLoaded");
		if(errMsg != null){
			Toast.makeText(mContext,errMsg,Toast.LENGTH_LONG).show();
		}//if

		//............................................		
		//if we have a new image and an old image.
		if(bmp != null && mBitmap != null){
			mImageView.setImageBitmap(null);
			mBitmap.recycle();
			mBitmap = null;
		}//if

		//............................................
		if(bmp != null){
			mBitmap = bmp;
			mImageView.setImageBitmap(mBitmap);
		}//if

		//............................................
		if(mCallBack != null) mCallBack.onPageLoaded((bmp != null),mCurrentPage);
		bmp = null;
	}//func
}//cls
