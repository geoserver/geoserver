/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.LineString;

public class GraticuleFeatureSource extends ContentFeatureSource {
    private final List<Double> steps;

    private final ReferencedEnvelope bounds;

    public GraticuleFeatureSource(
            ContentEntry entry, List<Double> steps, ReferencedEnvelope bounds) {
        super(entry, Query.ALL);

        this.steps = steps;
        this.bounds = bounds;
    }

    List<Double> getSteps() {
        return steps;
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        if (query.getFilter() != Filter.INCLUDE) {
            return null;
        }
        return bounds;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        return -1;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        return new GraticuleFeatureReader(this, query);
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(getEntry().getName());

        tb.add(LineFeatureBuilder.ID_ATTRIBUTE_NAME, Integer.class);
        tb.add(
                LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME,
                LineString.class,
                bounds.getCoordinateReferenceSystem());
        tb.setDefaultGeometry(LineFeatureBuilder.DEFAULT_GEOMETRY_ATTRIBUTE_NAME);
        tb.setCRS(bounds.getCoordinateReferenceSystem());
        tb.add(LineFeatureBuilder.LEVEL_ATTRIBUTE_NAME, Integer.class);
        tb.add(LineFeatureBuilder.VALUE_LABEL_NAME, String.class);
        tb.add(LineFeatureBuilder.VALUE_ATTRIBUTE_NAME, Double.class);
        tb.add(LineFeatureBuilder.ORIENTATION_NAME, Boolean.class);
        SimpleFeatureType type = tb.buildFeatureType();
        return type;
    }
}
