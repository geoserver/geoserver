/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.rest.format.StreamDataFormat;
import org.json.simple.JSONObject;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.restlet.data.MediaType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Writes out a layer description
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class DescribeJSONFormat extends StreamDataFormat {
    protected DescribeJSONFormat() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        throw new UnsupportedOperationException("Can't read capabilities documents with this class");
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        FeatureTypeInfo fti = (FeatureTypeInfo) object;
        SimpleFeatureType schema = (SimpleFeatureType) fti.getFeatureType();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out);
            writer.write("[");

            Map<String, String> attributes = new LinkedHashMap<String, String>();
            for (AttributeDescriptor att : schema.getAttributeDescriptors()) {
                attributes.put(att.getLocalName(), findAttributeType(att));
            }
            JSONObject.writeJSONString(attributes, writer);

            writer.write("]");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private String findAttributeType(AttributeDescriptor att) {
        Class binding = att.getType().getBinding();
        if (Geometry.class.isAssignableFrom(binding)) {
            return findGeometryType(binding);
        } else if (Number.class.isAssignableFrom(binding)) {
            return "number";
        } else if (Date.class.isAssignableFrom(binding)) {
            return "timestamp";
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return "boolean";
        } else {
            return "string";
        }
    }

    private String findGeometryType(Class binding) {
        if (GeometryCollection.class.isAssignableFrom(binding)) {
            if (MultiPoint.class.isAssignableFrom(binding)) {
                return "MultiPoint";
            } else if (MultiPolygon.class.isAssignableFrom(binding)) {
                return "MultiPolygon";
            } else if (MultiLineString.class.isAssignableFrom(binding)) {
                return "MultiLineString";
            } else {
                return "GeometryCollection";
            }
        } else {
            if (Point.class.isAssignableFrom(binding)) {
                return "Point";
            } else if (Polygon.class.isAssignableFrom(binding)) {
                return "Polygon";
            } else if (LineString.class.isAssignableFrom(binding)) {
                return "LineString";
            } else {
                return "Geometry";
            }
        }
    }

}
