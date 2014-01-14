/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A Form validator that takes the workspace and store name form components and validates there's no
 * an existing {@link StoreInfo} in the selected workspace with the same name than the one assigned
 * through the store name form component.
 * 
 * @author Andrea Aime - OpenGeo
 * @author Gabriel Roldan - OpenGeo
 */
@SuppressWarnings("serial")
public class StoreNameValidator implements IFormValidator {

    FormComponent workspaceComponent;

    FormComponent storeNameComponent;

    private String edittingStoreId;

    private boolean required;

    /**
     * 
     * @param workspaceFormComponent
     *            the form component for the {@link WorkspaceInfo} assigned to the {@link StoreInfo}
     *            being edited
     * @param storeNameFormComponent
     *            the form component for the name assigned to the {@link StoreInfo}
     * @param edittingStoreId
     *            the id for the store being edited. May be {@code null} if we're talking of a new
     *            Store
     */
    public StoreNameValidator(final FormComponent workspaceFormComponent,
            final FormComponent storeNameFormComponent, final String edittingStoreId) {
        this(workspaceFormComponent, storeNameFormComponent, edittingStoreId, true);
    }
    
    /**
     * 
     * @param workspaceFormComponent
     *            the form component for the {@link WorkspaceInfo} assigned to the {@link StoreInfo}
     *            being edited
     * @param storeNameFormComponent
     *            the form component for the name assigned to the {@link StoreInfo}
     * @param edittingStoreId
     *            the id for the store being edited. May be {@code null} if we're talking of a new
     *            Store
     * @param required
     *            true if store name is required
     */
    public StoreNameValidator(final FormComponent workspaceFormComponent,
            final FormComponent storeNameFormComponent, final String edittingStoreId, boolean required) {
        this.workspaceComponent = workspaceFormComponent;
        this.storeNameComponent = storeNameFormComponent;
        this.edittingStoreId = edittingStoreId;
        this.required = required;
    }

    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[] { workspaceComponent, storeNameComponent };
    }

    /**
     * Performs the cross validation between the selected workspace and the assigned store name
     * <p>
     * If there's already a {@link StoreInfo} in the selected workspace with the same name than the
     * choosed one the store name form component is set with a proper {@link IValidationError error
     * message}
     * </p>
     * 
     * @see IFormValidator#validate(Form)
     */
    public void validate(final Form form) {
        final FormComponent[] components = getDependentFormComponents();
        final FormComponent wsComponent = components[0];
        final FormComponent nameComponent = components[1];

        WorkspaceInfo workspace = (WorkspaceInfo) wsComponent.getConvertedInput();
        String name = (String) nameComponent.getConvertedInput();
        
        if(name == null) {
            if(required) {
                ValidationError error = new ValidationError();
                error.addMessageKey("StoreNameValidator.storeNameRequired");
                nameComponent.error((IValidationError) error);
            }
            return;
        }

        Catalog catalog = GeoServerApplication.get().getCatalog();

        final StoreInfo existing = catalog.getStoreByName(workspace, name, StoreInfo.class);
        if (existing != null) {
            final String existingId = existing.getId();
            if (!existingId.equals(edittingStoreId)) {
                ValidationError error = new ValidationError();
                error.addMessageKey("StoreNameValidator.storeExistsInWorkspace");
                error.setVariable("workspace", workspace.getName());
                error.setVariable("storeName", name);
                nameComponent.error((IValidationError) error);
            }
        }
    }

}
