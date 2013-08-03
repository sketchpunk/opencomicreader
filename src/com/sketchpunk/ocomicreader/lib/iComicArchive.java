package com.sketchpunk.ocomicreader.lib;

import java.io.InputStream;
import java.util.List;

public interface iComicArchive{
	public void close();
	public boolean isStreamResetable();
	public void clearCache();
	
	public boolean loadFile(String path);
	public List<String> getPageList();
	public InputStream getItemInputStream(String path);
	public boolean getLibraryData(String[] outVar);
}//interface
