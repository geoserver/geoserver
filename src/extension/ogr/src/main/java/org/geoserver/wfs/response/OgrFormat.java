/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameters defining an output format generated using ogr2ogr from
 * either a GML dump 
 * @author Andrea Aime - OpenGeo
 *
 */
public class OgrFormat {
    /**
     * The -f parameter
     */
    public String ogrFormat;
    
    /**
     * The GeoServer output format name
     */
    public String formatName;
    
    /**
     * The extension of the generated file, if any (shall include a dot, example, ".tab")
     */
    public String fileExtension;
    
    /**
     * The options that will be added to the command line
     */
    public List<String> options;
    
    /**
     * If the output is a single file that can be streamed back. In that case we also need
     * to know the mime type
     */
    public boolean singleFile;
    
    /**
     * The mime type of the single file output
     */
    public String mimeType;

    public OgrFormat(String ogrFormat, String formatName, String fileExtension, boolean singleFile, 
            String mimeType, String... options) {
        this.ogrFormat = ogrFormat;
        this.formatName = formatName;
        this.fileExtension = fileExtension;
        this.singleFile = singleFile;
        this.mimeType = mimeType;
        if(options != null) {
            this.options = new ArrayList<String>(Arrays.asList(options));
        }
    }

    
}
