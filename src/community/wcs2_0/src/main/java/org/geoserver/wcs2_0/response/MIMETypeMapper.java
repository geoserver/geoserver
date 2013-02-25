/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wcs2_0.response;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.data.DataUtilities;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

/**
 * Simple mapping utility to map native formats to Mime Types using ImageIO reader capabilities. 
 * 
 * It does perform caching of the mappings. Tha cache should be very small, hence it uses hard references.
 * 
 * @author Simone Giannechini, GeoSolutions
 *
 */
public class MIMETypeMapper {
    
    private Logger LOGGER = Logging.getLogger(MIMETypeMapper.class);

    private GeoServerDataDirectory dataDirectory;

    private final ConcurrentHashMap<String, String> mapping = new ConcurrentHashMap<String, String>(10,0.9f,1);
    
    private MIMETypeMapper() {
    }
    
    /**
     * Returns a mime types or null for the provided {@link CoverageInfo} using the {@link CoverageInfo#getNativeFormat()}
     * as its key.
     * 
     * @param cInfo the {@link CoverageInfo} to find a mime type for
     * @return a mime types or null for the provided {@link CoverageInfo} using the {@link CoverageInfo#getNativeFormat()}
     * as its key.
     * @throws IOException in case we don't manage to open the underlying file
     */
    public String mapNativeFormat(final CoverageInfo cInfo) throws IOException{
        // checks 
        Utilities.ensureNonNull("cInfo", cInfo);
        
        //=== k, let's se if we have a mapping for the MIME TYPE
        final String nativeFormat=cInfo.getNativeFormat();
        if(mapping.containsKey(nativeFormat)){
            return mapping.get(nativeFormat);
        }
        
        // no mapping let's go with the ImageIO reader code
        final String source=cInfo.getStore().getURL();
        final URL sourceURL= new URL(source);
        final File sourceFile= DataUtilities.urlToFile(sourceURL);
        if(sourceFile==null){
            return null;
        }
        
        ImageInputStream inStream=null;
        ImageReader reader=null;
        try{
            inStream=ImageIO.createImageInputStream(sourceFile);
            if(inStream==null){
                LOGGER.info("Unable to create an imageinputstream for this file:"+sourceFile.getAbsolutePath());
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inStream);
            if(readers.hasNext()){
                reader=readers.next();
                mapping.putIfAbsent(nativeFormat, reader.getOriginatingProvider().getMIMETypes()[0]);
                return mapping.get(nativeFormat);
            }
        }catch (Exception e) {
            // TODO: handle exception
        } finally{
            try{
                if(inStream!=null){
                    inStream.close();
                }
            }catch (Exception e) {
                // TODO: handle exception
            }
            
            try{
                if(reader!=null){
                    reader.dispose();
                }
            }catch (Exception e) {
                // TODO: handle exception
            }            
        }
        return null;
        
    }
}
