package org.geoserver.web.wicket;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

public class UpDownPanel<T extends Object> extends Panel {
    
    T entry;
    private ImageAjaxLink upLink;

    private ImageAjaxLink downLink;

    private Component container;
    
    public UpDownPanel(String id, final T entry, final List<T> items, Component container,
            final StringResourceModel upTitle, final StringResourceModel downTitle) {
        super( id );
        this.entry = entry;
        this.setOutputMarkupId(true);
        this.container = container;
        
        upLink = new ImageAjaxLink("up", new ResourceReference(getClass(),
                "../img/icons/silk/arrow_up.png")) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                int index = items.indexOf( UpDownPanel.this.entry );
                items.remove( index );
                items.add(Math.max(0, index - 1), UpDownPanel.this.entry);
                target.addComponent(UpDownPanel.this.container);
                target.addComponent(this);
                target.addComponent(downLink);   
                target.addComponent(upLink);                    
            }
            
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("title", downTitle.getString());
                if ( items.indexOf( entry ) == 0 ) {
                    tag.put("style", "visibility:hidden");
                } else {
                    tag.put("style", "visibility:visible");
                }
            }
        };
        upLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("up", upLink)));
        upLink.setOutputMarkupId(true);
        add( upLink);            

        downLink = new ImageAjaxLink("down", new ResourceReference(getClass(),
                "../img/icons/silk/arrow_down.png")) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                int index = items.indexOf( UpDownPanel.this.entry );
                items.remove( index );
                items.add(Math.min(items.size(), index + 1), UpDownPanel.this.entry);
                target.addComponent(UpDownPanel.this.container);
                target.addComponent(this);                    
                target.addComponent(downLink);   
                target.addComponent(upLink);                    
            }
            
            @Override
            protected void onComponentTag(ComponentTag tag) {
                tag.put("title", upTitle.getString());
                if ( items.indexOf( entry ) == items.size() - 1) {
                    tag.put("style", "visibility:hidden");
                } else {
                    tag.put("style", "visibility:visible");
                }
            }
        };
        downLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("down", downLink)));
        downLink.setOutputMarkupId(true);
        add( downLink);
    }
}