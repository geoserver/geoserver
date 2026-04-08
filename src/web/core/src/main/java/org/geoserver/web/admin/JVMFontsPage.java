/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.geoserver.web.admin.PreviewFontProvider.PREVIEW_IMAGE;
import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Shows a paged list of the fonts that are available to the JVM
 *
 * @author Miles Jordan, Australian Antarctic Division
 */
public class JVMFontsPage extends ServerAdminPage {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(JVMFontsPage.class);

    PreviewFontProvider provider = new PreviewFontProvider();

    GeoServerTablePanel<PreviewFont> table;

    public JVMFontsPage() {
        updateModel();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        if (!isCssEmpty) {
            response.render(CssHeaderItem.forReference(
                    new PackageResourceReference(getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    @SuppressWarnings("serial")
    private void updateModel() {
        table = new GeoServerTablePanel<>("table", provider) {
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<PreviewFont> itemModel, Property<PreviewFont> property) {
                PreviewFont previewFont = itemModel.getObject();

                if (property == PREVIEW_IMAGE) {
                    BufferedDynamicImageResource image = previewFont.getPreviewImage();
                    Fragment f = new Fragment(id, "previewImageFragment", JVMFontsPage.this);
                    Image preview = new Image("previewImage", image);
                    preview.add(AttributeModifier.append("class", "gs-font-preview"));
                    f.add(preview);
                    return f;
                }
                return null;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }
}
