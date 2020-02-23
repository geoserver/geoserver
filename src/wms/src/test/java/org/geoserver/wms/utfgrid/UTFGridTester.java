/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Helper class to test UTFGrid contents
 *
 * @author Andrea Aime - GeoSolutions
 */
class UTFGridTester {

    private JSONArray keys;

    private JSONObject data;

    private JSONArray grid;

    UTFGridTester(JSON json) {
        this(json, 256, 256, 4);
    }

    UTFGridTester(JSON json, int width, int height, int resolution) {
        JSONObject utfGrid = (JSONObject) json;
        assertTrue("Missing grid property", utfGrid.has("grid"));
        assertTrue("Missing keys property", utfGrid.has("keys"));
        assertTrue("Missing data property", utfGrid.has("data"));

        // validate relation between keys and data
        keys = utfGrid.getJSONArray("keys");
        data = utfGrid.getJSONObject("data");
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            if (i == 0) {
                assertEquals("", key);
            } else {
                assertEquals("" + i, key);
                assertTrue(data.has(key));
            }
        }

        grid = utfGrid.getJSONArray("grid");

        int gridWidth = width / resolution;
        int gridHeight = height / resolution;
        assertEquals(grid.size(), gridHeight);
        for (int r = 0; r < gridHeight; r++) {
            String gridRow = grid.getString(r);
            assertEquals(gridWidth, gridRow.length());
            for (int c = 0; c < gridWidth; c++) {
                char code = gridRow.charAt(c);
                if (code == ' ') {
                    continue;
                }
                code = gridToKey(code);

                // make sure the key is there, the data is there
                assertTrue(code < keys.size());
                assertTrue(data.has(String.valueOf((int) code)));
            }
        }
    }

    /** Returns the number of keys */
    int getKeyCount() {
        return keys.size();
    }

    /** Returns the feature for the given grid code */
    JSONObject getFeature(char code) {
        String key = "" + (int) gridToKey(code);
        return data.getJSONObject(key);
    }

    /** Check the specified code can be found in the grid at row/col */
    void assertGridPixel(char code, int row, int col) {
        String gridRow = grid.getString(row);
        assertEquals(
                "Expected '" + code + "' but was '" + gridRow.charAt(col) + "'.",
                code,
                gridRow.charAt(col));
    }

    private char gridToKey(char code) {
        if (code >= 93) {
            code--;
        }
        if (code >= 35) {
            code--;
        }
        code -= 32;
        return code;
    }
}
