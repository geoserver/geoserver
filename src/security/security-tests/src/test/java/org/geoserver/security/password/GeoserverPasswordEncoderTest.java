/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GeoserverPasswordEncoderTest extends GeoServerMockTestSupport {

    protected String testPassword = "geoserver";
    protected char[] testPasswordArray = testPassword.toCharArray();
    protected char[] emptyArray = new char[] {};
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    @Test
    public void testPlainTextEncoder() {
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();

        assertEquals(PasswordEncodingType.PLAIN, encoder.getEncodingType());
        assertEquals("plain:" + testPassword, encoder.encodePassword(testPassword, null));
        assertTrue(encoder.isResponsibleForEncoding("plain:123"));
        assertFalse(encoder.isResponsibleForEncoding("digest1:123"));

        String enc = encoder.encodePassword(testPassword, null);
        String enc2 = encoder.encodePassword(testPasswordArray, null);
        assertTrue(encoder.isPasswordValid(enc, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc, testPasswordArray, null));
        assertTrue(encoder.isPasswordValid(enc2, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc2, testPasswordArray, null));

        assertFalse(encoder.isPasswordValid(enc, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc, "plain:blabla".toCharArray(), null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla".toCharArray(), null));

        assertEquals(testPassword, encoder.decode(enc));
        assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc)));
        assertEquals(testPassword, encoder.decode(enc2));
        assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc2)));

        enc = encoder.encodePassword("", null);
        assertTrue(encoder.isPasswordValid(enc, "", null));
        enc2 = encoder.encodePassword(emptyArray, null);
        assertTrue(encoder.isPasswordValid(enc, emptyArray, null));
    }

    @Test
    public void testConfigPlainTextEncoder() {
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();
        GeoServerMultiplexingPasswordEncoder encoder2 =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());

        assertEquals(PasswordEncodingType.PLAIN, encoder.getEncodingType());
        assertEquals("plain:" + testPassword, encoder.encodePassword(testPassword, null));
        assertTrue(encoder.isResponsibleForEncoding("plain:123"));
        assertFalse(encoder.isResponsibleForEncoding("digest1:123"));

        String enc = encoder.encodePassword(testPassword, null);
        String enc2 = encoder.encodePassword(testPasswordArray, null);
        assertTrue(encoder.isPasswordValid(enc, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc, testPasswordArray, null));
        assertTrue(encoder.isPasswordValid(enc2, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc2, testPasswordArray, null));

        assertTrue(encoder2.isPasswordValid(enc, testPassword, null));
        assertTrue(encoder2.isPasswordValid(enc, testPasswordArray, null));
        assertTrue(encoder2.isPasswordValid(enc2, testPassword, null));
        assertTrue(encoder2.isPasswordValid(enc2, testPasswordArray, null));

        assertFalse(encoder.isPasswordValid(enc, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc, "plain:blabla".toCharArray(), null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla".toCharArray(), null));

        assertFalse(encoder2.isPasswordValid(enc, "plain:blabla", null));
        assertFalse(encoder2.isPasswordValid(enc, "plain:blabla".toCharArray(), null));
        assertFalse(encoder2.isPasswordValid(enc2, "plain:blabla", null));
        assertFalse(encoder2.isPasswordValid(enc2, "plain:blabla".toCharArray(), null));

        assertEquals(testPassword, encoder.decode(enc));
        assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc)));
        assertEquals(testPassword, encoder.decode(enc2));
        assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc2)));

        assertEquals(testPassword, encoder2.decode(enc));
        assertTrue(Arrays.equals(testPasswordArray, encoder2.decodeToCharArray(enc)));
        assertEquals(testPassword, encoder2.decode(enc2));
        assertTrue(Arrays.equals(testPasswordArray, encoder2.decodeToCharArray(enc2)));

        enc = encoder.encodePassword("", null);
        assertTrue(encoder.isPasswordValid(enc, "", null));
        assertTrue(encoder2.isPasswordValid(enc, "", null));
        enc2 = encoder.encodePassword(emptyArray, null);
        assertTrue(encoder.isPasswordValid(enc, emptyArray, null));
        assertTrue(encoder2.isPasswordValid(enc, emptyArray, null));
    }

    @Test
    public void testDigestEncoder() {
        GeoServerPasswordEncoder encoder = getDigestPasswordEncoder();
        GeoServerMultiplexingPasswordEncoder encoder2 =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());

        assertEquals(PasswordEncodingType.DIGEST, encoder.getEncodingType());
        assertTrue(encoder.encodePassword(testPassword, null).startsWith("digest1:"));

        String enc = encoder.encodePassword(testPassword, null);
        String enc2 = encoder.encodePassword(testPasswordArray, null);
        assertTrue(encoder.isPasswordValid(enc, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc, testPasswordArray, null));
        assertTrue(encoder.isPasswordValid(enc2, testPassword, null));
        assertTrue(encoder.isPasswordValid(enc2, testPasswordArray, null));

        assertTrue(encoder2.isPasswordValid(enc, testPassword, null));
        assertTrue(encoder2.isPasswordValid(enc, testPasswordArray, null));
        assertTrue(encoder2.isPasswordValid(enc2, testPassword, null));
        assertTrue(encoder2.isPasswordValid(enc2, testPasswordArray, null));

        assertFalse(encoder.isPasswordValid(enc, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc, "plain:blabla".toCharArray(), null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla", null));
        assertFalse(encoder.isPasswordValid(enc2, "plain:blabla".toCharArray(), null));

        assertFalse(encoder2.isPasswordValid(enc, "plain:blabla", null));
        assertFalse(encoder2.isPasswordValid(enc, "plain:blabla".toCharArray(), null));
        assertFalse(encoder2.isPasswordValid(enc2, "plain:blabla", null));
        assertFalse(encoder2.isPasswordValid(enc2, "plain:blabla".toCharArray(), null));

        enc = encoder.encodePassword("", null);
        assertTrue(encoder.isPasswordValid(enc, "", null));
        assertTrue(encoder2.isPasswordValid(enc, "", null));
        enc2 = encoder.encodePassword(emptyArray, null);
        assertTrue(encoder.isPasswordValid(enc, emptyArray, null));
        assertTrue(encoder2.isPasswordValid(enc, emptyArray, null));

        try {
            encoder.decode(enc);
            fail("Must fail, digested passwords cannot be decoded");
        } catch (UnsupportedOperationException ex) {
        }

        try {
            encoder2.decode(enc);
            fail("Must fail, digested passwords cannot be decoded");
        } catch (UnsupportedOperationException ex) {
        }

        // Test if encoding does not change between versions
        assertTrue(
                encoder.isPasswordValid(
                        "digest1:CTBPxdfHvqy0K0M6uoYlb3+fPFrfMhpTm7+ey5rL/1xGI4s6g8n/OrkXdcyqzJ3D",
                        testPassword,
                        null));

        assertTrue(
                encoder2.isPasswordValid(
                        "digest1:CTBPxdfHvqy0K0M6uoYlb3+fPFrfMhpTm7+ey5rL/1xGI4s6g8n/OrkXdcyqzJ3D",
                        testPassword,
                        null));
    }

    //    @Test public void testDigestEncoderBytes() {
    //        GeoServerPasswordEncoder encoder = getDigestPasswordEncoder();
    //        assertEquals(PasswordEncodingType.DIGEST,encoder.getEncodingType());
    //        assertTrue(encoder.encodePassword(testPassword.toCharArray(),
    // null).startsWith("digest1:"));
    //
    //        String enc = encoder.encodePassword(testPassword.toCharArray(), null);
    //        assertTrue(encoder.isPasswordValid(enc, testPassword.toCharArray(), null));
    //        assertFalse(encoder.isPasswordValid(enc, "digest1:blabla".toCharArray(), null));
    //
    //        try {
    //            encoder.decode(enc);
    //            fail("Must fail, digested passwords cannot be decoded");
    //        } catch (UnsupportedOperationException ex) {
    //        }
    //
    //        enc = encoder.encodePassword("".toCharArray(), null);
    //        assertTrue(encoder.isPasswordValid(enc, "".toCharArray(), null));
    //
    //        assertTrue(encoder.isPasswordValid(
    //            "digest1:vimlmdmyH+VoUV1jkM+p8/uIyDY+h+WOtmSYUPT6r3SWtkg26oi5E08Yfo1v7jzz",
    //            testPassword,null));
    //    }

    @Test
    public void testEmptyEncoder() {
        GeoServerPasswordEncoder encoder =
                getSecurityManager().loadPasswordEncoder(GeoServerEmptyPasswordEncoder.class);
        assertEquals(PasswordEncodingType.EMPTY, encoder.getEncodingType());
        String encodedPassword = encoder.getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER;

        assertEquals(encodedPassword, encoder.encodePassword((String) null, null));
        assertEquals(encodedPassword, encoder.encodePassword((char[]) null, null));
        assertEquals(encodedPassword, encoder.encodePassword("", null));
        assertEquals(encodedPassword, encoder.encodePassword(new char[] {}, null));
        assertEquals(encodedPassword, encoder.encodePassword("blbal", null));
        assertEquals(encodedPassword, encoder.encodePassword("blbal".toCharArray(), null));
        assertFalse(encoder.isPasswordValid(encodedPassword, "blabla", null));
        assertFalse(encoder.isPasswordValid(encodedPassword, "blabla".toCharArray(), null));
        assertFalse(encoder.isPasswordValid(encodedPassword, "", null));
        assertFalse(encoder.isPasswordValid(encodedPassword, "".toCharArray(), null));

        try {
            encoder.decode("");
            fail("Must fail, empty passwords cannot be decoded");
        } catch (UnsupportedOperationException ex) {
        }

        GeoServerMultiplexingPasswordEncoder encoder2 =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());
        assertFalse(encoder2.isPasswordValid(encodedPassword, "blabla", null));
        assertFalse(encoder2.isPasswordValid(encodedPassword, "blabla".toCharArray(), null));
        assertFalse(encoder2.isPasswordValid(encodedPassword, "", null));
        assertFalse(encoder2.isPasswordValid(encodedPassword, "".toCharArray(), null));

        try {
            encoder2.decode("");
            fail("Must fail, empty passwords cannot be decoded");
        } catch (UnsupportedOperationException ex) {
        }
    }

    protected List<GeoServerPasswordEncoder> getConfigPBEEncoders() {
        List<GeoServerPasswordEncoder> result = new ArrayList<GeoServerPasswordEncoder>();
        result.add(getPBEPasswordEncoder());
        if (getSecurityManager().isStrongEncryptionAvailable()) {
            result.add(getStrongPBEPasswordEncoder());
        } else {
            LOGGER.warning("Skipping strong encryption tests for configuration passwords");
        }
        return result;
    }

    @Test
    public void testConfigPBEEncoder() throws Exception {

        // TODO runs from eclpise, but not from mnv clean install
        // assertTrue("masterpw".equals(MasterPasswordProviderImpl.get().getMasterPassword()));

        GeoServerMultiplexingPasswordEncoder encoder2 =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager());

        for (GeoServerPasswordEncoder encoder : getConfigPBEEncoders()) {
            encoder.initialize(getSecurityManager());
            assertEquals(PasswordEncodingType.ENCRYPT, encoder.getEncodingType());

            assertTrue(
                    encoder.encodePassword(testPassword, null)
                            .startsWith(
                                    encoder.getPrefix()
                                            + AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));

            String enc = encoder.encodePassword(testPassword, null);
            String enc2 = encoder.encodePassword(testPasswordArray, null);
            assertTrue(encoder.isPasswordValid(enc, testPassword, null));
            assertTrue(encoder.isPasswordValid(enc, testPasswordArray, null));
            assertTrue(encoder.isPasswordValid(enc2, testPassword, null));
            assertTrue(encoder.isPasswordValid(enc2, testPasswordArray, null));

            assertTrue(encoder2.isPasswordValid(enc, testPassword, null));
            assertTrue(encoder2.isPasswordValid(enc, testPasswordArray, null));
            assertTrue(encoder2.isPasswordValid(enc2, testPassword, null));
            assertTrue(encoder2.isPasswordValid(enc2, testPasswordArray, null));

            assertFalse(encoder.isPasswordValid(enc, "crypt1:blabla", null));
            assertFalse(encoder.isPasswordValid(enc, "crypt1:blabla".toCharArray(), null));
            assertFalse(encoder.isPasswordValid(enc2, "crypt1:blabla", null));
            assertFalse(encoder.isPasswordValid(enc2, "crypt1:blabla".toCharArray(), null));

            assertFalse(encoder2.isPasswordValid(enc, "crypt1:blabla", null));
            assertFalse(encoder2.isPasswordValid(enc, "crypt1:blabla".toCharArray(), null));
            assertFalse(encoder2.isPasswordValid(enc2, "crypt1:blabla", null));
            assertFalse(encoder2.isPasswordValid(enc2, "crypt1:blabla".toCharArray(), null));

            assertEquals(testPassword, encoder.decode(enc));
            assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc)));
            assertEquals(testPassword, encoder.decode(enc2));
            assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc2)));

            assertEquals(testPassword, encoder2.decode(enc));
            assertTrue(Arrays.equals(testPasswordArray, encoder2.decodeToCharArray(enc)));
            assertEquals(testPassword, encoder2.decode(enc2));
            assertTrue(Arrays.equals(testPasswordArray, encoder2.decodeToCharArray(enc2)));

            enc = encoder.encodePassword("", null);
            assertTrue(encoder.isPasswordValid(enc, "", null));
            assertTrue(encoder2.isPasswordValid(enc, "", null));
            enc2 = encoder.encodePassword(emptyArray, null);
            assertTrue(encoder.isPasswordValid(enc, emptyArray, null));
            assertTrue(encoder2.isPasswordValid(enc, emptyArray, null));
        }
    }

    protected List<GeoServerPBEPasswordEncoder> getPBEEncoders() {
        List<GeoServerPBEPasswordEncoder> result = new ArrayList<GeoServerPBEPasswordEncoder>();
        result.add(getPBEPasswordEncoder());
        if (getSecurityManager().isStrongEncryptionAvailable()) {
            result.add(getStrongPBEPasswordEncoder());
        } else {
            LOGGER.warning("Skipping strong encryption tests for user passwords");
        }
        return result;
    }

    @Test
    public void testUserGroupServiceEncoder() throws Exception {

        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        // getPBEPasswordEncoder();

        //        boolean fail = true;
        //        try {
        //            encoder.initializeFor(service);
        //        } catch (IOException ex){
        //            fail = false;
        //        }
        //        assertFalse(fail);

        String password = "testpassword";
        char[] passwordArray = password.toCharArray();
        KeyStoreProvider keyStoreProvider = getSecurityManager().getKeyStoreProvider();
        keyStoreProvider.setUserGroupKey(service.getName(), password.toCharArray());

        GeoServerMultiplexingPasswordEncoder encoder3 =
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager(), service);

        for (GeoServerPBEPasswordEncoder encoder : getPBEEncoders()) {
            encoder.initializeFor(service);

            assertEquals(PasswordEncodingType.ENCRYPT, encoder.getEncodingType());
            assertEquals(
                    encoder.getKeyAliasInKeyStore(),
                    keyStoreProvider.aliasForGroupService(service.getName()));

            GeoServerPBEPasswordEncoder encoder2 =
                    (GeoServerPBEPasswordEncoder)
                            getSecurityManager().loadPasswordEncoder(encoder.getName());
            encoder2.initializeFor(service);

            assertFalse(encoder == encoder2);
            String enc = encoder.encodePassword(password, null);
            assertTrue(
                    enc.startsWith(
                            encoder.getPrefix()
                                    + AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));
            String encFromArray = encoder.encodePassword(passwordArray, null);
            assertTrue(
                    encFromArray.startsWith(
                            encoder.getPrefix()
                                    + AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));

            assertFalse(enc.equals(password));
            assertFalse(Arrays.equals(encFromArray.toCharArray(), passwordArray));

            assertTrue(encoder2.isPasswordValid(enc, password, null));
            assertTrue(encoder2.isPasswordValid(encFromArray, password, null));
            assertTrue(encoder2.isPasswordValid(enc, passwordArray, null));
            assertTrue(encoder2.isPasswordValid(encFromArray, passwordArray, null));

            assertTrue(encoder3.isPasswordValid(enc, password, null));
            assertTrue(encoder3.isPasswordValid(encFromArray, password, null));
            assertTrue(encoder3.isPasswordValid(enc, passwordArray, null));
            assertTrue(encoder3.isPasswordValid(encFromArray, passwordArray, null));

            assertEquals(password, encoder2.decode(enc));
            assertEquals(password, encoder3.decode(enc));
            assertEquals(password, encoder.decode(enc));
            assertEquals(password, encoder.decode(encFromArray));
            assertTrue(Arrays.equals(passwordArray, encoder.decodeToCharArray(enc)));
            assertTrue(Arrays.equals(passwordArray, encoder.decodeToCharArray(encFromArray)));
        }
    }

    @Test
    public void testCustomPasswordProvider() {
        ClassPathXmlApplicationContext appContext =
                new ClassPathXmlApplicationContext("classpath*:/passwordSecurityContext.xml");
        appContext.refresh();

        List<GeoServerPasswordEncoder> encoders =
                GeoServerExtensions.extensions(GeoServerPasswordEncoder.class, appContext);
        boolean found = false;
        for (GeoServerPasswordEncoder enc : encoders) {
            if (enc.getPrefix() != null && enc.getPrefix().equals("plain4711")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }
}
