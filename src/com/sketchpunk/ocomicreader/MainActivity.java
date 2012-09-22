package com.sketchpunk.ocomicreader;

import com.sketchpunk.ocomicreader.lib.ComicLibrary;

import sage.data.Sqlite;
import sage.data.SqlCursorLoader;
import sage.adapter.SqlCursorAdapter;
import sage.loader.LoadImageView;

import android.os.Bundle;
import android.os.Environment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity 
	implements SqlCursorAdapter.AdapterCallback
	,LoaderManager.LoaderCallbacks<Cursor>
	,LoadImageView.OnImageLoadedListener
	,ComicLibrary.SyncCallback
	,OnItemClickListener{

	private GridView mGridView;
	private SqlCursorAdapter mAdapter;
	private Sqlite mDb;
	
	private String mThumbPath;
	private ProgressDialog mProgress;
	
	/*========================================================
	Main*/
	@Override
	public void onDestroy(){
		super.onDestroy();
	}//func
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mThumbPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenComicReader/thumbs/";
        
        //....................................
        //Setup Main View Area
        mAdapter = new SqlCursorAdapter(this);
        mAdapter.setItemLayout(R.layout.listitem_main);
        
        mGridView = (GridView) findViewById(R.id.lvMain);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        
        registerForContextMenu(mGridView); //Route event from Activity to View
        
        //....................................
        //Start DB and Data Loader
        mDb = new Sqlite(this);
        mDb.openRead();
        
        getSupportLoaderManager().initLoader(0,null,this);
    }//func

    
	/*========================================================
	State*/
    @Override
    public void onPause() {
        super.onPause();
        System.out.println("ON PAUSE");
    }//func

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("ON RESUME");
        
        if(mDb == null) mDb = new Sqlite(this);
        if(!mDb.isOpen()) mDb.openRead();
    }//func
    
    
    /*========================================================
	Action Bar Menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }//func

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        	//................................................
        	case R.id.menu_settings:
        		Intent intent = new Intent(this,PrefActivity.class);
        		//intent.putExtra("path",(String)view.getTag());
        		this.startActivityForResult(intent,0);
        		break;

        	//................................................
        	case R.id.menu_reset:
        		final Context oThis = this;
        		sage.ui.Dialogs.ConfirmBox(this,"Reset Library","Are you sure you want to reset the library?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){ComicLibrary.clearAll(oThis); refreshData(); }
				});
        		break;
        	
        	//................................................
        	case R.id.menu_import:
        		sage.ui.Dialogs.ConfirmBox(this,"Sync Library","Are you sure you want sync the library?",new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog,int id){startSync();}
				});
        		break;
        }//switch
        return true;
    }//cls
    

    /*========================================================
	Context menu*/
    @Override
	public void onCreateContextMenu(ContextMenu menu,View v,ContextMenuInfo menuInfo){
		switch(v.getId()){
			case R.id.lvMain:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
				AdapterItemRef ref = (AdapterItemRef)info.targetView.getTag();
				
				menu.setHeaderTitle(ref.lblTitle.getText().toString());
				menu.add(0, 1, 0,"Edit");
				menu.add(0, 2, 1,"Delete");
			break;
		}//switch
	}//func
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		//final ListItemRef ref = (ListItemRef)info.targetView.getTag();
		
		switch(item.getItemId()){
			case 1:
				Toast.makeText(this,"Feature not implemented",Toast.LENGTH_SHORT).show();
			break;
		}//switch
	  return true;
	}//func

	
    /*========================================================
	UI Events*/
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
		AdapterItemRef itmRef = (AdapterItemRef)view.getTag();

    	Intent intent = new Intent(this,ViewActivity.class);
		intent.putExtra("path",itmRef.path);
	    this.startActivityForResult(intent,0);
	}//func
    

    /*========================================================
	Cursor Loader*/
	private void refreshData(){ getSupportLoaderManager().restartLoader(0,null,this); }//func
	
    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg){
    	SqlCursorLoader cursorLoader = new SqlCursorLoader(this,mDb);
    	cursorLoader.setRaw("SELECT comicID [_id],title,path FROM ComicLibrary ORDER BY title");
    	return cursorLoader;
	}//func
    
	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}//func

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}//func
	
	
    /*========================================================
	Adapter Events*/
	protected class AdapterItemRef{
    	String id;
    	String path;
    	TextView lblTitle;
    	ImageView imgCover;
    	Bitmap bitmap = null;
    }//cls
	
	@Override
	public View onCreateListItem(View v){
    	try{
    		AdapterItemRef itmRef = new AdapterItemRef();
    		itmRef.lblTitle = (TextView)v.findViewById(R.id.lblTitle);
    		itmRef.imgCover = (ImageView)v.findViewById(R.id.imgCover);
    		itmRef.imgCover.setTag(itmRef);

			v.setTag(itmRef);
    	}catch(Exception e){
    		System.out.println("onCreateListItem " + e.getMessage());
    	}//try
		return v;
	}//func

	@Override
	public void onBindListItem(View v,Cursor c){
		try{
			AdapterItemRef itmRef = (AdapterItemRef)v.getTag();
			//..............................................
			itmRef.id = c.getString(mAdapter.getColIndex("_id"));
			itmRef.path = c.getString(mAdapter.getColIndex("path"));
			itmRef.lblTitle.setText(c.getString(mAdapter.getColIndex("title")));

			LoadImageView.loadImage(mThumbPath + itmRef.id + ".jpg",itmRef.imgCover,this);
    	}catch(Exception e){
    		System.out.println("onBindListItem " + e.getMessage());
    	}//try
	}//func

	
    /*========================================================
	Image Loading*/
	@Override
	public void onImageLoaded(boolean isSuccess,Bitmap bmp,View view){
		if(view == null) return;
		ImageView iv = (ImageView)view;
		
		if(!isSuccess) iv.setImageBitmap(null); //release reference, if cover didn't load show that it didn't.
		
		AdapterItemRef itmRef = (AdapterItemRef)iv.getTag();
		if(itmRef.bitmap != null){
			itmRef.bitmap.recycle();
			itmRef.bitmap = null;
		}//if
		
		itmRef.bitmap = bmp; //keeping reference to make sure to clear it out when its not needed
	}//func

	
    /*========================================================
	Sync Library*/
	private void startSync(){
		if(ComicLibrary.startSync(this)){
			if(mProgress != null){
				if(!mProgress.isShowing()){
					mProgress.show(this,"Library Syncing","",true);
					return;
				}//if
			}//if

			mProgress = ProgressDialog.show(this,"Library Syncing","",true);
		}else{
			Toast.makeText(this,"Sync did not start", Toast.LENGTH_SHORT).show();
		}//if
	}//func

	
	@Override
	public void OnSyncProgress(String txt){
		if(mProgress != null){
			if(mProgress.isShowing())mProgress.setMessage(txt);
		}//if
		
		System.out.println("PROGRESS");
	}//func
	

	@Override
	public void onSyncComplete(int status){
		//............................................
		if(mProgress != null){ mProgress.dismiss(); mProgress = null; }//if
		
		System.out.println("onSyncComplete");
		
		//............................................
		switch(status){
			case ComicLibrary.STATUS_COMPLETE: refreshData(); break;
		
			case ComicLibrary.STATUS_NOSETTINGS:
				Toast.makeText(this,"No sync folders have been set. Go to settings.",Toast.LENGTH_LONG).show();
			break;
		}//switch
	}//func
}//cls
