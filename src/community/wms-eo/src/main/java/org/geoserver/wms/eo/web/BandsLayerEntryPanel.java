/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.wms.eo.EoLayerType;

/** Allows to edit the Band Coverage Layer */
@SuppressWarnings("serial")
public class BandsLayerEntryPanel extends Panel {

    private LayerInfo layer;
    private StyleInfo layerStyle;

    @SuppressWarnings({"rawtypes"})
    public BandsLayerEntryPanel(String id, final Form form, WorkspaceInfo workspace) {
        super(id);
        setOutputMarkupId(true);

        LayerGroupInfo group = (LayerGroupInfo) form.getModel().getObject();

        int pos = 0;
        for (PublishedInfo p : group.getLayers()) {
            if (p instanceof LayerInfo) {
                if (EoLayerType.BAND_COVERAGE
                        .name()
                        .equals(((LayerInfo) p).getMetadata().get(EoLayerType.KEY))) {
                    layer = (LayerInfo) p;
                    layerStyle = group.getStyles().get(pos);
                }
            }
            pos++;
        }

        Link link =
                new Link("bandsLayer") {
                    @Override
                    public void onClick() {
                        PageParameters pp = new PageParameters();
                        if (layer.getResource().getStore().getWorkspace() != null) {
                            pp.add(
                                    ResourceConfigurationPage.WORKSPACE,
                                    layer.getResource().getStore().getWorkspace().getName());
                        }
                        pp.add(ResourceConfigurationPage.NAME, layer.getName());
                        setResponsePage(ResourceConfigurationPage.class, pp);
                    }
                };
        link.add(new Label("bandsLayerName", new PropertyModel(layer, "name")));
        add(link);

        // global styles
        List<StyleInfo> globalStyles = new ArrayList<StyleInfo>();
        List<StyleInfo> allStyles = GeoServerApplication.get().getCatalog().getStyles();
        for (StyleInfo s : allStyles) {
            if (s.getWorkspace() == null) {
                globalStyles.add(s);
            }
        }

        // available styles
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        styles.addAll(globalStyles);
        if (workspace != null) {
            styles.addAll(GeoServerApplication.get().getCatalog().getStylesByWorkspace(workspace));
        }
        Collections.sort(styles, new StyleInfoNameComparator());

        DropDownChoice<StyleInfo> styleField =
                new DropDownChoice<StyleInfo>(
                        "bandsLayerStyle",
                        new PropertyModel<StyleInfo>(this, "layerStyle"),
                        styles) {
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        return form.getConverter(type);
                    }
                };
        styleField.setNullValid(true);
        styleField.setOutputMarkupId(true);
        add(styleField);
    }

    public void setLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public void setLayerStyle(StyleInfo layerStyle) {
        this.layerStyle = layerStyle;
    }

    public LayerInfo getLayer() {
        return layer;
    }

    public StyleInfo getLayerStyle() {
        return layerStyle;
    }
}
