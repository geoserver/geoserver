/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

/**
 * Password Encoder for encrypting url params
 * 
 * @author christian
 *
 */

package org.geoserver.web;

import java.security.GeneralSecurityException;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.util.crypt.AbstractCrypt;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.ICryptFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.password.URLParamEncryptionHttpSessionListener;
import org.geotools.util.logging.Logging;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * Encryptor factory for apache wicket
 * 
 * @author christian
 *
 */
public class GeoserverWicketEncrypterFactory implements ICryptFactory {
    
    static ICryptFactory Factory;
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");    
    
    ICrypt NoCrypt = new ICrypt() {

        @Override
        public String decryptUrlSafe(String text) {
            return text;
        }

        @Override
        public String encryptUrlSafe(String plainText) {
            return plainText;
        }

        @Override
        public void setKey(String key) {
        }
        
    };
    
    
    
    class CryptImpl extends AbstractCrypt {
        protected StandardPBEByteEncryptor enc;
        
        CryptImpl (StandardPBEByteEncryptor enc) {            
            this.enc = enc;
        }
        @Override
        protected byte[] crypt(byte[] input, int mode) throws GeneralSecurityException {
            if (mode==Cipher.ENCRYPT_MODE) {
                return enc.encrypt(input);
            } else {
                return enc.decrypt(input);
            }
        }
    };
    
    /**
     * Look up in the Spring Context for an implementation
     * of {@link ICryptFactory}
     * if nothing found use this default.
     * @return
     */
    public static ICryptFactory get() {
        if (Factory!=null) return Factory;
        Factory = GeoServerExtensions.bean(ICryptFactory.class);
        if (Factory==null)
            Factory=new GeoserverWicketEncrypterFactory();
        return Factory;
    }
    
    protected GeoserverWicketEncrypterFactory()  {        
   }

    @Override
    public ICrypt newCrypt() {
        RequestCycle cycle = RequestCycle.get();
        ServletWebRequest req = (ServletWebRequest)cycle.getRequest();
        HttpSession s = (HttpSession) req.getHttpServletRequest().getSession(false);
        StandardPBEByteEncryptor enc = null;
        if (s!=null) {
            enc = (StandardPBEByteEncryptor) 
                    s.getAttribute(URLParamEncryptionHttpSessionListener.SESSION_PARAM_NAME);
            if (enc==null) {
                URLParamEncryptionHttpSessionListener.addEncrypterToSession(
                        GeoServerApplication.get().getSecurityManager(), s);
                LOGGER.warning("Url parameter encrypter set on the fly");
            }
        }
        
        if (enc==null) {
            LOGGER.warning("No Url parameter encrypter available");
            return NoCrypt;
        } else {        
            return new CryptImpl(enc);
        }        
    }
  
 }
