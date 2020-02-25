/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
