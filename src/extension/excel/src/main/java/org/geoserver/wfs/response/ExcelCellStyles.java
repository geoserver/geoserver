/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelCellStyles {
    private CellStyle dateStyle;

    private CellStyle headerStyle;

    private CellStyle warningStyle;

    public ExcelCellStyles(Workbook wb) {
        CreationHelper helper = wb.getCreationHelper();
        DataFormat fmt = helper.createDataFormat();

        dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(fmt.getFormat("yyyy-mm-dd hh:mm:ss"));

        headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        warningStyle = wb.createCellStyle();
        Font warningFont = wb.createFont();
        warningFont.setBold(true);
        warningFont.setColor(Font.COLOR_RED);
        warningStyle.setFont(warningFont);
    }

    public CellStyle getDateStyle() {
        return dateStyle;
    }

    public CellStyle getHeaderStyle() {
        return headerStyle;
    }

    public CellStyle getWarningStyle() {
        return warningStyle;
    }
}
