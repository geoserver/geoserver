/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.media.jai.PlanarImage;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.EnvelopeCompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageView.SelectedResolution;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.grid.io.footprint.FootprintBehavior;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CoverageViewTest extends GeoServerSystemTestSupport {

    private static final String RGB_IR_VIEW = "RgbIrView";
    private static final String S2_REDUCED_VIEW = "s2reduced_view";
    private static final String BANDS_FLAGS_VIEW = "BandsFlagsView";
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static QName S2REDUCED = new QName(MockData.SF_URI, "s2reduced", MockData.SF_PREFIX);
    protected static QName IR_RGB = new QName(MockData.SF_URI, "ir-rgb", MockData.SF_PREFIX);
    protected static QName BANDS_FLAGS =
            new QName(MockData.SF_URI, "bands-flags", MockData.SF_PREFIX);

    @Before
    public void cleanupCatalog() {
        // attempt to solve intermittend build failure
        getGeoServer().reset();
    }

    static CoordinateReferenceSystem UTM32N;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
        testData.setUpRasterLayer(S2REDUCED, "s2reduced.zip", null, null, TestData.class);
        testData.setUpRasterLayer(IR_RGB, "ir-rgb.zip", null, null, TestData.class);
        testData.setUpRasterLayer(BANDS_FLAGS, "bands-flags.zip", null, null, TestData.class);

        UTM32N = CRS.decode("EPSG:32632", true);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup the coverage view
        final Catalog cat = getCatalog();
        configureIROnCatalog(cat);
        configureBandsFlagsOnCatalog(cat);
    }

    private void configureIROnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        coverageInfo.getDimensions().get(0).setName("Red");
        coverageInfo.getDimensions().get(1).setName("Green");
        coverageInfo.getDimensions().get(2).setName("Blue");
        coverageInfo.getDimensions().get(3).setName("Infrared");
        cat.add(coverageInfo);
    }

    private void configureBandsFlagsOnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("bands-flags");
        final CoverageView coverageView = buildBandsFlagsView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(BANDS_FLAGS_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
    }

    private CoverageView buildRgbIRView() {
        final CoverageBand rBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "0")),
                        "rband",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageBand gBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "1")),
                        "gband",
                        1,
                        CompositionType.BAND_SELECT);
        final CoverageBand bBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "2")),
                        "bband",
                        2,
                        CompositionType.BAND_SELECT);
        final CoverageBand irBand =
                new CoverageBand(
                        Collections.singletonList(new InputCoverageBand("ir", "0")),
                        "irband",
                        3,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView(RGB_IR_VIEW, Arrays.asList(rBand, gBand, bBand, irBand));
        // old coverage views deserialize with null in these fields, force it to test backwards
        // compatibility
        coverageView.setEnvelopeCompositionType(null);
        coverageView.setSelectedResolution(null);
        return coverageView;
    }

    private CoverageView buildBandsFlagsView() {
        String[] sources =
                new String[] {
                    "SWIR",
                    "VNIR",
                    "QUALITY_CLASSES",
                    "QUALITY_CLOUD",
                    "QUALITY_CLOUDSHADOW",
                    "QUALITY_HAZE",
                    "QUALITY_SNOW"
                };

        List<CoverageBand> bands = new ArrayList<>();
        for (String source : sources) {
            if (source.startsWith("QUALITY_")) {
                CoverageBand band =
                        new CoverageBand(
                                Arrays.asList(new InputCoverageBand(source, "0")),
                                source,
                                0,
                                CompositionType.BAND_SELECT);
                bands.add(band);
            } else {
                for (int i = 0; i < 3; i++) {
                    CoverageBand band =
                            new CoverageBand(
                                    Arrays.asList(new InputCoverageBand(source, "" + i)),
                                    source + "_" + i,
                                    i,
                                    CompositionType.BAND_SELECT);
                    bands.add(band);
                }
            }
        }

        final CoverageView coverageView = new CoverageView(BANDS_FLAGS_VIEW, bands);
        return coverageView;
    }

    @Test
    public void testPreserveCoverageBandNames() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        List<CoverageDimensionInfo> dimensions = coverageInfo.getDimensions();
        assertEquals("rband", dimensions.get(0).getName());
        assertEquals("gband", dimensions.get(1).getName());
        assertEquals("bband", dimensions.get(2).getName());
        assertEquals("irband", dimensions.get(3).getName());
    }

    /** */
    @Test
    public void testCoverageView() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("watertemp");

        final InputCoverageBand band = new InputCoverageBand("watertemp", "0");
        final CoverageBand outputBand =
                new CoverageBand(
                        Collections.singletonList(band),
                        "watertemp@0",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView("waterView", Collections.singletonList(outputBand));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo("waterView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        final MetadataMap metadata = coverageInfo.getMetadata();
        final CoverageView metadataCoverageView =
                (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
        assertEquals(metadataCoverageView, coverageView);

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage =
                resPool.getGridCoverage(coverageInfo, "waterView", bbox, null);
        assertEquals(coverage.getNumSampleDimensions(), 1);

        disposeCoverage(coverage);
        final GridCoverageReader reader = resPool.getGridCoverageReader(coverageInfo, null);
        reader.dispose();
    }

    private void disposeCoverage(GridCoverage coverage) {
        RenderedImage ri = coverage.getRenderedImage();
        if (coverage instanceof GridCoverage2D) {
            ((GridCoverage2D) coverage).dispose(true);
        }
        if (ri instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
        }
    }

    /** */
    @Test
    public void testBands() throws Exception {

        // Test input bands
        final InputCoverageBand u = new InputCoverageBand("u-component", "0");
        final InputCoverageBand v = new InputCoverageBand("u-component", "0");
        assertEquals(u, v);

        final InputCoverageBand empty = new InputCoverageBand();
        v.setCoverageName("v-component");
        v.setBand("1");
        assertNotEquals(u, v);
        assertNotEquals(u, empty);

        // Test output bands
        final CoverageBand outputBandU =
                new CoverageBand(
                        Collections.singletonList(u), "u@1", 0, CompositionType.BAND_SELECT);

        final CoverageBand outputBandV = new CoverageBand();
        outputBandV.setInputCoverageBands(Collections.singletonList(v));
        outputBandV.setDefinition("v@0");
        outputBandV.setIndex(1);
        outputBandV.setCompositionType(CompositionType.BAND_SELECT);

        assertNotEquals(outputBandU, outputBandV);

        // Test compositions
        CompositionType defaultComposition = CompositionType.getDefault();
        assertEquals("Band Selection", defaultComposition.displayValue());
        assertEquals("BAND_SELECT", defaultComposition.toValue());
        assertEquals(outputBandU.getCompositionType(), defaultComposition);

        // Test coverage views
        final List<CoverageBand> bands = new ArrayList<CoverageBand>();
        bands.add(outputBandU);
        bands.add(outputBandV);

        final CoverageView coverageView = new CoverageView("wind", bands);
        final CoverageView sameViewDifferentName = new CoverageView();
        sameViewDifferentName.setName("winds");
        sameViewDifferentName.setCoverageBands(bands);
        assertNotEquals(coverageView, sameViewDifferentName);

        assertEquals(coverageView.getBand(1), outputBandV);
        assertEquals(outputBandU, coverageView.getBands("u-component").get(0));
        assertEquals(2, coverageView.getSize());
        assertEquals(2, coverageView.getCoverageBands().size());
        assertEquals("wind", coverageView.getName());
    }

    @Test
    public void testRGBIrToRGB() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // no transparency due to footprint
        GeneralParameterValue[] params =
                buildFootprintBandParams(FootprintBehavior.None, new int[] {0, 1, 2});
        GridCoverage solidCoverage = reader.read(params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Red", "Green", "Blue");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {0, 1, 2});
        GridCoverage txCoverage = reader.read(params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Red", "Green", "Blue", "ALPHA_BAND");
        } finally {
            disposeCoverage(solidCoverage);
        }
    }

    @Test
    public void testRGBIrToIr() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params =
                buildFootprintBandParams(FootprintBehavior.None, new int[] {3});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Infrared");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {3});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Infrared", "ALPHA_BAND");
        } finally {
            disposeCoverage(solidCoverage);
        }
    }

    @Test
    public void testRGBIrToIrGB() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params =
                buildFootprintBandParams(FootprintBehavior.None, new int[] {3, 1, 2});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Infrared", "Green", "Blue");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {3, 1, 2});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Infrared", "Green", "Blue", "ALPHA_BAND");
        } finally {
            disposeCoverage(solidCoverage);
        }
    }

    @Test
    public void testRGBIrToRed() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params =
                buildFootprintBandParams(FootprintBehavior.None, new int[] {0});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Red");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {0});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Red", "ALPHA_BAND");
        } finally {
            disposeCoverage(solidCoverage);
        }
    }

    private void assertBandNames(GridCoverage coverage, String... bandNames) {
        assertEquals(bandNames.length, coverage.getNumSampleDimensions());
        for (int i = 0; i < bandNames.length; i++) {
            String expectedName = bandNames[i];
            String actualName = coverage.getSampleDimension(i).getDescription().toString();
            assertEquals(expectedName, actualName);
        }
    }

    private GeneralParameterValue[] buildFootprintBandParams(
            FootprintBehavior footprintBehavior, int[] bands) {
        final List<ParameterValue<?>> parameters = new ArrayList<ParameterValue<?>>();
        parameters.add(
                new DefaultParameterDescriptor<>(
                                AbstractGridFormat.FOOTPRINT_BEHAVIOR.getName().toString(),
                                AbstractGridFormat.FOOTPRINT_BEHAVIOR.getValueClass(),
                                null,
                                footprintBehavior.name())
                        .createValue());
        parameters.add(
                new DefaultParameterDescriptor<>(
                                AbstractGridFormat.BANDS.getName().toString(),
                                AbstractGridFormat.BANDS.getValueClass(),
                                null,
                                bands)
                        .createValue());
        return (GeneralParameterValue[])
                parameters.toArray(new GeneralParameterValue[parameters.size()]);
    }

    /**
     * Tests a heterogeneous view without setting any extra configuration (falling back on defaults)
     */
    @Test
    public void testHeterogeneousViewDefaults() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsDefaults",
                        cv -> {},
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");
        GridCoverage2D coverage = null;
        try {
            // default resolution policy is "best"
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(1007, reader.getResolutionLevels()[0][0], 1);
            assertEquals(1007, reader.getResolutionLevels()[0][1], 1);

            // default envelope policy is "union"
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            assertEquals(399960, envelope.getMinimum(0), 1);
            assertEquals(5190240, envelope.getMinimum(1), 1);
            assertEquals(509760, envelope.getMaximum(0), 1);
            assertEquals(5300040, envelope.getMaximum(1), 1);

            // read the full coverage to verify it's consistent
            coverage = reader.read(null);
            assertCoverageResolution(coverage, 1007, 1007);

            assertEquals(coverage.getEnvelope(), envelope);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    /**
     * Tests a heterogeneous view without setting any extra configuration (falling back on defaults)
     */
    @Test
    public void testHeterogeneousViewIntersectionEnvelope() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsIntersection",
                        cv -> {
                            cv.setEnvelopeCompositionType(EnvelopeCompositionType.INTERSECTION);
                        },
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");
        GridCoverage2D coverage = null;
        try {
            // default resolution policy is "best"
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(1007, reader.getResolutionLevels()[0][0], 1);
            assertEquals(1007, reader.getResolutionLevels()[0][1], 1);

            // one of the granules has been cut to get a tigheter envelope
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            assertEquals(399960, envelope.getMinimum(0), 1);
            assertEquals(5192273, envelope.getMinimum(1), 1);
            assertEquals(507726, envelope.getMaximum(0), 1);
            assertEquals(5300040, envelope.getMaximum(1), 1);

            // checking the coverage it's not particularly useful as it does not get cut,
            // the bounds are just metadata
            coverage = reader.read(null);
            assertCoverageResolution(coverage, 1007, 1007);
            Envelope coverageEnvelope = coverage.getEnvelope();
            assertEquals(399960, coverageEnvelope.getMinimum(0), 1);
            assertEquals(5190240, coverageEnvelope.getMinimum(1), 1);
            assertEquals(509760, coverageEnvelope.getMaximum(0), 1);
            assertEquals(5300040, coverageEnvelope.getMaximum(1), 1);

        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    @Test
    public void testHeterogeneousViewResolutionLowest() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsLowest",
                        cv -> {
                            cv.setSelectedResolution(SelectedResolution.WORST);
                        },
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");
        GridCoverage2D coverage = null;
        try {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(6100, reader.getResolutionLevels()[0][0], 1);
            assertEquals(6100, reader.getResolutionLevels()[0][1], 1);

            // no point checking the coverage, this is again just metadata, just smoke testing
            // the read will work
            coverage = reader.read(null);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    private void assertCoverageResolution(GridCoverage2D coverage, double resX, double resY) {
        AffineTransform2D mt = (AffineTransform2D) coverage.getGridGeometry().getGridToCRS2D();
        assertEquals(resX, mt.getScaleX(), 1);
        assertEquals(resY, Math.abs(mt.getScaleY()), 1);
    }

    /** Hit the view outside its bounds, should return null */
    @Test
    public void testHeterogeneousViewOutsideBounds() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsOutsideBounds",
                        cv -> {},
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");
        GridCoverage2D coverage = null;
        try {
            ParameterValue<GridGeometry2D> gg =
                    AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            gg.setValue(
                    new GridGeometry2D(
                            new GridEnvelope2D(0, 0, 10, 10),
                            new ReferencedEnvelope(0, 1000, 0, 1000, UTM32N)));
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            coverage = reader.read(new GeneralParameterValue[] {gg});
            assertNull(coverage);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    @Test
    public void testHeterogeneousViewBandSelectionBestResolution() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsBest",
                        cv -> {
                            // use the default: BEST
                        },
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");

        // check band resolutions with specific band selections
        checkBandSelectionResolution(info, new int[] {0}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {0, 5}, 2033, 2033);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {1, 8, 5}, 1007, 1007);
    }

    @Test
    public void testHeterogeneousViewBandSelectionWorstResolution() throws Exception {
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        "s2AllBandsWorst",
                        cv -> {
                            cv.setSelectedResolution(SelectedResolution.WORST);
                        },
                        "B01",
                        "B02",
                        "B03",
                        "B04",
                        "B05",
                        "B06",
                        "B07",
                        "B08",
                        "B09",
                        "B10",
                        "B11",
                        "B12");

        // check band resolutions with specific band selections
        checkBandSelectionResolution(info, new int[] {0}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 5}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {1, 5}, 2033, 2033);
    }

    public void checkBandSelectionResolution(
            CoverageInfo info, int[] bands, double expectedResolutionX, double expectedResolutionY)
            throws IOException {
        GridCoverage2D coverage = null;
        try {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader) info.getGridCoverageReader(null, null);

            ParameterValue<int[]> bandsValue = AbstractGridFormat.BANDS.createValue();
            bandsValue.setValue(bands);
            coverage = reader.read(new GeneralParameterValue[] {bandsValue});
            assertNotNull(coverage);
            assertCoverageResolution(coverage, expectedResolutionX, expectedResolutionY);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    private CoverageInfo buildHeterogeneousResolutionView(
            String name, Consumer<CoverageView> viewCustomizer, String... coverageNames)
            throws Exception {
        List<CoverageBand> bands = new ArrayList<>();
        int bandIdx = 0;
        for (String coverageName : coverageNames) {
            CoverageBand band =
                    new CoverageBand(
                            Arrays.asList(new InputCoverageBand(coverageName, "0")),
                            coverageName,
                            bandIdx++,
                            CompositionType.BAND_SELECT);
            bands.add(band);
        }
        final CoverageView coverageView = new CoverageView(name, bands);
        viewCustomizer.accept(coverageView);

        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("s2reduced");
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(name, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);

        return cat.getCoverage(coverageInfo.getId());
    }

    @Test
    public void testCoverageViewGranuleSource() throws Exception {
        final String VIEW_NAME = "view";
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        VIEW_NAME,
                        cv -> {
                            cv.setSelectedResolution(SelectedResolution.BEST);
                        },
                        "B02",
                        "B03",
                        "B04");

        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) info.getGridCoverageReader(null, null);
        GranuleSource source = reader.getGranules(VIEW_NAME, true);
        Query query = new Query(VIEW_NAME);
        // used to throw exception here
        SimpleFeatureCollection granules = source.getGranules(query);
        // just check we can pull data from it
        DataUtilities.first(granules);
        // there are three bands, so three granules making up the coverage
        assertEquals(3, granules.size());
    }

    @Test
    public void testCoverageViewGranuleSourceAggregation() throws Exception {
        final String VIEW_NAME = "viewAggregate";
        CoverageInfo info =
                buildHeterogeneousResolutionView(
                        VIEW_NAME,
                        cv -> {
                            cv.setSelectedResolution(SelectedResolution.BEST);
                        },
                        "B02",
                        "B03",
                        "B04",
                        "B01");

        StructuredGridCoverage2DReader reader =
                (StructuredGridCoverage2DReader) info.getGridCoverageReader(null, null);
        GranuleSource source = reader.getGranules(VIEW_NAME, true);
        Query query = new Query(VIEW_NAME);
        // used to throw exception here
        SimpleFeatureCollection granules = source.getGranules(query);
        MinVisitor visitor = new MinVisitor("location");
        granules.accepts(visitor, null);
        assertEquals("20170410T103021026Z_fullres_CC2.1989_T32TMT_B01.tif", visitor.getMin());
    }

    @Test
    public void testBandsFlagsView() throws Exception {
        // creation in the setup would have failed before the fix for
        // [GEOT-6168] CoverageView setup fails if one of the source bands has an indexed color
        // model

        CoverageInfo info = getCatalog().getCoverageByName(BANDS_FLAGS_VIEW);
        GridCoverageReader reader = info.getGridCoverageReader(null, null);
        GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
        assertEquals(11, coverage.getRenderedImage().getSampleModel().getNumBands());
        coverage.dispose(true);
    }
}
