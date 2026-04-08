/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import com.hazelcast.spring.session.HazelcastIndexedSessionRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.springframework.context.ApplicationContext;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Delegates to a Spring Session {@link SessionRepositoryFilter} backed by Hazelcast when clustering and session-sharing
 * are enabled. The delegate is created lazily on the first request, reusing the
 * {@link com.hazelcast.core.HazelcastInstance} already managed by {@link HzCluster}.
 *
 * <p>The old {@code com.hazelcast.web.WebFilter} required Hazelcast to be fully started before the filter was
 * initialised and could not be swapped at runtime. Spring Session's {@link SessionRepositoryFilter} is a plain
 * {@link Filter} with no such constraint, making lazy and conditional initialisation straightforward.
 *
 * <p>The {@link HazelcastIndexedSessionRepository} is built programmatically inside {@link #createDelegate()} rather
 * than declared in {@code applicationContext.xml}. This keeps all session-sharing wiring self-contained in this class
 * and avoids the awkward {@code factory-method} XML gymnastics that would otherwise be needed to pass the
 * {@link com.hazelcast.core.HazelcastInstance} into a Spring bean before it is fully started.
 *
 * @author Kevin Smith, OpenGeo (original)
 */
public class HzSessionShareFilter implements Filter {

    /**
     * Name of the Hazelcast {@code IMap} used to store sessions. Kept consistent with the previous {@code map-name}
     * init-param so that rolling upgrades don't orphan existing sessions.
     */
    public static final String SESSION_MAP_NAME = "geoserver-sessions";

    /** Default session timeout — matches the servlet container default. */
    private static final Duration DEFAULT_MAX_INACTIVE = Duration.ofMinutes(30);

    // Volatile: cheap uncontended read on the hot doFilter path; the synchronized block
    // in createDelegate() guarantees safe publication on the write path.
    private volatile SessionRepositoryFilter<?> delegate;

    // Held so we can call destroy() on reset without leaking the MapListener
    // that HazelcastIndexedSessionRepository registers on the IMap.
    private HazelcastIndexedSessionRepository activeRepository;

    private ServletContext srvCtx;
    private HzCluster cluster;

    // -------------------------------------------------------------------------
    // Filter lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        srvCtx = filterConfig.getServletContext();
        // Eagerly stash the context reference; do NOT touch HzCluster here because
        // the Spring ApplicationContext may not be fully refreshed yet.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Double-checked locking: cheap volatile read on hot path.
        if (delegate == null) {
            createDelegate();
        }

        if (delegate != null) {
            delegate.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        tearDownDelegate();
    }

    // -------------------------------------------------------------------------
    // Lazy delegate initialisation
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link HazelcastIndexedSessionRepository} directly from the {@link com.hazelcast.core.HazelcastInstance}
     * owned by {@link HzCluster}, then wraps it in a {@link SessionRepositoryFilter}. The repository is initialised via
     * {@link HazelcastIndexedSessionRepository#afterPropertiesSet()} so its {@code IMap} listener is registered before
     * the filter starts serving requests.
     *
     * <p>Thread-safe via double-checked locking on the volatile {@link #delegate} field.
     */
    private void createDelegate() throws ServletException {
        if (!getCluster().isSessionSharing()) {
            return;
        }

        synchronized (this) {
            if (delegate != null) {
                return; // Another thread beat us here
            }

            HazelcastIndexedSessionRepository repository =
                    new HazelcastIndexedSessionRepository(getCluster().getHz());
            repository.setSessionMapName(SESSION_MAP_NAME);
            repository.setDefaultMaxInactiveInterval(DEFAULT_MAX_INACTIVE);
            repository.afterPropertiesSet(); // registers the IMap listener

            activeRepository = repository;
            delegate = new SessionRepositoryFilter<>(repository);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Lazily resolves {@link HzCluster} from the web application context. */
    private HzCluster getCluster() {
        if (cluster == null) {
            ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(srvCtx);
            cluster = ac.getBean("hzCluster", HzCluster.class);
        }
        return cluster;
    }

    /**
     * Deregisters the active repository's {@code IMap} listener and clears the delegate. Called on {@link #destroy()}
     * and from {@link #resetDelegate()}.
     */
    private void tearDownDelegate() {
        synchronized (this) {
            delegate = null;
            if (activeRepository != null) {
                activeRepository.destroy(); // deregisters the IMap MapListener
                activeRepository = null;
            }
        }
    }

    /**
     * Clears the running delegate so that the next request re-evaluates whether session sharing is still enabled and,
     * if so, constructs a fresh repository. Call this from the admin UI when toggling session sharing on or off at
     * runtime.
     */
    public void resetDelegate() {
        tearDownDelegate();
    }

    /** Returns the delegate, for testing purposes */
    SessionRepositoryFilter<?> getDelegate() {
        return delegate;
    }
}
