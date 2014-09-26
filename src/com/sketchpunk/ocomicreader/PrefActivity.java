package com.sketchpunk.ocomicreader;

import java.util.List;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class PrefActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener,Preference.OnPreferenceChangeListener{
	private static final boolean ALWAYS_SINGLE_PANE = false; //Disables Two-Pane for Tablets.
	
	//---------------------------------------------------------------------------------
	//Events
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		//prefs.registerOnSharedPreferenceChangeListener(this);
		
		//If single plane, Add the Fragment that will combined all the others.
		if(isSinglePane(this)) this.getFragmentManager().beginTransaction().replace(android.R.id.content,new CombinedFragment()).commit();
		else{
			//Remove some padding from the headers.
			ListView lv = (ListView)findViewById(android.R.id.list);
			if(lv != null) lv.setPadding(0,0,0,0);
		}//if
	}//func

    @Override
    public boolean onIsMultiPane(){ return isXLargeTablet(this) && !isSinglePane(this); }

	@Override
	public void onBuildHeaders(List<Header> target){
		if(isSinglePane(this)) return;

		SyncPreferenceFragment.appendHeader(target);
		LibraryPreferenceFragment.appendHeader(target);
		ViewPreferenceFragment.appendHeader(target);
	   	FuncPreferenceFragment.appendHeader(target);
	}//func
	
	//---------------------------------------------------------------------------------
	@Override
	protected boolean isValidFragment(String fragmentName){
		//if(MYFRAGMENT.class.getName().equals(fragmentName)) return true;
		//TODO, Quick fix for an error someone is having. Its security in 4.4. Return true is lazy and will allow Fragment Injection Attacks.
		//So fix this to test if the fragment name is one of the fragments this activity loads.
		return true;
	}//func
	
	
	//---------------------------------------------------------------------------------
	//HELPER
    //determine if the device has an extra-large screen.
    private static boolean isXLargeTablet(Context context){
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }//func

    private static boolean isSinglePane(Context context){
        return ALWAYS_SINGLE_PANE || !isXLargeTablet(context);
    }//func
    
    @Override
	public boolean onPreferenceClick(Preference pref){
    	String key = pref.getKey();
    	final Context context = this;
    	
		if(key.equals("funcClearSeries")){	     		
			sage.ui.Dialogs.ConfirmBox(context,"Reset Series","Are you sure you want to reset all the series names to blank?",new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearSeries(context); }
			});
		}else if(key.equals("funcClearLib")){	     		
			sage.ui.Dialogs.ConfirmBox(context,"Clear Library","Are you sure you want to reset the whole library?",new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearAll(context); }
			});
		}else if(key.equals("funcClearCover")){	     		
			sage.ui.Dialogs.ConfirmBox(context,"Clear Cover Cache","Are you sure you want to delete all the covers in the cache?",new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog,int id){ ComicLibrary.clearCovers(context); }
			});
		}//if
    	
		return false;
	}//func

	//A preference value change listener that updates the preference's summary to reflect its new value.
	@Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        
		//.............................................
        if(preference instanceof ListPreference){
            // For list preferences, look up the correct display value in the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary((index >= 0)?listPreference.getEntries()[index]:null);

		//.............................................
        }else preference.setSummary(stringValue); // For all other preferences, set the summary to the value's simple string representation.
        
        return true;
    }//func
	
	
	//---------------------------------------------------------------------------------
	//Preference Fragments
    public static class PrefFragment extends PreferenceFragment{
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

	    void bindPreferenceClick(CharSequence prefName){
			Preference pref = findPreference(prefName);
			if(pref == null){ System.out.println("Pref Not Found " +  prefName); return; }

		    pref.setOnPreferenceClickListener((Preference.OnPreferenceClickListener)this.getActivity());
	    }//func

	    void bindPreferenceChange(CharSequence prefName){
		    Preference pref = findPreference(prefName);
		    if(pref == null){ System.out.println("Pref Not Found " +  prefName); return; }

		    Preference.OnPreferenceChangeListener listener = (Preference.OnPreferenceChangeListener)this.getActivity();
		    pref.setOnPreferenceChangeListener(listener); // Set the listener to watch for value changes.

		    // Trigger the listener immediately with the preference's current value.
		    listener.onPreferenceChange(pref,PreferenceManager.getDefaultSharedPreferences(pref.getContext()).getString(pref.getKey(),""));
	    }//func
    }//cls

	public static class CombinedFragment extends PrefFragment{
		@Override public void onViewCreated(View v,Bundle savedInstanceState) {
			super.onViewCreated(v,savedInstanceState);

			//Screen is usually created when calling addPreferencesFromResource, but when dynamically trying to add
			//preferences or categories before making the call, preferencescreen doesn't exist. So create one.
			if(this.getPreferenceScreen() == null) this.setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));

			//Bind all the preferences to this fragment.
			SyncPreferenceFragment.bind(this,true);
			LibraryPreferenceFragment.bind(this,true);
			ViewPreferenceFragment.bind(this,true);
		   	FuncPreferenceFragment.bind(this,true);
	    }//func
    }//func

	public static class SyncPreferenceFragment extends PrefFragment{
	    public static final int XMLRES = R.xml.pref_sync;
	    public static final String HEADER = "SYNC SETTINGS";

	    public static void appendHeader(List<Header> target){
		    Header h = new Header();
		    h.title = HEADER;
		    h.fragment = SyncPreferenceFragment.class.getName();
		    target.add(h);
	    }//func

	    public static void bind(PrefFragment f,boolean incHeader){
		    if(incHeader){
			    PreferenceCategory header = new PreferenceCategory(f.getActivity());
			    header.setTitle(HEADER);
			    f.getPreferenceScreen().addPreference(header);
		    }//if

		    f.addPreferencesFromResource(XMLRES);
		    f.bindPreferenceChange("syncfolder1");
		    f.bindPreferenceChange("syncfolder2");
	    }//func
	    @Override public void onCreate(Bundle savedInstanceState){ super.onCreate(savedInstanceState); bind(this,false); }//func
    }//cls

    public static class LibraryPreferenceFragment extends PrefFragment{
	    public static final int XMLRES = R.xml.pref_library;
	    public static final String HEADER = "LIBRARY SETTINGS";

	    public static void appendHeader(List<Header> target){
		    Header h = new Header();
		    h.title = HEADER;
		    h.fragment = LibraryPreferenceFragment.class.getName();
		    target.add(h);
	    }//func

	    public static void bind(PrefFragment f,boolean incHeader){
		    if(incHeader){
			    PreferenceCategory header = new PreferenceCategory(f.getActivity());
			    header.setTitle(HEADER);
			    f.getPreferenceScreen().addPreference(header);
		    }//if

		    f.addPreferencesFromResource(XMLRES);
	    }//func
	    @Override public void onCreate(Bundle savedInstanceState){ super.onCreate(savedInstanceState); bind(this,false); }//func
    }//cls
    
    public static class ViewPreferenceFragment extends PrefFragment{
	    public static final int XMLRES = R.xml.pref_view;
	    public static final String HEADER = "VIEWING SETTINGS";

	    public static void appendHeader(List<Header> target){
		    Header h = new Header();
		    h.title = HEADER;
		    h.fragment = ViewPreferenceFragment.class.getName();
		    target.add(h);
	    }//func

	    public static void bind(PrefFragment f,boolean incHeader){
		    if(incHeader){
			    PreferenceCategory header = new PreferenceCategory(f.getActivity());
			    header.setTitle(HEADER);
			    f.getPreferenceScreen().addPreference(header);
		    }//if

		    f.addPreferencesFromResource(XMLRES);
	    }//func
	    @Override public void onCreate(Bundle savedInstanceState){ super.onCreate(savedInstanceState); bind(this,false); }//func
    }//cls
    
    public static class FuncPreferenceFragment extends PrefFragment{
	    public static final int XMLRES = R.xml.pref_func;
	    public static final String HEADER = "FUNCTIONS";

	    public static void appendHeader(List<Header> target){
		    Header h = new Header();
		    h.title = HEADER;
		    h.fragment = ViewPreferenceFragment.class.getName();
		    target.add(h);
	    }//func

	    public static void bind(PrefFragment f,boolean incHeader){
		    if(incHeader){
			    PreferenceCategory header = new PreferenceCategory(f.getActivity());
			    header.setTitle(HEADER);
			    f.getPreferenceScreen().addPreference(header);
		    }//if

		    f.addPreferencesFromResource(XMLRES);
		    f.bindPreferenceClick("funcClearLib");
		    f.bindPreferenceClick("funcClearSeries");
		    f.bindPreferenceClick("funcClearCover");
	    }//func
	    @Override public void onCreate(Bundle savedInstanceState){ super.onCreate(savedInstanceState); bind(this,false); }//func
    }//cls


	/*
	TODO
	//========================================================
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
    */
}//cls