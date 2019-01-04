/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geotools.util.logging.Logging;

/**
 * Base class for GetFeatureInfo delegates responsible of creating GetFeatureInfo responses in
 * different formats.
 *
 * <p>Subclasses should implement one or more output formats, wich will be returned in a list of
 * mime type strings in <code>getSupportedFormats</code>. For example, a subclass can be created to
 * write one of the following output formats:
 *
 * <ul>
 *   <li>text/plain
 *   <li>text/html
 * </ul>
 *
 * <p>This abstract class takes care of executing the request in the sense of taking the
 * GetFeatureInfo request parameters such as query_layers, bbox, x, y, etc., create the gt2 query
 * objects for each featuretype and executing it. This process leads to a set of FeatureResults
 * objects and its metadata, wich will be given to the <code>execute(FeatureTypeInfo[] ,
 * FeatureResults[])</code> method, that a subclass should implement as a matter of setting up any
 * resource/state it needs to later encoding.
 *
 * <p>So, it should be enough to a subclass to implement the following methods in order to produce
 * the requested output format:
 *
 * <ul>
 *   <li>execute(FeatureTypeInfo[], FeatureResults[], int, int)
 *   <li>canProduce(String mapFormat)
 *   <li>getSupportedFormats()
 *   <li>writeTo(OutputStream)
 * </ul>
 *
 * @author Gabriel Roldan
 * @author Chris Holmes
 * @version $Id$
 */
public abstract class GetFeatureInfoOutputFormat {

    /** A logger for this class. */
    protected static final Logger LOGGER = Logging.getLogger(GetFeatureInfoOutputFormat.class);

    private final String contentType;

    public GetFeatureInfoOutputFormat(final String contentType) {
        this.contentType = contentType;
    }

    public abstract void write(
            FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException;

    /**
     * Evaluates if this GetFeatureInfo producer can generate the map format specified by <code>
     * mapFormat</code>, where <code>mapFormat</code> is the MIME type of the requested response.
     *
     * @param mapFormat the MIME type of the required output format, might be {@code null}
     * @return true if class can produce a map in the passed format
     */
    public boolean canProduce(String mapFormat) {
        return this.contentType.equalsIgnoreCase(mapFormat);
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the charset for this outputFormat. The default implementation returns <code>null
     * </code>, in this case no encoding should be set. Subclasses returning text documents
     * (CSV,HTML,JSON) should override taking into account SettingsInfo.getCharset() as well as the
     * specific encoding requirements of the returned format.
     */
    public String getCharset() {
        return null;
    }
}
