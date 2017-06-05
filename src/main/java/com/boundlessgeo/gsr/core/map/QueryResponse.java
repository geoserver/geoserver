/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.map;

import com.boundlessgeo.gsr.core.feature.FeatureEncoder;
import com.boundlessgeo.gsr.core.GSRModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Map service query response model
 */
public class QueryResponse implements GSRModel {


    public QueryResponse(FeatureTypeInfo featureType, Filter geometryFilter, boolean returnIdsOnly, boolean returnGeometry, String[] properties, CoordinateReferenceSystem outCRS) throws IOException {

        FeatureSource<? extends FeatureType, ? extends Feature> source =
                featureType.getFeatureSource(null, null);
        final String[] effectiveProperties = adjustProperties(returnGeometry, properties, source.getSchema());

        final Query query;
        if (effectiveProperties == null) {
            query = new Query(featureType.getName(), geometryFilter);
        } else {
            query = new Query(featureType.getName(), geometryFilter, effectiveProperties);
        }
        query.setCoordinateSystemReproject(outCRS);

        if (returnIdsOnly) {
            FeatureEncoder.featureIdSetToJson(source.getFeatures(query), null /*json*/);
        } else {
            final boolean reallyReturnGeometry = returnGeometry || properties == null;
            FeatureEncoder.featuresToJson(source.getFeatures(query), null /*json*/, reallyReturnGeometry);
        }
        //TODO - split FeatureEncoder between JSONConverter and this...
    }

    private String[] adjustProperties(boolean addGeometry, String[] originalProperties, FeatureType schema) {
        if (originalProperties == null) {
            return null;
        }

        String[] effectiveProperties =
                new String[originalProperties.length + (addGeometry ? 1 : 0)];
        for (int i = 0; i < originalProperties.length; i++) {
            effectiveProperties[i] = adjustOneProperty(originalProperties[i], schema);
        }
        if (addGeometry){
            effectiveProperties[effectiveProperties.length - 1] =
                    schema.getGeometryDescriptor().getLocalName();
        }

        return effectiveProperties;
    }

    private String adjustOneProperty(String name, FeatureType schema) {
        List<String> candidates = new ArrayList<String>();
        for (PropertyDescriptor d : schema.getDescriptors()) {
            String pname = d.getName().getLocalPart();
            if (pname.equals(name)) {
                return name;
            } else if (pname.equalsIgnoreCase(name)) {
                candidates.add(pname);
            }
        }
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() == 0) throw new NoSuchElementException("No property " + name + " in " + schema);
        throw new NoSuchElementException("Ambiguous request: " + name + " corresponds to " + candidates);
    }
}

