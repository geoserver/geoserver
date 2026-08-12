/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsStatus;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.geoserver.platform.security.SecurityDefaults;
import org.geotools.util.SuppressFBWarnings;

/**
 * Reports how cryptography is set up in this JVM, for the module status and the status page.
 *
 * <p>Nothing is cached. The provider list and the approved-only mode can be changed by any other code running in the
 * same JVM, so every call reads the current state
 */
public final class FipsSetup {

    /** System property that puts BouncyCastle in approved-only mode for every thread that starts after it is set. */
    static final String APPROVED_ONLY_PROPERTY = "org.bouncycastle.fips.approved_only";

    /** Set to 1 by a Linux kernel booted with fips=1, absent on a system that is not in FIPS mode. */
    private static final Path KERNEL_FIPS_FLAG = Path.of("/proc/sys/crypto/fips_enabled");

    /** Regular BouncyCastle provider, the one class the FIPS-validated distribution lacks. */
    private static final String REGULAR_BC_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    /** Short names for the bean and class names the security defaults answer with. */
    private static final Map<String, String> SHORT_NAMES = Map.of(
            FipsSecurityDefaults.AES_GCM_ENCODER, "AES-GCM",
            FipsSecurityDefaults.AES_GCM_MASTER_PASSWORD_PROVIDER, "AES-GCM file");

    private FipsSetup() {}

    /**
     * Whether the FIPS jars replaced the regular BouncyCastle ones. That is the only classpath this module works on.
     * Both sets of jars use the same {@code org.bouncycastle} package names, so with both present no class loads.
     */
    public static boolean isFipsClasspath() {
        try {
            // loaded without initializing it: initializing is what fails with the signer mismatch
            Class.forName(REGULAR_BC_PROVIDER_CLASS, false, FipsSetup.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    /** The BouncyCastle self tests passed and the module is usable. */
    public static boolean isFipsModuleReady() {
        return FipsStatus.isReady();
    }

    /** {@link FipsStatus#READY}, or why the module refused to start. */
    public static String getFipsModuleStatus() {
        return FipsStatus.getStatusMessage();
    }

    /**
     * Approved-only mode as the deployment asked for it. Reads the system property, not
     * {@link CryptoServicesRegistrar#isInApprovedOnlyMode()}, which answers for the calling thread only.
     */
    public static boolean isApprovedOnlyRequested() {
        return Boolean.parseBoolean(System.getProperty(APPROVED_ONLY_PROPERTY));
    }

    /** Approved-only mode of the calling thread, which is what its crypto calls are held to. */
    public static boolean isApprovedOnlyForThisThread() {
        return CryptoServicesRegistrar.isInApprovedOnlyMode();
    }

    /**
     * Whether the operating system is in FIPS mode, empty when it does not say. A JVM can run the validated module on a
     * system that is not in FIPS mode. That is fine for testing, but it is not a compliant deployment.
     */
    public static Optional<Boolean> isOperatingSystemInFipsMode() {
        try {
            return Optional.of("1".equals(Files.readString(KERNEL_FIPS_FLAG).trim()));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** One algorithm GeoServer needs, and whether this JVM offers it. Serializable so a Wicket page can hold it. */
    public record AlgorithmStatus(String use, String type, String algorithm, boolean available)
            implements Serializable {}

    /**
     * The algorithms a FIPS deployment cannot work without, in the order they are needed. When one of these is missing
     * GeoServer stops working, while a gap elsewhere in the provider may never be noticed.
     */
    public static List<AlgorithmStatus> getRequiredAlgorithms() {
        String keyStoreType = SecurityDefaults.get(SecurityDefaults.Setting.KEYSTORE_TYPE, "JCEKS");
        return List.of(
                algorithm("Keystore holding the configuration keys", "KeyStore", keyStoreType),
                algorithm("Password encryption and URL parameter encryption", "Cipher", "AES/GCM/NoPadding"),
                algorithm("Key derivation for password encryption", "SecretKeyFactory", "PBKDF2WithHmacSHA256"),
                algorithm("Hashing of user account passwords", "MessageDigest", "SHA-256"));
    }

    /** One line of the report: a short label, and the value this JVM answers for it. */
    public record Fact(String label, String value) implements Serializable {}

    /** The whole setup, as short values a reader can compare with the documentation. */
    public static List<Fact> getFacts() {
        Provider provider = Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME);
        return List.of(
                new Fact("Crypto module", getFipsModuleStatus()),
                new Fact("Approved-only mode", isApprovedOnlyRequested() ? "on" : "off"),
                new Fact(
                        "Operating system FIPS mode",
                        isOperatingSystemInFipsMode()
                                .map(on -> on ? "yes" : "no")
                                .orElse("unknown")),
                new Fact(
                        "Crypto provider",
                        provider == null ? "not installed" : provider.getName() + " " + provider.getVersionStr()),
                new Fact("Provider position", providerPosition(provider)),
                new Fact("Keystore format", shortName(SecurityDefaults.Setting.KEYSTORE_TYPE)),
                new Fact("Config password encoder", shortName(SecurityDefaults.Setting.CONFIG_PASSWORD_ENCODER)),
                new Fact("User password encoder", shortName(SecurityDefaults.Setting.USER_GROUP_PASSWORD_ENCODER)),
                new Fact("Master password storage", shortName(SecurityDefaults.Setting.MASTER_PASSWORD_PROVIDER)),
                new Fact("Random source", getRandomSource()));
    }

    /**
     * Where the validated provider sits in the provider list. Java uses the first provider that offers an algorithm, so
     * anything ahead of it does the encryption instead.
     */
    private static String providerPosition(Provider provider) {
        if (provider == null) {
            return "not installed";
        }
        Provider first = Security.getProviders()[0];
        return first == provider ? "first" : "behind " + first.getName();
    }

    /** The value in force for the setting, shortened when there is a name a reader knows it by. */
    private static String shortName(SecurityDefaults.Setting setting) {
        String value = SecurityDefaults.get(setting, "GeoServer default");
        return SHORT_NAMES.getOrDefault(value, value);
    }

    /**
     * The random source GeoServer gets when it asks for one, as algorithm and provider. A missing algorithm name is not
     * a fault. Java then uses the platform default, and what counts is which provider makes the bytes.
     */
    @SuppressFBWarnings(
            value = "DMI_RANDOM_USED_ONLY_ONCE",
            justification = "the instance is built to be described, not to produce bytes")
    private static String getRandomSource() {
        SecureRandom random = new SecureRandom();
        return random.getAlgorithm() + " (" + random.getProvider().getName() + ")";
    }

    private static AlgorithmStatus algorithm(String use, String type, String algorithm) {
        return new AlgorithmStatus(use, type, algorithm, isAvailable(type, algorithm));
    }

    private static boolean isAvailable(String type, String algorithm) {
        try {
            switch (type) {
                case "KeyStore" -> KeyStore.getInstance(algorithm);
                case "Cipher" -> Cipher.getInstance(algorithm);
                case "SecretKeyFactory" -> SecretKeyFactory.getInstance(algorithm);
                case "MessageDigest" -> MessageDigest.getInstance(algorithm);
                case "SecureRandom" -> SecureRandom.getInstance(algorithm);
                default -> throw new IllegalArgumentException("Unknown service type " + type);
            }
            return true;
        } catch (NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException e) {
            return false;
        }
    }
}
