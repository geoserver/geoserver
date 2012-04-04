package org.geoserver.security.password;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geotools.util.logging.Logging;

public class GeoserverPasswordEncoderTest extends GeoServerSecurityTestSupport {

    
    protected String testPassword="geoserver";
    protected char[] testPasswordArray=testPassword.toCharArray();
    protected char[] emptyArray = new char[] {};
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");

    @Override
    protected String[] getSpringContextLocations() {
        String[] locations = super.getSpringContextLocations();
        String[] newLocations=null;
        String classPath = "classpath*:/passwordSecurityContext.xml";
        if (locations==null) {
            newLocations = new String[] {classPath};
        } else {        
            newLocations =Arrays.copyOf(locations, locations.length+1);
            newLocations[newLocations.length-1]=classPath;
        }
        return newLocations;
    }

        
    public void testPlainTextEncoder() {
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();

        assertEquals(PasswordEncodingType.PLAIN,encoder.getEncodingType());
        assertEquals("plain:"+testPassword,encoder.encodePassword(testPassword, null));
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
    
    public void testConfigPlainTextEncoder() {
        GeoServerPasswordEncoder encoder = getPlainTextPasswordEncoder();
        
        assertEquals(PasswordEncodingType.PLAIN,encoder.getEncodingType());
        assertEquals("plain:"+testPassword,encoder.encodePassword(testPassword, null));
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

    
    public void testDigestEncoder() {
        GeoServerPasswordEncoder encoder = getDigestPasswordEncoder();

        assertEquals(PasswordEncodingType.DIGEST,encoder.getEncodingType());
        assertTrue(encoder.encodePassword(testPassword, null).startsWith("digest1:"));
        
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

                
        enc = encoder.encodePassword("", null);
        assertTrue(encoder.isPasswordValid(enc, "", null));
        enc2 = encoder.encodePassword(emptyArray, null);
        assertTrue(encoder.isPasswordValid(enc, emptyArray, null));

        try {
            encoder.decode(enc);
            fail("Must fail, digested passwords cannot be decoded");
        } catch (UnsupportedOperationException ex) {            
        }


        // Test if encoding does not change between versions 
        assertTrue(encoder.isPasswordValid(
                "digest1:CTBPxdfHvqy0K0M6uoYlb3+fPFrfMhpTm7+ey5rL/1xGI4s6g8n/OrkXdcyqzJ3D",
                testPassword,null));
    }

//    public void testDigestEncoderBytes() {
//        GeoServerPasswordEncoder encoder = getDigestPasswordEncoder();
//        assertEquals(PasswordEncodingType.DIGEST,encoder.getEncodingType());
//        assertTrue(encoder.encodePassword(testPassword.toCharArray(), null).startsWith("digest1:"));
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

    public void testEmptyEncoder() {
        GeoServerPasswordEncoder encoder = new GeoServerEmptyPasswordEncoder();
        assertEquals(PasswordEncodingType.EMPTY, encoder.getEncodingType());

        assertNull(encoder.encodePassword((String)null, null));
        assertNull(encoder.encodePassword((char[])null, null));
        assertNull(encoder.encodePassword("", null));
        assertNull(encoder.encodePassword(new char[]{}, null));

        try {
            encoder.encodePassword(testPassword, null); 
            fail("non null/empty password should fail");
        }
        catch(IllegalArgumentException e) {}

        try {
            encoder.encodePassword(testPassword.toCharArray(), null); 
            fail("non null/empty password should fail");
        }
        catch(IllegalArgumentException e) {}
    }

    protected List<String> getConfigPBEEncoderNames() {
        List<String> result = new ArrayList<String>();
        result.add(getPBEPasswordEncoder().getName());
        if (getSecurityManager().isStrongEncryptionAvailable()) {
            result.add(getStrongPBEPasswordEncoder().getName());
        } else {
            LOGGER.warning("Skipping strong encryption tests for configuration passwords");
        }
        return result;
    }
    
    public void testConfigPBEEncoder() throws Exception {
        
        // TODO runs from eclpise, but not from mnv clean install 
        //assertTrue("masterpw".equals(MasterPasswordProviderImpl.get().getMasterPassword()));
        
        System.out.println("Strong cryptography enabled: " +
            getSecurityManager().isStrongEncryptionAvailable());

        

        List<String> encoderNames = getConfigPBEEncoderNames();
        for (String encoderName: encoderNames) {
            GeoServerPasswordEncoder encoder = (GeoServerPBEPasswordEncoder) 
                    GeoServerExtensions.bean(encoderName);
            encoder.initialize(getSecurityManager());
            assertEquals(PasswordEncodingType.ENCRYPT,encoder.getEncodingType());
            
            assertTrue(encoder.encodePassword(testPassword, null).
                    startsWith(encoder.getPrefix()+AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));
            
            
            String enc = encoder.encodePassword(testPassword, null);
            String enc2 = encoder.encodePassword(testPasswordArray, null);
            assertTrue(encoder.isPasswordValid(enc, testPassword, null));
            assertTrue(encoder.isPasswordValid(enc, testPasswordArray, null));
            assertTrue(encoder.isPasswordValid(enc2, testPassword, null));
            assertTrue(encoder.isPasswordValid(enc2, testPasswordArray, null));
            
            assertFalse(encoder.isPasswordValid(enc, "crypt1:blabla", null));
            assertFalse(encoder.isPasswordValid(enc, "crypt1:blabla".toCharArray(), null));
            assertFalse(encoder.isPasswordValid(enc2, "crypt1:blabla", null));
            assertFalse(encoder.isPasswordValid(enc2, "crypt1:blabla".toCharArray(), null));

            
            assertEquals(testPassword, encoder.decode(enc));        
            assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc)));
            assertEquals(testPassword, encoder.decode(enc2));
            assertTrue(Arrays.equals(testPasswordArray, encoder.decodeToCharArray(enc2)));
            
            enc = encoder.encodePassword("", null);
            assertTrue(encoder.isPasswordValid(enc, "", null));
            enc2 = encoder.encodePassword(emptyArray, null);
            assertTrue(encoder.isPasswordValid(enc, emptyArray, null));
            
            
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

    
    public void testUserGroupServiceEncoder() throws Exception {
        
        GeoServerUserGroupService service = getSecurityManager().
                loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
        
        getPBEPasswordEncoder();

//        boolean fail = true;
//        try {
//            encoder.initializeFor(service);
//        } catch (IOException ex){
//            fail = false;
//        }
//        assertFalse(fail);
        
        String password = "testpassword";
        char [] passwordArray = password.toCharArray();
        KeyStoreProvider keyStoreProvider = getSecurityManager().getKeyStoreProvider();
        keyStoreProvider.setUserGroupKey(service.getName(), password.toCharArray());
        
        for (GeoServerPBEPasswordEncoder encoder: getPBEEncoders()) {
            encoder.initializeFor(service);
                         
            assertEquals(PasswordEncodingType.ENCRYPT,encoder.getEncodingType());
            assertEquals(encoder.getKeyAliasInKeyStore(),
                keyStoreProvider.aliasForGroupService(service.getName()));

            GeoServerPBEPasswordEncoder encoder2 = (GeoServerPBEPasswordEncoder) 
                getSecurityManager().loadPasswordEncoder(encoder.getName());
            encoder2.initializeFor(service);
        
            assertFalse(encoder==encoder2);        
            String enc = encoder.encodePassword(password , null);                        
            assertTrue(enc.
                    startsWith(encoder.getPrefix()+AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));
            String encFromArray = encoder.encodePassword(passwordArray , null);                        
            assertTrue(encFromArray.
                    startsWith(encoder.getPrefix()+AbstractGeoserverPasswordEncoder.PREFIX_DELIMTER));
            
            assertFalse(enc.equals(password ));
            assertFalse(Arrays.equals(encFromArray.toCharArray(),passwordArray));
            
            assertTrue(encoder2.isPasswordValid(enc, password , null));
            assertTrue(encoder2.isPasswordValid(encFromArray, password , null));
            assertTrue(encoder2.isPasswordValid(enc, passwordArray , null));
            assertTrue(encoder2.isPasswordValid(encFromArray, passwordArray , null));

            
            assertEquals(password ,encoder2.decode(enc));
            assertEquals(password ,encoder.decode(enc));
            assertEquals(password ,encoder.decode(encFromArray));
            assertTrue(Arrays.equals(passwordArray ,encoder.decodeToCharArray(enc)));
            assertTrue(Arrays.equals(passwordArray ,encoder.decodeToCharArray(encFromArray)));
        }
    }
    
    public void testCustomPasswordProvider() {
        List<GeoServerPasswordEncoder> encoders = GeoServerExtensions.extensions(GeoServerPasswordEncoder.class);
        boolean found = false;
        for (GeoServerPasswordEncoder enc : encoders) {
            if (enc.getPrefix()!= null && enc.getPrefix().equals("plain4711")) {
                found=true;
                break;
            }            		
        }
        assertTrue(found);
    }
        
}
