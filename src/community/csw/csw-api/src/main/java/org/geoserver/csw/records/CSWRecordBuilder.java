/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;

/**
 * A helper that builds CSW Dublin core records as GeoTools features
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordBuilder {

    ComplexFeatureBuilder fb = new ComplexFeatureBuilder(CSWRecordTypes.RECORD);

    AttributeBuilder ab = new AttributeBuilder(new LenientFeatureFactoryImpl());

    /**
     * Adds an element to the current record
     * 
     * @param name
     * @param values
     */
    public void addElement(String name, String... values) {
        for (String value : values) {
            AttributeDescriptor descriptor = CSWRecordTypes.getDescriptor(name);
            ab.setDescriptor(descriptor);
            ab.add(null, value, CSWRecordTypes.SIMPLE_LITERAL_VALUE);
            Attribute element = ab.build();

            fb.append(CSWRecordTypes.DC_ELEMENT_NAME, element);
        }
    }
    
    /**
     * Adds a bounding box to the record. The envelope must be in WGS84 
     * 
     * @param env
     */
    public void addBoundingBox(ReferencedEnvelope env) {
        CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
        if(crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            throw new IllegalArgumentException("The envelope should be provided in WGS84");
        }
        
        Polygon poly = JTS.toGeometry(env);
        poly.setUserData(DefaultGeographicCRS.WGS84);
        ab.setDescriptor(CSWRecordTypes.RECORD_BBOX_DESCRIPTOR);
        Attribute element = ab.buildSimple(null, poly);

        fb.append(CSWRecordTypes.RECORD_BBOX_NAME, element);
    }

    /**
     * Builds a record and sets up to work on the next one
     * 
     * @param id
     * @return
     */
    public Feature build(String id) {
        return fb.buildFeature(id);
    }

}
