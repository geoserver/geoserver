/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.importer.ImportTask;

public class ImportTaskModel extends LoadableDetachableModel<ImportTask> {

    long context;
    long id;

    public ImportTaskModel(ImportTask task) {
        this(task.getContext().getId(), task.getId());
    }

    public ImportTaskModel(long context, long id) {
        this.context = context;
        this.id = id;
    }

    @Override
    protected ImportTask load() {
        return ImporterWebUtils.importer().getContext(context).task(id);
    }
}
