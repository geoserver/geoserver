/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Holds information about how to identify a geoserver node within a cluster.
 *
 * <p>Contains both the node identifier itself, and information about how to style the id for UI
 * purposes.
 */
public class GeoServerNodeData {

    /** System property, environment variable, etc... used to specify the node id. */
    public static final String GEOSERVER_NODE_OPTS = "GEOSERVER_NODE_OPTS";

    static final String DEFAULT_NODE_ID_TEMPLATE =
            "position:absolute; top:12px; left:12px; right:28px; width:auto; background:$background; padding: 1px; border: 1px solid #0076a1; color:$color; font-weight:bold";

    static final Logger LOGGER = Logging.getLogger(GeoServerNodeData.class);

    final String nodeId;
    final String nodeIdStyle;

    public GeoServerNodeData(String nodeId, String nodeIdStyle) {
        this.nodeId = nodeId;
        this.nodeIdStyle = nodeIdStyle;
    }

    static final Map<String, Function<InetAddress, String>> SUBSTITUTIONS;

    static {
        Map<String, Function<InetAddress, String>> subs = new HashMap<>();

        subs.put("host_ip", InetAddress::getHostAddress);
        subs.put("host_name", InetAddress::getHostName);
        subs.put(
                "host_short_name",
                addr -> {
                    String name = addr.getHostName();
                    return name.split("\\.")[0];
                });
        subs.put(
                "host_compact_name",
                addr -> {
                    String name = addr.getHostName();
                    String[] parts = name.split("\\.");
                    String hostName = parts[0];
                    return Streams.concat(
                                    Stream.of(hostName),
                                    Arrays.stream(parts)
                                            .skip(1)
                                            .map((String p) -> p.substring(0, 1)))
                            .collect(Collectors.joining("."));
                });

        SUBSTITUTIONS = Collections.unmodifiableMap(subs);
    }

    /** Creates a node data from a format-options style string. */
    public static GeoServerNodeData createFromString(String nodeOpts) {
        String nodeId = null;
        String nodeIdStyle = null;

        if (nodeOpts == null) {
            nodeId = null;
            nodeIdStyle = null;
        } else {
            try {
                Map<String, String> options = parseProperties(nodeOpts);
                String id = options.get("id");
                if (id != null) {
                    if (SUBSTITUTIONS.keySet().stream().anyMatch(id::contains)) {
                        final InetAddress address = getLocalHostLANAddress();
                        for (Entry<String, Function<InetAddress, String>> e :
                                SUBSTITUTIONS.entrySet()) {
                            final String token = String.format("$%s", e.getKey());
                            final String value = e.getValue().apply(address);
                            id = id.replace(token, value);
                        }
                    }
                }

                nodeId = id;
                String bgcolor = options.get("background");
                if (bgcolor == null) {
                    bgcolor = "#dadada";
                }
                String style = DEFAULT_NODE_ID_TEMPLATE.replace("$background", bgcolor);
                String color = options.get("color");
                if (color == null) {
                    color = "#0076a1";
                }
                nodeIdStyle = style.replace("$color", color);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Failed to parse GEOSERVER_NODE_OPTS, expected syntax is id:<nodeid>;color:<css_color>, but got "
                                + nodeOpts
                                + " instead. Disabling NODE_ID GUI element");
                nodeId = null;
                nodeIdStyle = null;
            }
        }

        return new GeoServerNodeData(nodeId, nodeIdStyle);
    }

    /**
     * Creates the node data from environment variable, system property, etc... define by {@link
     * #GEOSERVER_NODE_OPTS}
     */
    public static GeoServerNodeData createFromEnvironment() {
        return createFromString(GeoServerExtensions.getProperty(GEOSERVER_NODE_OPTS));
    }

    private static InetAddress mockAddress;

    @VisibleForTesting
    static void setMockAddress(InetAddress mockAddress) {
        GeoServerNodeData.mockAddress = mockAddress;
    }

    @VisibleForTesting
    static void clearMockAddress() {
        GeoServerNodeData.mockAddress = null;
    }

    static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        if (Objects.nonNull(mockAddress)) {
            return mockAddress;
        }
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
                    interfaces.hasMoreElements(); ) {
                NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
                // each interface can have more than one address
                for (Enumeration inetAddrs = ni.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
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
            UnknownHostException unknownHostException =
                    new UnknownHostException("Failed to determine LAN address");
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    /**
     * Parses a format-options like paramter, ; separated, with : as the assignment operator,
     * key1:value1;key2=value2;..., using backslash as the escape char if needed
     */
    private static Map<String, String> parseProperties(String property) {
        Map<String, String> properties = new HashMap<>();
        List<String> kvps = KvpUtils.escapedTokens(property, ';');

        for (String kvp : kvps) {
            List<String> kv = KvpUtils.escapedTokens(kvp, ':', 2);
            String key = kv.get(0).toLowerCase();
            String value = kv.size() == 1 ? "true" : KvpUtils.unescape(kv.get(1));
            properties.put(key, value);
        }

        return properties;
    }

    /** The node id. */
    public String getId() {
        return nodeId;
    }

    /** The node id styling. */
    public String getIdStyle() {
        return nodeIdStyle;
    }
}
