/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.format;




import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;

import com.moesol.geoserver.sync.format.BloomFilterFeatureCollectionWriter;

public class BloomFilterOutputFormat extends WFSGetFeatureOutputFormat {

	public BloomFilterOutputFormat(GeoServer gs, Set<String> outputFormats) {
		super(gs, outputFormats);
	}

	public BloomFilterOutputFormat(GeoServer gs, String outputFormat) {
		super(gs, outputFormat);
	}

	public BloomFilterOutputFormat(GeoServer gs) {
        //this is the name of your output format, it is the string
        // that will be used when requesting the format in a 
        // GEtFeature request: 
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super(gs, "sha1");
    }

    @Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        // return the mime type of the format here, the parent 
        // class returns 'text/xml'
        // return super.getMimeType(value, operation);
    	return "text/plain";
    }
    
    @Override
    protected boolean canHandleInternal(Operation operation) {
        //any additional checks that need to be performed to 
        // determine when the output format should be "engaged" 
        // should go here
        return super.canHandleInternal(operation);
    }
    
   // @Override
    protected void write(FeatureCollectionType featureCollection,
            OutputStream output, Operation getFeature) throws IOException,
            ServiceException {
    	
        // let's check which attributes are specified ATTS=A,B,C
        GetFeatureType gft = (GetFeatureType) getFeature.getParameters()[0];
        String atts = (String) gft.getFormatOptions().get("ATTRIBUTES");
    	
    	BloomFilterFeatureCollectionWriter writer = new BloomFilterFeatureCollectionWriter(featureCollection, output, getFeature);
    	writer.parseAttributesToInclude(atts);
		writer.write();
    }

	@Override
	protected void write(FeatureCollectionResponse arg0, OutputStream arg1,
			Operation arg2) throws IOException, ServiceException {
		// TODO Auto-generated method stub
		
	}

}
