/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.importer.ImportTask;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

public class ReprojectTransform extends AbstractTransform implements InlineVectorTransform {

    private static final long serialVersionUID = 1L;

    CoordinateReferenceSystem source, target;
    transient MathTransform transform;

    public CoordinateReferenceSystem getSource() {
        return source;
    }

    public void setSource(CoordinateReferenceSystem source) {
        this.source = source;
    }

    public CoordinateReferenceSystem getTarget() {
        return target;
    }

    public void setTarget(CoordinateReferenceSystem target) {
        this.target = target;
    }

    public ReprojectTransform(CoordinateReferenceSystem target) {
        this(null, target);
    }

    public ReprojectTransform(CoordinateReferenceSystem source, CoordinateReferenceSystem target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public SimpleFeatureType apply(
            ImportTask task, DataStore dataStore, SimpleFeatureType featureType) throws Exception {

        // update the layer metadata
        ResourceInfo r = task.getLayer().getResource();
        r.setNativeCRS(target);
        r.setSRS(ResourcePool.lookupIdentifier(target, true));
        if (r.getNativeBoundingBox() != null) {
            r.setNativeBoundingBox(r.getNativeBoundingBox().transform(target, true));
        }
        // retype the schema
        return SimpleFeatureTypeBuilder.retype(featureType, target);
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        if (transform == null) {
            // compute the reprojection transform
            CoordinateReferenceSystem source = this.source;
            if (source == null) {
                // try to determine source crs from data
                source = oldFeature.getType().getCoordinateReferenceSystem();
            }

            if (source == null) {
                throw new IllegalStateException("Unable to determine source projection");
            }

            transform = CRS.findMathTransform(source, target, true);
        }

        Geometry g = (Geometry) oldFeature.getDefaultGeometry();
        if (g != null) {
            feature.setDefaultGeometry(JTS.transform(g, transform));
        }
        return feature;
    }

    @Override
    public String toString() {
        return "ReprojectTransform{"
                + "source="
                + source
                + ", target="
                + target
                + ", transform="
                + transform
                + '}';
    }
}
