/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtl {

    public static <T extends Object> List<T> asList(T... data) {
        return new ArrayList<>(Arrays.asList(data));
    }
}
