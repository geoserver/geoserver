/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.settings;

import org.apache.wicket.model.LoadableDetachableModel;
import org.locationtech.geogig.storage.cache.CacheManager;
import org.locationtech.geogig.storage.cache.CacheManagerBean;

public class CacheManagerBeanModel extends LoadableDetachableModel<CacheManagerBean> {

    private static final long serialVersionUID = 6581462577974274256L;

    @Override
    protected CacheManagerBean load() {
        return CacheManager.INSTANCE;
    }
}
