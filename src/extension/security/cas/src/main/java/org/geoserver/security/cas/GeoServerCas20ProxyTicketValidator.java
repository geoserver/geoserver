/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.cas;

import java.util.List;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.jasig.cas.client.util.XmlUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.InvalidProxyChainTicketValidationException;
import org.jasig.cas.client.validation.ProxyList;
import org.jasig.cas.client.validation.TicketValidationException;

/**
 * This class is a copy of the original Cas20ProxyTicketValidator class.
 * 
 * GeoServer needs to know if a ticket is a proxy ticket or a service ticket.
 * 
 * The method {@link #customParseResponse(String, Assertion)} adds an key value pair
 * to {@link Assertion#getAttributes()}, the key is {@link GeoServerCasConstants#CAS_PROXYLIST_KEY},
 * the value a String array of proxies.
 * 
 * If the attribute is present, the assertion was created by validating a proxy ticket.
 * If the attribute is missing, the assertion was created by validating a service ticket. 
 * 
 * @author christian
 *
 */
public class GeoServerCas20ProxyTicketValidator extends Cas20ServiceTicketValidator {

    static Logger LOGGER = Logging.getLogger("org.geoserver.security.cas");
    private boolean acceptAnyProxy;

    /** This should be a list of an array of Strings */
    private ProxyList allowedProxyChains = new ProxyList();

    public GeoServerCas20ProxyTicketValidator(final String casServerUrlPrefix) {
        super(casServerUrlPrefix);
    }

    public ProxyList getAllowedProxyChains() {
        return this.allowedProxyChains;
    }

    protected String getUrlSuffix() {
        return "proxyValidate";
    }

    protected void customParseResponse(final String response, final Assertion assertion) throws TicketValidationException {
        final List proxies = XmlUtils.getTextForElements(response, "proxy");
        final String[] proxiedList = (String[]) proxies.toArray(new String[proxies.size()]);

        if (proxiedList.length>0) {
            assertion.getAttributes().put(GeoServerCasConstants.CAS_PROXYLIST_KEY, proxiedList);
            LOGGER.info("Proxy ticket validated");
        } else {
            LOGGER.info("Service ticket validated");
        }
        
        // this means there was nothing in the proxy chain, which is okay
        if (proxies == null || proxies.isEmpty() || this.acceptAnyProxy) {
            return;
        }
        
        if (allowedProxyChains.contains(proxiedList)) {
            return;
        }

        throw new InvalidProxyChainTicketValidationException("Invalid proxy chain: " + proxies.toString());
    }

    public void setAcceptAnyProxy(final boolean acceptAnyProxy) {
        this.acceptAnyProxy = acceptAnyProxy;
    }

    public void setAllowedProxyChains(final ProxyList allowedProxyChains) {
        this.allowedProxyChains = allowedProxyChains;
    }

    
}
