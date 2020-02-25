/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parameters defining an output format generated using an external tool from either a GML or a
 * shapefile or a GeoTIFF dump.
 *
 * @author Andrea Aime - OpenGeo
 * @author Stefano Costa - GeoSolutions
 */
public class Format {
    /** The tool output format name */
    private String toolFormat;

    /** The GeoServer output format name */
    private String geoserverFormat;

    /** The extension of the generated file, if any (shall include a dot, example, ".tab") */
    private String fileExtension;

    /** The options that will be added to the command line */
    private List<String> options;

    /** The type of format, used to instantiate the correct converter */
    private OutputType type;

    /**
     * If the output is a single file that can be streamed back. In that case we also need to know
     * the mime type
     */
    private boolean singleFile;

    /** The mime type of the single file output */
    private String mimeType;

    /** Eventual adapters to to be run before encoding */
    List<FormatAdapter> formatAdapters;

    public Format() {
        this.options = Collections.emptyList();
    }

    public Format(
            String toolFormat,
            String formatName,
            String fileExtension,
            boolean singleFile,
            String mimeType,
            OutputType type,
            String... options) {
        this.toolFormat = toolFormat;
        this.geoserverFormat = formatName;
        this.fileExtension = fileExtension;
        this.singleFile = singleFile;
        this.mimeType = mimeType;
        this.type = type;
        if (options != null) {
            this.options = new ArrayList<String>(Arrays.asList(options));
        }
        if (type == null) {
            this.type = OutputType.BINARY;
        }
    }

    public Format(
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

    /** @return the toolFormat */
    public String getToolFormat() {
        return toolFormat;
    }

    /** @param toolFormat the toolFormat to set */
    public void setToolFormat(String toolFormat) {
        this.toolFormat = toolFormat;
    }

    /** @return the geoserverFormat */
    public String getGeoserverFormat() {
        return geoserverFormat;
    }

    /** @param geoserverFormat the geoserverFormat to set */
    public void setGeoserverFormat(String geoserverFormat) {
        this.geoserverFormat = geoserverFormat;
    }

    /** Returns the configured format adapters, or an empty list if none was setup */
    public List<FormatAdapter> getFormatAdapters() {
        if (formatAdapters == null) {
            formatAdapters = new ArrayList<>();
        }
        return formatAdapters;
    }

    /** @return the fileExtension */
    public String getFileExtension() {
        return fileExtension;
    }

    /** @param fileExtension the fileExtension to set */
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /** @return the options */
    public List<String> getOptions() {
        return options;
    }

    /** @param options the options to set */
    public void setOptions(List<String> options) {
        this.options = options;
    }

    /** @return the type */
    public OutputType getType() {
        return type;
    }

    /** @param type the type to set */
    public void setType(OutputType type) {
        this.type = type;
    }

    /** @return the singleFile */
    public boolean isSingleFile() {
        return singleFile;
    }

    /** @param singleFile the singleFile to set */
    public void setSingleFile(boolean singleFile) {
        this.singleFile = singleFile;
    }

    /** @return the mimeType */
    public String getMimeType() {
        return mimeType;
    }

    /** @param mimeType the mimeType to set */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
