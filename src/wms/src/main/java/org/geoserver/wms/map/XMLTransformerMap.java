/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.xml.transform.TransformerBase;

/**
 * A {@link WebMap} that's represented by a geotools {@link TransformerBase}, got xml output formats
 *
 * @author Gabriel Roldan
 * @see XMLTransformerMapResponse
 */
public class XMLTransformerMap extends WebMap {

    private TransformerBase transformer;

    private Object transformerSubject;

    /**
     * @param transformer the transformer that writes to the response stream
     * @param subject the object to be passed down to the transformer, might be {@code null} at the
     *     user's choice
     * @param mimeType the MIME-Type to be declared in the response
     */
    public XMLTransformerMap(
            final WMSMapContent mapContent,
            final TransformerBase transformer,
            final Object subject,
            final String mimeType) {
        super(mapContent);
        this.transformer = transformer;
        this.transformerSubject = subject;
        setMimeType(mimeType);
    }

    /** @return the xml transformer that writes to the destination output stream */
    public TransformerBase getTransformer() {
        return transformer;
    }

    /** @return the object to be passed down to the transformer */
    public Object getTransformerSubject() {
        return transformerSubject;
    }
}
