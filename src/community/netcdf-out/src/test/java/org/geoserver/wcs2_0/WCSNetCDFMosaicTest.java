/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geoserver.web.netcdf.DataPacking;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.GlobalAttribute;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.io.netcdf.crs.NetCDFCRSAuthorityFactory;
import org.geotools.feature.NameImpl;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WCSNetCDFMosaicTest extends WCSNetCDFBaseTest {

    private static final double DELTA = 1E-6;
    private static final double PACKED_FILL_VALUE = -32768d;
    private static final String ORIGINAL_UNIT = "km";
    private static final String CANONICAL_UNIT = "m";
    private final static double ORIGINAL_FILL_VALUE = -9999.0d;
    private final static double ORIGINAL_PIXEL_VALUE = 9219.328d;

    public static QName LATLONMOSAIC = new QName(CiteTestData.WCS_URI, "2DLatLonCoverage", CiteTestData.WCS_PREFIX);
    public static QName DUMMYMOSAIC = new QName(CiteTestData.WCS_URI, "DummyCoverage", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYCF = new QName(CiteTestData.WCS_URI, "visibilityCF", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYPACKED = new QName(CiteTestData.WCS_URI, "visibilityPacked", CiteTestData.WCS_PREFIX);
    public static QName VISIBILITYCFPACKED = new QName(CiteTestData.WCS_URI, "visibilityCFPacked", CiteTestData.WCS_PREFIX);

    private final static String STANDARD_NAME = "visibility_in_air";
    private final static Section NETCDF_SECTION;
    private CoverageView coverageView = null;

    static{
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
        testData.copyTo(getClass().getResourceAsStream("reduced-cf-standard-name-table.xml"), "cf-standard-name-table.xml");
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
        testData.addRasterLayer(LATLONMOSAIC, "2DLatLonCoverage.zip", null, null, this.getClass(), getCatalog());
        setupRasterDimension(getLayerId(LATLONMOSAIC), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(getLayerId(LATLONMOSAIC), ResourceInfo.CUSTOM_DIMENSION_PREFIX + "BANDS", DimensionPresentation.LIST, null);

        testData.addRasterLayer(DUMMYMOSAIC, "gom.zip", null, null, this.getClass(), getCatalog());

        createCoverageView();
        addViewToCatalog();

        testData.addRasterLayer(VISIBILITYCF, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYCF);

        testData.addRasterLayer(VISIBILITYPACKED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYPACKED);

        testData.addRasterLayer(VISIBILITYCFPACKED, "visibility.zip", null, null, this.getClass(), getCatalog());
        setupNetCDFoutSettings(VISIBILITYCFPACKED);
    }

    private void setupNetCDFoutSettings(QName name) {
        CoverageInfo info = getCatalog().getCoverageByName(getLayerId(name));

        // Set the Declared SRS
        info.setSRS("EPSG:4326");
        info.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);

        String layerName = name.getLocalPart().toUpperCase();
        boolean isPackedLayer = layerName.contains("PACKED");
        boolean isCF = layerName.contains("CF");
        // Update the UnitOfMeasure to km and noData to -9999
        CoverageDimensionInfo dimension = info.getDimensions().get(0);
        String originalUnit = ORIGINAL_UNIT; 
        dimension.setUnit(originalUnit);

        List<Double> nullValues = dimension.getNullValues();
        if (nullValues != null) {
            nullValues.clear();
            nullValues.add(ORIGINAL_FILL_VALUE);
        }

        NetCDFLayerSettingsContainer container = new NetCDFLayerSettingsContainer();
        container.setCompressionLevel(0);
        container.setShuffle(true);
        container.setDataPacking(isPackedLayer ? DataPacking.SHORT : DataPacking.NONE);

        List<GlobalAttribute> attributes = new ArrayList<GlobalAttribute>();
        attributes.add(new GlobalAttribute("custom_attribute", "testing WCS"));
        attributes.add(new GlobalAttribute("Conventions", "CF-1.6"));
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
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__2DLatLonCoverage&format=application/custom&subset=time,http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC(\"2013-11-01T00:00:00.000Z\")&subset=BANDS(\"MyBand\")");
        assertNotNull(response);
        GridCoverage2D lastResult = applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();
        assertTrue(lastResult instanceof GranuleStack);
        GranuleStack stack = (GranuleStack) lastResult;

        //we expect a single granule which covers the entire mosaic
        for(GridCoverage2D c : stack.getGranules()){
            assertEquals(30., c.getEnvelope2D().getHeight(),0.001);
            assertEquals(45., c.getEnvelope2D().getWidth(),0.001);
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
            MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1"
                    + "&coverageId=wcs__2DLatLonCoverage&format=application/custom&subset=time,http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC(\"2013-11-01T00:00:00.000Z\")&subset=BANDS(\"MyBand\")");
            assertNotNull(response);
            GridCoverage2D lastResult = applicationContext.getBean(WCSResponseInterceptor.class)
                    .getLastResult();
            assertTrue(lastResult instanceof GranuleStack);
            GranuleStack stack = (GranuleStack) lastResult;

            // we expect a single granule which covers the entire mosaic
            for (GridCoverage2D c : stack.getGranules()) {
                System.out.println(c.getEnvelope());
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
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__dummyView&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/time(\"2013-01-08T00:00:00.000Z\")");
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
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__dummyView&format=application/x-netcdf4&subset=http://www.opengis.net/def/axis/OGC/0/time(\"2013-01-08T00:00:00.000Z\")");
        assertNotNull(response);

        assertEquals((isNC4Available ? "application/x-netcdf4" : "application/xml"),
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

        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__visibilityCF&format=application/x-netcdf");
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
        System.out.println("CF:" + data);
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
    public void testRequestNetCDFDataPacking() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__visibilityPacked&format=application/x-netcdf");
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
        assertEquals((short)(packedData + 0.5), data , DELTA);

        dataset.close();
    }

    @Test
    public void testRequestNetCDFCFDataPacking() throws Exception {

        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__visibilityCFPacked&format=application/x-netcdf");
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
        assertEquals((short)(packedData + 0.5), data , DELTA);

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
    
    private void addViewToCatalog() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName(DUMMYMOSAIC.getLocalPart());

        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo("dummyView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD","false");
        cat.add(coverageInfo);
        final LayerInfo layerInfo = builder.buildLayer(coverageInfo);
        cat.add(layerInfo);
        setupRasterDimension("dummyView", ResourceInfo.TIME, DimensionPresentation.LIST, null);
    }

    private void createCoverageView() throws Exception {
        final InputCoverageBand band1 = new InputCoverageBand("NO2", "0");
        final CoverageBand outputBand1 = new CoverageBand(Collections.singletonList(band1),
                "NO2@0", 0, CompositionType.BAND_SELECT);

        final InputCoverageBand band2 = new InputCoverageBand("BrO", "0");
        final CoverageBand outputBand2 = new CoverageBand(Collections.singletonList(band2),
                "BrO@0", 1, CompositionType.BAND_SELECT);
        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(2);
        coverageBands.add(outputBand1);
        coverageBands.add(outputBand2);
        coverageView = new CoverageView("dummyView", coverageBands);
    }
}
