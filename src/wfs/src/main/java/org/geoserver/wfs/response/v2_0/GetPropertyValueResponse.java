/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.v2_0;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.namespace.QName;

import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.ValueCollectionType;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xml.Encoder;

public class GetPropertyValueResponse extends WFSResponse {

    public GetPropertyValueResponse(GeoServer gs) {
        super(gs, ValueCollectionType.class);
    }

    @Override
    protected void encode(Encoder encoder, Object value, OutputStream output, Operation op) 
        throws IOException, ServiceException {

        GetPropertyValueType request = (GetPropertyValueType) op.getParameters()[0];
        QueryType query = (QueryType) request.getAbstractQueryExpression();
        QName typeName = (QName) query.getTypeNames().get(0);
        
        NamespaceInfo ns = gs.getCatalog().getNamespaceByURI(typeName.getNamespaceURI());
        
        encoder.getNamespaces().declarePrefix(ns.getPrefix(), ns.getURI());
        encoder.encode(value, WFS.ValueCollection, output);
    }

}
