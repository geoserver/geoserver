/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DNSNameAttribute;
import com.sun.xacml.attr.IPv4AddressAttribute;
import com.sun.xacml.attr.IPv6AddressAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

/**
 * Builds a request for URL Matching against regular expressions Http parameters are encoded as
 * resources
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class URLMatchRequestCtxBuilder extends RequestCtxBuilder {
    private String urlString = null, remoteHost = null, remoteIP = null;

    private Map<String, Object> httpParams;

    public String getUrlString() {
        return urlString;
    }

    public URLMatchRequestCtxBuilder(XACMLRole role, String urlString, String method,
            Map<String, Object> httpParams, String remoteIP, String remoteHost) {
        super(role, method);
        this.urlString = urlString;
        this.httpParams = httpParams;
        this.remoteHost = remoteHost;
        this.remoteIP = remoteIP;
    }

    @Override
    public RequestCtx createRequestCtx() {

        Set<Subject> subjects = new HashSet<Subject>(1);
        addRole(subjects);

        Set<Attribute> resources = new HashSet<Attribute>(1);
        addGeoserverResource(resources);
        addResource(resources, XACMLConstants.URlResourceURI, urlString);
        if (httpParams != null && httpParams.size() > 0) {
            for (Entry<String, Object> entry : httpParams.entrySet()) {
                URI paramURI = null;
                try {
                    paramURI = new URI(XACMLConstants.URLParamPrefix + entry.getKey());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e); // should never happen
                }
                if (entry.getValue() instanceof String[]) {
                    for (String value : (String[]) entry.getValue()) {
                        addResource(resources, paramURI, value);
                    }
                } else {
                    addResource(resources, paramURI, entry.getValue().toString());
                }

            }
        }


        Set<Attribute> actions = new HashSet<Attribute>(1);
        addAction(actions);

        Set<Attribute> environment = new HashSet<Attribute>(1);
        try {
            if (remoteHost != null) {
                environment.add(new Attribute(XACMLConstants.DNSNameEnvironmentURI, null, null,
                        new DNSNameAttribute(remoteHost)));
            }
            if (remoteIP != null) {
                InetAddress addr = InetAddress.getByName(remoteIP);
                if (addr instanceof Inet4Address)
                    environment.add(new Attribute(XACMLConstants.IPAddressEnvironmentURI, null, null,
                            new IPv4AddressAttribute(addr)));
                if (addr instanceof Inet6Address) {
                    environment.add(new Attribute(XACMLConstants.IPAddressEnvironmentURI, null, null,
                            new IPv6AddressAttribute(addr)));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex); // should not happen
        }
        
        

        RequestCtx ctx = new RequestCtx(subjects, resources, actions, environment);
        return ctx;

    }

}
