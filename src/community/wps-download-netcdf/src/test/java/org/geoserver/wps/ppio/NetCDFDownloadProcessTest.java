/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.AbstractNetCDFEncoder;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.download.DownloadEstimatorProcess;
import org.geoserver.wps.gs.download.DownloadProcess;
import org.geoserver.wps.gs.download.DownloadProcessTest;
import org.geoserver.wps.gs.download.Parameter;
import org.geoserver.wps.gs.download.Parameters;
import org.geoserver.wps.gs.download.StaticDownloadServiceConfiguration;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.util.NullProgressListener;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

public class NetCDFDownloadProcessTest extends WPSTestSupport {

    private static QName TIMESERIES = new QName(WCS_URI, "timeseries", WCS_PREFIX);
    private static final FilterFactory FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
        setupRasterDimension(TIMESERIES, ResourceInfo.TIME, DimensionPresentation.LIST, null, "ISO8601", null);
    }

    protected WPSResourceManager getResourceManager() {
        return GeoServerExtensions.bean(WPSResourceManager.class);
    }

    public static class AutoDisposableNetCDFReader extends NetCDFReader implements AutoCloseable {

        public AutoDisposableNetCDFReader(File file) throws DataSourceException {
            super(file, null);
        }

        @Override
        public void close() {
            dispose();
        }

        public AutoDisposableGridCoverage2D read() throws IOException {
            GridCoverage2D gc = super.read(null);
            assertNotNull(gc);
            return new AutoDisposableGridCoverage2D("", gc);
        }
    }

    private DownloadProcess createDefaultTestingDownloadProcess(WPSResourceManager resourceManager) {
        GeoServer geoserver = getGeoServer();
        DownloadEstimatorProcess limits =
                new DownloadEstimatorProcess(new StaticDownloadServiceConfiguration(), geoserver);
        return new DownloadProcess(geoserver, limits, resourceManager);
    }

    /**
     * Test download of spatio temporal raster data using a time filter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadSpatioTemporalRaster() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        Filter filter = FF.and(
                FF.before(FF.property("time"), FF.literal("2018-01-01")),
                FF.after(FF.property("time"), FF.literal("2014-01-01")));

        RawData raster = downloadProcess.execute(
                getLayerId(TIMESERIES), // layerName
                filter, // filter
                "application/x-netcdf", // outputFormat
                "application/x-netcdf",
                null, // targetCRS
                null, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                null, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (DownloadProcessTest.AutoCloseableResource resource =
                        new DownloadProcessTest.AutoCloseableResource(resourceManager, raster);
                AutoDisposableNetCDFReader reader = new AutoDisposableNetCDFReader(resource.getFile());
                AutoDisposableGridCoverage2D gc = reader.read()) {
            String timeDomain = reader.getMetadataValue(reader.getGridCoverageNames()[0], "TIME_DOMAIN");
            assertEquals(
                    "2015-01-01T00:00:00.000Z/2015-01-01T00:00:00.000Z,2016-01-01T00:00:00.000Z/2016-01-01T00:00:00.000Z,2017-01-01T00:00:00.000Z/2017-01-01T00:00:00.000Z",
                    timeDomain);
            assertNotNull(gc);
            RenderedImage ri = gc.getRenderedImage();
            assertEquals(25, ri.getHeight());
            assertEquals(25, ri.getWidth());
            assertExpectedExtension(raster, "nc");
        }
    }

    /**
     * Test download of spatio temporal raster data, providing writing params.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDownloadSpatioTemporalRasterWriteParams() throws Exception {
        final WPSResourceManager resourceManager = getResourceManager();

        // Creates the new process for the download
        DownloadProcess downloadProcess = createDefaultTestingDownloadProcess(resourceManager);
        Filter filter = FF.and(
                FF.before(FF.property("time"), FF.literal("2018-01-01")),
                FF.after(FF.property("time"), FF.literal("2014-01-01")));

        Parameters parameters = new Parameters();
        List<Parameter> parametersList = parameters.getParameters();
        final String dataPacking = "SHORT";
        final String variableName = "testingTemperature";
        final String uom = "K";

        parametersList.add(new Parameter(AbstractNetCDFEncoder.VARIABLE_NAME_KEY, variableName));
        parametersList.add(new Parameter(AbstractNetCDFEncoder.DATA_PACKING_KEY, dataPacking));
        parametersList.add(new Parameter(AbstractNetCDFEncoder.UOM_KEY, uom));

        RawData raster = downloadProcess.execute(
                getLayerId(TIMESERIES), // layerName
                filter, // filter
                "application/x-netcdf", // outputFormat
                "application/x-netcdf",
                null, // targetCRS
                null, // roiCRS
                null, // roi
                false, // cropToGeometry
                null, // interpolation
                null, // targetSizeX
                null, // targetSizeY
                null, // bandSelectIndices
                parameters, // Writing params
                false,
                false,
                0d,
                null,
                new NullProgressListener() // progressListener
                );

        try (DownloadProcessTest.AutoCloseableResource resource =
                new DownloadProcessTest.AutoCloseableResource(resourceManager, raster)) {
            File file = resource.getFile();
            DatasetUrl url = DatasetUrl.findDatasetUrl(file.getAbsolutePath());
            try (NetcdfDataset dataset = NetcdfDatasets.acquireDataset(url, null)) {

                Dimension dimension = dataset.findDimension("time");
                assertNotNull(dimension);

                // check that the variable name write param has been used
                Variable var = dataset.findVariable(variableName);
                assertNotNull(var);

                // DataPacking check
                DataType dataType = var.getDataType();
                assertEquals(DataType.SHORT, dataType);
                Attribute scaleFactor = var.findAttribute("scale_factor");
                assertNotNull(scaleFactor);
                assertTrue(scaleFactor.getNumericValue().doubleValue() > 0);
                Attribute addOffset = var.findAttribute("add_offset");
                assertNotNull(addOffset);
                assertTrue(addOffset.getNumericValue().doubleValue() > 10);

                // Check the unit of measure write param has been used
                assertEquals("K", var.getUnitsString());
            }
        }
    }

    private void assertExpectedExtension(RawData raster, String extension) {
        assertEquals(
                "org.geoserver.wps.process.ResourceRawData", raster.getClass().getName());
        ResourceRawData rrd = (ResourceRawData) raster;
        assertEquals(extension, rrd.getFileExtension());
    }
}
