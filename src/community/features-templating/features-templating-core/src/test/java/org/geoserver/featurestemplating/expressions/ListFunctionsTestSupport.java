/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import java.util.ArrayList;
import java.util.List;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public abstract class ListFunctionsTestSupport {

    protected static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected List<SimpleFeature> featureList = new ArrayList<>();

    @Before
    public void setup() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("stringValue", String.class);
        tb.add("doubleValue", Double.class);
        tb.add("intValue", Integer.class);
        tb.add("dupProperty", String.class);
        tb.add("geometry", Point.class);
        tb.setName("listFunTest");
        SimpleFeatureType schema = tb.buildFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        GeometryFactory factory = new GeometryFactory();
        for (int i = 1; i < 10; i++) {
            fb.add(getCharForNumber(i));
            fb.add(Double.valueOf(i + "." + i));
            fb.add(i);
            if (i % 2 == 0) fb.add("even");
            else fb.add("odd");
            fb.add(factory.createPoint(new Coordinate(i, i)));
            featureList.add(fb.buildFeature(null));
        }
    }

    private String getCharForNumber(int i) {
        return i > 0 && i < 27 ? String.valueOf((char) (i + 64)) : null;
    }
}
