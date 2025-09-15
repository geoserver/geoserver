/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.IOException;
import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.util.logging.Logging;

/**
 * Style page tab for displaying layer attributes. Includes a link for changing the current preview layer. Delegates to
 * {@link BandsPanel} or {@link LayerAttributePanel} to display the attributes, depending on the type of the layer
 * resource.
 */
public class LayerAttributePanel extends StyleEditTabPanel {

    static final Logger LOGGER = Logging.getLogger(LayerAttributePanel.class);

    @Serial
    private static final long serialVersionUID = -5936224477909623317L;

    public LayerAttributePanel(String id, AbstractStylePage parent) throws IOException {
        super(id, parent);

        // Change layer link
        PropertyModel<String> layerNameModel = new PropertyModel<>(parent.getLayerModel(), "prefixedName");
        add(new SimpleAjaxLink<>("changeLayer", layerNameModel) {
            @Serial
            private static final long serialVersionUID = 7341058018479354596L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GSModalWindow popup = parent.getPopup();

                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model<>("Choose layer to edit"));
                popup.setContent(new LayerChooser(popup.getContentId(), parent));
                popup.show(target);
            }
        });

        this.setDefaultModel(parent.getLayerModel());

        updateAttributePanel();
    }

    @Override
    protected void configurationChanged() {
        try {
            updateAttributePanel();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not update LayerAttributePanel", e);
        }
    }

    protected void updateAttributePanel() throws IOException {
        ResourceInfo resource = this.getStylePage().getLayerInfo().getResource();

        if (this.get("attributePanel") != null) {
            this.remove("attributePanel");
        }
        if (resource instanceof FeatureTypeInfo info1) {
            this.add(new DataPanel("attributePanel", info1));
        } else if (resource instanceof CoverageInfo info) {
            this.add(new BandsPanel("attributePanel", info));
        }
    }
}
