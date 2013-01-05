package sage.pref;

import java.io.File;
import java.util.LinkedHashMap;

import sage.adapter.MapAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FolderPref extends DialogPreference implements DialogInterface.OnClickListener
	,MapAdapter.AdapterCallback,AdapterView.OnItemClickListener{
	private ListView mListView;
	private LinkedHashMap<String,String> mFolderList;
	private MapAdapter<String,String> mAdapter;
	private TextView mPathView;
	private String mCurrentPath;
	
	/*========================================================
	*/	
	public FolderPref(Context context,AttributeSet attrib){
		super(context,attrib);
		//setDialogLayoutResource(com.sketchpunk.ocomicreader.R.layout.dialogpref_folder);
	}//func

	public FolderPref(Context context,AttributeSet attrib,int defStyle) {
		super(context,attrib,defStyle);
		//setDialogLayoutResource(com.sketchpunk.ocomicreader.R.layout.dialogpref_folder);
	}//func

	
	/*========================================================
	*/
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);  
		builder.setPositiveButton("Save",this);
		builder.setNegativeButton("Cancel",this);
		builder.setNeutralButton("Clear",this);
	}//func

	@Override
	protected View onCreateDialogView(){
		LinearLayout container = new LinearLayout(getContext());
	    container.setOrientation(LinearLayout.VERTICAL);

	    //......................................
	    mPathView = new TextView(this.getContext());
	    container.addView(mPathView,LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
	    
	    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mPathView.getLayoutParams();
	    params.setMargins(8,0,0,0);
	    mPathView.setLayoutParams(params);

	    //......................................
		mFolderList = new LinkedHashMap<String,String>();
		loadPath(Environment.getExternalStorageDirectory().getAbsolutePath());
	    
	    //......................................
		mAdapter = new MapAdapter<String,String>(this.getContext());
		mAdapter.setListItemLayout(com.sketchpunk.ocomicreader.R.layout.listitem_dlgfolder); //this should by dynamic, remove dependency
		mAdapter.setCallback(this);
		mAdapter.setSortEnabled(true);
		mAdapter.setData(mFolderList);
		
	    mListView = new ListView(this.getContext());
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);
		container.addView(mListView,LinearLayout.LayoutParams.MATCH_PARENT,250);

	    //......................................
		return container;
	}//func
	
	@Override
	protected void onBindDialogView(View view) {}//func
	
	@Override
	protected void onDialogClosed(boolean positiveResult){}//func


	/*========================================================
	Adapter Events*/
	@Override
	public View onDisplayListItem(View v, Object key) {
		TextView tv = (TextView)v.findViewById(com.sketchpunk.ocomicreader.R.id.lblTitle);
		tv.setText(mFolderList.get((String)key).toString());
		v.setTag((String)key);
		return v;
	}//func

	
	/*========================================================
	UI Events*/
	@Override
	public void onClick(DialogInterface dialog, int which){
		switch(which){
			case -1: this.persistString(mCurrentPath); break; //Positive
			case -3: this.persistString(null); break; //Neutral
		}//switch
	}//func
	
	@Override
	public void onItemClick(AdapterView<?> parent,View view,int position,long id){
		String path = (String)view.getTag();
		if(path.equals("..")){
			int pos = mCurrentPath.lastIndexOf('/');
			if(pos > 0) path = mCurrentPath.substring(0,pos);
		}//if
		
		loadPath(path);
		mAdapter.refresh();
	}//func
	

	/*========================================================
	*/
	private void loadPath(String path){
		mCurrentPath = path;
		mPathView.setText(mCurrentPath);

		//..............................
		mFolderList.clear();
		mFolderList.put("..","..");
		
		//..............................
		File fObj = new File(path);
		File[] fList = fObj.listFiles();
		
		if(fList != null){
			for(File file:fList){
				if(file.isDirectory()) mFolderList.put(file.getPath(),file.getName());
			}//for
		}//if
	}//func
}//cls
