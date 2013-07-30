package org.geoserver.importer.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geotools.util.logging.Logging;
import org.geoserver.importer.ImportContext;

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
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load import " + id, e);
            return null;
        }
    }
}
