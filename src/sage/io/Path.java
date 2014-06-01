package sage.io;

public class Path {
	public static String removeExt(String txt) {
		int pos = txt.lastIndexOf(".");
		if (pos == -1)
			return txt;
		return txt.substring(0, pos);
	}

	public static String getExt(String txt) {
		int pos = txt.lastIndexOf(".");
		if (pos == -1)
			return "";
		return txt.substring(pos + 1);
	}

	public static String removeLast(String txt) {
		int pos = txt.lastIndexOf("/");
		if (pos == -1)
			return "";
		return txt.substring(0, pos);
	}

	public static String getLast(String txt) {
		int pos = txt.lastIndexOf("/");
		if (pos == -1)
			return "";
		return txt.substring(pos + 1);
	}

	public static String getParentName(String txt) {
		return getLast(removeLast(txt));
	}
}
