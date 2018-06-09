/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Panel for selecting a layer from the list of layers. Used by {@link LayerAttributePanel} and
 * {@link OpenLayersPreviewPanel} to change the preview layer.
 */
public class LayerChooser extends Panel {

    private static final long serialVersionUID = -127345071729297975L;

    private static class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private static final long serialVersionUID = -2117784735301652240L;

        private AbstractStylePage parent;

        public LayerProvider(AbstractStylePage parent) {
            this.parent = parent;
        }

        public static Property<LayerInfo> workspace =
                new AbstractProperty<LayerInfo>("Workspace") {
                    private static final long serialVersionUID = -7055816211775541759L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getResource().getStore().getWorkspace().getName();
                    }
                };

        public static Property<LayerInfo> store =
                new AbstractProperty<LayerInfo>("Store") {
                    private static final long serialVersionUID = -4021230907568644439L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getResource().getStore().getName();
                    }
                };

        public static Property<LayerInfo> name =
                new AbstractProperty<LayerInfo>("Layer") {
                    private static final long serialVersionUID = 8913729089849537790L;

                    public Object getPropertyValue(LayerInfo x) {
                        return x.getName();
                    }
                };

        @Override
        public List<LayerInfo> getItems() {
            List<LayerInfo> items = new ArrayList<LayerInfo>();
            for (LayerInfo l : parent.getCatalog().getLayers()) {
                if (l.getResource() instanceof FeatureTypeInfo) {
                    items.add(l);
                }
                if (l.getResource() instanceof CoverageInfo) {
                    items.add(l);
                }
            }
            return items;
        }

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, store, name);
        }
    }

    public LayerChooser(final String id, final AbstractStylePage parent) {
        super(id);
        LayerProvider provider = new LayerProvider(parent);
        GeoServerTablePanel<LayerInfo> table =
                new GeoServerTablePanel<LayerInfo>("layer.table", provider) {
                    private static final long serialVersionUID = 1196129584558094662L;

                    @Override
                    public Component getComponentForProperty(
                            String id, IModel<LayerInfo> value, Property<LayerInfo> property) {
                        final LayerInfo layer = (LayerInfo) value.getObject();
                        final String text = property.getPropertyValue(layer).toString();

                        if (property == LayerProvider.name) {
                            return new Fragment(id, "layer.link", LayerChooser.this) {
                                private static final long serialVersionUID = -7619814477490657757L;

                                {
                                    add(
                                            new GeoServerAjaxFormLink("link", parent.styleForm) {
                                                {
                                                    add(
                                                            new Label(
                                                                    "layer.name",
                                                                    new Model<String>(text)));
                                                }

                                                private static final long serialVersionUID =
                                                        8020574396677784792L;

                                                @Override
                                                protected void onClick(
                                                        AjaxRequestTarget target, Form<?> form) {
                                                    parent.getLayerModel().setObject(layer);
                                                    parent.getPopup().close(target);
                                                    parent.configurationChanged();
                                                    parent.addFeedbackPanels(target);
                                                    target.add(parent.styleForm);
                                                }

                                                @Override
                                                public boolean getDefaultFormProcessing() {
                                                    return false;
                                                }
                                            });
                                }
                            };
                        } else {
                            return new Label(id, text);
                        }
                    }
                };
        add(table);
    }
}
