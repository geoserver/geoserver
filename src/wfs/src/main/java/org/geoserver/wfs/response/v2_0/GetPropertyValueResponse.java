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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

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
        Catalog catalog = gs.getCatalog();

        // determine if the queried feature type is simple or complex
        boolean isSimple = true;
        Name featureTypeName = new NameImpl(typeName.getNamespaceURI(), typeName.getLocalPart());
        FeatureTypeInfo ftInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (ftInfo != null) {
            try {
                isSimple = ftInfo.getFeatureType() instanceof SimpleFeatureType;
            } catch (Exception e) {
                // ignore broken feature types
            }
        }

        if (isSimple) {
            NamespaceInfo ns = catalog.getNamespaceByURI(typeName.getNamespaceURI());
            encoder.getNamespaces().declarePrefix(ns.getPrefix(), ns.getURI());
        } else {
            // complex features may contain elements belonging to any namespace in the catalog,
            // so we better have them all declared in the encoder's namespace context
            WorkspaceInfo localWorkspace = LocalWorkspace.get();
            if (localWorkspace != null) {
                // deactivate workspace filtering
                LocalWorkspace.remove();
            }
            try {
                for (NamespaceInfo nameSpaceinfo : catalog.getNamespaces()) {
                    if (encoder.getNamespaces().getURI(nameSpaceinfo.getPrefix()) == null) {
                        encoder.getNamespaces()
                                .declarePrefix(nameSpaceinfo.getPrefix(), nameSpaceinfo.getURI());
                    }
                }
            } finally {
                // make sure local workspace filtering is repositioned
                LocalWorkspace.set(localWorkspace);
            }
        }

        encoder.encode(value, WFS.ValueCollection, output);
    }
}
