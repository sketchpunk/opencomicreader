package com.sketchpunk.ocomicreader.lib;

import java.io.File;

import java.util.Stack;
import java.util.UUID;

import sage.data.Sqlite;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.sketchpunk.ocomicreader.lib.ComicLibrary.ComicFindFilter;

public class LibrarySync implements Runnable{
	private Context mContext;
	private Sqlite mDb;
	private String mCachePath; //Thumbnail path.
	private String mSyncFld1; //First Sync Folder Setting
	private String mSyncFld2; //Second Sync Folder Setting
	private boolean mIncImgFlds = false; //When crawling for comic files, Include folders that have images in them. 
	private boolean mSkipCrawl = false;  //Skip finding new comics, just process the current library.
	private boolean mUseFldForSeries = false; //User can choose to force series name from the parent folder name instead of using the series parser.
	private int mCoverHeight,mCoverQuality;

	public LibrarySync(Context context){
		mContext = context;
		mCachePath = ComicLibrary.getThumbCachePath();

		//Get sync preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mSyncFld1 = prefs.getString("syncfolder1","");
		mSyncFld2 = prefs.getString("syncfolder2","");
		mIncImgFlds = prefs.getBoolean("syncImgFlds",false);
		mUseFldForSeries = prefs.getBoolean("syncFldForSeries",false);
		
		mCoverHeight = prefs.getInt("syncCoverHeight",300);
		mCoverQuality = prefs.getInt("syncCoverQuality",70);
	}//func
	
	@Override
	public void run(){
		//Check if folders have been setup for sync.
		if(mSyncFld1.isEmpty() && mSyncFld2.isEmpty()){ sendComplete(ComicLibrary.STATUS_NOSETTINGS); return; }//if
		
		//.....................................
		mDb = new Sqlite(mContext); mDb.openWrite();
		try{
			if(!mSkipCrawl){
				crawlComicFiles();
				if(mIncImgFlds) crawlComicFolders();
			}//if
			
			processLibrary();
		}catch(Exception e){
			System.err.println("Sync " + e.getMessage());
			e.printStackTrace();
		}//try

		//.....................................
		//Complete
		mDb.close();
		sendComplete(ComicLibrary.STATUS_COMPLETE);			
		mContext = null;
	}//func
	
	
	/*========================================================
	Send thread safe messages*/
	private void sendProgress(String txt){
		Bundle rtn = new Bundle();
		rtn.putString("msg",txt);
		
		Message msg = new Message();
		msg.what = 1;
		msg.obj = mContext;
		msg.setData(rtn);
		
		ComicLibrary.EventHandler.sendMessage(msg);
	}//func
	
