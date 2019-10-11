/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.ExtraVariable;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.GlobalAttribute;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.VariableAttribute;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.coverage.io.netcdf.crs.NetCDFCRSAuthorityFactory;
import org.geotools.feature.NameImpl;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.mock.web.MockHttpServletResponse;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

@TestSetup(run = TestSetupFrequency.ONCE)
public class WCSNetCDFMosaicTest extends WCSNetCDFBaseTest {

    private static final double DELTA = 1E-6;
    private static final double DELTA2 = 1E-4;
    private static final double PACKED_FILL_VALUE = -32768d;
    private static final String ORIGINAL_UNIT = "km";
    private static final String CANONICAL_UNIT = "m";
    private static final double ORIGINAL_FILL_VALUE = -9999.0d;
    private static final double ORIGINAL_PIXEL_VALUE = 9219.328d;

    public static QName LATLONMOSAIC =
            new QName(CiteTestData.WCS_URI, "2DLatLonCoverage", CiteTestData.WCS_PREFIX);
    public static QName DUMMYMOSAIC =
            new QName(CiteTestData.WCS_URI, "DummyCoverage", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYCF =
            new QName(CiteTestData.WCS_URI, "visibilityCF", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYPACKED =
            new QName(CiteTestData.WCS_URI, "visibilityPacked", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYCOMPRESSED =
            new QName(CiteTestData.WCS_URI, "visibilityCompressed", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYCFPACKED =
            new QName(CiteTestData.WCS_URI, "visibilityCFPacked", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYNANPACKED =
            new QName(CiteTestData.WCS_URI, "visibilityNaNPacked", CiteTestData.WCS_PREFIX);
    public static QName TEMPERATURE_SURFACE =
            new QName(CiteTestData.WCS_URI, "Temperature_surface", CiteTestData.WCS_PREFIX);

    public static QName BANDWITHCRS =
            new QName(CiteTestData.WCS_URI, "Band1", CiteTestData.WCS_PREFIX);

    private static final String STANDARD_NAME = "visibility_in_air";
    private static final Section NETCDF_SECTION;
    private CoverageView coverageView = null;

    static {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        final List<Range> ranges = new LinkedList<Range>();
        ranges.add(new Range(1));
        ranges.add(new Range(1));
        NETCDF_SECTION = new Section(ranges);
    }

    @Before
    public void init() {

        // make sure CRS ordering is correct
        System.setProperty("org.geotools.referencing.forceXY", "true");
        System.setProperty("user.timezone", "GMT");
    }

    @AfterClass
    public static void close() {
        System.clearProperty("org.geotools.referencing.forceXY");
        System.clearProperty("user.timezone");
        System.clearProperty(NetCDFCRSAuthorityFactory.SYSTEM_DEFAULT_USER_PROJ_FILE);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // workaround to add our custom multi dimensional format
        try {
            Field field = GetCoverage.class.getDeclaredField("mdFormats");
            field.setAccessible(true);
            ((Set<String>) field.get(null)).add(WCSResponseInterceptor.MIME_TYPE);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        super.onSetUp(testData);
        testData.addRasterLayer(
                LATLONMOSAIC, "2DLatLonCoverage.zip", null, null, this.getClass(), getCatalog());
        setupRasterDimension(
                getLayerId(LATLONMOSAIC), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(LATLONMOSAIC),
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + "BANDS",
                DimensionPresentation.LIST,
                null);

        testData.addRasterLayer(DUMMYMOSAIC, "gom.zip", null, null, this.getClass(), getCatalog());

        createCoverageView();
        addViewToCatalog();

        testData.addRasterLayer(
                VISIBILITYCF, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYCF);

        testData.addRasterLayer(
                VISIBILITYPACKED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYPACKED);

        testData.addRasterLayer(
                VISIBILITYCFPACKED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYCFPACKED);

        testData.addRasterLayer(
                VISIBILITYNANPACKED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYNANPACKED, false);

        testData.addRasterLayer(
                VISIBILITYCOMPRESSED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYCOMPRESSED);

        testData.addRasterLayer(
                BANDWITHCRS, "utm_esri_pe_string.nc", null, null, this.getClass(), getCatalog());

        testData.addRasterLayer(
                TEMPERATURE_SURFACE,
                "Temperature_surface.zip",
                null,
                null,
                this.getClass(),
                getCatalog());
        setupRasterDimension(
                getLayerId(TEMPERATURE_SURFACE),
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null);
        configureTemperatureSurface();
    }

    private void setupNetCDFoutSettings(QName name) {
        setupNetCDFoutSettings(name, true);
    }

    private void setupNetCDFoutSettings(QName name, boolean setNoData) {
        CoverageInfo info = getCatalog().getCoverageByName(getLayerId(name));

        // Set the Declared SRS
        info.setSRS("EPSG:4326");
        info.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);

        String layerName = name.getLocalPart().toUpperCase();
        boolean isPackedLayer = layerName.contains("PACKED");
        boolean isCF = layerName.contains("CF");
        boolean isCompressed = layerName.contains("COMPRESSED");
        // Update the UnitOfMeasure to km and noData to -9999
        CoverageDimensionInfo dimension = info.getDimensions().get(0);
        String originalUnit = ORIGINAL_UNIT;
        dimension.setUnit(originalUnit);

        List<Double> nullValues = dimension.getNullValues();
        if (nullValues != null) {
            nullValues.clear();
            if (setNoData) nullValues.add(ORIGINAL_FILL_VALUE);
        }

        NetCDFLayerSettingsContainer container = new NetCDFLayerSettingsContainer();
        container.setCompressionLevel(isCompressed ? 9 : 0);
        container.setShuffle(true);
        container.setDataPacking(isPackedLayer ? DataPacking.SHORT : DataPacking.NONE);

        List<GlobalAttribute> attributes = new ArrayList<GlobalAttribute>();
        attributes.add(new GlobalAttribute("custom_attribute", "testing WCS"));
        attributes.add(new GlobalAttribute("Conventions", "CF-1.6"));
        attributes.add(new GlobalAttribute("NULLAttribute", null));
        container.setGlobalAttributes(attributes);

        // Setting a different name from the standard table as well as the proper
        // canonical unit
        container.setLayerName(STANDARD_NAME);
        container.setLayerUOM(isCF ? CANONICAL_UNIT : originalUnit);

        String key = "NetCDFOutput.Key";
        info.getMetadata().put(key, container);
        getCatalog().save(info);
    }

    @Test
    public void testRequestCoverage() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__2DLatLonCoverage&format=application/custom&subset=time,http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC(\"2013-11-01T00:00:00.000Z\")&subset=BANDS(\"MyBand\")");
        assertNotNull(response);
        GridCoverage2D lastResult =
                applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();
        assertTrue(lastResult instanceof GranuleStack);
        GranuleStack stack = (GranuleStack) lastResult;

        // we expect a single granule which covers the entire mosaic
        for (GridCoverage2D c : stack.getGranules()) {
            assertEquals(30., c.getEnvelope2D().getHeight(), 0.001);
            assertEquals(45., c.getEnvelope2D().getWidth(), 0.001);
        }
        assertEquals(1, stack.getGranules().size());
    }

    @Test
    public void testRequestCoverageLatLon() throws Exception {
        final WCSInfo wcsInfo = getWCS();
        final boolean oldLatLon = wcsInfo.isLatLon();
        wcsInfo.setLatLon(true);
        getGeoServer().save(wcsInfo);
        try {
            // http response from the request inside the string
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__2DLatLonCoverage&format=application/custom&subset=time,http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC(\"2013-11-01T00:00:00.000Z\")&subset=BANDS(\"MyBand\")");
            assertNotNull(response);
            GridCoverage2D lastResult =
                    applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();
            assertTrue(lastResult instanceof GranuleStack);
            GranuleStack stack = (GranuleStack) lastResult;

            // we expect a single granule which covers the entire mosaic
            for (GridCoverage2D c : stack.getGranules()) {
                // System.out.println(c.getEnvelope());
                assertEquals(45., c.getEnvelope2D().getHeight(), 0.001);
                assertEquals(30., c.getEnvelope2D().getWidth(), 0.001);
            }
            assertEquals(1, stack.getGranules().size());
        } finally {
            wcsInfo.setLatLon(oldLatLon);
            getGeoServer().save(wcsInfo);
        }
    }

    @Test
    public void testRequestCoverageView() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__dummyView&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/time(\"2013-01-08T00:00:00.000Z\")");
        assertNotNull(response);

        assertEquals("application/x-netcdf", response.getContentType());
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "out.nc", new File("./target"));
        try {
            FileUtils.writeByteArrayToFile(file, netcdfOut);

            NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
            assertNotNull(dataset);
            dataset.close();
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @Test
    public void testRequestNetCDF4() throws Exception {

        boolean isNC4Available = NetCDFUtilities.isNC4CAvailable();
        if (!isNC4Available && LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("NetCDF C library not found. NetCDF4 output will not be created");
        }

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__dummyView&format=application/x-netcdf4&subset=http://www.opengis.net/def/axis/OGC/0/time(\"2013-01-08T00:00:00.000Z\")");
        assertNotNull(response);

        assertEquals(
                (isNC4Available ? "application/x-netcdf4" : "application/xml"),
                response.getContentType());
        if (isNC4Available) {
            byte[] netcdfOut = getBinary(response);
            File file = File.createTempFile("netcdf", "out.nc", new File("./target"));
            FileUtils.writeByteArrayToFile(file, netcdfOut);

            NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
            assertNotNull(dataset);
            dataset.close();
        }
    }

    @Test
    public void testRequestNetCDFUomConversion() throws Exception {

        // http response from the request inside the string
        CoverageInfo info = getCatalog().getCoverageByName(new NameImpl("wcs", "visibilityCF"));
        assertTrue(info.getDimensions().get(0).getUnit().equalsIgnoreCase(ORIGINAL_UNIT));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__visibilityCF&format=application/x-netcdf");
        assertNotNull(response);
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "outCF.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, netcdfOut);

        NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
        Variable var = dataset.findVariable(STANDARD_NAME);
        assertNotNull(var);

        // Check the unit has been converted to meter
        String unit = var.getUnitsString();
        assertEquals(CANONICAL_UNIT, unit);

        Array readData = var.read(NETCDF_SECTION);
        assertEquals(DataType.FLOAT, readData.getDataType());
        float data = readData.getFloat(0);

        // Data have been converted to canonical unit (m) from km.
        // Data value is bigger
        assertEquals(data, (ORIGINAL_PIXEL_VALUE) * 1000, DELTA);

        Attribute fillValue = var.findAttribute(NetCDFUtilities.FILL_VALUE);
        Attribute standardName = var.findAttribute(NetCDFUtilities.STANDARD_NAME);
        assertNotNull(standardName);
        assertEquals(STANDARD_NAME, standardName.getStringValue());
        assertNotNull(fillValue);
        assertEquals(ORIGINAL_FILL_VALUE, fillValue.getNumericValue().doubleValue(), DELTA);

        // Check global attributes have been added
        Attribute attribute = dataset.findGlobalAttribute("custom_attribute");
        assertNotNull(attribute);
        assertEquals("testing WCS", attribute.getStringValue());

        dataset.close();
    }

    @Test
    public void testRequestNetCDFCompression() throws Exception {
        boolean isNC4Available = NetCDFUtilities.isNC4CAvailable();
        if (!isNC4Available && LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("NetCDF C library not found. NetCDF4 output will not be created");
        }

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__visibilityCompressed&format=application/x-netcdf4");
        assertNotNull(response);

        assertEquals(
                (isNC4Available ? "application/x-netcdf4" : "application/xml"),
                response.getContentType());

        if (isNC4Available) {
            byte[] netcdfOut = getBinary(response);
            File file = File.createTempFile("netcdf", "outCompressed.nc", new File("./target"));
            FileUtils.writeByteArrayToFile(file, netcdfOut);

            NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
            assertNotNull(dataset);

            Variable var = dataset.findVariable(STANDARD_NAME);
            assertNotNull(var);
            final long varByteSize = var.getSize() * var.getDataType().getSize();

            // The output file is smaller than the size of the underlying variable.
            // Compression successfully occurred
            assertTrue(netcdfOut.length < varByteSize);
            dataset.close();
        }
    }

    @Test
    public void testRequestNetCDFDataPacking() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__visibilityPacked&format=application/x-netcdf");
        assertNotNull(response);
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "outPK.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, netcdfOut);

        NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
        Variable var = dataset.findVariable(STANDARD_NAME);
        assertNotNull(var);

        // Check the unit hasn't been converted
        String unit = var.getUnitsString();
        assertEquals(ORIGINAL_UNIT, unit);

        Attribute fillValue = var.findAttribute(NetCDFUtilities.FILL_VALUE);
        assertNotNull(fillValue);

        // There is dataPacking, therefore, fillValue should have been changed
        assertEquals(PACKED_FILL_VALUE, fillValue.getNumericValue().doubleValue(), 1E-6);

        Attribute addOffsetAttr = var.findAttribute(DataPacking.ADD_OFFSET);
        assertNotNull(addOffsetAttr);

        Attribute scaleFactorAttr = var.findAttribute(DataPacking.SCALE_FACTOR);
        assertNotNull(scaleFactorAttr);
        double scaleFactor = scaleFactorAttr.getNumericValue().doubleValue();
        double addOffset = addOffsetAttr.getNumericValue().doubleValue();

        Array readData = var.read(NETCDF_SECTION);
        assertEquals(DataType.SHORT, readData.getDataType());
        short data = readData.getShort(0);
        // Data has been packed to short

        double packedData = (ORIGINAL_PIXEL_VALUE - addOffset) / scaleFactor;
        assertEquals((short) (packedData + 0.5), data, DELTA);

        dataset.close();
    }

    @Test
    public void testRequestNetCDFCFDataPacking() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__visibilityCFPacked&format=application/x-netcdf");
        assertNotNull(response);
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "outCFPK.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, netcdfOut);

        NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
        Variable var = dataset.findVariable(STANDARD_NAME);
        assertNotNull(var);

        // Check the unit has been converted to meter
        String unit = var.getUnitsString();
        assertEquals(CANONICAL_UNIT, unit);

        Attribute addOffsetAttr = var.findAttribute(DataPacking.ADD_OFFSET);
        assertNotNull(addOffsetAttr);

        Attribute scaleFactorAttr = var.findAttribute(DataPacking.SCALE_FACTOR);
        assertNotNull(scaleFactorAttr);
        double scaleFactor = scaleFactorAttr.getNumericValue().doubleValue();
        double addOffset = addOffsetAttr.getNumericValue().doubleValue();

        Array readData = var.read(NETCDF_SECTION);
        assertEquals(DataType.SHORT, readData.getDataType());
        short data = readData.getShort(0);
        // Data has been packed to short

        // Going from original unit to canonical, then packing
        double packedData = ((ORIGINAL_PIXEL_VALUE * 1000) - addOffset) / scaleFactor;
        assertEquals((short) (packedData + 0.5), data, DELTA);

        Attribute fillValue = var.findAttribute(NetCDFUtilities.FILL_VALUE);
        assertNotNull(fillValue);
        // There is dataPacking, therefore, fillValue should have been changed
        assertEquals(PACKED_FILL_VALUE, fillValue.getNumericValue().doubleValue(), DELTA);

        // Check global attributes have been added
        Attribute attribute = dataset.findGlobalAttribute("custom_attribute");
        assertNotNull(attribute);
        assertEquals("testing WCS", attribute.getStringValue());

        dataset.close();
    }

    @Test
    public void testRequestNetCDFNaNDataPacking() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__visibilityNaNPacked&format=application/x-netcdf");
        assertNotNull(response);
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "outNaNPK.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, netcdfOut);

        NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
        Variable var = dataset.findVariable(STANDARD_NAME);
        assertNotNull(var);

        Array readData = var.read(NETCDF_SECTION);
        assertEquals(DataType.SHORT, readData.getDataType());

        // Check the fix on dataPacking NaN management
        assertNotEquals(readData.getShort(0), -32768, 1E-6);
        dataset.close();
    }

    @Test
    public void testRequestNetCDFCrs() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ows?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__Band1&format=application/x-netcdf"
                                + "&subsettingcrs=http://www.opengis.net/def/crs/EPSG/0/4326"
                                + "&subset=Long(-118,-116)&subset=Lat(56,58)");
        // Original data was UTM 32611
        assertNotNull(response);
        byte[] netcdfOut = getBinary(response);
        File file = File.createTempFile("netcdf", "outcrs.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, netcdfOut);

        // Retrieve the GeoTransform attribute from the output NetCDF
        NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath());
        String geoTransform = dataset.findGlobalAttribute("GeoTransform").getStringValue();
        dataset.close();
        assertNotNull(geoTransform);

