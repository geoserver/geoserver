/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class CoverageViewEditPage extends CoverageViewAbstractPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -3932025430605245513L;

    public CoverageViewEditPage(
            String workspaceName,
            String storeName,
            String coverageName,
            CoverageInfo coverageInfo,
            ResourceConfigurationPage previusPage)
            throws IOException {
        super(workspaceName, storeName, coverageName, coverageInfo);
        this.previousPage = previusPage;
        this.coverageInfo = coverageInfo;
    }

    private CoverageInfo coverageInfo;

    private ResourceConfigurationPage previousPage;

    protected void onSave() {
        try {
            final Catalog catalog = getCatalog();
            final CatalogBuilder builder = new CatalogBuilder(catalog);
            final CoverageStoreInfo coverageStoreInfo = catalog.getCoverageStore(storeId);
            final CoverageView coverageView = buildCoverageView();
            final List<CoverageBand> coverageBands = coverageView.getCoverageBands();
            if (coverageBands == null || coverageBands.isEmpty()) {
                throw new IllegalArgumentException("No output bands have been specified ");
            }
            coverageView.updateCoverageInfo(name, coverageStoreInfo, builder, coverageInfo);

            // set it back in the main page and redirect to it
            previousPage.updateResource(coverageInfo);
            setResponsePage(previousPage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(
                    new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                            .getString());
        }
    }

    protected void onCancel() {
        setResponsePage(previousPage);
    }
}
