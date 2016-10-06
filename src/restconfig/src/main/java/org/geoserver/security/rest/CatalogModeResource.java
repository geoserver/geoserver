/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geoserver.rest.format.MapXMLFormat;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REST Resource for manipulation the catalog mode. See {@link CatalogMode}.
 * 
 * @author christian
 *
 */
public class CatalogModeResource extends MapResource {

    static final String MODE_ELEMENT = "mode";

    static final String XML_ROOT_ELEM = "catalog";

    DataAccessRuleDAO ruleDAO;

    public CatalogModeResource() {
        ruleDAO = DataAccessRuleDAO.get();
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        ArrayList<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(new MapXMLFormat(XML_ROOT_ELEM));
        formats.add(new MapJSONFormat());
        return formats;
    }

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return false;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    public void handleGet() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw AbstractAccessControlResource.createNonAdminException();
        super.handleGet();
    }

    @Override
    public void handlePut() {
        if (getManager().checkAuthenticationForAdminRole() == false)
            throw AbstractAccessControlResource.createNonAdminException();
        super.handlePut();
    }

    @Override
    public Map getMap() throws Exception {

        CatalogMode mode = ((DataAccessRuleDAO) ruleDAO).getMode();
        Map modeMap = new HashMap();
        modeMap.put(MODE_ELEMENT, mode.toString());
        return modeMap;
    }

    @Override
    protected void putMap(Map map) throws Exception {

        String mode = (String) map.get(MODE_ELEMENT);

        if (mode == null)
            throw createRestletException(new RestletException("Element " + MODE_ELEMENT
                    + " is missing", Status.CLIENT_ERROR_NOT_FOUND));

        CatalogMode modeValue = null;
        for (CatalogMode m : CatalogMode.values()) {
            if (m.toString().equals(mode)) {
                modeValue = m;
                break;
            }
        }

        if (modeValue == null)
            throw createRestletException(new RestletException("Not a valid mode: " + mode,
                    Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY));

        ((DataAccessRuleDAO) ruleDAO).setCatalogMode(modeValue);
        ruleDAO.storeRules();

    }

}
