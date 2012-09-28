package com.sketchpunk.ocomicreader.lib;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ComicZip implements iComicArchive{
	ZipFile mArchive;

	//I need to load a stream twice to read an image, so 
	//instead of finding the same item again, save ref.
	ZipEntry mLastItemReq = null;
	String mLastItemReqPath = "";

	/*--------------------------------------------------------
	*/
	public ComicZip(){}//func

	
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

	@Override
	public boolean loadFile(String path) {
		try{
			mArchive = new ZipFile(path);
			return true;
		}catch(Exception e){
		}//try;
		return false;
	}//func
	
	
	/*--------------------------------------------------------
	*/
	public List<String> getPageList(){
		try{
			String itmName;
			ZipEntry itm;
			List<String> pageList = new ArrayList<String>();
			Enumeration entries = mArchive.entries();

			//..................................
			while(entries.hasMoreElements()) {
				itm = (ZipEntry)entries.nextElement();
				if(itm.isDirectory()) continue;
				
				itmName = itm.getName().toLowerCase();
				if(itmName.endsWith(".jpg") || itmName.endsWith(".gif") || itmName.endsWith(".png")){
					pageList.add(itm.getName());
				}//if
			}//while

			//..................................
			if(pageList.size() > 0){
				Collections.sort(pageList); //Sort the page names
				return pageList;
			}//if
		}catch(Exception e){
			System.err.println("LoadArchive " + e.getMessage());
		}//try

		return null;
	}//func

	public InputStream getItemInputStream(String path){
		try{
			System.out.println(path);
			if(mLastItemReqPath.equals(path) && mLastItemReq != null){
				return mArchive.getInputStream(mLastItemReq);
			}//if
			
			//....................................
			mLastItemReqPath = path;
			mLastItemReq = mArchive.getEntry(path);
			return mArchive.getInputStream(mLastItemReq);
		}catch(Exception e){}
		
		return null;
	}//func
	
	public boolean getLibraryData(String[] outVar){
		outVar[0] = "0"; //Page Count
		outVar[1] = ""; //Path to Cover Entry

		try{
			int pgCnt = 0;
			String coverPath = "";
			
			String itmName;
			ZipEntry itm;
			Enumeration entries = mArchive.entries();

			//..................................
			while(entries.hasMoreElements()) {
				itm = (ZipEntry)entries.nextElement();
				if(itm.isDirectory()) continue;
				
				itmName = itm.getName().toLowerCase();
				if(itmName.endsWith(".jpg") || itmName.endsWith(".gif") || itmName.endsWith(".png")){
					if(pgCnt == 0) coverPath = itm.getName();
					pgCnt++;
				}//if
			}//while

			if(pgCnt > 0){
				outVar[0] = Integer.toString(pgCnt);
				outVar[1] = coverPath;
			}//if
		}catch(Exception e){
			System.err.println("getLibraryData " + e.getMessage());
			return false;
		}//try

		return true;
	}//func
			
}//cls
