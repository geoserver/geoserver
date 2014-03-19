/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;

/**
 * Shows and allows editing of the {@link MetadataLinkInfo} attached to a
 * {@link ResourceInfo}
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@SuppressWarnings("serial")
public class MetadataLinkEditor extends Panel {
    /**
     * Can't depend on the wms module here, but beware links of type ISO19115:2003 won't show up in
     * WMS 1.1.1 GetCaps
     */
    private static final List<String> LINK_TYPES = Arrays.asList("ISO19115:2003", "FGDC",
            "TC211", "19139", "other");
    private ListView<MetadataLinkInfo> links;
    private Label noMetadata;
    private WebMarkupContainer table;

    /**
     * @param id
     * @param model Must return a {@link ResourceInfo}
     */
    public MetadataLinkEditor(String id, final IModel<ResourceInfo> resourceModel) {
        super(id, resourceModel);
        
        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);
        links = new ListView<MetadataLinkInfo>("links", new PropertyModel<List<MetadataLinkInfo>>(resourceModel, "metadataLinks")) {

            @Override
            protected void populateItem(ListItem<MetadataLinkInfo> item) {
                
                // odd/even style
                item.add(new SimpleAttributeModifier("class",
                        item.getIndex() % 2 == 0 ? "even" : "odd"));

                // link info
                DropDownChoice<String> dropDownChoice = new DropDownChoice<String>("type",
                        new PropertyModel<String>(item.getModel(), "metadataType"), LINK_TYPES);
                dropDownChoice.setRequired(true);
                item.add(dropDownChoice);
                FormComponentFeedbackBorder urlBorder = new FormComponentFeedbackBorder("urlBorder");
                item.add(urlBorder);
                TextField<String> format = new TextField<String>("format", new PropertyModel<String>(item.getModel(), "type"));
                format.setRequired(true);
                item.add(format);
                TextField<String> url = new TextField<String>("metadataLinkURL", new PropertyModel<String>(item.getModel(), "content"));
                url.add(new UrlValidator());
                url.setRequired(true);
                urlBorder.add(url);
                
                // remove link
                AjaxLink<MetadataLinkInfo> link = 
                        new AjaxLink<MetadataLinkInfo>("removeLink", item.getModel()) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ResourceInfo ri = (ResourceInfo) resourceModel.getObject();
                        ri.getMetadataLinks().remove(getModelObject());
                        updateLinksVisibility();
                        target.addComponent(container);
                    }
                    
                };
                item.add(link);
            }
        };
        // this is necessary to avoid loosing item contents on edit/validation checks
        links.setReuseItems(true);
        table.add(links);
        
        // the no metadata links label
        noMetadata = new Label("noLinks", new ResourceModel("noMetadataLinksSoFar"));
        container.add(noMetadata);
        updateLinksVisibility();
        
        // add new link button
        AjaxButton button = new AjaxButton("addlink") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                ResourceInfo ri = (ResourceInfo) resourceModel.getObject();
                MetadataLinkInfo link = ri.getCatalog().getFactory().createMetadataLink();;
                link.setMetadataType(LINK_TYPES.get(0));
                link.setType("text/plain");
                ri.getMetadataLinks().add(link);
                updateLinksVisibility();
                
                target.addComponent(container);
            }
            
        };
        add(button);
    }

    private void updateLinksVisibility() {
        ResourceInfo ri = (ResourceInfo) getDefaultModelObject();
        boolean anyLink = ri.getMetadataLinks().size() > 0;
        table.setVisible(anyLink);
        noMetadata.setVisible(!anyLink);
    }
    
    public class UrlValidator extends AbstractValidator<String>{
        @Override
        protected void onValidate(IValidatable<String> validatable) {
            String url = validatable.getValue();
            if (url != null )
            {
                try {
                    MetadataLinkInfoImpl.validate(url);
                } catch (IllegalArgumentException ex) {
                    error(validatable);
                }
            }
        }
    }
}
