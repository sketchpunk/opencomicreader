package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Stack;
import java.util.UUID;

import sage.data.Sqlite;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

public class ComicLibrary{
	private static Thread mWorkerThread = null;
	
	/*========================================================
	status constants*/
	public final static int STATUS_NOSETTINGS = 1;
	public final static int STATUS_COMPLETE = 0;
	public final static String UKNOWN_SERIES = "-unknown-";

	
	/*========================================================
	Thread safe messaging*/
	//Object used to handle threadsafe call backs
	public static Handler EventHandler = new Handler(){ public void handleMessage(Message msg){ ComicLibrary.onHandle(msg); }};
	
	//Execute the requested Call back.
    public static void onHandle(Message msg){
    	SyncCallback cb = (SyncCallback)msg.obj;
    	Bundle data = msg.getData();
    	
    	switch(msg.what){
    		//...............................
    		case 1: //progress
    			if(cb != null) cb.onSyncProgress(data.getString("msg"));
    		break;
    			
       		//...............................
    		case 0: //complete
    			if(cb != null) cb.onSyncComplete(data.getInt("status"));
    			mWorkerThread = null;
    		break;
    	}//switch
    }//func
	
	/*========================================================
	sync methods*/
    public static boolean startSync(Context context){
		//...............................
    	if(mWorkerThread != null){
    		if(mWorkerThread.isAlive()) return false;
    	}//func
    	
    	//...............................
		mWorkerThread = new Thread(new LibrarySync(context));
		mWorkerThread.start();
		return true;
    }//func
    
    
    
	/*========================================================
	Static Methods*/
	
	public static String getThumbCachePath(){
		//........................................
		//Make sure the cache folder exists.
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/";
		File file = new File(path);
        if(!file.exists()) file.mkdirs();

		//........................................
        //Create nomedia file so thumbs aren't indexed for gallery
        file = new File(path,".nomedia");
        if (!file.exists()){
            try{ file.createNewFile(); }catch (Exception e){}
        }//if
        
        return path;
	}//func
	
	public static void removeComic(Context context,String id,boolean delComic){
    	File file;
    	sage.data.Sqlite db = new sage.data.Sqlite(context);
    	db.openWrite();
    	
    	if(delComic){ //Delete comic from device
    		String comicPath = db.scalar("SELECT path FROM ComicLibrary WHERE comicID = '"+id.replace("'","''")+"';",null);
    		if(comicPath != null && !comicPath.isEmpty()){
    			file = new File(comicPath);
    			if(file.exists()) file.delete();
    		}//if
    	}//if
    	
    	//Delete comic from library
    	if(db.delete("ComicLibrary", "comicID = '"+id.replace("'","''")+"'",null) > 0){
    		file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/" + id +".jpg");
    		if(file.exists()) file.delete();
    	}//if
    	
    	db.close();
    }//func
    
    public static void setComicProgress(Context context,String id,int state,boolean applySeries){
    	String sql = "UPDATE ComicLibrary SET ";
    	sql += (state == 0)?"pgRead=0,pgCurrent=0":"pgRead=pgCount,pgCurrent=pgCount"; //0-Reset or 1-Mark as Read.
    	
    	if(applySeries) sql += " WHERE series in (SELECT series FROM ComicLibrary WHERE comicID = '"+id+"')";
    	else sql += " WHERE comicID = '"+id+"'";
    	
    	sage.data.Sqlite.execSql(context,sql,null);
    }//func
	
	public static boolean clearAll(Context context){
		//................................................
		//Delete thumbnails
		String cachePath = getThumbCachePath();

		File fObj = new File(cachePath);
		File[] fList = fObj.listFiles(new ThumbFindFilter());
		if(fList != null){
			for(File file:fList) file.delete();
		}//if

		//................................................
		Sqlite.delete(context,"ComicLibrary","",null);
		return true;
	}//func

	public static boolean createThumb(int coverHeight,int coverQuality,iComicArchive archive,String coverPath,String saveTo){
		boolean rtn = false;
		InputStream iStream = archive.getItemInputStream(coverPath);

		if(iStream != null){
			Bitmap bmp = null;
			try{				
				//....................................
				//Get file dimension
				BitmapFactory.Options bmpOption = new BitmapFactory.Options();
				bmpOption.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(iStream,null,bmpOption);
				
				//calc scale
				int scale = (bmpOption.outHeight > coverHeight)? Math.round((float)bmpOption.outHeight/coverHeight) : 0;
				bmpOption.inSampleSize = scale;
				bmpOption.inJustDecodeBounds = false;
				
				//....................................
				//Load bitmap and rescale
				iStream.close(); //the first read should of closed the stream. Just do it just incase it didn't
				iStream = archive.getItemInputStream(coverPath);
				bmp = BitmapFactory.decodeStream(iStream,null,bmpOption);
				
				//....................................
				//Save bitmap to file
				File file = new File(saveTo);
				FileOutputStream out = new FileOutputStream(file);
				bmp.compress(Bitmap.CompressFormat.JPEG,coverQuality,out);

				//....................................
				out.close();
				bmp.recycle(); bmp = null;
				
				rtn = true;
			}catch(Exception e){
				System.err.println("Error creating thumb " + e.getMessage());
				if(bmp != null){ bmp.recycle(); bmp = null; }//if
			}//try
			
			if(iStream != null){
				try{ iStream.close(); }catch(Exception e){}
			}//if
		}//if
		
		return rtn;
	}//func
	
	public static void clearCovers(Context context){
		//................................................
		//Delete thumbnails
		String cachePath = getThumbCachePath();

		File fObj = new File(cachePath);
		File[] fList = fObj.listFiles(new ThumbFindFilter());
		if(fList != null){
			for(File file:fList) file.delete();
		}//if
		
		//................................................
		sage.data.Sqlite.execSql(context,"UPDATE ComicLibrary SET isCoverExists=0",null);
	}//func
	
	public static void clearSeries(Context context){
		ContentValues cv = new ContentValues();
		cv.put("series",ComicLibrary.UKNOWN_SERIES);
		sage.data.Sqlite.update(context,"ComicLibrary",cv,null,null);
	}//func
	
	public static void setSeriesName(Context context,String comicID,String seriesName){
		ContentValues cv = new ContentValues();
		cv.put("series",seriesName);
		sage.data.Sqlite.update(context,"ComicLibrary",cv,"comicID=?",new String[]{comicID});
	}//func
	
	public static void renameSeries(Context context,String oldSeries,String newSeries){
		ContentValues cv = new ContentValues();
		cv.put("series",newSeries);
		sage.data.Sqlite.update(context,"ComicLibrary",cv,"series=?",new String[]{oldSeries});
	}//func

	//************************************************************
	// Support Objects
	//************************************************************
	public static interface SyncCallback{
		public void onSyncProgress(String txt);
		public void onSyncComplete(int status);
	}//interface
	
    protected static class ComicFindFilter implements java.io.FileFilter{
    	private final String[] mExtList = new String[]{".zip",".cbz",".rar",".cbr"};
    	public boolean accept(File o){
    		if(o.isDirectory()) return true; //Want to allow folders
    		for(String extension:mExtList){
    			if(o.getName().toLowerCase(Locale.getDefault()).endsWith(extension)) return true;
    		}//for
    		return false;
    	}//func
    }//cls
	
    protected static class ThumbFindFilter implements java.io.FileFilter{
    	public boolean accept(File o){
    		if(o.isDirectory()) return false;
    		else if(o.getName().toLowerCase(Locale.getDefault()).endsWith(".jpg")) return true;
    		return false;
    	}//func
    }//cls
    
}//cls
