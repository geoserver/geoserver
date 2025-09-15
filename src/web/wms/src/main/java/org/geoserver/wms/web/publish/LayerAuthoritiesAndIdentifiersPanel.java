/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 1L;

    public LayerAuthoritiesAndIdentifiersPanel(
            final String id, final boolean isRootLayer, final IModel<? extends CatalogInfo> layerModel) {

        super(id);

        // authority URLs for the this layer
        IModel<List<AuthorityURLInfo>> authURLsModel =
                LiveCollectionModel.list(new PropertyModel<>(layerModel, "authorityURLs"));
        AuthorityURLListEditor authUrlEditor = new AuthorityURLListEditor("authorityurls", authURLsModel);
        add(authUrlEditor);

        // Layer Identifiers for this layer
        IModel<List<LayerIdentifierInfo>> identifiersModel =
                LiveCollectionModel.list(new PropertyModel<>(layerModel, "identifiers"));

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
