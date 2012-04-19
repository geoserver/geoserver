/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.password;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * {@link HttpSessionListener} implementation creating an URL
 * password encrypter on session creation and scrambling its
 * key on session termination.
 * 
 * 
 * @author christian
 *
 */
public class URLParamEncryptionHttpSessionListener implements HttpSessionListener {

    private GeoServerSecurityManager manager;
    
    public final static String SESSION_PARAM_NAME = "_URL_PARAM_ENCODER"; 
    static String SESSION_PARAM_KEYNAME = "_URL_PARAM_ENCODER_KEY";
    /**
     * Needed if {@link GeoServerSecurityManager#isEncryptingUrlParams()} was false
     * on session creation but is true now. 
     * 
     * If the encrypting code misses a key it can register a new one on the fly. 
     * 
     * @param manager
     * @param session
     */
    public static StandardPBEByteEncryptor addEncrypterToSession (GeoServerSecurityManager manager, HttpSession session) {
        char[] key = manager.getRandomPassworddProvider().getRandomPasswordWithDefaultLength();
        
        StandardPBEByteEncryptor enc = new StandardPBEByteEncryptor();
        enc.setPasswordCharArray(key);
        
        if (manager.isStrongEncryptionAvailable()) {
            enc.setProvider(new BouncyCastleProvider());
            enc.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
        }
        else // US export restrictions
            enc.setAlgorithm("PBEWITHMD5ANDDES");

        
        session.setAttribute(SESSION_PARAM_NAME, enc);
        session.setAttribute(SESSION_PARAM_KEYNAME, key);
        return enc;
    }
    
    public URLParamEncryptionHttpSessionListener(GeoServerSecurityManager manager) {
        this.manager=manager;
    }
    
    /**
     * Create key if {@link GeoServerSecurityManager#isEncryptingUrlParams()}
     * is true
     * 
     */
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        if (manager.getSecurityConfig().isEncryptingUrlParams())
            addEncrypterToSession(manager, se.getSession());
    }

    /**
     * Scramble key if it exists
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // scramble password
        char[] key = (char[])
                se.getSession().getAttribute(SESSION_PARAM_KEYNAME);        
        if (key!=null) 
            manager.getRandomPassworddProvider().getRandomPassword(key);
        
        se.getSession().removeAttribute(SESSION_PARAM_KEYNAME);
        se.getSession().removeAttribute(SESSION_PARAM_NAME);
    }
    
}
