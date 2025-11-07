/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (factored out from org.geoserver.geofence.server.web.GeofenceRulePage)
 */
package org.geoserver.acl.plugin.web.accessrules.layerdetails;

import static org.geoserver.acl.domain.rules.LayerDetails.LayerType.LAYERGROUP;
import static org.geoserver.acl.domain.rules.LayerDetails.LayerType.RASTER;
import static org.geoserver.acl.domain.rules.LayerDetails.LayerType.VECTOR;

import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;
import org.geoserver.acl.plugin.web.accessrules.event.PublishedInfoChangeEvent;
import org.geoserver.catalog.PublishedInfo;

@SuppressWarnings("serial")
class LayerTypeFormComponent extends FormComponentPanel<LayerType> {

    private RadioGroup<LayerType> group;
    private Radio<LayerType> unknown, vector, raster, layergroup;

    public LayerTypeFormComponent(String id, IModel<LayerType> model, IModel<PublishedInfo> layerModel) {
        super(id, model);
        setOutputMarkupPlaceholderTag(true);

        group = new RadioGroup<>("layerType", model);
        unknown = new Radio<>("UNKNOWN", Model.of(), group);
        vector = new Radio<>("VECTOR", Model.of(VECTOR), group);
        raster = new Radio<>("RASTER", Model.of(RASTER), group);
        layergroup = new Radio<>("LAYERGROUP", Model.of(LAYERGROUP), group);
        group.add(unknown, vector, raster, layergroup);
        add(group);

        init(layerModel.getObject());
    }

    @Override
    public void convertInput() {
        setConvertedInput(group.getConvertedInput());
    }

    @Override
    public void onEvent(IEvent<?> event) {
        if (event.getPayload() instanceof PublishedInfoChangeEvent) {
            onPublishedInfoChangeEvent((PublishedInfoChangeEvent) event.getPayload());
        }
    }

    private void onPublishedInfoChangeEvent(PublishedInfoChangeEvent event) {
        PublishedInfo info = event.getInfo().orElse(null);
        init(info);
        event.getTarget().add(this);
    }

    private void init(PublishedInfo info) {
        if (null == info) {
            unknown.setEnabled(true);
            disable(vector, raster, layergroup);
        } else {
            switch (info.getType()) {
                case GROUP:
                    layergroup.setEnabled(true);
                    disable(unknown, raster, vector);
                    break;
                case VECTOR:
                    vector.setEnabled(true);
                    disable(unknown, raster, layergroup);
                    break;
                case RASTER:
                case REMOTE:
                case WMS:
                case WMTS:
                    raster.setEnabled(true);
                    disable(unknown, vector, layergroup);
                    break;
                default:
                    unknown.setEnabled(true);
                    disable(vector, raster, layergroup);
                    break;
            }
        }
    }

    private void disable(Radio<?>... options) {
        for (Radio<?> opt : options) {
            opt.setEnabled(false);
        }
    }
}
