/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.StringUtils;

/** @author Chris Berry http://opensource.atlassian.com/projects/spring/browse/SEC-531 */
public class RESTfulDefinitionSource implements FilterInvocationSecurityMetadataSource {

    private static Log log = LogFactory.getLog(RESTfulDefinitionSource.class);

    private static final String[] validMethodNames = {"GET", "PUT", "DELETE", "POST"};

    /** Underlying SecurityMetedataSource object */
    private RESTfulPathBasedFilterInvocationDefinitionMap delegate = null;
    /** rest access rules dao */
    private RESTAccessRuleDAO dao;

    /** Override the method in FilterInvocationSecurityMetadataSource */
    public Collection<ConfigAttribute> getAttributes(Object object)
            throws IllegalArgumentException {

        if ((object == null) || !this.supports(object.getClass())) {
            throw new IllegalArgumentException("Object must be a FilterInvocation");
        }

        String url = ((FilterInvocation) object).getRequestUrl();
        String method = ((FilterInvocation) object).getHttpRequest().getMethod();

        return delegate().lookupAttributes(cleanURL(url), method);
    }

    /** this form is invalid for this implementation */
    public Collection<ConfigAttribute> lookupAttributes(String url) {
        throw new IllegalArgumentException(
                "lookupAttributes(String url) is INVALID for RESTfulDefinitionSource");
    }

