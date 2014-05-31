package sage.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqliteHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "main.db";
	public static final int DATABASE_VERSION = 2;

	private static SqliteHelper mInstance = null;

	public static SqliteHelper getInstance(Context ctx) {
		// app context will prevent leaking of activity context
		if (mInstance == null)
			mInstance = new SqliteHelper(ctx.getApplicationContext());
		return mInstance;
	}

	public SqliteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS [ComicLibrary]("
				+ "comicID VARCHAR(40) PRIMARY KEY NOT NULL"
				+ ",title VARCHAR(255) NOT NULL,path VARCHAR(255) NOT NULL,isCoverExists INT NOT NULL"
				+ ",pgCount INT NOT NULL,pgRead INT NOT NULL,pgCurrent INT NOT NULL,series VARCHAR(300) NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (int i = oldVersion + 1; i <= newVersion; i++) {
			switch (i) {
			case 2:
				db.execSQL("ALTER TABLE ComicLibrary ADD COLUMN series VARCHAR(300) NULL;");
				break;
			}
		}
	}
}