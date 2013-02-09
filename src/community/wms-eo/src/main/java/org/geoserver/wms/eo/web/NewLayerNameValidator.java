/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;


/**
 * A form validator that takes the workspace and the layer name form components and validates there's no 
 * an existing {@link LayerInfo} in the selected workspace with the same name.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class NewLayerNameValidator implements IFormValidator {

    protected FormComponent workspaceComponent;
    protected FormComponent layerNameComponent;
    protected boolean required;

    
    public NewLayerNameValidator(FormComponent workspaceFormComponent, FormComponent layerNameComponent, boolean required) {
        this.workspaceComponent = workspaceFormComponent;
        this.layerNameComponent = layerNameComponent;
    }

    
    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[] { workspaceComponent, layerNameComponent };
    }

    public void validate(final Form form) {
        final FormComponent[] components = getDependentFormComponents();
        final FormComponent wsComponent = components[0];
        final FormComponent nameComponent = components[1];

        WorkspaceInfo workspace = (WorkspaceInfo) wsComponent.getConvertedInput();
        String name = (String) nameComponent.getConvertedInput();
        
        if (workspace == null || StringUtils.isEmpty(name)) {
            if (required) {
                ValidationError error = new ValidationError();
                error.addMessageKey("NewLayerNameValidator.layerNameRequired");
                nameComponent.error((IValidationError) error);
            }
        } else {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            ResourceInfo existing = catalog.getResourceByName(workspace.getName(), name, ResourceInfo.class);
            if (existing != null) {
                ValidationError error = new ValidationError();
                error.addMessageKey("NewLayerNameValidator.layerExistsInWorkspace");
                error.setVariable("workspace", workspace.getName());
                error.setVariable("layerName", name);
            }
        }
    }
}