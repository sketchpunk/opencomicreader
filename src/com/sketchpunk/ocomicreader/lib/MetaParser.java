package com.sketchpunk.ocomicreader.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class MetaParser{
	public static String[] ComicRack(InputStream iStream){
		String[] rtn = new String[4]; //Title,Series,Volume,Issue
		XmlPullParser parser = Xml.newPullParser();
		InputStreamReader iReader = null;
		
		boolean isDone = false;
	    String nodename = null;
	    int eventType = 0;
	  
		try{
			iReader = new InputStreamReader(iStream);	
		    parser.setInput(iReader);
		    eventType = parser.getEventType();

		    while(eventType != XmlPullParser.END_DOCUMENT && !isDone){
		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT: break;
		            case XmlPullParser.START_TAG:
		            	nodename = parser.getName();
		            	if(nodename.equalsIgnoreCase("Title"))			rtn[0] = parser.nextText();
		            	else if(nodename.equalsIgnoreCase("Series"))	rtn[1] = parser.nextText();
		            	else if(nodename.equalsIgnoreCase("Volume"))	rtn[2] = parser.nextText();
		            	else if(nodename.equalsIgnoreCase("Number"))	rtn[3] = parser.nextText();
		            	else if(nodename.equalsIgnoreCase("Pages"))		isDone = true;
		            break;
		        }//switch
		        eventType = parser.next();
		    }//while
		}catch(FileNotFoundException e){
		}catch(XmlPullParserException e){
		}catch(IOException e){
		}//try
		
		if(iReader != null) try{ iReader.close(); iReader = null; }catch(IOException e){}
		return rtn;
	}//func
}//cls