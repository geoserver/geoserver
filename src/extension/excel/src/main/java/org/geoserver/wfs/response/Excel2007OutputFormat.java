/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.geoserver.config.GeoServer;

/**
 * Excel 2007 WFS output format
 *
 * @author Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public class Excel2007OutputFormat extends ExcelOutputFormat {
    private static Logger log = Logger.getLogger(Excel2007OutputFormat.class);

    /**
     * Constructor setting the format type as "excel2007" in addition to file extension, mime type,
     * and row and column limits
     *
     * @param gs
     */
    public Excel2007OutputFormat(GeoServer gs) {
        super(gs, "excel2007");
        rowLimit = (int) Math.pow(2, 20); // 1,048,576
        colLimit = (int) Math.pow(2, 14); // 16,384
        fileExtension = "xlsx";
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    /** Returns a new SXSSFWorkbook workbook */
    @Override
    protected Workbook getNewWorkbook() {
        return new SXSSFWorkbook(1);
    }
}
