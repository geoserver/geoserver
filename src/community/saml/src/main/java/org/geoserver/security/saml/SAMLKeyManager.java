/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import java.security.cert.X509Certificate;
import java.util.Set;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.springframework.security.saml.key.EmptyKeyManager;
import org.springframework.security.saml.key.KeyManager;

public class SAMLKeyManager implements KeyManager {

    private KeyManager delegate = new EmptyKeyManager();

    public KeyManager getDelegate() {
        return delegate;
    }

    public void setDelegate(KeyManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Iterable<Credential> resolve(CriteriaSet criteria) throws SecurityException {
        return this.delegate.resolve(criteria);
    }

    @Override
    public Credential resolveSingle(CriteriaSet criteria) throws SecurityException {
        return this.delegate.resolveSingle(criteria);
    }

    @Override
    public Credential getCredential(String keyName) {
        return this.delegate.getCredential(keyName);
    }

    @Override
    public Credential getDefaultCredential() {
        return this.delegate.getDefaultCredential();
    }

    @Override
    public String getDefaultCredentialName() {
        return this.delegate.getDefaultCredentialName();
    }

    @Override
    public Set<String> getAvailableCredentials() {
        return this.delegate.getAvailableCredentials();
    }

    @Override
    public X509Certificate getCertificate(String alias) {
        return this.delegate.getCertificate(alias);
    }
}
