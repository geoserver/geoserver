package org.geoserver.security;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Enumeration;
import javax.crypto.SecretKey;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

public class MigrateKeystore {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println(
                    "Usage: MigrateKeystore <old_keystore> <old_password> <new_keystore> <new_password> [keystore_type] [provider]");
            System.out.println("  keystore_type: BCFKS (default) or PKCS11");
            System.out.println("  provider: BCFIPS (default) or SunPKCS11");
            System.exit(1);
        }

        String oldKeystorePath = args[0];
        String oldPassword = args[1];
        String newKeystorePath = args[2];
        String newPassword = args[3];
        String keystoreType = args.length > 4 ? args[4] : "BCFKS";
        String provider = args.length > 5 ? args[5] : "BCFIPS";

        // Initialize Bouncy Castle FIPS provider
        if (Security.getProvider("BCFIPS") == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }

        // Load old keystore (JKS/JCEKS)
        KeyStore oldKeystore = KeyStore.getInstance("JCEKS");
        try (FileInputStream fis = new FileInputStream(oldKeystorePath)) {
            oldKeystore.load(fis, oldPassword.toCharArray());
        }

        // Create new keystore (BCFKS or PKCS#11)
        KeyStore newKeystore = KeyStore.getInstance(keystoreType, provider);
        newKeystore.load(null, newPassword.toCharArray());

        // Migrate entries
        Enumeration<String> aliases = oldKeystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (oldKeystore.isKeyEntry(alias)) {
                Key key = oldKeystore.getKey(alias, oldPassword.toCharArray());
                Certificate[] certChain = oldKeystore.getCertificateChain(alias);
                KeyStore.Entry entry;
                if (key instanceof SecretKey) {
                    entry = new KeyStore.SecretKeyEntry((SecretKey) key);
                } else if (key instanceof PrivateKey && certChain != null) {
                    entry = new KeyStore.PrivateKeyEntry((PrivateKey) key, certChain);
                } else {
                    System.out.println("Skipping unknown key type for alias: " + alias);
                    continue;
                }
                newKeystore.setEntry(alias, entry, new KeyStore.PasswordProtection(newPassword.toCharArray()));
            } else if (oldKeystore.isCertificateEntry(alias)) {
                Certificate cert = oldKeystore.getCertificate(alias);
                newKeystore.setCertificateEntry(alias, cert);
            }
        }

        // Save new keystore
        try (FileOutputStream fos = new FileOutputStream(newKeystorePath)) {
            newKeystore.store(fos, newPassword.toCharArray());
        }

        System.out.println("Migration completed successfully.");
        System.out.println("Old keystore: " + oldKeystorePath + " (JCEKS)");
        System.out.println("New keystore: " + newKeystorePath + " (" + keystoreType + "/" + provider + ")");
    }
}
