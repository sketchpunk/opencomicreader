package com.sketchpunk.ocomicreader;

import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class PrefActivity extends PreferenceActivity{
	/*========================================================
	*/
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }//func

    @Override
    public void onBuildHeaders(List<Header> target){
        loadHeadersFromResource(R.xml.prefheader,target);
    }//func


	//************************************************************
	//Preference View
	//************************************************************
    public static class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
    	private SharedPreferences prefs;
    	
    	@Override
    	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key){
    		//Update summary with new selected folder
    		if(key.equals("syncfolder1") || key.equals("syncfolder2")){
    			Preference p = this.findPreference(key);
    			p.setSummary(prefs.getString(key,""));
    		}//if
    	}//func

    	@Override
    	public void onDestroy(){
    		prefs.unregisterOnSharedPreferenceChangeListener(this);
    		super.onDestroy();
    	}//func
    	
    	@Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            
            //.............................................
            prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);

            //.............................................
            //Bundle args = this.getArguments();
            //String val = args.getString("test");
            //System.out.println(val);

            //.............................................
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.prefs);
            
            //.............................................
            //Set current values to summary.
            Preference p = this.findPreference("syncfolder1");
            if(p != null) p.setSummary(prefs.getString("syncfolder1","- no folder selected -"));
            
            p = this.findPreference("syncfolder2");
            if(p != null) p.setSummary(prefs.getString("syncfolder2","- no folder selected -"));
        }//func
    }//cls
}//cls