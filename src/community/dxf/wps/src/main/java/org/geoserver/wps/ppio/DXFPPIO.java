/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.geoserver.wfs.response.dxf.DXFWriter;
import org.geoserver.wfs.response.dxf.DXFWriterFinder;
import org.geotools.feature.FeatureCollection;

/**
 * Outputs feature collections in DXF format
 * 
 * @author Andrea Aime - OpenGeo, Peter Hopfgartner - R3 GIS
 * 
 */
public class DXFPPIO extends CDataPPIO {

    protected DXFPPIO() {
        super(FeatureCollection.class, FeatureCollection.class, "application/dxf");
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        DXFWriter dxfWriter = DXFWriterFinder.getWriter("14", w);
        String[] names = {"wps_result"};
        dxfWriter.setOption("layers", names);
        int[] colors = {1};
        dxfWriter.setOption("colors", colors);
        
        List lft = new LinkedList();
        lft.add(value);
        dxfWriter.write(lft,"14");
        w.flush();            
    }

    @Override
    public Object decode(InputStream input) throws Exception {
    	throw new Exception("not implemented");
    }

    @Override
    public Object decode(String input) throws Exception {
    	throw new Exception("not implemented");
    }
    
    @Override
    public String getFileExtension() {
        return "dxf";
    }

}
