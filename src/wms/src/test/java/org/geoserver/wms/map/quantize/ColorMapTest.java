package org.geoserver.wms.map.quantize;

import java.util.HashSet;
import java.util.Set;

import org.geoserver.wms.map.quantize.ColorMap;
import org.geoserver.wms.map.quantize.ColorUtils;
import org.geoserver.wms.map.quantize.ColorMap.ColorEntry;

import junit.framework.TestCase;

public class ColorMapTest extends TestCase {

    public void testSimpleIncrement() {
        ColorMap map = new ColorMap();

        map.increment(255, 255, 255, 255);
        assertEquals(1, map.get(255, 255, 255, 255));
        map.increment(255, 255, 255, 255);
        assertEquals(2, map.get(255, 255, 255, 255));
        assertEquals(1, map.size());
    }

    public void testPutIncrement() {
        ColorMap map = new ColorMap();

        map.put(255, 255, 255, 255, 10);
        assertEquals(10, map.get(255, 255, 255, 255));
        map.increment(255, 255, 255, 255);
        assertEquals(11, map.get(255, 255, 255, 255));
        assertEquals(1, map.size());
    }
    
    public void testRehash() {
        ColorMap map = new ColorMap(16);
        assertEquals(16, map.table.length);
        
        // this will force the map to rehash
        for (int i = 1; i < 20; i++) {
            map.put(255, 255, 255, i, i);
        }
        assertEquals(64, map.table.length);
        assertEquals((int) (64 * ColorMap.DEFAULT_LOAD_FACTOR), map.threshold);
        
        // check we can still retrieve the data as expected
        for (int i = 1; i < 20; i++) {
            assertEquals(i, map.get(255, 255, 255, i));
        }
    }
    
    public void testIterate() {
        ColorMap map = new ColorMap(16);
        assertEquals(16, map.table.length);
        
        // this will force the map to rehash
        for (int i = 1; i < 20; i++) {
            map.put(255, 255, 255, i, i);
        }
        assertEquals(64, map.table.length);
        
        Set<Integer> colors = new HashSet<Integer>();
        for (ColorEntry ce : map) {
            Integer color = ce.color;
            assertFalse(colors.contains(color));
            colors.add(color);
            assertEquals(ColorUtils.alpha(ce.color),  ce.value);
        }
        assertEquals(map.size, colors.size());
    }
}
