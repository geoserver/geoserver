/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Checks the Excel output formats are advertised and usable through OGC API Features. */
public class ExcelOGCAPIFeaturesTest extends OGCApiTestSupport {

    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLS_MIME = "application/msexcel";

    @Test
    public void testExcelFormatsAdvertisedInCollection() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + roadSegments, 200);

        for (String format : List.of(XLSX_MIME, XLS_MIME)) {
            List<?> links = json.read("$.links[?(@.rel=='items' && @.type=='" + format + "')]", List.class);
            assertEquals("Expected exactly one items link for " + format, 1, links.size());
            String href = (String) ((Map<?, ?>) links.get(0)).get("href");
            assertTrue(href, href.contains("/collections/cite:RoadSegments/items"));
        }
    }

    @Test
    public void testItemsAsExcel2007() throws Exception {
        MockHttpServletResponse response = getItems(XLSX_MIME);
        assertEquals(XLSX_MIME, response.getContentType());
        try (Workbook wb = new XSSFWorkbook(toStream(response))) {
            checkWorkbook(wb);
        }
    }

    @Test
    public void testItemsAsExcel97() throws Exception {
        MockHttpServletResponse response = getItems(XLS_MIME);
        assertEquals(XLS_MIME, response.getContentType());
        try (Workbook wb = new HSSFWorkbook(toStream(response))) {
            checkWorkbook(wb);
        }
    }

    private MockHttpServletResponse getItems(String format) throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        return getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + roadSegments + "/items?f=" + ResponseUtils.urlEncode(format), 200);
    }

    private InputStream toStream(MockHttpServletResponse response) {
        // read the raw bytes, avoiding any char conversion
        return new ByteArrayInputStream(response.getContentAsByteArray());
    }

    private void checkWorkbook(Workbook wb) throws Exception {
        Sheet sheet = wb.getSheet(MockData.ROAD_SEGMENTS.getLocalPart());
        assertNotNull("Missing sheet " + MockData.ROAD_SEGMENTS.getLocalPart(), sheet);

        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(getLayerId(MockData.ROAD_SEGMENTS));
        int count = info.getFeatureSource(null, null).getFeatures().size();
        // header row + one row per feature
        assertEquals(count + 1, sheet.getPhysicalNumberOfRows());

        Row header = sheet.getRow(0);
        assertNotNull(header);
        assertEquals("FID", header.getCell(0).getStringCellValue());
    }
}
