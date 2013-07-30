package org.geoserver.importer.web;

import java.io.Serializable;

import org.apache.wicket.model.Model;
import org.geoserver.importer.ImportContext;

public class ImportTasksModel extends Model {

    ImportContext imp;

    public ImportTasksModel(ImportContext imp) {
        super((Serializable) imp.getTasks());
    }

}
