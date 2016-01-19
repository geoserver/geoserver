/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import static org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class MultipleLayerChooser extends Panel {
    private static final long serialVersionUID = -59522993086560769L;

    private class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        private static final long serialVersionUID = -1800971869092748431L;

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
                private static final long serialVersionUID = -1851109132536014276L;

                @Override 
                public Object getPropertyValue(LayerInfo x) {
                    return x.getResource().getStore().getWorkspace().getName();
                }
            };

        Property<LayerInfo> layer = 
            new AbstractProperty<LayerInfo>("Layer") {
                private static final long serialVersionUID = -1041914399204405146L;

                @Override 
                public Object getPropertyValue(LayerInfo x) {
                    return x.getName();
                }
            };

        Property<LayerInfo> associated = 
            new AbstractProperty<LayerInfo>("Associated") {
                private static final long serialVersionUID = 890930107903888545L;

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

        GeoServerTablePanel<LayerInfo> layerTable =
            new GeoServerTablePanel<LayerInfo>("layer.table", layerProvider) {
                private static final long serialVersionUID = 6100831799966767858L;

                @Override 
                public Component getComponentForProperty(
                    String id, IModel<LayerInfo> value, Property<LayerInfo> property
                ) {
                    final LayerInfo layer = (LayerInfo)value.getObject();
                    String text = property.getPropertyValue(layer).toString();
                    if (property == layerProvider.associated) {
                        IModel<Boolean> model = 
                            new IModel<java.lang.Boolean>() {
                                private static final long serialVersionUID = -5895600269146950033L;
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
                            private static final long serialVersionUID = 3572882767660629935L;

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
