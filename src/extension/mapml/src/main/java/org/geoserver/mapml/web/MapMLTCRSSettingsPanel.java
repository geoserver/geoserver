/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.util.MetadataMapModel;

public class MapMLTCRSSettingsPanel extends Panel {
    public MapMLTCRSSettingsPanel(String id, IModel<SettingsInfo> settingsInfoIModel) {
        super(id, settingsInfoIModel);

        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(settingsInfoIModel, "metadata");

        MetadataMap metadataMap = metadata.getObject();

        // Initialize List if missing
        if (metadataMap != null && !metadataMap.containsKey(TiledCRSConstants.TCRS_METADATA_KEY)) {
            metadataMap.put(TiledCRSConstants.TCRS_METADATA_KEY, new ArrayList<>());
        }

        MetadataMapModel metadataModel =
                new MetadataMapModel<>(metadataMap, TiledCRSConstants.TCRS_METADATA_KEY, List.class);

        MapMLGridsets mapMlGridsets = (MapMLGridsets) GeoServerExtensions.bean("mapMLGridsets");
        List<String> names = mapMlGridsets.getCandidateGridSets();
        IModel<List<String>> availableGridSetsModel = Model.ofList(names);
        @SuppressWarnings("unchecked")
        Palette tcrsSelector =
                new Palette<String>("tcrspalette", metadataModel, availableGridSetsModel, new TCSRenderer(), 7, false) {

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("MapMLTCRSPanel.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("MapMLTCRSPanel.availableHeader"));
                    }
                };
        tcrsSelector.add(new DefaultTheme());
        add(tcrsSelector);
    }

    static class TCSRenderer extends ChoiceRenderer<String> {

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }

        @Override
        public Object getDisplayValue(String object) {
            return object;
        }
    }
}
