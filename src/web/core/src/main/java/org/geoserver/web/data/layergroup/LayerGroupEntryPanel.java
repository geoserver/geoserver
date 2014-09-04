/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

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
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.UpDownPanel;

/**
 * Allows to edit the list of layers contained in a layer group
 */
@SuppressWarnings("serial")
public class LayerGroupEntryPanel extends Panel {

    public static Property<LayerGroupEntry> LAYER = new PropertyPlaceholder<LayerGroupEntry>(
            "layer");

    public static Property<LayerGroupEntry> DEFAULT_STYLE = new PropertyPlaceholder<LayerGroupEntry>(
            "defaultStyle");

    public static Property<LayerGroupEntry> STYLE = new PropertyPlaceholder<LayerGroupEntry>(
            "style");

    public static Property<LayerGroupEntry> REMOVE = new PropertyPlaceholder<LayerGroupEntry>(
            "remove");

    static List PROPERTIES = Arrays.asList(LAYER, DEFAULT_STYLE, STYLE, REMOVE);

    ModalWindow popupWindow;
    GeoServerTablePanel<LayerGroupEntry> layerTable;
    List<LayerGroupEntry> items;
    GeoServerDialog dialog;
    
    public LayerGroupEntryPanel( String id, LayerGroupInfo layerGroup ) {
        super( id );
        
        items = new ArrayList<LayerGroupEntry>();
        for ( int i = 0; i < layerGroup.getLayers().size(); i++ ) {
            PublishedInfo layer = layerGroup.getLayers().get( i );
            StyleInfo style = layerGroup.getStyles().get( i );
            items.add( new LayerGroupEntry( layer, style ) );
        }
        
        add( popupWindow = new ModalWindow( "popup" ) );
        add(dialog = new GeoServerDialog("dialog"));
        add(new HelpLink("layersHelp").setDialog(dialog));
        
        //layers
        add(layerTable = new ReorderableTablePanel<LayerGroupEntry>("layers", items, PROPERTIES) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<LayerGroupEntry> property) {
                if (property == LAYER) {
                    return layerLink( id, itemModel );
                }
                if (property == DEFAULT_STYLE) {
                    return defaultStyleCheckbox( id, itemModel );
                }
                if (property == STYLE) {
                    return styleLink( id, itemModel );
                }
                if (property == REMOVE) {
                    return removeLink( id, itemModel );
                }
                
                return null;
            }
            
        }.setFilterable( false ));
        layerTable.setItemReuseStrategy(new DefaultItemReuseStrategy());
        layerTable.setOutputMarkupId( true );
        
        add( new AjaxLink( "addLayer" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                popupWindow.setContent( new LayerListPanel(popupWindow.getContentId()) {
                    @Override
                    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {
                        popupWindow.close( target );
                        
                        items.add(
                            new LayerGroupEntry( layer, layer.getDefaultStyle() ) );
                        
                        //getCatalog().save( lg );
                        target.addComponent( layerTable );
                    }
                });
                
                popupWindow.show(target);
            }
        });
        
        add( new AjaxLink( "addLayerGroup" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setTitle(new ParamResourceModel("chooseLayerGroup", this));
                popupWindow.setContent( new LayerGroupListPanel(popupWindow.getContentId()) {
                    @Override
                    protected void handleLayerGroup(LayerGroupInfo layerGroup, AjaxRequestTarget target) {
                        popupWindow.close( target );
                        
                        items.add(
                            new LayerGroupEntry( layerGroup, null ) );
                        
                        target.addComponent( layerTable );
                    }
                });
                
                popupWindow.show(target);
            }
        });        
    }
    
    public List<LayerGroupEntry> getEntries() {
        return items;
    }
    
    Component layerLink(String id, IModel itemModel) {
        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        return new Label( id, entry.getLayer().prefixedName());
    }
    
    Component defaultStyleCheckbox(String id, IModel itemModel) {
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
    
    Component styleLink(String id, final IModel itemModel) {
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
    
    Component removeLink(String id, IModel itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        ImageAjaxLink link = new ImageAjaxLink( id, new ResourceReference( getClass(), "../../img/icons/silk/delete.png") ) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                
                items.remove( entry );
                target.addComponent( layerTable );
            }
        };
        link.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("AbstractLayerGroupPage.th.remove", link)));
        return link;
    }
    
    Component positionPanel(String id, IModel itemModel) {
        ParamResourceModel upTitle = new ParamResourceModel("moveToBottom", this);
        ParamResourceModel downTitle = new ParamResourceModel("moveToBottom", this);
        return new UpDownPanel<LayerGroupEntry>(id, (LayerGroupEntry) itemModel.getObject(), items,
                layerTable, upTitle, downTitle);
    }

}
