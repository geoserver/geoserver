/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.geofence.rest.xml.JaxbRoleList;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class RolesRestController {

    protected GeoServerSecurityManager securityManager;

    public RolesRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public void somethingNotFound(IllegalArgumentException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	response.sendError(404, exception.getMessage());
    }

    @RequestMapping(value = "/rest/roles", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbRoleList get() throws IOException {
        return get(securityManager.getActiveRoleService());
    }

    @RequestMapping(value = "/rest/roles/user/{user}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    protected @ResponseBody JaxbRoleList getUser(@PathVariable("user") String userName)
            throws IOException {
        return getUser(securityManager.getActiveRoleService(), userName);
    }

    @RequestMapping(value = "/rest/roles/role/{role}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
    public @ResponseStatus(HttpStatus.CREATED) void insert(@PathVariable("role") String roleName)
            throws IOException {
        insert(securityManager.getActiveRoleService(), roleName);
    }

    @RequestMapping(value = "/rest/roles/role/{role}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("role") String roleName)
            throws IOException {
        delete(securityManager.getActiveRoleService(), roleName);
    }

    @RequestMapping(value = "/rest/roles/role/{role}/user/{user}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void associate(@PathVariable("role") String roleName,
            @PathVariable("user") String userName) throws IOException {
        associate(securityManager.getActiveRoleService(), roleName, userName);
    }

    @RequestMapping(value = "/rest/roles/role/{role}/user/{user}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void disassociate(@PathVariable("role") String roleName,
            @PathVariable("user") String userName) throws IOException {
        disassociate(securityManager.getActiveRoleService(), roleName, userName);
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    protected @ResponseBody JaxbRoleList get(@PathVariable("serviceName") String serviceName)
            throws IOException {
        return get(getService(serviceName));
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}/user/{user}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    protected @ResponseBody JaxbRoleList getUser(@PathVariable("serviceName") String serviceName,
            @PathVariable("user") String userName) throws IOException {
        return getUser(getService(serviceName), userName);
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}/role/{role}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.CREATED) void insert(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName)
            throws IOException {
        insert(getService(serviceName), roleName);
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}/role/{role}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void delete(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName)
            throws IOException {
        delete(getService(serviceName), roleName);
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}/role/{role}/user/{user}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void associate(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName,
            @PathVariable("user") String userName) throws IOException {
        associate(getService(serviceName), roleName, userName);
    }

    @RequestMapping(value = "/rest/roles/service/{serviceName}/role/{role}/user/{user}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void disassociate(
            @PathVariable("serviceName") String serviceName, @PathVariable("role") String roleName,
            @PathVariable("user") String userName) throws IOException {
        disassociate(getService(serviceName), roleName, userName);
    }

    protected JaxbRoleList getUser(GeoServerRoleService roleService, String userName)
            throws IOException {
        return new JaxbRoleList(roleService.getRolesForUser(userName));
    }

    protected JaxbRoleList get(GeoServerRoleService roleService) throws IOException {
        return new JaxbRoleList(roleService.getRoles());
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
