/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Password Encoder for encrypting url params
 *
 * @author christian
 */
package org.geoserver.web;

import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.crypt.AbstractCrypt;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.ICryptFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * Encryptor factory for apache wicket
 *
 * @author christian
 */
public class GeoserverWicketEncrypterFactory implements ICryptFactory {

    static ICryptFactory Factory;
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    static final String ICRYPT_ATTR_NAME = "__ICRYPT";

    ICrypt NoCrypt =
            new ICrypt() {

                @Override
                public String decryptUrlSafe(String text) {
                    return text;
                }

                @Override
                public String encryptUrlSafe(String plainText) {
                    return plainText;
                }

                @Override
                public void setKey(String key) {}
            };

    class CryptImpl extends AbstractCrypt {
        protected StandardPBEByteEncryptor enc;

        CryptImpl(StandardPBEByteEncryptor enc) {
            this.enc = enc;
        }

        @Override
        protected byte[] crypt(byte[] input, int mode) throws GeneralSecurityException {
            if (mode == Cipher.ENCRYPT_MODE) {
                return enc.encrypt(input);
            } else {
                return enc.decrypt(input);
            }
        }
    };

    /**
     * Look up in the Spring Context for an implementation of {@link ICryptFactory} if nothing found
     * use this default.
     */
    public static ICryptFactory get() {
        if (Factory != null) return Factory;
        Factory = GeoServerExtensions.bean(ICryptFactory.class);
        if (Factory == null) Factory = new GeoserverWicketEncrypterFactory();
        return Factory;
    }

    protected GeoserverWicketEncrypterFactory() {}

    @Override
    public ICrypt newCrypt() {
        RequestCycle cycle = RequestCycle.get();
        ServletWebRequest req = (ServletWebRequest) cycle.getRequest();
        HttpSession s = (HttpSession) req.getContainerRequest().getSession(false);
        if (s != null) {
            return getEncrypterFromSession(s);
        } else {
            LOGGER.warning("No session availabe to get url parameter encrypter");
            return NoCrypt;
        }
    }

    protected ICrypt getEncrypterFromSession(HttpSession s) {
        ICrypt result = (ICrypt) s.getAttribute(ICRYPT_ATTR_NAME);
        if (result != null) return result;

        GeoServerSecurityManager manager = GeoServerApplication.get().getSecurityManager();
        char[] key = manager.getRandomPassworddProvider().getRandomPasswordWithDefaultLength();

        StandardPBEByteEncryptor enc = new StandardPBEByteEncryptor();
        enc.setPasswordCharArray(key);
        // since the password is copied, we can scramble it
        manager.disposePassword(key);

        if (manager.isStrongEncryptionAvailable()) {
            enc.setProvider(new BouncyCastleProvider());
            enc.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
        } else // US export restrictions
        enc.setAlgorithm("PBEWITHMD5ANDDES");

        result = new CryptImpl(enc);
        s.setAttribute(ICRYPT_ATTR_NAME, result);
        return result;
    }
}
