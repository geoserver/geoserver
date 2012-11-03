/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.type.DateUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * WFS output format for a GetFeature operation in which the outputFormat is "csv".
 * The refence specification for this format can be found in this RFC:
 * http://www.rfc-editor.org/rfc/rfc4180.txt
 *
 * @author Justin Deoliveira, OpenGeo, jdeolive@opengeo.org
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org
 * @author Andrea Aime, OpenGeo
 */
public class CSVOutputFormat extends WFSGetFeatureOutputFormat {

    public CSVOutputFormat(GeoServer gs) {
        //this is the name of your output format, it is the string
        // that will be used when requesting the format in a 
        // GEtFeature request: 
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super(gs, "csv");
    }
    
    /**
     * @return "text/csv";
     */
    @Override
    public String getMimeType(Object value, Operation operation)
               throws ServiceException {
        // won't allow browsers to open it directly, but that's the mime
        // state in the RFC
        return "text/csv";
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }
    
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFileName = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
        return outputFileName + ".csv";
    }
    
    /**
     * @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation)
     */
    @Override
    protected void write(FeatureCollectionResponse featureCollection,
            OutputStream output, Operation getFeature) throws IOException,
            ServiceException {
    	   //write out content here
        
        //create a writer
        BufferedWriter w = new BufferedWriter( new OutputStreamWriter( output ) );
                   
        //get the feature collection
        SimpleFeatureCollection fc = 
            (SimpleFeatureCollection) featureCollection.getFeature().get(0);
           
        //write out the header
        SimpleFeatureType ft = fc.getSchema();
        w.write("FID,");
        for ( int i = 0; i < ft.getAttributeCount(); i++ ) {
            AttributeDescriptor ad = ft.getDescriptor( i );
            w.write( prepCSVField(ad.getLocalName()) );
               
            if ( i < ft.getAttributeCount()-1 ) {
               w.write( "," );
            }
        }
        // by RFC each line is terminated by CRLF
        w.write( "\r\n" );
        
        // prepare the formatter for numbers
        NumberFormat coordFormatter = NumberFormat.getInstance(Locale.US);
        coordFormatter.setMaximumFractionDigits(getInfo().getGeoServer().getSettings().getNumDecimals());
        coordFormatter.setGroupingUsed(false);
           
        //write out the features
        SimpleFeatureIterator i = fc.features();
        try {
            while( i.hasNext() ) {
                SimpleFeature f = i.next();
                // dump fid
                w.write(prepCSVField(f.getID()));
                w.write(",");
                // dump attributes
                for ( int j = 0; j < f.getAttributeCount(); j++ ) {
                    Object att = f.getAttribute( j );
                    if ( att != null ) {
                        String value = null;
                        if(att instanceof Number) {
                            // don't allow scientific notation in the output, as OpenOffice won't 
                            // recognize that as a number 
                            value = coordFormatter.format(att);
                        } else if(att instanceof Date) {
                            // serialize dates in ISO format
                            if(att instanceof java.sql.Date)
                                value = DateUtil.serializeSqlDate((java.sql.Date) att);
                            else if(att instanceof java.sql.Time)
                                value = DateUtil.serializeSqlTime((java.sql.Time) att);
                            else
                                value = DateUtil.serializeDateTime((Date) att);
                        } else {
                            // everything else we just "toString"
                            value = att.toString();
                        }
                        w.write( prepCSVField(value) );
                    }
                    if ( j < f.getAttributeCount()-1 ) {
                        w.write(",");    
                    }
                }
                // by RFC each line is terminated by CRLF
                w.write( "\r\n" );
            }
        } finally {
            i.close();
        }
           
        w.flush();
    }
    
    /*
     * The CSV "spec" explains that fields with certain properties must be
     * delimited by double quotes, and also that double quotes within fields
     * must be escaped.  This method takes a field and returns one that
     * obeys the CSV spec.
     */    
    private String prepCSVField(String field){
    	// "embedded double-quote characters must be represented by a pair of double-quote characters."
    	String mod = field.replaceAll("\"", "\"\"");
    	
    	/*
    	 * Enclose string in double quotes if it contains double quotes, commas, or newlines
    	 */
    	if(mod.matches(".*(\"|\n|,).*")){
    		mod = "\"" + mod + "\"";
    	}
    	
		return mod;
    	
    }
    
    @Override
    public String getCapabilitiesElementName() {
    	return "CSV";
    }

}
