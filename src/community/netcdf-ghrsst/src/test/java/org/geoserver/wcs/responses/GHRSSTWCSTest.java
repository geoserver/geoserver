/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.kvp.WCSKVPTestSupport;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Base support class for NetCDF wcs tests.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
public class GHRSSTWCSTest extends WCSKVPTestSupport {

    public static QName SST = new QName(CiteTestData.WCS_URI, "sst", CiteTestData.WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no extra data needed
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog cat = getCatalog();

        // setup the NetCDF
        testData.addRasterLayer(SST, "sst-orbit053.nc", null, null, this.getClass(), cat);

        // build a view putting toghether all bands
        CoverageStoreInfo storeInfo = cat.getCoverageStores().get(0);
        final CoverageView coverageView = buildSstView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);
        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(SST.getLocalPart(), storeInfo, builder);
        cat.add(coverageInfo);
        LayerInfo layer = builder.buildLayer(coverageInfo);
        cat.add(layer);

        // enable time
        setupRasterDimension(getLayerId(SST), ResourceInfo.TIME, DimensionPresentation.LIST, null);

        // setup GHRSST output mode
        CoverageInfo coverage = cat.getCoverageByName("wcs:sst");
        NetCDFLayerSettingsContainer settings = new NetCDFLayerSettingsContainer();
        settings.setCopyAttributes(true);
        settings.setCopyGlobalAttributes(true);
        settings.getMetadata().put(GHRSSTEncoder.SETTINGS_KEY, Boolean.TRUE);
        coverage.getMetadata().put(NetCDFSettingsContainer.NETCDFOUT_KEY, settings);
        cat.save(coverage);
    }

    private CoverageView buildSstView() {
        String[] bandNames =
                new String[] {
                    "pixels_per_bin",
                    "sea_surface_temperature",
                    "sst_dtime",
                    "quality_level",
                    "wind_speed",
                    "wind_speed_dtime_from_sst",
                    "sea_ice_fraction",
                    "sea_ice_fraction_dtime_from_sst",
                    "sses_bias",
                    "sses_standard_deviation"
                };

        List<CoverageView.CoverageBand> coverageBands = new ArrayList<>();
        for (int i = 0; i < bandNames.length; i++) {
            String bandName = bandNames[i];
            final CoverageView.CoverageBand band =
                    new CoverageView.CoverageBand(
                            Arrays.asList(new CoverageView.InputCoverageBand(bandName, "0")),
                            bandName,
                            i,
                            CoverageView.CompositionType.BAND_SELECT);
            coverageBands.add(band);
        }
        final CoverageView coverageView = new CoverageView(SST.getLocalPart(), coverageBands);
        return coverageView;
    }

    /** Test NetCDF output from a coverage view having the required GHRSST bands/variables */
    @Test
    public void testGHRSST() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageid="
                                + getLayerId(SST).replace(":", "__")
                                + "&format=application/x-netcdf");
        assertEquals(200, response.getStatus());
        assertEquals("application/x-netcdf", response.getContentType());

        // hope the file name structure is correct, not 100% sure
        String contentDispostion = response.getHeader("Content-disposition");
        assertEquals(
                "inline; filename=19121213214553-EUR-L3U_GHRSST-SSTint-AVHRR_METOP_A-v02.0-fv01.0.nc",
                contentDispostion);
        byte[] responseBytes = getBinary(response);
        File file = File.createTempFile("ghrsst", ".nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, responseBytes);
        try (NetcdfDataset dataset =
                NetcdfDataset.openDataset(file.getAbsolutePath(), true, null)) {
            assertNotNull(dataset);

            // check global attributes
            Map<String, Attribute> globalAttributes = getGlobalAttributeMap(dataset);
            assertNotNull(globalAttributes.get("uuid"));
            UUID.fromString(globalAttributes.get("uuid").getStringValue());
            assertNotNull(globalAttributes.get("date_created"));
            assertNotNull(globalAttributes.get("spatial_resolution"));
            // input test file is a freak that really has this resolution, as verified with gdalinfo
            // e.g. gdalinfo NETCDF:"sst-orbit053.nc":"sea_surface_temperature"
            assertEquals(
                    "179.9499969482422 degrees",
                    globalAttributes.get("spatial_resolution").getStringValue());
            // likewise, it really has time time
            assertNotNull(globalAttributes.get("start_time"));
            assertNotNull(globalAttributes.get("time_coverage_start"));
            assertEquals("19121213T204553Z", globalAttributes.get("start_time").getStringValue());
            assertEquals(
                    "19121213T204553Z",
                    globalAttributes.get("time_coverage_start").getStringValue());
            assertNotNull(globalAttributes.get("stop_time"));
            assertNotNull(globalAttributes.get("time_coverage_end"));
            assertEquals("19121213T204553Z", globalAttributes.get("stop_time").getStringValue());
            assertEquals(
                    "19121213T204553Z", globalAttributes.get("time_coverage_end").getStringValue());
            // and these bounds
            double EPS = 1e-3;
            assertNotNull(globalAttributes.get("northernmost_latitude"));
            assertNotNull(globalAttributes.get("southernmost_latitude"));
            assertNotNull(globalAttributes.get("westernmost_longitude"));
            assertNotNull(globalAttributes.get("easternmost_longitude"));
            assertEquals(
                    119.925,
                    globalAttributes.get("northernmost_latitude").getNumericValue().doubleValue(),
                    EPS);
            assertEquals(
                    -119.925,
                    globalAttributes.get("southernmost_latitude").getNumericValue().doubleValue(),
                    EPS);
            assertEquals(
                    -269.925,
                    globalAttributes.get("westernmost_longitude").getNumericValue().doubleValue(),
                    EPS);
            assertEquals(
                    269.925,
                    globalAttributes.get("easternmost_longitude").getNumericValue().doubleValue(),
                    EPS);
            // resolution, take 2
            assertNotNull(globalAttributes.get("geospatial_lat_units"));
            assertNotNull(globalAttributes.get("geospatial_lon_units"));
            assertEquals("degrees", globalAttributes.get("geospatial_lat_units").getStringValue());
            assertEquals("degrees", globalAttributes.get("geospatial_lon_units").getStringValue());

            // sea surface temperature
            Variable sst = dataset.findVariable("sea_surface_temperature");
            assertNotNull(sst);
            assertEquals(DataType.SHORT, sst.getDataType());
            assertNotNull(sst.findAttribute("scale_factor"));
            assertNotNull(sst.findAttribute("add_offset"));
            assertAttributeValue(sst, "comment", "Marine skin surface temperature");
            assertAttributeValue(sst, "long_name", "sea surface skin temperature");
            assertAttributeValue(sst, "standard_name", "sea_surface_skin_temperature");
            assertAttributeValue(sst, "units", "kelvin");
            assertAttributeValue(sst, "depth", "10 micrometres");

            // wind speed
            Variable windSpeed = dataset.findVariable("wind_speed");
            assertNotNull(windSpeed);
            assertEquals(DataType.BYTE, windSpeed.getDataType());
            assertNotNull(windSpeed.findAttribute("scale_factor"));
            assertNotNull(windSpeed.findAttribute("add_offset"));
            assertAttributeValue(
                    windSpeed,
                    "comment",
                    "Typically represents surface winds (10 meters above the sea " + "surface)");
            assertAttributeValue(windSpeed, "long_name", "wind speed");
            assertAttributeValue(windSpeed, "standard_name", "wind_speed");
            assertAttributeValue(windSpeed, "units", "m s-1");

            // enable ehancing to check values
            dataset.enhance(NetcdfDataset.getEnhanceAll());
            assertValues(
                    dataset,
                    "sea_surface_temperature",
                    new double[] {301, 302, 303, 304, 305, 306, 307, 308, 309},
                    2e-3);
            assertValues(dataset, "wind_speed", new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9}, 0.2);
        } finally {
            // FileUtils.deleteQuietly(file);
        }
    }

    private Map<String, Attribute> getGlobalAttributeMap(NetcdfDataset dataset) {
        return dataset.getGlobalAttributes()
                .stream()
                .collect(Collectors.toMap(Attribute::getShortName, Function.identity()));
    }

    private void assertValues(
            NetcdfDataset dataset, String variableName, double[] expectedValues, double tolerance)
            throws IOException {
        Variable variable = dataset.findVariable(variableName);
        double[] values = (double[]) variable.read().copyTo1DJavaArray();
        assertArrayEquals(values, expectedValues, tolerance);
    }

    private void assertAttributeValue(Variable variable, String name, Object value) {
        Attribute attribute = variable.findAttribute(name);
        if (value == null) {
            assertNull(attribute);
        } else {
            assertNotNull(attribute);
            assertEquals(value, attribute.getValue(0));
        }
    }

    /** Test NetCDF output from a coverage view having the required GHRSST bands/variables */
    @Test
    public void testGHRSSTSubset() throws Exception {
        // test requires NetCDF-4 native libs to be available
        Assume.assumeTrue(NetCDFUtilities.isNC4CAvailable());

        // this used to crash
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageid="
                                + getLayerId(SST).replace(":", "__")
                                + "&subset=Long(-10,10)&subset=Lat(-10,10)"
                                + "&format=application/x-netcdf4");
        assertEquals(200, response.getStatus());
        assertEquals("application/x-netcdf4", response.getContentType());
    }
}
