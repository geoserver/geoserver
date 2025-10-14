/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.SequentialExecutionController;
import org.geoserver.rest.security.xml.JaxbRoleList;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "rolesRestController")
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/roles")
public class RolesRestController implements SequentialExecutionController {

    protected GeoServerSecurityManager securityManager;

    public RolesRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void somethingNotFound(IllegalArgumentException exception, HttpServletResponse response) throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @GetMapping(
            value = "",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbRoleList get() throws IOException {
        return get(securityManager.getActiveRoleService());
    }

    @GetMapping(
            value = "/user/{user}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    protected JaxbRoleList getUser(@PathVariable("user") String userName) throws IOException {
        return getUser(securityManager.getActiveRoleService(), userName);
    }

    @GetMapping(
            value = "/group/{group}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    protected JaxbRoleList getGroup(@PathVariable("group") String groupName) throws IOException {
        return getGroup(securityManager.getActiveRoleService(), groupName);
    }

    @PostMapping(
            value = "/role/{role}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(HttpStatus.CREATED) void insert(@PathVariable("role") String roleName) throws IOException {
        insert(securityManager.getActiveRoleService(), roleName);
    }

    @DeleteMapping(
            value = "/role/{role}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("role") String roleName) throws IOException {
        delete(securityManager.getActiveRoleService(), roleName);
    }

    @PostMapping(value = "/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void associateUser(
            @PathVariable("role") String roleName, @PathVariable("user") String userName) throws IOException {
        associateUser(securityManager.getActiveRoleService(), roleName, userName);
    }

    @DeleteMapping(value = "/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void disassociateUser(
            @PathVariable("role") String roleName, @PathVariable("user") String userName) throws IOException {
        disassociateUser(securityManager.getActiveRoleService(), roleName, userName);
    }

    @PostMapping(value = "/role/{role}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void associateGroup(
            @PathVariable("role") String roleName, @PathVariable("group") String groupName) throws IOException {
        associateToGroup(securityManager.getActiveRoleService(), roleName, groupName);
    }

    @DeleteMapping(value = "/role/{role}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void disassociateGroup(
            @PathVariable("role") String roleName, @PathVariable("group") String groupName) throws IOException {
        disassociateToGroup(securityManager.getActiveRoleService(), roleName, groupName);
    }

    @GetMapping(
            value = "/service/{serviceName}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    protected JaxbRoleList get(@PathVariable("serviceName") String serviceName) throws IOException {
        return get(getService(serviceName));
    }

    @GetMapping(
            value = "/service/{serviceName}/user/{user}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    protected JaxbRoleList getUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName) throws IOException {
        return getUser(getService(serviceName), userName);
    }

    @GetMapping(
            value = "/service/{serviceName}/group/{group}",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    protected JaxbRoleList getGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("group") String groupName)
            throws IOException {
        return getGroup(getService(serviceName), groupName);
    }

    @PostMapping(value = "/service/{serviceName}/role/{role}")
    public @ResponseStatus(HttpStatus.CREATED) void insert(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName) throws IOException {
        insert(getService(serviceName), roleName);
    }

    @DeleteMapping(value = "/service/{serviceName}/role/{role}")
    public @ResponseStatus(HttpStatus.OK) void delete(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName) throws IOException {
        delete(getService(serviceName), roleName);
    }

    @PostMapping(value = "/service/{serviceName}/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void associateUser(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("user") String userName)
            throws IOException {
        associateUser(getService(serviceName), roleName, userName);
    }

    @DeleteMapping(value = "/service/{serviceName}/role/{role}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void disassociateUser(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("user") String userName)
            throws IOException {
        disassociateUser(getService(serviceName), roleName, userName);
    }

    @PostMapping(value = "/service/{serviceName}/role/{role}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void associateGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("group") String groupName)
            throws IOException {
        associateToGroup(getService(serviceName), roleName, groupName);
    }

    @DeleteMapping(value = "/service/{serviceName}/role/{role}/group/{user}")
    public @ResponseStatus(HttpStatus.OK) void disassociateGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("role") String roleName,
            @PathVariable("user") String groupName)
            throws IOException {
        disassociateToGroup(getService(serviceName), roleName, groupName);
    }

    protected void associateToGroup(GeoServerRoleService roleService, String roleName, String groupName)
            throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.associateRoleToGroup(getRole(store, roleName), groupName);
        } finally {
            store.store();
        }
    }

    protected void disassociateToGroup(GeoServerRoleService roleService, String roleName, String groupName)
            throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.disAssociateRoleFromGroup(getRole(store, roleName), groupName);
        } finally {
            store.store();
        }
    }

    protected JaxbRoleList getUser(GeoServerRoleService roleService, String userName) throws IOException {
        return JaxbRoleList.fromGS(roleService.getRolesForUser(userName));
    }

    protected JaxbRoleList getGroup(GeoServerRoleService roleService, String groupName) throws IOException {
        return JaxbRoleList.fromGS(roleService.getRolesForGroup(groupName));
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

    protected void associateUser(GeoServerRoleService roleService, String roleName, String userName)
            throws IOException {
        GeoServerRoleStore store = getStore(roleService);
        try {
            store.associateRoleToUser(getRole(store, roleName), userName);
        } finally {
            store.store();
        }
    }

    protected void disassociateUser(GeoServerRoleService roleService, String roleName, String userName)
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
            throw new IllegalArgumentException("Provided roleservice does not exist: " + serviceName);
        }
        return roleService;
    }

    protected GeoServerRole getRole(GeoServerRoleService service, String roleName) throws IOException {
        GeoServerRole role = service.getRoleByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Provided role does not exist: " + roleName);
        }
        return role;
    }
}
