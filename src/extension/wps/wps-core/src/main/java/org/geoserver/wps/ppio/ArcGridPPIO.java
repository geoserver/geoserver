/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.parameter.Parameter;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Decodes/encodes a GeoTIFF file
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class ArcGridPPIO extends CDataPPIO {

    protected ArcGridPPIO() {
        super(GridCoverage2D.class, GridCoverage2D.class, "application/arcgrid");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // ArcGrid files can be read directly from stream
        return new ArcGridFormat().getReader(input).read(null);
    }
    
    @Override
    public Object decode(String arcgrid) throws Exception {
        // if the user forgot to add the final newline let's just add it
        if(!arcgrid.endsWith("\n")) {
            arcgrid += "\n";
        }
        ByteArrayInputStream in = new ByteArrayInputStream(arcgrid.getBytes());
        return new ArcGridFormat().getReader(in).read(null);
    }


    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Parameter<Boolean> forceSquareCells = new Parameter<Boolean>(ArcGridFormat.FORCE_CELLSIZE, Boolean.TRUE);
        new ArcGridFormat().getWriter(os).write((GridCoverage2D) value, 
                new GeneralParameterValue[] {forceSquareCells});
    }
    
    @Override
    public String getFileExtension() {
        return "zip";
    }

}
