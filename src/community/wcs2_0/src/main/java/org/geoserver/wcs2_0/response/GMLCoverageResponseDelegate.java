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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Encoding a {@link GridCoverage2D} as per WCS 2.0 GML format.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GMLCoverageResponseDelegate implements CoverageResponseDelegate {

    /** FILE_EXTENSION */
    private static final String FILE_EXTENSION = "gml";
    
    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/gml+xml";
    
    /** FORMAT_ALIASES */
    private static final List<String> FORMAT_ALIASES = Arrays.asList(FILE_EXTENSION,MIME_TYPE);
    
    final static String SRS_STARTER="http://www.opengis.net/def/crs/EPSG/0/";
    
    /** Can be used to map dimensions name to indexes*/
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;
    


    public GMLCoverageResponseDelegate(EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.envelopeDimensionsMapper=envelopeDimensionsMapper;
        
    }

    @Override
    public boolean canProduce(String outputFormat) {
        return FORMAT_ALIASES.contains(outputFormat);
    }

    @Override
    public String getMimeType(String outputFormat) {
        return MIME_TYPE;
    }

    @Override
    public String getFileExtension(String outputFormat) {
        return FILE_EXTENSION;
    }

    @Override
    public void encode(GridCoverage2D coverage, String outputFormat,
            Map<String, String> econdingParameters, OutputStream output) throws ServiceException,
            IOException {
        
        
        final GMLTransformer transformer= new GMLTransformer(envelopeDimensionsMapper);
        transformer.setIndentation(4);
        try {
            transformer.transform(coverage, output);
        } catch (TransformerException e) {
            new WcsException(e);
        }

    }

    @Override
    public List<String> getOutputFormats() {
        return FORMAT_ALIASES;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