    public Collection<ConfigAttribute> lookupAttributes(String url, String method) {
        return delegate().lookupAttributes(cleanURL(url), method);
    }

    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return delegate().getAllConfigAttributes();
    }

    public boolean supports(Class clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    public RESTfulDefinitionSource(RESTAccessRuleDAO dao) {
        this.dao = dao;

        // force a read of the property file at startup
        dao.reload();
    }

    public void reload() {
        delegate = null;
    }

    RESTfulPathBasedFilterInvocationDefinitionMap delegate() {
        if (delegate == null || dao.isModified()) {
            synchronized (this) {
                delegate = new RESTfulPathBasedFilterInvocationDefinitionMap();
                for (String rule : dao.getRules()) {
                    processPathList(rule);
                }
            }
        }
        return delegate;
    }

    private void processPathList(String pathToRoleList) throws IllegalArgumentException {

        /*
        FilterInvocationDefinitionDecorator source = new FilterInvocationDefinitionDecorator();
        source.setDecorated( delegate );
        source.setConvertUrlToLowercaseBeforeComparison( true );
        */
        delegate.setConvertUrlToLowercaseBeforeComparison(true);

        BufferedReader br = new BufferedReader(new StringReader(pathToRoleList));
        int counter = 0;
        String line;

        List<RESTfulDefinitionSourceMapping> mappings =
                new ArrayList<RESTfulDefinitionSourceMapping>();

        while (true) {
            counter++;
            try {
                line = br.readLine();
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe.getMessage());
            }

            if (line == null) {
                break;
            }

            line = line.trim();

            if (log.isDebugEnabled()) {
                log.debug("Line " + counter + ": " + line);
            }

            if (line.startsWith("//")) {
                continue;
            }

            // Skip lines that are not directives
            if (line.lastIndexOf('=') == -1) {
                continue;
            }

            if (line.lastIndexOf("==") != -1) {
                throw new IllegalArgumentException(
                        "Only single equals should be used in line " + line);
            }

            // Tokenize the line into its name/value tokens
            // As per SEC-219, use the LAST equals as the delimiter between LHS and RHS

            String name = substringBeforeLast(line, "=");
            String value = substringAfterLast(line, "=");

            if (!StringUtils.hasText(name) || !StringUtils.hasText(value)) {
                throw new IllegalArgumentException(
                        "Failed to parse a valid name/value pair from " + line);
            }

            String antPath = name;
            String methods = null;

            int firstColonIndex = name.indexOf(":");
            if (log.isDebugEnabled())
                log.debug("~~~~~~~~~~ name= " + name + " firstColonIndex= " + firstColonIndex);

            if (firstColonIndex != -1) {
                antPath = name.substring(0, firstColonIndex);
                methods = name.substring((firstColonIndex + 1), name.length());
            }
            if (log.isDebugEnabled())
                log.debug(
                        "~~~~~~~~~~ name= "
                                + name
                                + " antPath= "
                                + antPath
                                + " methods= "
                                + methods);

            String[] methodList = null;
            if (methods != null) {
                methodList = methods.split(",");

                // Verify methodList is valid
                for (int ii = 0; ii < methodList.length; ii++) {
                    boolean matched = false;
                    for (int jj = 0; jj < validMethodNames.length; jj++) {
                        if (methodList[ii].equals(validMethodNames[jj])) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        throw new IllegalArgumentException(
                                "The HTTP Method Name ("
                                        + methodList[ii]
                                        + " does NOT equal a valid name (GET,PUT,POST,DELETE)");
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("methodList = " + Arrays.toString(methodList));
            }

            // Should all be lowercase; check each character
            // We only do this for Ant (regexp have control chars)
            for (int i = 0; i < antPath.length(); i++) {
                String character = antPath.substring(i, i + 1);
                if (!character.toLowerCase().equals(character)) {
                    throw new IllegalArgumentException(
                            "You are using Ant Paths, yet you have specified an uppercase character in line: "
                                    + line
                                    + " (character '"
                                    + character
                                    + "')");
                }
            }

            RESTfulDefinitionSourceMapping mapping = new RESTfulDefinitionSourceMapping();
            mapping.setUrl(antPath);
            mapping.setHttpMethods(methodList);

            String[] tokens = StringUtils.commaDelimitedListToStringArray(value);

            for (int i = 0; i < tokens.length; i++) {
                mapping.addConfigAttribute(new SecurityConfig(tokens[i].trim()));
            }
            mappings.add(mapping);
        }

        // This will call the addSecureUrl in RESTfulPathBasedFilterInvocationDefinitionMap
        //   which is how this whole convoluted beast gets wired together
        // source.setMappings(mappings);
        setMappings(mappings);
    }

    public void setMappings(List<RESTfulDefinitionSourceMapping> mappings) {

        Iterator<RESTfulDefinitionSourceMapping> it = mappings.iterator();
        while (it.hasNext()) {
            RESTfulDefinitionSourceMapping mapping = it.next();
            delegate.addSecureUrl(
                    mapping.getUrl(), mapping.getHttpMethods(), mapping.getConfigAttributes());
        }

        //        Iterator it = mappings.iterator();
        //        while (it.hasNext()) {
        //            RESTfulDefinitionSourceMapping mapping =
        // (RESTfulDefinitionSourceMapping)it.next();
        //            ConfigAttributeDefinition configDefinition = new ConfigAttributeDefinition();
        //
        //            Iterator configAttributesIt = mapping.getConfigAttributes().iterator();
        //            while (configAttributesIt.hasNext()) {
        //                String s = (String) configAttributesIt.next();
        //                configDefinition.addConfigAttribute( new SecurityConfig(s) );
        //            }
        //            delegate.addSecureUrl(mapping.getUrl(), mapping.getHttpMethods(),
        // configDefinition);
        //        }
    }

    /** Hacks an incoming url to help with matching. */
    String cleanURL(String url) {
        // remove any trailing slashes
        url = url.replaceAll("/+$", "");
        return url;
    }

    private String substringBeforeLast(String str, String separator) {
        if (str == null || separator == null || str.length() == 0 || separator.length() == 0) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    private String substringAfterLast(String str, String separator) {
        if (str == null || str.length() == 0) {
            return str;
        }
        if (separator == null || separator.length() == 0) {
            return "";
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1 || pos == (str.length() - separator.length())) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    // ++++++++++++++++++++++++
    public static class RESTfulDefinitionSourceMapping {
        private String url = null;
        private Collection<ConfigAttribute> configAttributes = new ArrayList<ConfigAttribute>();
        private String[] httpMethods = null;

        public void setHttpMethods(String[] httpMethods) {
            this.httpMethods = httpMethods;
        }

        /** A null httpMethods implies that ALL methods are accepted */
        public String[] getHttpMethods() {
            return httpMethods;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setConfigAttributes(Collection<ConfigAttribute> roles) {
            this.configAttributes = roles;
        }

        public Collection<ConfigAttribute> getConfigAttributes() {
            return configAttributes;
        }

        public void addConfigAttribute(ConfigAttribute configAttribute) {
            configAttributes.add(configAttribute);
        }
    }
}
