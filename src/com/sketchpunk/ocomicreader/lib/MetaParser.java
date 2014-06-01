package com.sketchpunk.ocomicreader.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class MetaParser {
	public static String[] ComicRack(InputStream iStream) {
		String[] rtn = { "", "", "", "" }; // Title, Series, Volume, Issue
		XmlPullParser parser = Xml.newPullParser();
		InputStreamReader iReader = null;

		boolean isDone = false;
		String nodename = null;
		int eventType = 0, itmCnt = 0;

		try {
			iReader = new InputStreamReader(iStream);
			parser.setInput(iReader);
			eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT && !isDone) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					nodename = parser.getName();
					if (nodename.equalsIgnoreCase("Title")) {
						rtn[0] = parser.nextText();
						itmCnt++;
					} else if (nodename.equalsIgnoreCase("Series")) {
						rtn[1] = parser.nextText();
						itmCnt++;
					} else if (nodename.equalsIgnoreCase("Volume")) {
						rtn[2] = parser.nextText();
						itmCnt++;
					} else if (nodename.equalsIgnoreCase("Number")) {
						rtn[3] = parser.nextText();
						itmCnt++;
					} else if (nodename.equalsIgnoreCase("Pages"))
						isDone = true;
					break;
				}
				eventType = parser.next();
			}
		} catch (FileNotFoundException e) {
			System.out.println("METAPARSER " + e.getMessage());
		} catch (XmlPullParserException e) {
			System.out.println("METAPARSER " + e.getMessage());
		} catch (IOException e) {
			System.out.println("METAPARSER " + e.getMessage());
		}

		if (iReader != null)
			try {
				iReader.close();
				iReader = null;
			} catch (IOException e) {
			}
		return (itmCnt != 0) ? rtn : null;
	}
}