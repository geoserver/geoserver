/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geotools.util.MapEntry;

/**
 * De/convert a workspace and a coverage name into a single string.
 * <p/>
 * Some external formats do not allow to use semicolons in some strings.
 * This class offers methods to encode and decode workspace and names into a single string
 * without using semicolons.
 * <p/>
 * We simply use a "__" as separator. This should reduce the conflicts with existing underscores.
 * This encoding is not unique, so the {@link #decode(java.lang.String decode) method
 * return a list of possibile workspace,name combinations. You'll need to check which workspace
 * is really existing.
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class CoverageIdConverter {

    private final static String DELIMITER = String.valueOf("__");

    public static String encode(String workspace, String covName) {
        return workspace + DELIMITER + covName;
    }

    /**
     *
     * @return a List of possibile workspace/name pairs, possibly empty if the input could not be decoded;
     */
    public static List<MapEntry<String,String>> decode(String qualifiedName) {
        int lastPos = qualifiedName.lastIndexOf(DELIMITER);

        if( lastPos == -1)
            return Collections.EMPTY_LIST;
//            throw new IllegalArgumentException("Delimiter not found in input string '"+qualifiedName+"'");

        List<MapEntry<String,String>> ret = new ArrayList<MapEntry<String, String>>();
        while (lastPos > -1) {
            String ws   = qualifiedName.substring(0, lastPos);
            String name = qualifiedName.substring(lastPos+DELIMITER.length());
            ret.add(new MapEntry<String, String>(ws, name));
            lastPos = qualifiedName.lastIndexOf(DELIMITER, lastPos-1);
        }
        return ret;
    }

}
