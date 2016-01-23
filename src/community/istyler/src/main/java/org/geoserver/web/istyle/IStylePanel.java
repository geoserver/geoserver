/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.istyle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;

public class IStylePanel extends Panel {

    OpenLayersMapPanel mapPanel;
    TextArea sldTextArea;
    DropDownChoice layerChoice;
    DropDownChoice styleChoice;
    String sld;
    
    public IStylePanel(String id, LayerInfo l) {
        super(id);
        
        LayerInfo layer = l; 
        Catalog catalog = ((GeoServerApplication)getApplication()).getCatalog();
        if ( layer == null ) {
            layer = catalog.getLayers().get(0);
        }
        
        add(mapPanel = new OpenLayersMapPanel("map", layer));

        Form form = new Form("form");
        add(form);
        
        form.add(sldTextArea = new TextArea("editor", new PropertyModel(this, "sld")));
        sldTextArea.setOutputMarkupId(true);
        //sldTextArea.add(new EditAreaBehavior());
        
        updateSLD();
        
        //TODO: do not pass in layers directly, but load them via a detachable model
        form.add(layerChoice = 
            new DropDownChoice("layers", new Model(), catalog.getLayers(), new ChoiceRenderer() {
                @Override
                public Object getDisplayValue(Object object) {
                    return ((LayerInfo)object).getName();
                }
        }));
        layerChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                LayerInfo l = (LayerInfo) layerChoice.getModelObject();
                mapPanel.update(l, null, target);

                updateStyles(l);
                target.add(styleChoice);
                
                updateSLD();
                target.add(sldTextArea);
            }
        });
        layerChoice.setModelObject(layer);
        layerChoice.setEnabled(l == null);
        
        form.add(styleChoice = 
            new DropDownChoice("styles", new Model(), new ArrayList(layer.getStyles()), new ChoiceRenderer() {
                @Override
                public Object getDisplayValue(Object object) {
                    return ((StyleInfo)object).getName();
                }
        }));
        styleChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                StyleInfo s = (StyleInfo) styleChoice.getModelObject();
                mapPanel.update(null,s,target);
                
                updateSLD();
                target.add(sldTextArea);
            }
        });
        updateStyles(layer);
        
        form.add(new AjaxButton("save", form) {
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                //sldTextArea.inputChanged();
                //sldTextArea.processInput();
                
                Catalog catalog = ((GeoServerApplication)getApplication()).getCatalog();
                StyleInfo style = (StyleInfo) styleChoice.getModelObject();
                
                try {
                    catalog.getResourcePool().writeStyle(style, new ByteArrayInputStream(sld.getBytes()));
                    catalog.save(style);
                    
                    mapPanel.update(null,style,target);
                } 
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
            
            /*@Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
               
            }
            
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // we need to force EditArea to update the textarea contents (which it hid)
                // before submitting the form, otherwise the validation will go bye bye
                return new AjaxCallDecorator() {
                    @Override
                    public CharSequence decorateScript(CharSequence script) {
                        return "document.getElementById('editor').value = editAreaLoader.getValue('editor');" + script;
                    }
                };
            }
        });*/
        
        form.add(new GeoServerAjaxFormLink("revert", form) {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                
            }
        });
    }

    void updateSLD() {
        try {
            Catalog catalog = ((GeoServerApplication)getApplication()).getCatalog();
            BufferedReader r = 
                catalog.getResourcePool().readStyle(mapPanel.getCurrentStyle());
            
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = r.readLine()) != null ) {
                builder.append(line).append("\n");
            }
            r.close();
            
            this.sld = builder.toString();
            sldTextArea.setModelObject(this.sld);
        } 
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    void updateStyles(LayerInfo l) {
        List styles = new ArrayList();
        styles.add( l.getDefaultStyle() );
        styles.addAll(l.getStyles()); 
        
        styleChoice.setChoices(styles);
        styleChoice.setModelObject(l.getDefaultStyle());
    }

}
