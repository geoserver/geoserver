/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import it.geosolutions.imageio.pam.PAMDataset;
import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.api.style.Symbolizer;
import org.geotools.coverage.grid.io.PAMResourceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.xml.styling.SLDParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for RasterAttributeTableVisitor */
public class RasterAttributeTableVisitorTest {

    public static final String NATIVE_NAME = "rat";
    static File ROOT = new File("./src/test/resources/org/geoserver/wms/featureinfo");
    static GeoTiffReader READER;

    @BeforeClass
    public static void prepare() throws DataSourceException {
        READER = new GeoTiffReader(new File(ROOT, "rat.tiff"));
    }

    @AfterClass
    public static void cleanup() {
        READER.dispose();
    }

    @Test
    public void testBelowMinScale() throws Exception {
        RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(0.1, NATIVE_NAME, READER);
        getRatStyle().accept(visitor);
        assertNull(visitor.getAttributeTableEnricher());
    }

    @Test
    public void testAboveMaxScale() throws Exception {
        RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(2e6, NATIVE_NAME, READER);
        getRatStyle().accept(visitor);
        assertNull(visitor.getAttributeTableEnricher());
    }

    @Test
    public void testNoVendorOption() throws Exception {
        Style style = getRatStyle();
        Symbolizer symbolizer =
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        symbolizer.getOptions().clear();

        // scale in range, but vendor option got removed
        RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(1000, NATIVE_NAME, READER);
        style.accept(visitor);
        assertNull(visitor.getAttributeTableEnricher());
    }

    @Test
    public void testGetEnricher() throws Exception {
        RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(1000, NATIVE_NAME, READER);
        getRatStyle().accept(visitor);
        AttributeTableEnricher enricher = visitor.getAttributeTableEnricher();
        assertNotNull(enricher);
        PAMDataset.PAMRasterBand band = enricher.getPamRasterBand();
        PAMDataset pam = ((PAMResourceInfo) READER.getInfo(NATIVE_NAME)).getPAMDataset();
        assertEquals(pam.getPAMRasterBand().get(0), band);
    }

    @Test
    public void testNoPAMDataset() throws Exception {
        GeoTiffReader reader =
                new GeoTiffReader(new File("./src/test/resources/org/geoserver/wms/wms_1_1_1/tazbm.tiff"));
        try {
            RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(1000, "tazbm", reader);
            getRatStyle().accept(visitor);
            assertNull(visitor.getAttributeTableEnricher());
        } finally {
            reader.dispose();
        }
    }

    /** This file has an internal PAM dataset without a raster attribute table */
    @Test
    public void testInternalPam() throws Exception {
        File source = new File(ROOT, "rat.tiff");
        File target = new File("./target/not_rat.tiff");
        FileUtils.copyFile(source, target);
        GeoTiffReader reader = new GeoTiffReader(target);
        try {
            RasterAttributeTableVisitor visitor = new RasterAttributeTableVisitor(1000, "tazbm", reader);
            getRatStyle().accept(visitor);
            assertNull(visitor.getAttributeTableEnricher());
        } finally {
            reader.dispose();
        }
    }

    private Style getRatStyle() throws FileNotFoundException {
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(new File(ROOT, "rat.sld"));
        StyledLayerDescriptor sld = parser.parseSLD();
        return ((NamedLayer) sld.getStyledLayers()[0]).getStyles()[0];
    }
}
