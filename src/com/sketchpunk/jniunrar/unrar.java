package com.sketchpunk.jniunrar;
/*NOTE: To recreate c header, Select this file then use the external tools 
button that's already setup with javah.*/

public class unrar{
    static{ 
    	try{
    		System.loadLibrary("jniunrar");
    	}catch(UnsatisfiedLinkError e){
    		e.printStackTrace();
    	}//try
    }//static
    
    public static native String getVersion();
	public static native String[] getEntries(String arcPath,String extList);
	public static native byte[] extractEntryToArray(String arcPath,String entryName);
}//cls
