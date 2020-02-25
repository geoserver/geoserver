/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Arrays;
import org.geoserver.security.password.RandomPasswordProvider;
import org.geotools.data.Query;
import org.opengis.filter.Filter;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Common security utility methods.
 *
 * @author mcr
 */
public class SecurityUtils {

    /**
     * Spring Secruity 3.x drops the common base security exception class SpringSecurityException,
     * now the test is based on the package name
     *
     * @param t the exception to check
     * @return true if the exception is caused by Spring Security
     */
    public static boolean isSecurityException(Throwable t) {
        return t != null
                && t.getClass().getPackage().getName().startsWith("org.springframework.security");
    }

    /**
     * Converts a char array to a byte array.
     *
     * <p>This method is unsafe since the charset is not specified, one of {@link #toBytes(char[],
     * String)} or {@link #toBytes(char[], Charset)} should be used instead. When not specified
     * {@link Charset#defaultCharset()} is used.
     */
    public static byte[] toBytes(char[] ch) {
        return toBytes(ch, Charset.defaultCharset());
    }

    /** Converts a char array to a byte array. */
    public static byte[] toBytes(char[] ch, String charset) {
        return toBytes(ch, Charset.forName(charset));
    }

    /** Converts a char array to a byte array. */
    public static byte[] toBytes(char[] ch, Charset charset) {
        ByteBuffer buff = charset.encode(CharBuffer.wrap(ch));
        byte[] tmp = new byte[buff.limit()];
        buff.get(tmp);
        return tmp;
    }

    /**
     * Converts byte array to char array.
     *
     * <p>This method is unsafe since the charset is not specified, one of {@link #toChars(byte[],
     * String)} or {@link #toChars(byte[], Charset)} should be used instead. When not specified
     * {@link Charset#defaultCharset()} is used.
     */
    public static char[] toChars(byte[] b) {
        return toChars(b, Charset.defaultCharset());
    }

    /** Converts byte array to char array. */
    public static char[] toChars(byte[] b, String charset) {
        return toChars(b, Charset.forName(charset));
    }

    /** Converts byte array to char array. */
    public static char[] toChars(byte[] b, Charset charset) {
        CharBuffer buff = charset.decode(ByteBuffer.wrap(b));
        char[] tmp = new char[buff.limit()];
        buff.get(tmp);
        return tmp;
    }

    /** Trims null characters off the end of the specified character array. */
    //    public static char[] trimNullChars(char[] ch) {
    //        int i = ch.length-1;
    //        while(i > -1 && ch[i] == 0) {
    //            i--;
    //        }
    //        return i < ch.length-1 ? Arrays.copyOf(ch, i+1) : ch;
    //    }

    /**
     * Scrambles a char array overwriting all characters with random characters, used for scrambling
     * plain text passwords after usage to avoid keeping them around in memory.
     */
    public static void scramble(char[] ch) {
        if (ch == null) return;
        RandomPasswordProvider rpp = new RandomPasswordProvider();
        rpp.getRandomPassword(ch);
    }

    /**
     * Scrambles a byte array overwriting all characters with random characters, used for scrambling
     * plain text passwords after usage to avoid keeping them around in memory.
     */
    public static void scramble(byte[] ch) {
        if (ch == null) return;
        RandomPasswordProvider rpp = new RandomPasswordProvider();
        rpp.getRandomPassword(ch);
    }
    /** Builds the write query based on the access limits class */
    public static Query getWriteQuery(WrapperPolicy policy) {
        if (policy.getAccessLevel() != AccessLevel.READ_WRITE) {
            return new Query(null, Filter.EXCLUDE);
        } else if (policy.getLimits() == null) {
            return Query.ALL;
        } else if (policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();
            return val.getWriteQuery();
        } else {
            throw new IllegalArgumentException(
                    "SecureFeatureStore has been fed "
                            + "with unexpected AccessLimits class "
                            + policy.getLimits().getClass());
        }
    }

    /** Creates the inverse permutation array. */
    public static int[] createInverse(int[] perm) {
        int[] inverse = new int[perm.length];
        for (int i = 0; i < perm.length; i++) {
            inverse[perm[i]] = i;
        }
        return inverse;
    }

    /**
     * Applies a permutation.
     *
     * @param base source array
     * @param times number of repetitions
     * @param perm the permutation
     */
    public static char[] permute(char[] base, int times, int[] perm) {

        char[][] working = new char[2][base.length];

        System.arraycopy(base, 0, working[0], 0, base.length);
        for (int j = 0; j < times; j++) {
            int source = j % 2;
            int target = (j + 1) % 2;
            for (int i = 0; i < working[source].length; i++)
                working[target][perm[i]] = working[source][i];
        }
        char[] result = working[1].clone();
        Arrays.fill(working[0], '0');
        Arrays.fill(working[1], '0');
        return result;
    }

    /**
     * Extracts the username from auth principal or returns null. A static method that simply checks
     * for concrete principal class, casts to it and invokes the correct method to extract the
     * username.
     *
     * @param principal auth principal
     */
    public static String getUsername(Object principal) {
        String username = null;
        if (principal != null) {
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof Principal) {
                username = ((Principal) principal).getName();
            } else {
                username = principal.toString();
            }
        }

        return username;
    }
}
