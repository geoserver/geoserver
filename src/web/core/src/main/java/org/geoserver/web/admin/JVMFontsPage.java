/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.geoserver.web.admin.PreviewFontProvider.PREVIEW_IMAGE;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Shows a paged list of the fonts that are available to the JVM
 *
 * @author Miles Jordan, Australian Antarctic Division
 */
public class JVMFontsPage extends ServerAdminPage {

    PreviewFontProvider provider = new PreviewFontProvider();

    GeoServerTablePanel<PreviewFont> table;

    public JVMFontsPage() {
        updateModel();
    }

    @SuppressWarnings("serial")
    private void updateModel() {
        table =
                new GeoServerTablePanel<PreviewFont>("table", provider) {
                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<PreviewFont> itemModel,
                            Property<PreviewFont> property) {
                        PreviewFont previewFont = itemModel.getObject();

                        if (property == PREVIEW_IMAGE) {
                            BufferedDynamicImageResource image = previewFont.getPreviewImage();
                            Fragment f =
                                    new Fragment(id, "previewImageFragment", JVMFontsPage.this);
                            f.add(new Image("previewImage", image));
                            return f;
                        }
                        return null;
                    }
                };
        table.setOutputMarkupId(true);
        add(table);
    }
}
