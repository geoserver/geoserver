/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.geofence.rest.xml.JaxbGroupList;
import org.geoserver.geofence.rest.xml.JaxbUser;
import org.geoserver.geofence.rest.xml.JaxbUserList;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class UsersRestController {

    protected GeoServerSecurityManager securityManager;

    private static final String DEFAULT_ROLE_SERVICE_NAME = "default";
    
    private String getDefaultServiceName() {
        if (this.securityManager != null &&
                this.securityManager.getActiveRoleService() != null) {
            return this.securityManager.getActiveRoleService().getName();
        }
        
        return DEFAULT_ROLE_SERVICE_NAME;
    }

    public UsersRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void somethingNotFound(IllegalArgumentException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	response.sendError(404, exception.getMessage());
    }

    @RequestMapping(value = "/rest/usergroup/users", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbUserList getUsers() throws IOException {
        return getUsers(getDefaultServiceName());
    }

    @RequestMapping(value = "/rest/usergroup/groups", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbGroupList getGroups() throws IOException {
        return getGroups(getDefaultServiceName());
    }

    @RequestMapping(value = "/rest/usergroup/group/{group}/users", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbUserList getUsersFromGroup(@PathVariable("group") String groupName)
            throws IOException {
        return getUsersFromGroup(getDefaultServiceName(), groupName);
    }

    @RequestMapping(value = "/rest/usergroup/user/{user}/groups", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbGroupList getGroupsFromUser(@PathVariable("user") String userName)
            throws IOException {
        return getGroupsFromUser(getDefaultServiceName(), userName);
    }

    @RequestMapping(value = "/rest/usergroup/users", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.CREATED) void insertUser(@RequestBody JaxbUser user)
            throws PasswordPolicyException, IOException {
        insertUser(getDefaultServiceName(), user);
    }

    @RequestMapping(value = "/rest/usergroup/user/{user}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void updateUser(@PathVariable("user") String userName,
            @RequestBody JaxbUser user) throws PasswordPolicyException, IOException {
        updateUser(getDefaultServiceName(), userName, user);
    }

    @RequestMapping(value = "/rest/usergroup/user/{user}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void deleteUser(@PathVariable("user") String userName)
            throws IOException {
        deleteUser(getDefaultServiceName(), userName);
    }

    @RequestMapping(value = "/rest/usergroup/group/{group}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.CREATED) void insertGroup(
            @PathVariable("group") String groupName) throws PasswordPolicyException, IOException {
        insertGroup(getDefaultServiceName(), groupName);
    }

    @RequestMapping(value = "/rest/usergroup/group/{group}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void deleteGroup(@PathVariable("group") String groupName)
            throws IOException {
        deleteGroup(getDefaultServiceName(), groupName);
    }

    @RequestMapping(value = "/rest/usergroup/user/{user}/group/{group}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void associateUserToGroup(
            @PathVariable("user") String userName, @PathVariable("group") String groupName)
            throws IOException {
        associateUserToGroup(getDefaultServiceName(), userName, groupName);
    }

    @RequestMapping(value = "/rest/usergroup/user/{user}/group/{group}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void disassociateUserFromGroup(
            @PathVariable("user") String userName, @PathVariable("group") String groupName)
            throws IOException {
        disassociateUserFromGroup(getDefaultServiceName(), userName, groupName);
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/users", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbUserList getUsers(@PathVariable("serviceName") String serviceName)
            throws IOException {
        return new JaxbUserList(getService(serviceName).getUsers());
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/groups", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbGroupList getGroups(@PathVariable("serviceName") String serviceName)
            throws IOException {
        return new JaxbGroupList(getService(serviceName).getUserGroups());
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/group/{group}/users", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbUserList getUsersFromGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupService service = getService(serviceName);
        return new JaxbUserList(service.getUsersForGroup(getGroup(service, groupName)));
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/user/{user}/groups", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    public @ResponseBody JaxbGroupList getGroupsFromUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName)
            throws IOException {
        GeoServerUserGroupService service = getService(serviceName);
        return new JaxbGroupList(service.getGroupsForUser(getUser(service, userName)));
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/users", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.CREATED) void insertUser(
            @PathVariable("serviceName") String serviceName, @RequestBody JaxbUser user)
            throws PasswordPolicyException, IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.addUser(user.toUser(store));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/user/{user}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void updateUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName,
            @RequestBody JaxbUser user) throws PasswordPolicyException, IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.updateUser(user.toUser(getUser(store, userName)));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/user/{user}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void deleteUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName)
            throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.removeUser(getUser(store, userName));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/group/{group}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.CREATED) void insertGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("group") String groupName)
            throws PasswordPolicyException, IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.addGroup(new GeoServerUserGroup(groupName));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/group/{group}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void deleteGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.removeGroup(getGroup(store, groupName));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/user/{user}/group/{group}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void associateUserToGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName,
            @PathVariable("group") String groupName) throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.associateUserToGroup(getUser(store, userName),
                    getGroup(store, groupName));
        } finally {
            store.store();
        }
    }

    @RequestMapping(value = "/rest/usergroup/service/{serviceName}/user/{user}/group/{group}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void disassociateUserFromGroup(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName,
            @PathVariable("group") String groupName) throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.disAssociateUserFromGroup(getUser(store, userName),
                    getGroup(store, groupName));
        } finally {
            store.store();
        }
    }
    
    protected GeoServerUserGroupService getService(String serviceName) throws IOException {
        GeoServerUserGroupService service = securityManager.loadUserGroupService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Provided user/group service does not exist: " + serviceName);
        } else {
            return securityManager.loadUserGroupService(serviceName);
        } 
    }

    protected GeoServerUserGroupStore getStore(String serviceName) throws IOException {
        GeoServerUserGroupService service = securityManager.loadUserGroupService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Provided user/group service does not exist: " + serviceName);
        } else if (service.canCreateStore()) {
            return securityManager.loadUserGroupService(serviceName).createStore();
        } else {
            throw new IOException("Provided UserGroupService is read-only.");
        }
    }
    
    protected GeoServerUser getUser(GeoServerUserGroupService service, String userName) throws IOException {
        GeoServerUser user = service.getUserByUsername(userName);
        if (user == null) {
            throw new IllegalArgumentException("Provided username does not exist: " + userName);
        }
        return user;
    }
    
    protected GeoServerUserGroup getGroup(GeoServerUserGroupService service, String groupName) throws IOException {
        GeoServerUserGroup group = service.getGroupByGroupname(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Provided groupname does not exist: " + groupName);
        }
        return group;
    }    

}
