/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.exception;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 * @author Juan Marin - OpenGeo
 * 
 */

@XStreamAlias(value = "")
public class ServiceException {

    private ServiceError error;

    public ServiceError getError() {
        return error;
    }

    public void setServiceError(ServiceError error) {
        this.error = error;
    }

    public ServiceException(ServiceError error) {
        this.error = error;
    }
}
