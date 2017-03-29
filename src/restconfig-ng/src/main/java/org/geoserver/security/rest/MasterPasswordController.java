/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.catalog.rest.NamedMap;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Master password controller
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/masterpw")
public class MasterPasswordController extends RestBaseController {

    private static final Logger LOGGER = Logging.getLogger(MasterPasswordController.class);

    static final String MP_CURRENT_KEY = "oldMasterPassword";

    static final String MP_NEW_KEY = "newMasterPassword";

    static final String XML_ROOT_ELEM = "masterPassword";

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public NamedMap<String, String> getMasterPassword() throws IOException {

        if (getManager().checkAuthenticationForAdminRole() == false) {
            throw new RestException("Amdinistrative privelges required", HttpStatus.FORBIDDEN);
        }

        char[] masterpw = getManager().getMasterPasswordForREST();

        NamedMap<String, String> m = new NamedMap<>(XML_ROOT_ELEM);
        m.put(MP_CURRENT_KEY, new String(masterpw));

        getManager().disposePassword(masterpw);
        return m;

    }

    // @Override
    // protected void putMap(Map map) throws Exception {
    // putMap=map;
    // }

    // /**
    // * PUT is allowed if {@link MasterPasswordProviderConfig#isReadOnly()}
    // * evaluates to <code>false</code> and the principal has administrative
    // * access
    // */
    // @Override
    // public boolean allowPut() {
    //
    // if (getManager().checkAuthenticationForAdminRole()==false)
    // return false;
    //
    // String providerName;
    // try {
    // providerName = getManager().loadMasterPasswordConfig().getProviderName();
    // return getManager().loadMasterPassswordProviderConfig(providerName).isReadOnly()==false;
    // } catch (IOException e) {
    // throw new RestletException("Cannot calculate if PUT is allowed",
    // Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e);
    // }
    // }
    //
    // /**
    // * Trigger a master password change
    // */
    // @Override
    // public void handlePut() {
    // super.handlePut();
    //
    // String current = (String) putMap.get(MP_CURRENT_KEY);
    // String newpass = (String) putMap.get(MP_NEW_KEY);
    //
    // if (StringUtils.isNotBlank(current)==false)
    // throw new RestletException("no master password",
    // Status.CLIENT_ERROR_BAD_REQUEST);
    //
    // if (StringUtils.isNotBlank(newpass)==false)
    // throw new RestletException("no master password",
    // Status.CLIENT_ERROR_BAD_REQUEST);
    //
    // char[] currentArray = current.trim().toCharArray();
    // char[] newpassArray = newpass.trim().toCharArray();
    //
    //
    // GeoServerSecurityManager m = getManager();
    // try {
    // m.saveMasterPasswordConfig(m.loadMasterPasswordConfig(), currentArray,
    // newpassArray, newpassArray);
    // } catch (Exception e) {
    // throw new RestletException("Cannot change master password",
    // Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e);
    // } finally {
    // m.disposePassword(currentArray);
    // m.disposePassword(newpassArray);
    // }
    // }

}