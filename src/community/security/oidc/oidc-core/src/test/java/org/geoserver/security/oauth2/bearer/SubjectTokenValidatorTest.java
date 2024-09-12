/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class SubjectTokenValidatorTest {

    String userName = "MYUSERNAME";

    public SubjectTokenValidator getValidator() {
        SubjectTokenValidator validator = new SubjectTokenValidator();
        return validator;
    }

    @Test
    public void testAzureGood() throws Exception {
        Map jwt = new HashMap();
        Map userInfo = new HashMap();

        jwt.put("sub", userName);
        userInfo.put("sub", userName);

        SubjectTokenValidator validator = getValidator();
        validator.verifyToken(null, jwt, userInfo);
    }

    @Test
    public void testKeyCloakGood() throws Exception {
        Map jwt = new HashMap();
        Map xms_st = new HashMap();

        Map userInfo = new HashMap();

        xms_st.put("sub", userName);
        jwt.put("xms_st", xms_st);
        userInfo.put("sub", userName);

        SubjectTokenValidator validator = getValidator();
        validator.verifyToken(null, jwt, userInfo);
    }

    @Test(expected = Exception.class)
    public void testbad1() throws Exception {
        Map jwt = new HashMap();
        Map userInfo = new HashMap();

        SubjectTokenValidator validator = getValidator();
        validator.verifyToken(null, jwt, userInfo);
    }

    @Test(expected = Exception.class)
    public void testbad2() throws Exception {
        Map jwt = new HashMap();
        Map xms_st = new HashMap();

        Map userInfo = new HashMap();

        xms_st.put("sub", "baduser");
        jwt.put("xms_st", xms_st);
        userInfo.put("sub", userName);

        SubjectTokenValidator validator = getValidator();
        validator.verifyToken(null, jwt, userInfo);
    }
}
