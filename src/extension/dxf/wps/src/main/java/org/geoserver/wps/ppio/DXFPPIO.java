/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import org.geoserver.wfs.response.dxf.DXFWriter;
import org.geoserver.wfs.response.dxf.DXFWriterFinder;
import org.geotools.feature.FeatureCollection;

/**
 * Outputs feature collections in DXF format
 *
 * @author Andrea Aime - OpenGeo, Peter Hopfgartner - R3 GIS
 */
public class DXFPPIO extends CDataPPIO {
    protected DXFPPIO() {
        super(FeatureCollection.class, FeatureCollection.class, "application/dxf");
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Charset dxfcharset = Charset.forName("Cp1252");
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, dxfcharset));
        DXFWriter dxfWriter = DXFWriterFinder.getWriter("14", w);
        String[] names = {"wps_result"};
        dxfWriter.setOption("layers", names);
        int[] colors = {1};
        dxfWriter.setOption("colors", colors);
        dxfWriter.setOption("writeattributes", true);

        List<Object> lft = new LinkedList<Object>();
        lft.add(value);
        dxfWriter.write(lft, "14");
        w.flush();
    }

    @Override
    public Object decode(InputStream input) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("DXF files can not be used as input");
    }

    @Override
    public Object decode(String input) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("DXF files can not be used as input");
    }

    @Override
    public String getFileExtension() {
        return "dxf";
    }
}
