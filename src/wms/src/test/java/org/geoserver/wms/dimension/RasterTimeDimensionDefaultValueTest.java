/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.DateUtil;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.util.DefaultFileFilter;
import org.geotools.util.Range;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the WMS default value support for TIME dimension raster layers.
 *
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class RasterTimeDimensionDefaultValueTest extends WMSDimensionsTestSupport {

    static final QName WATTEMP_FUTURE =
            new QName(MockData.SF_URI, "watertemp_future_generated", MockData.SF_PREFIX);

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        prepareFutureCoverageData(WATTEMP_FUTURE, this.getDataDirectory(), this.getCatalog());
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); // with the initialized application context
    }

    @Test
    public void testDefaultTimeCoverageSelector() throws Exception {
        // Use default default value strategy:
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, null);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        long todayMidnight = cal.getTimeInMillis();

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == todayMidnight);
    }

    @Test
    public void testExplicitCurrentTimeCoverageSelector() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(DimensionDefaultValueSetting.TIME_CURRENT);
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        long todayMidnight = cal.getTimeInMillis();

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == todayMidnight);
    }

    @Test
    public void testFixedTimeRange() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("P1M/PRESENT");
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        // the default is a single value, as we get the nearest to the range
        java.util.Date curr = new java.util.Date();
        Range d = (Range) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default range", d != null);
        // check "now" it's in the same minute... should work for even the slowest build server
        assertDateEquals(curr, (java.util.Date) d.getMaxValue(), MILLIS_IN_MINUTE);
        // the beginning
        assertDateEquals(
                new Date(curr.getTime() - 30l * MILLIS_IN_DAY),
                (java.util.Date) d.getMinValue(),
                60000);
    }

    @Test
    public void testExplicitMinTimeCoverageSelector() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        // From src/test/resources/org/geoserver/wms/watertemp.zip:
        Date expected = Date.valueOf("2008-10-31");

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the smallest one", d.getTime() == expected.getTime());
    }

    @Test
    public void testExplicitMaxTimeCoverageSelector() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        // This is what the test data setup does, and it makes a difference at the
        // end of the month (e.g. 29 Jan)
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        long oneYearInFuture = cal.getTimeInMillis();

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the biggest one", d.getTime() == oneYearInFuture);
    }

    @Test
    public void testExplicitFixedTimeCoverageSelector() throws Exception {
        String fixedTimeStr = "2012-06-01T03:00:00.000Z";
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedTimeStr);
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        long fixedTime = DateUtil.parseDateTime(fixedTimeStr);

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the fixed one", d.getTime() == fixedTime);
    }

    @Test
    public void testExplicitNearestToGivenTimeCoverageSelector() throws Exception {
        String preferredTimeStr = "2009-01-01T00:00:00.000Z";
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(preferredTimeStr);
        setupResourceDimensionDefaultValue(WATTEMP_FUTURE, ResourceInfo.TIME, defaultValueSetting);

        // From src/test/resources/org/geoserver/wms/watertemp.zip:
        Date expected = Date.valueOf("2008-11-01");

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());

        java.util.Date d = (java.util.Date) wms.getDefaultTime(coverage);
        assertTrue("Returns a valid Default time", d != null);
        assertTrue("Default time should be the closest one", d.getTime() == expected.getTime());
    }

    public static void prepareFutureCoverageData(
            QName coverageName, GeoServerDataDirectory dataDirectory, Catalog catalog)
            throws IOException {
        SimpleDateFormat tsFormatter = new SimpleDateFormat("yyyyMMdd");

        // Prepare the target dates for the dummy coverages to be created
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        long justPast = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
        long oneMonthInFuture = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));

        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        long oneYearInFuture = cal.getTimeInMillis();

        // Copy watertemp.zip test coverage resource in the data dir under a different name:
        GeoServerResourceLoader loader = catalog.getResourceLoader();
        File targetDir =
                loader.createDirectory(
                        dataDirectory.root(),
                        coverageName.getPrefix() + File.separator + coverageName.getLocalPart());
        File target = new File(targetDir, coverageName.getLocalPart() + ".zip");
        loader.copyFromClassPath("org/geoserver/wms/dimension/watertemp.zip", target);

        // unpack the archive
        IOUtils.decompress(target, targetDir);

        // delete archive
        target.delete();

        // Make three new dummy coverage files with the needed timestamps:
        File input = null;
        File output = null;
        FilenameFilter tiffFilter = new DefaultFileFilter("*.tiff");
        String[] tiffnames = targetDir.list(tiffFilter);

        if (tiffnames != null) {
            if (tiffnames.length > 0) {
                input = new File(targetDir, tiffnames[0]);
                output =
                        new File(
                                targetDir,
                                "DUMMY_watertemp_000_"
                                        + tsFormatter.format(new Date(justPast))
                                        + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);

                output =
                        new File(
                                targetDir,
                                "DUMMY_watertemp_000_"
                                        + tsFormatter.format(new Date(oneMonthInFuture))
                                        + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);

                output =
                        new File(
                                targetDir,
                                "DUMMY_watertemp_000_"
                                        + tsFormatter.format(new Date(oneYearInFuture))
                                        + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);
            }
        }
        addRasterLayerFromDataDir(WATTEMP_FUTURE, dataDirectory, catalog);
    }

    /*
     * This method is necessary here, because SystemTestData#addRasterLayer assumes that the raster data file is somewhere in the classpath and should
     * be copied to the data directory before registering the new layer. This method skips the copy and extract and assumes that the coverage data is
     * already in contained in the data directory.
     */
    private static void addRasterLayerFromDataDir(
            QName qName, GeoServerDataDirectory dataDirectory, Catalog catalog) throws IOException {
        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();

        // setup the data
        File file = new File(dataDirectory.root() + File.separator + prefix, name);

        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "There is no file with name '"
                            + prefix
                            + File.separator
                            + name
                            + "' in the data directory");
        }

        // load the format/reader
        AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(file);
        if (format == null) {
            throw new RuntimeException("No format for " + file.getCanonicalPath());
        }
        GridCoverage2DReader reader = null;
        try {
            reader = (GridCoverage2DReader) format.getReader(file);
            if (reader == null) {
                throw new RuntimeException(
                        "No reader for "
                                + file.getCanonicalPath()
                                + " with format "
                                + format.getName());
            }

            // configure workspace if it doesn't already exist
            if (catalog.getWorkspaceByName(prefix) == null) {
                ((SystemTestData) testData).addWorkspace(prefix, qName.getNamespaceURI(), catalog);
            }
            // create the store
            CoverageStoreInfo store = catalog.getCoverageStoreByName(prefix, name);
            if (store == null) {
                store = catalog.getFactory().createCoverageStore();
            }

            store.setName(name);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);
            store.setURL(URLs.fileToUrl(file).toString());
            store.setType(format.getName());

            if (store.getId() == null) {
                catalog.add(store);
            } else {
                catalog.save(store);
            }

            // create the coverage
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(store);

            CoverageInfo coverage = null;

            try {

                coverage = builder.buildCoverage(reader, null);
                // coverage read params
                if (format instanceof ImageMosaicFormat) {
                    // make sure we work in immediate mode
                    coverage.getParameters()
                            .put(
                                    AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode(),
                                    Boolean.FALSE);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }

            coverage.setName(name);
            coverage.setTitle(name);
            coverage.setDescription(name);
            coverage.setEnabled(true);

            CoverageInfo cov = catalog.getCoverageByCoverageStore(store, name);
            if (cov == null) {
                catalog.add(coverage);
            } else {
                builder.updateCoverage(cov, coverage);
                catalog.save(cov);
                coverage = cov;
            }

            LayerInfo layer = catalog.getLayerByName(new NameImpl(qName));
            if (layer == null) {
                layer = catalog.getFactory().createLayer();
            }
            layer.setResource(coverage);

            layer.setDefaultStyle(catalog.getStyleByName(SystemTestData.DEFAULT_RASTER_STYLE));
            layer.setType(PublishedType.RASTER);
            layer.setEnabled(true);

            if (layer.getId() == null) {
                catalog.add(layer);
            } else {
                catalog.save(layer);
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }
}
