package sage.data;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.DatabaseUtils.InsertHelper;

//region *** Notes ***
// Delete a database : zApp.getContext().deleteDatabase(dbName);
//http://sqlcipher.net/open-source
//https://github.com/sqlcipher/android-database-sqlcipher
//https://github.com/commonsguy/cwac-loaderex

/*
    Sqlite db = new Sqlite(new QueueDB());
    ContentValues values = new ContentValues();

    values.put(KEY_DESC, alarm.getDesc());
    values.put(KEY_REPEAT_DAY, alarm.getRepeatDay());
    values.put(KEY_REPEAT_TYPE, alarm.getRepeatType());
    values.put(KEY_CALENDAR, Long.toString(alarm.getCalendarInMillis()));
    values.put(KEY_APP, alarm.getApp());
    values.put(KEY_ACTIVE, alarm.getActive());

    db.insert(TABLE_NAME, null, values);
    db.close();
*/
//endregion

public class Sqlite{
	//region Vars, Constructors
	private SQLiteOpenHelper mDbHelper;
	private SQLiteDatabase mDb = null;
	private boolean mIsTransactionOpen = false;

	public Sqlite(SQLiteOpenHelper dbHelper){ mDbHelper = dbHelper; }//func
	//endregion

	//region Main Functions
	public Sqlite openRead(){ mDb = mDbHelper.getReadableDatabase(); return this;} //throws SQLException
	public Sqlite openWrite(){ mDb = mDbHelper.getWritableDatabase(); return this;}

	public boolean isOpen(){ return (mDb != null && mDb.isOpen()); }//func

	public void close(){
		if(mDb != null && mDb.isOpen()){
			if(mIsTransactionOpen) mDb.endTransaction();
			mDb.close();
		}//if
		if(mDbHelper != null) mDbHelper.close();
	}//func

	public SQLiteStatement compileStatement(String sql){ return mDb.compileStatement(sql); }
	//endregion

	//region Transactions
	public void beginTransaction(){ mDb.beginTransaction(); mIsTransactionOpen = true; }//func
	public void endTransaction(){ mDb.endTransaction(); mIsTransactionOpen = false; }//func
	public void commit(){ if(mIsTransactionOpen) mDb.setTransactionSuccessful(); }//func
	//endregion

	//region Modifying db operations
	public boolean execSql(String sql,String[] args){
		try{
			if(args == null) mDb.execSQL(sql);
			else mDb.execSQL(sql,args);
		}catch(Exception e){
			//Logger.error("DB ExecSql Fail", e);
			return false;
		}//try
		return true;
	}//func

	public long replace(String sTable,ContentValues data){
		return mDb.replace(sTable,null,data);
	}//func

	public int delete(String tblName,String whereClause,String[] args){
		int rtn = 0;

		try{
			rtn = mDb.delete(tblName,whereClause,args);
		}catch(Exception e){
			//Logger.error("DB Delete Fail", e);
			rtn = -1;
		}//if

		return rtn;
	}//func

	public long insert(String sTable,ContentValues cv){
		if(mDb.isReadOnly()){
			//Logger.error("------------------Database is set to read only-------");
			return 0;
		}//if
		return mDb.insert(sTable,null,cv);
	}//func

	public int update(String sTable,ContentValues cv,String where,String[] whereArgs){
		return mDb.update(sTable,cv,where,whereArgs);
	}//func
	//endregion

	//region Get db operations
	public Cursor raw(String sql,String[] args){
		Cursor rtn = null;

		try{
			rtn = mDb.rawQuery(sql, args);
		}catch(Exception e){
			//Logger.error("DB Raw Fail", e);
		}//try

		return rtn;
	}//func

	public String scalar(String sql,String[] args){
		if(mDb == null){
			//Logger.error("Scalar DB Not open"); return "";
		}//if

		String rtn = "";
		Cursor cur = null;
		try{
			cur = mDb.rawQuery(sql,args);
			if(cur != null){
				if(cur.moveToFirst()) rtn = cur.getString(0);
			}//if
		}catch(Exception e){
			//Logger.error("Error Scalar",e);
		}finally{
			if(cur != null && !cur.isClosed()) cur.close();
		}//try

		return rtn;
	}//func

	public ArrayList<String> scalarCol(String sql,String[] args,String colName){
		ArrayList<String> rtn = new ArrayList<>();
		Cursor cur = null;
		try{
			cur = raw(sql,args);
			if(cur != null){
				int col = cur.getColumnIndex(colName);
				for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext())
					rtn.add(cur.getString(col));

				cur.close(); cur = null;
			}//if
		}catch(Exception e){
			//Logger.error("Error Scalar Col",e);
		}finally{
			if(cur != null && !cur.isClosed()) cur.close();
		}//try

		return rtn;
	}//func

	public Map<String,String> scalarRow(String sql,String[] args){
		Map<String,String> rtn = null;
		try{
			Cursor cur = mDb.rawQuery(sql,args);
			if(cur != null){
				if(cur.moveToFirst()){
					rtn = new HashMap<>();
					for(int i = 0; i < cur.getColumnCount(); i++){
						rtn.put(cur.getColumnName(i),cur.getString(i));
					}//for
				}//if
				cur.close();
			}//if
		}catch(Exception e){
			//Logger.error("Error Scalar Row",e);
		}//func

		return rtn;
	}//func

	//Returns[COLS][ROWS]
	public String[][] arrayCols(String sql,String[] args){
		String[][] rtn = null;
		try{
			Cursor cur = mDb.rawQuery(sql,args);
			if(cur != null){
				int rowCnt = cur.getCount(), colCnt = cur.getColumnCount();

				if(rowCnt > 0 && colCnt > 0 && cur.moveToFirst()){
					int row = 0;
					rtn = new String[colCnt][];

					for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext()){
						for(int c = 0; c < colCnt; c++){
							if(row == 0) rtn[c] = new String[rowCnt];
							rtn[c][row] = cur.getString(c);
						}//for
						row++;
					}//for
				}//of

				cur.close();
			}//if
		}catch(Exception e){
			System.out.println(e.getMessage());
			//Logger.error("SQLITE ArrayCol",e);
		}//func

		return rtn;
	}//func

	//Returns[ROWS][COLS]
	public String[][] arrayRows(String sql,String[] args){
		String[][] rtn = null;
		try{
			Cursor cur = mDb.rawQuery(sql,args);
			if(cur != null){
				int rowCnt = cur.getCount(), colCnt = cur.getColumnCount();

				if(rowCnt > 0 && colCnt > 0 && cur.moveToFirst()){
					int row = -1;
					rtn = new String[rowCnt][];

					for(boolean isOk = cur.moveToFirst(); isOk; isOk = cur.moveToNext()){
						rtn[++row] = new String[colCnt];
						for(int c = 0; c < colCnt; c++) rtn[row][c] = cur.getString(c);
					}//for
				}//of

				cur.close();
			}//if
		}catch(Exception e){
			//Logger.error("Error Array Row",e);
		}//func

		return rtn;
	}//func
	//endregion
}//cls