/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
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

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import static org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class LayerChooser extends Panel {

    private static class LayerProvider extends GeoServerDataProvider<FeatureTypeInfo> {
        private CssDemoPage demo;

        public LayerProvider(CssDemoPage demo) {
            this.demo = demo;
        }

        public static Property<FeatureTypeInfo> workspace =
            new AbstractProperty<FeatureTypeInfo>("Workspace") {
                public Object getPropertyValue(FeatureTypeInfo x) {
                    return x.getStore().getWorkspace().getName();
                }
            };

        public static Property<FeatureTypeInfo> store =
            new AbstractProperty<FeatureTypeInfo>("Store") {
                public Object getPropertyValue(FeatureTypeInfo x) {
                    return x.getStore().getName();
                }
            };

        public static Property<FeatureTypeInfo> name =
            new AbstractProperty<FeatureTypeInfo>("Layer") {
                public Object getPropertyValue(FeatureTypeInfo x) {
                    return x.getName();
                }
            };

        @Override
        public List<FeatureTypeInfo> getItems() {
            return demo.catalog().getFeatureTypes();
        }

        @Override
        public List<Property<FeatureTypeInfo>> getProperties() {
            return Arrays.asList(workspace, store, name);
        }
    }

    public LayerChooser(final String id, final CssDemoPage demo) {
        super(id);
        LayerProvider provider = new LayerProvider(demo);
        GeoServerTablePanel<FeatureTypeInfo> table =
            new GeoServerTablePanel<FeatureTypeInfo>("layer.table", provider) {
                @Override
                public Component getComponentForProperty(
                    String id, IModel value, Property<FeatureTypeInfo> property
                ) {
                    final FeatureTypeInfo layer = (FeatureTypeInfo) value.getObject();
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
                                        params.put("layer", layer.getPrefixedName());
                                        params.put("style", demo.getStyleInfo().getName());
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
