/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.NamedMap;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Master password controller */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/masterpw")
public class MasterPasswordController extends RestBaseController {

    static final String MP_CURRENT_KEY = "oldMasterPassword";

    static final String MP_NEW_KEY = "newMasterPassword";

    static final String XML_ROOT_ELEM = "masterPassword";

    GeoServerSecurityManager getManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public NamedMap<String, String> masterPasswordGet() throws IOException {

        if (!getManager().checkAuthenticationForAdminRole()) {
            throw new RestException("Amdinistrative privelges required", HttpStatus.FORBIDDEN);
        }

        char[] masterpw = getManager().getMasterPasswordForREST();

        NamedMap<String, String> m = new NamedMap<>(XML_ROOT_ELEM);
        m.put(MP_CURRENT_KEY, new String(masterpw));

        getManager().disposePassword(masterpw);
        return m;
    }

    @PutMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void masterPasswordPut(@RequestBody Map<String, String> putMap) throws IOException {
        if (!getManager().checkAuthenticationForAdminRole()) {
            // yes, for backwards compat, it's really METHOD_NOT_ALLOWED
            throw new RestException(
                    "Amdinistrative privelges required", HttpStatus.METHOD_NOT_ALLOWED);
        }

        String providerName;
        try {
            providerName = getManager().loadMasterPasswordConfig().getProviderName();
            if (getManager().loadMasterPassswordProviderConfig(providerName).isReadOnly()) {
                throw new RestException(
                        "Master password provider does not allow writes",
                        HttpStatus.METHOD_NOT_ALLOWED);
            }
        } catch (IOException e) {
            throw new RestException(
                    "Master password provider does not allow writes",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }

        String current = putMap.get(MP_CURRENT_KEY);
        String newpass = putMap.get(MP_NEW_KEY);

        if (!StringUtils.isNotBlank(current))
            throw new RestException("no master password", HttpStatus.BAD_REQUEST);

        if (!StringUtils.isNotBlank(newpass))
            throw new RestException("no master password", HttpStatus.BAD_REQUEST);

        char[] currentArray = current.trim().toCharArray();
        char[] newpassArray = newpass.trim().toCharArray();

        GeoServerSecurityManager m = getManager();
        try {
            m.saveMasterPasswordConfig(
                    m.loadMasterPasswordConfig(), currentArray, newpassArray, newpassArray);
        } catch (Exception e) {
            throw new RestException(
                    "Cannot change master password", HttpStatus.UNPROCESSABLE_ENTITY, e);
        } finally {
            m.disposePassword(currentArray);
            m.disposePassword(newpassArray);
        }
    }
}
