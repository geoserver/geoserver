/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import com.google.common.base.Predicate;
import org.geoserver.ows.Request;

/**
 * Matches OWS requests based on service, method and output format
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OWSRequestMatcher implements Predicate<Request> {

    String service;

    String method;

    String outputFormat;

    public OWSRequestMatcher() {
        this(null, null, null);
    }

    public OWSRequestMatcher(String service) {
        this(service, null, null);
    }

    public OWSRequestMatcher(String service, String method) {
        this(service, method, null);
    }

    public OWSRequestMatcher(String service, String method, String outputFormat) {
        this.service = service;
        this.method = method;
        this.outputFormat = outputFormat;

        if (service == null && (method != null || outputFormat != null))
            throw new IllegalArgumentException(
                    "Invalid OWS definition, service cannot be non null when method "
                            + "or output format are provided");
        else if (method == null && outputFormat != null)
            throw new IllegalArgumentException(
                    "Invalid OWS definition, output format cannot be valued if "
                            + "method is not provided");
    }

    @Override
    public String toString() {
        if (service == null) {
            return "Any OGC request";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(service);
            if (method != null) {
                sb.append(".").append(method);
                if (outputFormat != null) {
                    sb.append(".=").append(outputFormat);
                }
            }
            return sb.toString();
        }
    }

    @Override
    public boolean apply(Request request) {
        if (service == null) {
            return true;
        } else if (!service.equalsIgnoreCase(request.getService())) {
            return false;
        }

        if (method == null) {
            return true;
        } else if (!method.equalsIgnoreCase(request.getRequest())) {
            return false;
        }

        if (outputFormat == null) {
            return true;
        } else if (!outputFormat.equalsIgnoreCase(request.getOutputFormat())) {
            return false;
        }

        return true;
    }

    /** Returns the matched service (case insensitive) */
    public String getService() {
        return service;
    }

    /** Returns the matched method (case insensitive) */
    public String getMethod() {
        return method;
    }

    /** Returns the matched output format (case insensitive) */
    public String getOutputFormat() {
        return outputFormat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((outputFormat == null) ? 0 : outputFormat.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OWSRequestMatcher other = (OWSRequestMatcher) obj;
        if (method == null) {
            if (other.method != null) return false;
        } else if (!method.equals(other.method)) return false;
        if (outputFormat == null) {
            if (other.outputFormat != null) return false;
        } else if (!outputFormat.equals(other.outputFormat)) return false;
        if (service == null) {
            if (other.service != null) return false;
        } else if (!service.equals(other.service)) return false;
        return true;
    }
}
