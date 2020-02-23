/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.io.IOException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General Geoserver password encoding interface
 *
 * @author christian
 */
public interface GeoServerPasswordEncoder extends PasswordEncoder, BeanNameAware {

    public static final String PREFIX_DELIMTER = ":";

    /** Initialize this encoder. */
    void initialize(GeoServerSecurityManager securityManager) throws IOException;

    /** Initialize this encoder for a {@link GeoServerUserGroupService} object. */
    void initializeFor(GeoServerUserGroupService service) throws IOException;

    /** @return the {@link PasswordEncodingType} */
    PasswordEncodingType getEncodingType();

    /** The name of the password encoder. */
    String getName();

    /** @return true if this encoder has encoded encPass */
    boolean isResponsibleForEncoding(String encPass);

    /**
     * Decodes an encoded password. Only supported for {@link PasswordEncodingType#ENCRYPT} and
     * {@link PasswordEncodingType#PLAIN} encoders, ie those that return <code>true</code> from
     * {@link #isReversible()}.
     *
     * @param encPass The encoded password.
     */
    String decode(String encPass) throws UnsupportedOperationException;

    /**
     * Decodes an encoded password to a char array.
     *
     * @see #decode(String)
     */
    char[] decodeToCharArray(String encPass) throws UnsupportedOperationException;

    /**
     * Encodes a raw password from a char array.
     *
     * @see #encodePassword(char[], Object)
     */
    String encodePassword(char[] password, Object salt);

    /**
     * Encodes a raw password from a String.
     *
     * @see #encodePassword(String, Object)
     */
    String encodePassword(String password, Object salt);

    /**
     * Validates a specified "raw" password (as char array) against an encoded password.
     *
     * @see {@link #isPasswordValid(String, char[], Object)}
     */
    boolean isPasswordValid(String encPass, char[] rawPass, Object salt);

    /**
     * Validates a specified "raw" password (as char array) against an encoded password.
     *
     * @see {@link #isPasswordValid(String, String, Object)}
     */
    boolean isPasswordValid(String encPass, String rawPass, Object salt);

    /**
     * @return a prefix which is stored with the password. This prefix must be unique within all
     *     {@link GeoServerPasswordEncoder} implementations.
     *     <p>Reserved:
     *     <p>plain digest1 crypt1
     *     <p>A plain text password is stored as
     *     <p>plain:password
     */
    String getPrefix();

    /**
     * Is this encoder available without installing the unrestricted policy files of the java
     * cryptographic extension
     */
    boolean isAvailableWithoutStrongCryptogaphy();

    /**
     * Flag indicating if the encoder can decode an encrypted password back into its original plain
     * text form.
     */
    boolean isReversible();
}
