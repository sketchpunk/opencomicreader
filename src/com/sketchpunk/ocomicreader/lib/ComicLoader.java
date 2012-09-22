package com.sketchpunk.ocomicreader.lib;

//https://github.com/edmund-wagner/junrar  Might be suitable for rar/cbr files, has getInputStream
//https://github.com/junrar

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import sage.loader.LoadImageView;

public class ComicLoader implements LoadImageView.OnImageLoadingListener,LoadImageView.OnImageLoadedListener{
	public static interface CallBack{
		public void onPageLoaded(boolean isSuccess,int pWidth,int pHeight);
	}//interface

	/*--------------------------------------------------------
	*/
	private int mPageLen, mCurrentPage, mPageWidth, mPageHeight;
	private float mScreenWidth, mScreenHeight, mMaxSize;
	private CallBack mCallBack;
	
	private ComicPageView mImageView;
	private ZipFile mZipFile;
	private List<String> mPageList;
	private Bitmap mBitmap = null;

	public ComicLoader(Context context,ComicPageView o){
		mImageView = o;
		mPageList = new ArrayList<String>();

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
	

	/*--------------------------------------------------------
	Methods*/
	public boolean close(){		
		try{
			if(mZipFile != null){ mZipFile.close(); mZipFile = null; }//if

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
		try {
			String itmName;
			ZipEntry itm;
			mZipFile = new ZipFile(path);
			Enumeration entries = mZipFile.entries();

			//..................................
			while(entries.hasMoreElements()) {
				itm = (ZipEntry)entries.nextElement();
				if(itm.isDirectory()) continue;
				
				itmName = itm.getName().toLowerCase();
				if(itmName.endsWith(".jpg") || itmName.endsWith(".gif") || itmName.endsWith(".png")){
					mPageList.add(itm.getName());
				}//if
			}//while

			//..................................
			mPageLen = mPageList.size();
			if(mPageLen > 0){
				Collections.sort(mPageList); //Sort the page names
				mCurrentPage = -1;
				return true;
			}else{ 
				mZipFile.close(); mZipFile = null;
				return false;
			}//if
		}catch(Exception e){
			System.err.println("LoadArchive " + e.getMessage());
		}//try

		return false;
	}//func
	
	
	/*--------------------------------------------------------
	Paging Methods*/
	public boolean gotoPage(int pos){
		if(pos < 0 || pos >= mPageLen) return false;
		
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
		ZipEntry itm = mZipFile.getEntry(path);
		Bitmap bmp = null;
		
		if(itm != null){
			InputStream iStream = null;
			try{
				iStream = mZipFile.getInputStream(itm);				

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

				//....................................
				//Load bitmap
				iStream.close(); iStream = null;
				iStream = mZipFile.getInputStream(itm);	
				bmp = BitmapFactory.decodeStream(iStream,null,bmpOption); 
			}catch(Exception e){
				System.err.println("Error loading comic page " + e.getMessage());
			}//try
			
			if(iStream != null){
				try{ iStream.close(); iStream = null; }catch(Exception e){}
			}//if
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
			mCallBack.onPageLoaded((bmp != null),mPageWidth, mPageHeight);
		}//if
		
		bmp = null;
	}//func
}//cls
