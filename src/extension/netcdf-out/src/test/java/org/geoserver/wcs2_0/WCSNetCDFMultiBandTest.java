/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.BandSetting;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.VariableAttribute;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * Multi-band → multi-variable WCS NetCDF output tests.
 *
 * <p>Exercises the per-band output path that {@link org.geoserver.wcs.responses.DefaultNetCDFEncoder} takes when the
 * source coverage has more than one sample dimension — typically a {@link CoverageView} with
 * {@link CompositionType#BAND_SELECT BAND_SELECT} bands. Two scenarios are covered:
 *
 * <ol>
 *   <li>{@link #testMultiBandDefault default behavior}, with no per-band overrides configured — the encoder writes one
 *       output variable per band, named after the band's sample dimension description (which equals the
 *       {@code BAND_SELECT} {@code <definition>} value).
 *   <li>{@link #testMultiBandWithBandSettings explicit BandSetting overrides}, where the layer's NetCDF Output
 *       configuration carries a {@code bandSettings} list — the encoder uses each entry's {@code name}, {@code uom},
 *       and {@code variableAttributes} to override the auto-derived defaults.
 * </ol>
 *
 * <p>The single-band fast path is exercised by {@link WCSNetCDFTest#testRequestNetCDF} and friends; this class exists
 * strictly to lock in the multi-band contract.
 */
@TestSetup(run = TestSetupFrequency.ONCE)
public class WCSNetCDFMultiBandTest extends WCSNetCDFBaseTest {

    /**
     * Source mosaic providing the {@code NO2} and {@code BrO} single-variable coverages we wire into a
     * {@link CoverageView} with two {@code BAND_SELECT} bands. Reused from {@link WCSNetCDFMosaicTest}.
     */
    public static final QName SOURCE_MOSAIC =
            new QName(CiteTestData.WCS_URI, "MultiBandSourceMosaic", CiteTestData.WCS_PREFIX);

    /** The multi-band view layer published from {@link #SOURCE_MOSAIC} that the tests query. */
    public static final QName MULTIBAND_VIEW =
            new QName(CiteTestData.WCS_URI, "multiBandView", CiteTestData.WCS_PREFIX);

    /** Output band names — also the {@code BAND_SELECT} {@code <definition>} values, propagated to the sample dims. */
    private static final String BAND_NAME_NO2 = "NO2@0";

    private static final String BAND_NAME_BRO = "BrO@0";

    @BeforeClass
    public static void init() {
        WCSNetCDFBaseTest.init();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(SOURCE_MOSAIC, "gom.zip", null, null, this.getClass(), getCatalog());
        addCoverageViewToCatalog();
    }

    /** Build a {@link CoverageView} with two {@code BAND_SELECT} bands and register it as a published layer. */
    private void addCoverageViewToCatalog() throws Exception {
        Catalog cat = getCatalog();
        CoverageStoreInfo store = cat.getCoverageStoreByName(SOURCE_MOSAIC.getLocalPart());
        CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(store);

        InputCoverageBand inputNo2 = new InputCoverageBand("NO2", "0");
        CoverageBand outputNo2 =
                new CoverageBand(Collections.singletonList(inputNo2), BAND_NAME_NO2, 0, CompositionType.BAND_SELECT);
        InputCoverageBand inputBro = new InputCoverageBand("BrO", "0");
        CoverageBand outputBro =
                new CoverageBand(Collections.singletonList(inputBro), BAND_NAME_BRO, 1, CompositionType.BAND_SELECT);
        List<CoverageBand> bands = new ArrayList<>(2);
        bands.add(outputNo2);
        bands.add(outputBro);
        CoverageView view = new CoverageView(MULTIBAND_VIEW.getLocalPart(), bands);

        CoverageInfo viewInfo = view.createCoverageInfo(MULTIBAND_VIEW.getLocalPart(), store, builder);
        viewInfo.getParameters().put("USE_IMAGEN_IMAGEREAD", "false");
        cat.add(viewInfo);
        LayerInfo layerInfo = builder.buildLayer(viewInfo);
        cat.add(layerInfo);
    }

    /**
     * Default multi-band behavior: with no {@code bandSettings} configured, the encoder writes one output variable per
     * band, named after the band's sample dimension description. For a {@code COVERAGE_VIEW} with {@code BAND_SELECT}
     * the description equals the {@code <definition>} field, so we expect variables named {@value #BAND_NAME_NO2} and
     * {@value #BAND_NAME_BRO}. The legacy single-variable named after the layer must NOT exist.
     */
    @Test
    public void testMultiBandDefault() throws Exception {
        // Make sure no leftover bandSettings or layerName from a previous test pollute this run
        clearLayerNetCDFSettings();

        File output = requestCoverage();
        try (NetcdfDataset dataset = NetcdfDatasets.openDataset(output.getAbsolutePath())) {
            Variable no2 = dataset.findVariable(BAND_NAME_NO2);
            Variable bro = dataset.findVariable(BAND_NAME_BRO);
            assertNotNull("output NetCDF must contain a '" + BAND_NAME_NO2 + "' variable", no2);
            assertNotNull("output NetCDF must contain a '" + BAND_NAME_BRO + "' variable", bro);

            // Both bands share the source coverage's spatial grid: same dim count, same dim shape.
            assertEquals(no2.getDimensions().size(), bro.getDimensions().size());
            assertArrayEquals(
                    "both bands' output variables must share the spatial dims of the source coverage",
                    no2.getShape(),
                    bro.getShape());

            // The legacy single-var named after the layer must be absent — confirms the multi-band path actually fired
            // instead of the single-band collapse fallback.
            assertNull(
                    "legacy single-var named after the layer must NOT be present when multi-band fires",
                    dataset.findVariable(MULTIBAND_VIEW.getLocalPart()));

            // Each band carries its own _FillValue (sourced from the per-band sample dimension).
            assertNotNull("NO2@0 must have its _FillValue", no2.findAttribute("_FillValue"));
            assertNotNull("BrO@0 must have its _FillValue", bro.findAttribute("_FillValue"));
        }
    }

    /**
     * Per-band override: the layer config carries a {@code bandSettings} list with a {@code name} + {@code uom} +
     * {@code variableAttributes} per band. The encoder must use those values verbatim — output variables get the
     * configured names, the configured units, and the configured per-band attributes (here, CF {@code standard_name}).
     * The container-level {@code variableAttributes} also still apply.
     */
    @Test
    public void testMultiBandWithBandSettings() throws Exception {
        NetCDFLayerSettingsContainer container = new NetCDFLayerSettingsContainer();

        // Container-level attribute applied to every band's output variable.
        List<VariableAttribute> globalVarAttrs = new ArrayList<>();
        globalVarAttrs.add(new VariableAttribute("source", "WCSNetCDFMultiBandTest"));
        container.setVariableAttributes(globalVarAttrs);

        // Per-band overrides — names, UoMs, and a per-band CF standard_name attr.
        List<BandSetting> bandSettings = new ArrayList<>(2);
        bandSettings.add(new BandSetting(
                "nitrogen_dioxide",
                "kg m-3",
                Collections.singletonList(
                        new VariableAttribute("standard_name", "mass_concentration_of_nitrogen_dioxide_in_air"))));
        bandSettings.add(new BandSetting(
                "bromine_monoxide",
                "kg m-3",
                Collections.singletonList(
                        new VariableAttribute("standard_name", "mass_concentration_of_bromine_monoxide_in_air"))));
        container.setBandSettings(bandSettings);

        applyContainer(container);

        File output = requestCoverage();
        try (NetcdfDataset dataset = NetcdfDatasets.openDataset(output.getAbsolutePath())) {
            Variable no2 = dataset.findVariable("nitrogen_dioxide");
            Variable bro = dataset.findVariable("bromine_monoxide");
            assertNotNull("output must contain 'nitrogen_dioxide'", no2);
            assertNotNull("output must contain 'bromine_monoxide'", bro);

            // The auto-derived names from the BAND_SELECT <definition> must NOT be present — the user's overrides win.
            assertNull(
                    "auto-derived '" + BAND_NAME_NO2 + "' must not be present when BandSetting.name overrides it",
                    dataset.findVariable(BAND_NAME_NO2));
            assertNull(
                    "auto-derived '" + BAND_NAME_BRO + "' must not be present when BandSetting.name overrides it",
                    dataset.findVariable(BAND_NAME_BRO));

            // Per-band UoM was applied.
            assertEquals("kg m-3", attrValue(no2, "units"));
            assertEquals("kg m-3", attrValue(bro, "units"));

            // Per-band variable attribute (CF standard_name) was applied.
            assertEquals("mass_concentration_of_nitrogen_dioxide_in_air", attrValue(no2, "standard_name"));
            assertEquals("mass_concentration_of_bromine_monoxide_in_air", attrValue(bro, "standard_name"));

            // Container-level variable attribute applies to every band.
            assertEquals("WCSNetCDFMultiBandTest", attrValue(no2, "source"));
            assertEquals("WCSNetCDFMultiBandTest", attrValue(bro, "source"));

            // Sanity: the two distinct variables really were emitted (not a single var named after the first
            // BandSetting).
            assertNotEquals(no2.getFullName(), bro.getFullName());
        }
    }

    /** Issue the WCS GetCoverage request and persist the response payload to a temp file for downstream parsing. */
    private File requestCoverage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1"
                + "&coverageId=wcs__"
                + MULTIBAND_VIEW.getLocalPart()
                + "&format=application/x-netcdf");
        assertEquals(200, response.getStatus());
        assertEquals("application/x-netcdf", response.getContentType());

        byte[] bytes = getBinary(response);
        assertTrue("response must carry a non-empty NetCDF payload", bytes.length > 0);
        File out = File.createTempFile("multiband-", ".nc", new File("./target"));
        FileUtils.writeByteArrayToFile(out, bytes);
        return out;
    }

    /** Replace the layer's NetCDF Output config with the supplied container, persisting via the catalog. */
    private void applyContainer(NetCDFLayerSettingsContainer container) {
        CoverageInfo info = getCatalog().getCoverageByName(getLayerId(MULTIBAND_VIEW));
        info.getMetadata().put(NetCDFSettingsContainer.NETCDFOUT_KEY, container);
        getCatalog().save(info);
    }

    /** Remove any previously-set NetCDF Output config from the layer. */
    private void clearLayerNetCDFSettings() {
        CoverageInfo info = getCatalog().getCoverageByName(getLayerId(MULTIBAND_VIEW));
        info.getMetadata().remove(NetCDFSettingsContainer.NETCDFOUT_KEY);
        getCatalog().save(info);
    }

    /** Convenience: read the string value of an attribute, failing the test with a clear message if absent. */
    private static String attrValue(Variable var, String name) {
        Attribute att = var.findAttribute(name);
        assertNotNull("variable '" + var.getFullName() + "' must declare attribute '" + name + "'", att);
        return att.getStringValue();
    }
}
