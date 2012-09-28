package com.sketchpunk.ocomicreader.lib;
//http://mvnrepository.com/artifact/com.github.junrar/junrar


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

import android.os.Environment;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.io.ReadOnlyAccessFile;
import com.github.junrar.rarfile.FileHeader;

public class ComicRar implements iComicArchive{
	Archive mArchive;
	
	//I need to load a stream twice to read an image, so 
	//instead of finding the same item again, save ref.
	FileHeader mLastItemReq = null; 
	String mLastItemReqPath = "";

	/*--------------------------------------------------------
	*/
	public ComicRar(){}//func
	
	
	/*--------------------------------------------------------
	*/	
	public void close(){
		mLastItemReq = null;
		if(mArchive != null){ 
			try{
				mArchive.close(); mArchive = null;
			}catch(Exception e){
			}//try
		}//if
	}//func

	public boolean loadFile(String path) {
		boolean rtn = false;

		File f = new File(path);
		if(f.exists()){
			try {
				mArchive = new Archive(f);
				
				if(mArchive.isEncrypted()){
					mArchive.close();
					mArchive = null;
				}else rtn = true;
			}catch(Exception e){
				System.out.println("Load RAR File Error " + e.getMessage());
			}//try
		}//if
		return rtn;
	}//func
	
	/*--------------------------------------------------------
	*/
	public List<String> getPageList(){
		List<String> pageList = new ArrayList<String>();
		
		//..................................
		String itmName;
		List<FileHeader> files = mArchive.getFileHeaders();
		for(FileHeader fh : files){
			if(fh.isDirectory()) continue;
			
			itmName = fh.getFileNameString().toLowerCase();
			if(itmName.endsWith(".jpg") || itmName.endsWith(".gif") || itmName.endsWith(".png")){
				pageList.add(fh.getFileNameString());
			}//if
		}//func

		//..................................
		if(pageList.size() > 0){
			Collections.sort(pageList); //Sort the page names
			return pageList;
		}//if

		return null;
	}//func

	public InputStream getItemInputStream(String path){
		try{
			if(mLastItemReqPath.equals(path) && mLastItemReq != null){
				return mArchive.getInputStream(mLastItemReq);
			}//if

			//........................................
			//if not the same, then
			List<FileHeader> files = mArchive.getFileHeaders();
			for(FileHeader fh : files){
				if(fh.isDirectory()) continue;
				else if(fh.getFileNameString().equals(path)){
					mLastItemReq = fh;
					mLastItemReqPath = path;
					return mArchive.getInputStream(mLastItemReq);
				}//if
			}//func
		}catch(Exception e){}

		return null;
	}//func

	public boolean getLibraryData(String[] outVar) {	
		int pgCnt = 0;
		String itmName,coverPath = "";

		outVar[0] = "0"; //Page Count
		outVar[1] = ""; //Path to Cover Entry
		
		//..................................
		//TODO:instead of loading the whole list, there is a way to traverse one item at a time.
		List<FileHeader> files = mArchive.getFileHeaders();
		for(FileHeader fh : files){
			if(fh.isDirectory()) continue;

			itmName = fh.getFileNameString().toLowerCase();
			if(itmName.endsWith(".jpg") || itmName.endsWith(".gif") || itmName.endsWith(".png")){
				if(pgCnt == 0) coverPath = fh.getFileNameString();
				pgCnt++;
			}//if
		}//for

		//..................................
		if(pgCnt > 0){
			outVar[0] = Integer.toString(pgCnt);
			outVar[1] = coverPath;
			
			return false;
		}//if
		
		return true;
	}//func

}//func
