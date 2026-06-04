/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geotools.util.logging.Logging;

/**
 * Add-only merge of user/group and role data from a backup archive's security services into the live target's existing
 * services, used by the {@code BK_MERGE_SECURITY} restore mode.
 *
 * <p>It migrates users / groups / roles across instances <em>without</em> replacing the target's security
 * configuration, keystore or master password — so it works even when the target's master password differs from the
 * source's, where a verbatim security copy would carry the source-encrypted keystore (unreadable on the target).
 *
 * <p><b>Add-only:</b> an entry already present in the target by name is left untouched — the target's own admin
 * credentials are never overwritten; only entries the target lacks are added. Encoded passwords are carried verbatim
 * (GeoServer's user store keeps an already-prefixed password as-is, see
 * {@code AbstractUserGroupStore.preparePassword}), so one-way digest passwords keep working on the target; reversible
 * (PBE / keystore-bound) passwords are carried but cannot be decoded on a target with a different keystore and must be
 * reset there. A user-group / role service the target lacks, or a read-only one (e.g. LDAP), is skipped with a warning
 * rather than merged.
 */
public class SecurityMerger {

    private static final Logger LOGGER = Logging.getLogger(SecurityMerger.class);

    private final GeoServerSecurityManager target;

    private int usersAdded;
    private int groupsAdded;
    private int rolesAdded;
    private final List<String> warnings = new ArrayList<>();

    public SecurityMerger(GeoServerSecurityManager target) {
        this.target = target;
    }

    public int getUsersAdded() {
        return usersAdded;
    }

    public int getGroupsAdded() {
        return groupsAdded;
    }

    public int getRolesAdded() {
        return rolesAdded;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    /** Merges every user-group and role service found in {@code source} into the target's matching services. */
    public void merge(GeoServerSecurityManager source) throws IOException {
        for (String name : source.listUserGroupServices()) {
            mergeUserGroupService(source, name);
        }
        for (String name : source.listRoleServices()) {
            mergeRoleService(source, name);
        }
    }

    private void mergeUserGroupService(GeoServerSecurityManager source, String name) throws IOException {
        GeoServerUserGroupService sourceService = source.loadUserGroupService(name);
        if (sourceService == null) {
            return;
        }
        GeoServerUserGroupService targetService = target.loadUserGroupService(name);
        if (targetService == null) {
            warnings.add("user-group service '" + name + "' does not exist on the target; skipping its users/groups");
            return;
        }
        if (!targetService.canCreateStore()) {
            warnings.add("user-group service '" + name + "' is read-only on the target (e.g. LDAP); skipping merge");
            return;
        }

        // createStore() returns a store already initialized from the (unwrapped) service, holding the target's
        // current users and groups; the loops below only add entries the store does not already contain.
        GeoServerUserGroupStore store = targetService.createStore();

        // add-only groups
        for (GeoServerUserGroup group : sourceService.getUserGroups()) {
            if (store.getGroupByGroupname(group.getGroupname()) == null) {
                store.addGroup(store.createGroupObject(group.getGroupname(), group.isEnabled()));
                groupsAdded++;
            }
        }
        // add-only users (encoded password carried verbatim by the store)
        for (GeoServerUser user : sourceService.getUsers()) {
            if (store.getUserByUsername(user.getUsername()) == null) {
                GeoServerUser copy = store.createUserObject(user.getUsername(), user.getPassword(), user.isEnabled());
                copy.getProperties().putAll(user.getProperties());
                try {
                    store.addUser(copy);
                    usersAdded++;
                } catch (PasswordPolicyException e) {
                    warnings.add("could not add user '" + user.getUsername() + "': " + e.getMessage());
                }
            }
        }
        // group memberships for the newly mergeable users
        for (GeoServerUser user : sourceService.getUsers()) {
            GeoServerUser targetUser = store.getUserByUsername(user.getUsername());
            if (targetUser == null) {
                continue;
            }
            for (GeoServerUserGroup group : sourceService.getGroupsForUser(user)) {
                GeoServerUserGroup targetGroup = store.getGroupByGroupname(group.getGroupname());
                if (targetGroup != null) {
                    store.associateUserToGroup(targetUser, targetGroup);
                }
            }
        }
        store.store();
        LOGGER.info(() -> "Merged user-group service '" + name + "': +" + usersAdded + " users, +" + groupsAdded
                + " groups (add-only)");
    }

    private void mergeRoleService(GeoServerSecurityManager source, String name) throws IOException {
        GeoServerRoleService sourceService = source.loadRoleService(name);
        if (sourceService == null) {
            return;
        }
        GeoServerRoleService targetService = target.loadRoleService(name);
        if (targetService == null) {
            warnings.add("role service '" + name + "' does not exist on the target; skipping its roles");
            return;
        }
        if (!targetService.canCreateStore()) {
            warnings.add("role service '" + name + "' is read-only on the target; skipping merge");
            return;
        }

        // createStore() returns a store already initialized from the (unwrapped) service, holding the target's
        // current roles; the loops below only add roles/associations the store does not already contain.
        GeoServerRoleStore store = targetService.createStore();

        // add-only roles
        for (GeoServerRole role : sourceService.getRoles()) {
            if (store.getRoleByName(role.getAuthority()) == null) {
                store.addRole(store.createRoleObject(role.getAuthority()));
                rolesAdded++;
            }
        }
        // role hierarchy (parent), and user/group associations, for roles now present on both sides
        for (GeoServerRole role : sourceService.getRoles()) {
            GeoServerRole targetRole = store.getRoleByName(role.getAuthority());
            if (targetRole == null) {
                continue;
            }
            GeoServerRole sourceParent = sourceService.getParentRole(role);
            if (sourceParent != null) {
                GeoServerRole targetParent = store.getRoleByName(sourceParent.getAuthority());
                if (targetParent != null) {
                    store.setParentRole(targetRole, targetParent);
                }
            }
            for (String username : sourceService.getUserNamesForRole(role)) {
                store.associateRoleToUser(targetRole, username);
            }
            for (String groupname : sourceService.getGroupNamesForRole(role)) {
                store.associateRoleToGroup(targetRole, groupname);
            }
        }
        store.store();
        LOGGER.info(() -> "Merged role service '" + name + "': +" + rolesAdded + " roles (add-only)");
    }
}
