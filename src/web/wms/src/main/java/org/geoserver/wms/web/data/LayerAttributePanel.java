package org.geoserver.wms.web.data;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.util.logging.Logging;

public class LayerAttributePanel extends StyleEditTabPanel {

    static final Logger LOGGER = Logging.getLogger(LayerAttributePanel.class);

    private static final long serialVersionUID = -5936224477909623317L;

    public LayerAttributePanel(String id, AbstractStylePage parent) throws IOException {
        super(id, parent);
        
        //Change layer link
        PropertyModel<String> layerNameModel = new PropertyModel<String>(parent.getLayerModel(),"prefixedName");
        add(new SimpleAjaxLink<String>("changeLayer", layerNameModel) {
            private static final long serialVersionUID = 7341058018479354596L;

            public void onClick(AjaxRequestTarget target) {
                ModalWindow popup = parent.getPopup();
                
                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model<String>("Choose layer to edit"));
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
        if (resource instanceof FeatureTypeInfo) {
            this.add(new DataPanel("attributePanel", (FeatureTypeInfo) resource));
        } else if (resource instanceof CoverageInfo) {
            this.add(new BandsPanel("attributePanel", (CoverageInfo) resource));
        }
    }

}
