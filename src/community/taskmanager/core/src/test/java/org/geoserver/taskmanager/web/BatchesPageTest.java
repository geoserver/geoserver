/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.util.Collection;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;

public class BatchesPageTest extends AbstractBatchesPanelTest<BatchesPage> {

    @Override
    protected Configuration getConfiguration() {
        return null;
    }

    @Override
    protected BatchesPage newPage() {
        return new BatchesPage();
    }

    @Override
    protected String prefix() {
        return "";
    }

    @Override
    protected Collection<Batch> getBatches() {
        return dao.getAllBatches();
    }
}
