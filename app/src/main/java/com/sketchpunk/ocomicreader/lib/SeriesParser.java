package com.sketchpunk.ocomicreader.lib;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeriesParser{
	private ArrayList<ParseItem> mParseList;
	
	public SeriesParser(){
		mParseList = new ArrayList<ParseItem>();
		
		//Mangas tend to be grouped by parent folder, where the filename is just a variation of chapter or volume
		mParseList.add(new ParseItem("/([\\s\\w\\.]+)/v(ol)?(ume)?[\\s_]*\\d+.*\\.(\\w{3})$")); // /[series]/[v|ol|ume] 000[garbage].cbr
		mParseList.add(new ParseItem("/([\\s\\w\\.]+)/c(h)?(apter)?[\\s_]*\\d+.*\\.(\\w{3})$")); // /[series]/[c|h|apter] 000[garbage].cbr
		
		//Determine through file name with a variation of chapter or volume
		mParseList.add(new ParseItem("/([\\s\\w]+)[\\s\\-_]*v(ol)?(ume)?[\\s_]*\\d+.*\\.(\\w{3})$")); // /[series] [-] [v|ol|ume] 000[garbage].cbr
		mParseList.add(new ParseItem("/([\\s\\w]+)[\\s\\-_]*c(h)?(apter)?[\\s_]*\\d+.*\\.(\\w{3})$")); // /[series] [-] [c|h|apter] 000[garbage].cbr
		
		//Determine through filename with T following a number, Usually french books use t for TOME instead of chapter or volume.
		mParseList.add(new ParseItem("/([\\s\\w]+)[\\s\\-_]*t(ome)?[\\s_]*\\d+.*\\.(\\w{3})$")); // /[series] [t|ome] 000[garbage].cbr
		
		//Determine through filename with a number near the end.
		mParseList.add(new ParseItem("/([\\s\\w]+)[\\s\\-]+\\d+.*\\.(\\w{3})$")); // /[series] [-] 000[garbage].cbr

		//Determine through filename with a - usually following a sub title with no number system.
		mParseList.add(new ParseItem("/([\\s\\w]+)[\\s_]*\\-.*\\.(\\w{3})$")); // /[series] [-] [subtitle].cbr
		
		//When all else fails, Just get the filename without extension.
		mParseList.add(new ParseItem("/(.*)\\.(\\w{3})$")); // /[series].cbr
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
		private int mSeriesGroup = 1; //Determine which group from the pattern is the series name.
		
		public ParseItem(String pat){ mPat = Pattern.compile(pat,Pattern.CASE_INSENSITIVE); }//func
		public ParseItem(int seriesGrp,String pat){
			mPat = Pattern.compile(pat);
			mSeriesGroup = seriesGrp;
		}//func
		
		public String parse(String txt){
			Matcher m = mPat.matcher(txt);
			if(m.find()) return m.group(mSeriesGroup).replace("_"," ").trim(); //Remove any underscores and spaces from the end
			return "";
		}//func
	}//cls
}//cls