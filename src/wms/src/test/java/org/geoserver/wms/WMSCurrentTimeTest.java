/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.io.DefaultFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Tests the WMS "current" value resolver for TIME dimension for vector and raster layers by adding some objects having the TIME values in the future.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class WMSCurrentTimeTest extends WMSTestSupport {

    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd",
            MockData.SF_PREFIX);

    static final QName WATTEMP_FUTURE = new QName(MockData.SF_URI, "watertemp_future_generated",
            MockData.SF_PREFIX);

    WMS wms;

    private CoverageInfo timeWithStartEnd;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        testData.addVectorLayer(TIME_WITH_START_END, Collections.EMPTY_MAP,
                "TimeElevationWithStartEnd.properties", getClass(), getCatalog());
        setupFeatureTimeDimension("startTime", "endTime");

        prepareFutureCoverageData(WATTEMP_FUTURE);
        setupCoverageTimeDimension(WATTEMP_FUTURE);
    }

    @Before
    public void setWMS() throws Exception {
        wms = new WMS(getGeoServer());
    }

    @Test
    public void testVectorCurrentTimeSelector() throws Exception {
        int fid = 1000;
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance();

        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());

        // Set currentTimeSelection to "latest":
        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "latest");

        // Add a feature with time in the past:
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 2);
        Date twoDaysAgo = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, twoDaysAgo, null, 0, 0);

        // The selection strategy should not affect times in the past:

        java.util.Date d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the latest", d.getTime() == twoDaysAgo.getTime());

        // Set currentTimeSelection to "nearest":
        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "nearest");

        d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the latest", d.getTime() == twoDaysAgo.getTime());

        // Add some features with future times:

        // Set to midnight today (test data feature type somehow cuts to startTime to full days):
        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));

        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
        Date oneDayInFuture = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, oneDayInFuture, null, 0, 0);

        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        Date oneYearInFuture = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, oneYearInFuture, null, 0, 0);

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "latest");
        d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the furthest in the future", d.getTime() == oneYearInFuture
                .getTime());

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "nearest");
        d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the closest to the current time",
                d.getTime() == oneDayInFuture.getTime());

        // Add a new feature in the past, but very close to current time (today midnight):
        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        Date todayMidnight = new Date(cal.getTimeInMillis());
        this.addFeature(fid++, todayMidnight, null, 0, 0);

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "latest");
        d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the furthest in the future", d.getTime() == oneYearInFuture
                .getTime());

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "nearest");
        d = wms.getCurrentTime(timeWithStartEnd);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the closest in the past", d.getTime() == todayMidnight
                .getTime());
    }

    @Test
    public void testCoverageCurrentTimeSelector() throws Exception {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        long todayMidnight = cal.getTimeInMillis();

        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        long oneYearInFuture = cal.getTimeInMillis();

        CoverageInfo coverage = getCatalog().getCoverageByName(WATTEMP_FUTURE.getLocalPart());
        GridCoverage2DReader reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null,
                null);
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "latest");
        java.util.Date d = wms.getCurrentTime(coverage, dimensions);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the furthest in the future", d.getTime() == oneYearInFuture);

        System.setProperty("org.geoserver.wms.currentTimeSelectionStrategy", "nearest");
        d = wms.getCurrentTime(coverage, dimensions);
        assertTrue("Returns a valid current time", d != null);
        assertTrue("The current time is the closest in the past", d.getTime() == todayMidnight);

    }

    protected void setupFeatureTimeDimension(String start, String end) {
        FeatureTypeInfo info = getCatalog()
                .getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.TIME, di);
        getCatalog().save(info);
    }

    protected void setupCoverageTimeDimension(QName name) {
        CoverageInfo info = getCatalog().getCoverageByName(name.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.TIME, di);
        getCatalog().save(info);
    }

    protected void addFeature(int id, Date startTime, Date endTime, double startElevation,
            double endElevation) throws IOException {
        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                TIME_WITH_START_END.getLocalPart());
        FeatureStore fs = (FeatureStore) timeWithStartEnd.getFeatureSource(null, null);
        SimpleFeatureType type = (SimpleFeatureType) timeWithStartEnd.getFeatureType();
        MemoryFeatureCollection coll = new MemoryFeatureCollection(type);
        StringBuffer content = new StringBuffer();
        content.append(id);
        content.append('|');
        content.append(startTime.toString());
        content.append('|');
        if (endTime != null){
            content.append(endTime.toString());
        }
        content.append('|');
        content.append(startElevation);
        content.append('|');
        content.append(endElevation);
        
        SimpleFeature f = DataUtilities.createFeature(type, content.toString());
        coll.add(f);
        org.geotools.data.Transaction tx = fs.getTransaction();
        fs.addFeatures(coll);
        tx.commit();
    }

    private void prepareFutureCoverageData(QName coverageName) throws IOException {
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
        GeoServerResourceLoader loader = getCatalog().getResourceLoader();
        File targetDir = loader.createDirectory(getDataDirectory().root(), coverageName.getPrefix()
                + File.separator + coverageName.getLocalPart());
        File target = new File(targetDir, coverageName.getLocalPart() + ".zip");
        loader.copyFromClassPath("org/geoserver/wms/watertemp.zip", target);

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
                output = new File(targetDir, "DUMMY_watertemp_000_"
                        + tsFormatter.format(new Date(justPast)) + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);

                output = new File(targetDir, "DUMMY_watertemp_000_"
                        + tsFormatter.format(new Date(oneMonthInFuture)) + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);

                output = new File(targetDir, "DUMMY_watertemp_000_"
                        + tsFormatter.format(new Date(oneYearInFuture)) + "T0000000_12.tiff");
                FileUtils.copyFile(input, output);
            }
        }
        addRasterLayerFromDataDir(WATTEMP_FUTURE, getCatalog());

    }

    /*
     * This method is necessary here, because SystemTestData#addRasterLayer assumes that the raster data file is somewhere in the classpath and should
     * be copied to the data directory before registering the new layer. This method skips the copy and extract and assumes that the coverage data is
     * already in contained in the data directory.
     */
    private void addRasterLayerFromDataDir(QName qName, Catalog catalog) throws IOException {
        String prefix = qName.getPrefix();
        String name = qName.getLocalPart();

        // setup the data
        File file = new File(this.getDataDirectory().root() + File.separator + prefix, name);

        if (!file.exists()) {
            throw new IllegalArgumentException("There is no file with name '" + prefix
                    + File.separator + name + "' in the data directory");
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
                throw new RuntimeException("No reader for " + file.getCanonicalPath()
                        + " with format " + format.getName());
            }

            // configure workspace if it doesn't already exist
            if (catalog.getWorkspaceByName(prefix) == null) {
                ((SystemTestData) this.testData).addWorkspace(prefix, qName.getNamespaceURI(),
                        catalog);
            }
            // create the store
            CoverageStoreInfo store = catalog.getCoverageStoreByName(prefix, name);
            if (store == null) {
                store = catalog.getFactory().createCoverageStore();
            }

            store.setName(name);
            store.setWorkspace(catalog.getWorkspaceByName(prefix));
            store.setEnabled(true);
            store.setURL(DataUtilities.fileToURL(file).toString());
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
                            .put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode(),
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
            layer.setType(LayerInfo.Type.RASTER);
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
