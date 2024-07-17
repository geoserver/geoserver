/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import java.util.SortedSet;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.SequentialExecutionController;
import org.geoserver.rest.security.xml.JaxbGroupList;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geoserver.rest.security.xml.JaxbUserList;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "usergroupRestController")
@RequestMapping(path = RestBaseController.ROOT_PATH + "/security/usergroup")
public class UsersRestController implements SequentialExecutionController {

    protected GeoServerSecurityManager securityManager;

    private static final String DEFAULT_ROLE_SERVICE_NAME = "default";

    private String getDefaultServiceName() {
        String defaultServiceName =
                System.getProperty("org.geoserver.rest.DefaultUserGroupServiceName");
        return defaultServiceName == null ? DEFAULT_ROLE_SERVICE_NAME : defaultServiceName;
    }

    public UsersRestController(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void somethingNotFound(IllegalArgumentException exception, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @GetMapping(
            value = "/users",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbUserList getUsers() throws IOException {
        return getUsers(getDefaultServiceName());
    }

    @GetMapping(
            value = "/groups",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbGroupList getGroups() throws IOException {
        return getGroups(getDefaultServiceName());
    }

    @GetMapping(
            value = "/group/{group}/users",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbUserList getUsersFromGroup(@PathVariable("group") String groupName)
            throws IOException {
        return getUsersFromGroup(getDefaultServiceName(), groupName);
    }

    @GetMapping(
            value = "/user/{user}/groups",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbGroupList getGroupsFromUser(@PathVariable("user") String userName)
            throws IOException {
        return getGroupsFromUser(getDefaultServiceName(), userName);
    }

    @PostMapping(value = "/users")
    public @ResponseStatus(HttpStatus.CREATED) void insertUser(@RequestBody JaxbUser user)
            throws PasswordPolicyException, IOException {
        insertUser(getDefaultServiceName(), user);
    }

    @PostMapping(value = "/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void updateUser(
            @PathVariable("user") String userName, @RequestBody JaxbUser user)
            throws PasswordPolicyException, IOException {
        updateUser(getDefaultServiceName(), userName, user);
    }

    @DeleteMapping(value = "/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void deleteUser(@PathVariable("user") String userName)
            throws IOException {
        deleteUser(getDefaultServiceName(), userName);
    }

    @PostMapping(value = "/group/{group}")
    public @ResponseStatus(HttpStatus.CREATED) void insertGroup(
            @PathVariable("group") String groupName) throws PasswordPolicyException, IOException {
        insertGroup(getDefaultServiceName(), groupName);
    }

    @DeleteMapping(value = "/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void deleteGroup(@PathVariable("group") String groupName)
            throws IOException {
        deleteGroup(getDefaultServiceName(), groupName);
    }

    @PostMapping(value = "/user/{user}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void associateUserToGroup(
            @PathVariable("user") String userName, @PathVariable("group") String groupName)
            throws IOException {
        associateUserToGroup(getDefaultServiceName(), userName, groupName);
    }

    @DeleteMapping(value = "/user/{user}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void disassociateUserFromGroup(
            @PathVariable("user") String userName, @PathVariable("group") String groupName)
            throws IOException {
        disassociateUserFromGroup(getDefaultServiceName(), userName, groupName);
    }

    @GetMapping(
            value = "/service/{serviceName}/users",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbUserList getUsers(@PathVariable("serviceName") String serviceName)
            throws IOException {
        return new JaxbUserList(getService(serviceName).getUsers());
    }

    @GetMapping(
            value = "/service/{serviceName}/groups",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbGroupList getGroups(@PathVariable("serviceName") String serviceName)
            throws IOException {
        return new JaxbGroupList(getService(serviceName).getUserGroups());
    }

    @GetMapping(
            value = "/service/{serviceName}/group/{group}/users",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbUserList getUsersFromGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupService service = getService(serviceName);
        return new JaxbUserList(service.getUsersForGroup(getGroup(service, groupName)));
    }

    @GetMapping(
            value = "/service/{serviceName}/user/{user}/groups",
            produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public JaxbGroupList getGroupsFromUser(
            @PathVariable("serviceName") String serviceName, @PathVariable("user") String userName)
            throws IOException {
        GeoServerUserGroupService service = getService(serviceName);
        return new JaxbGroupList(service.getGroupsForUser(getUser(service, userName)));
    }

    @PostMapping(value = "/service/{serviceName}/users")
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

    @PostMapping(value = "/service/{serviceName}/user/{user}")
    public @ResponseStatus(HttpStatus.OK) void updateUser(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("user") String userName,
            @RequestBody JaxbUser user)
            throws PasswordPolicyException, IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.updateUser(user.toUser(getUser(store, userName)));
        } finally {
            store.store();
        }
    }

    @DeleteMapping(value = "/service/{serviceName}/user/{user}")
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

    @PostMapping(value = "/service/{serviceName}/group/{group}")
    public @ResponseStatus(HttpStatus.CREATED) void insertGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("group") String groupName)
            throws PasswordPolicyException, IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.addGroup(new GeoServerUserGroup(groupName));
        } finally {
            store.store();
        }
    }

    @DeleteMapping(value = "/service/{serviceName}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void deleteGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.removeGroup(getGroup(store, groupName));
        } finally {
            store.store();
        }
    }

    @PostMapping(value = "/service/{serviceName}/user/{user}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void associateUserToGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("user") String userName,
            @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        GeoServerUserGroupService service = getService(serviceName);
        SortedSet<GeoServerUserGroup> groupsForUser =
                service.getGroupsForUser(
                        getUser(service, userName)); // There should be fewer groups than users
        for (GeoServerUserGroup group : groupsForUser) {
            if (groupName.equals(group.getGroupname())) {
                throw new RestException(
                        "Username already associated with this groupname",
                        HttpStatus.OK); // In the future 409 Conflict?
            }
        }
        try {
            store.associateUserToGroup(getUser(store, userName), getGroup(store, groupName));
        } finally {
            store.store();
        }
    }

    @DeleteMapping(value = "/service/{serviceName}/user/{user}/group/{group}")
    public @ResponseStatus(HttpStatus.OK) void disassociateUserFromGroup(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("user") String userName,
            @PathVariable("group") String groupName)
            throws IOException {
        GeoServerUserGroupStore store = getStore(serviceName);
        try {
            store.disAssociateUserFromGroup(getUser(store, userName), getGroup(store, groupName));
        } finally {
            store.store();
        }
    }

    protected GeoServerUserGroupService getService(String serviceName) throws IOException {
        GeoServerUserGroupService service = securityManager.loadUserGroupService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException(
                    "Provided user/group service does not exist: " + serviceName);
        } else {
            return securityManager.loadUserGroupService(serviceName);
        }
    }

    protected GeoServerUserGroupStore getStore(String serviceName) throws IOException {
        GeoServerUserGroupService service = securityManager.loadUserGroupService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException(
                    "Provided user/group service does not exist: " + serviceName);
        } else if (service.canCreateStore()) {
            return securityManager.loadUserGroupService(serviceName).createStore();
        } else {
            throw new IOException("Provided UserGroupService is read-only.");
        }
    }

    protected GeoServerUser getUser(GeoServerUserGroupService service, String userName)
            throws IOException {
        GeoServerUser user = service.getUserByUsername(userName);
        if (user == null) {
            throw new IllegalArgumentException("Provided username does not exist: " + userName);
        }
        return user;
    }

    protected GeoServerUserGroup getGroup(GeoServerUserGroupService service, String groupName)
            throws IOException {
        GeoServerUserGroup group = service.getGroupByGroupname(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Provided groupname does not exist: " + groupName);
        }
        return group;
    }
}
