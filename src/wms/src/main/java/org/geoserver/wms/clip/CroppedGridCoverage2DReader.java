/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.decorators.DecoratingGridCoverage2DReader;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.JTS;
import org.geotools.image.ImageWorker;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

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
        this.roiGeom =
                ClippedFeatureSource.reproject(delegate.getCoordinateReferenceSystem(), roiGeom);
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
    public GeneralBounds getOriginalEnvelope() {
        GeneralBounds originalEnvelope = super.getOriginalEnvelope();
        try {
            // clip original envelope with ROI
            Geometry envIntersection =
                    roiGeom.intersection(JTS.toGeometry(originalEnvelope.toRectangle2D()));
            envIntersection.setUserData(originalEnvelope.getCoordinateReferenceSystem());
            return GeneralBounds.toGeneralEnvelope(JTS.toEnvelope(envIntersection));
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
        Hints hints = new Hints(ImageWorker.FORCE_MOSAIC_ROI_PROPERTY, true);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, hints);
    }
}
