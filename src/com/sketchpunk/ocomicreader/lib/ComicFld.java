package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import sage.io.FFilter;

public class ComicFld implements iComicArchive {
	private File mFile;

	public ComicFld() {
	}

	public void clearCache() {
	}

	public boolean isStreamResetable() {
		return false;
	}

	public void close() {
	}

	@Override
	public boolean loadFile(String path) {
		try {
			mFile = new File(path);
			return (mFile.exists() && mFile.isDirectory());
		} catch (Exception e) {
		}
		return false;
	}

	public List<String> getPageList() {
		try {
			FFilter filter = new FFilter(
					new String[] { ".jpg", ".jpeg", ".png" });
			File[] fList = mFile.listFiles(filter);

			if (fList == null)
				return null;

			List<String> pageList = new ArrayList<String>();
			for (File file : fList)
				pageList.add(file.getPath());

			if (pageList.size() > 0) {
				Collections.sort(pageList); // Sort the page names
				return pageList;
			}
		} catch (Exception e) {
			System.err.println("LoadArchive " + e.getMessage());
		}

		return null;
	}

	public InputStream getItemInputStream(String path) {
		try {
			File file = new File(path);
			if (!file.exists())
				return null;

			return new FileInputStream(file);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public boolean getLibraryData(String[] outVar) {
		outVar[0] = "0"; // Page Count
		outVar[1] = ""; // Path to Cover Entry
		outVar[2] = ""; // Path to Meta Data

		try {
			int pgCnt = 0;
			String itmName, compare, coverPath = "", metaPath = "";

			FFilter filter = new FFilter(new String[] { ".jpg", ".jpeg",
					".png", ".xml" });
			File[] fList = mFile.listFiles(filter);

			for (File file : fList) {
				itmName = file.getPath();
				compare = itmName.toLowerCase(Locale.getDefault());

				if (compare.endsWith(".jpg") || compare.endsWith(".gif")
						|| compare.endsWith(".png")) {
					if (pgCnt == 0 || itmName.compareTo(coverPath) < 0)
						coverPath = itmName;
					pgCnt++;
				} else if (compare.endsWith("comicinfo.xml"))
					metaPath = itmName;
			}

			if (pgCnt > 0) {
				outVar[0] = Integer.toString(pgCnt);
				outVar[1] = coverPath;
				outVar[2] = metaPath;
			}
		} catch (Exception e) {
			System.err.println("getLibraryData " + e.getMessage());
			return false;
		}

		return true;
	}

	public String[] getMeta() {
		String path = mFile.getPath() + "/ComicInfo.xml";
		File file = new File(path);

		if (!file.exists())
			return null;

		String[] data = null;
		try {
			InputStream iStream = new FileInputStream(file);
			data = MetaParser.ComicRack(iStream);
			iStream.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

		return data;
	}

}
