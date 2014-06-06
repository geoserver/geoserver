/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.core.geometry;

import java.util.NoSuchElementException;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public enum SpatialRelationship {
    INTERSECTS("SpatialRelIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return filters.intersects(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.intersects(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    CONTAINS("SpatialRelContains") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return filters.contains(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.contains(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    CROSSES("SpatialRelCrosses") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return filters.crosses(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.crosses(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    ENVELOPE_INTERSECTS("SpatialRelEnvelopeIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return filters.bbox(geometryProperty, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), null);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return createEnvelopeFilter(geometryProperty, JTS.toEnvelope(geometry), relationParam);
        }
    },
    
    INDEX_INTERSECTS("SpatialRelIndexIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return ENVELOPE_INTERSECTS.createEnvelopeFilter(geometryProperty, envelope, relationParam);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return ENVELOPE_INTERSECTS.createGeometryFilter(geometryProperty, geometry, relationParam);
        }
    },
    
    OVERLAPS("SpatialRelOverlaps") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return filters.overlaps(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.overlaps(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    TOUCHES("SpatialRelTouches") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope), relationParam);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.touches(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    WITHIN("SpatialRelWithin") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope), relationParam);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.within(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    RELATION("SpatialRelRelation") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope), relationParam);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam) {
            return filters.equals(filters.literal(true), 
                    filters.function("relatePattern", filters.property(geometryProperty), filters.literal(geometry), filters.literal(relationParam)));
        }
    };
    
    private static final FilterFactory2 filters = CommonFactoryFinder.getFilterFactory2();
    
    private final String name;
    
    SpatialRelationship(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    static public SpatialRelationship fromRequestString(String requestString) {
        for (SpatialRelationship sr : values()) {
            if (sr.getName().equals(requestString)) {
                return sr;
            }
        }
        throw new NoSuchElementException(requestString + " is not a recognized type of spatial relationship");
    }

    public abstract Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope, String relationParam);

    public abstract Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry, String relationParam);
}
