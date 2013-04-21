/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.data.layergroup.LayerGroupEntry;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.wms.eo.EoLayerType;


/**
 * Allows to edit the list of layers contained in a layer group
 */
public class EoLayerGroupEntryPanel extends Panel {

    private ModalWindow popupWindow;
    private LayerGroupEntryProvider entryProvider;
    private GeoServerTablePanel<LayerGroupEntry> layerTable;
    private List<LayerGroupEntry> items;

    
    public EoLayerGroupEntryPanel(String id, final LayerGroupInfo layerGroup) {
        super(id);

        items = new ArrayList<LayerGroupEntry>();
        for (int i = 0; i < layerGroup.getLayers().size(); i++) {
            PublishedInfo layer = layerGroup.getLayers().get(i);
            if (layer instanceof LayerInfo) {
                String type = (String) layer.getMetadata().get(EoLayerType.KEY);
                if (EoLayerType.BAND_COVERAGE.name().equals(type) ||
                        EoLayerType.COVERAGE_OUTLINE.name().equals(type)) {
                    // skip this layer
                    continue;
                }
            }
            
            StyleInfo style = layerGroup.getStyles().get(i);
            items.add(new LayerGroupEntry(layer, style));
        }

        add(popupWindow = new ModalWindow("popup"));

        // layers
        entryProvider = new LayerGroupEntryProvider(items);
        add(layerTable = new GeoServerTablePanel<LayerGroupEntry>("layers", entryProvider) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel, Property<LayerGroupEntry> property) {
                if (property == LayerGroupEntryProvider.LAYER) {
                    return layerLink(id, itemModel);
                }
                if (property == LayerGroupEntryProvider.DEFAULT_STYLE) {
                    return defaultStyleCheckbox(id, itemModel);
                }
                if (property == LayerGroupEntryProvider.STYLE) {
                    return styleLink(id, itemModel);
                }
                if (property == LayerGroupEntryProvider.REMOVE) {
                    return removeLink(id, itemModel);
                }
                if (property == LayerGroupEntryProvider.POSITION) {
                    return positionPanel(id, itemModel);
                }

                return null;
            }
        }.setFilterable(false));
        layerTable.setOutputMarkupId(true);

        final Panel addLayerPanel = new AddEoLayerPanel(popupWindow.getContentId(), layerGroup) {
            @Override
            protected void onCancel(AjaxRequestTarget target) {
                popupWindow.close(target);
            }
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, LayerInfo maskLayer, LayerInfo paramsLayer) {
                if (maskLayer != null) {
                    entryProvider.getItems().add(new LayerGroupEntry(maskLayer, maskLayer.getDefaultStyle()));
                }                       
                
                if (paramsLayer != null) {
                    entryProvider.getItems().add(new LayerGroupEntry(paramsLayer, paramsLayer.getDefaultStyle()));                
                }

                target.addComponent(layerTable);
                
                popupWindow.close(target);
            }
        };
        
        add(new AjaxLink("addLayer") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight(375);
                popupWindow.setInitialWidth(525);
                popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                popupWindow.setContent(addLayerPanel);
                popupWindow.show(target);
            }
        });
    }
    
    public List<LayerGroupEntry> getEntries() {
        return items;
    }
    
    private Component layerLink(String id, IModel itemModel) {
        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        return new Label( id, entry.getLayer().prefixedName());
    }
    
    private Component defaultStyleCheckbox(String id, IModel itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        Fragment f = new Fragment(id, "defaultStyle", this);
        CheckBox ds = new CheckBox("checkbox", new Model(entry.isDefaultStyle()));
        ds.add(new OnChangeAjaxBehavior() {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean useDefault = (Boolean) getComponent().getDefaultModelObject();
                entry.setDefaultStyle(useDefault);
                target.addComponent(layerTable);
                
            }
        });
        f.add(ds);
        return f;
    }
    
    private Component styleLink(String id, final IModel itemModel) {
        // decide if the style is the default and the current style name
        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        String styleName = null;
        boolean defaultStyle = true;
        if(entry.getStyle() != null) {
            styleName = entry.getStyle().getName();
            defaultStyle = false;
        } else if(entry.getLayer() instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) entry.getLayer();
            if (layer.getDefaultStyle() != null) {
                styleName = layer.getDefaultStyle().getName();
            }
        }
            
        // build and returns the link, but disable it if the style is the default
        SimpleAjaxLink link = new SimpleAjaxLink( id, new Model(styleName)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setTitle(new ParamResourceModel("chooseStyle", this));
                popupWindow.setContent( new StyleListPanel( popupWindow.getContentId() ) {
                    @Override
                    protected void handleStyle(StyleInfo style, AjaxRequestTarget target) {
                        popupWindow.close( target );
                        
                        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
                        entry.setStyle( style );
                        
                        //redraw
                        target.addComponent( layerTable );
                    }
                });
                popupWindow.show(target);
            }

        };
        link.getLink().setEnabled(!defaultStyle);
        return link;
    }
    
    private Component removeLink(String id, IModel itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        ImageAjaxLink link = new ImageAjaxLink(id, new ResourceReference(getClass(), "../../../web/img/icons/silk/delete.png")) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                items.remove(entry);
                target.addComponent(layerTable);
            }
        };
        link.getImage().add(
                new AttributeModifier("alt", true, new ParamResourceModel("EoLayerGroupEditPage.th.remove", link)));
        return link;
    }
    
    private Component positionPanel(String id, IModel itemModel) {
        return new PositionPanel(id, (LayerGroupEntry) itemModel.getObject());
    }
  
    static class LayerGroupEntryProvider extends GeoServerDataProvider<LayerGroupEntry> {

        public static Property<LayerGroupEntry> LAYER = new PropertyPlaceholder<LayerGroupEntry>("layer");
        public static Property<LayerGroupEntry> DEFAULT_STYLE = new PropertyPlaceholder<LayerGroupEntry>("defaultStyle");
        public static Property<LayerGroupEntry> STYLE = new PropertyPlaceholder<LayerGroupEntry>("style");
        public static Property<LayerGroupEntry> REMOVE = new PropertyPlaceholder<LayerGroupEntry>("remove");
        public static Property<LayerGroupEntry> POSITION = new PropertyPlaceholder<LayerGroupEntry>("position");

        static List PROPERTIES = Arrays.asList(POSITION, LAYER, DEFAULT_STYLE, STYLE, REMOVE);

        List<LayerGroupEntry> items;

        public LayerGroupEntryProvider(List<LayerGroupEntry> items) {
            this.items = items;
        }

        @Override
        protected List<LayerGroupEntry> getItems() {
            return items;
        }

        @Override
        protected List<Property<LayerGroupEntry>> getProperties() {
            return PROPERTIES;
        }
    }

    class PositionPanel extends Panel {

        private LayerGroupEntry entry;
        private ImageAjaxLink upLink;
        private ImageAjaxLink downLink;
        
        public PositionPanel(String id, LayerGroupEntry entry) {
            super(id);
            this.entry = entry;
            this.setOutputMarkupId(true);
            
            upLink = new ImageAjaxLink("up", new ResourceReference(getClass(),
                    "../../../web/img/icons/silk/arrow_up.png")) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    int index = items.indexOf(PositionPanel.this.entry);
                    items.remove(index);
                    items.add(Math.max(0, index - 1), PositionPanel.this.entry);
                    target.addComponent(layerTable);
                    target.addComponent(this);
                    target.addComponent(downLink);   
                    target.addComponent(upLink);
                }
            };
            upLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);
            
            downLink = new ImageAjaxLink("down", new ResourceReference(
                    getClass(), "../../../web/img/icons/silk/arrow_down.png")) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    int index = items.indexOf(PositionPanel.this.entry);
                    items.remove(index);
                    items.add(Math.min(items.size(), index + 1), PositionPanel.this.entry);
                    target.addComponent(layerTable);
                    target.addComponent(this);                    
                    target.addComponent(downLink);   
                    target.addComponent(upLink);
                }
            };
            downLink.getImage()
                    .add(new AttributeModifier("alt", true, new ParamResourceModel("down",
                            downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);
        }
    }
        
    abstract static class StyleListPanel extends GeoServerTablePanel<StyleInfo> {

        static Property<StyleInfo> NAME = new BeanProperty<StyleInfo>("name", "name");

        public StyleListPanel(String id) {
            super(id, new GeoServerDataProvider<StyleInfo>() {
                @Override
                protected List<StyleInfo> getItems() {
                    return getCatalog().getStyles();
                }

                @Override
                protected List<Property<StyleInfo>> getProperties() {
                    return Arrays.asList(NAME);
                }

                public IModel newModel(Object object) {
                    return new StyleDetachableModel((StyleInfo) object);
                }
            });
            getTopPager().setVisible(false);
        }

        @Override
        protected Component getComponentForProperty(String id, IModel itemModel,
                Property<StyleInfo> property) {
            final StyleInfo style = (StyleInfo) itemModel.getObject();
            if (property == NAME) {
                return new SimpleAjaxLink(id, NAME.getModel(itemModel)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        handleStyle(style, target);
                    }
                };
            }

            return null;
        }

        protected abstract void handleStyle(StyleInfo style, AjaxRequestTarget target);

    }
}