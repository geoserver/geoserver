/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Tests {@link SecurityMerger}'s add-only merge of users/groups/roles from a backup archive's security services into
 * the live target's existing services (the {@code BK_MERGE_SECURITY} restore mode), without touching the target's
 * configuration, keystore or master password.
 *
 * <p>The "archive" source is a copy of the live data directory's {@code security/} folder, loaded through a throwaway
 * {@link GeoServerSecurityManager} (the same pattern the restore tasklet uses), with an extra user and role added to
 * it. After merging it into the live target those entries must appear, while the pre-existing ones stay untouched.
 */
public class SecurityMergerTest extends GeoServerSystemTestSupport {

    private static final String MERGE_USER = "merge_src_user";
    private static final String MERGE_ROLE = "ROLE_MERGE_SRC";

    @Test
    public void testAddOnlyMergeAddsMissingUsersAndRoles() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        GeoServerSecurityManager target = getSecurityManager();
        String ugName = "default";
        GeoServerRoleService targetRoleService = target.getActiveRoleService();
        String roleServiceName = targetRoleService.getName();

        // 1. snapshot the live security folder into a temp data directory to act as the archive's source
        File sourceRoot = File.createTempFile("merge-src-", "dir", new File("target"));
        assertTrue(sourceRoot.delete());
        assertTrue(sourceRoot.mkdirs());
        File liveSecurity = getDataDirectory().get("security").dir();
        FileUtils.copyDirectory(liveSecurity, new File(sourceRoot, "security"));

        GeoServerSecurityManager source = null;
        try {
            source = new GeoServerSecurityManager(new GeoServerDataDirectory(new GeoServerResourceLoader(sourceRoot)));
            source.setApplicationContext(applicationContext);
            source.reload();

            // 2. add a user + role to the SOURCE that the target does not have
            GeoServerUserGroupService srcUg = source.loadUserGroupService(ugName);
            GeoServerUserGroupStore srcUgStore = srcUg.createStore();
            srcUgStore.addUser(srcUgStore.createUserObject(MERGE_USER, "MergeUserPwd1234", true));
            srcUgStore.store();

            GeoServerRoleService srcRoleService = source.loadRoleService(roleServiceName);
            GeoServerRoleStore srcRoleStore = srcRoleService.createStore();
            srcRoleStore.addRole(srcRoleStore.createRoleObject(MERGE_ROLE));
            srcRoleStore.store();

            // sanity: the target lacks both before the merge
            assertNull(target.loadUserGroupService(ugName).getUserByUsername(MERGE_USER));
            assertNull(target.loadRoleService(roleServiceName).getRoleByName(MERGE_ROLE));

            // 3. merge source -> target, then reload as the tasklet would
            SecurityMerger merger = new SecurityMerger(target);
            merger.merge(source);
            target.reload();

            // 4. the missing user + role were added, the pre-existing admin user was left untouched
            assertNotNull(
                    "the source-only user must be merged into the target",
                    target.loadUserGroupService(ugName).getUserByUsername(MERGE_USER));
            assertNotNull(
                    "the source-only role must be merged into the target",
                    target.loadRoleService(roleServiceName).getRoleByName(MERGE_ROLE));
            assertNotNull(
                    "the pre-existing admin user must be left intact",
                    target.loadUserGroupService(ugName).getUserByUsername("admin"));
            assertTrue("at least the one source-only user was added", merger.getUsersAdded() >= 1);
            assertTrue("at least the one source-only role was added", merger.getRolesAdded() >= 1);
        } finally {
            if (source != null) {
                try {
                    source.destroy();
                } catch (Exception ignore) {
                    // best effort
                }
            }
            // clean the merged entries out of the live target so other tests are unaffected
            cleanupTarget(target, ugName, roleServiceName);
            FileUtils.deleteQuietly(sourceRoot);
        }
    }

    private void cleanupTarget(GeoServerSecurityManager target, String ugName, String roleServiceName) {
        try {
            GeoServerUserGroupService ug = target.loadUserGroupService(ugName);
            if (ug.canCreateStore() && ug.getUserByUsername(MERGE_USER) != null) {
                GeoServerUserGroupStore store = ug.createStore();
                store.removeUser(store.getUserByUsername(MERGE_USER));
                store.store();
            }
            GeoServerRoleService roles = target.loadRoleService(roleServiceName);
            if (roles.canCreateStore() && roles.getRoleByName(MERGE_ROLE) != null) {
                GeoServerRoleStore store = roles.createStore();
                store.removeRole(store.getRoleByName(MERGE_ROLE));
                store.store();
            }
            target.reload();
        } catch (Exception ignore) {
            // best effort cleanup
        }
    }
}
