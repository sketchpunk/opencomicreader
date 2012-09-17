package sage.data;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseUtils.InsertHelper;

public class Sqlite{
	private SqliteHelper dbHelper;
	private SQLiteDatabase mDb;
	
	/*========================================================
	Main*/
	public Sqlite(Context context){ dbHelper = SqliteHelper.getInstance(context); }//func

	public void openRead(){ mDb = dbHelper.getReadableDatabase(); } //throws SQLException 
	public void openWrite(){ mDb = dbHelper.getWritableDatabase(); }

	public boolean isOpen(){ return (mDb == null)? false : mDb.isOpen(); }//func
	
	public void close(){
		if(mDb != null) mDb.close();
		if(dbHelper != null) dbHelper.close();
	}//func

	
	/*========================================================
	Transactions*/
	public void beginTransaction(){ mDb.beginTransaction(); }//func
	public void endTransaction(){ mDb.endTransaction(); }//func
	public void commit(){ mDb.setTransactionSuccessful(); }//func
	

	/*========================================================
	*/
	public InsertHelper getInsertHelper(String tblName){
		return new InsertHelper(mDb,tblName);
	}//func
	

	/*========================================================
	Static Functions*/
	public static int delete(Context c,String tbl,String whereClause,String[] args){
		Sqlite db = new Sqlite(c);
		
		db.openWrite();
		int rtn = db.delete(tbl,whereClause,args);
		db.close();
		
		return rtn;
	}//func
	
	public static long replace(Context c,String sTable,ContentValues data){
		Sqlite db = new Sqlite(c);
		
		db.openWrite();
		long rtn = db.replace(sTable,data);
		db.close();
		
		return rtn;
	}//func
	
	public static String scalar(Context c,String sql,String[] args){
		Sqlite db = new Sqlite(c);
		
		db.openRead();
		String rtn = db.scalar(sql,args);
		db.close();
		
		return rtn;
	}//func

	public static Map<String,String> scalarRow(Context c,String sql,String[] args){
		Map<String,String> rtn = new HashMap<String,String>();
		Sqlite db = new Sqlite(c);
		
		db.openRead();
		Cursor cur = db.raw(sql,args);
		
		if(cur != null){
			if(cur.moveToFirst()){
				for(int i = 0; i < cur.getColumnCount(); i++){
					rtn.put(cur.getColumnName(i),cur.getString(i));
				}//for
			}//if
			cur.close();
		}//if
		
		db.close();
		return rtn;
	}//func
	
	public static long insert(Context c,String sTable,ContentValues cv){
		Sqlite db = new Sqlite(c);
		
		db.openWrite();
		long rtn = db.insert(sTable,cv);
		db.close();
		
		return rtn;
	}//func
	
	
	/*========================================================
	Modifying db operations*/
	public void execSql(String sql,String[] args){
		try{
			if(args == null) mDb.execSQL(sql);
			else mDb.execSQL(sql,args);
		}catch(Exception e){
			System.out.println("DB ExecSQL Fail : " + e.getMessage());
		}//try
	}//func
	
	public long replace(String sTable,ContentValues data){
		return mDb.replace(sTable,null,data);
	}//func
	
	public int delete(String tblName,String whereClause,String[] args){
		int rtn = 0;
		
		try{
			rtn = mDb.delete(tblName,whereClause,args);
		}catch(Exception e){
			System.out.println("SQL Error : " + e.getMessage());
		}//if

		return rtn;
	}//func
	
	public long insert(String sTable,ContentValues cv){
		return mDb.insert(sTable,null,cv);
	}//func
	
	
	/*========================================================
	Get db operations*/
	public Cursor raw(String sql,String[] args){
		Cursor rtn = null;
		
		try{
			rtn = mDb.rawQuery(sql, args);
		}catch(Exception e){
			System.out.println("SQL Raw Error " + e.getMessage());
		}//try
		
		return rtn;
	}//func

	public String scalar(String sql,String[] args){
		String rtn = "";
		Cursor cur = null;
		try{
			cur = mDb.rawQuery(sql,args);
			if(cur != null){
				if(cur.moveToFirst()) rtn = cur.getString(0);
			}//if
		}catch(Exception e){
			System.out.println("scalar error " + e.getMessage());
		}finally{
			if(cur != null){
				if(!cur.isClosed()) cur.close();
			}//if
		}//try
	
		return rtn;
	}//func
	
	public ArrayList<String> scalarCol(String sql,String[] args,String colName){
		ArrayList<String> rtn = new ArrayList<String>();
		
		Cursor cur = raw(sql,args);
		if(cur != null){
			int col = cur.getColumnIndex(colName);
			for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext()) rtn.add(cur.getString(col));
			cur.close();
		}//if

		return rtn;
	}//func
	

	/*========================================================
	
	public void insertSomething(String a, String b, String c){
		ContentValues con = new ContentValues();
		con.put("_id",a);
		openWrite();
		gDb.insert("some table",null,con);
		gDb.update("table",con, "a=1",null);
		gDb.delete("table", "id = 1",null);
		close();
		
		//db.query(false, "table", new String[]{}, null, null, null, null, null, null);
	}//func
	*/
}//cls