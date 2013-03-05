/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.geoserver.catalog.CoverageInfo;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

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

    private final ConcurrentHashMap<String, String> mapping = new ConcurrentHashMap<String, String>(10,0.9f,1);

    /**
     * Constructor.
     * 
     */
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
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Trying to map mime type for coverageinfo: "+cInfo.toString());
        }
        if(mapping.containsKey(nativeFormat)){

            final String mime = mapping.get(nativeFormat);
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("Found mapping for nativeFormat: "+nativeFormat+ mime);
            }
            return mime;
        }
        
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine("Unable to find mapping , let's open an ImageReader to the original source");
        }
        // no mapping let's go with the ImageIO reader code
        final File sourceFile = GeoserverDataDirectory.findDataFile(cInfo.getStore().getURL());
        if(sourceFile==null){ 
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("Original source is null");
            }
            return null;
        } else {
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("Original source: "+sourceFile.getAbsolutePath());
            }            
        }
        
        ImageInputStream inStream=null;
        ImageReader reader=null;
        try{
            inStream=ImageIO.createImageInputStream(sourceFile);
            if(inStream==null){
                LOGGER.warning("Unable to create an imageinputstream for this file:"+sourceFile.getAbsolutePath());
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inStream);
            if(readers.hasNext()){
                reader=readers.next();
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.fine("Found reader for format: "+reader.getFormatName());
                }                  
                mapping.putIfAbsent(nativeFormat, reader.getOriginatingProvider().getMIMETypes()[0]);
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.fine("Added mapping: "+mapping.get(nativeFormat));
                }                  
                return mapping.get(nativeFormat);
            } else {
                LOGGER.warning("Unable to create a reader for this file:"+sourceFile.getAbsolutePath());
            }
        }catch (Exception e) {
            if(LOGGER.isLoggable(Level.WARNING)){
                LOGGER.warning("Unable to map mime type for coverage: "+cInfo.toString());
            }
        } finally{
            try{
                if(inStream!=null){
                    inStream.close();
                }
            }catch (Exception e) {
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
                }
            }
            
            try{
                if(reader!=null){
                    reader.dispose();
                }
            }catch (Exception e) {
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
                }
            }            
        }
        return null;
        
    }
}
