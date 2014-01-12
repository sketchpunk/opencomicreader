package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PrefActivity extends PreferenceActivity{
	/*========================================================
	*/
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.getFragmentManager().beginTransaction().replace(android.R.id.content,new PrefFragment()).commit();
    }//func

    //@Override
    //public void onBuildHeaders(List<Header> target){
    //    loadHeadersFromResource(R.xml.prefheader,target);
    //}//func


	//************************************************************
	//Preference View
	//************************************************************
    public static class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener,OnPreferenceClickListener{
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
    	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
    		//Remove all that padding around the settings.
    	    View view = super.onCreateView(inflater, container, savedInstanceState);
    	    if(view != null){
            	ListView lv = (ListView) view.findViewById(android.R.id.list);
            	if(lv != null) lv.setPadding(0,0,0,0);
            }//if
    	    return view;
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
            
            //.............................................
            //Set Functions
            ((Preference)this.findPreference("funcClearLib")).setOnPreferenceClickListener(this);
            ((Preference)this.findPreference("funcClearSeries")).setOnPreferenceClickListener(this);
            ((Preference)this.findPreference("funcClearCover")).setOnPreferenceClickListener(this);
        }//func

		@Override
		public boolean onPreferenceClick(Preference pref){
			final Context context = this.getActivity();
			
			if(pref.getKey().compareTo("funcClearSeries") == 0){	     		
				sage.ui.Dialogs.ConfirmBox(context,"Reset Series","Are you sure you want to reset all the series names to blank?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearSeries(context); }
				});
			}else if(pref.getKey().compareTo("funcClearLib") == 0){	     		
				sage.ui.Dialogs.ConfirmBox(context,"Clear Library","Are you sure you want to reset the whole library?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearAll(context); }
				});
			}else if(pref.getKey().compareTo("funcClearCover") == 0){	     		
				sage.ui.Dialogs.ConfirmBox(context,"Clear Cover Cache","Are you sure you want to delete all the covers in the cache?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearCovers(context); }
				});
			}//if
			return false;
		}//func
    }//cls
}//cls