/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/** Data Access Object for {@link ProxyBaseExtensionRule}. */
public class ProxyBaseExtRuleDAO {
    private static final Logger LOGGER = Logging.getLogger(ProxyBaseExtRuleDAO.class);
    private static SecureXStream xStream;

    public static final String PROXY_BASE_EXT_RULES_DIRECTORY = "proxy-base-ext";
    public static final String PROXY_BASE_EXT_RULES_PATH =
            PROXY_BASE_EXT_RULES_DIRECTORY + "/proxy-base-ext.xml";
    private static final String DATA_DIRECTORY = "dataDirectory";

    static {
        xStream = new SecureXStream();
        xStream.registerConverter(new ProxyBaseExtRuleConverter());
        xStream.alias("ProxyBaseExtensionRule", ProxyBaseExtensionRule.class);
        xStream.alias(
                "ProxyBaseExtensionRules", ProxyBaseExtRuleDAO.ProxyBaseExtensionRuleList.class);
        xStream.addImplicitCollection(
                ProxyBaseExtRuleDAO.ProxyBaseExtensionRuleList.class, "proxyBaseExtensionRules");
        xStream.allowTypes(
                new Class[] {
                    ProxyBaseExtensionRule.class,
                    ProxyBaseExtRuleDAO.ProxyBaseExtensionRuleList.class
                });
    }

    /**
     * Get the {@link ProxyBaseExtensionRule} from the {@link GeoServerDataDirectory}.
     *
     * @param resource
     * @return
     */
    public static List<ProxyBaseExtensionRule> getProxyBaseExtensionRules(Resource resource) {
        if (resource.getType() == Resource.Type.RESOURCE) {
            try (InputStream inputStream = resource.in()) {
                if (inputStream.available() == 0) {
                    LOGGER.log(Level.FINE, "Proxy Base Extension Rules file seems to be empty.");
                } else {
                    ProxyBaseExtensionRuleList list =
                            (ProxyBaseExtensionRuleList) xStream.fromXML(inputStream);
                    return list.proxyBaseExtensionRules == null
                            ? new ArrayList<>()
                            : list.proxyBaseExtensionRules;
                }
            } catch (Exception exception) {
                throw new ProxyBaseExtException(
                        exception, "Error parsing Proxy Base Extension rule files.");
            }
        } else {
            LOGGER.log(Level.INFO, "Proxy Base Extension rules file does not exist.");
        }
        return new ArrayList<>();
    }

    /**
     * Get a list of {@link ProxyBaseExtensionRule} from the {@link GeoServerDataDirectory}.
     *
     * @return a list of {@link ProxyBaseExtensionRule}
     */
    public static List<ProxyBaseExtensionRule> getRules() {
        Resource rules = getDataDirectory().get(PROXY_BASE_EXT_RULES_PATH);
        return getProxyBaseExtensionRules(rules);
    }

    /**
     * Save or Update a {@link ProxyBaseExtensionRule} in the {@link GeoServerDataDirectory}.
     *
     * @param proxyBaseExtensionRule the {@link ProxyBaseExtensionRule} to save or update
     */
    public static void saveOrUpdateProxyBaseExtRule(ProxyBaseExtensionRule proxyBaseExtensionRule) {
        Resource proxyBaseExtensionRules = getDataDirectory().get(PROXY_BASE_EXT_RULES_PATH);
        saveOrUpdateProxyBaseExtRule(proxyBaseExtensionRule, proxyBaseExtensionRules);
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean(DATA_DIRECTORY);
    }

    /**
     * Write the {@link ProxyBaseExtensionRule} in the {@link GeoServerDataDirectory}.
     *
     * @param proxyBaseExtRule the {@link ProxyBaseExtensionRule} to write
     * @param input the input {@link Resource}
     */
    public static void saveOrUpdateProxyBaseExtRule(
            ProxyBaseExtensionRule proxyBaseExtRule, Resource input) {
        List<ProxyBaseExtensionRule> proxyBaseExtensionRules = getProxyBaseExtensionRules(input);
        boolean exists = false;
        for (int i = 0; i < proxyBaseExtensionRules.size() && !exists; i++) {
            if (proxyBaseExtensionRules.get(i).getId().equals(proxyBaseExtRule.getId())) {
                proxyBaseExtensionRules.set(i, proxyBaseExtRule);
                exists = true;
            }
        }
        if (!exists) {
            proxyBaseExtensionRules.add(proxyBaseExtRule);
        }

        writeProxyBaseExtRules(proxyBaseExtensionRules, input);
    }

    /**
     * Delete a {@link ProxyBaseExtensionRule} from the {@link GeoServerDataDirectory}.
     *
     * @param proxyBaseExtRulesIds the {@link ProxyBaseExtensionRule} ids to delete
     */
    public static void deleteProxyBaseExtRules(String... proxyBaseExtRulesIds) {
        Resource proxyBaseExtRules = getDataDirectory().get(PROXY_BASE_EXT_RULES_PATH);
        deleteProxyBaseExtRules(proxyBaseExtRules, proxyBaseExtRulesIds);
    }

    /**
     * Delete a {@link ProxyBaseExtensionRule} from the {@link GeoServerDataDirectory}.
     *
     * @param inputResource the input {@link Resource}
     * @param forwardParameterIds the {@link ProxyBaseExtensionRule} ids to delete
     */
    public static void deleteProxyBaseExtRules(
            Resource inputResource, String... forwardParameterIds) {

        List<ProxyBaseExtensionRule> collect =
                getProxyBaseExtensionRules(inputResource).stream()
                        .filter(p -> !ArrayUtils.contains(forwardParameterIds, p.getId()))
                        .collect(Collectors.toList());

        writeProxyBaseExtRules(collect, inputResource);
    }

    private static void writeProxyBaseExtRules(
            List<ProxyBaseExtensionRule> proxyBaseExtRules, Resource output) {
        try (OutputStream outputStream = output.out()) {
            xStream.toXML(new ProxyBaseExtensionRuleList(proxyBaseExtRules), outputStream);
        } catch (Throwable exception) {
            throw new ProxyBaseExtException(
                    exception, "Something bad happened when writing Proxy Base Extension rules.");
        }
    }

    /** Exception thrown when something bad happens during the mangling process. */
    public static final class ProxyBaseExtException extends RuntimeException {

        public ProxyBaseExtException(Throwable cause, String message, Object... messageArguments) {
            super(String.format(message, messageArguments), cause);
        }
    }
    /** Support class for XStream serialization */
    static final class ProxyBaseExtensionRuleList {
        List<ProxyBaseExtensionRule> proxyBaseExtensionRules;

        public ProxyBaseExtensionRuleList(List<ProxyBaseExtensionRule> rules) {
            this.proxyBaseExtensionRules = rules;
        }
    }
}
