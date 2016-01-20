/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.web.wicket.XMLNameValidator;

/**
 * Allows creation of a new workspace
 */
public class WorkspaceNewPage extends GeoServerSecuredPage {

	private static final long serialVersionUID = -4355978268880701910L;
	
	Form<WorkspaceInfo> form;
    TextField<String> nsUriTextField;
    boolean defaultWs;
    
    public WorkspaceNewPage() {
        WorkspaceInfo ws = getCatalog().getFactory().createWorkspace();
        
        form = new Form<WorkspaceInfo>( "form", new CompoundPropertyModel<WorkspaceInfo>(ws) ) {
			private static final long serialVersionUID = 6088042051374665053L;

			@Override
            protected void onSubmit() {
                Catalog catalog = getCatalog();
                
                WorkspaceInfo ws = (WorkspaceInfo) form.getModelObject();
                
                NamespaceInfo ns = catalog.getFactory().createNamespace();
                ns.setPrefix ( ws.getName() );
                ns.setURI(nsUriTextField.getDefaultModelObjectAsString());
                
                catalog.add( ws );
                catalog.add( ns );
                if(defaultWs)
                    catalog.setDefaultWorkspace(ws);
                
                //TODO: set the response page to be the edit 
                doReturn(WorkspacePage.class);
            }
        };
        add(form);
        
        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setRequired(true);
        nameTextField.add(new XMLNameValidator());
        nameTextField.add(new StringValidator() {

			private static final long serialVersionUID = -5475431734680134780L;

			@Override
            public void validate(IValidatable<String> validatable) {
                if(CatalogImpl.DEFAULT.equals(validatable.getValue())) {
                    validatable.error(new ValidationError("defaultWsError").addKey("defaultWsError"));
                }
            }
        });
        form.add( nameTextField.setRequired(true) );
        
        nsUriTextField = new TextField<String>( "uri", new Model<String>() );
        // maybe a bit too restrictive, but better than not validation at all
        nsUriTextField.setRequired(true);
        nsUriTextField.add(new URIValidator());
        form.add( nsUriTextField );
        
        CheckBox defaultChk = new CheckBox("default", new PropertyModel<Boolean>(this, "defaultWs"));
        form.add(defaultChk);
        
        SubmitLink submitLink = new SubmitLink( "submit", form );
        form.add( submitLink );
        form.setDefaultButton(submitLink);
        
        AjaxLink<Void> cancelLink = new AjaxLink<Void>( "cancel" ) {
			private static final long serialVersionUID = -1731475076965108576L;

			@Override
            public void onClick(AjaxRequestTarget target) {
                doReturn(WorkspacePage.class);
            }
        };
        form.add( cancelLink );
        
    }
}
