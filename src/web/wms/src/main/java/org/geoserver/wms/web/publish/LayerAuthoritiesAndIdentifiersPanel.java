/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.List;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;

public class LayerAuthoritiesAndIdentifiersPanel extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(LayerAuthoritiesAndIdentifiersPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Configure layer authority information.
     *
     * <p>This panel works with both PublishedInfo and WMSInfo models that contain the necessary {@code authorityURLs}
     * and {@code identifiers} properties. While these do not have a common interface describing these methods, they do
     * both extend {@code Info}.
     *
     * @param id Panel wicket id
     * @param isRootLayer Indicates WMSInfo model should be looked up to configure settings for the root layer.
     * @param layerModel Layer model, GroupLayer model, or WMSInfo model.
     */
    public LayerAuthoritiesAndIdentifiersPanel(
            final String id, final boolean isRootLayer, final IModel<? extends Info> layerModel) {

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
