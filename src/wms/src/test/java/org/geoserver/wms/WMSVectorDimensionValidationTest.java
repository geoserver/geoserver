package org.geoserver.wms;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.geoserver.data.test.MockData.SF_PREFIX;
import static org.geoserver.data.test.MockData.SF_URI;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/**
 * this suite performs tests on the WMS request validation feature added for <a
 * href="https://osgeo-org.atlassian.net/browse/GEOS-9757">GEOS-9757 - Return a service exception
 * when client provided WMS dimensions are not a match</a>.
 *
 * @author skalesse
 * @since 2024-07-11
 */
public class WMSVectorDimensionValidationTest extends WMSTestSupport {

    private static final QName TIME_WITH_START_END =
            new QName(SF_URI, "TimeWithStartEnd", SF_PREFIX);
    private static final QName ELEVATION_WITH_START_END =
            new QName(SF_URI, "ElevationWithStartEnd", SF_PREFIX);
    private static final QName CUSTOMS_DIM_WITH_START_END =
            new QName(SF_URI, "CustomDimensionWithStartEnd", SF_PREFIX);

    private WMS wms;

    @Before
    public void setWMS() {
        wms = getWMS();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        String testDataFileName = "TimeElevationWithStartEnd.properties";
        testData.addVectorLayer(
                TIME_WITH_START_END, emptyMap(), testDataFileName, getClass(), getCatalog());

        testData.addVectorLayer(
                ELEVATION_WITH_START_END, emptyMap(), testDataFileName, getClass(), getCatalog());

        testData.addVectorLayer(
                CUSTOMS_DIM_WITH_START_END, emptyMap(), testDataFileName, getClass(), getCatalog());
    }

    /**
     * for a 'time' dimension, if a matching value is provided in the request the validation should
     * succeed
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void matching_time_dimension_should_validate() throws IOException {
        // the dimension and its value
        String dimension = "time";
        Date dimensionValue = Date.from(Instant.parse("2012-02-11T10:15:30.00Z"));
        // create a map request
        GetMapRequest request = mapRequest(singletonList(dimensionValue), emptyList(), emptyMap());
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(), TIME_WITH_START_END, dimension, "startTime", "endTime"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * for a 'time' dimension, if a mismatching value is provided in the request the validation
     * should fail.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_time_dimension_should_not_validate() throws IOException {
        // the dimension and its value
        String dimension = "time";
        Date dimensionValue = Date.from(Instant.parse("2012-02-10T10:15:30.00Z"));
        // create a map request
        GetMapRequest request = mapRequest(singletonList(dimensionValue), emptyList(), emptyMap());
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(), TIME_WITH_START_END, dimension, "startTime", "endTime"),
                request,
                dimension,
                dimensionValue,
                false);
    }

    /**
     * for a 'time' dimension, if a mismatching value is provided in the request the validation
     * should succeed, in case validation is disabled in the settings.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_time_dimension_should_validate_if_validation_disabled()
            throws IOException {
        // the dimension and its value
        String dimension = "time";
        Date dimensionValue = Date.from(Instant.parse("2012-02-10T10:15:30.00Z"));
        // create a map request
        GetMapRequest request = mapRequest(singletonList(dimensionValue), emptyList(), emptyMap());
        // disable validation
        setValidationEnabled(false);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(), TIME_WITH_START_END, dimension, "startTime", "endTime"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * for an 'elevation' dimension, if a matching value is provided in the request the validation
     * should succeed
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void matching_elevation_dimension_should_validate() throws IOException {
        // the dimension and its value
        String dimension = "elevation";
        double dimensionValue = 2; // matching elevation
        // create a map request
        GetMapRequest request = mapRequest(emptyList(), singletonList(dimensionValue), emptyMap());
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        ELEVATION_WITH_START_END,
                        dimension,
                        "startElevation",
                        "endElevation"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * for an 'elevation' dimension, if a mismatching value is provided in the request the
     * validation should fail.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_elevation_dimension_should_not_validate() throws IOException {
        // the dimension and its value
        String dimension = "elevation";
        double dimensionValue = 5; // mismatching elevation
        // create a map request
        GetMapRequest request = mapRequest(emptyList(), singletonList(dimensionValue), emptyMap());
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        ELEVATION_WITH_START_END,
                        dimension,
                        "startElevation",
                        "endElevation"),
                request,
                dimension,
                dimensionValue,
                false);
    }

    /**
     * for an 'elevation' dimension, if a mismatching value is provided in the request the
     * validation should succeed, in case validation is disabled in the settings.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_elevation_dimension_should_validate_if_validation_is_disabled()
            throws IOException {
        // the dimension and its value
        String dimension = "elevation";
        double dimensionValue = 5; // mismatching elevation
        // create a map request
        GetMapRequest request = mapRequest(emptyList(), singletonList(dimensionValue), emptyMap());
        // disable validation
        setValidationEnabled(false);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        ELEVATION_WITH_START_END,
                        dimension,
                        "startElevation",
                        "endElevation"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * for a custom dimension, if a matching value is provided in the request the validation should
     * succeed
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void matching_custom_time_dimension_should_validate() throws IOException {
        // the dimension and its value
        String dimension = WMS.DIM_ + "reference_time";
        Instant dimensionValue = Instant.parse("2012-02-11T10:15:30.00Z");
        // create a map request
        Map<String, String> customDimensions = new HashMap<>();
        customDimensions.put(dimension.toUpperCase(), ISO_INSTANT.format(dimensionValue));
        GetMapRequest request = mapRequest(emptyList(), emptyList(), customDimensions);
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        CUSTOMS_DIM_WITH_START_END,
                        dimension,
                        "startTime",
                        "endTime"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * for a custom dimension, if a mismatching value is provided in the request the validation
     * should fail.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_custom_time_dimension_should_not_validate() throws IOException {
        // the dimension and its value
        String dimension = WMS.DIM_ + "reference_time";
        Instant dimensionValue = Instant.parse("2012-02-10T10:15:30.00Z");
        // create a map request
        Map<String, String> customDimensions = new HashMap<>();
        customDimensions.put(dimension.toUpperCase(), ISO_INSTANT.format(dimensionValue));
        GetMapRequest request = mapRequest(emptyList(), emptyList(), customDimensions);
        // disable validation
        setValidationEnabled(true);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        CUSTOMS_DIM_WITH_START_END,
                        dimension,
                        "startTime",
                        "endTime"),
                request,
                dimension,
                dimensionValue,
                false);
    }

    /**
     * for a custom dimension, if a mismatching value is provided in the request the validation
     * should succeed, in case validation is disabled in the settings.
     *
     * @throws IOException an unexpected IO exception is thrown
     */
    @Test
    public void mismatching_custom_time_dimension_should_validate_if_disabled() throws IOException {
        // the dimension and its value
        String dimension = WMS.DIM_ + "reference_time";
        Instant dimensionValue = Instant.parse("2012-02-10T10:15:30.00Z");
        // create a map request
        Map<String, String> customDimensions = new HashMap<>();
        customDimensions.put(dimension.toUpperCase(), ISO_INSTANT.format(dimensionValue));
        GetMapRequest request = mapRequest(emptyList(), emptyList(), customDimensions);
        // disable validation
        setValidationEnabled(false);
        // run validation and assert
        assertValidationSuccess(
                setupStartEndDimension(
                        getCatalog(),
                        CUSTOMS_DIM_WITH_START_END,
                        dimension,
                        "startTime",
                        "endTime"),
                request,
                dimension,
                dimensionValue,
                true);
    }

