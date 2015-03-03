/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;

import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.CodeType;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.DescribeCoverageType;
import net.opengis.wcs11.DomainSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.GetCapabilitiesType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.GridCrsType;
import net.opengis.wcs11.OutputType;
import net.opengis.wcs11.RangeSubsetType;
import net.opengis.wcs11.TimePeriodType;
import net.opengis.wcs11.TimeSequenceType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.wcs.kvp.GridCS;
import org.geoserver.wcs.kvp.GridType;
import org.geoserver.wcs.response.DescribeCoverageTransformer;
import org.geoserver.wcs.response.WCSCapsTransformer;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class DefaultWebCoverageService111 implements WebCoverageService111 {
    Logger LOGGER = Logging.getLogger(DefaultWebCoverageService111.class);

    private Catalog catalog;

    private GeoServer geoServer;

    private CoverageResponseDelegateFinder responseFactory;

    public DefaultWebCoverageService111(GeoServer geoServer,
            CoverageResponseDelegateFinder responseFactory) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.responseFactory = responseFactory;
    }

    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }

    public WCSCapsTransformer getCapabilities(GetCapabilitiesType request) {
        // do the version negotiation dance
        List<String> provided = new ArrayList<String>();
        // provided.add("1.0.0");
        provided.add("1.1.0");
        provided.add("1.1.1");
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionOws11(provided, accepted);

        // TODO: add support for 1.0.0 in here

        if ("1.1.0".equals(version) || "1.1.1".equals(version)) {
            WCSCapsTransformer capsTransformer = new WCSCapsTransformer(geoServer);
            capsTransformer.setEncoding(Charset.forName((getServiceInfo().getGeoServer()
                    .getSettings().getCharset())));
            return capsTransformer;
        }

        throw new WcsException("Could not understand version:" + version);
    }

    public DescribeCoverageTransformer describeCoverage(DescribeCoverageType request) {
        final String version = request.getVersion();
        if ("1.1.0".equals(version) || "1.1.1".equals(version)) {
            WCSInfo wcs = getServiceInfo();
            DescribeCoverageTransformer describeTransformer = new DescribeCoverageTransformer(wcs,
                    catalog, responseFactory);
            describeTransformer.setEncoding(Charset.forName(wcs.getGeoServer().getSettings()
                    .getCharset()));
            return describeTransformer;
        }

        throw new WcsException("Could not understand version:" + version);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public GridCoverage[] getCoverage(GetCoverageType request) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(new StringBuffer("execute CoverageRequest response. Called request is: ")
                    .append(request).toString());
        }

        WCSInfo wcs = getServiceInfo();

        CoverageInfo meta = null;
        GridCoverage2D coverage = null;
        try {
            CodeType identifier = request.getIdentifier();
            if (identifier == null)
                throw new WcsException("Internal error, the coverage identifier must not be null",
                        InvalidParameterValue, "identifier");
            meta = catalog.getCoverageByName(identifier.getValue());
            if (meta == null) {
                throw new WcsException("No such coverage: " + request.getIdentifier().getValue());
            }

            // first let's run some sanity checks on the inputs
            checkDomainSubset(meta, request.getDomainSubset(), wcs);
            checkRangeSubset(meta, request.getRangeSubset());
            checkOutput(meta, request.getOutput());

            // grab the format, the reader using the default params
            final GridCoverage2DReader reader = (GridCoverage2DReader) meta.getGridCoverageReader(
                    null, WCSUtils.getReaderHints(wcs));

            // handle spatial domain subset, if needed
            final GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
            final BoundingBoxType bbox = request.getDomainSubset().getBoundingBox();
            final CoordinateReferenceSystem nativeCRS = originalEnvelope
                    .getCoordinateReferenceSystem();
            final GeneralEnvelope requestedEnvelopeInNativeCRS;
            final GeneralEnvelope requestedEnvelope;
            if (bbox != null) {
                // first off, parse the envelope corners
                double[] lowerCorner = new double[bbox.getLowerCorner().size()];
                double[] upperCorner = new double[bbox.getUpperCorner().size()];
                for (int i = 0; i < lowerCorner.length; i++) {
                    lowerCorner[i] = (Double) bbox.getLowerCorner().get(i);
                    upperCorner[i] = (Double) bbox.getUpperCorner().get(i);
                }
                requestedEnvelope = new GeneralEnvelope(lowerCorner, upperCorner);
                // grab the native crs
                // if no crs has beens specified, the native one is assumed
                if (bbox.getCrs() == null) {
                    requestedEnvelope.setCoordinateReferenceSystem(nativeCRS);
                    requestedEnvelopeInNativeCRS = requestedEnvelope;
                } else {
                    // otherwise we need to transform
                    final CoordinateReferenceSystem bboxCRS = CRS.decode(bbox.getCrs());
                    requestedEnvelope.setCoordinateReferenceSystem(bboxCRS);
                    if (!CRS.equalsIgnoreMetadata(bboxCRS, nativeCRS)) {
                        CoordinateOperationFactory of = CRS.getCoordinateOperationFactory(true);
                        CoordinateOperation co = of.createOperation(bboxCRS, nativeCRS);
                        requestedEnvelopeInNativeCRS = CRS.transform(co, requestedEnvelope);
                    } else {
                        requestedEnvelopeInNativeCRS = new GeneralEnvelope(requestedEnvelope);
                    }
                }
            } else {
                requestedEnvelopeInNativeCRS = reader.getOriginalEnvelope();
                requestedEnvelope = requestedEnvelopeInNativeCRS;
            }

            final GridCrsType gridCRS = request.getOutput().getGridCRS();

            // Compute the crs that the final coverage will be served into
            final CoordinateReferenceSystem targetCRS;
            if (gridCRS == null) {
                targetCRS = reader.getOriginalEnvelope().getCoordinateReferenceSystem();
            } else {
                targetCRS = CRS.decode(gridCRS.getGridBaseCRS());
            }

            //
            // Raster destination size
            //
            int elevationLevels = 0;
            double[] elevations = null;

            // grab the grid to world transformation
            MathTransform gridToCRS = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);

            //
            // TIME Values
            //
            final List<Date> timeValues = new LinkedList<Date>();

            TimeSequenceType temporalSubset = request.getDomainSubset().getTemporalSubset();

            if (temporalSubset != null && temporalSubset.getTimePosition() != null
                    && temporalSubset.getTimePosition().size() > 0) {
                for (Iterator it = temporalSubset.getTimePosition().iterator(); it.hasNext();) {
                    Date tp = (Date) it.next();
                    timeValues.add(tp);
                }
            } else if (temporalSubset != null && temporalSubset.getTimePeriod() != null
                    && temporalSubset.getTimePeriod().size() > 0) {
                for (Iterator it = temporalSubset.getTimePeriod().iterator(); it.hasNext();) {
                    TimePeriodType tp = (TimePeriodType) it.next();
                    Date beginning = (Date) tp.getBeginPosition();
                    Date ending = (Date) tp.getEndPosition();

                    timeValues.add(beginning);
                    timeValues.add(ending);
                }
            }

            // now we have enough info to read the coverage, grab the parameters
            // and add the grid geometry info
            final GeneralEnvelope intersectionEnvelopeInSourceCRS = new GeneralEnvelope(
                    requestedEnvelopeInNativeCRS);
            intersectionEnvelopeInSourceCRS.intersect(originalEnvelope);

            final GridGeometry2D requestedGridGeometry = new GridGeometry2D(
                    PixelInCell.CELL_CENTER, gridToCRS, intersectionEnvelopeInSourceCRS, null);

            final ParameterValueGroup readParametersDescriptor = reader.getFormat()
                    .getReadParameters();
            GeneralParameterValue[] readParameters = CoverageUtils.getParameters(
                    readParametersDescriptor, meta.getParameters());
            readParameters = (readParameters != null ? readParameters
                    : new GeneralParameterValue[0]);

            //
            // Setting coverage reading params.
            //
            final ParameterValue requestedGridGeometryParam = new DefaultParameterDescriptor(
                    AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(),
                    GeneralGridGeometry.class, null, requestedGridGeometry).createValue();

            /*
             * Test if the parameter "TIME" is present in the WMS request, and by the way in the reading parameters. If it is the case, one can adds
             * it to the request. If an exception is thrown, we have nothing to do.
             */
            final List<GeneralParameterDescriptor> parameterDescriptors = readParametersDescriptor
                    .getDescriptor().descriptors();
            ParameterValue time = null;
            boolean hasTime = timeValues.size() > 0;
            ParameterValue elevation = null;
            boolean hasElevation = elevations != null && !Double.isNaN(elevations[0]);

            if (hasElevation || hasTime) {
                for (GeneralParameterDescriptor pd : parameterDescriptors) {

                    final String code = pd.getName().getCode();

                    //
                    // TIME
                    //
                    if (code.equalsIgnoreCase("TIME")) {
                        time = (ParameterValue) pd.createValue();
                        time.setValue(timeValues);
                    }

                    //
                    // ELEVATION
                    //
                    if (code.equalsIgnoreCase("ELEVATION")) {
                        elevation = (ParameterValue) pd.createValue();
                        elevation.setValue(elevations[0]);
                    }

                    // leave?
                    if ((hasElevation && elevation != null && hasTime && time != null)
                            || !hasElevation && hasTime && time != null || hasElevation
                            && elevation != null && !hasTime)
                        break;
                }
            }
            //
            // add read parameters
            //
            int addedParams = 1 + (hasTime ? 1 : 0) + (hasElevation ? 1 : 0);
            // add to the list
            GeneralParameterValue[] readParametersClone = new GeneralParameterValue[readParameters.length
                    + addedParams--];
            System.arraycopy(readParameters, 0, readParametersClone, 0, readParameters.length);
            readParametersClone[readParameters.length + addedParams--] = requestedGridGeometryParam;
            if (hasTime)
                readParametersClone[readParameters.length + addedParams--] = time;
            if (hasElevation)
                readParametersClone[readParameters.length + addedParams--] = elevation;
            readParameters = readParametersClone;

            // Check we're not being requested to read too much data from input (first check,
            // guesses the grid size using the information contained in CoverageInfo)
            WCSUtils.checkInputLimits(wcs, meta, reader, requestedGridGeometry);

            //
            // Check if we have a filter among the params
            //
            Filter filter = WCSUtils.getRequestFilter();
            if (filter != null) {
                readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                        filter, "FILTER", "Filter");
            }

            //
            // make sure we work in streaming mode
            //
            // work in streaming fashion when JAI is involved
            readParameters = WCSUtils.replaceParameter(readParameters, Boolean.FALSE,
                    AbstractGridFormat.USE_JAI_IMAGEREAD);

            //
            // perform Read ...
            //
            coverage = (GridCoverage2D) reader.read(readParameters);
            if ((coverage == null) || !(coverage instanceof GridCoverage2D)) {
                throw new IOException("The requested coverage could not be found.");
            }

            // now that we have read the coverage double check the input size
            WCSUtils.checkInputLimits(wcs, coverage);

            // some raster sources do not really read less data (arcgrid for example), we may need to crop
            if (!intersectionEnvelopeInSourceCRS.contains(coverage.getEnvelope2D(), true)) {
                coverage = WCSUtils.crop(coverage, intersectionEnvelopeInSourceCRS);
            }

            /**
             * Band Select (works on just one field)
             */
            GridCoverage2D bandSelectedCoverage = coverage;
            String interpolationType = null;
            if (request.getRangeSubset() != null) {
                if (request.getRangeSubset().getFieldSubset().size() > 1) {
                    throw new WcsException("Multi field coverages are not supported yet");
                }

                FieldSubsetType field = (FieldSubsetType) request.getRangeSubset().getFieldSubset()
                        .get(0);
                interpolationType = field.getInterpolationType();

                // handle axis subset
                if (field.getAxisSubset().size() > 1) {
                    throw new WcsException("Multi axis coverages are not supported yet");
                }
                if (field.getAxisSubset().size() == 1) {
                    // prepare a support structure to quickly get the band index
                    // of a
                    // key
                    List<CoverageDimensionInfo> dimensions = meta.getDimensions();
                    Map<String, Integer> dimensionMap = new HashMap<String, Integer>();
                    for (int i = 0; i < dimensions.size(); i++) {
                        String keyName = dimensions.get(i).getName().replace(' ', '_');
                        dimensionMap.put(keyName, i);
                    }

                    // extract the band indexes
                    AxisSubsetType axisSubset = (AxisSubsetType) field.getAxisSubset().get(0);
                    List keys = axisSubset.getKey();
                    int[] bands = new int[keys.size()];
                    for (int j = 0; j < bands.length; j++) {
                        final String key = (String) keys.get(j);
                        Integer index = dimensionMap.get(key);
                        if (index == null)
                            throw new WcsException("Unknown field/axis/key combination "
                                    + field.getIdentifier().getValue() + "/"
                                    + axisSubset.getIdentifier() + "/" + key);
                        bands[j] = index;
                    }

                    // finally execute the band select
                    try {
                        bandSelectedCoverage = (GridCoverage2D) WCSUtils
                                .bandSelect(coverage, bands);
                    } catch (WcsException e) {
                        throw new WcsException(e.getLocalizedMessage());
                    }
                }
            }

            /**
             * Checking for supported Interpolation Methods
             */
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            if (interpolationType != null) {
                if (interpolationType.equalsIgnoreCase("linear") || interpolationType.equalsIgnoreCase("bilinear")) {
                    interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                } else if (interpolationType.equalsIgnoreCase("cubic") || interpolationType.equalsIgnoreCase("bicubic")) {
                    interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
                } else if (interpolationType.equalsIgnoreCase("nearest")) {
                    interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                }
            }

            // adjust the grid geometry to use the final bbox and crs
            final GeneralEnvelope intersectionEnvelope;
            boolean reprojectionNeeded = !CRS.equalsIgnoreMetadata(nativeCRS, targetCRS);
            if (reprojectionNeeded) {
                CoordinateOperationFactory of = CRS.getCoordinateOperationFactory(true);
                CoordinateOperation co = of.createOperation(nativeCRS, targetCRS);
                intersectionEnvelope = CRS.transform(co, intersectionEnvelopeInSourceCRS);
            } else {
                intersectionEnvelope = intersectionEnvelopeInSourceCRS;
            }

            // compute the output resolution
            double pixelSizeX;
            double pixelSizeY;
            if (gridCRS != null) {
                Double[] origin = (Double[]) gridCRS.getGridOrigin();
                Double[] offsets = (Double[]) gridCRS.getGridOffsets();

                // from the specification if grid origin is omitted and the crs
                // is 2d the default it's 0,0
                if (origin == null) {
                    origin = new Double[] { 0.0, 0.0 };
                }

                // if no offsets has been specified we try to default on the
                // native ones
                if (offsets == null) {
                    offsets = estimateOffsets(reader, gridCRS, gridToCRS, intersectionEnvelope,
                            reprojectionNeeded);
                }

                // building the actual transform for the resulting grid geometry
                AffineTransform tx;
                if (gridCRS.getGridType().equals(GridType.GT2dSimpleGrid.getXmlConstant())) {
                    tx = new AffineTransform(offsets[0], 0, 0, offsets[1], origin[0], origin[1]);
                } else if (gridCRS.getGridType().equals(GridType.GT2dGridIn2dCrs.getXmlConstant())) {
                    tx = new AffineTransform(offsets[0], offsets[1], offsets[2], offsets[3],
                            origin[0], origin[1]);
                } else {
                    tx = new AffineTransform(offsets[0], offsets[4], offsets[1], offsets[3],
                            origin[0], origin[1]);

                    if (origin.length != 3 || offsets.length != 6)
                        throw new WcsException("", InvalidParameterValue, "GridCRS");

                    //
                    // ELEVATIONS
                    //

                    // TODO: draft code ... it needs more study!
                    elevationLevels = (int) Math.round(requestedEnvelope.getUpperCorner()
                            .getOrdinate(2) - requestedEnvelope.getLowerCorner().getOrdinate(2));

                    // compute the elevation levels, we have elevationLevels values
                    if (elevationLevels > 0) {
                        elevations = new double[elevationLevels];

                        elevations[0] = requestedEnvelope.getLowerCorner().getOrdinate(2); // TODO put the extrema
                        elevations[elevationLevels - 1] = requestedEnvelope.getUpperCorner()
                                .getOrdinate(2);
                        if (elevationLevels > 2) {
                            final int adjustedLevelsNum = elevationLevels - 1;
                            double step = (elevations[elevationLevels - 1] - elevations[0])
                                    / adjustedLevelsNum;
                            for (int i = 1; i < adjustedLevelsNum; i++)
                                elevations[i] = elevations[i - 1] + step;
                        }
                    }
                }
                pixelSizeX = Math.abs(tx.getScaleX());
                pixelSizeY = Math.abs(tx.getScaleY());
                gridToCRS = new AffineTransform2D(tx);
            } else {
                Double[] offsets = estimateOffsets(reader, gridCRS, gridToCRS,
                        intersectionEnvelope, reprojectionNeeded);
                if (offsets.length == 2) {
                    pixelSizeX = Math.abs(offsets[0]);
                    pixelSizeY = Math.abs(offsets[1]);
                    AffineTransform tx = new AffineTransform(offsets[0], 0, 0, offsets[1], 0, 0);
                    gridToCRS = new AffineTransform2D(tx);
                } else if (offsets.length == 6) {
                    AffineTransform tx = new AffineTransform(offsets[0], offsets[1], offsets[3], 
                            offsets[4], offsets[2], offsets[5]);
                    pixelSizeX = Math.abs(XAffineTransform.getScaleX0(tx));
                    pixelSizeY = Math.abs(XAffineTransform.getScaleY0(tx));
                    gridToCRS = new AffineTransform2D(tx);                    
                } else {
                    AffineTransform tx = new AffineTransform(offsets[0], offsets[1], offsets[3],
                            offsets[4], 0, 0);
                    pixelSizeX = Math.abs(XAffineTransform.getScaleX0(tx));
                    pixelSizeY = Math.abs(XAffineTransform.getScaleY0(tx));
                    gridToCRS = new AffineTransform2D(tx);
                }
            }

            /**
             * Reproject
             */
            // adjust to have at least one pixel in the output
            if (intersectionEnvelope.getSpan(0) < Math.abs(pixelSizeX)) {
                double minX = intersectionEnvelope.getMinimum(0);
                intersectionEnvelope.setRange(0, minX, minX + pixelSizeX);
            }
            if (intersectionEnvelope.getSpan(1) < Math.abs(pixelSizeY)) {
                double minY = intersectionEnvelope.getMinimum(1);
                intersectionEnvelope.setRange(1, minY, minY + pixelSizeY);
            }

            final GridGeometry2D destinationGridGeometry = new GridGeometry2D(
                    PixelInCell.CELL_CENTER, gridToCRS, intersectionEnvelope, null);

            // before extracting the output make sure it's not too big
            WCSUtils.checkOutputLimits(wcs, destinationGridGeometry.getGridRange2D(),
                    bandSelectedCoverage.getRenderedImage().getSampleModel());

            // reproject if necessary
            boolean sameGridGeometry = bandSelectedCoverage.getGridGeometry().equals(
                    destinationGridGeometry);
            if (reprojectionNeeded || !sameGridGeometry) {
                final GridCoverage2D reprojectedCoverage = WCSUtils.resample(bandSelectedCoverage,
                        nativeCRS, targetCRS, destinationGridGeometry, interpolation);

                return new GridCoverage[] { reprojectedCoverage };
            } else {
                return new GridCoverage[] { bandSelectedCoverage };
            }
        } catch (Throwable e) {
            if (coverage != null) {
                CoverageCleanerCallback.addCoverages(coverage);
            }
            if (e instanceof WcsException) {
                throw (WcsException) e;
            } else {
                throw new WcsException(e);
            }
        }

    }

    private Double[] estimateOffsets(final GridCoverage2DReader reader, final GridCrsType gridCRS,
            MathTransform gridToCRS, final GeneralEnvelope intersectionEnvelope,
            boolean reprojectionNeeded) {
        Double[] offsets;
        if (!(gridToCRS instanceof AffineTransform2D) && !(gridToCRS instanceof IdentityTransform))
            throw new WcsException(
                    "Internal error, the coverage we're playing with does not have an affine transform...");

        if (!reprojectionNeeded) {
            if (gridCRS != null) {
                if (gridToCRS instanceof IdentityTransform) {
                    if (gridCRS.getGridType().equals(GridType.GT2dSimpleGrid.getXmlConstant())
                            || gridCRS.getGridType().equals(
                                    GridType.GT2dGridIn2dCrs.getXmlConstant()))
                        offsets = new Double[] { 1.0, -1.0 };
                    else
                        offsets = new Double[] { 1.0, 0.0, 0.0, 0.0, -1.0, 0.0 };
                } else {
                    AffineTransform2D affine = (AffineTransform2D) gridToCRS;
                    if (gridCRS.getGridType().equals(GridType.GT2dSimpleGrid.getXmlConstant())
                            || gridCRS.getGridType().equals(
                                    GridType.GT2dGridIn2dCrs.getXmlConstant()))
                        offsets = new Double[] { affine.getScaleX(), affine.getScaleY() };
                    else
                        offsets = new Double[] { affine.getScaleX(), affine.getShearX(),
                                affine.getShearY(), affine.getScaleY() };
                }
            } else {
                AffineTransform2D at = (AffineTransform2D) gridToCRS;
                offsets = new Double[] { at.getScaleX(), at.getShearX(), at.getTranslateX(), 
                        at.getShearY(), at.getScaleY(), at.getTranslateY() };
            }
        } else {
            // the input resolution is going to be completed unrelated to the output one
            // make an estimate assuming we want to keep the output raster with roughly
            // the same size as the input one
            double teWidth = intersectionEnvelope.getSpan(0);
            double teHeight = intersectionEnvelope.getSpan(1);

            double targetRatio = teWidth / teHeight;
            GridEnvelope gr = reader.getOriginalGridRange();
            int targetRasterWidth = gr.getSpan(0);
            int targetRasterHeight = (int) Math.ceil(targetRasterWidth / targetRatio);
            double scaleX = teWidth / targetRasterWidth;
            double scaleY = -teHeight / targetRasterHeight;

            if (gridCRS == null
                    || gridCRS.getGridType().equals(GridType.GT2dSimpleGrid.getXmlConstant())
                    || gridCRS.getGridType().equals(GridType.GT2dGridIn2dCrs.getXmlConstant())) {
                offsets = new Double[] { scaleX, scaleY };
            } else {
                offsets = new Double[] { scaleX, 0.0, 0.0, 0.0, scaleY, 0.0 };
            }
        }
        return offsets;
    }

    private void checkDomainSubset(CoverageInfo meta, DomainSubsetType domainSubset, WCSInfo wcs)
            throws Exception {
        BoundingBoxType bbox = domainSubset.getBoundingBox();

        // domain subset should actually be always specified, but we try to be more lenient
        // (we should probably have a "strict cite" behavior
        if (bbox == null) {
            return;
        }

        // workaround for http://jira.codehaus.org/browse/GEOT-1710
        if ("urn:ogc:def:crs:OGC:1.3:CRS84".equals(bbox.getCrs())) {
            bbox.setCrs("EPSG:4326");
        }

        CoordinateReferenceSystem bboxCRs = CRS.decode(bbox.getCrs());
        GridCoverage2DReader reader = (GridCoverage2DReader) meta.getGridCoverageReader(null,
                WCSUtils.getReaderHints(wcs));
        Envelope gridEnvelope = reader.getOriginalEnvelope();
        GeneralEnvelope gridEnvelopeBboxCRS = null;
        if (bboxCRs instanceof GeographicCRS) {
            try {
                CoordinateOperationFactory cof = CRS.getCoordinateOperationFactory(true);

                final CoordinateOperation operation = cof.createOperation(
                        gridEnvelope.getCoordinateReferenceSystem(), bboxCRs);
                gridEnvelopeBboxCRS = CRS.transform(operation, gridEnvelope);
            } catch (Exception e) {
                // this may happen, there is nothing we can do about it, we just
                // use the back transformed envelope to be more lenient about
                // which coordinate coorections to make on the longitude axis
                // should the antimeridian style be used
            }
        }

        // check the coordinates, but make sure the case 175,-175 is handled
        // as valid for the longitude axis in a geographic coordinate system
        // see section 7.6.2 of the WCS 1.1.1 spec)
        List<Double> lower = bbox.getLowerCorner();
        List<Double> upper = bbox.getUpperCorner();
        for (int i = 0; i < lower.size(); i++) {
            if (lower.get(i) > upper.get(i)) {
                final CoordinateSystemAxis axis = bboxCRs.getCoordinateSystem().getAxis(i);
                // see if the coordinates can be fixed
                if (bboxCRs instanceof GeographicCRS && axis.getDirection() == AxisDirection.EAST) {

                    if (gridEnvelopeBboxCRS != null) {
                        // try to guess which one needs to be fixed
                        final double envMax = gridEnvelopeBboxCRS.getMaximum(i);
                        if (envMax >= lower.get(i))
                            upper.set(i,
                                    upper.get(i)
                                            + (axis.getMaximumValue() - axis.getMinimumValue()));
                        else
                            lower.set(i,
                                    lower.get(i)
                                            - (axis.getMaximumValue() - axis.getMinimumValue()));

                    } else {
                        // just fix the upper and hope...
                        upper.set(i,
                                upper.get(i) + (axis.getMaximumValue() - axis.getMinimumValue()));
                    }
                }

                // if even after the fix we're in the wrong situation, complain
                if (lower.get(i) > upper.get(i)) {
                    throw new WcsException("illegal bbox, min of dimension " + (i + 1) + ": "
                            + lower.get(i) + " is " + "greater than max of same dimension: "
                            + upper.get(i), WcsExceptionCode.InvalidParameterValue, "BoundingBox");
                }
            }

        }
    }

    /**
     * Checks that the elements of the Output part of the request do make sense by comparing them to the coverage metadata
     *
     * @param info
     * @param rangeSubset
     */
    private void checkOutput(CoverageInfo meta, OutputType output) {
        if (output == null)
            return;

        String format = output.getFormat();
        String declaredFormat = getDeclaredFormat(meta.getSupportedFormats(), format);
        if (declaredFormat == null)
            throw new WcsException("format " + format + " is not supported for this coverage",
                    InvalidParameterValue, "format");

        final GridCrsType gridCRS = output.getGridCRS();
        if (gridCRS != null) {
            // check grid base crs is valid, and eventually default it out
            String gridBaseCrs = gridCRS.getGridBaseCRS();
            if (gridBaseCrs != null) {
                // make sure the requested is among the supported ones, by
                // making a
                // code level
                // comparison (to avoid assuming epsg:xxxx and
                // http://www.opengis.net/gml/srs/epsg.xml#xxx are different
                // ones.
                // We'll also consider the urn one comparable, allowing eventual
                // axis flip on the
                // geographic crs
                String actualCRS = null;
                final String gridBaseCrsCode = extractCode(gridBaseCrs);
                for (Iterator it = meta.getResponseSRS().iterator(); it.hasNext();) {
                    final String responseCRS = (String) it.next();
                    final String code = extractCode(responseCRS);
                    if (code.equalsIgnoreCase(gridBaseCrsCode)) {
                        actualCRS = responseCRS;
                    }
                }
                if (actualCRS == null)
                    throw new WcsException("CRS " + gridBaseCrs
                            + " is not among the supported ones for coverage " + meta.getName(),
                            WcsExceptionCode.InvalidParameterValue, "GridBaseCrs");
                gridCRS.setGridBaseCRS(gridBaseCrs);
            } else {
                String code = GML2EncodingUtils.epsgCode(meta.getCRS());
                gridCRS.setGridBaseCRS("urn:x-ogc:def:crs:EPSG:" + code);
            }

            // check grid type makes sense and apply default otherwise
            String gridTypeValue = gridCRS.getGridType();
            GridType type = GridType.GT2dGridIn2dCrs;
            if (gridTypeValue != null) {
                type = null;
                for (GridType gt : GridType.values()) {
                    if (gt.getXmlConstant().equalsIgnoreCase(gridTypeValue))
                        type = gt;
                }
                if (type == null)
                    throw new WcsException("Unknown grid type " + gridTypeValue,
                            InvalidParameterValue, "GridType");
                else if (type == GridType.GT2dGridIn3dCrs)
                    throw new WcsException("Unsupported grid type " + gridTypeValue,
                            InvalidParameterValue, "GridType");
            }
            gridCRS.setGridType(type.getXmlConstant());

            // check gridcs and apply only value we know about
            String gridCS = gridCRS.getGridCS();
            if (gridCS != null) {
                if (!gridCS.equalsIgnoreCase(GridCS.GCSGrid2dSquare.getXmlConstant()))
                    throw new WcsException("Unsupported grid cs " + gridCS, InvalidParameterValue,
                            "GridCS");
            }
            gridCRS.setGridCS(GridCS.GCSGrid2dSquare.getXmlConstant());

            // check the grid origin and set defaults
            CoordinateReferenceSystem crs = null;
            try {
                crs = CRS.decode(gridCRS.getGridBaseCRS());
            } catch (Exception e) {
                throw new WcsException("Could not understand crs " + gridCRS.getGridBaseCRS(),
                        WcsExceptionCode.InvalidParameterValue, "GridBaseCRS");
            }
            if (!gridCRS.isSetGridOrigin() || gridCRS.getGridOrigin() == null) {
                // if not set, we have a default of "0 0" as a string, since I
                // cannot
                // find a way to make it default to new Double[] {0 0} I'll fix
                // it here
                Double[] origin = new Double[type.getOriginArrayLength()];
                Arrays.fill(origin, 0.0);
                gridCRS.setGridOrigin(origin);
            } else {
                Double[] gridOrigin = (Double[]) gridCRS.getGridOrigin();
                // make sure the origin dimension matches the output crs
                // dimension
                if (gridOrigin.length != type.getOriginArrayLength())
                    throw new WcsException("Grid origin size (" + gridOrigin.length
                            + ") inconsistent with grid type " + type.getXmlConstant()
                            + " that requires (" + type.getOriginArrayLength() + ")",
                            WcsExceptionCode.InvalidParameterValue, "GridOrigin");
                gridCRS.setGridOrigin(gridOrigin);
            }

            // perform same checks on the offsets
            Double[] gridOffsets = (Double[]) gridCRS.getGridOffsets();
            if (gridOffsets != null) {
                // make sure the origin dimension matches the grid type
                if (type.getOffsetArrayLength() != gridOffsets.length)
                    throw new WcsException("Invalid offsets lenght, grid type "
                            + type.getXmlConstant() + " requires " + type.getOffsetArrayLength(),
                            InvalidParameterValue, "GridOffsets");
            } else {
                gridCRS.setGridOffsets(null);
            }
        }
    }

    /**
     * Extracts only the final part of an EPSG code allowing for a specification independent comparison (that is, it removes the EPSG:, urn:xxx:,
     * http://... prefixes)
     * 
     * @param srsName
     * @return
     */
    private String extractCode(String srsName) {
        if (srsName.startsWith("http://www.opengis.net/gml/srs/epsg.xml#"))
            return srsName.substring(40);
        else if (srsName.startsWith("urn:"))
            return srsName.substring(srsName.lastIndexOf(':') + 1);
        else if (srsName.startsWith("EPSG:"))
            return srsName.substring(5);
        else
            return srsName;
    }

    /**
     * Checks if the supported format string list contains the specified format, doing a case insensitive search. If found the declared output format
     * name is returned, otherwise null is returned.
     * 
     * @param supportedFormats
     * @param format
     * @return
     */
    private String getDeclaredFormat(List supportedFormats, String format) {
        // supported formats may be setup using old style formats, first scan
        // the
        // configured list
        for (Iterator it = supportedFormats.iterator(); it.hasNext();) {
            String sf = (String) it.next();
            if (sf.equalsIgnoreCase(format)) {
                return sf;
            } else {
                CoverageResponseDelegate delegate = responseFactory.encoderFor(sf);
                if (delegate != null && delegate.canProduce(format))
                    return sf;
            }
        }
        return null;
    }

    /**
     * Checks that the elements of the RangeSubset part of the request do make sense by comparing them to the coverage metadata
     * 
     * @param info
     * @param rangeSubset
     */
    private void checkRangeSubset(CoverageInfo info, RangeSubsetType rangeSubset) {
        // quick escape if no range subset has been specified (it's legal)
        if (rangeSubset == null)
            return;

        if (rangeSubset.getFieldSubset().size() > 1) {
            throw new WcsException("Multi field coverages are not supported yet",
                    InvalidParameterValue, "RangeSubset");
        }

        // check field identifier
        FieldSubsetType field = (FieldSubsetType) rangeSubset.getFieldSubset().get(0);
        final String fieldId = field.getIdentifier().getValue();
        if (!fieldId.equalsIgnoreCase("contents"))
            throw new WcsException("Unknown field " + fieldId, InvalidParameterValue, "RangeSubset");

        // check interpolation
        String interpolation = field.getInterpolationType();
        if (interpolation != null) {
            boolean interpolationSupported = false;

            if (interpolation.equalsIgnoreCase("nearest")) {
                interpolation = "nearest";
            } else if (interpolation.equalsIgnoreCase("cubic") || interpolation.equalsIgnoreCase("bicubic")) {
                interpolation = "bicubic";
            } else if (interpolation.equalsIgnoreCase("linear") || interpolation.equalsIgnoreCase("bilinear")) {
                interpolation = "bilinear";
            }

            for (String method : info.getInterpolationMethods()) {
                if (method.toLowerCase().startsWith(interpolation)) {
                    interpolationSupported = true;
                    break;
                }
            }

            if (!interpolationSupported)
                throw new WcsException(
                        "The requested Interpolation method is not supported by this Coverage.",
                        InvalidParameterValue, "RangeSubset");
        }

        // check axis
        if (field.getAxisSubset().size() > 1) {
            throw new WcsException("Multi axis coverages are not supported yet",
                    InvalidParameterValue, "RangeSubset");
        } else if (field.getAxisSubset().size() == 0)
            return;

        AxisSubsetType axisSubset = (AxisSubsetType) field.getAxisSubset().get(0);
        final String axisId = axisSubset.getIdentifier();
        if (!axisId.equalsIgnoreCase("Bands"))
            throw new WcsException("Unknown axis " + axisId + " in field " + fieldId,
                    InvalidParameterValue, "RangeSubset");

        // prepare a support structure to quickly get the band index of a key
        // (and remember we replaced spaces with underscores in the keys to
        // avoid issues
        // with the kvp parsing of indentifiers that include spaces)
        List<CoverageDimensionInfo> dimensions = info.getDimensions();
        Set<String> dimensionMap = new HashSet<String>();
        for (int i = 0; i < dimensions.size(); i++) {
            String keyName = dimensions.get(i).getName().replace(' ', '_');
            dimensionMap.add(keyName);
        }

        // check keys
        List keys = axisSubset.getKey();
        int[] bands = new int[keys.size()];
        for (int j = 0; j < bands.length; j++) {
            final String key = (String) keys.get(j);
            String parsedKey = null;
            for (String dimensionName : dimensionMap) {
                if (dimensionName.equalsIgnoreCase(key)) {
                    parsedKey = dimensionName;
                    break;
                }
            }
            if (parsedKey == null)
                throw new WcsException("Unknown field/axis/key combination " + fieldId + "/"
                        + axisSubset.getIdentifier() + "/" + key, InvalidParameterValue,
                        "RangeSubset");
            else
                keys.set(j, parsedKey);
        }
    }

    /**
     * 
     * @param date
     * @return
     */
    private static Date cvtToGmt(Date date) {
        TimeZone tz = TimeZone.getDefault();
        Date ret = new Date(date.getTime() - tz.getRawOffset());

        // if we are now in DST, back off by the delta. Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() - tz.getDSTSavings());

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if (tz.inDaylightTime(dstDate)) {
                ret = dstDate;
            }
        }

        return ret;
    }

}
