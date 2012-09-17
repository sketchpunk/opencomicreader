package sage.io;

public class Path{
	public static String RemoveExt(String txt) {
		 int pos = txt.lastIndexOf(".");
		 if(pos == -1) return txt;
		 return txt.substring(0,pos);
	}//func
}//cls
