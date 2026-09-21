/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.json;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.TimeZone;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.util.factory.Hints;
import org.junit.Test;

public class GeoJSONFeatureWriterTest {

    @Test
    public void testCollectionTimeStampUsesConfiguredLocalTimeZone() {
        TimeZone previousTimeZone = TimeZone.getDefault();
        Object previousDateTimeFormat = Hints.getSystemDefault(Hints.DATE_TIME_FORMAT_HANDLING);
        Object previousLocalTime = Hints.getSystemDefault(Hints.LOCAL_DATE_TIME_HANDLING);
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));
            Hints.putSystemDefault(Hints.DATE_TIME_FORMAT_HANDLING, true);
            Hints.putSystemDefault(Hints.LOCAL_DATE_TIME_HANDLING, true);

            StringWriter writer = new StringWriter();
            GeoJSONBuilder builder = new GeoJSONBuilder(writer);
            new TestGeoJSONFeatureWriter().writeTimeStamp(builder);

            assertTrue(writer.toString(), writer.toString().matches("\\{\"timeStamp\":\".*\\+08:00\"\\}"));
        } finally {
            TimeZone.setDefault(previousTimeZone);
            restoreHint(Hints.DATE_TIME_FORMAT_HANDLING, previousDateTimeFormat);
            restoreHint(Hints.LOCAL_DATE_TIME_HANDLING, previousLocalTime);
        }
    }

    private static void restoreHint(Hints.Key key, Object value) {
        if (value == null) {
            Hints.removeSystemDefault(key);
        } else {
            Hints.putSystemDefault(key, value);
        }
    }

    private static final class TestGeoJSONFeatureWriter extends GeoJSONFeatureWriter<FeatureType, Feature> {

        private TestGeoJSONFeatureWriter() {
            super(null);
        }

        @Override
        protected boolean isFeatureBounding() {
            return false;
        }

        private void writeTimeStamp(GeoJSONBuilder builder) {
            builder.object();
            writeCollectionTimeStamp(builder);
            builder.endObject();
        }
    }
}
