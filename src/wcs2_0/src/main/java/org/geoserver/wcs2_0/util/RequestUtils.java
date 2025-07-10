/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.util;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.Interpolation;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.util.WCSUtils;

/**
 * Utility class performing operations related to http requests.
 *
 * <p>TODO: these methods should be put back into org.geoserver.ows.util.RequestUtils
 */
public class RequestUtils {

    /** {@link Logger} for this class. */
    private static final Logger LOGGER = Logging.getLogger(RequestUtils.class);

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will return the negotiated
     * version to be used for response according to the OWS 2.0 specification.
     *
     * <p>The difference from the 11 version is that here versions can have format "x.y".
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" or "x.y" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" or "x.y" format)
     * @return the negotiated version to be used for response
     * @see org.geoserver.ows.util.RequestUtils#getVersionOWS(List, List, boolean)
     */
    public static String getVersionOws20(List<String> providedList, List<String> acceptedList) {

        // first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest supported version
        if (acceptedList == null || acceptedList.isEmpty())
            return provided.last().toString();

        // next figure out what the client accepts (and check they are good version numbers)
        List<Version> accepted = new ArrayList<>();
        for (String v : acceptedList) {
            checkVersionNumber20(v, "AcceptVersions");

            accepted.add(new Version(v));
        }

        // from the specification "The server, upon receiving a GetCapabilities request, shall scan
        // through this list and find the first version number that it supports"
        Version negotiated = null;
        for (Version version : accepted) {
            if (provided.contains(version)) {
                negotiated = version;
                break;
            }
        }

        // from the spec: "If the list does not contain any version numbers that the server
        // supports, the server shall return an Exception with
        // exceptionCode="VersionNegotiationFailed"
        if (negotiated == null)
            throw new OWS20Exception(
                    "Could not find any matching version", OWS20Exception.OWSExceptionCode.VersionNegotiationFailed);

        return negotiated.toString();
    }

    /**
     * Checks the validity of a version number (the specification version numbers, two or three dot separated integers
     * between 0 and 99). Throws a ServiceException if the version number is not valid.
     *
     * @param v the version number (in string format)
     * @param locator the locator for the service exception (may be null)
     */
    public static void checkVersionNumber20(String v, String locator) throws ServiceException {
        if (!v.matches("[0-9]{1,2}\\.[0-9]{1,2}(\\.[0-9]{1,2})?")) {
            String msg = v + " is an invalid version number";
            throw new OWS20Exception(
                    "Could not find any matching version," + msg,
                    OWS20Exception.OWSExceptionCode.VersionNegotiationFailed,
                    locator);
        }
    }

    /** Reads the best matching grid out of a grid coverage applying sub-sampling and using overviews as necessary */
    public static GridCoverage2D readBestCoverage(
            CoverageInfo cinfo,
            final GridCoverage2DReader reader,
            final Object params,
            final GridGeometry2D readGG,
            final Interpolation interpolation,
            final OverviewPolicy overviewPolicy,
            Hints hints)
            throws IOException {

        ////
        //
        // Intersect the present envelope with the request envelope, also in WGS 84 to make sure
        // there is an actual intersection
        //
        ////
        try {
            final ReferencedEnvelope coverageEnvelope = WCSUtils.fitEnvelope(cinfo, reader);
            if (!coverageEnvelope.intersects((BoundingBox) ReferencedEnvelope.reference(readGG.getEnvelope()))) {
                return null;
            }

        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to compare data and request envelopes, proceeding with rendering anyways",
                    e);
        }

        // //
        // It is an GridCoverage2DReader, let's use parameters
        // if we have any supplied by a user.
        // //
        // first I created the correct ReadGeometry
        final Parameter<GridGeometry2D> readGGParam =
                (Parameter<GridGeometry2D>) AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
        readGGParam.setValue(readGG);

        final Parameter<Interpolation> readInterpolation =
                (Parameter<Interpolation>) ImageMosaicFormat.INTERPOLATION.createValue();
        readInterpolation.setValue(interpolation);

        final Parameter<OverviewPolicy> readOverview =
                (Parameter<OverviewPolicy>) AbstractGridFormat.OVERVIEW_POLICY.createValue();
        readOverview.setValue(overviewPolicy);
        // then I try to get read parameters associated with this
        // coverage if there are any.
        GridCoverage2D coverage = null;
        GeneralParameterValue[] readParams = (GeneralParameterValue[]) params;
        final int length = readParams == null ? 0 : readParams.length;
        if (length > 0) {
            // //
            //
            // Getting parameters to control how to read this coverage.
            // Remember to check to actually have them before forwarding
            // them to the reader.
            //
            // //

            // we have a valid number of parameters, let's check if
            // also have a READ_GRIDGEOMETRY2D. In such case we just
            // override it with the one we just build for this
            // request.
            final String readGGName =
                    AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();
            final String readInterpolationName =
                    ImageMosaicFormat.INTERPOLATION.getName().toString();
            final String overviewPolicyName =
                    AbstractGridFormat.OVERVIEW_POLICY.getName().toString();
            int i = 0;
            boolean foundInterpolation = false;
            boolean foundGG = false;
            boolean foundOverviewPolicy = false;
            for (; i < length; i++) {
                final String paramName = readParams[i].getDescriptor().getName().toString();
                if (paramName.equalsIgnoreCase(readGGName)) {
                    ((Parameter) readParams[i]).setValue(readGG);
                    foundGG = true;
                } else if (paramName.equalsIgnoreCase(readInterpolationName)) {
                    ((Parameter) readParams[i]).setValue(interpolation);
                    foundInterpolation = true;
                } else if (paramName.equalsIgnoreCase(overviewPolicyName)) {
                    // Override the default overview value using the one found on read params
                    ((Parameter) readParams[i]).setValue(overviewPolicy);
                    foundOverviewPolicy = true;
                }
            }

            // did we find anything?
            if (!foundGG || !foundInterpolation || !foundOverviewPolicy) { // || !(foundBgColor && bgColor != null)) {
                // add the correct read geometry to the supplied
                // params since we did not find anything
                List<GeneralParameterValue> paramList = new ArrayList<>();
                paramList.addAll(Arrays.asList(readParams));
                if (!foundGG) {
                    paramList.add(readGGParam);
                }
                if (!foundInterpolation) {
                    paramList.add(readInterpolation);
                }
                if (!foundOverviewPolicy && readOverview != null && readOverview.getValue() != null) {
                    paramList.add(readOverview);
                }

                readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
            }
            coverage = reader.read(readParams);
        } else {
            coverage = reader.read(readGGParam, readInterpolation);
        }

        return coverage;
    }

    /**
     * Returns the "Sample to geophysics" transform as an affine transform, or {@code null} if none. Note that the
     * returned instance may be an immutable one, not necessarly the default Java2D implementation.
     *
     * @param gridToCRS The "grid to CRS" {@link MathTransform} transform.
     * @return The "grid to CRS" affine transform of the given coverage, or {@code null} if none or if the transform is
     *     not affine.
     */
    public static AffineTransform getAffineTransform(final MathTransform gridToCRS) {
        if (gridToCRS == null) {
            return null;
        }

        if (gridToCRS instanceof AffineTransform) {
            return (AffineTransform) gridToCRS;
        }
        return null;
    }
    /**
     * This utility method can be used to read a small sample {@link GridCoverage2D} for inspection from the specified
     * {@link GridCoverage2DReader}.
     *
     * @param reader the {@link GridCoverage2DReader} that we'll read the coverage from
     */
    public static GridCoverage2D readSampleGridCoverage(GridCoverage2DReader reader) throws Exception {
        final Format coverageFormat = reader.getFormat();

        final ParameterValueGroup readParams = coverageFormat.getReadParameters();
        final Map<String, Serializable> parameters = CoverageUtils.getParametersKVP(readParams);
        return CoverageUtils.readSampleGridCoverage(reader, readParams, parameters, null, false);
    }

    /**
     * This utility method can be used to read a small sample {@link GridCoverage2D} for inspection from the specified
     * {@link CoverageInfo}.
     *
     * @param ci the {@link CoverageInfo} that contains the description of the GeoServer coverage to read from.
     */
    public static GridCoverage2D readSampleGridCoverage(CoverageInfo ci) throws Exception {
        final GridCoverage2DReader reader = getCoverageReader(ci);
        return readSampleGridCoverage(reader);
    }

    /** Grabs the reader from the specified coverage */
    public static GridCoverage2DReader getCoverageReader(CoverageInfo ci) throws IOException, Exception {
        // get a reader for this coverage
        final CoverageStoreInfo store = ci.getStore();
        final GridCoverageReader reader_ =
                ci.getGridCoverageReader(new DefaultProgressListener(), GeoTools.getDefaultHints());
        if (reader_ == null) {
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + store.getFormat().getName());
        }
        final GridCoverage2DReader reader = (GridCoverage2DReader) reader_;
        return reader;
    }

    /** Makes sure the version is present and supported */
    public static void checkVersion(String version) {
        if (version == null) {
            throw new WCS20Exception(
                    "Missing version", OWS20Exception.OWSExceptionCode.MissingParameterValue, "version");
        }

        if (!WCS20Const.V201.equals(version) && !WCS20Const.V20.equals(version)) {
            throw new WCS20Exception("Could not understand version:" + version);
        }
    }

    /** Makes sure the service is present and supported */
    public static void checkService(String serviceName) {
        if (serviceName == null) {
            throw new WCS20Exception(
                    "Missing service name", OWS20Exception.OWSExceptionCode.MissingParameterValue, "service");
        }
        if (!"WCS".equals(serviceName)) {
            throw new WCS20Exception(
                    "Error in service name, expected value: WCS",
                    OWS20Exception.OWSExceptionCode.InvalidParameterValue,
                    serviceName);
        }
    }
}
