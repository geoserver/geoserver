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
package org.geoserver.wcs.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.geoserver.config.GeoServer;
import org.geotools.util.Utilities;

/**
 * Base class for {@link CoverageResponseDelegate} implementations;
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public abstract class BaseCoverageResponseDelegate implements CoverageResponseDelegate {

    /** Output formats for this {@link CoverageResponseDelegate}. */
    private final List<String> outputFormats;

    /** File extensions for this {@link CoverageResponseDelegate}. */
    private final CaseInsensitiveMap fileExtensions;

    /** MIME Types for this {@link CoverageResponseDelegate}. */
    private final CaseInsensitiveMap mimeTypes;

    /** The current geoserver instance. */
    protected final GeoServer geoserver;

    /**
     * Constructor for this {@link BaseCoverageResponseDelegate};
     *
     * @param geoserver the {@link GeoServer} instance to get options from
     * @param outputFormats Output formats for this {@link CoverageResponseDelegate}
     * @param fileExtensions File extensions for this {@link CoverageResponseDelegate}
     * @param mimeTypes MIME Types for this {@link CoverageResponseDelegate}
     */
    public BaseCoverageResponseDelegate(
            GeoServer geoserver,
            List<String> outputFormats,
            Map<String, String> fileExtensions,
            Map<String, String> mimeTypes) {
        Utilities.ensureNonNull("outputFormats", outputFormats);
        Utilities.ensureNonNull("GeoServer", geoserver);
        Utilities.ensureNonNull("fileExtensions", fileExtensions);
        Utilities.ensureNonNull("mimeTypes", mimeTypes);
        if (outputFormats.isEmpty()) {
            throw new IllegalArgumentException("Empty list of outputFormats provided");
        }
        if (fileExtensions.isEmpty()) {
            throw new IllegalArgumentException("Empty list of fileExtensions provided");
        }
        if (mimeTypes.isEmpty()) {
            throw new IllegalArgumentException("Empty list of mimeTypes provided");
        }
        this.mimeTypes = new CaseInsensitiveMap(new HashMap<String, String>(mimeTypes));
        this.outputFormats = new ArrayList<String>(outputFormats);
        this.fileExtensions = new CaseInsensitiveMap(fileExtensions);
        this.geoserver = geoserver;
    }

    /** Default implementation, implementers should override. */
    @Override
    public boolean canProduce(String outputFormat) {
        Utilities.ensureNonNull("outputFormat", outputFormat);
        return outputFormats.contains(outputFormat) || mimeTypes.values().contains(outputFormat);
    }

    @Override
    public String getMimeType(String outputFormat) {
        Utilities.ensureNonNull("outputFormat", outputFormat);
        if (mimeTypes.containsKey(outputFormat)) {
            return (String) mimeTypes.get(outputFormat);
        }
        if (mimeTypes.values().contains(outputFormat)) {
            return outputFormat;
        }
        return null;
    }

    @Override
    public String getFileExtension(String outputFormat) {
        if (fileExtensions.containsKey(outputFormat)) {
            return (String) fileExtensions.get(outputFormat);
        }
        if (mimeTypes.values().contains(outputFormat)) {
            return (String) fileExtensions.values().iterator().next();
        }
        return null;
    }

    @Override
    public List<String> getOutputFormats() {
        return new ArrayList<String>(outputFormats);
    }

    /** Default implementation, implementers should override. */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Provides a fallback, a value that looks like a GMLCOV conformance class. Delegates that can
     * be actually associated to an official conformance class should override this method
     */
    public String getConformanceClass(String format) {
        return "http://www.opengis.net/spec/WCS_coverage-encoding-x" + getMimeType(format);
    }
}
