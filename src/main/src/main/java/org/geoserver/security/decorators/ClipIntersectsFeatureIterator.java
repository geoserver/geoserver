/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.HashMap;
import java.util.Map;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.ClippedFeatureIterator;
import org.locationtech.jts.geom.Geometry;

/**
 * A SimpleFeatureCollection that can filter features' geometries by a clip (crop) spatialFilter and
 * by an intersects spatialFilter. If a geometry is hit by both the result of the two filters is
 * merged.
 */
class ClipIntersectsFeatureIterator extends ClippedFeatureIterator {
    private Geometry intersects;

    /**
     * @param delegate delegate Iterator to be used as a delegate.
     * @param clip the geometry to be used to clip (crop features).
     * @param intersects the geometry to be used to intersects features.
     * @param schema the featureType
     * @param preserveZ flag to set to true if the clipping process should preserve the z dimension
     */
    ClipIntersectsFeatureIterator(
            SimpleFeatureIterator delegate,
            Geometry clip,
            Geometry intersects,
            SimpleFeatureType schema,
            boolean preserveZ) {
        super(delegate, clip, schema, preserveZ);
        this.intersects = intersects;
    }

    @Override
    public boolean hasNext() {

        while (next == null && delegate.hasNext()) {
            // try building the clipped feature out of the original feature, if the
            // default geometry is clipped out, skip it
            SimpleFeature f = delegate.next();

            boolean doTheClip = intersects == null ? true : false;

            Map<Name, Geometry> intersectedGeometries = null;
            if (intersects != null) {
                Map<Name, Geometry> geometryAttributes = extractGeometryAttributes(f);
                intersectedGeometries =
                        getIntersectingGeometries(geometryAttributes, f.getFeatureType());
                // if there is at least one geometryCollection or not all the geometry
                // attributes were intersected performs also the clip
                if (intersectedGeometries != null)
                    doTheClip = geometryAttributes.size() > intersectedGeometries.size();
            }

            boolean clippedOut = false;
            if (doTheClip) clippedOut = prepareBuilderForNextFeature(f);

            if (!clippedOut && doTheClip) {
                // build the next feature
                next = fb.buildFeature(f.getID());
                unionWithIntersected(intersectedGeometries);

            } else if (intersectedGeometries != null && !intersectedGeometries.isEmpty()) {
                next = f;
                for (Name name : intersectedGeometries.keySet()) {
                    next.setAttribute(name, intersectedGeometries.get(name));
                }
            }

            fb.reset();
        }

        return next != null;
    }

    // union the clipped geometries with the intersected one
    private void unionWithIntersected(Map<Name, Geometry> intersectedGeometries) {
        for (Name name : intersectedGeometries.keySet()) {
            Geometry intersected = intersectedGeometries.get(name);
            if (intersected != null && !intersected.isEmpty())
                next.setAttribute(name, ((Geometry) next.getAttribute(name)).union(intersected));
        }
    }

    private Map<Name, Geometry> getIntersectingGeometries(
            Map<Name, Geometry> geometryAttributes, SimpleFeatureType type) {
        Map<Name, Geometry> intersectedGeometries = new HashMap<>();
        for (Name name : geometryAttributes.keySet()) {
            Geometry geom = geometryAttributes.get(name);
            if (geom.intersects(intersects)) {
                intersectedGeometries.put(name, geom);
            }
        }
        return intersectedGeometries;
    }

    private Map<Name, Geometry> extractGeometryAttributes(SimpleFeature f) {
        Map<Name, Geometry> geometryAttributes = new HashMap<>();
        for (AttributeDescriptor ad : f.getFeatureType().getAttributeDescriptors()) {
            Object attribute = f.getAttribute(ad.getName());
            if (ad instanceof GeometryDescriptor) {
                geometryAttributes.put(ad.getName(), (Geometry) attribute);
            }
        }
        return geometryAttributes;
    }
}
