/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.decorators.DecoratingGridCoverage2DReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** @author ImranR */
public class CroppedGridCoverage2DReader extends DecoratingGridCoverage2DReader {
    /** Parameters used to control the {@link Crop} operation. */
    private static final ParameterValueGroup cropParams;

    static final Logger LOGGER =
            Logging.getLogger(CroppedGridCoverage2DReader.class.getCanonicalName());

    static {
        final CoverageProcessor processor =
                new CoverageProcessor(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        cropParams = processor.getOperation("CoverageCrop").getParameters();
    }

    /** Cached crop factory */
    private static final Crop coverageCropFactory = new Crop();

    Geometry roiGeom;

    public CroppedGridCoverage2DReader(GridCoverage2DReader delegate, Geometry roiGeom) {
        super(delegate);
        this.roiGeom = reproject(delegate.getCoordinateReferenceSystem(), roiGeom);
    }

    private Geometry reproject(CoordinateReferenceSystem gridCRS, Geometry clipGeom) {
        // re-project if required
        try {
            CoordinateReferenceSystem geomCRS = CRS.decode("EPSG:" + clipGeom.getSRID());
            if (CRS.isTransformationRequired(geomCRS, gridCRS)) {
                MathTransform mt = CRS.findMathTransform(geomCRS, gridCRS);
                clipGeom = JTS.transform(clipGeom, mt);
                clipGeom.setSRID(CRS.lookupEpsgCode(gridCRS, false));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failed to reproject " + clipGeom.toText());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return clipGeom;
    }

    @Override
    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        return delegate.getInfo(coverageName);
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {

        return getCroppedGrid(super.read(parameters), roiGeom);
    }

    @Override
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return getCroppedGrid(super.read(coverageName, parameters), roiGeom);
    }

    @Override
    public GeneralEnvelope getOriginalEnvelope() {
        GeneralEnvelope originalEnvelope = super.getOriginalEnvelope();
        try {
            // clip original envelope with ROI
            Geometry envIntersection =
                    roiGeom.intersection(JTS.toGeometry(originalEnvelope.toRectangle2D()));
            envIntersection.setSRID(
                    CRS.lookupEpsgCode(originalEnvelope.getCoordinateReferenceSystem(), false));
            return GeneralEnvelope.toGeneralEnvelope(JTS.toEnvelope(envIntersection));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return originalEnvelope;
    }

    private static synchronized GridCoverage2D getCroppedGrid(
            GridCoverage2D grid, Geometry clipGeom) {
        final ParameterValueGroup param = cropParams.clone();
        param.parameter("source").setValue(grid);
        param.parameter("ROI").setValue(clipGeom);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, null);
    }
}
