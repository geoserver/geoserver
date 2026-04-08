/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.geoserver.excel.ExcelWriter;
import org.geotools.data.simple.SimpleFeatureCollection;

public class XLSXPPIO extends CDataPPIO {

    protected XLSXPPIO() {
        super(SimpleFeatureCollection.class, SimpleFeatureCollection.class, "application/vnd.ms-excel");
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.ENCODING;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new RuntimeException("XLSX files decoding is not available.");
    }

    @Override
    public Object decode(Object input) throws Exception {
        throw new RuntimeException("XLSX files decoding is not available.");
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new RuntimeException("XLSX files decoding is not available.");
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        new ExcelWriter().write(List.of((SimpleFeatureCollection) value), os, ExcelWriter.ExcelFormat.XLSX);
    }
}
