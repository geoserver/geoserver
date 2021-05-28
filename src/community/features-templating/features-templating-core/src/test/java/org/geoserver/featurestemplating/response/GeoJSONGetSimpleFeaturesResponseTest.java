/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;

public class GeoJSONGetSimpleFeaturesResponseTest extends TemplateJSONSimpleTestSupport {

    protected void checkFeature(JSONObject feature) {
        assertNotNull(feature.getString("id"));
        assertNotNull(feature.getString("name"));
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("type"), "MultiPolygon");
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        assertNotNull(coordinates);
        assertFalse(coordinates.isEmpty());
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSON.getFilename();
    }
}
