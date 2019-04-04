/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;

public class LayerPage extends ResourceConfigurationPage {

    PageParameters sourcePage;

    public LayerPage(LayerInfo info, PageParameters sourcePage) {
        super(info, true);
        this.sourcePage = sourcePage;
    }

    @Override
    protected void doSave() {
        if (getPublishedInfo().getId() == null) {
            // do not call super.doSave(), because this layer is not part of the catalog yet

            onSuccessfulSave();
        } else {
            super.doSave();
        }
    }

    @Override
    protected void onSuccessfulSave() {
        setResponsePage(ImportPage.class, sourcePage);
    }

    @Override
    protected void onCancel() {
        // TODO: cancel doesn't roll back any changes
        setResponsePage(ImportPage.class, sourcePage);
    }
}
