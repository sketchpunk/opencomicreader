package com.sketchpunk.ocomicreader.lib;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeriesParser{
	private ArrayList<ParseItem> mParseList;
	
	public SeriesParser(){
		mParseList = new ArrayList<ParseItem>();
		mParseList.add(new ParseItem("([\\s_]+v(ol)?(ume)?[\\s_]*\\d+)"));
		mParseList.add(new ParseItem("([\\s_]+c(h)?(apter)?[\\s_]*\\d+)"));
		mParseList.add(new ParseItem("([\\s_]+-)"));
		mParseList.add(new ParseItem("([\\s_]+[tT]*\\d+)"));
	}//func
	
	public String get(String txt){
		if(txt == null || txt.isEmpty()) return "";
		String tmp;
		for(int i=0; i < mParseList.size(); i++){
			tmp = mParseList.get(i).parse(txt);
			if(!tmp.isEmpty()) return tmp;
		}//for
		return txt;
	}//func
	
	protected static class ParseItem{
		private Pattern mPat;
		
		public ParseItem(String pat){
			mPat = Pattern.compile(pat);
		}//func
		
		public String parse(String txt){
			Matcher m = mPat.matcher(txt);
			if(m.find()) return txt.substring(0,m.start()).replace("_"," ").trim();
			return "";
		}//func
	}//cls
}//cls