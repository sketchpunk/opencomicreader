package com.sketchpunk.ocomicreader.lib;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class PageLoader{
	public static interface CallBack{
		public void onImageLoaded(String errMsg,Bitmap bmp,int imgType);
	}//interface
	
    /*========================================================
	*/
	private LoadingTask mTask;
	public PageLoader(){}
	
	public void loadImage(CallBack callback,String imgPath,float maxSize,iComicArchive archive,int imgType){
		if(mTask != null){
			if(mTask.getStatus() != AsyncTask.Status.FINISHED){
				if(!mTask.imagePath.equals(imgPath)) mTask.cancel(true);
				else return; //Already loaded that image.
			}//if
		}//if

		mTask = new LoadingTask(callback,archive,imgType);
		mTask.execute(new Float(maxSize),imgPath);
	}//func
	
	public void close(){
		cancelTask();
	}//func
	
	public void cancelTask(){
		if(mTask != null){
			if(mTask.getStatus() != AsyncTask.Status.FINISHED) mTask.cancel(true);
		}//if
	}//func
	
	public boolean isLoading(){
		if(mTask != null){
			if(mTask.getStatus() == AsyncTask.Status.FINISHED) return false;
			else return true;
		}//if
		
		return false;
	}//func
	
	//************************************************************
	// Load Image through thread
	//************************************************************
	protected static class LoadingTask extends AsyncTask{
		private WeakReference<iComicArchive> mArchive = null;
		private WeakReference<CallBack> mCallBack = null;
		private String errMsg = null;
		public String imagePath = null;
		private int mImgType;
		
		public LoadingTask(CallBack callback,iComicArchive archive,int imgType){
			mArchive = new WeakReference<iComicArchive>(archive);
			mCallBack = new WeakReference<CallBack>(callback);
			mImgType = imgType;
		}//func
		
		@Override
		protected Object doInBackground(Object... params) {
			//...................................
			final iComicArchive archive = mArchive.get();
			if(archive == null) return null;
			
			//...................................			
			float maxSize = ((Float)params[0]).floatValue();
			imagePath = (String)params[1];
			
			InputStream iStream = archive.getItemInputStream(imagePath);
			Bitmap bmp = null;
			
			if(iStream != null){
				try{
					//...................................
					//Get file dimension and downscale if needed
					BitmapFactory.Options bmpOption = new BitmapFactory.Options();
					bmpOption.inJustDecodeBounds = true;
					if(!this.isCancelled()) BitmapFactory.decodeStream(iStream,null,bmpOption);
					
					int scale = 0;
					if(!this.isCancelled()){
						if(Math.max(bmpOption.outHeight,bmpOption.outWidth) > maxSize){
							if(bmpOption.outHeight > bmpOption.outWidth) scale = Math.round((float)bmpOption.outHeight / maxSize);
							else scale = Math.round((float)bmpOption.outWidth / maxSize);
						}//if
						
						bmpOption.inSampleSize = scale;
						bmpOption.inJustDecodeBounds = false;
						bmpOption.inScaled = false;
					}//if

					//....................................
					//Load bitmap
					iStream.close(); iStream = null;
					if(!this.isCancelled()){
						iStream = archive.getItemInputStream(imagePath);
						bmp = BitmapFactory.decodeStream(iStream,null,bmpOption);
					}//if
				}catch(Exception e){
					//System.err.println("Error loading comic page " + e.getMessage());
					errMsg = "Error loading comic page. Email comic file to sketchpunk@ymail.com for troubleshooting.";
				}//try
				
				if(iStream != null){
					try{ iStream.close(); iStream = null; }catch(Exception e){}
				}//if
			}else{
				errMsg = "Unable to load image input stream. Email comic file to sketchpunk@ymail.com for troubleshooting.";
			}//if
			return bmp;
		}//func
		
		@Override
		protected void onPostExecute(Object bmp){
			//--------------------------
			//if the task has been cancelled, don't bother doing anything else.
			if(this.isCancelled()){
				if(bmp != null){ 
					((Bitmap)bmp).recycle();
					bmp = null;
				}//if
			}//if

			//.....................................
			//When done loading the image, alert parent
			if(mCallBack != null){
				final CallBack cb = mCallBack.get();
				if(cb != null) cb.onImageLoaded(errMsg,(Bitmap)bmp,mImgType);
			}//if
		}//func
	}//cls
}//cls