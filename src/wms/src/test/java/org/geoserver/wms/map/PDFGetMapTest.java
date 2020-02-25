/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class PDFGetMapTest extends WMSTestSupport {

    String bbox = "-1.5,-0.5,1.5,1.5";

    String layers = getLayerId(MockData.BASIC_POLYGONS);

    String requestBase =
            "wms?bbox="
                    + bbox
                    + "&layers="
                    + layers
                    + "&Format=application/pdf&request=GetMap"
                    + "&width=300&height=300&srs=EPSG:4326";

    static boolean tilingPatterDefault = PDFMapResponse.ENCODE_TILING_PATTERNS;

    @After
    public void setupTilingPatternEncoding() {
        PDFMapResponse.ENCODE_TILING_PATTERNS = tilingPatterDefault;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle("burg-fill", "burg-fill.sld", PDFGetMapTest.class, catalog);
        testData.addStyle("triangle-fill", "triangle-fill.sld", PDFGetMapTest.class, catalog);
        testData.addStyle("hatch-fill", "hatch-fill.sld", PDFGetMapTest.class, catalog);

        // copy over the svg icon
        File styles = new File(testData.getDataDirectoryRoot(), "styles");
        File burg = new File(styles, "burg02.svg");
        try (InputStream is = getClass().getResourceAsStream("burg02.svg");
                FileOutputStream fos = new FileOutputStream(burg)) {
            IOUtils.copy(is, fos);
        }
    }

    @Test
    public void testBasicPolygonMap() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(requestBase + "&styles=");
        assertEquals("application/pdf", response.getContentType());
        PDTilingPattern tilingPattern = getTilingPattern(response.getContentAsByteArray());
        assertNull(tilingPattern);
    }

    @Test
    public void testSvgFillOptimization() throws Exception {
        // get a single polygon to ease testing
        MockHttpServletResponse response =
                getAsServletResponse(
                        requestBase + "&styles=burg-fill&featureId=BasicPolygons.1107531493630");
        assertEquals("application/pdf", response.getContentType());

        PDTilingPattern tilingPattern = getTilingPattern(response.getContentAsByteArray());
        assertNotNull(tilingPattern);
        assertEquals(20, tilingPattern.getXStep(), 0d);
        assertEquals(20, tilingPattern.getYStep(), 0d);
    }

    @Test
    public void testSvgFillOptimizationDisabled() throws Exception {
        PDFMapResponse.ENCODE_TILING_PATTERNS = false;

        // get a single polygon to ease testing
        MockHttpServletResponse response =
                getAsServletResponse(
                        requestBase + "&styles=burg-fill&featureId=BasicPolygons.1107531493630");
        assertEquals("application/pdf", response.getContentType());

        // the tiling pattern encoding has been disabled
        PDTilingPattern tilingPattern = getTilingPattern(response.getContentAsByteArray());
        assertNull(tilingPattern);
    }

    @Test
    public void testTriangleFillOptimization() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        requestBase
                                + "&styles=triangle-fill&featureId=BasicPolygons.1107531493630");
        assertEquals("application/pdf", response.getContentType());

        File file = new File("./target/test.pdf");
        org.apache.commons.io.FileUtils.writeByteArrayToFile(
                file, response.getContentAsByteArray());

        PDTilingPattern tilingPattern = getTilingPattern(response.getContentAsByteArray());
        assertNotNull(tilingPattern);
        assertEquals(20, tilingPattern.getXStep(), 0d);
        assertEquals(20, tilingPattern.getYStep(), 0d);
    }

    @Test
    public void testHatchFillOptimization() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        requestBase + "&styles=hatch-fill&featureId=BasicPolygons.1107531493630");
        assertEquals("application/pdf", response.getContentType());

        // for hatches we keep the existing "set of parallel lines" optimization approach, need to
        // determine
        // if we want to remove it or not yet
        PDTilingPattern tilingPattern = getTilingPattern(response.getContentAsByteArray());
        assertNull(tilingPattern);
    }

    /**
     * Returns the last tiling pattern found during a render of the PDF document. Can be used to
     * extract one tiling pattern that gets actually used to render shapes (meant to be used against
     * a document that only has a single tiling pattern)
     */
    PDTilingPattern getTilingPattern(byte[] pdfDocument)
            throws InvalidPasswordException, IOException {
        // load the document using PDFBOX (iText is no good for parsing tiling patterns, mostly
        // works
        // well for text and image extraction, spent a few hours trying to use it with no results)
        PDDocument doc = PDDocument.load(pdfDocument);
        PDPage page = doc.getPage(0);

        // use a graphics stream engine, it's the only thing I could find that parses the PDF
        // deep enough to allow catching the tiling pattern in parsed form
        AtomicReference<PDTilingPattern> pattern = new AtomicReference<>();
        PDFStreamEngine engine =
                new PDFGraphicsStreamEngine(page) {

                    @Override
                    public void strokePath() throws IOException {}

                    @Override
                    public void shadingFill(COSName shadingName) throws IOException {}

                    @Override
                    public void moveTo(float x, float y) throws IOException {}

                    @Override
                    public void lineTo(float x, float y) throws IOException {}

                    @Override
                    public Point2D getCurrentPoint() throws IOException {
                        return null;
                    }

                    @Override
                    public void fillPath(int windingRule) throws IOException {}

                    @Override
                    public void fillAndStrokePath(int windingRule) throws IOException {}

                    @Override
                    public void endPath() throws IOException {}

                    @Override
                    public void drawImage(PDImage pdImage) throws IOException {}

                    @Override
                    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
                            throws IOException {}

                    @Override
                    public void closePath() throws IOException {}

                    @Override
                    public void clip(int windingRule) throws IOException {}

                    @Override
                    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3)
                            throws IOException {}
                };

        // setup the tiling pattern trap
        engine.addOperator(
                new SetNonStrokingColorN() {

                    @Override
                    public void process(Operator operator, List<COSBase> arguments)
                            throws IOException {
                        super.process(operator, arguments);

                        PDColor color = context.getGraphicsState().getNonStrokingColor();
                        if (context.getGraphicsState().getNonStrokingColorSpace()
                                instanceof PDPattern) {
                            PDPattern colorSpace =
                                    (PDPattern)
                                            context.getGraphicsState().getNonStrokingColorSpace();
                            PDAbstractPattern ap = colorSpace.getPattern(color);
                            if (ap instanceof PDTilingPattern) {
                                pattern.set((PDTilingPattern) ap);
                            }
                        }
                    }
                });
        // run it
        engine.processPage(page);

        return pattern.get();
    }
}
