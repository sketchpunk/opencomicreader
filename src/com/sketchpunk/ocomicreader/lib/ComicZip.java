package com.sketchpunk.ocomicreader.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ComicZip implements iComicArchive {
	ZipFile mArchive;

	// I need to load a stream twice to read an image, so
	// instead of finding the same item again, save ref.
	ZipEntry mLastItemReq = null;
	String mLastItemReqPath = "";

	public ComicZip() {
	}

	public void clearCache() {
	}

	public boolean isStreamResetable() {
		return false;
	}

	public void close() {
		mLastItemReq = null;
		if (mArchive != null) {
			try {
				mArchive.close();
				mArchive = null;
			} catch (Exception e) {
			}
		}
	}

	@Override
	public boolean loadFile(String path) {
		try {
			mArchive = new ZipFile(path);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public List<String> getPageList() {
		try {
			String itmName;
			ZipEntry itm;
			List<String> pageList = new ArrayList<String>();
			Enumeration entries = mArchive.entries();

			while (entries.hasMoreElements()) {
				itm = (ZipEntry) entries.nextElement();
				if (itm.isDirectory())
					continue;

				itmName = itm.getName().toLowerCase(Locale.getDefault());
				if (itmName.endsWith(".jpg") || itmName.endsWith(".gif")
						|| itmName.endsWith(".png")) {
					pageList.add(itm.getName());
				}
			}

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
			if (mLastItemReqPath.equals(path) && mLastItemReq != null) {
				return mArchive.getInputStream(mLastItemReq);
			}

			mLastItemReqPath = path;
			mLastItemReq = mArchive.getEntry(path);
			return mArchive.getInputStream(mLastItemReq);
		} catch (Exception e) {
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

			ZipEntry itm;
			Enumeration<? extends ZipEntry> entries = mArchive.entries();

			while (entries.hasMoreElements()) {
				itm = entries.nextElement();
				if (itm.isDirectory())
					continue;

				itmName = itm.getName();
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
		// Find Meta data in archive
		String metaPath = "";
		try {
			ZipEntry itm;
			String itmName, compare;
			Enumeration<? extends ZipEntry> entries = mArchive.entries();

			while (entries.hasMoreElements()) {
				itm = entries.nextElement();
				if (itm.isDirectory())
					continue;

				itmName = itm.getName();
				compare = itmName.toLowerCase(Locale.getDefault());
				if (compare.endsWith("comicinfo.xml")) {
					metaPath = itmName;
					break;
				}
			}

			if (metaPath.isEmpty())
				return null;
		} catch (Exception e) {
			System.err.println("error getting meta data " + e.getMessage());
			return null;
		}

		// Parse the meta data.
		String[] data = null;
		try {
			InputStream iStream = getItemInputStream(metaPath);
			data = MetaParser.ComicRack(iStream);
			iStream.close();
		} catch (IOException e) {
			System.err.println("getting meta from zip " + e.getMessage());
		}

		return data;
	}
}
