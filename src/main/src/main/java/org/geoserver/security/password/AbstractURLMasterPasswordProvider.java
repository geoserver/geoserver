/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.toBytes;
import static org.geoserver.security.SecurityUtils.toChars;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.URLs;

/**
 * Reads and writes the master password at a url. Subclasses pick the encryption, see {@link #encrypt} and
 * {@link #decrypt}: a FIPS deployment needs different algorithms, but the url handling stays the same.
 */
public abstract class AbstractURLMasterPasswordProvider extends MasterPasswordProvider {

    /** base encryption key */
    static final char[] BASE = {
        'U', 'n', '6', 'd', 'I', 'l', 'X', 'T', 'Q', 'c', 'L', ')', '$', '#', 'q', 'J', 'U',
        'l', 'X', 'Q', 'U', '!', 'n', 'n', 'p', '%', 'U', 'r', '5', 'U', 'u', '3', '5', 'H',
        '`', 'x', 'P', 'F', 'r', 'X'
    };

    /** permutation indices, this permutation has a cycle of 169 --> more than 168 iterations have no effect */
    static final int[] PERM = {
        32, 19, 30, 11, 34, 26, 3, 21, 9, 37, 38, 13, 23, 2, 18, 4, 20, 1, 29, 17, 0, 31, 14, 36, 12, 24, 15, 35, 16,
        39, 25, 5, 10, 8, 7, 6, 33, 27, 28, 22
    };

    protected URLMasterPasswordProviderConfig config;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        this.config = (URLMasterPasswordProviderConfig) config;
    }

    @Override
    protected char[] doGetMasterPassword() throws Exception {
        try {
            try (InputStream in = input(config.getURL(), getConfigDir())) {
                return toChars(decode(IOUtils.toByteArray(in)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doSetMasterPassword(char[] passwd) throws Exception {
        try (OutputStream out = output(config.getURL(), getConfigDir())) {
            out.write(encode(passwd));
        }
    }

    Resource getConfigDir() throws IOException {
        return getSecurityManager().masterPasswordProvider().get(getName());
    }

    /** Handles the plain text case so implementations only deal with the protected form. */
    byte[] encode(char[] passwd) {
        if (!config.isEncrypting()) {
            return toBytes(passwd);
        }
        return encrypt(passwd);
    }

    byte[] decode(byte[] passwd) {
        if (!config.isEncrypting()) {
            return passwd;
        }
        return decrypt(passwd);
    }

    /** Protects the password for storage; called only when the configuration asks for encryption. */
    protected abstract byte[] encrypt(char[] passwd);

    /** Reverses {@link #encrypt(char[])}. */
    protected abstract byte[] decrypt(byte[] passwd);

    /** Obfuscation key the stored form is protected with, the same for every installation. */
    protected char[] key() {
        // generate the key
        return SecurityUtils.permute(BASE, 32, PERM);
    }

    static OutputStream output(URL url, Resource configDir) throws IOException {
        // check for file url
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            File f = URLs.urlToFile(url);
            if (!f.isAbsolute()) {
                // make relative to config dir
                return configDir.get(f.getPath()).out();
            } else {
                return new FileOutputStream(f);
            }
        } else {
            URLConnection cx = url.openConnection();
            cx.setDoOutput(true);
            return cx.getOutputStream();
        }
    }

    static InputStream input(URL url, Resource configDir) throws IOException {
        // check for a file url
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            File f = URLs.urlToFile(url);
            // check if the file is relative
            if (!f.isAbsolute()) {
                // make it relative to the config directory for this password provider
                Resource res = configDir.get(f.getPath());
                if (res.getType() != Type.RESOURCE) { // file must already exist.
                    throw new FileNotFoundException();
                }
                return res.in();
            } else {
                return new FileInputStream(f);
            }
        } else {
            return url.openStream();
        }
    }
}
