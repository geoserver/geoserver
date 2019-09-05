/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.metadata.data.service.impl.PlaceHolderUtil;
import org.junit.Test;

public class PlaceHolderUtilTest {

    private static String PATTERN = "Hello, this is ${a} telling ${b} not to forget to ${c}.";

    private static String STR = "Hello, this is Mike telling John not to forget to buy beer.";

    private static String PATTERN_2 = "Hello, this is ${name}.";

    private static String STR_1 = "Hello, this is Lisa.";

    private static String STR_2 = "Hello, this is Eric.";

    private Map<String, String> map = new HashMap<>();

    private Map<String, List<String>> map2 = new HashMap<>();

    public PlaceHolderUtilTest() {
        map.put("a", "Mike");
        map.put("b", "John");
        map.put("c", "buy beer");

        map2.put("name", Lists.newArrayList("Lisa", "Eric"));
    }

    @Test
    public void testReplacePlaceHolders() {
        assertEquals(STR, PlaceHolderUtil.replacePlaceHolders(PATTERN, map));
    }

    @Test
    public void testReversePlaceHolders() {
        assertEquals(map, PlaceHolderUtil.reversePlaceHolders(PATTERN, STR));
    }

    @Test
    public void testReplacePlaceHolder() {
        assertEquals(
                Lists.newArrayList(STR_1, STR_2),
                PlaceHolderUtil.replacePlaceHolder(PATTERN_2, map2));
    }
}
