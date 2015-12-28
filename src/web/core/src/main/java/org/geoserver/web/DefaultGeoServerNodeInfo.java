/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Default node id customizer, will respond to a system variable, env variable or servlet context
 * variable named GEOSERVER_NODE_OPTS with the following sytanx:
 * <code>id:<theid>;background:<bgcolor>;color=<color><code>
 * The <code>background</code> and <code>color</code> properties are optional, the id can be a fixed
 * string or can contain the <code>$host_ip</code> or <code>$host_name</code> variable that will be
 * expanded to the first non loopback IP address of the machine, or the equivalent host name
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultGeoServerNodeInfo implements GeoServerNodeInfo {

    static final String GEOSERVER_NODE_OPTS = "GEOSERVER_NODE_OPTS";

    static final Logger LOGGER = Logging.getLogger(DefaultGeoServerNodeInfo.class);

    static String NODE_ID;

    static String NODE_ID_STYLE;

    static final String DEFAULT_NODE_ID_TEMPLATE = "position:absolute; top:12px; left:12px; right:28px; width:auto; background:$background; padding: 1px; border: 1px solid #0076a1; color:$color; font-weight:bold";

    static {
        initializeFromEnviroment();
    }

    protected static void initializeFromEnviroment() {
        String property = GeoServerExtensions.getProperty(GEOSERVER_NODE_OPTS);
        if (property == null) {
            NODE_ID = null;
            NODE_ID_STYLE = null;
        } else {
            try {
                Map<String, String> options = parseProperties(property);
                String id = (String) options.get("id");
                if (id != null) {
                    if (id.contains("$host_ip")) {
                        InetAddress address = getLocalHostLANAddress();
                        id = id.replace("$host_ip", address.getHostAddress());
                    } else if (id.contains("$host_name")) {
                        InetAddress address = getLocalHostLANAddress();
                        id = id.replace("$host_name", address.getHostName());
                    }
                }
                NODE_ID = id;
                String bgcolor = (String) options.get("background");
                if (bgcolor == null) {
                    bgcolor = "#dadada";
                }
                String style = DEFAULT_NODE_ID_TEMPLATE.replace("$background", bgcolor);
                String color = (String) options.get("color");
                if (color == null) {
                    color = "#0076a1";
                }
                NODE_ID_STYLE = style.replace("$color", color);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                        "Failed to parse GEOSERVER_NODE_OPTS, expected syntax is id:<nodeid>;color:<css_color>, but got "
                                + property + " instead. Disabling NODE_ID GUI element");
                NODE_ID = null;
                NODE_ID_STYLE = null;
            }
        }
    }

    static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration interfaces = NetworkInterface.getNetworkInterfaces(); interfaces
                    .hasMoreElements();) {
                NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
                // each interface can have more than one address
                for (Enumeration inetAddrs = ni.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    // we are not interested in loopback
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // Fall back to whatever localhost provides
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException(
                        "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address");
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    /**
     * Parses a format-options like paramter, ; separated, with : as the assignment operator,
     * key1:value1;key2=value2;..., using backslash as the escape char if needed
     */
    private static Map<String, String> parseProperties(String property) {
        Map<String, String> properties = new HashMap<String, String>();
        List<String> kvps = KvpUtils.escapedTokens(property, ';');

        for (String kvp : kvps) {
            List<String> kv = KvpUtils.escapedTokens(kvp, ':', 2);
            String key = kv.get(0).toLowerCase();
            String value = kv.size() == 1 ? "true" : KvpUtils.unescape(kv.get(1));
            properties.put(key, value);
        }

        return properties;
    }

    @Override
    public String getId() {
        return NODE_ID;
    }

    @Override
    public void customize(WebMarkupContainer container) {
        container.add(new AttributeAppender("style", new Model<String>(NODE_ID_STYLE), ";"));
        container.setVisible(isNodeIdVisible(container));
    }

    /**
     * The element is visible if an admin is logged in, and the id is not null
     * @param parent
     * @return
     */
    protected boolean isNodeIdVisible(WebMarkupContainer parent) {
        if (NODE_ID == null) {
            return false;
        }
        // we don't show the node id to all users, only to the admin
        Authentication auth = ((GeoServerSession) parent.getSession()).getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        } else {
            GeoServerSecurityManager securityManager = GeoServerApplication.get()
                    .getSecurityManager();
            return securityManager.checkAuthenticationForAdminRole(auth);
        }
    }

}
