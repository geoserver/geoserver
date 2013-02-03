/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import static org.geoserver.web.wicket.GeoServerDataProvider.AbstractProperty;
import static org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class MultipleLayerChooser extends Panel {
    private class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private final CssDemoPage demo;

        public LayerProvider(CssDemoPage demo) {
            this.demo = demo;
        }

        @Override
        public List<LayerInfo> getItems() {
            return demo.catalog().getLayers();
        }

        Property<LayerInfo> workspace = 
            new AbstractProperty<LayerInfo>("Workspace") {
                @Override 
                public Object getPropertyValue(LayerInfo x) {
                    return x.getResource().getStore().getWorkspace().getName();
                }
            };

        Property<LayerInfo> layer = 
            new AbstractProperty<LayerInfo>("Layer") {
                @Override 
                public Object getPropertyValue(LayerInfo x) {
                    return x.getName();
                }
            };

        Property<LayerInfo> associated = 
            new AbstractProperty<LayerInfo>("Associated") {
                @Override
                public Object getPropertyValue(LayerInfo x) {
                    return usesEditedStyle(x);
                }
            };

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, layer, associated);
        }
    }

    protected Boolean usesEditedStyle(LayerInfo l) {
        for (StyleInfo s : l.getStyles()) {
            if (s.getName().equals(demo.getStyleInfo().getName())) return true;
        }
        return l.getDefaultStyle().getName().equals(demo.getStyleInfo().getName());
    }

    private CssDemoPage demo;

    public MultipleLayerChooser(String id, final CssDemoPage demo) {
        super(id);
        this.demo = demo;

        final LayerProvider layerProvider = new LayerProvider(demo);

        GeoServerTablePanel layerTable =
            new GeoServerTablePanel<LayerInfo>("layer.table", layerProvider) {
                @Override 
                public Component getComponentForProperty(
                    String id, IModel value, Property<LayerInfo> property
                ) {
                    final LayerInfo layer = (LayerInfo)value.getObject();
                    String text = property.getPropertyValue(layer).toString();
                    if (property == layerProvider.associated) {
                        IModel<Boolean> model = 
                            new IModel<java.lang.Boolean>() {
                                public Boolean getObject() {
                                    return usesEditedStyle(layer);
                                }

                                public void setObject(java.lang.Boolean b) {
                                    if (b) {
                                        layer.getStyles().add(demo.getStyleInfo());
                                    } else {
                                        if (layer.getDefaultStyle().getName() == demo.getStyleInfo().getName()) {
                                            if (layer.getStyles().size() == 0) {
                                                layer.setDefaultStyle(demo.catalog().getStyleByName("point"));
                                            } else {
                                                StyleInfo s = layer.getStyles().iterator().next();
                                                layer.setDefaultStyle(s);
                                                layer.getStyles().remove(s);
                                            }
                                        } else {
                                            StyleInfo s = null;
                                            for (StyleInfo candidate : layer.getStyles()) {
                                                if (candidate.getName().equals(demo.getStyleInfo().getName())) {
                                                    s = candidate;
                                                    break;
                                                }
                                            }
                                            if (s != null) layer.getStyles().remove(s);
                                        }
                                    }
                                    demo.catalog().save(layer);
                                }
                                public void detach() {}
                            };

                        Fragment fragment = new Fragment(
                            id, "layer.association.checkbox", MultipleLayerChooser.this);
                        fragment.add(new AjaxCheckBox("selected", model) { 
                            public void onUpdate(AjaxRequestTarget target) {} 
                        });
                        return fragment;
                    } else {
                        return new Label(id, text);
                    }
                };
            };
        add(layerTable);
    }
}
