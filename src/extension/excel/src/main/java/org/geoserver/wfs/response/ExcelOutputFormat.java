/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

/**
 * Abstract base class for Excel WFS output format
 *
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org and Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public abstract class ExcelOutputFormat extends WFSGetFeatureOutputFormat {

    protected static int CELL_CHAR_LIMIT = (int) Math.pow(2, 15) - 1; // 32,767

    protected static String TRUNCATE_WARNING = "DATA TRUNCATED";

    protected int rowLimit;

    protected int colLimit;

    protected String fileExtension;

    protected String mimeType;

    public ExcelOutputFormat(GeoServer gs, String formatName) {
        super(gs, formatName);
    }

    protected abstract Workbook getNewWorkbook();

    /** @return mime type; */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return mimeType;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return fileExtension;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /** @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation) */
    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {

        // Create the workbook
        try (Workbook wb = getNewWorkbook()) {
            CreationHelper helper = wb.getCreationHelper();
            ExcelCellStyles styles = new ExcelCellStyles(wb);

            for (org.geotools.feature.FeatureCollection collection : featureCollection.getFeature()) {
                SimpleFeatureCollection fc = (SimpleFeatureCollection) collection;

                // create the sheet for this feature collection
                Sheet sheet = wb.createSheet(fc.getSchema().getTypeName());

                // write out the header
                Row header = sheet.createRow(0);

                SimpleFeatureType ft = fc.getSchema();

                Cell cell = header.createCell(0);
                cell.setCellValue(helper.createRichTextString("FID"));
                for (int i = 0; i < ft.getAttributeCount() && i < colLimit; i++) {
                    AttributeDescriptor ad = ft.getDescriptor(i);
                    cell = header.createCell(i + 1);
                    cell.setCellValue(helper.createRichTextString(ad.getLocalName()));
                    cell.setCellStyle(styles.getHeaderStyle());
                }

                // write out the features
                try (SimpleFeatureIterator i = fc.features()) {
                    int r = 0; // row index
                    while (i.hasNext()) {
                        r++; // start at 1, since header is at 0

                        Row row = sheet.createRow(r);
                        cell = row.createCell(0);

                        if (r == (rowLimit - 1) && i.hasNext()) {
                            // there are more features than rows available in this
                            // Excel format. write out a warning line and break
                            RichTextString rowWarning = helper.createRichTextString(
                                    TRUNCATE_WARNING + ": ROWS " + r + " - " + fc.size() + " NOT SHOWN");
                            cell.setCellValue(rowWarning);
                            cell.setCellStyle(styles.getWarningStyle());
                            break;
                        }

                        SimpleFeature f = i.next();
                        cell.setCellValue(helper.createRichTextString(f.getID()));
                        writeFeature(helper, styles, row, f);
                    }
                }
            }

            // write to output
            wb.write(output);
        }
    }

    private void writeFeature(CreationHelper helper, ExcelCellStyles styles, Row row, SimpleFeature f) {
        for (int j = 0; j < f.getAttributeCount() && j < colLimit; j++) {
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

                    // if string length > excel cell limit, truncate it and warn the
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
