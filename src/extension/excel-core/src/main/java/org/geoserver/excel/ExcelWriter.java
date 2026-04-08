/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;

public class ExcelWriter {

    /** This limit is the same for both Excel versions */
    private static final int CELL_CHAR_LIMIT = SpreadsheetVersion.EXCEL2007.getMaxTextLength();

    private static final String TRUNCATE_WARNING = "DATA TRUNCATED";

    public enum ExcelFormat {
        XLS(SpreadsheetVersion.EXCEL97),
        XLSX(SpreadsheetVersion.EXCEL2007);

        private final SpreadsheetVersion spreadsheetVersion;

        ExcelFormat(SpreadsheetVersion spreadsheetVersion) {
            this.spreadsheetVersion = spreadsheetVersion;
        }

        int rowLimit() {
            return spreadsheetVersion.getMaxRows();
        }

        int colLimit() {
            return spreadsheetVersion.getMaxColumns();
        }
    }

    public void write(List<FeatureCollection> fc, OutputStream output, ExcelFormat excelFormat) throws IOException {

        // Create the workbook
        try (Workbook wb = createFormatSpecificNotebook(excelFormat)) {
            CreationHelper helper = wb.getCreationHelper();
            ExcelCellStyles styles = new ExcelCellStyles(wb);

            for (FeatureCollection<?, ?> collection : fc) {
                SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) collection;

                // create the sheet for this feature collection
                Sheet sheet = wb.createSheet(featureCollection.getSchema().getTypeName());

                // write out the header
                Row header = sheet.createRow(0);

                SimpleFeatureType ft = featureCollection.getSchema();

                Cell cell = header.createCell(0);
                cell.setCellValue(helper.createRichTextString("FID"));
                for (int i = 0; i < ft.getAttributeCount() && i < excelFormat.colLimit(); i++) {
                    AttributeDescriptor ad = ft.getDescriptor(i);
                    cell = header.createCell(i + 1);
                    cell.setCellValue(helper.createRichTextString(ad.getLocalName()));
                    cell.setCellStyle(styles.getHeaderStyle());
                }

                // write out the features
                try (SimpleFeatureIterator i = featureCollection.features()) {
                    int r = 0; // row index
                    while (i.hasNext()) {
                        r++; // start at 1, since header is at 0

                        Row row = sheet.createRow(r);
                        cell = row.createCell(0);

                        if (r == (excelFormat.rowLimit() - 1) && i.hasNext()) {
                            // there are more features than rows available in this
                            // Excel format. write out a warning line and break
                            RichTextString rowWarning = helper.createRichTextString(
                                    TRUNCATE_WARNING + ": ROWS " + r + " - " + featureCollection.size() + " NOT SHOWN");
                            cell.setCellValue(rowWarning);
                            cell.setCellStyle(styles.getWarningStyle());
                            break;
                        }

                        SimpleFeature feature = i.next();
                        cell.setCellValue(helper.createRichTextString(feature.getID()));
                        writeFeature(helper, styles, row, feature, excelFormat);
                    }
                }
            }
            // write to output
            wb.write(output);
        }
    }

    private Workbook createFormatSpecificNotebook(ExcelFormat excelFormat) {
        switch (excelFormat) {
            case XLS:
                return new HSSFWorkbook();
            case XLSX:
                return new SXSSFWorkbook(1);
            default:
                throw new IllegalStateException("Unexpected Excel format: " + excelFormat);
        }
    }

    private void writeFeature(
            CreationHelper helper, ExcelCellStyles styles, Row row, SimpleFeature f, ExcelFormat excelFormat) {
        for (int j = 0; j < f.getAttributeCount() && j < excelFormat.colLimit(); j++) {
            Object att = f.getAttribute(j);
            if (att != null) {
                Cell cell = row.createCell(j + 1);
                if (att instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else if (att instanceof Date date) {
                    cell.setCellValue(date);
                    cell.setCellStyle(styles.getDateStyle());
                } else if (att instanceof Calendar calendar) {
                    cell.setCellValue(calendar);
                    cell.setCellStyle(styles.getDateStyle());
                } else if (att instanceof Boolean boolean1) {
                    cell.setCellValue(boolean1);
                } else {
                    // ok, it seems we have no better way than dump it as a string
                    String stringVal = att.toString();

                    // if string length > Excel cell limit, truncate it and warn the
                    // user, otherwise excel workbook will be corrupted
                    if (stringVal.length() > CELL_CHAR_LIMIT) {
                        stringVal = TRUNCATE_WARNING
                                + " "
                                + stringVal.substring(0, CELL_CHAR_LIMIT - TRUNCATE_WARNING.length() - 1);
                        cell.setCellStyle(styles.getWarningStyle());
                    }
                    cell.setCellValue(helper.createRichTextString(stringVal));
                }
            }
        }
    }
}
