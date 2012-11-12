package org.opengeo.gsr.core.geometry;

import java.util.NoSuchElementException;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public enum SpatialRelationship {
    INTERSECTS("SpatialRelIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return filters.intersects(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.intersects(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    CONTAINS("SpatialRelContains") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return filters.contains(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.contains(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    CROSSES("SpatialRelCrosses") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return filters.crosses(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.crosses(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    ENVELOPE_INTERSECTS("SpatialRelEnvelopeIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return filters.bbox(geometryProperty, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), null);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return createEnvelopeFilter(geometryProperty, JTS.toEnvelope(geometry));
        }
    },
    
    INDEX_INTERSECTS("SpatialRelIndexIntersects") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return ENVELOPE_INTERSECTS.createEnvelopeFilter(geometryProperty, envelope);
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return ENVELOPE_INTERSECTS.createGeometryFilter(geometryProperty, geometry);
        }
    },
    
    OVERLAPS("SpatialRelOverlaps") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return filters.overlaps(filters.property(geometryProperty), filters.literal(JTS.toGeometry(envelope)));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.overlaps(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    TOUCHES("SpatialRelTouches") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.touches(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    WITHIN("SpatialRelWithin") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            return filters.within(filters.property(geometryProperty), filters.literal(geometry));
        }
    },
    
    RELATION("SpatialRelRelation") {
        @Override
        public Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope) {
            return createGeometryFilter(geometryProperty, JTS.toGeometry(envelope));
        }

        @Override
        public Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry) {
            throw new UnsupportedOperationException("Don't know how to implement the Relation spatialRel type yet");
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

    public abstract Filter createEnvelopeFilter(String geometryProperty, com.vividsolutions.jts.geom.Envelope envelope);

    public abstract Filter createGeometryFilter(String geometryProperty, com.vividsolutions.jts.geom.Geometry geometry);
}
