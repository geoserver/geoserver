/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.io.Serial;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class LinkedLayersPanel extends Panel {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

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
    private static final long serialVersionUID = 4556549618384659724L;

    public LinkedLayersPanel(String id, IModel<MetadataTemplate> metadataTemplateModel) {
        super(id);

        add(new GeoServerTablePanel<>("layersTable", new LinkedLayersDataProvider(metadataTemplateModel)) {

            @Serial
            private static final long serialVersionUID = -6805672124565219769L;

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<ResourceInfo> itemModel, Property<ResourceInfo> property) {
                return null;
            }
        });
    }
}
