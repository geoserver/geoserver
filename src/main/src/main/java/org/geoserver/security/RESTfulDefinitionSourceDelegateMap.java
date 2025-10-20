/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/** @author Chris Berry http://opensource.atlassian.com/projects/spring/browse/SEC-531 */
@SuppressWarnings({"deprecation", "removal"})
public class RESTfulDefinitionSourceDelegateMap {

    private static Logger log = Logging.getLogger(RESTfulDefinitionSourceDelegateMap.class);

    // ~ Instance fields
    // ================================================================================================

    private Collection<EntryHolder> requestMap = new ArrayList<>();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private boolean convertUrlToLowercaseBeforeComparison = false;

    public void addSecureUrl(String antPath, String[] httpMethods, Collection<ConfigAttribute> attrs) {
        requestMap.add(new EntryHolder(antPath, httpMethods, attrs));

        if (log.isLoggable(Level.FINE)) {
            log.fine("Added Ant path: "
                    + antPath
                    + "; attributes: "
                    + attrs
                    + ", httpMethods: "
                    + Arrays.toString(httpMethods));
        }
    }

    public Collection<ConfigAttribute> getAllConfigAttributes() {
        Set<ConfigAttribute> set = new HashSet<>();

        for (EntryHolder h : requestMap) {
            set.addAll(h.getConfigAttributes());
        }

        return set;
        // return set.iterator();
    }

    public boolean isConvertUrlToLowercaseBeforeComparison() {
        return convertUrlToLowercaseBeforeComparison;
    }

    public void setConvertUrlToLowercaseBeforeComparison(boolean convertUrlToLowercaseBeforeComparison) {
        this.convertUrlToLowercaseBeforeComparison = convertUrlToLowercaseBeforeComparison;
    }

    public Collection<ConfigAttribute> lookupAttributes(String url, String httpMethod) {
        // Strip anything after a question mark symbol, as per SEC-161. See also SEC-321
        int firstQuestionMarkIndex = url.indexOf("?");

        if (firstQuestionMarkIndex != -1) {
            url = url.substring(0, firstQuestionMarkIndex);
        }

        if (isConvertUrlToLowercaseBeforeComparison()) {
            url = url.toLowerCase();

            if (log.isLoggable(Level.FINE)) {
                log.fine("Converted URL to lowercase, from: '"
                        + url
                        + "'; to: '"
                        + url
                        + "'  and httpMethod= "
                        + httpMethod);
            }
        }

        Iterator<EntryHolder> iter = requestMap.iterator();
        while (iter.hasNext()) {
            EntryHolder entryHolder = iter.next();

            String antPath = entryHolder.getAntPath();
            String[] methodList = entryHolder.getHttpMethodList();
            if (log.isLoggable(Level.FINE)) {
                log.fine("~~~~~~~~~~ antPath= " + antPath + " methodList= " + Arrays.toString(methodList));
            }

            boolean matchedPath = pathMatcher.match(antPath, url);
            boolean matchedMethods = true;
            if (methodList != null) {
                matchedMethods = false;
                for (String s : methodList) {
                    if (s.equals(httpMethod)) {
                        matchedMethods = true;
                        break;
                    }
                }
            }
            if (log.isLoggable(Level.FINE))
                log.fine("Candidate is: '"
                        + url
                        + "'; antPath is "
                        + antPath
                        + "; matchedPath="
                        + matchedPath
                        + "; matchedMethods="
                        + matchedMethods);

            if (matchedPath && matchedMethods) {
                log.fine(
                        "returning " + StringUtils.collectionToCommaDelimitedString(entryHolder.getConfigAttributes()));
                return entryHolder.getConfigAttributes();
            }
        }
        return null;
    }

    // ~ Inner Classes
    // ==================================================================================================

    protected static class EntryHolder {
        private Collection<ConfigAttribute> configAttributes;
        private String antPath;
        private String[] httpMethodList;

        public EntryHolder(String antPath, String[] httpMethodList, Collection<ConfigAttribute> attrs) {
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
