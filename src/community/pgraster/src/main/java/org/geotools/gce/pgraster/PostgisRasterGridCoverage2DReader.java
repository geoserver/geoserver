/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.geotools.gce.pgraster;

import com.google.common.base.Stopwatch;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageReadParam;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.DataSourceException;
import org.geotools.gce.pgraster.config.Config;
import org.geotools.gce.pgraster.reader.ImageComposerThread;
import org.geotools.gce.pgraster.reader.ImageLevelInfo;
import org.geotools.gce.pgraster.reader.JDBCAccess;
import org.geotools.gce.pgraster.reader.PostgisRasterReaderState;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.BufferedCoordinateOperationFactory;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.TransformException;

/**
 * This reader is responsible for providing access to images and image pyramids stored in a JDBC
 * database as Postgis_Raster extension tiles.
 *
 * @author mcr
 * @since 2.5
 */
public class PostgisRasterGridCoverage2DReader extends AbstractGridCoverage2DReader {
    private static final Logger LOGGER = Logging.getLogger(PostgisRasterGridCoverage2DReader.class);

    protected static final CoordinateOperationFactory operationFactory =
            new BufferedCoordinateOperationFactory(
                    new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));

    private final JDBCAccess jdbcAccess;

    private Config config;

    private static Set<AxisDirection> UPDirections;

    private static Set<AxisDirection> LEFTDirections;

    // class initializer
    static {
        LEFTDirections = new HashSet<>();
        LEFTDirections.add(AxisDirection.DISPLAY_LEFT);
        LEFTDirections.add(AxisDirection.EAST);
        LEFTDirections.add(AxisDirection.GEOCENTRIC_X);
        LEFTDirections.add(AxisDirection.COLUMN_POSITIVE);

        UPDirections = new HashSet<>();
        UPDirections.add(AxisDirection.DISPLAY_UP);
        UPDirections.add(AxisDirection.NORTH);
        UPDirections.add(AxisDirection.GEOCENTRIC_Y);
        UPDirections.add(AxisDirection.ROW_POSITIVE);
    }

    /** @param source The source object. */
    public PostgisRasterGridCoverage2DReader(Object source, Hints uHints)
            throws IOException, MalformedURLException {
        this.source = source;

        URL url = PostgisRasterFormat.getURLFromSource(source);

        if (url == null) {
            throw new MalformedURLException(source.toString());
        }

        try {
            config = Config.readFrom(url);
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            throw new IOException(e);
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // Forcing longitude first since the geotiff specification seems to
        // assume that we have first longitude the latitude.
        //
        // /////////////////////////////////////////////////////////////////////
        if (uHints != null) {
            // prevent the use from reordering axes
            this.hints.add(uHints);
        }

        coverageName = config.getCoverageName();
        this.coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(this.hints);

        // /////////////////////////////////////////////////////////////////////
        //
        // Load tiles informations, especially the bounds, which will be
        // reused
        //
        // /////////////////////////////////////////////////////////////////////
        try {
            jdbcAccess = JDBCAccess.getJDBCAcess(config);
        } catch (Exception e1) {
            LOGGER.severe(e1.getLocalizedMessage());
            throw new IOException(e1);
        }

        // get the crs if able to
        final Object tempCRS = this.hints.get(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM);

        if (tempCRS != null) {
            this.crs = (CoordinateReferenceSystem) tempCRS;
            LOGGER.log(
                    Level.WARNING,
                    new StringBuffer("Using forced coordinate reference system ")
                            .append(crs.toWKT())
                            .toString());
        } else if (config.getCoordsys() != null) {
            String srsString = config.getCoordsys();

            try {
                crs = CRS.decode(srsString, false);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not find " + srsString, e);

                return;
            }
        } else {
            CoordinateReferenceSystem tempcrs = jdbcAccess.getLevelInfo(0).getCrs();

            if (tempcrs == null) {
                crs = AbstractGridFormat.getDefaultCRS();
                LOGGER.log(
                        Level.WARNING,
                        new StringBuffer(
                                        "Unable to find a CRS for this coverage, using a default one: ")
                                .append(crs.toWKT())
                                .toString());
            } else {
                crs = tempcrs;
            }
        }

        if (jdbcAccess.getNumOverviews() == -1) {
            String msg = "No levels found fond for coverage: " + config.getCoverageName();
            LOGGER.severe(msg);
            throw new IOException(msg);
        }

        Envelope env = jdbcAccess.getLevelInfo(0).getEnvelope();

        if (env == null) {
            String msg = "Coverage: " + config.getCoverageName() + " is not caluclated";
            LOGGER.severe(msg);
            throw new IOException(msg);
        }

        this.originalEnvelope =
                new GeneralEnvelope(
                        new Rectangle2D.Double(
                                env.getMinX(),
                                env.getMinY(),
                                env.getMaxX() - env.getMinX(),
                                env.getMaxY() - env.getMinY()));
        this.originalEnvelope.setCoordinateReferenceSystem(crs);

        highestRes = jdbcAccess.getLevelInfo(0).getResolution();
        numOverviews = jdbcAccess.getNumOverviews();
        overViewResolutions = new double[numOverviews][];

        for (int i = 0; i < numOverviews; i++) {
            overViewResolutions[i] = jdbcAccess.getLevelInfo(i + 1).getResolution();
        }

        originalGridRange =
                new GridEnvelope2D(
                        new Rectangle(
                                (int) Math.round(originalEnvelope.getSpan(0) / highestRes[0]),
                                (int) Math.round(originalEnvelope.getSpan(1) / highestRes[1])));
    }

    /**
     * Constructor.
     *
     * @param source The source object.
     */
    public PostgisRasterGridCoverage2DReader(Object source) throws IOException {
        this(source, null);
    }

    @Override
    public Format getFormat() {
        return new PostgisRasterFormat();
    }

    private void logRequestParams(GeneralParameterValue[] params) {
        LOGGER.info("----PARAMS START-------");

        for (GeneralParameterValue param : params) {
            @SuppressWarnings("unchecked")
            Parameter<Object> p = (Parameter<Object>) param;
            LOGGER.info(p.getDescriptor().getName().toString() + ": " + p.getValue());
        }

        LOGGER.info("----PARAMS END-------");
    }

    /*
     * @see
     * org.opengis.coverage.grid.GridCoverageReader#read(org.opengis.parameter.GeneralParameterValue
     * [])
     */
    @Override
    public GridCoverage2D read(GeneralParameterValue... params) throws IOException {
        logRequestParams(params);
        PostgisRasterReaderState state = new PostgisRasterReaderState();
        final Stopwatch sw = Stopwatch.createStarted();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Reading mosaic from " + coverageName);
            LOGGER.fine(
                    new StringBuffer("Highest res ")
                            .append(highestRes[0])
                            .append(" ")
                            .append(highestRes[1])
                            .toString());
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // Checking params
        //
        // /////////////////////////////////////////////////////////////////////
        state.setBackgroundColor(PostgisRasterFormat.BACKGROUND_COLOR.getDefaultValue());
        state.setOutputTransparentColor(
                PostgisRasterFormat.OUTPUT_TRANSPARENT_COLOR.getDefaultValue());

        if (params != null) {
            for (GeneralParameterValue generalParameterValue : params) {
                @SuppressWarnings("unchecked")
                Parameter<Object> param = (Parameter<Object>) generalParameterValue;

                if (param.getDescriptor()
                        .getName()
                        .getCode()
                        .equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                    final GridGeometry2D gg = (GridGeometry2D) param.getValue();
                    state.setRequestedEnvelope((GeneralEnvelope) gg.getEnvelope());
                    state.setRenderedImageRectangle(gg.getGridRange2D().getBounds());
                } else if (param.getDescriptor()
                        .getName()
                        .getCode()
                        .equals(PostgisRasterFormat.BACKGROUND_COLOR.getName().toString())) {
                    state.setBackgroundColor((Color) param.getValue());
                } else if (param.getDescriptor()
                        .getName()
                        .getCode()
                        .equals(
                                PostgisRasterFormat.OUTPUT_TRANSPARENT_COLOR
                                        .getName()
                                        .toString())) {
                    state.setOutputTransparentColor((Color) param.getValue());
                }
            }
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // Loading tiles trying to optimize as much as possible
        //
        // /////////////////////////////////////////////////////////////////////
        GridCoverage2D coverage = loadTiles(state);
        LOGGER.info("Mosaic Reader needs : " + sw.stop());

        return coverage;
    }

    /** transforms (if necessary) the requested envelope into the CRS used by this reader. */
    private void transformRequestedEnvelope(PostgisRasterReaderState state)
            throws DataSourceException {

        if (CRS.equalsIgnoreMetadata(
                state.getRequestedEnvelope().getCoordinateReferenceSystem(), crs)) {
            state.setRequestedEnvelopeTransformed(state.getRequestedEnvelope());

            return; // and finish
        }

        try {
            /** Buffered factory for coordinate operations. */

            // transforming the envelope back to the dataset crs in
            CoordinateOperation op =
                    operationFactory.createOperation(
                            state.getRequestedEnvelope().getCoordinateReferenceSystem(), crs);

            if (op.getMathTransform().isIdentity()) { // Identity Transform ?
                state.setRequestedEnvelopeTransformed(state.getRequestedEnvelope());
                return; // and finish
            }

            state.setRequestedEnvelopeTransformed(CRS.transform(op, state.getRequestedEnvelope()));
            state.getRequestedEnvelopeTransformed().setCoordinateReferenceSystem(crs);

            if (config.getIgnoreAxisOrder() == false) { // check for axis order required
                int indexX = indexOfX(crs);
                int indexY = indexOfY(crs);
                int indexRequestedX =
                        indexOfX(state.getRequestedEnvelope().getCoordinateReferenceSystem());
                int indexRequestedY =
                        indexOfY(state.getRequestedEnvelope().getCoordinateReferenceSystem());

                // x Axis problem ???
                if (indexX == indexRequestedY && indexY == indexRequestedX) {
                    state.setXAxisSwitch(true);
                    Rectangle2D tmp =
                            new Rectangle2D.Double(
                                    state.getRequestedEnvelopeTransformed().getMinimum(1),
                                    state.getRequestedEnvelopeTransformed().getMinimum(0),
                                    state.getRequestedEnvelopeTransformed().getSpan(1),
                                    state.getRequestedEnvelopeTransformed().getSpan(0));
                    state.setRequestedEnvelopeTransformed(new GeneralEnvelope(tmp));
                    state.getRequestedEnvelopeTransformed().setCoordinateReferenceSystem(crs);
                } else if (indexX != indexRequestedX || indexY != indexRequestedY) {
                    throw new DataSourceException("Unable to resolve the X Axis problem");
                }
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        new StringBuffer("Reprojected envelope ")
                                .append(state.getRequestedEnvelope().toString())
                                .append(" crs ")
                                .append(crs.toWKT())
                                .toString());
            }
        } catch (Exception e) {
            throw new DataSourceException("Unable to create a coverage for this source", e);
        }
    }

    /** Expand the Transformed Requested Envelope to fit the virtual tiles grid. * */
    private void expandRequestedEnvelope(PostgisRasterReaderState state) {

        GeneralEnvelope ret = state.getRequestedEnvelopeTransformed();
        ImageLevelInfo levelInfo = state.getImageLevelInfo();

        double xminRe = ret.getMinimum(0);
        double xmaxRe = ret.getMaximum(0);
        double yminRe = ret.getMinimum(1);
        double ymaxRe = ret.getMaximum(1);

        double xminLv = levelInfo.getExtentMinX();
        double yminLv = levelInfo.getExtentMinY();
        double xresLv = levelInfo.getResX();
        double yresLv = levelInfo.getResY();

        // pad one more pixel for a better interpolation
        double xminXt = (Math.floor((xminRe - xminLv) / xresLv) - 1) * xresLv + xminLv;
        double yminXt = (Math.floor((yminRe - yminLv) / yresLv) - 1) * yresLv + yminLv;
        double[] minXY = {xminXt, yminXt};

        // pad one more pixel for a better interpolation
        double xmaxXt = (Math.ceil((xmaxRe - xminLv) / xresLv) + 1) * xresLv + xminLv;
        double ymaxXt = (Math.ceil((ymaxRe - yminLv) / yresLv) + 1) * yresLv + yminLv;
        double[] maxXY = {xmaxXt, ymaxXt};

        GeneralEnvelope expanded = new GeneralEnvelope(minXY, maxXY);
        expanded.setCoordinateReferenceSystem(ret.getCoordinateReferenceSystem());

        state.setRequestedEnvelopeTransformedExpanded(expanded);
    }

    /** @return the gridcoverage as the final result */
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF") // pixelDimension gets into the ImageComposerThread
    // and is eventually dereferenced by some call to base constructor. Verified the bug is here,
    // just don't know how to fix it
    private GridCoverage2D loadTiles(PostgisRasterReaderState state) throws IOException {
        Rectangle renderedImageRectangle = state.getRenderedImageRectangle();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    new StringBuffer("Creating mosaic to comply with envelope ")
                            .append(
                                    (state.getRequestedEnvelope() != null)
                                            ? state.getRequestedEnvelope().toString()
                                            : null)
                            .append(" crs ")
                            .append(crs.toWKT())
                            .append(" dim ")
                            .append(
                                    (renderedImageRectangle == null)
                                            ? " null"
                                            : renderedImageRectangle.toString())
                            .toString());
        }

        transformRequestedEnvelope(state);

        // /////////////////////////////////////////////////////////////////////
        //
        // Check if we have something to load by intersecting the requested
        // envelope with the bounds of the data set. If not, give warning
        //
        // /////////////////////////////////////////////////////////////////////
        if (!state.getRequestedEnvelopeTransformed().intersects(this.originalEnvelope, true)) {
            LOGGER.warning("The requested envelope does not intersect the envelope of this mosaic");
            LOGGER.warning(state.getRequestedEnvelopeTransformed().toString());
            LOGGER.warning(originalEnvelope.toString());

            // return coverageFactory.create(coverageName, getEmptyImage((int)
            // pixelDimension
            // .getWidth(), (int) pixelDimension.getHeight(), backgroundColor,
            // outputTransparentColor), state
            // .getRequestedEnvelope());
            return null;
        }

        // /////////////////////////////////////////////////////////////////////
        //
        // Load feaures from the index
        // In case there are no features under the requested bbox which is legal
        // in case the mosaic is not a real sqare, we return a fake mosaic.
        //
        // /////////////////////////////////////////////////////////////////////
        final ImageReadParam readP = new ImageReadParam();
        Integer imageChoice = null;
        if (renderedImageRectangle != null) {
            try {
                imageChoice =
                        setReadParams(
                                OverviewPolicy.getDefaultPolicy(),
                                readP,
                                state.getRequestedEnvelopeTransformed(),
                                renderedImageRectangle);
                readP.setSourceSubsampling(1, 1, 0, 0);
            } catch (TransformException e) {
                LOGGER.severe(e.getLocalizedMessage());
                BufferedImage emptyImage =
                        state.getEmptyImage(
                                (int) renderedImageRectangle.getWidth(),
                                (int) renderedImageRectangle.getHeight());
                GeneralEnvelope envelope = state.getRequestedEnvelope();
                return coverageFactory.create(coverageName, emptyImage, envelope);
            }
        }
        if (imageChoice == null) {
            imageChoice = Integer.valueOf(0);
        }

        ImageLevelInfo levelInfo = jdbcAccess.getLevelInfo(imageChoice.intValue());
        state.setImageLevelInfo(levelInfo);
        LOGGER.info(
                "Coverage "
                        + levelInfo.getCoverageName()
                        + " using spatial table "
                        + levelInfo.getSpatialTableName()
                        + ", image table "
                        + levelInfo.getTileTableName());

        expandRequestedEnvelope(state);

        ImageComposerThread imageComposerThread =
                new ImageComposerThread(state, config, coverageFactory);
        imageComposerThread.start();

        jdbcAccess.startTileDecoders(
                state.getRequestedEnvelopeTransformedExpanded(),
                levelInfo,
                state.getTileQueue(),
                coverageFactory);

        try {
            imageComposerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        GridCoverage2D result = imageComposerThread.getGridCoverage2D();
        if (result == null) {
            return null;
        }

        return transformResult(result, state);
    }

    private GridCoverage2D transformResult(
            GridCoverage2D coverage, PostgisRasterReaderState state) {
        if (state.getRequestedEnvelopeTransformed() == state.getRequestedEnvelope()) {
            return coverage; // nothing to do
        }

        LOGGER.info("Image reprojection necessairy");
        // coverage.show();
        GridCoverage2D result =
                (GridCoverage2D)
                        Operations.DEFAULT.resample(
                                coverage,
                                state.getRequestedEnvelope().getCoordinateReferenceSystem());
        // result.show();
        // result = (GridCoverage2D) Operations.DEFAULT.crop(result,
        // requestedEnvelope);
        // result.show();
        // result = (GridCoverage2D) Operations.DEFAULT.scale(result, 1, 1,
        // -result.getRenderedImage().getMinX(),
        // -result.getRenderedImage().getMinY());
        //
        // result.show();
        // double scalex = pixelDimension.getWidth() / result.getRenderedImage()
        // .getWidth();
        // double scaley = pixelDimension.getHeight() /
        // result.getRenderedImage()
        // .getHeight();
        // result = (GridCoverage2D) Operations.DEFAULT.scale(result, scalex,
        // scaley, 0, 0);

        // avoid lazy calculation
        // RenderedImageAdapter adapter = new
        // RenderedImageAdapter(result.getRenderedImage());
        // /BufferedImage resultImage = adapter.getAsBufferedImage();

        return coverageFactory.create(
                result.getName(), result.getRenderedImage(), result.getEnvelope());
    }

    /**
     * @param crs CoordinateReference System
     * @return dimension index of y dir in crs
     */
    private int indexOfY(CoordinateReferenceSystem crs) {
        return indexOf(crs, UPDirections);
    }

    /**
     * @param crs CoordinateReference System
     * @return dimension index of X dir in crs
     */
    private int indexOfX(CoordinateReferenceSystem crs) {
        return indexOf(crs, LEFTDirections);
    }

    private int indexOf(CoordinateReferenceSystem crs, Set<AxisDirection> direction) {
        CoordinateSystem cs = crs.getCoordinateSystem();
        for (int index = 0; index < cs.getDimension(); index++) {
            CoordinateSystemAxis axis = cs.getAxis(index);
            if (direction.contains(axis.getDirection())) {
                return index;
            }
        }
        return -1;
    }
}
