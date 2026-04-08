/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;

/** Configures {@link LayerGroupInfo} WMS specific attributes */
public class LayerAuthoritiesAndIdentifiersConfigPanel extends PublishedConfigurationPanel<PublishedInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(LayerAuthoritiesAndIdentifiersConfigPanel.class);

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
    private static final long serialVersionUID = 8652096571563162644L;

    public LayerAuthoritiesAndIdentifiersConfigPanel(String id, IModel<? extends PublishedInfo> layerGroupModel) {
        super(id, layerGroupModel);

        // authority URLs and identifiers for this layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds =
                new LayerAuthoritiesAndIdentifiersPanel("authoritiesAndIds", false, layerGroupModel);
        add(authAndIds);
    }
}
