/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geoserver.security.SecurityUtils;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;

public class ReadOnlyDataAccessTest extends SecureObjectsTest {

    private DataAccess da;

    private NameImpl name;

    @Before
    public void setUp() throws Exception {
        FeatureSource fs = createNiceMock(FeatureSource.class);
        replay(fs);
        FeatureType schema = createNiceMock(FeatureType.class);
        replay(schema);
        da = createNiceMock(DataAccess.class);
        name = new NameImpl("blah");
        expect(da.getFeatureSource(name)).andReturn(fs);
        replay(da);
    }

    @Test
    public void testDontChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.hide(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isHide());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testChallenge() throws Exception {
        ReadOnlyDataAccess ro = new ReadOnlyDataAccess(da, WrapperPolicy.readOnlyChallenge(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isReadOnlyChallenge());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e) == false)
                fail("Should have thrown a security exception...");
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e) == false)
                fail("Should have thrown a security exception...");
        }
    }
}
