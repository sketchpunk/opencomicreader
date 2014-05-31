package sage.data;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class Sqlite {
	private SqliteHelper dbHelper;
	private SQLiteDatabase mDb;

	/*
	 * Main
	 */
	public Sqlite(Context context) {
		dbHelper = SqliteHelper.getInstance(context);
	}

	public void openRead() {
		mDb = dbHelper.getReadableDatabase();
	}

	public void openWrite() {
		mDb = dbHelper.getWritableDatabase();
	}

	public boolean isOpen() {
		return (mDb == null) ? false : mDb.isOpen();
	}

	public void close() {
		if (mDb != null)
			mDb.close();
		if (dbHelper != null)
			dbHelper.close();
	}

	/*
	 * Transactions
	 */
	public void beginTransaction() {
		mDb.beginTransaction();
	}

	public void endTransaction() {
		mDb.endTransaction();
	}

	public void commit() {
		mDb.setTransactionSuccessful();
	}

	public SQLiteStatement compileStatement(String sql) {
		return mDb.compileStatement(sql);
	}

	/*
	 * Static Functions
	 */
	public static int delete(Context c, String tbl, String whereClause,
			String[] args) {
		Sqlite db = new Sqlite(c);

		db.openWrite();
		int rtn = db.delete(tbl, whereClause, args);
		db.close();

		return rtn;
	}

	public static long replace(Context c, String sTable, ContentValues data) {
		Sqlite db = new Sqlite(c);

		db.openWrite();
		long rtn = db.replace(sTable, data);
		db.close();

		return rtn;
	}

	public static String scalar(Context c, String sql, String[] args) {
		Sqlite db = new Sqlite(c);

		db.openRead();
		String rtn = db.scalar(sql, args);
		db.close();

		return rtn;
	}

	public static Map<String, String> scalarRow(Context c, String sql,
			String[] args) {
		Sqlite db = new Sqlite(c);

		db.openRead();
		Map<String, String> rtn = db.scalarRow(sql, args);
		db.close();

		return rtn;
	}

	public static long insert(Context c, String sTable, ContentValues cv) {
		Sqlite db = new Sqlite(c);

		db.openWrite();
		long rtn = db.insert(sTable, cv);
		db.close();

		return rtn;
	}

	public static int update(Context c, String sTable, ContentValues cv,
			String whereClause, String[] whereArgs) {
		Sqlite db = new Sqlite(c);

		db.openWrite();
		int rtn = db.update(sTable, cv, whereClause, whereArgs);
		db.close();

		return rtn;
	}

	public static void execSql(Context c, String sql, String[] args) {
		Sqlite db = new Sqlite(c);
		db.openWrite();
		db.execSql(sql, args);
		db.close();
	}

	/*
	 * Modifying db operations
	 */
	public void execSql(String sql, String[] args) {
		try {
			if (args == null)
				mDb.execSQL(sql);
			else
				mDb.execSQL(sql, args);
		} catch (Exception e) {
			System.out.println("DB ExecSQL Fail : " + e.getMessage());
		}
	}

	public long replace(String sTable, ContentValues data) {
		return mDb.replace(sTable, null, data);
	}

	public int delete(String tblName, String whereClause, String[] args) {
		int rtn = 0;

		try {
			rtn = mDb.delete(tblName, whereClause, args);
		} catch (Exception e) {
			System.out.println("SQL Error : " + e.getMessage());
		}

		return rtn;
	}

	public long insert(String sTable, ContentValues cv) {
		return mDb.insert(sTable, null, cv);
	}

	public int update(String sTable, ContentValues cv, String where,
			String[] whereArgs) {
		return mDb.update(sTable, cv, where, whereArgs);
	}

	/*
	 * Get db operations
	 */
	public Cursor raw(String sql, String[] args) {
		Cursor rtn = null;

		try {
			rtn = mDb.rawQuery(sql, args);
		} catch (Exception e) {
			System.out.println("SQL Raw Error " + e.getMessage());
		}

		return rtn;
	}

	public String scalar(String sql, String[] args) {
		String rtn = "";
		Cursor cur = null;
		try {
			cur = mDb.rawQuery(sql, args);
			if (cur != null) {
				if (cur.moveToFirst())
					rtn = cur.getString(0);
			}
		} catch (Exception e) {
			System.out.println("scalar error " + e.getMessage());
		} finally {
			if (cur != null) {
				if (!cur.isClosed())
					cur.close();
			}
		}

		return rtn;
	}

	public ArrayList<String> scalarCol(String sql, String[] args, String colName) {
		ArrayList<String> rtn = new ArrayList<String>();

		Cursor cur = raw(sql, args);
		if (cur != null) {
			int col = cur.getColumnIndex(colName);
			for (boolean isOk = cur.moveToFirst(); isOk; isOk = cur
					.moveToNext())
				rtn.add(cur.getString(col));
			cur.close();
		}

		return rtn;
	}

	public Map<String, String> scalarRow(String sql, String[] args) {
		Map<String, String> rtn = new HashMap<String, String>();
		try {
			Cursor cur = mDb.rawQuery(sql, args);
			if (cur != null) {
				if (cur.moveToFirst()) {
					for (int i = 0; i < cur.getColumnCount(); i++) {
						rtn.put(cur.getColumnName(i), cur.getString(i));
					}
				}
				cur.close();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return rtn;
	}

}