/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.geoserver.security.HTTPMethod;
import org.geoserver.security.RequestFilterChain;
import org.springframework.util.StringUtils;

/**
 * Model for {@link RequestFilterChain}
 *
 * @author christian
 */
public class RequestFilterChainWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    RequestFilterChain chain;

    public RequestFilterChainWrapper(RequestFilterChain chain) {
        this.chain = chain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestFilterChainWrapper that = (RequestFilterChainWrapper) o;
        return Objects.equals(chain, that.chain);
    }

    public void setName(String name) {
        chain.setName(name);
    }

    public String getName() {
        return chain.getName();
    }

    public List<String> getPatterns() {
        return chain.getPatterns();
    }

    public void setPatterns(List<String> patterns) {
        chain.setPatterns(patterns);
    }

    public List<String> getFilterNames() {
        return chain.getFilterNames();
    }

    public void setFilterNames(String... filterNames) {
        chain.setFilterNames(filterNames);
    }

    public void setFilterNames(List<String> filterNames) {
        chain.setFilterNames(filterNames);
    }

    public int hashCode() {
        return chain.hashCode();
    }

    public boolean isDisabled() {
        return chain.isDisabled();
    }

    public void setDisabled(boolean disabled) {
        chain.setDisabled(disabled);
    }

    public boolean isAllowSessionCreation() {
        return chain.isAllowSessionCreation();
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        chain.setAllowSessionCreation(allowSessionCreation);
    }

    public boolean isRequireSSL() {
        return chain.isRequireSSL();
    }

    public void setRequireSSL(boolean requireSSL) {
        chain.setRequireSSL(requireSSL);
    }

    public boolean isMatchHTTPMethod() {
        return chain.isMatchHTTPMethod();
    }

    public void setMatchHTTPMethod(boolean matchHTTPMethod) {
        chain.setMatchHTTPMethod(matchHTTPMethod);
    }

    public Set<HTTPMethod> getHttpMethods() {
        return chain.getHttpMethods();
    }

    public void setHttpMethods(Set<HTTPMethod> httpMethods) {
        chain.setHttpMethods(httpMethods);
    }

    public String getPatternString() {
        if (chain.getPatterns() != null)
            return StringUtils.collectionToCommaDelimitedString(chain.getPatterns());
        else return "";
    }

    public void setPatternString(String patternString) {
        if (StringUtils.hasLength(patternString))
            chain.setPatterns(
                    Arrays.asList(StringUtils.commaDelimitedListToStringArray(patternString)));
        else chain.getPatterns().clear();
    }

    public boolean isGET() {
        return chain.getHttpMethods().contains(HTTPMethod.GET);
    }

    public void setGET(boolean gET) {
        if (gET) chain.getHttpMethods().add(HTTPMethod.GET);
        else chain.getHttpMethods().remove(HTTPMethod.GET);
    }

    public boolean isPUT() {
        return chain.getHttpMethods().contains(HTTPMethod.PUT);
    }

    public void setPUT(boolean pUT) {
        if (pUT) chain.getHttpMethods().add(HTTPMethod.PUT);
        else chain.getHttpMethods().remove(HTTPMethod.PUT);
    }

    public boolean isDELETE() {
        return chain.getHttpMethods().contains(HTTPMethod.DELETE);
    }

    public void setDELETE(boolean dELETE) {
        if (dELETE) chain.getHttpMethods().add(HTTPMethod.DELETE);
        else chain.getHttpMethods().remove(HTTPMethod.DELETE);
    }

    public boolean isPOST() {
        return chain.getHttpMethods().contains(HTTPMethod.POST);
    }

    public void setPOST(boolean pOST) {
        if (pOST) chain.getHttpMethods().add(HTTPMethod.POST);
        else chain.getHttpMethods().remove(HTTPMethod.POST);
    }

    public boolean isOPTIONS() {
        return chain.getHttpMethods().contains(HTTPMethod.OPTIONS);
    }

    public void setOPTIONS(boolean oPTIONS) {
        if (oPTIONS) chain.getHttpMethods().add(HTTPMethod.OPTIONS);
        else chain.getHttpMethods().remove(HTTPMethod.OPTIONS);
    }

    public boolean isTRACE() {
        return chain.getHttpMethods().contains(HTTPMethod.TRACE);
    }

    public void setTRACE(boolean tRACE) {
        if (tRACE) chain.getHttpMethods().add(HTTPMethod.TRACE);
        else chain.getHttpMethods().remove(HTTPMethod.TRACE);
    }

    public boolean isHEAD() {
        return chain.getHttpMethods().contains(HTTPMethod.HEAD);
    }

    public void setHEAD(boolean hEAD) {
        if (hEAD) chain.getHttpMethods().add(HTTPMethod.HEAD);
        else chain.getHttpMethods().remove(HTTPMethod.HEAD);
    }

    public RequestFilterChain getChain() {
        return chain;
    }

    public String getHttpMethodString() {
        if (chain.isMatchHTTPMethod())
            return StringUtils.collectionToCommaDelimitedString(chain.getHttpMethods());
        else return "*";
    }

    public String getRoleFilterName() {
        return chain.getRoleFilterName();
    }

    public void setRoleFilterName(String roleFilterName) {
        chain.setRoleFilterName(roleFilterName);
    }
}
