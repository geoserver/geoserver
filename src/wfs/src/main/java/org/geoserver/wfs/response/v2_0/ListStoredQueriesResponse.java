/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.v2_0;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.namespace.QName;
import net.opengis.wfs20.ListStoredQueriesResponseType;
import net.opengis.wfs20.StoredQueryListItemType;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xsd.Encoder;

public class ListStoredQueriesResponse extends WFSResponse {

    public ListStoredQueriesResponse(GeoServer gs) {
        super(gs, ListStoredQueriesResponseType.class);
    }

    @Override
    protected void encode(Encoder encoder, Object value, OutputStream output, Operation op)
            throws IOException, ServiceException {
        // check the returned types, they are qnames and we need to declare their prefixes
        ListStoredQueriesResponseType response = (ListStoredQueriesResponseType) value;
        for (StoredQueryListItemType sq : response.getStoredQuery()) {
            if (sq.getReturnFeatureType() != null) {
                for (QName qName : sq.getReturnFeatureType()) {
                    if (qName.getNamespaceURI() != null && qName.getPrefix() != null) {
                        encoder.getNamespaces()
                                .declarePrefix(qName.getPrefix(), qName.getNamespaceURI());
                    }
                }
            }
        }

        encoder.encode(value, WFS.ListStoredQueriesResponse, output);
    }
}
