/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import com.google.common.base.Predicate;
import org.geoserver.ows.Request;

/**
 * Matches a request by a certain IP address
 *
 * @author Andrea Aime - GeoSolutions
 */
public class IpRequestMatcher implements Predicate<Request> {

    private final String ip;

    public IpRequestMatcher(final String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public boolean apply(Request request) {
        final String incomingIp = IpFlowController.getRemoteAddr(request.getHttpRequest());
        boolean matches = ip.equals(incomingIp);
        return matches;
    }

    @Override
    public String toString() {
        return "ip=" + ip;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IpRequestMatcher other = (IpRequestMatcher) obj;
        if (ip == null) {
            if (other.ip != null) return false;
        } else if (!ip.equals(other.ip)) return false;
        return true;
    }
}
