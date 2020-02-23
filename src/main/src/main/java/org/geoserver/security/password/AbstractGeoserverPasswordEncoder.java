/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.password;

import java.io.IOException;
import java.security.Security;
import java.util.logging.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geotools.util.logging.Logging;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Abstract base implementation, delegating the encoding to third party encoders implementing {@link
 * PasswordEncoder}
 *
 * @author christian
 */
public abstract class AbstractGeoserverPasswordEncoder implements GeoServerPasswordEncoder {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected volatile PasswordEncoder stringEncoder;
    protected volatile CharArrayPasswordEncoder charEncoder;

    protected String name;

    private boolean availableWithoutStrongCryptogaphy;
    private boolean reversible = true;
    private String prefix;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String getName() {
        return name;
    }

    public void setBeanName(String beanName) {
        this.name = beanName;
    }

    /** Does nothing, subclases may override. */
    public void initialize(GeoServerSecurityManager securityManager) throws IOException {}

    /** Does nothing, subclases may override. */
    public void initializeFor(GeoServerUserGroupService service) throws IOException {}

    public AbstractGeoserverPasswordEncoder() {
        setAvailableWithoutStrongCryptogaphy(true);
    }

    protected PasswordEncoder getStringEncoder() {
        if (stringEncoder == null) {
            synchronized (this) {
                if (stringEncoder == null) {
                    stringEncoder = createStringEncoder();
                }
            }
        }
        return stringEncoder;
    }

    /** Creates the encoder instance used when source is a string. */
    protected abstract PasswordEncoder createStringEncoder();

    protected CharArrayPasswordEncoder getCharEncoder() {
        if (charEncoder == null) {
            synchronized (this) {
                if (charEncoder == null) {
                    charEncoder = createCharEncoder();
                }
            }
        }
        return charEncoder;
    }

    /** Creates the encoder instance used when source is a char array. */
    protected abstract CharArrayPasswordEncoder createCharEncoder();

    /** @return the concrete {@link PasswordEncoder} object */
    protected final PasswordEncoder getActualEncoder() {
        return null;
    }

    @Override
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        return doEncodePassword(getStringEncoder().encode(rawPass));
    }

    @Override
    public String encodePassword(char[] rawPass, Object salt) throws DataAccessException {
        return doEncodePassword(getCharEncoder().encodePassword(rawPass, salt));
    }

    String doEncodePassword(String encPass) {
        if (encPass == null) {
            return encPass;
        }

        StringBuffer buff = initPasswordBuffer();
        buff.append(encPass);
        return buff.toString();
    }

    StringBuffer initPasswordBuffer() {
        StringBuffer buff = new StringBuffer();
        if (getPrefix() != null) {
            buff.append(getPrefix()).append(GeoServerPasswordEncoder.PREFIX_DELIMTER);
        }
        return buff;
    }

    @Override
    public boolean isPasswordValid(String encPass, String rawPass, Object salt)
            throws DataAccessException {
        if (encPass == null) return false;
        return getStringEncoder().matches(rawPass, stripPrefix(encPass));
    }

    @Override
    public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
        if (encPass == null) return false;
        return getCharEncoder().isPasswordValid(stripPrefix(encPass), rawPass, salt);
    }

    String stripPrefix(String encPass) {
        return getPrefix() != null ? removePrefix(encPass) : encPass;
    }

    protected String removePrefix(String encPass) {
        return encPass.replaceFirst(getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER, "");
    }

    @Override
    public abstract PasswordEncodingType getEncodingType();

    /** @return true if this encoder has encoded encPass */
    public boolean isResponsibleForEncoding(String encPass) {
        if (encPass == null) return false;
        return encPass.startsWith(getPrefix() + GeoServerPasswordEncoder.PREFIX_DELIMTER);
    }

    public String decode(String encPass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("decoding passwords not supported");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return this.isPasswordValid(encodedPassword, rawPassword.toString(), null);
    }

    @Override
    public char[] decodeToCharArray(String encPass) throws UnsupportedOperationException {
        return encPass.toCharArray();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isAvailableWithoutStrongCryptogaphy() {
        return availableWithoutStrongCryptogaphy;
    }

    public void setAvailableWithoutStrongCryptogaphy(boolean availableWithoutStrongCryptogaphy) {
        this.availableWithoutStrongCryptogaphy = availableWithoutStrongCryptogaphy;
    }

    public boolean isReversible() {
        return reversible;
    }

    public void setReversible(boolean reversible) {
        this.reversible = reversible;
    }

    /** Interface for password encoding when source password is specified as char array. */
    protected interface CharArrayPasswordEncoder {

        String encodePassword(char[] rawPass, Object salt);

        boolean isPasswordValid(String encPass, char[] rawPass, Object salt);
    }
}
