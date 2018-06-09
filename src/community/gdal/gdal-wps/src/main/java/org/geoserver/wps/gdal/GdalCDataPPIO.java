/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.gdal;

import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.wcs.response.GdalCoverageResponseDelegate;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.coverage.grid.GridCoverage2D;

/** Encode text based output parameter using gdal_translate command */
public class GdalCDataPPIO extends CDataPPIO {

    private GdalCoverageResponseDelegate delegate;
    private String outputFormat;
    private String fileExtension;

    protected GdalCDataPPIO(
            String outputFormat, GdalCoverageResponseDelegate delegate, String mimeType) {
        super(GridCoverage2D.class, GridCoverage2D.class, mimeType);
        this.delegate = delegate;
        this.outputFormat = outputFormat;
        this.fileExtension = delegate.getFileExtension(outputFormat);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        delegate.encode((GridCoverage2D) value, outputFormat, null, os);
    }

    @Override
    public String getFileExtension() {
        return this.fileExtension;
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.ENCODING;
    }

    @Override
    public Object decode(String input) throws Exception {
        return null;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return null;
    }
}
