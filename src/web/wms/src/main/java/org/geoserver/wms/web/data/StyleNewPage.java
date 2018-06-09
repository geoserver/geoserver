/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.WicketRuntimeException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.Version;

/** Allows for editing a new style, includes file upload */
public class StyleNewPage extends AbstractStylePage {

    private static final long serialVersionUID = -6137191207739266238L;

    public StyleNewPage() {
        initUI(null);
        initPreviewLayer(null);
    }

    @Override
    protected void initUI(StyleInfo style) {
        super.initUI(style);

        if (!isAuthenticatedAsAdmin()) {
            // initialize the workspace drop down
            // default to first available workspace
            List<WorkspaceInfo> ws = getCatalog().getWorkspaces();
            if (!ws.isEmpty()) {
                styleModel.getObject().setWorkspace(ws.get(0));
            }
        }
    }

    @Override
    protected void onStyleFormSubmit() {
        // add the style
        Catalog catalog = getCatalog();
        StyleInfo model = styleForm.getModelObject();
        // Duplicate the model style so that values are preserved as models are detached
        StyleInfo s = catalog.getFactory().createStyle();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.updateStyle(s, model);

        StyleHandler styleHandler = styleHandler();

        // make sure the legend is null if there is no URL
        if (null == s.getLegend()
                || null == s.getLegend().getOnlineResource()
                || s.getLegend().getOnlineResource().isEmpty()) {
            s.setLegend(null);
        }

        // write out the SLD before creating the style
        try {
            if (s.getFilename() == null) {
                // TODO: check that this does not override any existing files
                s.setFilename(s.getName() + "." + styleHandler.getFileExtension());
            }
            catalog.getResourcePool().writeStyle(s, new ByteArrayInputStream(rawStyle.getBytes()));
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

        // store in the catalog
        try {
            Version version = styleHandler.version(rawStyle);
            s.setFormatVersion(version);
            catalog.add(s);
            styleForm.info("Style saved");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            error(e.getMessage());
            return;
        }
    }
}
