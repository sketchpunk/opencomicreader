package com.sketchpunk.ocomicreader;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class App extends Application{
	/*========================================================
	*/
	@Override
	public void onCreate(){ super.onCreate(); }//func
	public static int convertDpToPixels(float dp, Context context){
		Resources resources = context.getResources();
		return (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				dp,
				resources.getDisplayMetrics()
				);
	}
}//cls
