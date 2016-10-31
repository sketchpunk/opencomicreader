package sage.data;

public class StringUtil{
	public static boolean notNullOrEmpty(String str){ return (str != null && !str.isEmpty()); }


	public static String join(String[] ary, String delimiter, String prefix, String suffix){
		if(ary == null) return null;
		StringBuilder buf = new StringBuilder();

		if(notNullOrEmpty(prefix)) buf.append(prefix);

		//loop every element before the last one to easily add delimiter, then append the final element cleanly.
		for(int i=0; i < ary.length-1; i++) buf.append(ary[i] + delimiter);
		buf.append(ary[ary.length-1]);

		if(notNullOrEmpty(suffix)) buf.append(suffix);

		return buf.toString();
	}
}
