/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import java.security.SecureRandom;
import org.jasypt.salt.SaltGenerator;

/**
 * Salt generator backed by the JVM default {@link java.security.SecureRandom}, usable with any registered crypto
 * provider.
 *
 * <p>Jasypt's {@code RandomSaltGenerator} asks for {@code SHA1PRNG} by name, and no FIPS-validated provider has it.
 * There is no algorithm name that both the normal JDK providers and the FIPS ones offer, so this asks for no name at
 * all and takes what the JVM gives.
 *
 * <p>The salt is still stored inside the encryption result, as it was with the generator this replaces, so values
 * encrypted earlier can still be decrypted. The initialization vector is left alone on purpose: jasypt uses
 * {@code NoIvGenerator}, and setting one would add an IV to the output and change the stored format.
 */
class SecureRandomGenerator implements SaltGenerator {

    /** Shared on purpose: {@link SecureRandom} is thread safe, and the generator keeps nothing else. */
    static final SecureRandomGenerator INSTANCE = new SecureRandomGenerator();

    private final SecureRandom random = new SecureRandom();

    private SecureRandomGenerator() {}

    @Override
    public byte[] generateSalt(int lengthBytes) {
        byte[] salt = new byte[lengthBytes];
        random.nextBytes(salt);
        return salt;
    }

    @Override
    public boolean includePlainSaltInEncryptionResults() {
        return true;
    }
}
