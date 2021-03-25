/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;

public class JSONLDGetSimpleFeaturesResponseTest extends TemplateJSONSimpleTestSupport {

    protected void checkFeature(JSONObject feature) {
        assertNotNull(feature.getString("id"));
        assertNotNull(feature.getString("name"));
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("@type"), "MultiPolygon");
        assertNotNull(geometry.getString("wkt"));
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSONLD.getFilename();
    }
}
