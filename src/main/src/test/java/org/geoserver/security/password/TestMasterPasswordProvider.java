/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.io.IOException;

import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.MasterPasswordProvider;

public final class TestMasterPasswordProvider extends MasterPasswordProvider {

    @Override
    protected char[] doGetMasterPassword() throws Exception {
        return "geoserver3".toCharArray();
    }

    @Override
    protected void doSetMasterPassword(char[] passwd) throws Exception {
    }

    public static class Provider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends MasterPasswordProvider> getMasterPasswordProviderClass() {
            return TestMasterPasswordProvider.class;
        }

        @Override
        public MasterPasswordProvider createMasterPasswordProvider(
                MasterPasswordProviderConfig config) throws IOException {
            return new TestMasterPasswordProvider();
        }
    }
}
