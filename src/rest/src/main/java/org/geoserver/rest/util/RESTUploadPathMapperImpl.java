/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.IOException;
import java.util.Map;
import org.geoserver.catalog.Catalog;

/**
 * Abstract implementation of the {@link RESTUploadPathMapper} interface which does not remap the
 * input root and file path. All the various {@link RESTUploadPathMapper} implementations should
 * extend this base class.
 *
 * @author Nicola Lagomarsini Geosolutions S.A.S.
 */
public abstract class RESTUploadPathMapperImpl implements RESTUploadPathMapper {

    /** Catalog object used for searching the various settings inside the metadata map */
    protected Catalog catalog;

    public RESTUploadPathMapperImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    public void mapStorePath(
            StringBuilder rootDir, String workspace, String store, Map<String, String> storeParams)
            throws IOException {}

    public void mapItemPath(
            String workspace,
            String store,
            Map<String, String> storeParams,
            StringBuilder itemPath,
            String itemName)
            throws IOException {}

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
}
