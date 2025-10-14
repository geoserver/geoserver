/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Style page tab for configuring style-layer associations. Lists all layers, and allows setting the current style as
 * the default style for each layer, or as an associated style.
 */
public class LayerAssociationPanel extends StyleEditTabPanel {
    @Serial
    private static final long serialVersionUID = -59522993086560769L;

    private class LayerProvider extends GeoServerDataProvider<LayerInfo> {
        @Serial
        private static final long serialVersionUID = -1800971869092748431L;

        @Override
        public List<LayerInfo> getItems() {
            return getStylePage().getCatalog().getLayers();
        }

        Property<LayerInfo> workspace = new AbstractProperty<>("Workspace") {
            @Serial
            private static final long serialVersionUID = -1851109132536014276L;

            @Override
            public Object getPropertyValue(LayerInfo x) {
                return x.getResource().getStore().getWorkspace().getName();
            }
        };

        Property<LayerInfo> layer = new AbstractProperty<>("Layer") {
            @Serial
            private static final long serialVersionUID = -1041914399204405146L;

            @Override
            public Object getPropertyValue(LayerInfo x) {
                return x.getName();
            }
        };

        Property<LayerInfo> defaultStyle = new AbstractProperty<>("Default") {

            @Override
            public Object getPropertyValue(LayerInfo x) {
                return defaultEditedStyle(x);
            }
        };

        Property<LayerInfo> associatedStyle = new AbstractProperty<>("Associated") {
            @Serial
            private static final long serialVersionUID = 890930107903888545L;

            @Override
            public Object getPropertyValue(LayerInfo x) {
                return usesEditedStyle(x);
            }
        };

        @Override
        public List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(workspace, layer, defaultStyle, associatedStyle);
        }
    }

    protected Boolean usesEditedStyle(LayerInfo l) {
        for (StyleInfo s : l.getStyles()) {
            if (s.getName().equals(getStylePage().getStyleInfo().getName())) return true;
        }
        return false;
    }

    protected Boolean defaultEditedStyle(LayerInfo l) {
        StyleInfo s = l.getDefaultStyle();
        if (s != null) {
            return s.getName().equals(getStylePage().getStyleInfo().getName());
        }
        return false;
    }

    public LayerAssociationPanel(String id, AbstractStylePage parent) {
        super(id, parent);

        final LayerProvider layerProvider = new LayerProvider();

        GeoServerTablePanel<LayerInfo> layerTable = new GeoServerTablePanel<>("layer.table", layerProvider) {
            @Serial
            private static final long serialVersionUID = 6100831799966767858L;

            @Override
            public Component getComponentForProperty(String id, IModel<LayerInfo> value, Property<LayerInfo> property) {
                final LayerInfo layer = value.getObject();
                String text = property.getPropertyValue(layer).toString();
                if (property == layerProvider.defaultStyle) {
                    IModel<Boolean> model = new DefaultStyleModel(layer, parent);

                    Fragment fragment = new Fragment(id, "layer.default.checkbox", LayerAssociationPanel.this);
                    fragment.add(new AjaxCheckBox("default.selected", model) {
                        @Serial
                        private static final long serialVersionUID = 3572882767660629935L;

                        @Override
                        public void onUpdate(AjaxRequestTarget target) {}
                    });
                    return fragment;
                } else if (property == layerProvider.associatedStyle) {
                    IModel<Boolean> model = new AssociatedStyleModel(layer, parent);

                    Fragment fragment = new Fragment(id, "layer.association.checkbox", LayerAssociationPanel.this);
                    fragment.add(new AjaxCheckBox("association.selected", model) {
                        @Serial
                        private static final long serialVersionUID = 3572882767660629935L;

                        @Override
                        public void onUpdate(AjaxRequestTarget target) {}
                    });
                    return fragment;
                } else {
                    return new Label(id, text);
                }
            }
        };
        add(layerTable);
    }

    private class DefaultStyleModel implements IModel<Boolean> {
        @Serial
        private static final long serialVersionUID = -5895600269146950033L;

        private final LayerInfo layer;
        private final AbstractStylePage parent;

        public DefaultStyleModel(LayerInfo layer, AbstractStylePage parent) {
            this.layer = layer;
            this.parent = parent;
        }

        @Override
        public Boolean getObject() {
            return defaultEditedStyle(layer);
        }

        @Override
        public void setObject(Boolean b) {
            if (b) {
                layer.setDefaultStyle(parent.getStyleInfo());
            } else {
                if (layer.getStyles().isEmpty()) {
                    layer.setDefaultStyle(parent.getCatalog().getStyleByName("generic"));
                } else {
                    StyleInfo s = layer.getStyles().iterator().next();
                    layer.setDefaultStyle(s);
                }
            }
            parent.getCatalog().save(layer);
        }
    }

    private class AssociatedStyleModel implements IModel<Boolean> {
        @Serial
        private static final long serialVersionUID = -5895600269146950033L;

        private final LayerInfo layer;
        private final AbstractStylePage parent;

        public AssociatedStyleModel(LayerInfo layer, AbstractStylePage parent) {
            this.layer = layer;
            this.parent = parent;
        }

        @Override
        public Boolean getObject() {
            return usesEditedStyle(layer);
        }

        @Override
        public void setObject(Boolean b) {
            if (b) {
                layer.getStyles().add(parent.getStyleInfo());
            } else {
                StyleInfo s = null;
                for (StyleInfo candidate : layer.getStyles()) {
                    if (candidate.getName().equals(parent.getStyleInfo().getName())) {
                        s = candidate;
                        break;
                    }
                }
                if (s != null) layer.getStyles().remove(s);
            }
            parent.getCatalog().save(layer);
        }
    }
}
