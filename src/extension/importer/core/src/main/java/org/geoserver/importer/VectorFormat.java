/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Base class for vector based formats.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class VectorFormat extends DataFormat {

    protected static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    static {
        EMPTY_BOUNDS.setToNull();
    }

    /** Reads features from the data for the specified import item. */
    public abstract FeatureReader read(ImportData data, ImportTask item) throws IOException;

    /** Disposes the reader for the specified import item. */
    public abstract void dispose(FeatureReader reader, ImportTask item) throws IOException;

    /** Get the number of features from the data for the specified import item. */
    public abstract int getFeatureCount(ImportData data, ImportTask item) throws IOException;

    /**
     * Builds a {@link SimpleFeatureType} from the attributes declared in a {@link FeatureTypeInfo}
     */
    protected SimpleFeatureType buildFeatureTypeFromInfo(FeatureTypeInfo fti) {
        SimpleFeatureType ft;
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName(fti.getName());
        List<AttributeTypeInfo> attributes = fti.getAttributes();
        for (AttributeTypeInfo attr : attributes) {
            if (Geometry.class.isAssignableFrom(attr.getBinding())) {
                ftb.add(attr.getName(), attr.getBinding(), fti.getCRS());
            } else {
                ftb.add(attr.getName(), attr.getBinding());
            }
        }
        ft = ftb.buildFeatureType();
        return ft;
    }
}
