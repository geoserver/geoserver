/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.shape;

import static org.geotools.data.shapefile.ShapefileDataStoreFactory.URLP;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;

/**
 * Provides the form components for the directory of shapefiles store
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ShapefileDirectoryEditPanel extends ShapefileStoreEditPanel {

    public ShapefileDirectoryEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
    }

    protected Panel buildFileParamPanel(final IModel paramsModel) {
        DirectoryParamPanel file =
                new DirectoryParamPanel(
                        "url",
                        new MapModel(paramsModel, URLP.key),
                        new ParamResourceModel("shapefile", this),
                        true);
        file.setFileFilter(new Model<ExtensionFileFilter>(new ExtensionFileFilter(".shp")));
        return file;
    }
}