        String[] coefficients = geoTransform.split(" ");
        double m00 = Double.parseDouble(coefficients[1]);
        double m01 = Double.parseDouble(coefficients[2]);
        double m02 = Double.parseDouble(coefficients[0]);
        double m10 = Double.parseDouble(coefficients[4]);
        double m11 = Double.parseDouble(coefficients[5]);
        double m12 = Double.parseDouble(coefficients[3]);

        NetCDFReader reader = new NetCDFReader(file, null);
        MathTransform transform = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
        AffineTransform2D affineTransform = (AffineTransform2D) transform;

        reader.dispose();

        // Check the GeoTransform coefficients are valid
        assertEquals(m02, affineTransform.getTranslateX(), DELTA2);
        assertEquals(m12, affineTransform.getTranslateY(), DELTA2);
        assertEquals(m00, affineTransform.getScaleX(), DELTA2);
        assertEquals(m11, affineTransform.getScaleY(), DELTA2);
        assertEquals(m01, affineTransform.getShearX(), DELTA2);
        assertEquals(m10, affineTransform.getShearY(), DELTA2);
    }

    private void addViewToCatalog() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName(DUMMYMOSAIC.getLocalPart());

        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo("dummyView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        final LayerInfo layerInfo = builder.buildLayer(coverageInfo);
        cat.add(layerInfo);
        setupRasterDimension("dummyView", ResourceInfo.TIME, DimensionPresentation.LIST, null);
    }

    private void createCoverageView() throws Exception {
        final InputCoverageBand band1 = new InputCoverageBand("NO2", "0");
        final CoverageBand outputBand1 =
                new CoverageBand(
                        Collections.singletonList(band1), "NO2@0", 0, CompositionType.BAND_SELECT);

        final InputCoverageBand band2 = new InputCoverageBand("BrO", "0");
        final CoverageBand outputBand2 =
                new CoverageBand(
                        Collections.singletonList(band2), "BrO@0", 1, CompositionType.BAND_SELECT);
        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(2);
        coverageBands.add(outputBand1);
        coverageBands.add(outputBand2);
        coverageView = new CoverageView("dummyView", coverageBands);
    }

    /** Configure NetCDF output settings for <code>Temperature_surface</code>. */
    private void configureTemperatureSurface() {
        NetCDFLayerSettingsContainer container = new NetCDFLayerSettingsContainer();
        container.setCopyAttributes(true);
        List<VariableAttribute> variableAttributes = new ArrayList<VariableAttribute>();
        variableAttributes.add(
                new VariableAttribute("test-variable-attribute", "Test Variable Attribute"));
        variableAttributes.add(new VariableAttribute("Grib2_Parameter_Category", "Test Category"));
        container.setVariableAttributes(variableAttributes);
        List<ExtraVariable> extraVariables = new ArrayList<ExtraVariable>();
        extraVariables.add(new ExtraVariable("reftime", "forecast_reference_time", "time"));
        extraVariables.add(new ExtraVariable("reftime", "scalar_forecast_reference_time", ""));
        container.setExtraVariables(extraVariables);
        List<GlobalAttribute> globalAttributes = new ArrayList<GlobalAttribute>();
        globalAttributes.add(new GlobalAttribute("test-global-attribute", "Test Global Attribute"));
        globalAttributes.add(new GlobalAttribute("test-global-attribute-integer", "42"));
        globalAttributes.add(new GlobalAttribute("test-global-attribute-double", "1.5"));
        container.setGlobalAttributes(globalAttributes);
        CoverageInfo info = getCatalog().getCoverageByName(getLayerId(TEMPERATURE_SURFACE));
        info.getMetadata().put(NetCDFSettingsContainer.NETCDFOUT_KEY, container);
        getCatalog().save(info);
    }

    /**
     * Test <code>Temperature_surface</code> extra variables, variable attributes, and global
     * attributes of different types, for NetCDF-3 output.
     */
    @Test
    public void testExtraVariablesNetcdf3() throws Exception {
        checkExtraVariables("application/x-netcdf");
    }

    /**
     * Test <code>Temperature_surface</code> extra variables, variable attributes, and global
     * attributes of different types, for NetCDF-4 output.
     */
    @Test
    public void testExtraVariablesNetcdf4() throws Exception {
        assumeTrue(NetCDFUtilities.isNC4CAvailable());
        checkExtraVariables("application/x-netcdf4");
    }

    /**
     * Check <code>Temperature_surface</code> extra variables, variable attributes, and global
     * attributes of different type.
     *
     * @param format the output format MIME type
     */
    private void checkExtraVariables(String format) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?service=WCS&version=2.0.1&request=GetCoverage"
                                + "&coverageid=wcs__Temperature_surface&format="
                                + format);
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(format, response.getContentType());
        byte[] responseBytes = getBinary(response);
        File file =
                File.createTempFile(
                        "extra-variable-", "-wcs__Temperature_surface.nc", new File("./target"));
        FileUtils.writeByteArrayToFile(file, responseBytes);
        try (NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath())) {
            assertNotNull(dataset);
            // check dimensions
            Dimension timeDim = dataset.findDimension("time");
            assertNotNull(timeDim);
            assertEquals(2, timeDim.getLength());
            Dimension rlonDim = dataset.findDimension("rlon");
            assertNotNull(rlonDim);
            assertEquals(7, rlonDim.getLength());
            Dimension rlatDim = dataset.findDimension("rlat");
            assertNotNull(rlatDim);
            assertEquals(5, rlatDim.getLength());
            // check coordinate variables
            Variable timeVar = dataset.findVariable("time");
            assertNotNull(timeVar);
            assertEquals(1, timeVar.getDimensions().size());
            assertEquals(timeDim, timeVar.getDimensions().get(0));
            assertEquals("time", timeVar.findAttribute("long_name").getStringValue());
            assertEquals("time", timeVar.findAttribute("description").getStringValue());
            assertEquals(
                    "seconds since 1970-01-01 00:00:00 UTC",
                    timeVar.findAttribute("units").getStringValue());
            assertArrayEquals(
                    new double[] {1461664800, 1461708000},
                    (double[]) timeVar.read().copyTo1DJavaArray(),
                    (double) DELTA);
            Variable rlonVar = dataset.findVariable("rlon");
            assertNotNull(rlonVar);
            assertEquals(1, rlonVar.getDimensions().size());
            assertEquals(rlonDim, rlonVar.getDimensions().get(0));
            assertEquals("grid_longitude", rlonVar.findAttribute("long_name").getStringValue());
            assertEquals("grid_longitude", rlonVar.findAttribute("standard_name").getStringValue());
            assertEquals("degrees", rlonVar.findAttribute("units").getStringValue());
            assertArrayEquals(
                    new float[] {-30, -20, -10, 0, 10, 20, 30},
                    (float[]) rlonVar.read().copyTo1DJavaArray(),
                    (float) DELTA);
            Variable rlatVar = dataset.findVariable("rlat");
            assertNotNull(rlatVar);
            assertEquals(1, rlatVar.getDimensions().size());
            assertEquals(rlatDim, rlatVar.getDimensions().get(0));
            assertEquals("grid_latitude", rlatVar.findAttribute("long_name").getStringValue());
            assertEquals("grid_latitude", rlatVar.findAttribute("standard_name").getStringValue());
            assertEquals("degrees", rlatVar.findAttribute("units").getStringValue());
            assertArrayEquals(
                    new float[] {-20, -10, 0, 10, 20},
                    (float[]) rlatVar.read().copyTo1DJavaArray(),
                    (float) DELTA);
            // check projection variable
            Variable projVar = dataset.findVariable("rotated_latitude_longitude");
            assertNotNull(projVar);
            assertEquals(
                    "rotated_latitude_longitude",
                    projVar.findAttribute("grid_mapping_name").getStringValue());
            assertEquals(
                    74.0,
                    projVar.findAttribute("grid_north_pole_longitude")
                            .getNumericValue()
                            .doubleValue(),
                    DELTA);
            assertEquals(
                    36.0,
                    projVar.findAttribute("grid_north_pole_latitude")
                            .getNumericValue()
                            .doubleValue(),
                    DELTA);
            // check data variable
            Variable tempVar = dataset.findVariable("Temperature_surface");
            assertNotNull(tempVar);
            assertEquals(
                    "rotated_latitude_longitude",
                    tempVar.findAttribute("grid_mapping").getStringValue());
            assertEquals("K", tempVar.findAttribute("units").getStringValue());
            assertEquals(3, tempVar.getDimensions().size());
            assertEquals(timeDim, tempVar.getDimensions().get(0));
            assertEquals(rlatDim, tempVar.getDimensions().get(1));
            assertEquals(rlonDim, tempVar.getDimensions().get(2));
            assertArrayEquals(
                    new float[] {
                        300, 299, 298, 297, 296, 295, 294, 299, 300, 299, 298, 297, 296, 295, 298,
                        299, 300, 299, 298, 297, 296, 297, 298, 299, 300, 299, 298, 297, 296, 297,
                        298, 299, 300, 299, 298, 301, 300, 299, 298, 297, 296, 295, 300, 301, 300,
                        299, 298, 297, 296, 299, 300, 301, 300, 299, 298, 297, 298, 299, 300, 301,
                        300, 299, 298, 297, 298, 299, 300, 301, 300, 299
                    },
                    (float[]) tempVar.read().copyTo1DJavaArray(),
                    (float) DELTA);
            // some attributes expected to copied from source variable
            assertEquals("TMP", tempVar.findAttribute("abbreviation").getStringValue());
            assertEquals(
                    "Forecast",
                    tempVar.findAttribute("Grib2_Generating_Process_Type").getStringValue());
            // should not be copied from source variable as in the blacklist
            assertNull(tempVar.findAttribute("coordinates"));
            // test that copied variable attributes can be overwritten
            assertEquals(
                    "Test Category",
                    tempVar.findAttribute("Grib2_Parameter_Category").getStringValue());
            // test that a new variable attribute can be added
            assertEquals(
                    "Test Variable Attribute",
                    tempVar.findAttribute("test-variable-attribute").getStringValue());
            // extra variable copied from source with dimensions "time"
            Variable reftimeVar = dataset.findVariable("forecast_reference_time");
            assertEquals(1, reftimeVar.getDimensions().size());
            assertEquals(timeDim, reftimeVar.getDimensions().get(0));
            assertEquals(
                    "Hour since 2016-04-25T22:00:00Z",
                    reftimeVar.findAttribute("units").getStringValue());
            assertEquals(
                    "forecast_reference_time",
                    reftimeVar.findAttribute("standard_name").getStringValue());
            assertEquals(
                    "GRIB reference time", reftimeVar.findAttribute("long_name").getStringValue());
            assertArrayEquals(
                    new double[] {6, 3},
                    (double[]) reftimeVar.read().copyTo1DJavaArray(),
                    (double) DELTA);
            // scalar extra variable copied from source with dimensions ""
            Variable scalarReftimeVar = dataset.findVariable("scalar_forecast_reference_time");
            assertEquals(0, scalarReftimeVar.getDimensions().size());
            assertEquals(
                    "Hour since 2016-04-25T22:00:00Z",
                    scalarReftimeVar.findAttribute("units").getStringValue());
            assertEquals(
                    "forecast_reference_time",
                    scalarReftimeVar.findAttribute("standard_name").getStringValue());
            assertEquals(
                    "GRIB reference time",
                    scalarReftimeVar.findAttribute("long_name").getStringValue());
            double t = scalarReftimeVar.read().getDouble(0);
            // the value is nondeterministic because it depends
            // on which of two granules is used as the sample
            assertTrue(t == 6 || t == 3);
            // string global attribute
            assertEquals(
                    "Test Global Attribute",
                    dataset.findGlobalAttribute("test-global-attribute").getStringValue());
            // integer global attribute
            assertEquals(
                    DataType.INT,
                    dataset.findGlobalAttribute("test-global-attribute-integer").getDataType());
            assertEquals(
                    42,
                    dataset.findGlobalAttribute("test-global-attribute-integer").getNumericValue());
            // double global attribute
            assertEquals(
                    DataType.DOUBLE,
                    dataset.findGlobalAttribute("test-global-attribute-double").getDataType());
            assertEquals(
                    1.5,
                    dataset.findGlobalAttribute("test-global-attribute-double").getNumericValue());
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }
}
