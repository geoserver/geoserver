/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("filters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthFilterChainFilters {

    @XStreamAsAttribute
    @JsonAlias({"@name", "name"})
    private String name;

    // map Java field "clazz" to XML attribute "class"
    @XStreamAsAttribute
    @XStreamAlias("class")
    @JsonProperty("class")
    @JsonAlias({"@class", "clazz"})
    private String clazz;

    @XStreamAsAttribute
    @JsonAlias({"@path", "path"})
    private String path; // comma-separated patterns in XML

    @XStreamAsAttribute
    @JsonAlias({"@disabled", "disabled"})
    private Boolean disabled;

    @XStreamAsAttribute
    @JsonAlias({"@allowSessionCreation", "allowSessionCreation"})
    private Boolean allowSessionCreation;

    @XStreamAsAttribute
    @XStreamAlias("ssl")
    @JsonProperty("ssl")
    @JsonAlias({"requireSSL"})
    private Boolean requireSSL; // XML "ssl" ↔ model requireSSL

    @XStreamAsAttribute
    @JsonAlias({"@matchHTTPMethod", "matchHTTPMethod"})
    private Boolean matchHTTPMethod;

    // present only on certain subclasses; keep in DTO as optional attrs
    @XStreamAsAttribute
    @JsonAlias({"@interceptorName", "interceptorName"})
    private String interceptorName;

    @XStreamAsAttribute
    @JsonAlias({"@exceptionTranslationName", "exceptionTranslationName"})
    private String exceptionTranslationName;

    // optional role filter name (present on base)
    @XStreamAsAttribute
    @JsonAlias({"@roleFilterName", "roleFilterName"})
    private String roleFilterName;

    @XStreamImplicit(itemFieldName = "filter")
    @JsonProperty("filter")
    private List<String> filters = new ArrayList<>();

    // getters/setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getAllowSessionCreation() {
        return allowSessionCreation;
    }

    public void setAllowSessionCreation(Boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    public Boolean getRequireSSL() {
        return requireSSL;
    }

    public void setRequireSSL(Boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    public Boolean getMatchHTTPMethod() {
        return matchHTTPMethod;
    }

    public void setMatchHTTPMethod(Boolean matchHTTPMethod) {
        this.matchHTTPMethod = matchHTTPMethod;
    }

    public String getInterceptorName() {
        return interceptorName;
    }

    public void setInterceptorName(String interceptorName) {
        this.interceptorName = interceptorName;
    }

    public String getExceptionTranslationName() {
        return exceptionTranslationName;
    }

    public void setExceptionTranslationName(String exceptionTranslationName) {
        this.exceptionTranslationName = exceptionTranslationName;
    }

    public String getRoleFilterName() {
        return roleFilterName;
    }

    public void setRoleFilterName(String roleFilterName) {
        this.roleFilterName = roleFilterName;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }
}
