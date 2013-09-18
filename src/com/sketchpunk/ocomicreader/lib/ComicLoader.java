package com.sketchpunk.ocomicreader.lib;

import java.util.List;
import java.util.Locale;

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
		String ext = sage.io.Path.getExt(path).toLowerCase(Locale.getDefault());
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
	private Bitmap mCurrentBmp=null,mNextBmp=null,mPrevBmp=null;
	private Context mContext = null;

	public ComicLoader(Context context,ComicPageView o){
		mImageView = o;
		mContext = context;

	    //............................
		//Save Callback
		if(context instanceof CallBack) mCallBack = (CallBack)context;
		
		//............................
		//Get perferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mIsPreloading = prefs.getBoolean("preLoading",false);
		
		//............................
		//Get the window size
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mMaxSize = (float)Math.max((float)size.x,(float)size.y);
		
		mPageLoader = new PageLoader();
		mCurrentPage = -1;
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

			if(mCurrentBmp != null){
				mImageView.setImageBitmap(null);
				mCurrentBmp.recycle(); mCurrentBmp = null;
			}//if
			
			if(mNextBmp != null){mNextBmp.recycle(); mNextBmp = null;}
			if(mPrevBmp != null){mPrevBmp.recycle(); mPrevBmp = null;}

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

	public void refreshOrientation(){
		Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		mMaxSize = (float)Math.max((float)size.x,(float)size.y);
	}//func
	
	/*--------------------------------------------------------
	Paging Methods*/
	public int gotoPage(int pos){
		if(pos < 1 || pos > mPageLen || pos == mCurrentPage+1) return 0;
		
		//Arrays start ay 0, but pages start at 1.
		if(pos == mCurrentPage+2) return nextPage();
		else if(pos == mCurrentPage-2) return prevPage();
		else{
			mCurrentPage = pos-1; //Page to Index Conversion
			mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive,0);
		}//if
		
		return 1;
	}//func
	
	public int nextPage(){
		if(mCurrentPage+1 >= mPageLen) return 0;

		if(!mIsPreloading){
			mCurrentPage++;
			mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive,0);
		}else{
			//mPageLoader.cancelTask(); //Cancel any loading to prevent any bitmaps from changing.
			if(mPageLoader.isLoading()) return -1;
			
			mCurrentPage++;
			if(mPrevBmp != null){ mPrevBmp.recycle(); mPrevBmp = null;} //Clean up prev image
			if(mCurrentBmp != null) mPrevBmp = mCurrentBmp; //save current to prev incase user wants to go back one
			
			//if we have a next image, set view to that and preload next.
			if(mNextBmp != null){
				mCurrentBmp = mNextBmp;
				mNextBmp = null;
				
				if(mCurrentPage+1 < mPageLen) mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage+1),mMaxSize,mArchive,1);
				mImageView.setImageBitmap(mCurrentBmp);
				
				if(mCallBack != null) mCallBack.onPageLoaded(true,mCurrentPage+1);
			}else{
				mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive,0);
			}//if
		}//if
		
		return 1;
	}//func
	
	public int prevPage(){
		if(mCurrentPage-1 < 0) return 0;
		
		if(!mIsPreloading){
			mCurrentPage--;
			mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive,0);
		}else{
			//mPageLoader.cancelTask(); //Cancel any loading to prevent any bitmaps from changing.
			if(mPageLoader.isLoading()) return -1;
			
			mCurrentPage--;
			if(mNextBmp != null){ mNextBmp.recycle(); mNextBmp = null;} //Clean up next image
			if(mCurrentBmp != null) mNextBmp = mCurrentBmp; //save current to next incase user wants to go forward
			
			//if we have a prev image, set view to that and preload prev.
			if(mPrevBmp != null){
				mCurrentBmp = mPrevBmp;
				mPrevBmp = null;
				
				if(mCurrentPage-1 >= 0) mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage-1),mMaxSize,mArchive,2);
				mImageView.setImageBitmap(mCurrentBmp);
				
				if(mCallBack != null) mCallBack.onPageLoaded(true,mCurrentPage+1);
			}else{
				mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage),mMaxSize,mArchive,0);
			}//if
		}//if
		
		return 1;
	}//func

	
	/*--------------------------------------------------------
	Image Loading*/
	@Override
	public void onImageLoaded(String errMsg, Bitmap bmp,int imgType){
		if(errMsg != null){
			Toast.makeText(mContext,errMsg,Toast.LENGTH_LONG).show();
		}//if

		//............................................		
		//if we have a new image and an old image.
		if(bmp != null){
			switch(imgType){
				case 0:
					if(mCurrentBmp != null){mCurrentBmp.recycle(); mCurrentBmp = null;}
					mCurrentBmp = bmp;
					mImageView.setImageBitmap(mCurrentBmp);
					if(mCallBack != null) mCallBack.onPageLoaded((bmp != null),mCurrentPage+1);
					
					if(mIsPreloading && mCurrentPage+1 < mPageLen){
						mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage+1),mMaxSize,mArchive,1);
					}//if
					
					break;
				case 1:
					if(mNextBmp != null){mNextBmp.recycle(); mNextBmp = null;}
					mNextBmp = bmp;
					
					if(mIsPreloading && mCurrentPage-1 >= 0 && mPrevBmp == null){ //Preload prev if not available.
						mPageLoader.loadImage((PageLoader.CallBack)this,mPageList.get(mCurrentPage-1),mMaxSize,mArchive,2);
					}//if
					
					break;
				case 2:
					if(mPrevBmp != null){mPrevBmp.recycle(); mPrevBmp = null;}
					mPrevBmp = bmp;
					break;
			}//switch
		}//if

		//............................................
		bmp = null;
	}//func
}//cls
