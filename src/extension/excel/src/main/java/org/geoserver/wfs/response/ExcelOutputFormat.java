/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;


/**
 * WFS output format for a GetFeature operation in which the outputFormat is "excel".
 *
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org
 */
public class ExcelOutputFormat extends WFSGetFeatureOutputFormat {
    
    static final int EXCELL_CELL_CHAR_LIMIT = (int) Math.pow(2,15) - 1; //32,767
    static final String STRING_LENGTH_WARNING = "DATA TRUNCATED (EXCEEDS EXCEL LIMIT)";

    public ExcelOutputFormat(GeoServer gs) {
        //this is the name of your output format, it is the string
        // that will be used when requesting the format in a 
        // GEtFeature request: 
        // ie ;.../geoserver/wfs?request=getfeature&outputFormat=myOutputFormat
        super(gs, "excel");
    }
    
    /**
     * @return "application/msexcel";
     */
    @Override
    public String getMimeType(Object value, Operation operation)
               throws ServiceException {
         return "application/msexcel";
    }
    
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        GetFeatureType request = (GetFeatureType) OwsUtils.parameter(operation.getParameters(),
                GetFeatureType.class);
        String outputFileName = ((QName) ((QueryType) request.getQuery().get(0)).getTypeName().get(0))
            .getLocalPart();
        return (String[][]) new String[][] {
                { "Content-Disposition", "attachment; filename=" + outputFileName + ".xls" }
            };
    }
    
    /**
     * @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation)
     */
    @Override
    protected void write(FeatureCollectionType featureCollection,
            OutputStream output, Operation getFeature) throws IOException,
            ServiceException {
        //Create the workbook 
        HSSFWorkbook wb = new HSSFWorkbook();
        
        for (Iterator it = featureCollection.getFeature().iterator(); it.hasNext();) {
            SimpleFeatureCollection fc = (SimpleFeatureCollection) it.next();
    	
            // create the sheet for this feature collection    	
        	HSSFSheet sheet = wb.createSheet(fc.getSchema().getTypeName());
      
            // write out the header
        	HSSFRow header = sheet.createRow((short) 0);
        	
            SimpleFeatureType ft = (SimpleFeatureType) fc.getSchema();
            HSSFCell cell;
            
            cell = header.createCell(0);
            cell.setCellValue(new HSSFRichTextString("FID"));
            for ( int i = 0; i < ft.getAttributeCount(); i++ ) {
                AttributeDescriptor ad = ft.getDescriptor(i);
                
                cell = header.createCell(i+1);
                cell.setCellValue(new HSSFRichTextString(ad.getLocalName()));
            }
            
            // write out the features
            SimpleFeatureIterator i = fc.features();
            int r = 0; // row index
            try {
            	HSSFRow row;
                while( i.hasNext() ) {
                	r++; //start at 1, since header is at 0
                    SimpleFeature f = i.next();
                    row = sheet.createRow((short) r);
                    cell = row.createCell(0);
                    cell.setCellValue(new HSSFRichTextString(f.getID()));
                    for ( int j = 0; j < f.getAttributeCount(); j++ ) {
                        Object att = f.getAttribute( j );
                        if ( att != null ) {
                        	cell = row.createCell(j+1);
                        	if(att instanceof Number) {
                      	        cell.setCellValue(((Number) att).doubleValue());
                        	} else if(att instanceof Date) {
                        	    cell.setCellValue((Date) att);
                        	} else if(att instanceof Calendar) {
                        	    cell.setCellValue((Calendar) att);
                        	} else if(att instanceof Boolean) {
                        	    cell.setCellValue((Boolean) att);
                        	} else {
                                // ok, it seems we have no better way than dump it as a string
                                String stringVal = att.toString();

                                // if string length > excel cell limit, truncate it and warn the
                                // user, otherwise excel workbook will be corrupted
                                if (stringVal.length() > EXCELL_CELL_CHAR_LIMIT) {
                                    stringVal = STRING_LENGTH_WARNING + " "
                                            + stringVal.substring(0, EXCELL_CELL_CHAR_LIMIT
                                                    - STRING_LENGTH_WARNING.length() - 1);
                                }
                                cell.setCellValue(new HSSFRichTextString(stringVal));
                        	}
                        }
                    }    
                }
            } finally {
                fc.close( i );
            }
        }
        
        //write to output
        wb.write(output);
    }
}
