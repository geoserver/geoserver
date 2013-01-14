/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */package org.geoserver.w3ds.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class W3DSUtils {
	
	public static List<String> parseStrArray(String s) {
		List<String> output = new ArrayList<String>();
		String listString = s.substring(1, s.length() - 1);
		StringTokenizer tokens = new StringTokenizer(listString, ",");
		while (tokens.hasMoreTokens()) {
			output.add(((String) tokens.nextElement()).trim());
		}
		return output;
	}
	
	public static String[] parseStrArray(String s, String delimiter) {
		Pattern pattern = Pattern.compile(delimiter);
		return pattern.split(s);
	}
	
}
