/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.ClipProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** @author ImranR */
public class ClippedFeatureCollection<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureCollection<T, F> {

    static final Logger LOGGER = Logging.getLogger(ClippedFeatureCollection.class);

    Geometry clip;

    public ClippedFeatureCollection(FeatureCollection<T, F> delegate, Geometry clipGeometry) {
        super(delegate);

        this.clip = reproject(delegate.getSchema().getCoordinateReferenceSystem(), clipGeometry);
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
    public FeatureIterator<F> features() {
        return getClippedCollection(delegate, clip);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        ReferencedEnvelope orignalBounds = super.getBounds();
        return JTS.toEnvelope(clip.intersection(JTS.toGeometry(orignalBounds)));
    }

    @SuppressWarnings("unchecked")
    private FeatureIterator<F> getClippedCollection(FeatureCollection<T, F> fc, Geometry clipGeom) {
        if (fc instanceof SimpleFeatureCollection) {
            return ((FeatureCollection<T, F>)
                            new ClipProcess()
                                    .execute((SimpleFeatureCollection) fc, clipGeom, false))
                    .features();
        }
        return fc.features();
    }
}
