/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.List;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * As per GEOS-3256, validates that upon a workspace change there are no conflicting layer names
 * between the already registered resources for the datastore being edited and any other resource
 * configured for the workspace
 */
public class CheckExistingResourcesInWorkspaceValidator implements IValidator {
    private static final long serialVersionUID = -3520867380372087997L;

    private String wsId;

    private String storeId;

    public CheckExistingResourcesInWorkspaceValidator(
            final String storeId, final String workspaceId) {
        this.storeId = storeId;
        this.wsId = workspaceId;
    }

    public void validate(final IValidatable validatable) {

        final Catalog catalog = GeoServerApplication.get().getCatalog();
        final StoreInfo store = catalog.getStore(storeId, StoreInfo.class);
        final WorkspaceInfo previousWorkspace = catalog.getWorkspace(wsId);

        final WorkspaceInfo newWorkspace = (WorkspaceInfo) validatable.getValue();
        if (previousWorkspace.equals(newWorkspace)) {
            return;
        }

        final NamespaceInfo newNamespace = catalog.getNamespaceByPrefix(newWorkspace.getName());

        List<ResourceInfo> configuredResources =
                catalog.getResourcesByStore(store, ResourceInfo.class);

        // The datastore namespace may have changed and resources with the same name may already
        // exist...
        StringBuilder sb = new StringBuilder();
        for (ResourceInfo res : configuredResources) {
            ResourceInfo existing =
                    catalog.getResourceByName(newNamespace, res.getName(), ResourceInfo.class);
            if (existing != null) {
                sb.append(existing.getName()).append(" ");
            }
        }
        if (sb.length() > 0) {
            String message =
                    "The following resources already exist on the same namespace: " + sb.toString();
            validatable.error(new ValidationError().setMessage(message));
        }
    }
}
