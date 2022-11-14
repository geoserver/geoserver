/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.SequentialExecutionController;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.AbstractAccessRuleDAO;
import org.geoserver.security.impl.DataAccessRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public abstract class AbstractAclController<
                R extends Comparable<R>, DAO extends AbstractAccessRuleDAO<R>>
        implements SequentialExecutionController {

    public static final String ANY = "*";

    DAO ruleDAO;

    AbstractAclController(DAO ruleDAO) {
        this.ruleDAO = ruleDAO;
    }

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @GetMapping(
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    public RuleMap rulesGet() throws IOException {
        checkUserIsAdmin();

        try {
            return getMap();
        } catch (Exception e) {
            throw createRestException(e);
        }
    }

    @PostMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public void rulesPost(@RequestBody RuleMap<String, String> map) throws IOException {
        checkUserIsAdmin();

        try {
            postMap(map);
        } catch (Exception e) {
            throw createRestException(e);
        }
    }

    @PutMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public void rulesPut(@RequestBody RuleMap<String, String> map) throws IOException {
        checkUserIsAdmin();

        try {
            putMap(map);
        } catch (Exception e) {
            throw createRestException(e);
        }
    }

    @DeleteMapping(path = "/**")
    public void rulesDelete(HttpServletRequest request) throws UnsupportedEncodingException {
        checkUserIsAdmin();

        String thePath = request.getPathInfo();
        String ruleString = thePath.substring(getBasePath().length() + 1);
        ruleString = URLDecoder.decode(ruleString, "utf-8");

        String msg = validateRuleKey(ruleString);
        if (msg != null) throw new RestException(msg, HttpStatus.UNPROCESSABLE_ENTITY);

        R rule = null;
        for (R ruleCandidate : ruleDAO.getRules()) {
            if (ruleString.equals(keyFor(ruleCandidate))) {
                rule = ruleCandidate;
                break;
            }
        }

        if (rule == null) {
            throw new ResourceNotFoundException("Rule not found: " + ruleString);
        }

        try {
            // required since the DAO can reload the rules from disk independently
            synchronized (ruleDAO) {
                ruleDAO.removeRule(rule);
                ruleDAO.storeRules();
            }
        } catch (Exception e) {
            throw createRestException(e);
        }
    }

    /** Returns the base path of the ACL resource */
    protected abstract String getBasePath();

    protected void checkUserIsAdmin() {
        if (!getManager().checkAuthenticationForAdminRole()) {
            throw new RestException("Administrative privileges required", HttpStatus.FORBIDDEN);
        }
    }

    /** Adds a rule to a map */
    protected abstract void addRuleToMap(R rule, Map<String, String> map);

    public RuleMap<String, String> getMap() throws Exception {
        RuleMap<String, String> result = new RuleMap<>();
        for (R rule : ruleDAO.getRules()) {
            addRuleToMap(rule, result);
        }
        return result;
    }

    /** Calculate the the intersection of the keys */
    protected Set<Object> intersection(Map map) {

        Set<Object> result = new HashSet<>();

        Set<Object> ruleKeys = new HashSet<>();
        for (R rule : ruleDAO.getRules()) {
            ruleKeys.add(keyFor(rule));
        }

        if (ruleKeys.isEmpty() || map.isEmpty()) return result;

        for (Object key : ruleKeys) {
            if (map.containsKey(key)) result.add(key);
        }
        return result;
    }

    /** Calculate the keys not contained in the rule data access object */
    protected Set<Object> nonExistingKeys(Map<?, ?> map) {

        List<R> rules = ruleDAO.getRules();

        if (rules.isEmpty()) return map.keySet().stream().collect(Collectors.toSet());

        Set<Object> result = new HashSet<>();
        Set<Object> ruleKeys = new HashSet<>();
        for (R rule : rules) {
            ruleKeys.add(keyFor(rule));
        }
        for (Object key : map.keySet()) {
            if (!ruleKeys.contains(key)) result.add(key);
        }
        return result;
    }

    /** Returns the key string for a rule */
    protected abstract String keyFor(R rule);

    /**
     * Validate a rule, return an error message or <code>null</code> if the rule is ok
     *
     * @param ruleKey ,ruleValue
     */
    protected String validateRule(String ruleKey, String ruleValue) {
        return validateRuleKey(ruleKey);
        // TODO
        // roles are not validated at the moment
    }

    /**
     * Validates the string representation of a rule key. Return an error message or <code>null
     * </code> if the rule is ok
     */
    protected abstract String validateRuleKey(String ruleKey);

    /** Convert an {@link Entry} to a rule object */
    protected abstract R convertEntryToRule(Entry<String, String> entry);

    /** Validates the string representation of rule keys and values */
    protected void validateMap(Map<String, String> ruleMap) {
        for (Entry<String, String> entry : ruleMap.entrySet()) {
            String msg = validateRule(entry.getKey(), entry.getValue());
            if (msg != null) {
                throw new RestException(msg, HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    protected void postMap(Map<String, String> map) throws Exception {

        validateMap(map);

        Set<Object> commonKeys = intersection(map);

        if (!commonKeys.isEmpty()) {
            String msg = "Already existing rules: " + StringUtils.join(commonKeys.iterator(), ",");
            throw new RestException(msg, HttpStatus.CONFLICT);
        }

        // required since the DAO can reload the rules from disk independently
        synchronized (ruleDAO) {
            for (Entry<String, String> entry : map.entrySet()) {
                R rule = convertEntryToRule(entry);
                ruleDAO.addRule(rule);
            }
            ruleDAO.storeRules();
        }
    }

    protected void putMap(Map<String, String> map) throws Exception {
        validateMap(map);
        Set<Object> nonExisting = nonExistingKeys(map);

        if (!nonExisting.isEmpty()) {
            String msg = "Unknown rules: " + StringUtils.join(nonExisting.iterator(), ",");
            throw new RestException(msg, HttpStatus.CONFLICT);
        }

        // required since the DAO can reload the rules from disk independently
        synchronized (ruleDAO) {
            for (Entry<String, String> entry : map.entrySet()) {
                R rule = convertEntryToRule(entry);
                // TODO, will not work for REST
                ruleDAO.removeRule(rule);
                ruleDAO.addRule(rule);
            }
            ruleDAO.storeRules();
        }
    }

    /**
     * Parses a comma separated list of roles into a set of strings, with special handling for the
     * {@link DataAccessRule#ANY} role
     *
     * @param roleCsv Comma separated list of roles.
     */
    protected Set<String> parseRoles(String roleCsv) {
        // regexp: treat extra spaces as separators, ignore extra commas
        // "a,,b, ,, c" --> ["a","b","c"]
        String[] rolesArray = roleCsv.split("[\\s,]+");
        Set<String> roles = new HashSet<>(rolesArray.length);
        roles.addAll(Arrays.asList(rolesArray));

        // if any of the roles is * we just remove all of the others
        for (String role : roles) {
            if (ANY.equals(role)) return Collections.singleton("*");
        }

        return roles;
    }

    protected RestException createRestException(Exception ex) {
        if (ex instanceof RestException) {
            return (RestException) ex; // do nothing
        } else {
            return new RestException("", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }
}