    /**
     * enable / disable validation if dimension values.
     *
     * <p>Note, that GeoServer's default is {@code true}. See {@link
     * WMSInfoImpl#EXCEPTION_ON_INVALID_DIMENSION_DEFAULT}
     *
     * @param enabled {@code true} if validation should be enabled, {@code false} otherwise.
     */
    private void setValidationEnabled(boolean enabled) {
        WMSInfo wmsServiceInfo = wms.getServiceInfo();
        wmsServiceInfo.setExceptionOnInvalidDimension(enabled);
        wms.getGeoServer().save(wmsServiceInfo);
    }

    /**
     * asserts the validation success given the input request
     *
     * @param featureTypeInfo the feature type matching the request
     * @param request the {@link GetMapRequest} that is to be validated
     * @param dimension the name of the dimension to be validated (used for error logging)
     * @param dimensionValue the value of the dimension to be validated (used for error logging)
     * @param shouldSucceed {@code true} if the validation is expected to succeed, {@code false}
     *     otherwise
     * @throws IOException an unexpected IO exception
     */
    private void assertValidationSuccess(
            FeatureTypeInfo featureTypeInfo,
            GetMapRequest request,
            String dimension,
            Object dimensionValue,
            boolean shouldSucceed)
            throws IOException {
        try {
            wms.validateVectorDimensions(
                    request.getTime(), request.getElevation(), featureTypeInfo, request);
            if (!shouldSucceed) {
                Assert.fail(
                        "A validation exception is expected for vector dimension '"
                                + dimension
                                + "' with value: "
                                + dimensionValue);
            }
        } catch (ServiceException e) {
            if (shouldSucceed) {
                e.printStackTrace(System.err);
                Assert.fail(
                        "Unexpected exception during validation of vector dimension '"
                                + dimension
                                + "' with value "
                                + dimensionValue);
            }
        }
    }

    /**
     * create a dimension using the given dimension and attributes start/end
     *
     * @param catalog the catalog
     * @param featureTypeName the type name
     * @param dimension the dimension name
     * @param start the start attribute
     * @param end the end attribute
     * @return the feature type info for which the dimension was defined
     */
    private static FeatureTypeInfo setupStartEndDimension(
            Catalog catalog, QName featureTypeName, String dimension, String start, String end) {
        FeatureTypeInfo info = catalog.getFeatureTypeByName(featureTypeName.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        catalog.save(info);
        return info;
    }

    /**
     * creates a simple map request for the given times, elevations and custom dimensions.
     *
     * <p>Note, that layers, styles etc. are not set here. Add those after this call if required.
     *
     * @param times the times (can be {@code null} or empty)
     * @param elevations the elevations (can be {@code null} or empty)
     * @param customDimensions the custom dimensions (can be {@code null} or empty)
     * @return a simple map request
     */
    private static GetMapRequest mapRequest(
            List<Object> times, List<Object> elevations, Map<String, String> customDimensions) {

        GetMapRequest request = new GetMapRequest();
        request.setFormat(WMSMockData.DummyRasterMapProducer.MIME_TYPE);
        request.setWidth(512);
        request.setHeight(256);
        request.setBbox(new Envelope(-180, 180, -90, 90));
        request.setSRS("EPSG:4326");
        request.setCrs(DefaultGeographicCRS.WGS84);
        request.setRawKvp(new HashMap<>());
        request.setBaseUrl("http://example.geoserver.org/geoserver");

        if (times != null && !times.isEmpty()) {
            request.setTime(times);
        }

        if (elevations != null && !elevations.isEmpty()) {
            request.setElevation(elevations);
        }

        if (customDimensions != null && !customDimensions.isEmpty()) {
            request.getRawKvp().putAll(customDimensions);
        }

        return request;
    }
}
