package com.sketchpunk.ocomicreader.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

import sage.SageApp;
import sage.data.Sqlite;

public class MainDB extends SQLiteOpenHelper{
	//region Singleton Pattern
	private static MainDB mInstance = null;
	public static MainDB get(){
		if(mInstance == null) mInstance = new MainDB(SageApp.getContext());
		return mInstance;
	}//func

	public static File getPath(){ return SageApp.getContext().getDatabasePath(DB_NAME); }

	public static Sqlite openRead(){ return new Sqlite(get()).openRead(); }
	public static Sqlite openWrite(){ return new Sqlite(get()).openWrite(); }
	//endregion

	//region Variables and Constructors
	public static final String DB_NAME = "main.db";
	public static final int DB_VERSION = 2;

	public MainDB(Context context){ super(context,DB_NAME,null,DB_VERSION); }//const
	//endregion

	//region Events
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL("CREATE TABLE IF NOT EXISTS [ComicLibrary]("
				+"comicID VARCHAR(40) PRIMARY KEY NOT NULL"
				+",title VARCHAR(255) NOT NULL,path VARCHAR(255) NOT NULL,isCoverExists INT NOT NULL"
				+",pgCount INT NOT NULL,pgRead INT NOT NULL,pgCurrent INT NOT NULL,series VARCHAR(300) NULL);");

		/*
		Scanner scan = null;
		db.beginTransaction();
		System.out.println("Create Database - Main");
		String sql = "";
		try{
			scan = new Scanner(SageApp.getContext().getAssets().open("main.sql"));
			scan.useDelimiter(";");

			while(scan.hasNext()){
				sql = scan.next().trim();
				System.out.println("EXECUTE - " + sql);
				if(! sql.isEmpty()) db.execSQL(sql);
			}//while

			db.setTransactionSuccessful();
		}catch(IOException e){
			System.out.println("Error on " + sql);
			e.printStackTrace();
		}catch(Exception e){
			System.out.println("Error on " + sql);
			System.out.println(e.getMessage());
			e.printStackTrace();
		}finally{
			if(scan != null) scan.close();
		}//if

		db.endTransaction();
		*/
	}//func

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion,int newVersion){
		switch(oldVersion+1){ //Instead of a loop, try to cascade the update in one swoop.
			case 2:
				db.execSQL("ALTER TABLE ComicLibrary ADD COLUMN series VARCHAR(300) NULL;");
		}//switch
	}
	//endregion
}//cls
