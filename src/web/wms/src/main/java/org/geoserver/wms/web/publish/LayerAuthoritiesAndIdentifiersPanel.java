/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.List;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;

public class LayerAuthoritiesAndIdentifiersPanel extends Panel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public LayerAuthoritiesAndIdentifiersPanel(
            final String id,
            final boolean isRootLayer,
            final IModel<? extends CatalogInfo> layerModel) {

        super(id);

        // authority URLs for the this layer
        IModel<List<AuthorityURLInfo>> authURLsModel;
        authURLsModel =
                LiveCollectionModel.list(
                        new PropertyModel<List<AuthorityURLInfo>>(layerModel, "authorityURLs"));
        AuthorityURLListEditor authUrlEditor =
                new AuthorityURLListEditor("authorityurls", authURLsModel);
        add(authUrlEditor);

        // Layer Identifiers for this layer
        IModel<List<LayerIdentifierInfo>> identifiersModel;
        identifiersModel =
                LiveCollectionModel.list(
                        new PropertyModel<List<LayerIdentifierInfo>>(layerModel, "identifiers"));

        LayerIdentifierListEditor identifiersEditor =
                new LayerIdentifierListEditor("layerIdentifiers", identifiersModel, authUrlEditor);

        if (!isRootLayer) {
            WMSInfo serviceInfo = WMS.get().getServiceInfo();
            List<AuthorityURLInfo> rootLayerAuthorities = serviceInfo.getAuthorityURLs();
            identifiersEditor.setBaseAuthorities(rootLayerAuthorities);
        }
        add(identifiersEditor);
    }
}
