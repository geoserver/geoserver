/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.util.logging.Logging;

/**
 * Hides the algorithms that a FIPS operating system does not have, so a build on an ordinary machine fails where a FIPS
 * installation fails.
 *
 * <p>The JVM gets its algorithms from a list of providers. Each provider offers a set of services, one per algorithm. A
 * FIPS operating system ships a JDK whose providers are missing some of those services. This class takes the same ones
 * away here. It does nothing else: it does not check any code, it only makes those algorithms unavailable.
 *
 * <p>It is test code by design, called from {@link RestrictedJdkProvidersListener} and from
 * {@link org.geoserver.test.GeoServerBaseTestSupport} when the fips-test build profile asks for it. Taking services
 * away from the JDK does not belong in a shipped artifact, however well guarded. Configuration cannot do it either. A
 * {@code java.security} file can reorder providers, but it cannot drop a single service from one.
 *
 * <p>What stays matters as much as what goes. MD5 and SHA-1 digests, DES, the JKS and PKCS12 keystores and the whole
 * TLS stack keep working on a FIPS system. Such a system blocks them where the security decision is made instead:
 * {@code jdk.certpath.disabledAlgorithms} and {@code jdk.tls.disabledAlgorithms} refuse them in certificates and in
 * TLS. Removing them here would report failures that no deployment ever sees.
 */
public final class RestrictedJdkProviders {

    static final Logger LOGGER = Logging.getLogger(RestrictedJdkProviders.class);

    /**
     * The services to hide, each written the way {@link Provider} names them, {@code type.algorithm}.
     *
     * <p>The list was measured, not guessed. It comes from printing the services of every provider on Rocky Linux 9 in
     * FIPS mode, with the Red Hat JDK and the FIPS BouncyCastle first. Measure it there again before changing it: a
     * guessed list once hid services that PKCS12 keystores need.
     */
    private static final List<String> ABSENT_UNDER_FIPS = List.of(
            // there the JDK providers give no random numbers at all, they all come from the FIPS ones
            "SecureRandom.SHA1PRNG",
            "SecureRandom.DRBG",
            "SecureRandom.NativePRNG",
            "SecureRandom.NativePRNGBlocking",
            "SecureRandom.NativePRNGNonBlocking",
            "KeyStore.JCEKS",
            // only the old password based ciphers go. The PBEWithHmac...AndAES ones stay, they are
            // still offered there, and PKCS12 keystores need them
            "Cipher.PBEWithMD5AndDES",
            "Cipher.PBEWithMD5AndTripleDES",
            "Cipher.PBEWithSHA1AndDESede",
            "Cipher.PBEWithSHA1AndRC2_40",
            "Cipher.PBEWithSHA1AndRC2_128",
            "Cipher.PBEWithSHA1AndRC4_40",
            "Cipher.PBEWithSHA1AndRC4_128");

    private RestrictedJdkProviders() {}

    /**
     * Hides the services listed above. Every provider keeps its place in the list, and a provider that offers none of
     * those services is left alone.
     *
     * <p>A provider that offers one is replaced by a wrapper answering for it, minus that service. The two simpler ways
     * do not work. {@link Provider#remove(Object)} reports success, but a provider keeps its services in two places and
     * that call clears only the one {@code getInstance} does not read. Building a new provider with just the services
     * to keep fails too: a provider accepts only services created for itself, and a service does not tell its
     * alternative names and settings, so an equal one cannot be built.
     *
     * <p>Never wrap a provider that offers none of them. {@code new SecureRandom()} asks for no algorithm, so it does
     * not go through the wrapper: the JVM asks each provider for its preferred generator, from a list only that
     * provider can fill. A wrapper hides that list, the JVM then finds no generator at all and falls back to its built
     * in one, which BouncyCastle refuses for every key.
     */
    public static void apply() {
        Provider[] providers = Security.getProviders();
        for (int position = 0; position < providers.length; position++) {
            Provider provider = providers[position];
            if (provider instanceof FilteredProvider || restrictedServiceCount(provider) == 0) continue;
            LOGGER.fine(() -> "Hiding " + restrictedServiceCount(provider) + " services a FIPS system does not offer "
                    + "from " + provider.getName());
            Security.removeProvider(provider.getName());
            Security.insertProviderAt(new FilteredProvider(provider), position + 1); // the list is one based
        }
    }

    private static long restrictedServiceCount(Provider provider) {
        return provider.getServices().stream()
                .filter(RestrictedJdkProviders::restricted)
                .count();
    }

    private static boolean restricted(Provider.Service service) {
        return ABSENT_UNDER_FIPS.contains(service.getType() + "." + service.getAlgorithm());
    }

    /** Answers for the provider it wraps, minus the services that a FIPS system does not have. */
    private static final class FilteredProvider extends Provider {

        private final transient Provider delegate;

        FilteredProvider(Provider delegate) {
            super(delegate.getName(), delegate.getVersionStr(), delegate.getInfo());
            this.delegate = delegate;
        }

        /**
         * Filters on the service that comes back, not on the name that was asked for. An algorithm can be asked for
         * under more than one name, and only the wrapped provider knows them all.
         */
        @Override
        public Service getService(String type, String algorithm) {
            Service service = delegate.getService(type, algorithm);
            if (service == null || !restricted(service)) return service;
            LOGGER.fine(() -> "Hidden " + service.getType() + "." + service.getAlgorithm() + " from " + getName()
                    + ", a FIPS system has none");
            return null;
        }

        @Override
        public Set<Service> getServices() {
            return delegate.getServices().stream().filter(s -> !restricted(s)).collect(Collectors.toSet());
        }
    }
}
