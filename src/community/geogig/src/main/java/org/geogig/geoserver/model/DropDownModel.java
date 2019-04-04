/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.locationtech.geogig.repository.RepositoryResolver;

/**
 * Data model for the drop-down choice for GeoGig repository configuration. Currently, either a
 * Directory backend or a PostgreSQL backend are supported.
 */
public class DropDownModel implements IModel<Serializable> {

    private static final long serialVersionUID = 1L;
    static final String NO_DEFAULT_AVAILABLE = "No available repository types";

    public static final String PG_CONFIG = "PostgreSQL";
    public static final String DIRECTORY_CONFIG = "Directory";
    static String DEFAULT_CONFIG;
    public static final List<String> CONFIG_LIST = new ArrayList<>(2);

    static {
        if (RepositoryResolver.resolverAvailableForURIScheme("file")) {
            CONFIG_LIST.add(DIRECTORY_CONFIG);
        }
        if (RepositoryResolver.resolverAvailableForURIScheme("postgresql")) {
            CONFIG_LIST.add(PG_CONFIG);
        }
        if (!CONFIG_LIST.isEmpty()) {
            DEFAULT_CONFIG = CONFIG_LIST.get(0);
        } else {
            DEFAULT_CONFIG = NO_DEFAULT_AVAILABLE;
        }
    }

    private final IModel<RepositoryInfo> repoModel;
    private String type;

    public DropDownModel(IModel<RepositoryInfo> repoModel) {
        this.repoModel = repoModel;
        if (null == repoModel
                || null == repoModel.getObject()
                || null == repoModel.getObject().getLocation()) {
            type = DEFAULT_CONFIG;
        }
    }

    @Override
    public Serializable getObject() {
        if (type == null) {
            // get the type from the model
            RepositoryInfo repo = repoModel.getObject();
            URI location = repo != null ? repo.getLocation() : null;
            type = getType(location);
        }
        return type;
    }

    @Override
    public void setObject(Serializable object) {
        type = object.toString();
    }

    @Override
    public void detach() {
        if (repoModel != null) {
            repoModel.detach();
        }
        type = null;
    }

    public static String getType(URI location) {
        if (location != null) {
            if (null != location.getScheme()) {
                switch (location.getScheme()) {
                    case "postgresql":
                        return PG_CONFIG;
                    case "file":
                        return DIRECTORY_CONFIG;
                }
            }
        }
        return DEFAULT_CONFIG;
    }

    @VisibleForTesting
    static void setConfigList(List<String> configs, String defaultConfig) {
        // clear the existing list
        CONFIG_LIST.clear();
        // re-populate with provided configs
        CONFIG_LIST.addAll(configs);
        // set the default
        if (null != defaultConfig) {
            DEFAULT_CONFIG = defaultConfig;
        } else {
            DEFAULT_CONFIG = NO_DEFAULT_AVAILABLE;
        }
    }
}
