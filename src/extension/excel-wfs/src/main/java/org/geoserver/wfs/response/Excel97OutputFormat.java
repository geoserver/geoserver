/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.geoserver.config.GeoServer;
import org.geoserver.excel.ExcelWriter;

/**
 * Excel 97-2003 WFS output format
 *
 * @author Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public class Excel97OutputFormat extends ExcelOutputFormat {

    /** Constructor setting the format type as "excel" in addition to file extension, and mime type */
    public Excel97OutputFormat(GeoServer gs) {
        super(gs, "excel");
        fileExtension = "xls";
        mimeType = "application/msexcel";
    }

    @Override
    protected ExcelWriter.ExcelFormat excelFormat() {
        return ExcelWriter.ExcelFormat.XLS;
    }
}