	private void sendComplete(int status){
		Bundle rtn = new Bundle();
		rtn.putInt("status",status);
		
		Message msg = new Message();
		msg.what = 0;
		msg.obj = mContext;
		msg.setData(rtn);
		
		ComicLibrary.EventHandler.sendMessageDelayed(msg,200);
	}//func

	
	/*========================================================
	Finding File/Folders to save into the database.*/
	public int crawlComicFolders(){
		//....................................
		//Setup Variables
		Activity activity = (Activity)mContext;
		String[] cols = new String[]{ MediaStore.Images.Media._ID,
	            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
	            MediaStore.Images.Media.DATE_TAKEN,
	            MediaStore.MediaColumns.DATA};
		
		//....................................
		//Determine the where clause for the media query
		String[] sWhereAry;
		String sWhere;
		
		if(!mSyncFld1.isEmpty() && !mSyncFld2.isEmpty()){
			sWhereAry = new String[]{mSyncFld1+"%",mSyncFld2+"%"};
			sWhere = MediaStore.Images.Media.DATA + " like ? OR "+ MediaStore.Images.Media.DATA +" like ?) GROUP BY (2";
		}else if(!mSyncFld1.isEmpty()){
			sWhereAry = new String[]{mSyncFld1+"%"};
			sWhere = MediaStore.Images.Media.DATA + " like ?) GROUP BY (2";
		}else if(!mSyncFld2.isEmpty()){
			sWhereAry = new String[]{mSyncFld2+"%"};
			sWhere = MediaStore.Images.Media.DATA + " like ?) GROUP BY (2";
		}else{
			return 0;
		}//if
	    
		//....................................
	    //Query both External and Internal then merge results
		Cursor[] aryCur = new Cursor[2];
	    Cursor eCur = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cols,sWhere,sWhereAry,null); //"MAX(datetaken) DESC"
	    Cursor iCur = activity.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,cols,sWhere,sWhereAry,null);
	    
	    eCur.moveToFirst(); aryCur[0] = eCur;
	    iCur.moveToFirst(); aryCur[1] = iCur;
	    MergeCursor mcur = new MergeCursor(aryCur);

	    //Loop through cursor
	    if(mcur.moveToFirst()){
	    	int dCol = mcur.getColumnIndex(MediaStore.MediaColumns.DATA); //.getColumnIndex(MediaStore.MediaColumns.DATA);  	
	    	String path = "",tmp = "";

	    	SQLiteStatement sql = mDb.compileStatement("INSERT INTO ComicLibrary(isCoverExists,pgCount,pgRead,pgCurrent,comicID,title,path,series) VALUES(0,0,0,1,?,?,?,?);");
	    	mDb.beginTransaction();
	    	
	    	do{
	    		//------------------------------
   				//Check if already in library
	    		path = sage.io.Path.removeLast(mcur.getString(dCol)); //Remove Filename from the path
	    		sendProgress(path);
				
				tmp = mDb.scalar("SELECT comicID FROM ComicLibrary WHERE path = '"+path.replace("'","''")+"'",null);
				if(!tmp.isEmpty()) continue;

				//------------------------------
				//Not found, add it to library.
				sql.clearBindings();
				sql.bindString(1,UUID.randomUUID().toString());
				sql.bindString(2,sage.io.Path.getLast(path));
				sql.bindString(3,path);
				sql.bindString(4,ComicLibrary.UKNOWN_SERIES);
				//if(useFolderAsSeries) sql.bindString(4,file.getParentFile().getName());
				//else sql.bindString(4,sParser.get(path)); //sql.bindNull(4);
				
				if(sql.executeInsert() == 0){ System.err.println("ERROR saving folder to db"); }
	    	}while(mcur.moveToNext());
	    	
	    	//............................................
	    	mDb.commit();
			mDb.endTransaction();
	    }//if
	    
	    mcur.close();
	    return ComicLibrary.STATUS_COMPLETE;
	}//func
	
	public int crawlComicFiles(){
		//Crawls the Two paths in settings for comic archive files.
		ComicFindFilter filter = new ComicFindFilter();
	    Stack<String> stack = new Stack<String>();
	    File[] fList;
	    File fObj;
	    String tmp,path;
	    Boolean useFolderAsSeries = false;
	    SeriesParser sParser = new SeriesParser();
	    
    	//............................................
    	//Set initial paths
    	if(!mSyncFld1.isEmpty()) stack.push(mSyncFld1);
    	if(!mSyncFld2.isEmpty()) stack.push(mSyncFld2);
    	if(stack.size() == 0) return ComicLibrary.STATUS_NOSETTINGS;

    	//............................................
    	SQLiteStatement sql = mDb.compileStatement("INSERT INTO ComicLibrary(isCoverExists,pgCount,pgRead,pgCurrent,comicID,title,path,series) VALUES(0,0,0,0,?,?,?,?);");    	
    	mDb.beginTransaction();

    	while(!stack.isEmpty()){
    		//get list and do some validation
    		fObj = new File(stack.pop());
    		if(!fObj.exists()) continue;
    		fList = fObj.listFiles(filter); if(fList == null) continue;

    		//files/dir found.
    		for(File file:fList){
    			if(file.isDirectory()) stack.push(file.getPath()); //add to stack to continue to crawl.
    			else{
    				//------------------------------
    				//Check if already in library
    				sendProgress(file.getName());
    				path = file.getPath();

    				tmp = mDb.scalar("SELECT comicID FROM ComicLibrary WHERE path = '"+path.replace("'","''")+"'",null);
    				if(!tmp.isEmpty()) continue;

    				//------------------------------
    				//Not found, add it to library.
    				sql.clearBindings();
    				sql.bindString(1,UUID.randomUUID().toString());
    				sql.bindString(2,sage.io.Path.removeExt(file.getName()));
    				sql.bindString(3,path);
    				sql.bindString(4,ComicLibrary.UKNOWN_SERIES);

    				//if(useFolderAsSeries) sql.bindString(4,file.getParentFile().getName());
    				//else sql.bindString(4,sParser.get(path)); //sql.bindNull(4);
    				
    				if(sql.executeInsert() == 0){ System.err.println("ERROR saving comic to database"); }
    				//
    			}//if
    		}//for
    	}//while
    	
    	//............................................
    	mDb.commit();
		mDb.endTransaction();
    	
    	return ComicLibrary.STATUS_COMPLETE;
    }//func

	
	/*========================================================
	Process : Create Thumbs, GetPage Count, Remove not found file*/
	private void processLibrary(){
		StringBuilder delList = new StringBuilder(); //Can't delete records with an open cursor, save IDs and delete later.
		//int[] outVar = {0,0}; //PageCount,IsCoverCreated
		String[] comicInfo = {"","",""};//Page Count,Path to Cover Entry,Path to Meta Data
		String[] comicMeta; //Title,Series,Volume,Issue
		File file;
		String tmp,comicID,sql,comicPath,seriesName;
		iComicArchive archive;
		
		Cursor cur = mDb.raw("SELECT comicID,path,isCoverExists,series,title FROM ComicLibrary",null);
		SeriesParser sParser = null;
		
		for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext()){
			comicID = cur.getString(0);
			comicPath = cur.getString(1);
			file = new File(comicPath);
			
			//.........................................
			//if file does not exist, remove from library.
			if(!file.exists()){
				sendProgress("Removing reference to " + cur.getString(1));
				delList.append(",'"+comicID+"'"); //add id to list

				try{ //delete thumbnail if available
					file = new File(mCachePath+comicID+".jpg");
					if(file.exists()) file.delete();
				}catch(Exception e){}
				
				continue;
			}//if
			
			//.........................................
			//if thumb has not been generated.
			if(cur.getString(2).equals("0")){
				sendProgress("Creating thumbnail for " + comicPath);
				archive = ComicLoader.getArchiveInstance(comicPath);
				archive.getLibraryData(comicInfo);
				
				//No images in archive, then delete
				if(comicInfo[0] == "0"){ delList.append(",'"+comicID+"'"); continue; }//if
				
				sql = "pgCount=" + comicInfo[0]; //Start Building the update query.
				//Create ThumbNail
				if(ComicLibrary.createThumb(mCoverHeight,mCoverQuality,archive,comicInfo[1],mCachePath+comicID+".jpg")) sql += ",isCoverExists=1";

				//Get Meta Information
				comicMeta = archive.getMeta(); //Title,Series,Volume,Issue
				if(comicMeta != null){
					if(!comicMeta[0].isEmpty()) sql += ",title = '"+comicMeta[0].replaceAll("'","''")+"'";
					if(!comicMeta[1].isEmpty()) sql += ",series = '"+comicMeta[1].replaceAll("'","''")+"'";
				}//if}

				//Save information to the db.
				mDb.execSql(String.format("UPDATE ComicLibrary SET %s WHERE comicID = '%s'",sql,comicID),null);
				if(comicMeta != null && comicMeta[1] != "") continue; //Since series was updated from meta, Don't continue the rest of the loop which handles the series
			}//if
			
			//.........................................
			//if series does not exist, create a series name.
			seriesName = cur.getString(3);
			if(seriesName == null || seriesName.isEmpty() || seriesName.compareToIgnoreCase(ComicLibrary.UKNOWN_SERIES) == 0){
				if(mUseFldForSeries) seriesName = sage.io.Path.getParentName(comicPath);
				else{
					if(sParser == null) sParser = new SeriesParser(); //JIT
					seriesName = sParser.get(comicPath);
					
					//if seriesName ends up being the path, use the parent folder as the series name.
					if(seriesName == comicPath) seriesName = sage.io.Path.getParentName(comicPath);
				}//if
				
				if(!seriesName.isEmpty()) mDb.execSql(String.format("UPDATE ComicLibrary SET series='%s' WHERE comicID = '%s'",seriesName.replace("'","''"),comicID),null);
			}//if
		}//for
	
		cur.close(); cur = null;
		
		//.........................................
		//if there is a list of items to delete, do it now in one swoop.
		if(delList.length() > 0){
			sendProgress("Cleaning up library...");
			mDb.execSql(String.format("DELETE FROM ComicLibrary WHERE comicID in (%s)",delList.toString().substring(1)),null);
		}//if
	}//func
	
}//cls
