/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.security.xml.JaxbRoleList;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController(value = "rolesRestController")
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/roles")
public class RolesRestController {

    protected GeoServerSecurityManager securityManager;

    public RolesRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void somethingNotFound(IllegalArgumentException exception, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @GetMapping(
        value = "",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public JaxbRoleList get() throws IOException {
        return get(securityManager.getActiveRoleService());
    }

    @GetMapping(
        value = "/user/{user}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    protected JaxbRoleList getUser(@PathVariable("user") String userName) throws IOException {
        return getUser(securityManager.getActiveRoleService(), userName);
    }

    @PostMapping(
        value = "/role/{role}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseStatus(HttpStatus.CREATED) void insert(@PathVariable("role") String roleName)
            throws IOException {
        insert(securityManager.getActiveRoleService(), roleName);
    }

    @DeleteMapping(
        value = "/role/{role}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("role") String roleName)
            throws IOException {
        delete(securityManager.getActiveRoleService(), roleName);
    }

    @PostMapping(value = "/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void associate(
            @PathVariable("role") String roleName, @PathVariable("user") String userName)
            throws IOException {
        associate(securityManager.getActiveRoleService(), roleName, userName);
    }

    @DeleteMapping(value = "/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void disassociate(
            @PathVariable("role") String roleName, @PathVariable("user") String userName)
            throws IOException {
        disassociate(securityManager.getActiveRoleService(), roleName, userName);
    }

    @GetMapping(
        value = "/service/{serviceName}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    protected JaxbRoleList get(@PathVariable("serviceName") String serviceName) throws IOException {
        return get(getService(serviceName));
    }

    @GetMapping(
        value = "/service/{serviceName}/user/{user}",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    protected JaxbRoleList getUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName)
            throws IOException {
        return getUser(getService(serviceName), userName);
    }

    @PostMapping(value = "/service/{serviceName}/role/{role}")
    public @ResponseStatus(HttpStatus.CREATED) void insert(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName)
            throws IOException {
        insert(getService(serviceName), roleName);
    }

    @DeleteMapping(value = "/service/{serviceName}/role/{role}")
    public @ResponseStatus(HttpStatus.OK) void delete(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName)
            throws IOException {
        delete(getService(serviceName), roleName);
    }

    @PostMapping(value = "/service/{serviceName}/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void associate(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("user") String userName)
            throws IOException {
        associate(getService(serviceName), roleName, userName);
    }

    @DeleteMapping(value = "/service/{serviceName}/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void disassociate(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("user") String userName)
            throws IOException {
        disassociate(getService(serviceName), roleName, userName);
    }

    protected JaxbRoleList getUser(GeoServerRoleService roleService, String userName)
            throws IOException {
        return JaxbRoleList.fromGS(roleService.getRolesForUser(userName));
    }

    protected JaxbRoleList get(GeoServerRoleService roleService) throws IOException {
        return JaxbRoleList.fromGS(roleService.getRoles());
    }

    protected void insert(GeoServerRoleService roleService, String roleName) throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.addRole(new GeoServerRole(roleName));
        } finally {
            store.store();
        }
    }

    protected void delete(GeoServerRoleService roleService, String roleName) throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.removeRole(getRole(store, roleName));
        } finally {
            store.store();
        }
    }

    protected void associate(GeoServerRoleService roleService, String roleName, String userName)
            throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.associateRoleToUser(getRole(store, roleName), userName);
        } finally {
            store.store();
        }
    }

    protected void disassociate(GeoServerRoleService roleService, String roleName, String userName)
            throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.disAssociateRoleFromUser(getRole(store, roleName), userName);
        } finally {
            store.store();
        }
    }

    protected GeoServerRoleStore getStore(GeoServerRoleService roleService) throws IOException {
        if (roleService.canCreateStore()) {
            return roleService.createStore();
        } else {
            throw new IOException("Provided roleservice is read-only: " + roleService.getName());
        }
    }

    protected GeoServerRoleService getService(String serviceName) throws IOException {
        GeoServerRoleService roleService = securityManager.loadRoleService(serviceName);
        if (roleService == null) {
            throw new IllegalArgumentException(
                    "Provided roleservice does not exist: " + serviceName);
        }
        return roleService;
    }

    protected GeoServerRole getRole(GeoServerRoleService service, String roleName)
            throws IOException {
        GeoServerRole role = service.getRoleByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Provided role does not exist: " + roleName);
        }
        return role;
    }
}
