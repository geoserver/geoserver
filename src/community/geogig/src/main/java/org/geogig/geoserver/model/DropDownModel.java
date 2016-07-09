/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;

/**
 * Data model for the drop-down choice for GeoGig repository configuration. Currently, either a
 * Directory backend or a PostgreSQL backend are supported.
 */
public class DropDownModel implements IModel<Serializable> {

    private static final long serialVersionUID = 1L;

    public static final String PG_CONFIG = "PostgreSQL";
    public static final String DIRECTORY_CONFIG = "Directory";
    public static final String DEFAULT_CONFIG = DIRECTORY_CONFIG;
    public static final List<String> CONFIG_LIST = Arrays.asList(DropDownModel.DIRECTORY_CONFIG,
            DropDownModel.PG_CONFIG);

    private final IModel<RepositoryInfo> repoModel;
    private String type;

    public DropDownModel(IModel<RepositoryInfo> repoModel) {
        this.repoModel = repoModel;
        if (null == repoModel || null == repoModel.getObject() || null == repoModel.getObject()
                .getLocation()) {
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
}
