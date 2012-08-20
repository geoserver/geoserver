package org.opengeo.gsr.core.exception;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */

@XStreamAlias("error")
public class ServiceError {

    private String code;

    private String message;

    @XStreamImplicit(itemFieldName = "details")
    private List<String> details;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
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

    public ServiceError(String code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
