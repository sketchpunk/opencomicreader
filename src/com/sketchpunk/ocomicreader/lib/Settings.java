package com.sketchpunk.ocomicreader.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
}//cls
