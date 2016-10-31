package com.sketchpunk.ocomicreader.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.sketchpunk.ocomicreader.R;

import sage.SageApp;

public class Settings{
	private final SharedPreferences mPref;
	private SharedPreferences.Editor mEditor = null;

	public Settings(Context context){
		mPref = PreferenceManager.getDefaultSharedPreferences(context);
	}//func

	public int getInt(String sName){ return mPref.getInt(sName,0); }
    public String getValue(String sName){ return mPref.getString(sName,""); }
    
    public void saveValue(String sName,String sValue){
        if(mEditor == null) mEditor = mPref.edit();
        mEditor.putString(sName,sValue);
        mEditor.commit();
    }//func
    
    public void saveValue(String sName,int sValue){
    	if(mEditor == null) mEditor = mPref.edit();
        mEditor.putInt(sName,sValue);
        mEditor.commit();
    }//func


	public static String AppFld(String append){
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + SageApp.getContext().getString(R.string.fld_name) + "/";
		if(append != null && !append.isEmpty()) path += append + "/";
		return path;
	}
}//cls
