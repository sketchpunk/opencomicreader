package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.StringBuilder;
import java.util.Locale;
import java.util.Stack;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import sage.data.Sqlite;

public class ComicLibrary{
	private static Thread mWorkerThread = null;
	
	/*========================================================
	status constants*/
	public final static int STATUS_NOSETTINGS = 1;
	public final static int STATUS_COMPLETE = 0;

	/*========================================================
	 * Comic Database contract constants
	 */
	public final static String DB_TABLE_NAME_COMIC = "ComicLibrary";
	public final static String DB_COLUMN_NAME_COMICID = "comicID";
	public final static String DB_COLUMN_NAME_TITLE = "title";
	public final static String DB_COLUMN_NAME_PATH = "path";
	public final static String DB_COLUMN_NAME_PGCOUNT = "pgCount";
	public final static String DB_COLUMN_NAME_PGREAD = "pgRead";
	public final static String DB_COLUMN_NAME_PGCURRENT = "pgCurrent";
	public final static String DB_COLUMN_NAME_ISCOVEREXISTS = "isCoverExists";
	public final static String DB_COLUMN_NAME_SERIES = "series";
	
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
    			if(cb != null) cb.OnSyncProgress(data.getString("msg"));
    		break;
    			
       		//...............................
    		case 0: //complete
    			if(cb != null) cb.onSyncComplete(data.getInt("status"));
    			mWorkerThread = null;
    		break;
    	}//switch
    }//func

    
    /*========================================================
	methods*/
    public static void removeComic(Context context,String id,boolean delComic){
    	File file;
    	sage.data.Sqlite db = new sage.data.Sqlite(context);
    	db.openWrite();
    	
    	if(delComic){ //Delete comic from device
			String comicPath = db.scalar("SELECT "+DB_COLUMN_NAME_PATH+" FROM "+DB_TABLE_NAME_COMIC+" WHERE "+DB_COLUMN_NAME_COMICID+" = '"+id.replace("'","''")+"';",null);
    		if(comicPath != null && !comicPath.isEmpty()){
    			file = new File(comicPath);
    			if(file.exists()) file.delete();
    		}//if
    	}//if
    	
    	//Delete comic from library
		if(db.delete(DB_TABLE_NAME_COMIC, DB_COLUMN_NAME_COMICID+" = '"+id.replace("'","''")+"'",null) > 0){
    		file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/" + id +".jpg");
    		if(file.exists()) file.delete();
    	}//if
    	
    	db.close();
    }//func
    
    public static void setComicProgress(Context context,String id,int state,boolean applySeries){
		String sql = "UPDATE "+DB_TABLE_NAME_COMIC+" SET ";
		sql += (state == 0)?DB_COLUMN_NAME_PGREAD+"=0,"+DB_COLUMN_NAME_PGCURRENT+"=0":DB_COLUMN_NAME_PGREAD+"="+DB_COLUMN_NAME_PGCOUNT+","+DB_COLUMN_NAME_PGCURRENT+"="+DB_COLUMN_NAME_PGCOUNT; //0-Reset or 1-Mark as Read.
    	
		if(applySeries) sql += " WHERE "+DB_COLUMN_NAME_SERIES+" in (SELECT "+DB_COLUMN_NAME_SERIES+" FROM "+DB_TABLE_NAME_COMIC+" WHERE "+DB_COLUMN_NAME_COMICID+" = '"+id+"')";
		else sql += " WHERE "+DB_COLUMN_NAME_COMICID+" = '"+id+"'";
    	
    	sage.data.Sqlite.execSql(context,sql,null);
    }//func


	/*========================================================
	sync methods*/
    public static boolean startSync(Context context){
		//...............................
    	if(mWorkerThread != null){
    		if(mWorkerThread.isAlive()) return false;
    	}//func
    	
    	//...............................
		mWorkerThread = new Thread(new SyncRunnable(context));
		mWorkerThread.start();
		return true;
    }//func
    
	public static boolean clearAll(Context context){
		//................................................
		//Delete thumbnails
		String cachePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs";

		File fObj = new File(cachePath);
		File[] fList = fObj.listFiles(new ThumbFindFilter());
		if(fList != null){
			for(File file:fList) file.delete();
		}//if

		//................................................
		Sqlite.delete(context,"ComicLibrary","",null);
		return true;
	}//func
	
	
	//************************************************************
	//Import comics into Library
	//************************************************************
	protected static class SyncRunnable implements Runnable{
		private Context mContext;
		private Sqlite mDb;
		private String mCachePath;

		public SyncRunnable(Context context){
			mContext = context;
			
			//........................................
			//Make sure the cache folder exists.
			mCachePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/";
			File file = new File(mCachePath);
	        if(!file.exists()) file.mkdirs();

			//........................................
	        //Create nomedia file so thumbs aren't indexed for gallery
	        file = new File(mCachePath,".nomedia");
	        if (!file.exists()){
	            try{ file.createNewFile(); }catch (Exception e){}
	        }//if
		}//func

		@Override
		public void run(){
			int status = ComicLibrary.STATUS_COMPLETE;
			mDb = new Sqlite(mContext); mDb.openWrite();

			//.....................................
			try{
				if((status = crawlComics()) == 0) processLibrary();
			}catch(Exception e){
				System.out.println("Sync " + e.getMessage());
			}//try

			//.....................................
			//Complete
			mDb.close();
			sendComplete(status);			
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
		Process : Crawl for comics */
		
		//Crawls the Two paths in settings for comic archive files.
		private int crawlComics(){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			ComicFindFilter filter = new ComicFindFilter();
	    	Stack<String> stack = new Stack<String>();
	    	File[] fList;
	    	File fObj;
	    	String tmp,path;

	    	//............................................
	    	//Set initial paths
	    	tmp = prefs.getString("syncfolder1","");
	    	if(!tmp.isEmpty()) stack.push(tmp);
	    	
	    	tmp = prefs.getString("syncfolder2","");
	    	if(!tmp.isEmpty()) stack.push(tmp);
	    	
	    	if(stack.size() == 0) return ComicLibrary.STATUS_NOSETTINGS;
			
	    	//............................................
	    	//setup db stuff.
	        SeriesParser sParser = new SeriesParser();
	        
	        //TODO: InsertHelper has been deprecated in API17. Google has no replacement for it's bulk insert functionality.
	        //Research other ways for efficiently bulk inserts, Maybe something with Transactions.
			InsertHelper dbInsert = mDb.getInsertHelper(DB_TABLE_NAME_COMIC);
	    	
			int iComicID = dbInsert.getColumnIndex(DB_COLUMN_NAME_COMICID);
			int iTitle = dbInsert.getColumnIndex(DB_COLUMN_NAME_TITLE);
			int iPath = dbInsert.getColumnIndex(DB_COLUMN_NAME_PATH);
			int iPgCount = dbInsert.getColumnIndex(DB_COLUMN_NAME_PGCOUNT);
			int iPgRead = dbInsert.getColumnIndex(DB_COLUMN_NAME_PGREAD);
			int iPgCurrent = dbInsert.getColumnIndex(DB_COLUMN_NAME_PGCURRENT);
			int iIsCoverExists = dbInsert.getColumnIndex(DB_COLUMN_NAME_ISCOVEREXISTS);
			int iSeries = dbInsert.getColumnIndex(DB_COLUMN_NAME_SERIES);
			
			mDb.beginTransaction();

			//............................................
	    	while(!stack.isEmpty()){
	    		//get list and do some validation
	    		fObj = new File(stack.pop());
	    		if(!fObj.exists()) continue;
	    		fList = fObj.listFiles(filter); if(fList == null) continue;

	    		//files/dir found.
	    		for(File file:fList){
	    			if(file.isDirectory()){
	    				stack.push(file.getPath()); //add to stack to continue to crawl.
	    			}else{
	    				//------------------------------
	    				//Check if already in library
	    				sendProgress(file.getName());
	    				path = file.getPath(); //System.out.println(path);

						tmp = mDb.scalar("SELECT "+DB_COLUMN_NAME_COMICID+" FROM "+DB_TABLE_NAME_COMIC+" WHERE "+DB_COLUMN_NAME_PATH+" = '"+path.replace("'","''")+"'",null);
	    				if(!tmp.isEmpty()) continue;

	    				//------------------------------
	    				//Not found, add it to library.
	    				tmp = sage.io.Path.removeExt(file.getName());
	    				dbInsert.prepareForInsert();
						dbInsert.bind(iComicID,UUID.randomUUID().toString());
	    				dbInsert.bind(iTitle,tmp);
	    				dbInsert.bind(iPath,path);
	    				dbInsert.bind(iPgCount,0);
	    				dbInsert.bind(iPgRead,0);
	    				dbInsert.bind(iPgCurrent,1);
	    				dbInsert.bind(iIsCoverExists,0);
						dbInsert.bind(iSeries,sParser.get(path));
	    				
						if(dbInsert.execute() == -1){System.out.println("ERROR");}//if
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
		
		//look at what library items don't have covers yet and process it. If file not found, remove from library
		private void processLibrary(){
			StringBuilder delList = new StringBuilder(); //Can't delete records with an open cursor, save IDs and delete later.
			int[] outVar = {0,0}; //PageCount,IsCoverCreated
			File file;
			String tmp;
			Cursor cur = mDb.raw("SELECT "+DB_COLUMN_NAME_COMICID+","+
					DB_COLUMN_NAME_PATH+","+DB_COLUMN_NAME_ISCOVEREXISTS+","+
					DB_COLUMN_NAME_SERIES+","+DB_COLUMN_NAME_TITLE+
					" FROM "+DB_TABLE_NAME_COMIC+"",null);
			SeriesParser sParser = null;
			
			for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext()){
				file = new File(cur.getString(1));
				
				//.........................................
				//if file does not exist, remove from library.
				if(!file.exists()){
					sendProgress("Removing reference to " + cur.getString(1));
					
					if(delList.length() != 0) delList.append(",");
					delList.append("'"+cur.getString(0)+"'");
					
					//delete thumbnail if available
					try{
						file = new File(mCachePath + cur.getString(0) + ".jpg");
						if(file.exists()) file.delete();
					}catch(Exception e){}
					
					continue;
				}//if

				//.........................................
				//if cover exists, then it's been processed.
				if(cur.getString(2).equals("0")){
					sendProgress("Cover for " + cur.getString(1));
					processArchive(cur.getString(0),cur.getString(1),outVar);
					
					if(outVar[0] > 0){//if pagecnt is at least 1, update library.
						mDb.execSql(String.format("UPDATE "+
								DB_TABLE_NAME_COMIC+" SET "+
								DB_COLUMN_NAME_PGCOUNT+"=%d,"+
								DB_COLUMN_NAME_ISCOVEREXISTS+"=%d WHERE "+
								DB_COLUMN_NAME_COMICID+" = '%s'",
								outVar[0],outVar[1],cur.getString(0)),null);
					}else{ //if no pages found, its not a comic archive. Delete.
						if(delList.length() != 0) delList.append(",");
						delList.append("'"+cur.getString(0)+"'");
					}//if
				}//if
				
				//.........................................
				//if series does not exist, process the title for one
				tmp = cur.getString(3);
				if(tmp == null || tmp.isEmpty()){
					if(sParser == null) sParser = new SeriesParser(); //JIT
					tmp = cur.getString(4);
					if(tmp == null || tmp.isEmpty()) continue;
					
					tmp = sParser.get(tmp);
					if(!tmp.isEmpty()){
						mDb.execSql(String.format("UPDATE "+
								DB_TABLE_NAME_COMIC+" SET "+
								DB_COLUMN_NAME_SERIES+"='%s' WHERE "+
								DB_COLUMN_NAME_COMICID+" = '%s'",
								tmp.replace("'","''"),cur.getString(0)),null);
					}//if
				}//if
			}//for
			cur.close(); cur = null;
			
			//.........................................
			//if there is a list of items to delete, do it now in one swoop.
			if(delList.length() > 0){
				sendProgress("Cleaning up library...");
				mDb.execSql(String.format("DELETE FROM "+DB_TABLE_NAME_COMIC+
						" WHERE "+DB_COLUMN_NAME_COMICID+
						" in (%s)",delList.toString()),null);
			}//if
		}//func

		//look through archive for page count and the first image to use as a cover.
		private void processArchive(String fileID,String path,int[] outVar){
			outVar[0] = 0; //Page Count
			outVar[1] = 0; //Cover Created
			iComicArchive archive = ComicLoader.getArchiveInstance(path);
			
			//.............................................
			if(archive != null){
				String[] data = {"0",""};
				if(archive.getLibraryData(data)){
					outVar[0] = Integer.parseInt(data[0]);
					
					//if a page is found, make a thumb out of it.
					if(!data[1].equals("")){
						System.out.println("Cover " + data[1]);
						System.out.println("Pg Len " + data[0]);
						outVar[1] = (createThumb(archive,data[1],fileID))?1:0;
					}else outVar[1] = 0;
				}//if

				archive.close();
			}//if
		}//func

		//stream page out of archive and resize to use as a thumb
		private boolean createThumb(iComicArchive archive,String coverPath,String fileID){
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
					int scale = (bmpOption.outHeight > 200)? Math.round((float)bmpOption.outHeight/200) : 0;
					bmpOption.inSampleSize = scale;
					bmpOption.inJustDecodeBounds = false;
					
					//....................................
					//Load bitmap and rescale
					iStream.close(); //the first read should of closed the stream. Just do it just incase it didn't
					iStream = archive.getItemInputStream(coverPath);
					bmp = BitmapFactory.decodeStream(iStream,null,bmpOption);
					
					//....................................
					//Save bitmap to file
					File file = new File(mCachePath + fileID + ".jpg");
					FileOutputStream out = new FileOutputStream(file);
					bmp.compress(Bitmap.CompressFormat.JPEG,70,out);

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
	}//cls

	
	//************************************************************
	// Support Objects
	//************************************************************
	public static interface SyncCallback{
		public void OnSyncProgress(String txt);
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
