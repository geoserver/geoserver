/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.ogr.core.Format;
import org.geoserver.ogr.core.OutputType;

/**
 * Parameters defining an output format generated using ogr2ogr from either a GML or a shapefile
 * dump
 *
 * @author Andrea Aime - OpenGeo
 * @author Stefano Costa - GeoSolutions
 */
public class OgrFormat extends Format {

    public OgrFormat(
            String ogrFormat,
            String formatName,
            String fileExtension,
            boolean singleFile,
            String mimeType,
            OutputType type,
            String... options) {
        this.ogrFormat = ogrFormat;
        this.formatName = formatName;
        setFileExtension(fileExtension);
        setSingleFile(singleFile);
        setMimeType(mimeType);
        setType(type);
        if (options != null) {
            setOptions(new ArrayList<String>(Arrays.asList(options)));
        }
        if (type == null) {
            setType(OutputType.BINARY);
        }
    }

    public OgrFormat(
            String toolFormat,
            String formatName,
            String fileExtension,
            boolean singleFile,
            String mimeType,
            String... options) {
        this(
                toolFormat,
                formatName,
                fileExtension,
                singleFile,
                mimeType,
                OutputType.BINARY,
                options);
    }

    /** The -f parameter */
    private String ogrFormat;

    private String formatName;

    @Override
    public String getToolFormat() {
        return ogrFormat;
    }

    @Override
    public String getGeoserverFormat() {
        return formatName;
    }
}
