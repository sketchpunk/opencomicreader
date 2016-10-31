package sage.data;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceUtil{

	public static Map<String,String> getMapResource(Context c, int resId){
		Map<String,String> rtn = null;
		String key = null, val = null, tag = null;
		XmlResourceParser parser = c.getResources().getXml(resId);

		try{
			int etype = parser.getEventType();

			while(etype != XmlPullParser.END_DOCUMENT){
				switch(etype){
					case XmlPullParser.START_TAG:
						tag = parser.getName();

						if(tag.equals("entry")){
							key = parser.getAttributeValue(null,"key");
							if(key == null){ parser.close(); return null; }

						}else if(tag.equals("map")){
							boolean isLinked = parser.getAttributeBooleanValue(null,"linked",false);
							rtn = (isLinked)?new LinkedHashMap<String,String>() : new HashMap<String,String>();
						}

						break;

					case XmlPullParser.END_TAG:
						if(parser.getName().equals("entry")){
							rtn.put(key,val);
							key = null; val = null;
						}
						break;

					case XmlPullParser.TEXT:
						if(key != null) val = parser.getText();
						break;
				}
				etype = parser.next();
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}

		return rtn;
	}

}
