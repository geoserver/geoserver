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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

public class SyncChecksumOutputFormat extends WFSGetFeatureOutputFormat {
	public static ThreadLocal<String> JUNIT_SHA1_SYNC = new ThreadLocal<String>();

	public SyncChecksumOutputFormat(GeoServer gs, Set<String> outputFormats) {
		super(gs, outputFormats);
	}

	public SyncChecksumOutputFormat(GeoServer gs, String outputFormat) {
		super(gs, outputFormat);
	}

	public SyncChecksumOutputFormat(GeoServer gs) {
        //this is the name of your output format, it is the string
        // that will be used when requesting the format in a 
        // GEtFeature request: 
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super(gs, "SyncChecksum");
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

	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation getFeature) throws IOException, ServiceException {
		// TODO Auto-generated method stub
        // let's check which attributes are specified ATTS=A,B,C
        // let's check which attributes are specified ATTS=A,B,C
        // let's check which attributes are specified ATTS=A,B,C
		
		Object p0 = getFeature.getParameters()[0];
		String atts;
        String sha1SyncJson;
		if (p0 instanceof net.opengis.wfs.impl.GetFeatureTypeImpl) {
			net.opengis.wfs.impl.GetFeatureTypeImpl gft = (net.opengis.wfs.impl.GetFeatureTypeImpl) p0;
			atts = (String) gft.getFormatOptions().get("ATTRIBUTES");
	        sha1SyncJson = (String)gft.getFormatOptions().get("SYNC");
		} else if (p0 instanceof net.opengis.wfs20.impl.GetFeatureTypeImpl) {
			net.opengis.wfs20.impl.GetFeatureTypeImpl gft = (net.opengis.wfs20.impl.GetFeatureTypeImpl) p0;
			atts = (String) gft.getFormatOptions().get("ATTRIBUTES");
			sha1SyncJson = (String)gft.getFormatOptions().get("SYNC");
		} else {
			throw new IllegalArgumentException("unknown type: " + p0);
		}
		
        if (sha1SyncJson == null) {
        	// Try unit test source
        	sha1SyncJson = JUNIT_SHA1_SYNC.get();
        }
        
// Here's some sample code that finds the sha1 filter function.
// However, this instances is NOT the instance that collected the SHA-1's
// Therefore, we are sticking with the thread local state storage.
//        FindSha1SyncFilterFunction visitor = new FindSha1SyncFilterFunction();
//        QueryType queryType = (QueryType)gft.getQuery().get(0);
//        if (queryType != null) {
//        	queryType.getFilter().accept(visitor, null);
//        }
//        if (visitor.getSha1SyncFilterFunction() != null) {
//        	// Found it
//        	visitor.getSha1SyncFilterFunction().getFeatureSha1s();
//        }
    	
        if(getFeature.getParameters()[0] instanceof GetFeatureType) {
            // get the query for this featureCollection
            GetFeatureType request = OwsUtils.parameter(getFeature.getParameters(),
                    GetFeatureType.class);
            QueryType queryType = (QueryType) request.getQuery().get(0);
            if(queryType.getFunction().size()>0) {
                Function syncFunction = (Function) queryType.getFunction().get(0);
                List<FeatureCollection> filtered = new ArrayList<FeatureCollection>();
                FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
                Filter f = ff.equal(ff.literal("true"), syncFunction, false);
                
               
                for(FeatureCollection fc : (List<FeatureCollection>)featureCollection.getFeature()) {
                    filtered.add(new FilteringFeatureCollection<FeatureType, Feature>(fc, f));
                }
                
                featureCollection.getFeature().clear();
                featureCollection.getFeature().addAll(filtered);
                
                // Hack to initialize the threadlocals
                for(FeatureCollection fc : (List<FeatureCollection>)featureCollection.getFeature()) {
                    FeatureIterator fi = fc.features();
                    try {
                        while(fi.hasNext())
                            fi.next();
                    } finally {
                        fi.close();
                    }
                }
            }
        }
        
        FeatureCollectionSha1Sync writer = new FeatureCollectionSha1Sync(output);
    	writer.parseAttributesToInclude(atts);
    	writer.parseSha1SyncJson(sha1SyncJson);
		writer.write(featureCollection);		
	}

}
