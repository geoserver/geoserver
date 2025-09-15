/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.ClipProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/** @author ImranR */
public class ClippedFeatureSource<T extends FeatureType, F extends Feature> extends DecoratingFeatureSource<T, F> {
    static final Logger LOGGER = Logging.getLogger(ClippedFeatureSource.class);

    Geometry clip;

    public ClippedFeatureSource(FeatureSource<T, F> delegate, Geometry clipGeometry) {
        super(delegate);
        this.clip = reproject(delegate.getInfo().getCRS(), clipGeometry);
    }

    static Geometry reproject(CoordinateReferenceSystem fsCRS, Geometry clipGeom) {
        // re-project if required
        try {
            CoordinateReferenceSystem geomCRS = getGeometryCRS(clipGeom);
            if (CRS.isTransformationRequired(geomCRS, fsCRS)) {
                MathTransform mt = CRS.findMathTransform(geomCRS, fsCRS);
                clipGeom = JTS.transform(clipGeom, mt);
                clipGeom.setUserData(fsCRS);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failed to reproject " + clipGeom.toText());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return clipGeom;
    }

    private static CoordinateReferenceSystem getGeometryCRS(Geometry clipGeom) throws FactoryException {
        if (clipGeom.getUserData() instanceof CoordinateReferenceSystem) {
            return (CoordinateReferenceSystem) clipGeom.getUserData();
        } else {
            return CRS.decode("EPSG:" + clipGeom.getSRID());
        }
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        ReferencedEnvelope orignalBounds = super.getBounds();
        return JTS.toEnvelope(clip.intersection(JTS.toGeometry(orignalBounds)));
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return this.getFeatures(query).getBounds();
    }

    @Override
    public int getCount(Query query) throws IOException {
        return this.getFeatures(query).size();
    }

    @Override
    public FeatureCollection<T, F> getFeatures() throws IOException {
        return getClippedCollection(super.getFeatures(), clip);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return getClippedCollection(super.getFeatures(filter), clip);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        return getClippedCollection(super.getFeatures(query), clip);
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection<T, F> getClippedCollection(FeatureCollection<T, F> fc, Geometry clipGeom) {
        if (fc instanceof SimpleFeatureCollection collection) {
            return (FeatureCollection<T, F>) new ClipProcess().execute(collection, clipGeom, false);
        }
        return fc;
    }

    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        return iface != null && iface.isAssignableFrom(this.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        try {
            if (iface != null && iface.isAssignableFrom(this.getClass())) {
                return (T) this;
            }
            throw new java.sql.SQLException("Auto-generated unwrap failed; Revisit implementation");
        } catch (Exception e) {
            throw new java.sql.SQLException(e);
        }
    }
}
