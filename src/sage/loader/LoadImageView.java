package sage.loader;


import java.io.File;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public class LoadImageView{
	public static final int taskTagID = 1357924680;
	
	public static interface OnImageLoadingListener{
		public Bitmap onImageLoading(String path);
	}//interface
	
	public static interface OnImageLoadedListener{
		public void onImageLoaded(boolean isSuccess,Bitmap bmp,ImageView iv);
	}//interface
	
	
    /*========================================================
	*/	
	//Starting Point, Load the image through a thread
	public static void loadImage(String imgPath,ImageView iv,Object context){
		if(cancelRunningTask(imgPath,iv)){
			final LoadingTask task = new LoadingTask(iv,context);
			iv.setTag(taskTagID,task);
			task.execute(imgPath);
		}//if
	}//func
	
	//if a task is already running, cancel it.
	public static boolean cancelRunningTask(String imgPath,ImageView imgView){
		final LoadingTask task = getLoadingTask(imgView);
		
		if(task != null){
			final String taskImgPath = task.imagePath;
			
			if(!taskImgPath.equals(imgPath)) task.cancel(true);
			else return false;
		}//if
		
		return true;
	}//func
		
	//Get the loading task from the imageview
	public static LoadingTask getLoadingTask(ImageView iv){
		if(iv != null){
			final Object task = iv.getTag(taskTagID);
			if(task != null && task instanceof LoadingTask){
				return (LoadingTask)task;
			}//if
		}//if
		
		return null;
	}//func


	//************************************************************
	// Load Image through thread
	//************************************************************
	protected static class LoadingTask extends AsyncTask{
		private WeakReference mImgView = null;
		private WeakReference mOnImageLoading = null;
		private WeakReference mOnImageLoaded = null;
		
		public String imagePath; //used to compare if the same task
		
		public LoadingTask(ImageView iv,Object context){
			mImgView = new WeakReference(iv);

			if(context != null){
				if(context instanceof OnImageLoadingListener) mOnImageLoading = new WeakReference((OnImageLoadingListener)context);
				if(context instanceof OnImageLoadedListener) mOnImageLoaded = new WeakReference((OnImageLoadedListener)context);
			}//if
		}//func

		@Override
		protected Bitmap doInBackground(Object... params){
			imagePath = (String)params[0];
			
			//.....................................			
			//If callback exists, then we need to load the image in a special sort of way.
			if(mOnImageLoading != null){
				final OnImageLoadingListener callback = (OnImageLoadingListener)mOnImageLoading.get();
				return (callback != null)? callback.onImageLoading(imagePath) : null;
			}//if

			//.....................................
			//if no callback, then load the image ourselves
			File fImg = new File(imagePath);
			if(!fImg.exists()) return null;
				
			return BitmapFactory.decodeFile(imagePath,null);
		}//func

		@Override
		protected void onPostExecute(Object bmp){
			boolean isSuccess = false;
			
			//--------------------------
			//if the task has been cancelled, don't bother doing anything else.
			if(this.isCancelled()){
				if(bmp != null){ ((Bitmap)bmp).recycle(); bmp = null; }

			//--------------------------
			//if no callback, but we have an image and a view.
			}else if(mImgView != null && bmp != null){
				final ImageView iv = (ImageView) mImgView.get();
				final LoadingTask task = LoadImageView.getLoadingTask(iv);
				
				if(iv != null && bmp != null){
					iv.setImageBitmap((Bitmap)bmp);
					isSuccess = true;
				}//if
			
			//--------------------------
			//incase imageview doesn't exist anymore but bmp was loaded.
			}else if(bmp != null){ ((Bitmap)bmp).recycle(); bmp=null; }//if

			//.....................................
			//When done loading the image, alert parent
			if(mOnImageLoaded != null){
				final OnImageLoadedListener callback = (OnImageLoadedListener) mOnImageLoaded.get();
				if(callback != null) callback.onImageLoaded(isSuccess,(Bitmap)bmp,(ImageView) mImgView.get());
			}//if
		}//func
	}//cls
}//func