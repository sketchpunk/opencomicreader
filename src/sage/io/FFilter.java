package sage.io;

import java.io.File;
import java.util.Locale;

public class FFilter implements java.io.FileFilter {
	private final String[] mExtList;

	public FFilter(String[] ary) {
		mExtList = ary;
	}

	public boolean accept(File o) {
		if (o.isDirectory())
			return false;
		for (String extension : mExtList) {
			if (o.getName().toLowerCase(Locale.getDefault())
					.endsWith(extension))
				return true;
		}
		return false;
	}
}
