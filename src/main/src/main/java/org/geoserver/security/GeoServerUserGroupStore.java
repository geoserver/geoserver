/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * A class implementing this interface implements a backend for user and group management. The store
 * always operates on a {@link GeoServerUserGroupService} object.
 *
 * @author christian
 */
public interface GeoServerUserGroupStore extends GeoServerUserGroupService {

    /** Initializes itself from a service for future store modifications concerning this service */
    void initializeFromService(GeoServerUserGroupService service) throws IOException;

    /** discards all entries */
    void clear() throws IOException;

    /**
     * Adds a user, the {@link GeoServerUser#getPassword()} returns the raw password
     *
     * <p>The method must use #getPasswordValidatorName() to validate the raw password and
     * #getPasswordEncoderName() to encode the password.
     */
    void addUser(GeoServerUser user) throws IOException, PasswordPolicyException;

    /**
     * Updates a user
     *
     * <p>The method must be able to determine if {@link GeoServerUser#getPassword()} has changed
     * (reread from backend, check for a prefix, ...)
     *
     * <p>if the password has changed, it is a raw password and the method must use
     * #getPasswordValidatorName() to validate the raw password and #getPasswordEncoderName() to
     *
     * <p>encode the password.
     */
    void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException;

    /** Removes the specified user */
    boolean removeUser(GeoServerUser user) throws IOException;

    /** Adds a group */
    void addGroup(GeoServerUserGroup group) throws IOException;

    /** Updates a group */
    void updateGroup(GeoServerUserGroup group) throws IOException;

    /** Removes the specified group. */
    boolean removeGroup(GeoServerUserGroup group) throws IOException;

    /**
     * Synchronizes all changes with the backend store.On success, the associated {@link
     * GeoServerUserGroupService} object should be loaded
     */
    void store() throws IOException;

    /** Associates a user with a group, on success */
    void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group) throws IOException;

    /** Disassociates a user from a group, on success */
    void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group) throws IOException;

    /**
     * returns true if there are pending modifications not written to the backend store
     *
     * @return true/false
     */
    boolean isModified();
}
