/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.ClipProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** @author ImranR */
public class ClippedFeatureSource extends DecoratingFeatureSource {
    static final Logger LOGGER = Logging.getLogger(ClippedFeatureSource.class.getCanonicalName());
    static final ClipProcess clipProcess = new ClipProcess();

    Geometry clip;

    public ClippedFeatureSource(FeatureSource delegate, Geometry clipGeometry) {
        super(delegate);
        this.clip = reproject(delegate.getInfo().getCRS(), clipGeometry);
    }

    private Geometry reproject(CoordinateReferenceSystem fsCRS, Geometry clipGeom) {
        // re-project if required
        try {
            CoordinateReferenceSystem geomCRS = CRS.decode("EPSG:" + clipGeom.getSRID());
            if (CRS.isTransformationRequired(geomCRS, fsCRS)) {
                MathTransform mt = CRS.findMathTransform(geomCRS, fsCRS);
                clipGeom = JTS.transform(clipGeom, mt);
                clipGeom.setSRID(CRS.lookupEpsgCode(fsCRS, false));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failed to reproject " + clipGeom.toText());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return clipGeom;
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
    public FeatureCollection getFeatures() throws IOException {
        FeatureCollection fc = super.getFeatures();
        if (SimpleFeatureCollection.class.isAssignableFrom(fc.getClass())) {
            return getClippedCollection((SimpleFeatureCollection) fc, clip);
        }
        return fc;
    }

    @Override
    public FeatureCollection getFeatures(Filter filter) throws IOException {
        FeatureCollection fc = super.getFeatures(filter);
        if (SimpleFeatureCollection.class.isAssignableFrom(fc.getClass())) {
            return getClippedCollection((SimpleFeatureCollection) fc, clip);
        }
        return fc;
    }

    @Override
    public FeatureCollection getFeatures(Query query) throws IOException {
        FeatureCollection fc = super.getFeatures(query);
        if (SimpleFeatureCollection.class.isAssignableFrom(fc.getClass())) {
            return getClippedCollection((SimpleFeatureCollection) fc, clip);
        }
        return fc;
    }

    private static synchronized FeatureCollection getClippedCollection(
            SimpleFeatureCollection simpleFc, Geometry clipGeom) {
        return clipProcess.execute(simpleFc, clipGeom, false);
    }
}
