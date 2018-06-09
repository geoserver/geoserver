/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/** @author Chris Berry http://opensource.atlassian.com/projects/spring/browse/SEC-531 */
public class RESTfulPathBasedFilterInvocationDefinitionMap
        implements FilterInvocationSecurityMetadataSource {

    private static Log log = LogFactory.getLog(RESTfulPathBasedFilterInvocationDefinitionMap.class);

    // ~ Instance fields
    // ================================================================================================

    private Collection<EntryHolder> requestMap = new Vector<EntryHolder>();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private boolean convertUrlToLowercaseBeforeComparison = false;

    // ~ Methods
    // ========================================================================================================
    public boolean supports(Class clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    public void addSecureUrl(
            String antPath, String[] httpMethods, Collection<ConfigAttribute> attrs) {
        requestMap.add(new EntryHolder(antPath, httpMethods, attrs));

        if (log.isDebugEnabled()) {
            log.debug(
                    "Added Ant path: "
                            + antPath
                            + "; attributes: "
                            + attrs
                            + ", httpMethods: "
                            + Arrays.toString(httpMethods));
        }
    }

    public void addSecureUrl(String antPath, Collection<ConfigAttribute> attrs) {
        throw new IllegalArgumentException(
                "addSecureUrl(String, Collection<ConfigAttribute> ) is INVALID for RESTfulDefinitionSource");
    }

    public Collection<ConfigAttribute> getAllConfigAttributes() {
        Set<ConfigAttribute> set = new HashSet<ConfigAttribute>();

        for (EntryHolder h : requestMap) {
            set.addAll(h.getConfigAttributes());
        }

        return set;
        // return set.iterator();
    }

    public int getMapSize() {
        return this.requestMap.size();
    }

    public boolean isConvertUrlToLowercaseBeforeComparison() {
        return convertUrlToLowercaseBeforeComparison;
    }

    public void setConvertUrlToLowercaseBeforeComparison(
            boolean convertUrlToLowercaseBeforeComparison) {
        this.convertUrlToLowercaseBeforeComparison = convertUrlToLowercaseBeforeComparison;
    }

    public Collection<ConfigAttribute> getAttributes(Object object)
            throws IllegalArgumentException {
        if ((object == null) || !this.supports(object.getClass())) {
            throw new IllegalArgumentException("Object must be a FilterInvocation");
        }

        String url = ((FilterInvocation) object).getRequestUrl();
        String method = ((FilterInvocation) object).getHttpRequest().getMethod();

        return this.lookupAttributes(url, method);
    }

    public Collection<ConfigAttribute> lookupAttributes(String url) {
        throw new IllegalArgumentException(
                "lookupAttributes(String url) is INVALID for RESTfulDefinitionSource");
    }

    public Collection<ConfigAttribute> lookupAttributes(String url, String httpMethod) {
        // Strip anything after a question mark symbol, as per SEC-161. See also SEC-321
        int firstQuestionMarkIndex = url.indexOf("?");

        if (firstQuestionMarkIndex != -1) {
            url = url.substring(0, firstQuestionMarkIndex);
        }

        if (isConvertUrlToLowercaseBeforeComparison()) {
            url = url.toLowerCase();

            if (log.isDebugEnabled()) {
                log.debug(
                        "Converted URL to lowercase, from: '"
                                + url
                                + "'; to: '"
                                + url
                                + "'  and httpMethod= "
                                + httpMethod);
            }
        }

        Iterator iter = requestMap.iterator();
        while (iter.hasNext()) {
            EntryHolder entryHolder = (EntryHolder) iter.next();

            String antPath = entryHolder.getAntPath();
            String[] methodList = entryHolder.getHttpMethodList();
            if (log.isDebugEnabled()) {
                log.debug(
                        "~~~~~~~~~~ antPath= "
                                + antPath
                                + " methodList= "
                                + Arrays.toString(methodList));
            }

            boolean matchedPath = pathMatcher.match(antPath, url);
            boolean matchedMethods = true;
            if (methodList != null) {
                matchedMethods = false;
                for (int ii = 0; ii < methodList.length; ii++) {
                    if (methodList[ii].equals(httpMethod)) {
                        matchedMethods = true;
                        break;
                    }
                }
            }
            if (log.isDebugEnabled())
                log.debug(
                        "Candidate is: '"
                                + url
                                + "'; antPath is "
                                + antPath
                                + "; matchedPath="
                                + matchedPath
                                + "; matchedMethods="
                                + matchedMethods);

            if (matchedPath && matchedMethods) {
                log.debug(
                        "returning "
                                + StringUtils.collectionToCommaDelimitedString(
                                        entryHolder.getConfigAttributes()));
                return entryHolder.getConfigAttributes();
            }
        }
        return null;
    }

    // ~ Inner Classes
    // ==================================================================================================

    protected class EntryHolder {
        private Collection<ConfigAttribute> configAttributes;
        private String antPath;
        private String[] httpMethodList;

        public EntryHolder(
                String antPath, String[] httpMethodList, Collection<ConfigAttribute> attrs) {
            this.antPath = antPath;
            this.configAttributes = attrs;
            this.httpMethodList = httpMethodList;
        }

        protected EntryHolder() {
            throw new IllegalArgumentException("Cannot use default constructor");
        }

        public String getAntPath() {
            return antPath;
        }

        public String[] getHttpMethodList() {
            return httpMethodList;
        }

        public Collection<ConfigAttribute> getConfigAttributes() {
            return configAttributes;
        }
    }
}
