/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class PwdEnvParametrizationTest extends AbstractAuthenticationProviderTest {

    private static final String PLAIN_TXT_PRV = "plainProvider";
    private static final String PBE_PRV = "pbeProvider";
    private static final String STRONG_PBE_PRV = "strongProvider";

    private static final String PLAIN_UG_SRV = "plainTextUgSrv";

    private static final String PBE_UG_SRV = "pbeUgSrv";

    private static final String STRONG_PBE_UG_SRV = "strongPbeUgSrv";

    @BeforeClass
    public static void setupClass() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("ALLOW_ENV_PARAMETRIZATION");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        createUserGroupService(PLAIN_UG_SRV, getPlainTextPasswordEncoder().getName());
        createUserGroupService(PBE_UG_SRV, getPBEPasswordEncoder().getName());
        createUserGroupService(STRONG_PBE_UG_SRV, getStrongPBEPasswordEncoder().getName());
        UsernamePasswordAuthenticationProviderConfig plainTxt =
                new UsernamePasswordAuthenticationProviderConfig();
        plainTxt.setUserGroupServiceName(PLAIN_UG_SRV);
        plainTxt.setName(PLAIN_TXT_PRV);
        plainTxt.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
        getSecurityManager().saveAuthenticationProvider(plainTxt);
        UsernamePasswordAuthenticationProviderConfig pbe =
                new UsernamePasswordAuthenticationProviderConfig();
        pbe.setUserGroupServiceName(PBE_UG_SRV);
        pbe.setName(PBE_PRV);
        pbe.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
        getSecurityManager().saveAuthenticationProvider(pbe);

        UsernamePasswordAuthenticationProviderConfig strongPbe =
                new UsernamePasswordAuthenticationProviderConfig();
        strongPbe.setUserGroupServiceName(STRONG_PBE_UG_SRV);
        strongPbe.setName(STRONG_PBE_PRV);
        strongPbe.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
        getSecurityManager().saveAuthenticationProvider(strongPbe);
    }

    @Test
    public void testPbeUgSrvWithPlainTxtPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd1";
        System.setProperty(key, "plain:my_password");
        try {
            createGeoServerUser("test_env1", "${env.pwd1}", PBE_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PBE_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env1", "my_password");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testPbeUgSrvWithDigestPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd2";
        System.setProperty(
                key, "digest1:D9miJH/hVgfxZJscMafEtbtliG0ROxhLfsznyWfG38X2pda2JOSV4POi55PQI4tw");
        try {
            createGeoServerUser("test_env2", "${env.pwd2}", PBE_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PBE_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env2", "geoserver");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testPlainTxtUgSrvWithPlainTxtPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd3";
        System.setProperty(key, "plain:my_password");
        try {
            createGeoServerUser("test_env3", "${env.pwd3}", PLAIN_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PLAIN_TXT_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env3", "my_password");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testPlainTxtUgSrvWithDigestPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd4";
        System.setProperty(
                key, "digest1:D9miJH/hVgfxZJscMafEtbtliG0ROxhLfsznyWfG38X2pda2JOSV4POi55PQI4tw");
        try {
            createGeoServerUser("test_env4", "${env.pwd4}", PLAIN_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PLAIN_TXT_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env4", "geoserver");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testStrongPBETxtUgSrvWithPlainTxtPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd5";
        System.setProperty(key, "plain:my_password");
        try {
            createGeoServerUser("test_env5", "${env.pwd5}", STRONG_PBE_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(STRONG_PBE_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env5", "my_password");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testStrongPBEUgSrvWithDigestPwd() throws IOException, PasswordPolicyException {
        String key = "env.pwd6";
        System.setProperty(
                key, "digest1:D9miJH/hVgfxZJscMafEtbtliG0ROxhLfsznyWfG38X2pda2JOSV4POi55PQI4tw");
        try {
            createGeoServerUser("test_env6", "${env.pwd6}", PLAIN_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PLAIN_TXT_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env6", "geoserver");
            authentication = provider.authenticate(authentication);
            assertNotNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testStrongPBEUgSrvAuthFails() throws IOException, PasswordPolicyException {
        String key = "env.pwd7";
        System.setProperty(
                key, "digest1:D9miJH/hVgfxZJscMafEtbtliG0ROxhLfsznyWfG38X2pda2JOSV4POi55PQI4tw");
        try {
            createGeoServerUser("test_env7", "${env.pwd7}", PLAIN_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PLAIN_TXT_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env7", "wrong");
            authentication = provider.authenticate(authentication);
            assertNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testPlainTxtUgSrvAuthFails() throws IOException, PasswordPolicyException {
        String key = "env.pwd8";
        System.setProperty(
                key, "digest1:D9miJH/hVgfxZJscMafEtbtliG0ROxhLfsznyWfG38X2pda2JOSV4POi55PQI4tw");
        try {
            createGeoServerUser("test_env8", "${env.pwd8}", PLAIN_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PLAIN_TXT_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env8", "wrong");
            authentication = provider.authenticate(authentication);
            assertNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    @Test
    public void testPbeUgSrvAuthFails() throws IOException, PasswordPolicyException {
        String key = "env.pwd9";
        System.setProperty(key, "plain:my_password");
        try {
            createGeoServerUser("test_env9", "${env.pwd9}", PBE_UG_SRV);
            AuthenticationProvider provider =
                    getSecurityManager().loadAuthenticationProvider(PBE_PRV);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("test_env9", "wrong");
            authentication = provider.authenticate(authentication);
            assertNull(authentication);
        } finally {
            removePwdEnv(key);
        }
    }

    private void createGeoServerUser(String username, String pwd, String ugSrvName)
            throws IOException, PasswordPolicyException {
        GeoServerUserGroupService groupService =
                getSecurityManager().loadUserGroupService(ugSrvName);
        GeoServerUserGroupStore userGroupStore = groupService.createStore();
        GeoServerUser user = userGroupStore.createUserObject(username, pwd, true);
        userGroupStore.addUser(user);
        userGroupStore.store();
    }

    private void removePwdEnv(String env_var) {
        System.clearProperty(env_var);
    }
}
