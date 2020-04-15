/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.exception;

import static org.geoserver.gsr.GSRConfig.CURRENT_VERSION;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.geoserver.gsr.model.GSRModel;

/**
 * Wrapper for {@link ServiceError}, for correct JSON serialization
 *
 * @author Juan Marin - OpenGeo
 */
@XStreamAlias(value = "")
public class ServiceErrorWrapper implements GSRModel {

    private ServiceError error;

    public final double currentVersion = CURRENT_VERSION;

    public ServiceError getError() {
        return error;
    }

    public void setServiceError(ServiceError error) {
        this.error = error;
    }

    public ServiceErrorWrapper(ServiceError error) {
        this.error = error;
    }
}
