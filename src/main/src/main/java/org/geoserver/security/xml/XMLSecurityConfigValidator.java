/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLSecurityConfigException.*;

import java.io.File;
import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Validator for the XML implementation
 *
 * @author christian
 */
public class XMLSecurityConfigValidator extends SecurityConfigValidator {

    public XMLSecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        XMLRoleServiceConfig xmlConfig = (XMLRoleServiceConfig) config;
        validateCheckIntervall(xmlConfig.getCheckInterval());
        validateFileName(xmlConfig.getFileName());
    }

    @Override
    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        XMLUserGroupServiceConfig xmlConfig = (XMLUserGroupServiceConfig) config;
        validateCheckIntervall(xmlConfig.getCheckInterval());
        validateFileName(xmlConfig.getFileName());
    }

    protected void validateFileName(String fileName) throws SecurityConfigException {
        if (isNotEmpty(fileName) == false) throw createSecurityException(FILENAME_REQUIRED);
    }

    protected void validateCheckIntervall(long msecs) throws SecurityConfigException {
        if (msecs != 0 && msecs < 1000) throw createSecurityException(CHECK_INTERVAL_INVALID);
    }

    /**
     * Additional Validation. Removing this configuration may also remove the file where the roles
     * are contained. (the file may be stored within the configuration sub directory). The design
     * insists on an empty role file.
     */
    @Override
    public void validateRemoveRoleService(SecurityRoleServiceConfig config)
            throws SecurityConfigException {
        super.validateRemoveRoleService(config);

        XMLRoleServiceConfig xmlConfig = (XMLRoleServiceConfig) config;
        File file = new File(xmlConfig.getFileName());
        // check if if file name is absolute and not in standard role directory
        try {
            if (file.isAbsolute()
                    && !file.getCanonicalPath()
                            .startsWith(
                                    manager.role().get(config.getName()).dir().getCanonicalPath()
                                            + File.separator)) return;
            // file in security sub dir, check if roles exists
            if (manager.loadRoleService(config.getName()).getRoleCount() > 0) {
                throw createSecurityException(ROLE_SERVICE_NOT_EMPTY_$1, config.getName());
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Additional Validation. Removing this configuration may also remove the file where the users
     * and groups are contained. (the file may be stored within the configuration sub directory).
     * The design insists on an empty user/group file.
     */
    @Override
    public void validateRemoveUserGroupService(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        XMLUserGroupServiceConfig xmlConfig = (XMLUserGroupServiceConfig) config;
        File file = new File(xmlConfig.getFileName());
        // check if if file name is absolute and not in standard role directory
        try {

            if (file.isAbsolute()
                    && !file.getCanonicalPath()
                            .startsWith(
                                    manager.userGroup()
                                                    .get(config.getName())
                                                    .file()
                                                    .getCanonicalPath()
                                            + File.separator)) return;
            // file in security sub dir, check if roles exists
            GeoServerUserGroupService service = manager.loadUserGroupService(config.getName());
            if (service.getGroupCount() > 0 || service.getUserCount() > 0) {
                throw createSecurityException(USERGROUP_SERVICE_NOT_EMPTY_$1, config.getName());
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
        super.validateRemoveUserGroupService(config);
    }

    /** Additional validation, check if the file exists or can be created */
    @Override
    public void validateAddRoleService(SecurityRoleServiceConfig config)
            throws SecurityConfigException {
        super.validateAddRoleService(config);
        XMLRoleServiceConfig xmlConfig = (XMLRoleServiceConfig) config;
        File file = new File(xmlConfig.getFileName());
        if (checkFile(file) == false)
            throw createSecurityException(FILE_CREATE_FAILED_$1, file.getPath());
    }

    /** Additional validation, check if the file exists or can be created */
    @Override
    public void validateAddUserGroupService(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        super.validateAddUserGroupService(config);
        XMLUserGroupServiceConfig xmlConfig = (XMLUserGroupServiceConfig) config;
        File file = new File(xmlConfig.getFileName());
        if (checkFile(file) == false)
            throw createSecurityException(FILE_CREATE_FAILED_$1, file.getPath());
    }

    @Override
    public void validateModifiedRoleService(
            SecurityRoleServiceConfig config, SecurityRoleServiceConfig oldConfig)
            throws SecurityConfigException {
        super.validateModifiedRoleService(config, oldConfig);
        XMLRoleServiceConfig old = (XMLRoleServiceConfig) oldConfig;
        XMLRoleServiceConfig modified = (XMLRoleServiceConfig) config;

        if (old.getFileName().equals(modified.getFileName()) == false)
            throw createSecurityException(
                    FILENAME_CHANGE_INVALID_$2, old.getFileName(), modified.getFileName());
    }

    @Override
    public void validateModifiedUserGroupService(
            SecurityUserGroupServiceConfig config, SecurityUserGroupServiceConfig oldConfig)
            throws SecurityConfigException {
        super.validateModifiedUserGroupService(config, oldConfig);
        XMLUserGroupServiceConfig old = (XMLUserGroupServiceConfig) oldConfig;
        XMLUserGroupServiceConfig modified = (XMLUserGroupServiceConfig) config;

        if (old.getFileName().equals(modified.getFileName()) == false)
            throw createSecurityException(
                    FILENAME_CHANGE_INVALID_$2, old.getFileName(), modified.getFileName());
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {

        if (isNotEmpty(config.getUserGroupServiceName()) == false) {
            throw createSecurityException(USERGROUP_SERVICE_REQUIRED);
        }
        super.validate(config);
    }

    @Override
    protected SecurityConfigException createSecurityException(String errorid, Object... args) {
        return new XMLSecurityConfigException(errorid, args);
    }
}
