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
	
	public PageLoader(){}//func
	
	public void loadImage(CallBack callback,String imgPath,float maxSize,iComicArchive archive,int imgType){
		if(mTask != null){
			if(mTask.getStatus() != AsyncTask.Status.FINISHED){
				if(!mTask.imagePath.equals(imgPath)) mTask.cancel(true);
				else return; //Already loaded that image.
			}//if
		}//if

		mTask = new LoadingTask(callback,archive,imgType);
		mTask.execute(Float.valueOf(maxSize),imgPath);
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
	protected static class LoadingTask extends AsyncTask<Object,Object,Bitmap>{ //<Params, Progress, Result>
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
		protected Bitmap doInBackground(Object... params) {
			//...................................
			final iComicArchive archive = mArchive.get();
			if(archive == null) return null;
			
			//...................................			
			//float maxSize = ((Float)params[0]).floatValue(); //Might use this later for force scale down value.
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
					
					//Check for scaling
					if(!this.isCancelled()){
						//--------------------------------------
						//if over bitmap limit of 2048x2048, must scale down.
						if(bmpOption.outHeight > 2040 || bmpOption.outWidth > 2040){
							bmpOption.inScaled = false;
							bmpOption.inJustDecodeBounds = false;
							bmpOption.inSampleSize = 2; //Can only scale by whole numbers, Would be nice to scale by float. Min Scale is Half.
						}else{ //Image is fine, load original size.
							bmpOption = null;
						}//if
					}//if
					
					//Load image scaled or not.
					if(!this.isCancelled()){
						//Rar stream can be reset which is better, but zip's can not, new stream must be created.
						if(archive.isStreamResetable()) iStream.reset();
						else{
							iStream.close(); iStream = null;
							iStream = archive.getItemInputStream(imagePath);
						}//if
	
						bmp = BitmapFactory.decodeStream(iStream,null,bmpOption);
					}//if
					
					archive.clearCache(); //Rar caches the data, clear it out since it's already been saved as an image.
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
		protected void onPostExecute(Bitmap bmp){
			//--------------------------
			//if the task has been cancelled, don't bother doing anything else.
			if(this.isCancelled()){
				if(bmp != null){ 
					bmp.recycle();
					bmp = null;
				}//if
			}//if

			//.....................................
			//When done loading the image, alert parent
			if(mCallBack != null){
				final CallBack cb = mCallBack.get();
				if(cb != null) cb.onImageLoaded(errMsg,bmp,mImgType);
			}//if
		}//func
	}//cls
}//cls