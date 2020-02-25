/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.gml.CodeType;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.Gml4wcsFactory;
import net.opengis.gml.PointType;
import net.opengis.gml.RectifiedGridType;
import net.opengis.gml.TimePositionType;
import net.opengis.gml.VectorType;
import net.opengis.wcs10.AxisSubsetType;
import net.opengis.wcs10.DomainSubsetType;
import net.opengis.wcs10.GetCoverageType;
import net.opengis.wcs10.InterpolationMethodType;
import net.opengis.wcs10.IntervalType;
import net.opengis.wcs10.OutputType;
import net.opengis.wcs10.RangeSubsetType;
import net.opengis.wcs10.SpatialSubsetType;
import net.opengis.wcs10.TimePeriodType;
import net.opengis.wcs10.TimeSequenceType;
import net.opengis.wcs10.TypedLiteralType;
import net.opengis.wcs10.Wcs10Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.KvpUtils.Tokenizer;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.metadata.i18n.Errors;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * GetCoverage request reader for WCS 1.0.0
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class Wcs10GetCoverageRequestReader extends EMFKvpRequestReader {

    public static final String VERSION = "1.0.0";
    Catalog catalog;

    public Wcs10GetCoverageRequestReader(Catalog catalog) {
        super(GetCoverageType.class, Wcs10Factory.eINSTANCE);
        this.catalog = catalog;
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) super.read(request, kvp, rawKvp);

        // grab coverage info to perform further checks
        if (getCoverage.getSourceCoverage() == null) {
            if (kvp.get("coverage") == null)
                throw new WcsException(
                        "source coverage parameter is mandatory",
                        MissingParameterValue,
                        "source coverage");
            else getCoverage.setSourceCoverage((String) ((List) kvp.get("coverage")).get(0));
        }
        // if not specified, throw a resounding exception (by spec)
        if (!getCoverage.isSetVersion())
            throw new WcsException(
                    "Version has not been specified",
                    WcsExceptionCode.MissingParameterValue,
                    "version");

        // do the version negotiation dance
        List<String> provided = new ArrayList<String>();
        provided.add(Wcs10GetCoverageRequestReader.VERSION);
        List<String> accepted = null;
        if (getCoverage.getVersion() != null) {
            accepted = new ArrayList<String>();
            accepted.add(getCoverage.getVersion());
        }
        String version = RequestUtils.getVersionPreOws(provided, accepted);

        if (!Wcs10GetCoverageRequestReader.VERSION.equals(version)) {
            throw new WcsException(
                    "An invalid version number has been specified",
                    WcsExceptionCode.InvalidParameterValue,
                    "version");
        }
        getCoverage.setVersion(Wcs10GetCoverageRequestReader.VERSION);

        // build interpolation
        if (!getCoverage.isSetInterpolationMethod()) {
            getCoverage.setInterpolationMethod(parseInterpolation(kvp));
        }

        // build the domain subset
        getCoverage.setDomainSubset(parseDomainSubset(kvp));

        // build the range subset
        getCoverage.setRangeSubset(parseRangeSubset(kvp, getCoverage.getSourceCoverage()));

        // build output element
        getCoverage.setOutput(parseOutputElement(kvp));

        return getCoverage;
    }

    /** @param kvp */
    private DomainSubsetType parseDomainSubset(Map kvp) {
        final DomainSubsetType domainSubset = Wcs10Factory.eINSTANCE.createDomainSubsetType();
        final SpatialSubsetType spatialSubset = Wcs10Factory.eINSTANCE.createSpatialSubsetType();

        //
        // check for CRS
        //
        String crsName = (String) kvp.get("crs");
        if (crsName == null)
            throw new WcsException("CRS parameter is mandatory", MissingParameterValue, "crs");
        final CoordinateReferenceSystem crs = decodeCRS100(crsName);
        if (crs == null)
            throw new WcsException(
                    "CRS parameter is invalid:" + crsName, InvalidParameterValue, "crs");
        //        final VerticalCRS verticalCRS = CRS.getVerticalCRS(crs);
        //        final boolean hasVerticalCRS = verticalCRS != null;

        //
        // at least one between BBOX and TIME must be there
        //
        final GeneralEnvelope bbox = (GeneralEnvelope) kvp.get("BBOX");
        if (bbox == null)
            throw new WcsException("bbox parameter is mandatory", MissingParameterValue, "bbox");

        // afabiani: consider Elevation as band, forcing the bbox to be 2D only
        if (bbox.getDimension() != 2)
            throw new WcsException(
                    "Requested bounding box is not 2-dimensional: " + bbox.getDimension(),
                    InvalidParameterValue,
                    "bbox");

        final GeneralEnvelope envelope =
                new GeneralEnvelope(
                        /* TODO: ignore 3D CRS for now crs */ bbox.getDimension() == 3
                                ? DefaultGeographicCRS.WGS84_3D
                                : crs);
        if (
        /* TODO: ignore 3D CRS for now !hasVerticalCRS */ bbox.getDimension() == 2)
            envelope.setEnvelope(
                    bbox.getLowerCorner().getOrdinate(0),
                    bbox.getLowerCorner().getOrdinate(1),
                    bbox.getUpperCorner().getOrdinate(0),
                    bbox.getUpperCorner().getOrdinate(1));
        //        else if (/* TODO: ignore 3D CRS for now hasVerticalCRS */ bbox.getDimension() ==
        // 3)
        //            // 3D
        //            envelope.setEnvelope(bbox.getLowerCorner().getOrdinate(0),
        // bbox.getLowerCorner()
        //                    .getOrdinate(1), bbox.getLowerCorner().getOrdinate(2),
        // bbox.getUpperCorner()
        //                    .getOrdinate(0), bbox.getUpperCorner().getOrdinate(1),
        // bbox.getUpperCorner()
        //                    .getOrdinate(2));
        else
            throw new WcsException(
                    "bbox not compliant with the specified CRS", InvalidParameterValue, "bbox");

        //
        // TIME
        //
        TimeSequenceType timeSequence = null;
        Object time = kvp.get("TIME");
        if (time != null && time instanceof TimeSequenceType) {
            timeSequence = (TimeSequenceType) time;
        } else if (time != null) {
            timeSequence = Wcs10Factory.eINSTANCE.createTimeSequenceType();
            if (time instanceof Collection) {
                for (Object tPos : (Collection<Object>) time) {
                    addToTimeSequence(timeSequence, tPos);
                }
            }
        }
        if (timeSequence == null && bbox == null)
            throw new WcsException(
                    "Bounding box cannot be null, TIME has not been specified",
                    WcsExceptionCode.MissingParameterValue,
                    "BBOX");

        //
        // GRID management
        //
        final RectifiedGridType grid = Gml4wcsFactory.eINSTANCE.createRectifiedGridType();
        final Object w = kvp.get("width");
        final Object h = kvp.get("height");
        if (w != null && h != null) {
            //
            // normal grid management, only the envelope and the raster dimensions have been
            // specified,
            // we need to compute RESX, RESY, RESZ afterwards
            //

            // get W and H
            int width = w instanceof Integer ? ((Integer) w) : Integer.parseInt((String) w);
            int height = w instanceof Integer ? ((Integer) h) : Integer.parseInt((String) h);
            grid.getAxisName().add("x");
            grid.getAxisName().add("y");

            final Object d = kvp.get("depth");
            if (d != null) {
                // afabiani: we consider 2D grdis only
                throw new WcsException(
                        "3D grids are not supported.", InvalidParameterValue, "depth");
                //                // check that the envelope is 3D or throw an error
                //                if (bbox.getDimension() != 3)
                //                    throw new WcsException("Found depth but envelope is of
                // dimension "
                //                            + bbox.getDimension(), InvalidParameterValue, "");
                //
                //                // notice that as for the spec this element represents the number
                // of ticks on the
                //                // third dimension
                //                grid.getAxisName().add("z");
                //
                //                final int depth = Integer.parseInt((String) d);
                //                grid.setDimension(BigInteger.valueOf(3));
                //                // notice that the third element indicates how many layers we do
                // have requested on the third dimension
                //                grid.setLimits(new GeneralGridEnvelope(new int[] { 0, 0, 0 }, new
                // int[] {width, height, depth }, false));
                //
                //
                //                // 3D grid
                //                grid.setDimension(BigInteger.valueOf(3));
            } else {
                // 2d grid
                grid.setDimension(BigInteger.valueOf(2));
                grid.setLimits(new GridEnvelope2D(0, 0, width, height));
            }
        } else {
            //
            // we might be working with a rectified grid request there we need
            // to try and use that type. we cannot build a raster grid at this
            // stage yet since we have no idea about how the envelope will be intersected with the
            // native envelope for this raster
            //
            final Object rx = kvp.get("resx");
            final Object ry = kvp.get("resy");
            if (rx != null && ry != null) {
                // get resx e resy but correct also the sign for them taking into account
                final CoordinateSystem cs = crs.getCoordinateSystem();
                final AxisDirection northingDirection = cs.getAxis(1).getDirection();
                final int yAxisCorrection = AxisDirection.NORTH.equals(northingDirection) ? -1 : 1;
                final AxisDirection eastingDirection = cs.getAxis(0).getDirection();
                final int xAxisCorrection = AxisDirection.EAST.equals(eastingDirection) ? 1 : -1;
                final double resX = Double.parseDouble((String) rx) * xAxisCorrection;
                final double resY = Double.parseDouble((String) ry) * yAxisCorrection;

                // basic check, the resolution cannot be larger than any of the two spans
                // for the envelope because otherwise the size in raster space will be invalid
                // We expect the final raster area to be at least 2 pixel on each raster dimension
                if (Math.abs(envelope.getSpan(0) / Math.abs(resX)) < 2
                        || Math.abs(envelope.getSpan(1) / Math.abs(resY)) < 2)
                    throw new IllegalArgumentException(
                            Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$1, "resolutions"));

                // now compute offset vector for the transform from the envelope
                // Following ISO 19123 we use the CELL_CENTER convention but with the raster
                final double origX = envelope.getLowerCorner().getOrdinate(0) + resX / 2;
                final double origY = envelope.getUpperCorner().getOrdinate(1) + resY / 2;

                // create offset point
                final PointType origin = Gml4wcsFactory.eINSTANCE.createPointType();
                final DirectPositionType dp = Gml4wcsFactory.eINSTANCE.createDirectPositionType();
                origin.setPos(dp);
                origin.setSrsName(crsName);

                // create resolutions vector
                final VectorType resolutionVector = Gml4wcsFactory.eINSTANCE.createVectorType();

                //
                // Third dimension management
                //
                final Object rz = kvp.get("resz");
                if (rz != null) {
                    // afabiani: we consider 2D grdis only
                    throw new WcsException(
                            "3D grids are not supported.", InvalidParameterValue, "resz");
                    //                    // eventual depth
                    //                    final double resZ = Double.parseDouble((String) rz);
                    //                    // check that the envelope is 3D or throw an error
                    //                    if (bbox.getDimension() != 3)
                    //                        throw new WcsException("Found ResZ but envelope is of
                    // dimension "
                    //                                + bbox.getDimension(), InvalidParameterValue,
                    // "");
                    //                    final double origZ = bbox.getLowerCorner().getOrdinate(2);
                    //
                    //                    // 3D grid
                    //                    grid.setDimension(BigInteger.valueOf(3));
                    //                    // set the origin position
                    //                    dp.setDimension(grid.getDimension());
                    //                    dp.setValue(Arrays.asList(origX, origY, origZ));
                    //                    grid.setOrigin(origin);
                    //
                    //                    // set the resolution vector
                    //                    resolutionVector.setDimension(grid.getDimension());
                    //                    resolutionVector.setValue(Arrays.asList(resX, resY,
                    // resZ));
                    //                    grid.getOffsetVector().add(resolutionVector);
                } else {
                    // 2d grid
                    grid.setDimension(BigInteger.valueOf(2));
                    // set the origin position
                    dp.setDimension(grid.getDimension());
                    dp.setValue(Arrays.asList(origX, origY));
                    grid.setOrigin(origin);

                    // set the resolution vector
                    resolutionVector.setDimension(grid.getDimension());
                    resolutionVector.setValue(Arrays.asList(resX, resY));
                    grid.getOffsetVector().add(resolutionVector);
                }

            } else
                throw new WcsException(
                        "Could not recognize grid resolution", InvalidParameterValue, "");
        }

        spatialSubset.getEnvelope().add(envelope);
        spatialSubset.getGrid().add(grid);
        domainSubset.setSpatialSubset(spatialSubset);
        domainSubset.setTemporalSubset(timeSequence);

        return domainSubset;
    }

    /** */
    private void addToTimeSequence(TimeSequenceType timeSequence, Object tPos) {
        if (tPos instanceof Date) {
            final TimePositionType timePosition = Gml4wcsFactory.eINSTANCE.createTimePositionType();
            timePosition.setValue(tPos);
            timeSequence.getTimePosition().add(timePosition);
        } else if (tPos instanceof DateRange) {
            DateRange range = (DateRange) tPos;
            final TimePeriodType timePeriod = Wcs10Factory.eINSTANCE.createTimePeriodType();
            final TimePositionType start = Gml4wcsFactory.eINSTANCE.createTimePositionType();
            start.setValue(range.getMinValue());
            timePeriod.setBeginPosition(start);
            final TimePositionType end = Gml4wcsFactory.eINSTANCE.createTimePositionType();
            end.setValue(range.getMaxValue());
            timePeriod.setEndPosition(end);
            timeSequence.getTimePeriod().add(timePeriod);
        }
    }

    private RangeSubsetType parseRangeSubset(Map kvp, String coverageName) {
        final RangeSubsetType rangeSubset = Wcs10Factory.eINSTANCE.createRangeSubsetType();

        if (kvp.get("Band") != null) {
            Object axis = kvp.get("Band");
            if (axis instanceof AxisSubsetType) {
                rangeSubset.getAxisSubset().add(axis);
            } else checkTypeAxisRange(rangeSubset, axis, "Band");
        }

        if (kvp.get("ELEVATION") != null) {
            Object axis = kvp.get("ELEVATION");
            if (axis instanceof AxisSubsetType) {
                rangeSubset.getAxisSubset().add(axis);
            } else checkTypeAxisRange(rangeSubset, axis, "ELEVATION");
        }

        // check for custom dimensions
        CoverageInfo coverage = this.catalog.getCoverageByName(coverageName);
        if (coverage != null) {
            for (Entry<String, ?> entry : coverage.getMetadata().entrySet()) {
                String name = entry.getKey();
                if (name.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)
                        && entry.getValue() instanceof DimensionInfo
                        && ((DimensionInfo) entry.getValue()).isEnabled()) {
                    name = name.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                    Object value = kvp.get(name);
                    // ignore anything that got converted by a KVP parser so that they
                    // are only handled as keywords and not as a custom dimension since
                    // WCS 1.0.0 does not define away to avoid these name clashes.
                    if (value instanceof String) {
                        name = name.toUpperCase();
                        checkTypeAxisRange(rangeSubset, Arrays.asList(value), name);
                    }
                }
            }
        }

        return rangeSubset;
    }

    /** */
    @SuppressWarnings("unchecked")
    private void checkTypeAxisRange(
            final RangeSubsetType rangeSubset, Object axis, String axisName) {
        if (axis instanceof String) {
            String bands = (String) axis;
            if (bands != null) {
                if (bands.contains("/")) {
                    List<String> unparsed = KvpUtils.readFlat(bands, new Tokenizer("/"));

                    IntervalType interval = Wcs10Factory.eINSTANCE.createIntervalType();
                    TypedLiteralType min = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    TypedLiteralType max = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    TypedLiteralType res = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    if (unparsed.size() == 2) {
                        min.setValue(unparsed.get(0));
                        max.setValue(unparsed.get(1));

                        interval.setMin(min);
                        interval.setMax(max);
                    } else {
                        min.setValue(unparsed.get(0));
                        max.setValue(unparsed.get(1));
                        res.setValue(unparsed.get(2));

                        interval.setMin(min);
                        interval.setMax(max);
                        interval.setRes(res);
                    }

                    final AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();

                    axisSubset.setName(axisName);

                    axisSubset.getInterval().add(interval);

                    rangeSubset.getAxisSubset().add(axisSubset);

                } else {
                    List<String> unparsed = KvpUtils.readFlat(bands, KvpUtils.INNER_DELIMETER);

                    if (unparsed.size() == 0) {
                        throw new WcsException(
                                "Requested axis subset contains wrong number of values (should have at least 1): "
                                        + unparsed.size(),
                                WcsExceptionCode.InvalidParameterValue,
                                "band");
                    }

                    final AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();

                    axisSubset.setName(axisName);

                    for (String bandValue : unparsed) {
                        TypedLiteralType singleValue =
                                Wcs10Factory.eINSTANCE.createTypedLiteralType();
                        singleValue.setValue(bandValue);

                        axisSubset.getSingleValue().add(singleValue);
                    }
                    rangeSubset.getAxisSubset().add(axisSubset);
                }
            }
        } else if (axis instanceof Double || axis instanceof Integer) {
            final AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();

            axisSubset.setName(axisName);

            TypedLiteralType singleValue = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            singleValue.setValue(String.valueOf(axis));

            axisSubset.getSingleValue().add(singleValue);

            rangeSubset.getAxisSubset().add(axisSubset);
        } else if (axis instanceof Collection) {
            AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();
            axisSubset.setName(axisName);
            for (Object value : (Collection<?>) axis) {
                if (value instanceof NumberRange) {
                    NumberRange<?> range = (NumberRange<?>) value;
                    IntervalType interval = Wcs10Factory.eINSTANCE.createIntervalType();
                    TypedLiteralType min = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    TypedLiteralType max = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    min.setValue(Double.toString(range.getMinimum()));
                    max.setValue(Double.toString(range.getMaximum()));
                    interval.setMin(min);
                    interval.setMax(max);
                    axisSubset.getInterval().add(interval);
                } else {
                    TypedLiteralType singleValue = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                    singleValue.setValue(String.valueOf(value));
                    axisSubset.getSingleValue().add(singleValue);
                }
            }
            rangeSubset.getAxisSubset().add(axisSubset);
        }
    }

    private OutputType parseOutputElement(final Map<String, String> kvp) throws Exception {
        final OutputType output = Wcs10Factory.eINSTANCE.createOutputType();
        final CodeType crsType = Gml4wcsFactory.eINSTANCE.createCodeType();
        final CodeType formatType = Gml4wcsFactory.eINSTANCE.createCodeType();

        // check and set format
        String format = (String) kvp.get("format");
        if (format == null)
            throw new WcsException(
                    "format parameter is mandatory", MissingParameterValue, "format");

        final String crsName =
                (String)
                        (kvp.get("response_crs") != null
                                ? kvp.get("response_crs")
                                : kvp.get("crs"));
        CoordinateReferenceSystem crs = null;
        if (crsName != null) {
            crs = decodeCRS100(crsName);

            crsType.setValue(CRS.lookupIdentifier(crs, true));

            output.setCrs(crsType);
        }

        formatType.setValue(format);

        output.setFormat(formatType);

        return output;
    }

    /** DEcode the requested CRS following the WCS 1.0 style with LON,LAT axes order. */
    private static CoordinateReferenceSystem decodeCRS100(String crsName) throws WcsException {
        if ("WGS84(DD)".equals(crsName)) {
            crsName = "EPSG:4326";
        }

        try {
            // in 100 we work with Lon,Lat always
            return CRS.decode(crsName, true);
        } catch (NoSuchAuthorityCodeException e) {
            throw new WcsException(
                    "Could not recognize crs " + crsName, InvalidParameterValue, "crs");
        } catch (FactoryException e) {
            throw new WcsException(
                    "Could not recognize crs " + crsName, InvalidParameterValue, "crs");
        }
    }

    /**
     * Parses the interpolation parameter from the kvp. If nothing is present the default nearest
     * neighbor is set.
     */
    private InterpolationMethodType parseInterpolation(Map kvp) {
        if (kvp.containsKey("interpolation")) {
            return (InterpolationMethodType) kvp.get("interpolation");
        }
        return InterpolationMethodType.NEAREST_NEIGHBOR_LITERAL;
    }
}
