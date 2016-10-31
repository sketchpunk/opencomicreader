package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

import sage.data.Sqlite;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.sketchpunk.ocomicreader.data.MainDB;
//TODO can probaly remove alot of the context since the new sqlite com uses the app context.
public class ComicLibrary{
	private static Thread mWorkerThread = null;
	
	//region status constants
	public final static int STATUS_NOSETTINGS = 1;
	public final static int STATUS_COMPLETE = 0;
	public final static String UKNOWN_SERIES = "-unknown-";
	//endregion

	//region Thread safe messaging
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
    	}
    }
	//endregion
    
	//region Static methods
    public static boolean startSync(Context context){
		//...............................
    	if(mWorkerThread != null){
    		if(mWorkerThread.isAlive()) return false;
    	}//func
    	
    	//...............................
		mWorkerThread = new Thread(new LibrarySync(context));
		mWorkerThread.start();
		return true;
    }

	public static String getThumbCachePath(){
		//........................................
		//Make sure the cache folder exists.
		String path = Settings.AppFld("thumbs");
		File file = new File(path);
        if(!file.exists()) file.mkdirs();

		//........................................
        //Create nomedia file so thumbs aren't indexed for gallery
        file = new File(path,".nomedia");
        if (!file.exists()){
            try{ file.createNewFile(); }catch (Exception e){}
        }//if
        
        return path;
	}
	//endregion

	//region Manage Comics
	public static void removeComic(Context context,String id,boolean delComic){
    	File file;
    	Sqlite db = new Sqlite(MainDB.get()).openWrite();
    	
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
    }

    public static void setComicProgress(String[] idAry,int state,boolean applySeries){
		String idList = sage.data.StringUtil.join(idAry,"','","'","'"),
			sql = "UPDATE ComicLibrary SET " + ((state == 0)?"pgRead=0,pgCurrent=0":"pgRead=pgCount,pgCurrent=pgCount"); //0-Reset or 1-Mark as Read.

    	if(applySeries) sql += " WHERE series in (SELECT series FROM ComicLibrary WHERE comicID in ("+idList+"))";
    	else sql += " WHERE comicID in ("+idList+")";

		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.execSql(sql,null);
    }

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
		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.delete("ComicLibrary","",null);
		return true;
	}
    
	public static String getListSql(int filterMode,String seriesName,int nextPos){
		String sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists,path,pgCurrent FROM ComicLibrary";
		
   		switch(filterMode){
   			case 2: sql += " WHERE pgRead=0 ORDER BY title"; break; //Unread;
   			case 3: sql += " WHERE pgRead > 0 AND pgRead < pgCount-1 ORDER BY title"; break;//Progress
   			case 4: sql += " WHERE pgRead >= pgCount-1 ORDER BY title"; break;//Read
   			case 1: //Series
   				if(seriesName.isEmpty()){
   	       			sql = "SELECT min(comicID) [_id],series [title], series, sum(pgCount) [pgCount],sum(pgRead) [pgRead],min(isCoverExists) [isCoverExists],count(comicID) [cntIssue] FROM ComicLibrary GROUP BY series ORDER BY series";
   	       		}else{
   	       			sql = "SELECT comicID [_id],title,pgCount,pgRead,isCoverExists,path,pgCurrent,series FROM ComicLibrary WHERE series = '"+seriesName.replace("'", "''")+"' ORDER BY title";
   	       		}//if
   			break;
   		}//switch
   		
   		//When finishing reading a comic, get the next comic on the list to view.
   		if(nextPos > -1) sql += " LIMIT 1 OFFSET " + Integer.toString(nextPos);
   		
       	return sql;
	}
    //endregion

	//region Manage Covers
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
		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.execSql("UPDATE ComicLibrary SET isCoverExists=0",null);
	}
	//endregion
	
	//region Manage Series
	public static void setSeriesName(Context context,String comicID,String seriesName){
		ContentValues cv = new ContentValues();
		cv.put("series",seriesName);

		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.update("ComicLibrary",cv,"comicID=?",new String[]{comicID});
	}
	
	public static void renameSeries(Context context,String oldSeries,String newSeries){
		ContentValues cv = new ContentValues();
		cv.put("series",newSeries);

		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.update("ComicLibrary",cv,"series=?",new String[]{oldSeries});
	}
	
	public static void clearSeries(Context context){
		ContentValues cv = new ContentValues();
		cv.put("series",ComicLibrary.UKNOWN_SERIES);

		Sqlite db = new Sqlite(MainDB.get()).openWrite();
		db.update("ComicLibrary",cv,null,null);
	}
	//endregion
	
	//region Support Objects
	public static interface SyncCallback{
		public void onSyncProgress(String txt);
		public void onSyncComplete(int status);
	}

    protected static class ComicFindFilter implements java.io.FileFilter{
    	private final String[] mExtList = new String[]{".zip",".cbz",".rar",".cbr"};
    	public boolean accept(File o){
    		if(o.isDirectory()) return true; //Want to allow folders
    		for(String extension:mExtList){
    			if(o.getName().toLowerCase(Locale.getDefault()).endsWith(extension)) return true;
    		}
    		return false;
    	}
    }

    protected static class ThumbFindFilter implements java.io.FileFilter{
    	public boolean accept(File o){
    		if(o.isDirectory()) return false;
    		else if(o.getName().toLowerCase(Locale.getDefault()).endsWith(".jpg")) return true;
    		return false;
    	}
    }
	//endregion
}
