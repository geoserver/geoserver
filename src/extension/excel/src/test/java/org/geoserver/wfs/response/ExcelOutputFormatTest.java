/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExcelOutputFormatTest extends WFSTestSupport {
    @Test
    public void testExcel97OutputFormat() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&typeName=sf:PrimitiveGeoFeature&outputFormat=excel");
        InputStream in = getBinaryInputStream(resp);

        // check the mime type
        assertEquals("application/msexcel", resp.getContentType());

        // check the content disposition
        assertEquals(
                "attachment; filename=PrimitiveGeoFeature.xls",
                resp.getHeader("Content-Disposition"));

        HSSFWorkbook wb = new HSSFWorkbook(in);
        testExcelOutputFormat(wb);
    }

    @Test
    public void testExcel2007OutputFormat() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&typeName=sf:PrimitiveGeoFeature&outputFormat=excel2007");
        InputStream in = getBinaryInputStream(resp);

        // check the mime type
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resp.getContentType());

        // check the content disposition
        assertEquals(
                "attachment; filename=PrimitiveGeoFeature.xlsx",
                resp.getHeader("Content-Disposition"));

        XSSFWorkbook wb = new XSSFWorkbook(in);
        testExcelOutputFormat(wb);
    }

    private void testExcelOutputFormat(Workbook wb) throws IOException {
        Sheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);

        FeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);

        // check the number of rows in the output
        final int feautureRows = fs.getCount(Query.ALL);
        assertEquals(feautureRows + 1, sheet.getPhysicalNumberOfRows());

        // check the header is what we expect
        final SimpleFeatureType schema = (SimpleFeatureType) fs.getSchema();
        final Row header = sheet.getRow(0);
        assertEquals("FID", header.getCell(0).getRichStringCellValue().toString());
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            assertEquals(
                    schema.getDescriptor(i).getLocalName(),
                    header.getCell(i + 1).getRichStringCellValue().toString());
        }

        // check some selected values to see if the content and data type is the one
        // we expect
        FeatureIterator fi = fs.getFeatures().features();
        SimpleFeature sf = (SimpleFeature) fi.next();
        fi.close();

        // ... a string cell
        Cell cell = sheet.getRow(1).getCell(1);
        assertEquals(CellType.STRING, cell.getCellType());
        assertEquals(sf.getAttribute(0), cell.getRichStringCellValue().toString());
        // ... a geom cell
        cell = sheet.getRow(1).getCell(4);
        assertEquals(CellType.STRING, cell.getCellType());
        assertEquals(sf.getAttribute(3).toString(), cell.getRichStringCellValue().toString());
        // ... a number cell
        cell = sheet.getRow(1).getCell(6);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(((Number) sf.getAttribute(5)).doubleValue(), cell.getNumericCellValue(), 0d);
        // ... a date cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(10);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(sf.getAttribute(9), cell.getDateCellValue());
        // ... a boolean cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(12);
        assertEquals(CellType.BOOLEAN, cell.getCellType());
        assertEquals(sf.getAttribute(11), cell.getBooleanCellValue());
        // ... an empty cell (original value is null -> no cell)
        cell = sheet.getRow(1).getCell(3);
        assertNull(cell);
    }

    @Test
    public void testExcel97MultipleFeatureTypes() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&typeName=sf:PrimitiveGeoFeature,sf:GenericEntity&outputFormat=excel");
        InputStream in = getBinaryInputStream(resp);

        Workbook wb = new HSSFWorkbook(in);
        testMultipleFeatureTypes(wb);
    }

    @Test
    public void testExcel2007MultipleFeatureTypes() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&typeName=sf:PrimitiveGeoFeature,sf:GenericEntity&outputFormat=excel2007");
        InputStream in = getBinaryInputStream(resp);

        Workbook wb = new XSSFWorkbook(in);
        testMultipleFeatureTypes(wb);
    }

    private void testMultipleFeatureTypes(Workbook wb) throws IOException {
        // check we have the expected sheets
        Sheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);

        // check the number of rows in the output
        FeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());

        sheet = wb.getSheet("GenericEntity");
        assertNotNull(sheet);

        // check the number of rows in the output
        fs = getFeatureSource(MockData.GENERICENTITY);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());
    }
}
