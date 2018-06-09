/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Date;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert MonitorResutls to an Excel spreadsheet. */
@Component
public class ExcelMonitorConverter extends BaseMonitorConverter {

    private static final class ExcelRequestDataVisitor implements RequestDataVisitor {
        private final HSSFSheet sheet;

        int rowNumber = 1;

        private String[] fields;

        private ExcelRequestDataVisitor(HSSFSheet sheet, String[] fields) {
            this.sheet = sheet;
            this.fields = fields;
        }

        public void visit(RequestData data, Object... aggregates) {
            HSSFRow row = sheet.createRow(rowNumber++);
            for (int j = 0; j < fields.length; j++) {
                HSSFCell cell = row.createCell(j);
                Object obj = OwsUtils.get(data, fields[j]);
                if (obj == null) {
                    continue;
                }

                if (obj instanceof Date) {
                    cell.setCellValue((Date) obj);
                } else if (obj instanceof Number) {
                    cell.setCellValue(((Number) obj).doubleValue());
                } else {
                    cell.setCellValue(new HSSFRichTextString(obj.toString()));
                }
            }
        }
    }

    public ExcelMonitorConverter() {
        super(MonitorRequestController.EXCEL_MEDIATYPE);
    }

    @Override
    protected void writeInternal(MonitorQueryResults results, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Object object = results.getResult();
        Monitor monitor = results.getMonitor();

        // Create the workbook+sheet
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            final HSSFSheet sheet = wb.createSheet("requests");

            // create the header
            HSSFRow header = sheet.createRow(0);
            String[] fields = results.getFields();
            for (int i = 0; i < fields.length; i++) {
                HSSFCell cell = header.createCell(i);
                cell.setCellValue(new HSSFRichTextString(fields[i]));
            }

            // write out the request
            handleRequests(object, new ExcelRequestDataVisitor(sheet, fields), monitor);

            // write to output
            wb.write(outputMessage.getBody());
        }
    }
}
