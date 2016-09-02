package com.sketchpunk.ocomicreader.lib;

import java.util.Comparator;

/**
 * Perform a "Numeri-Lexical" comparison between two strings. This lets us
 * compare two page names and sort them correctly.
 * 
 * eg:
 * 
 * "Page2.jpg" < "Page10.jpg"
 * 
 * (The standard Java String comparator would sort these in the opposite order)
 * 
 */
class PageComparator implements Comparator<String>{

	public static final Comparator<String> COMPARATOR = new PageComparator();

	/**
	 * Perform a Numeri-Lexical comparison between two strings. Strings are
	 * compared character-by-character as normal, except when digits are
	 * encountered. In that case, each block of digits are compared as a single
	 * number, and the total difference is returned.
	 */
	@Override
	public int compare(String lhs, String rhs){
		int i = 0;
		int j = 0;
		int diff = 0;

		//..................................
		while(diff == 0){
			if(i >= lhs.length() || j >= rhs.length()){
				diff = lhs.length() - rhs.length();
				break;
			}//if

			int l = lhs.charAt(i);
			int r = rhs.charAt(j);

			if(Character.isDigit(l) && Character.isDigit(r)){
				int lstart = i;
				int rstart = j;
				i = glom(lhs, i);
				j = glom(rhs, j);

				l = Integer.parseInt(lhs.substring(lstart, i));
				r = Integer.parseInt(rhs.substring(rstart, j));
			}else{
				i++;
				j++;
			}//if

			diff = l - r;
		}//while

		return diff;
	}//func

	private static int glom(String s, int index){
		while(index < s.length() && Character.isDigit(s.charAt(index))){
			index++;
		}//while
		return index;
	}//func
}//cls
