package sage;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class SageApp extends Application{
	private static Context mContext = null;
	private Thread.UncaughtExceptionHandler mDefaultExceptionHandler = null;

	public void onCreate(){
		super.onCreate();
		SageApp.mContext = getApplicationContext();

		//If not debugging, Control the Unhandled Event Handling.
		//if(!Util.isDebugging()){
		//	mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler(); //Save Reference to default.
		//	Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
		//}//if
	}//func

	//region Static Global Functions
	public static Context getContext(){
		return SageApp.mContext;
	}//func
	//endregion

	//region Get Application Version
	public static String getVersionName(){
		if(mContext == null) return "";

		try{ return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),0).versionName;
		}catch(PackageManager.NameNotFoundException e){ e.printStackTrace(); }//try
		return "";
	}//func

	public static int getVersionCode(){
		if(mContext == null) return 0;

		try{ return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),0).versionCode;
		}catch(PackageManager.NameNotFoundException e){ e.printStackTrace(); }//try
		return 0;
	}//func

	public static String[] getVersion(){
		if(mContext == null) return null;

		try{
			PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),0);
			return new String[]{ pInfo.versionName, Integer.toString(pInfo.versionCode) };
		}catch(PackageManager.NameNotFoundException e){ e.printStackTrace(); }//try
		return null;
	}//func
	//endregion

	//region Error Handling
	//http://stackoverflow.com/questions/8943288/how-to-implement-uncaughtexception-android
	/*
	private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler(){
		@Override public void uncaughtException(Thread thread, Throwable throwable){
			//Logger.error("Uncaught.Exception",throwable);

			//Pass this exception back to the system (very important).
			if(mDefaultExceptionHandler != null) mDefaultExceptionHandler.uncaughtException(thread,throwable);
		}
	};
	*/
	//endregion

}
