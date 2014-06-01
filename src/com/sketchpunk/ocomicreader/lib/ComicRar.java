package com.sketchpunk.ocomicreader.lib;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.sketchpunk.jniunrar.unrar;

public class ComicRar implements iComicArchive {
	String mLastItemReqPath = "";
	String mArcPath = "";
	byte[] mByteCache = null;

	public ComicRar() {
	}

	public void close() {
		clearCache();
	}

	public boolean isStreamResetable() {
		return true;
	}

	public void clearCache() {
		if (mByteCache != null) {
			mByteCache = null;
			// Run Garbage collector. Hopefully clear out the
			// bytecache from memory.
			System.gc();
		}
	}

	public boolean loadFile(String path) {
		boolean rtn = false;

		File f = new File(path);
		if (f.exists()) {
			// TODO: In JNIUnrar, add function to check if archive is password
			// protected.
			mArcPath = path;
			rtn = true;
		}
		return rtn;
	}

	public List<String> getPageList() {
		String[] ary = unrar.getEntries(mArcPath, ".jpg,.png,.jpeg,.gif");
		if (ary == null)
			return null;

		List<String> pageList = Arrays.asList(ary);

		if (pageList.size() > 0) {
			Collections.sort(pageList); // Sort the page names
			return pageList;
		}

		return null;
	}

	public InputStream getItemInputStream(String path) {
		try {
			// Check if the last request is the same as the current one
			if (mLastItemReqPath.equals(path) && mByteCache != null) {
				return new ByteArrayInputStream(mByteCache);
			}

			// If bytes are cached, clear then out to get some memory back.
			if (mByteCache != null)
				clearCache();

			// Load up data from rar entry
			mByteCache = unrar.extractEntryToArray(mArcPath, path);
			if (mByteCache != null) {
				mLastItemReqPath = path;
				return new ByteArrayInputStream(mByteCache);
			}
		} catch (Exception e) {
		}

		return null;
	}

	public boolean getLibraryData(String[] outVar) {
		// List is already filtered and sorted, this is easier then zip
		List<String> pgList = getPageList();
		if (pgList != null && !pgList.isEmpty()) {
			outVar[0] = Integer.toString(pgList.size()); // Page Count
			outVar[1] = pgList.get(0); // Path to Cover Entry
			return true;
		}

		outVar[0] = "0"; // Page Count
		outVar[1] = ""; // Path to Cover Entry
		return false;
	}

	public String[] getMeta() {
		return null;
	}
}
