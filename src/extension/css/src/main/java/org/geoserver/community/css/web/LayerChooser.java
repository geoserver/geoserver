/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class LayerChooser extends Panel {

    private static class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private CssDemoPage demo;

        public LayerProvider(CssDemoPage demo) {
            this.demo = demo;
        }

        public static Property<LayerInfo> workspace =
            new AbstractProperty<LayerInfo>("Workspace") {
                public Object getPropertyValue(LayerInfo x) {
                    return x.getResource().getStore().getWorkspace().getName();
                }
            };

        public static Property<LayerInfo> store =
            new AbstractProperty<LayerInfo>("Store") {
                public Object getPropertyValue(LayerInfo x) {
                    return x.getResource().getStore().getName();
                }
            };

        public static Property<LayerInfo> name =
            new AbstractProperty<LayerInfo>("Layer") {
                public Object getPropertyValue(LayerInfo x) {
                    return x.getName();
                }
            };

        @Override
        public List<LayerInfo> getItems() {
            return demo.catalog().getLayers();
        }

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, store, name);
        }
    }

    public LayerChooser(final String id, final CssDemoPage demo) {
        super(id);
        LayerProvider provider = new LayerProvider(demo);
        GeoServerTablePanel<LayerInfo> table =
            new GeoServerTablePanel<LayerInfo>("layer.table", provider) {
                @Override
                public Component getComponentForProperty(
                    String id, IModel value, Property<LayerInfo> property
                ) {
                    final LayerInfo layer = (LayerInfo) value.getObject();
                    final String text = property.getPropertyValue(layer).toString();

                    if (property == LayerProvider.name) {
                        return new Fragment(id, "layer.link", LayerChooser.this) {
                            {
                                add(new AjaxLink("link") {
                                    {
                                        add(new Label("layer.name", new Model(text)));
                                    }
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        PageParameters params = new PageParameters();
                                        params.put("layer", layer.prefixedName());
                                        WorkspaceInfo workspace= demo.getStyleInfo().getWorkspace();
                                        if (workspace == null) {
                                            params.put("style", demo.getStyleInfo().getName());
                                        } else {
                                            params.put("style", workspace.getName() + ":" + demo.getStyleInfo().getName());
                                        }
                                        setResponsePage(CssDemoPage.class, params);
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
