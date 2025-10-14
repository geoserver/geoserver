/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.Test;

public class XLSXPPIOTest extends GeoServerTestSupport {

    @Test
    public void testEncodeOutputStream() throws Exception {

        SimpleFeatureType type = DataUtilities.createType(
                "Locations",
                "geom:Point:srid=4326,name:String,population:Integer,last_census_date:Date,density:Double,visited:Boolean,null_field:Float");

        DefaultFeatureCollection fc = new DefaultFeatureCollection("locations", type);

        fc.add(SimpleFeatureBuilder.build(
                type,
                new Object[] {
                    new WKTReader2().read("POINT (9.19 45.46)"),
                    "Milan",
                    1378000,
                    ZonedDateTime.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0), ZoneId.of("Europe/Rome")),
                    Double.MAX_VALUE,
                    true,
                    null
                },
                null));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new XLSXPPIO().encode(fc, os);

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()));

        assertEquals(1, wb.getNumberOfSheets());

        XSSFSheet sheet = wb.getSheetAt(0);

        assertEquals("Locations", sheet.getSheetName());

        Iterator<Row> rowIterator = sheet.iterator();

        assertTrue(rowIterator.hasNext());
        Row headerRow = rowIterator.next();

        DataFormatter formatter = new DataFormatter(Locale.US);
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        String headerRowStringContent = StreamSupport.stream(headerRow.spliterator(), false)
                .map(cell -> formatter.formatCellValue(cell, evaluator))
                .collect(Collectors.joining(","));

        assertEquals("FID,geom,name,population,last_census_date,density,visited,null_field", headerRowStringContent);

        assertTrue(rowIterator.hasNext());
        Row dataRow = rowIterator.next();

        Iterator<Cell> cellIterator = dataRow.iterator();
        assertTrue(cellIterator.hasNext());

        Cell fidCell = cellIterator.next();
        assertTrue(fidCell.getStringCellValue().startsWith("fid-"));

        String dataRowStringContent = StreamSupport.stream(((Iterable<Cell>) () -> cellIterator).spliterator(), false)
                .map(cell -> formatter.formatCellValue(cell, evaluator))
                .collect(Collectors.joining(","));

        assertEquals(
                "POINT (9.19 45.46),Milan,1378000,2025-01-01T00:00+01:00[Europe/Rome],1.79769E+308,TRUE",
                dataRowStringContent);

        assertFalse(rowIterator.hasNext());
    }

    @Test
    public void failsWhenExceedingExcelTextLimit() throws Exception {

        String textExceedingCellLimit = "B".repeat(SpreadsheetVersion.EXCEL2007.getMaxTextLength() + 1);

        SimpleFeatureType type = DataUtilities.createType("TooMuchText", "text:String");

        DefaultFeatureCollection fc = new DefaultFeatureCollection(null, type);

        fc.add(SimpleFeatureBuilder.build(type, new Object[] {textExceedingCellLimit}, null));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new XLSXPPIO().encode(fc, os);

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(os.toByteArray()));

        assertEquals(1, wb.getNumberOfSheets());

        XSSFSheet sheet = wb.getSheetAt(0);

        assertEquals("TooMuchText", sheet.getSheetName());

        Iterator<Row> rowIterator = sheet.iterator();

        assertTrue(rowIterator.hasNext());
        /* skipping header row */
        rowIterator.next();

        assertTrue(rowIterator.hasNext());
        Row dataRow = rowIterator.next();

        Iterator<Cell> cellIterator = dataRow.iterator();

        assertTrue(cellIterator.hasNext());
        /* skipping fid cell */
        cellIterator.next();

        Cell limitedTextCell = cellIterator.next();

        assertEquals(
                SpreadsheetVersion.EXCEL2007.getMaxTextLength(),
                limitedTextCell.getStringCellValue().length());
        assertTrue(limitedTextCell.getStringCellValue().startsWith("DATA TRUNCATED BBBBB"));

        assertFalse(rowIterator.hasNext());
    }
}
