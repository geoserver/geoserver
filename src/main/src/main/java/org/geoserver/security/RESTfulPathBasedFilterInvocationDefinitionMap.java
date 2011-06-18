package org.geoserver.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.intercept.web.DefaultFilterInvocationDefinitionSource;
import org.springframework.security.intercept.web.FilterInvocation;
import org.springframework.security.intercept.web.FilterInvocationDefinitionSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * 
 * @author Chris Berry
 * http://opensource.atlassian.com/projects/spring/browse/SEC-531
 *
 */

public class RESTfulPathBasedFilterInvocationDefinitionMap 
    //extends DefaultFilterInvocationDefinitionSource
    implements FilterInvocationDefinitionSource {
       
    static private Log log = LogFactory.getLog(RESTfulPathBasedFilterInvocationDefinitionMap.class);

    //~ Instance fields ================================================================================================

    private List requestMap = new Vector();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private boolean convertUrlToLowercaseBeforeComparison = false;

    //~ Methods ========================================================================================================
    public boolean supports(Class clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    
    public void addSecureUrl(String antPath, String[] httpMethods, ConfigAttributeDefinition attr) {
        requestMap.add( new EntryHolder(antPath, httpMethods, attr) );

        if (log.isDebugEnabled()) {
            log.debug("Added Ant path: " + antPath + "; attributes: " + attr + ", httpMethods: " +  httpMethods);
            if ( httpMethods != null ) {
                for( int ii=0; ii <  httpMethods.length; ii++ ) 
                    log.debug("httpMethods[" + ii + "]: " +  httpMethods[ii] );   
            }
        }
    }

    public void addSecureUrl(String antPath, ConfigAttributeDefinition attr) {
        throw new IllegalArgumentException( "addSecureUrl(String, ConfigAttributeDefinition) is INVALID for RESTfulDefinitionSource" );
    }

    public Collection getConfigAttributeDefinitions() {
        Set set = new HashSet();
        Iterator iter = requestMap.iterator();

        while (iter.hasNext()) {
            EntryHolder entryHolder = (EntryHolder) iter.next();
            set.add( entryHolder.getConfigAttributeDefinition() );
        }
        return set;
        //return set.iterator();
    }

    public int getMapSize() {
        return this.requestMap.size();
    }

    public boolean isConvertUrlToLowercaseBeforeComparison() {
        return convertUrlToLowercaseBeforeComparison;
    }

    public void setConvertUrlToLowercaseBeforeComparison(boolean convertUrlToLowercaseBeforeComparison) {
        this.convertUrlToLowercaseBeforeComparison = convertUrlToLowercaseBeforeComparison;
    }

    /** 
     * override the method in AbstractFilterInvocationDefinitionSource
     */
    public ConfigAttributeDefinition getAttributes(Object object)
        throws IllegalArgumentException {
        if ((object == null) || !this.supports(object.getClass())) {
            throw new IllegalArgumentException("Object must be a FilterInvocation");
        }

        String url = ((FilterInvocation) object).getRequestUrl();
        String method = ((FilterInvocation) object).getHttpRequest().getMethod();

        return this.lookupAttributes( url, method );
    }

    public ConfigAttributeDefinition lookupAttributes( String url ) { 
        throw new IllegalArgumentException( "lookupAttributes(String url) is INVALID for RESTfulDefinitionSource" );
    }

    public ConfigAttributeDefinition lookupAttributes( String url, String httpMethod ) {
        // Strip anything after a question mark symbol, as per SEC-161. See also SEC-321
        int firstQuestionMarkIndex = url.indexOf("?");

        if (firstQuestionMarkIndex != -1) {
            url = url.substring(0, firstQuestionMarkIndex);
        }

        if (isConvertUrlToLowercaseBeforeComparison()) {
            url = url.toLowerCase();

            if (log.isDebugEnabled()) {
                log.debug("Converted URL to lowercase, from: '" + url + "'; to: '" + url 
                          + "'  and httpMethod= " + httpMethod );
            }
        }

        Iterator iter = requestMap.iterator();
        while (iter.hasNext()) {
            EntryHolder entryHolder = (EntryHolder) iter.next();

            String antPath = entryHolder.getAntPath();
            String[] methodList = entryHolder.getHttpMethodList();
            if (log.isDebugEnabled()) {
                log.debug( "~~~~~~~~~~ antPath= " + antPath + " methodList= " + methodList );
                if ( methodList != null ) {
                    for( int ii=0; ii <  methodList.length; ii++ ) 
                        log.debug("method[" + ii + "]: " +  methodList[ii] );   
                }
            }

            boolean matchedPath = pathMatcher.match( antPath, url );
            boolean matchedMethods = true; 
            if ( methodList != null ) {
                matchedMethods = false;
                for( int ii=0; ii < methodList.length; ii++ ) {
                    if ( methodList[ii].equals( httpMethod ) ) {
                        matchedMethods = true;
                        break;
                    }
                } 
            }
            if ( log.isDebugEnabled() ) 
                log.debug("Candidate is: '" + url + "'; antPath is " + antPath
                          + "; matchedPath=" + matchedPath  + "; matchedMethods=" + matchedMethods );

            if ( matchedPath && matchedMethods ) {
                return entryHolder.getConfigAttributeDefinition();
            }
        }
        return null;
    }

    //~ Inner Classes ==================================================================================================

    protected class EntryHolder {
        private ConfigAttributeDefinition configAttributeDefinition;
        private String antPath;
        private String[] httpMethodList; 

        public EntryHolder( String antPath, String[] httpMethodList, ConfigAttributeDefinition attr ) {
            this.antPath = antPath;
            this.configAttributeDefinition = attr;
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

        public ConfigAttributeDefinition getConfigAttributeDefinition() {
            return configAttributeDefinition;
        }
    }
}