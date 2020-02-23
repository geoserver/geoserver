/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.geoserver.config.GeoServer;

/**
 * Excel 97-2003 WFS output format
 *
 * @author Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public class Excel97OutputFormat extends ExcelOutputFormat {

    /**
     * Constructor setting the format type as "excel" in addition to file extension, mime type, and
     * row and column limits
     */
    public Excel97OutputFormat(GeoServer gs) {
        super(gs, "excel");
        rowLimit = (int) Math.pow(2, 16); // 65,536
        colLimit = (int) Math.pow(2, 8); // 256
        fileExtension = "xls";
        mimeType = "application/msexcel";
    }

    /** Returns a new HSSFWorkbook workbook */
    @Override
    protected Workbook getNewWorkbook() {
        return new HSSFWorkbook();
    }
}
