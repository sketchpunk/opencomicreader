package com.sketchpunk.ocomicreader.lib;

import java.io.InputStream;
import java.util.List;

import com.sketchpunk.ocomicreader.ui.ComicPageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import sage.loader.LoadImageView;

public class ComicLoader implements LoadImageView.OnImageLoadingListener,LoadImageView.OnImageLoadedListener{
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
	private int mPageLen, mCurrentPage, mPageWidth, mPageHeight;
	private float mScreenWidth, mScreenHeight, mMaxSize;
	private CallBack mCallBack;
	
	private ComicPageView mImageView;
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
		mScreenWidth = (float)size.x;
		mScreenHeight = (float)size.y;
		mMaxSize = (float)Math.max(mScreenWidth,mScreenHeight);
	}//func

	/*--------------------------------------------------------
	Getters*/
	//Since the event has been created, these getters plus the variables won't be needed anymore.
	public int getPageWidth(){ return mPageWidth; }
	public int getPageHeight(){ return mPageHeight; }
	public int getCurrentPage(){ return mCurrentPage; }
	public int getPageCount(){ return mPageLen; }

	/*--------------------------------------------------------
	Methods*/
	public boolean close(){		
		try{
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
		LoadImageView.loadImage(mPageList.get(mCurrentPage),mImageView,this);
		
		return true;
	}//func
	
	public boolean nextPage(){
		if(mCurrentPage+1 >= mPageLen) return false;
		mCurrentPage++;
		
		//return loadCurrentPage();

		LoadImageView.loadImage(mPageList.get(mCurrentPage),mImageView,this);		
		return true;
	}//func
	
	public boolean prevPage(){
		if(mCurrentPage-1 < 0) return false;
		mCurrentPage--;
		
		LoadImageView.loadImage(mPageList.get(mCurrentPage),mImageView,this);
		return true;
		//return loadCurrentPage();
	}//func

	
	/*--------------------------------------------------------
	Image Loading*/
	//call back use do a custom image loading in the task
	@Override
	public Bitmap onImageLoading(String path){
		InputStream iStream = mArchive.getItemInputStream(path);
		Bitmap bmp = null;
		
		if(iStream != null){
			try{
				//....................................
				//Get file dimension and downscale if needed
				BitmapFactory.Options bmpOption = new BitmapFactory.Options();
				bmpOption.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(iStream,null,bmpOption);
				
				int scale = 0;
				if(Math.max(bmpOption.outHeight,bmpOption.outWidth) > mMaxSize){
					if(bmpOption.outHeight > bmpOption.outWidth) scale = Math.round((float)bmpOption.outHeight / mMaxSize);
					else scale = Math.round((float)bmpOption.outWidth / mMaxSize);
				}//if
				
				bmpOption.inSampleSize = scale;
				bmpOption.inJustDecodeBounds = false;
				bmpOption.inScaled = false;

				//....................................
				//Load bitmap
				iStream.close(); iStream = null;
				iStream = mArchive.getItemInputStream(path);
				bmp = BitmapFactory.decodeStream(iStream,null,bmpOption); 
			}catch(Exception e){
				//System.err.println("Error loading comic page " + e.getMessage());
				Toast.makeText(mContext,"Error loading comic page. Email comic file to sketchpunk@ymail.com for troubleshooting.",Toast.LENGTH_LONG).show();
			}//try
			
			if(iStream != null){
				try{ iStream.close(); iStream = null; }catch(Exception e){}
			}//if
		}else{
			Toast.makeText(mContext,"Unable to load image input stream. Email comic file to sketchpunk@ymail.com for troubleshooting.",Toast.LENGTH_LONG).show();
		}//if
		return bmp;
	}//func

	//after is task is complete, get our image.
	@Override
	public void onImageLoaded(boolean isSuccess,Bitmap bmp,View view){
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
			mPageWidth = mBitmap.getWidth();
			mPageHeight = mBitmap.getHeight();
		}//if

		//............................................
		if(mCallBack != null){
			mCallBack.onPageLoaded((bmp != null),mCurrentPage);
		}//if
		
		bmp = null;
	}//func
}//cls
