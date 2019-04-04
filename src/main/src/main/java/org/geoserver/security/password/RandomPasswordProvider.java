/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.security.SecureRandom;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Class for generating random passwords using {@link SecureRandom}.
 *
 * <p>The password alphabet is {@link #PRINTABLE_ALPHABET}. Since the alphabet is not really big,
 * the length of the password is important.
 *
 * @author christian
 */
public class RandomPasswordProvider {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** alphabet */
    public static final char[] PRINTABLE_ALPHABET = {
        '!', '\"', '#', '$', '%', '&', '\'', '(',
        ')', '*', '+', ',', '-', '.', '/', '0',
        '1', '2', '3', '4', '5', '6', '7', '8',
        '9', ':', ';', '<', '?', '@', 'A', 'B',
        'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
        'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        '[', '\\', ']', '^', '_', '`', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
        's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '{', '|', '}', '~',
    };

    /**
     * The default password length assures a key strength of 2 ^ 261 {@link #PRINTABLE_ALPHABET} has
     * 92 characters ln (92 ^ 40 ) / ln (2) = 260.942478242
     */
    public static int DefaultPasswordLength = 40;

    /**
     * Creates a random password of the specified length, if length <=0, return <code>null</code>
     */
    public char[] getRandomPassword(int length) {
        if (length <= 0) return null;
        char[] buff = new char[length];
        getRandomPassword(buff);
        return buff;
    }

    public char[] getRandomPasswordWithDefaultLength() {
        char[] buff = new char[DefaultPasswordLength];
        getRandomPassword(buff);
        return buff;
    }

    /** Creates a random password filling the specified character array. */
    public void getRandomPassword(char[] buff) {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < buff.length; i++) {
            int index = random.nextInt(Integer.MAX_VALUE) % PRINTABLE_ALPHABET.length;
            if (index < 0) index += PRINTABLE_ALPHABET.length;
            buff[i] = PRINTABLE_ALPHABET[index];
        }
    }
    /** Creates a random password filling the specified byte array. */
    public void getRandomPassword(byte[] buff) {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < buff.length; i++) {
            int index = random.nextInt(Integer.MAX_VALUE) % PRINTABLE_ALPHABET.length;
            if (index < 0) index += PRINTABLE_ALPHABET.length;
            buff[i] = (byte) PRINTABLE_ALPHABET[index];
        }
    }
}
