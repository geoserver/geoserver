/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.excel.ExcelWriter;

/**
 * Excel 2007 WFS output format
 *
 * @author Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public class Excel2007OutputFormat extends ExcelOutputFormat {

    static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** Constructor setting the format type as "excel2007" in addition to file extension, and mime type */
    public Excel2007OutputFormat(GeoServer gs) {
        super(gs, Set.of("excel2007", MIME_TYPE));
        fileExtension = "xlsx";
        mimeType = MIME_TYPE;
    }

    @Override
    protected ExcelWriter.ExcelFormat excelFormat() {
        return ExcelWriter.ExcelFormat.XLSX;
    }
}
