package org.geoserver.netcdf;

import static org.junit.Assert.assertEquals;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Properties;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.imageio.netcdf.NetCDFUnitFormat;
import org.junit.Test;

public class NetCDFUnitTest extends GeoServerSystemTestSupport {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "analyzed_sst", MockData.SF_PREFIX),
                "test-data/sst.nc",
                "nc",
                new HashMap(),
                getClass(),
                getCatalog());
        // workaround for SystemTestData assumption that rasters with a single coverage
        // should use the store name for the coverage name
        CoverageInfo ci = getCatalog().getCoverageByName("sf:analyzed_sst");
        ci.setNativeCoverageName("analyzed_sst");
        getCatalog().save(ci);
    }

    @Test
    public void testUnit() throws Exception {
        CoverageDimensionInfo dimension = getSSTCoverageDimensionInfo();
        assertEquals("\u2103", dimension.getUnit());
    }

    @Test
    public void testUnitAliasesOnReload() throws Exception {
        // reconfigure units with something funny
        Resource aliasResource = getDataDirectory().get(NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
        Properties p = new Properties();
        p.put("celsius", "g*(m/s)^2");
        try (OutputStreamWriter osw = new OutputStreamWriter(aliasResource.out(), "UTF8")) {
            p.store(osw, null);
        }
        try {
            // force unit definitions to be reloaded
            getGeoServer().reload();

            // try again
            CoverageDimensionInfo dimension = getSSTCoverageDimensionInfo();
            assertEquals("g*m^2*s^-2", dimension.getUnit());
        } finally {
            aliasResource.delete();
            getGeoServer().reload();
        }
    }

    @Test
    public void testUnitAliasesOnReset() throws Exception {
        // reconfigure units with something funny
        Resource aliasResource = getDataDirectory().get(NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
        Properties p = new Properties();
        p.put("celsius", "g*(m/s)^2");
        try (OutputStreamWriter osw = new OutputStreamWriter(aliasResource.out(), "UTF8")) {
            p.store(osw, null);
        }
        try {
            // force unit definitions to be reloaded
            getGeoServer().reset();

            // try again
            CoverageDimensionInfo dimension = getSSTCoverageDimensionInfo();
            assertEquals("g*m^2*s^-2", dimension.getUnit());
        } finally {
            aliasResource.delete();
            getGeoServer().reset();
        }
    }

    @Test
    public void testUnitReplacementsOnReset() throws Exception {
        // reconfigure units with something funny
        Resource replacementsResource =
                getDataDirectory().get(NetCDFUnitFormat.NETCDF_UNIT_REPLACEMENTS);
        Properties p = new Properties();
        p.put("celsius", "g*(m/s)^2");
        try (OutputStreamWriter osw = new OutputStreamWriter(replacementsResource.out(), "UTF8")) {
            p.store(osw, null);
        }
        try {
            // force unit definitions to be reloaded
            getGeoServer().reset();

            // try again
            CoverageDimensionInfo dimension = getSSTCoverageDimensionInfo();
            assertEquals("g*m^2*s^-2", dimension.getUnit());
        } finally {
            replacementsResource.delete();
            getGeoServer().reset();
        }
    }

    private CoverageDimensionInfo getSSTCoverageDimensionInfo() throws Exception {
        CoverageStoreInfo store =
                getCatalog()
                        .getStoreByName(
                                MockData.SF_PREFIX, "analyzed_sst", CoverageStoreInfo.class);
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        cb.setStore(store);
        CoverageInfo coverage = cb.buildCoverage();

        return coverage.getDimensions().get(0);
    }
}
