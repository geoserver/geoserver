/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.AbstractAccessRuleDAO;
import org.geoserver.security.impl.DataAccessRule;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REST Resource for reading and changing access control resources
 * 
 * @author christian
 *
 */
public abstract class AbstractAccessControlResource<DAO extends AbstractAccessRuleDAO<Comparable<?>>>
        extends MapResource {

    public static final String ANY = "*";

    DAO ruleDAO;

    AbstractAccessControlResource(DAO ruleDAO) {
        this.ruleDAO = ruleDAO;
    }

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        ArrayList<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new RuleXMLFormat());
        formats.add(new MapJSONFormat());
        return formats;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    static RestletException createNonAdminException() {
        return new RestletException("Amdinistrative priveleges required",
                Status.CLIENT_ERROR_FORBIDDEN);
    }

    @Override
    public void handleGet() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw createNonAdminException();
        super.handleGet();
    }

    @Override
    public void handlePost() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw createNonAdminException();
        super.handlePost();
    }

    @Override
    public void handlePut() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw createNonAdminException();
        super.handlePut();
    }

    @Override
    public void handleDelete() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw createNonAdminException();

        String ruleURI = (String) getRequest().getAttributes().get("rule");
        String ruleString = null;
        try {
            ruleString = URLDecoder.decode(ruleURI, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            throw new RestletException(e1.getMessage(), Status.SERVER_ERROR_INTERNAL, e1);
        }

        String msg = validateRuleKey(ruleString);
        if (msg != null)
            throw createRestletException(new RestletException(msg,
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY));

        Comparable<?> rule = null;
        for (Comparable<?> ruleCandidate : ruleDAO.getRules()) {
            if (ruleString.equals(keyFor(ruleCandidate))) {
                rule = ruleCandidate;
                break;
            }
        }

        if (rule == null) {
            throw new RestletException("Rule not found: " + rule, Status.CLIENT_ERROR_NOT_FOUND);
        }

        try {
            ruleDAO.removeRule(rule);
            ruleDAO.storeRules();

        } catch (Exception e) {
            throw createRestletException(e);
        }
    }

    /**
     * Adds a rule to a map
     * 
     * @param rule
     * @param map
     */
    protected abstract void addRuleToMap(Comparable rule, Map<String, String> map);

    @Override
    public Map getMap() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        for (Comparable<?> rule : ruleDAO.getRules()) {
            addRuleToMap(rule, result);
        }
        return result;
    }

    /**
     * Calculate the the intersection of the keys
     * 
     * 
     * @param props
     * @param map
     * @return
     */
    protected Set<Object> intersection(Map map) {

        Set<Object> result = new HashSet<Object>();

        Set<Object> ruleKeys = new HashSet<Object>();
        for (Comparable<?> rule : ruleDAO.getRules()) {
            ruleKeys.add(keyFor(rule));
        }

        if (ruleKeys.isEmpty() || map.isEmpty())
            return result;

        for (Object key : ruleKeys) {
            if (map.containsKey(key))
                result.add(key);
        }
        return result;
    }

    /**
     * Calculate the keys not contained in the rule data access object
     * 
     * 
     * @param props
     * @param map
     * @return
     */
    protected Set<Object> nonExistingKeys(Map map) {

        List<Comparable<?>> rules = ruleDAO.getRules();

        if (rules.isEmpty())
            return map.keySet();

        Set<Object> result = new HashSet<Object>();
        Set<Object> ruleKeys = new HashSet<Object>();
        for (Comparable<?> rule : rules) {
            ruleKeys.add(keyFor(rule));
        }
        for (Object key : map.keySet()) {
            if (ruleKeys.contains(key) == false)
                result.add(key);
        }
        return result;
    }

    /**
     * Returns the key string for a rule
     * 
     * @param rule
     * @return
     */
    protected abstract String keyFor(Comparable<?> rule);

    /**
     * Validate a rule, return an error message or <code>null</code> if the rule is ok
     * 
     * @param ruleKey
     *            ,ruleValue
     * @param rule
     * @return
     */
    protected String validateRule(String ruleKey, String ruleValue) {
        return validateRuleKey(ruleKey);
        // TODO
        // roles are not validated at the moment
    }

    /**
     * Validates the string representation of a rule key. Return an error message or
     * <code>null</code> if the rule is ok
     * 
     * @param ruleKey
     * @return
     */
    protected abstract String validateRuleKey(String ruleKey);

    /**
     * Convert an {@link Entry} to a rule object
     * 
     * @param entry
     * @return
     */
    protected abstract Comparable convertEntryToRule(Entry<String, String> entry);

    /**
     * Validates the string representation of rule keys and values
     * 
     * @param ruleMap
     */
    protected void validateMap(Map<String, String> ruleMap) {
        for (Entry<String, String> entry : ruleMap.entrySet()) {
            String msg = validateRule(entry.getKey(), entry.getValue());
            if (msg != null)
                throw createRestletException(new RestletException(msg,
                        Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY));
        }
    }

    @Override
    protected void postMap(Map map) throws Exception {

        validateMap(map);

        Set<Object> commonKeys = intersection(map);

        if (commonKeys.isEmpty() == false) {
            String msg = "Already existing rules: " + StringUtils.join(commonKeys.iterator(), ",");
            throw new RestletException(msg, Status.CLIENT_ERROR_CONFLICT);
        }

        List<Comparable<?>> toBeAdded = new ArrayList<Comparable<?>>();
        for (Object entry : map.entrySet()) {
            Comparable rule = convertEntryToRule((Entry<String, String>) entry);
            ruleDAO.addRule(rule);
        }
        ruleDAO.storeRules();

    }

    @Override
    protected void putMap(Map map) throws Exception {

        validateMap(map);
        Set<Object> nonExisting = nonExistingKeys(map);

        if (nonExisting.isEmpty() == false) {
            String msg = "Unknown rules: " + StringUtils.join(nonExisting.iterator(), ",");
            throw new RestletException(msg, Status.CLIENT_ERROR_CONFLICT);
        }

        for (Object entry : map.entrySet()) {
            Comparable rule = convertEntryToRule((Entry<String, String>) entry);
            // TODO, will not work for REST
            ruleDAO.removeRule(rule);
            ruleDAO.addRule(rule);
        }
        ruleDAO.storeRules();
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
        Set<String> roles = new HashSet<String>(rolesArray.length);
        roles.addAll(Arrays.asList(rolesArray));

        // if any of the roles is * we just remove all of the others
        for (String role : roles) {
            if (ANY.equals(role))
                return Collections.singleton("*");
        }

        return roles;
    }

}
