/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.exception;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * A serializable error, returned to the user by the api
 *
 * @author Juan Marin, OpenGeo
 */

@XStreamAlias("error")
public class ServiceError {

    private int code;

    private String message;

    @XStreamImplicit(itemFieldName = "details")
    private List<String> details;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public ServiceError(int code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
