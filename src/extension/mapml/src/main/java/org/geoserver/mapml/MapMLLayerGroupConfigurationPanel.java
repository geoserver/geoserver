/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_MULTIEXTENTS;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.geoserver.mapml.MapMLLayerConfigurationPanel.getAvailableMimeTypes;

import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.util.logging.Logging;

/**
 * LayerGroup configuration panel for MapML
 *
 * @author prushforth
 */
public class MapMLLayerGroupConfigurationPanel extends PublishedConfigurationPanel<LayerGroupInfo> {
    static final Logger LOGGER = Logging.getLogger(MapMLLayerGroupConfigurationPanel.class);

    private static final long serialVersionUID = 1L;
    public static final String METADATA = "metadata";

    DropDownChoice<String> mime;

    /**
     * Adds MapML configuration panel
     *
     * @param panelId
     * @param model
     */
    public MapMLLayerGroupConfigurationPanel(final String panelId, final IModel<LayerGroupInfo> model) {
        super(panelId, model);

        MapModel<String> licenseTitleModel =
                new MapModel<>(new PropertyModel<>(model, METADATA), MapMLConstants.LICENSE_TITLE);
        TextField<String> licenseTitle = new TextField<>("licenseTitle", licenseTitleModel);
        add(licenseTitle);

        MapModel<String> licenseLinkModel =
                new MapModel<>(new PropertyModel<>(model, METADATA), MapMLConstants.LICENSE_LINK);
        TextField<String> licenseLink = new TextField<>("licenseLink", licenseLinkModel);
        add(licenseLink);

        // add the checkbox to select tiled or not
        MapModel<Boolean> useTilesModel = new MapModel<>(new PropertyModel<>(model, METADATA), MAPML_USE_TILES);
        CheckBox useTiles = new CheckBox("useTiles", useTilesModel);
        useTiles.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                ajaxRequestTarget.add(mime);
                boolean useTilesChecked = useTiles.getConvertedInput();
                mime.setChoices(getAvailableMimeTypes(model.getObject(), useTilesChecked));
            }
        });
        add(useTiles);

        // add the checkbox to select features or not
        MapModel<Boolean> useFeaturesModel =
                new MapModel<>(new PropertyModel<>(model, METADATA), MapMLConstants.MAPML_USE_FEATURES);
        CheckBox useFeatures = new CheckBox(MapMLConstants.USE_FEATURES, useFeaturesModel);
        useFeatures.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(mime);
                // if we are using features, we don't use a default mime type
                mime.setEnabled(!useFeatures.getConvertedInput());
            }
        });
        add(useFeatures);

        // add the checkbox to expose sub-layers / set usemultiextents:true or :false
        MapModel<Boolean> useMultiExtentsModel =
                new MapModel<>(new PropertyModel<>(model, METADATA), MAPML_USE_MULTIEXTENTS);
        CheckBox useMultiExtents = new CheckBox("useMultiExtents", useMultiExtentsModel);
        add(useMultiExtents);

        MapModel<String> mimeModel = new MapModel<>(new PropertyModel<>(model, METADATA), MapMLConstants.MAPML_MIME);
        boolean useTilesFromModel =
                Boolean.TRUE.equals(model.getObject().getMetadata().get(MAPML_USE_TILES, Boolean.class));
        mime = new DropDownChoice<>(
                MapMLConstants.MIME, mimeModel, getAvailableMimeTypes(model.getObject(), useTilesFromModel));
        mime.setOutputMarkupId(true);
        mime.setNullValid(false);
        // if we are using features, we don't use a mime type
        if (useFeaturesModel.getObject() != null) {
            String useFeaturesString = String.valueOf(useFeaturesModel.getObject());
            boolean useFeaturesBoolean = Boolean.parseBoolean(useFeaturesString);
            mime.setEnabled(!useFeaturesBoolean);
        }
        add(mime);
    }
}
