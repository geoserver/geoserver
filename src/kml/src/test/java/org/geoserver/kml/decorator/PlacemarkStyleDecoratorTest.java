/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.PlacemarkStyleDecoratorFactory.PlacemarkStyleDecorator;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.styling.TextSymbolizer;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

public class PlacemarkStyleDecoratorTest extends org.geoserver.wms.icons.IconTestSupport {

    /**
     * Test partial style transformation.
     *
     * <p>The YSLD parser is not supplying the same defaults as the SLD parser, producing styles
     * that violate some of the (perfectly sensible SLD 1.0) assumptions made by
     * PlacemarkStyleDecorator.
     */
    @Test
    public void testYSLDTextSymbolizerEncoding() {
        TextSymbolizer text =
                text("text", "NAME", font("Arial Black", null, "bold", 8), fill(Color.white, null));

        PlacemarkStyleDecoratorFactory.PlacemarkStyleDecorator decorator =
                new PlacemarkStyleDecorator();

        KmlEncodingContext context = new FakeKmlEncodingContext(featureType);
        SimpleFeatureCollection collection = DataUtilities.collection(fieldIs1);
        context.setCurrentFeatureCollection(collection);
        context.setCurrentFeature(fieldIs1);
        context.setCurrentSymbolizers(Collections.singletonList(text));

        Placemark placemark = new Placemark();
        decorator.decorate(placemark, context);
    }

    public static class FakeKmlEncodingContext extends KmlEncodingContext {
        private SimpleFeatureType featureType;

        public FakeKmlEncodingContext(SimpleFeatureType featureType) {
            this.featureType = featureType;
        }

        public List<SimpleFeatureType> getFeatureTypes() {
            List<SimpleFeatureType> results = new ArrayList<SimpleFeatureType>();
            results.add(featureType);
            return results;
        }

        @Override
        public SimpleFeatureType getCurrentFeatureType() {
            return featureType;
        }
    }
}
