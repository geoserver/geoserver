/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.net.URI;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

/**
 * Resource configuration panel for MapML
 */
public class MapMLConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 1L;

    /**
     * Adds MapML configuration panel 
     */
    public MapMLConfigurationPanel(final String panelId, final IModel<LayerInfo> model) {
        super(panelId, model);

        MapModel<String> licenseTitleModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.licenseTitle");
        TextField<String> licenseTitle = new TextField<String>("licenseTitle", licenseTitleModel);
        add(licenseTitle);

        MapModel<String> licenseLinkModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.licenseLink");
        TextField<String> licenseLink = new TextField<String>("licenseLink", licenseLinkModel);
        add(licenseLink);

        // add the checkbox to select tiled or not
        MapModel<Boolean> useTilesModel =
                new MapModel<Boolean>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.useTiles");
        CheckBox useTiles = new CheckBox("useTiles", useTilesModel);
        add(useTiles);

        
    }

}
