/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.importer.ImportContext;
import org.geotools.util.logging.Logging;

public class ImportContextModel extends LoadableDetachableModel<ImportContext> {

    static Logger LOGGER = Logging.getLogger(ImportContextModel.class);

    long id;

    public ImportContextModel(ImportContext imp) {
        this(imp.getId());
    }

    public ImportContextModel(long id) {
        this.id = id;
    }

    @Override
    protected ImportContext load() {
        try {
            return ImporterWebUtils.importer().getContext(id);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load import " + id, e);
            return null;
        }
    }
}
