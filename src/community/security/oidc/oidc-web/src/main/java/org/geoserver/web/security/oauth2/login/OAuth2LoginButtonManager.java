/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ExtensionProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.SecurityManagerListener;
import org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId;
import org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationRegistry;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent;
import org.geoserver.web.LoginFormInfo;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Manages dynamic registration of OAuth2 / OpenID Connect login buttons — one per active filter instance.
 *
 * <p>Each {@link GeoServerOAuth2LoginAuthenticationFilter} that activates a provider publishes an
 * {@link OAuth2LoginButtonEnablementEvent} during its construction. This manager responds by creating a corresponding
 * {@link LoginFormInfo} singleton bean — visible to {@link org.geoserver.web.GeoServerBasePage}'s by-type scan — with a
 * deep link to the filter's scoped authorization endpoint. When the provider is disabled (or the filter reconfigured to
 * a different provider type), the matching singleton is deregistered.
 *
 * <p>This per-filter registration replaces the previous design which declared four static {@link LoginFormInfo} beans
 * (one per built-in provider type) whose {@code loginPath} was rewritten in place. The static design collapsed multiple
 * filter instances sharing the same provider type into a single button pointing at "an arbitrary surviving one", which
 * was incorrect when administrators register several filters of the same provider type — for example one OIDC filter
 * per identity provider (Keycloak, Auth0, custom Entra, ...). With dynamic registration each filter gets its own
 * dedicated button with its own deep link.
 *
 * <p>Per-instance chain-membership gating happens at registration time inside {@link #sweepOAuth2Filters}: only OAuth2
 * filters that are bound to at least one request filter chain produce a singleton; filters that are dropped from every
 * chain have their singletons destroyed on the next sweep. The rendering side
 * ({@link org.geoserver.web.GeoServerBasePage}) therefore stays unchanged and continues to consume all registered
 * {@link LoginFormInfo} beans by type.
 *
 * <h2>Lifecycle and thread-safety</h2>
 *
 * <p>The manager listens via {@link EventListener} for {@link OAuth2LoginButtonEnablementEvent}s that arrive whenever a
 * {@link GeoServerOAuth2LoginAuthenticationFilter} is (re)built — both at startup as the security configuration is
 * loaded, and any time an administrator saves a filter through the UI. Multiple saves can race on different filters, so
 * the listener is {@code synchronized}: each enable / disable composes a map update with a bean-factory mutation that
 * must be atomic together.
 *
 * @see OAuth2LoginButtonEnablementEvent
 * @see org.geoserver.web.LoginFormInfo
 * @see org.geoserver.web.GeoServerBasePage
 */
public class OAuth2LoginButtonManager
        implements BeanFactoryAware,
                ApplicationListener<ContextRefreshedEvent>,
                SecurityManagerListener,
                ExtensionProvider<LoginFormInfo> {

    private static final Logger LOGGER = Logging.getLogger(OAuth2LoginButtonManager.class);

    /** Separator between filter name and base registration ID inside a scoped registration ID. */
    static final String SCOPED_REG_ID_SEPARATOR = "__";

    /** Prefix for dynamically-registered {@link LoginFormInfo} singleton bean names. */
    static final String DYNAMIC_BEAN_NAME_PREFIX = "oauth2LoginButton__";

    /** Icon resource (under the {@link OAuth2LoginAuthProviderPanel} package) used when no provider matches. */
    private static final String DEFAULT_ICON = "openid.png";

    /**
     * Cast to {@link DefaultListableBeanFactory} rather than the more generic {@code ConfigurableListableBeanFactory}
     * interface because we need both {@code registerSingleton(String, Object)} (inherited from
     * {@code SingletonBeanRegistry}) and {@code destroySingleton(String)} (defined on the
     * {@code DefaultSingletonBeanRegistry} support class, not on the generic factory interface in Spring 7+). All
     * GeoServer web application contexts use this concrete factory.
     */
    private DefaultListableBeanFactory beanFactory;

    /** Dynamically-registered buttons keyed by scoped registration ID. */
    private final Map<String, LoginFormInfo> registeredButtons = new ConcurrentHashMap<>();

    /**
     * Resolved on the first {@link ContextRefreshedEvent}; the bean factory is set by Spring earlier (via
     * {@link #setBeanFactory(BeanFactory)}) but the security manager bean is not necessarily ready until the context
     * has finished initialising. Used to register ourselves as a {@link SecurityManagerListener} and to perform a
     * post-save sweep of all OIDC filters — see {@link #sweepOAuth2Filters()}.
     */
    private GeoServerSecurityManager securityManager;

    /** Guards against duplicate listener registration on repeat context-refresh events (e.g. test harnesses). */
    private boolean securityManagerListenerRegistered = false;

    public OAuth2LoginButtonManager() {
        super();
    }

    /**
     * On Spring context refresh, hook the manager into GeoServer's {@link GeoServerSecurityManager} as a
     * {@link SecurityManagerListener} and do an initial sweep so the buttons for any pre-existing OIDC filters register
     * without having to wait for the next save.
     *
     * <p>This is the runtime hook that makes "save a new OIDC filter through the UI → button appears on next page load"
     * work without a container restart. {@link GeoServerSecurityManager#saveFilter} alone does not rebuild filter
     * instances for fresh saves (its {@code fireChanged} flag only flips for <em>modifications</em> to filters already
     * bound to a chain), so newly-saved filters never get to the build path that publishes the
     * {@link OAuth2LoginButtonEnablementEvent}. The fix is layered:
     *
     * <ul>
     *   <li>When the admin then binds the new filter to the {@code /web/**} chain and saves it, the security manager
     *       calls {@link GeoServerSecurityManager#saveSecurityConfig} which DOES fire change notifications.
     *   <li>We listen for those notifications via {@link #handlePostChanged} and sweep all OIDC filters by name,
     *       forcing each to load through the provider's {@code createFilter} path, which publishes the enablement
     *       events, which {@link #enablementChanged} captures and turns into singleton {@link LoginFormInfo} beans.
     *   <li>The sweep is idempotent — {@link #registerButton} no-ops on already-registered scoped IDs.
     * </ul>
     */
    /**
     * Register as a {@link SecurityManagerListener} after the application context is fully refreshed.
     *
     * <p>Lifecycle choice rationale:
     *
     * <ul>
     *   <li>{@link #setBeanFactory(BeanFactory)} and {@code afterPropertiesSet()} fire <em>during</em> this bean's
     *       initialization. Asking the factory for {@code GeoServerSecurityManager.class} at that moment triggers
     *       Spring to resolve {@code authenticationManager} (still mid-construction in GeoServer's bean graph) and
     *       throws {@code BeanCurrentlyInCreationException}.
     *   <li>{@code @EventListener ContextRefreshedEvent} would work, but oidc-web's {@code applicationContext.xml}
     *       doesn't declare {@code <context:annotation-config/>}, so the {@code EventListenerMethodProcessor} that
     *       handles {@code @EventListener} is not installed in this context.
     *   <li>{@link ApplicationListener} is a Spring-detected interface: any singleton bean implementing it is
     *       automatically wired by {@code AbstractApplicationContext.registerListeners()} during context refresh, with
     *       no annotation processing required.
     * </ul>
     *
     * <p>Context refresh fires the listener AFTER every bean in this context (and its parents) is fully constructed, so
     * the {@code GeoServerSecurityManager} lookup is safe.
     *
     * <p>The {@link #securityManagerListenerRegistered} flag guards against duplicate registration when the event fires
     * multiple times — e.g. if both the parent and a child context publish refresh events that reach this bean.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (securityManagerListenerRegistered || beanFactory == null) {
            return;
        }
        try {
            securityManager = beanFactory.getBean(GeoServerSecurityManager.class);
            securityManager.addListener(this);
            securityManagerListenerRegistered = true;
            LOGGER.log(Level.CONFIG, "OAuth2LoginButtonManager registered as SecurityManagerListener");
            // Initial sweep: the security manager may have rebuilt filter chains before this listener was wired
            // (the chain-proxy listener fires eagerly too). The sweep is idempotent against already-registered
            // buttons and corrects for any startup-time event we missed.
            sweepOAuth2Filters();
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "OAuth2LoginButtonManager could not register itself as a SecurityManagerListener; "
                            + "OIDC filter changes will not take effect until the next container restart. Cause: ",
                    e);
        }
    }

    /**
     * Invoked by GeoServer's {@link GeoServerSecurityManager} after a security configuration change (chain edits,
     * filter modifications). Sweeps all OIDC filters so any newly-bound filter gets its login button registered without
     * requiring a JVM restart.
     */
    @Override
    public void handlePostChanged(GeoServerSecurityManager pSecurityManager) {
        LOGGER.log(Level.FINE, "OAuth2LoginButtonManager.handlePostChanged invoked — running sweep");
        sweepOAuth2Filters();
    }

    /**
     * Brings the registered-button set in line with the current security configuration. Two responsibilities:
     *
     * <ul>
     *   <li><b>Register buttons for in-chain filters.</b> For every {@link GeoServerOAuth2LoginAuthenticationFilter}
     *       that is bound to at least one request filter chain, force a load via the provider's {@code createFilter}
     *       path — this calls the builder's {@code build()} which publishes the per-provider enable / disable events
     *       that {@link #enablementChanged} turns into singleton {@link LoginFormInfo} beans.
     *   <li><b>Drop buttons for off-chain filters.</b> Walk the existing registry, derive each entry's filter name from
     *       its scoped registration ID, and destroy any singleton whose filter is no longer bound to any chain. This is
     *       what gates button visibility on chain membership at <em>registration</em> time, so the rendering side
     *       (GeoServerBasePage) does not need any per-instance chain-membership knowledge.
     * </ul>
     *
     * <p>The sweep is idempotent: repeated runs against an unchanged config are no-ops at both the registration and
     * destroy layers.
     */
    private void sweepOAuth2Filters() {
        if (securityManager == null) {
            return;
        }
        try {
            Set<String> inChainFilterNames = collectInChainFilterNames();
            LOGGER.log(
                    Level.FINE,
                    "OAuth2LoginButtonManager.sweep: in-chain={0}; currently-registered={1}",
                    new Object[] {inChainFilterNames, registeredButtons.keySet()});

            // 1. Drop buttons for filters that were once in-chain but no longer are. Iterate over a snapshot of the
            //    registry keys to avoid concurrent modification while destroying singletons inside the loop.
            for (String existingScopedRegId : new HashSet<>(registeredButtons.keySet())) {
                String existingFilterName = filterNameFromScopedRegId(existingScopedRegId);
                if (!inChainFilterNames.contains(existingFilterName)) {
                    LOGGER.log(
                            Level.CONFIG,
                            "Dropping OAuth2 login button for filter {0} (no longer in any chain)",
                            existingFilterName);
                    unregisterButton(existingScopedRegId);
                }
            }

            // 1a. Also purge ClientRegistration contributions for off-chain filters from the shared registry —
            //     otherwise Spring's resolver would still find the stale registration and the off-chain filter's
            //     authorization URL would 302 to the IdP even though no GeoServer filter is mounted to handle the
            //     callback. The button manager owns lifecycle here because it is the listener wired to security
            //     configuration changes; the OAuth2 builder publishes registrations, the manager removes them.
            GeoServerOAuth2ClientRegistrationRegistry registry = lookupClientRegistrationRegistry();
            if (registry != null) {
                registry.retainFilters(inChainFilterNames);
            }

            // 2. Load every in-chain OAuth2 filter so its builder publishes the enable / disable events for each
            //    provider; the manager's @EventListener then registers (or refreshes) the matching buttons.
            SortedSet<String> oauth2FilterNames =
                    securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class);
            for (String filterName : oauth2FilterNames) {
                if (!inChainFilterNames.contains(filterName)) {
                    continue;
                }
                try {
                    // loadFilter goes through the provider's createFilter, which calls the builder's build(),
                    // which publishes the enablement events for each enabled provider on the filter.
                    securityManager.loadFilter(filterName);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Could not load OAuth2 filter " + filterName + " during sweep; skipping", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "OAuth2 filter sweep failed", e);
        }
    }

    /**
     * Resolve the application-wide {@link GeoServerOAuth2ClientRegistrationRegistry}, if present. May be {@code null}
     * in legacy / minimal test contexts that do not declare the registry bean — in which case the manager simply skips
     * registry cleanup and relies on the builder's fallback path.
     */
    private GeoServerOAuth2ClientRegistrationRegistry lookupClientRegistrationRegistry() {
        if (beanFactory == null) {
            return null;
        }
        try {
            return beanFactory.getBean(GeoServerOAuth2ClientRegistrationRegistry.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Collect every filter name that appears in any request filter chain. A filter referenced by more than one chain
     * counts once. The set is consulted by {@link #sweepOAuth2Filters} to decide which OAuth2 filters are eligible to
     * produce a login button.
     */
    private Set<String> collectInChainFilterNames() throws Exception {
        Set<String> names = new HashSet<>();
        for (RequestFilterChain chain :
                securityManager.getSecurityConfig().getFilterChain().getRequestChains()) {
            if (chain.getFilterNames() != null) {
                names.addAll(chain.getFilterNames());
            }
        }
        return names;
    }

    @Override
    public void setBeanFactory(BeanFactory pBeanFactory) throws BeansException {
        if (pBeanFactory instanceof DefaultListableBeanFactory) {
            // Just remember the factory; defer the {@link GeoServerSecurityManager} lookup + listener registration
            // until {@link #onApplicationEvent} runs, which is the only Spring hook that fires AFTER the entire
            // application context has finished initializing (so {@code authenticationManager} is no longer
            // mid-construction and the {@code GeoServerSecurityManager} bean is reachable).
            this.beanFactory = (DefaultListableBeanFactory) pBeanFactory;
        } else {
            LOGGER.log(
                    Level.WARNING,
                    "OAuth2LoginButtonManager requires DefaultListableBeanFactory but got {0};"
                            + " dynamic login button registration is disabled.",
                    pBeanFactory.getClass().getName());
        }
    }

    /**
     * Reacts to a provider being enabled or disabled on a specific OAuth2 / OIDC filter instance.
     *
     * <p>{@code synchronized} so that concurrent saves on different filters cannot race the singleton registry: each
     * register / unregister composes a map update with a bean-factory mutation that must be atomic together.
     */
    @EventListener
    public synchronized void enablementChanged(OAuth2LoginButtonEnablementEvent pEvent) {
        if (beanFactory == null) {
            // setBeanFactory either was never called (unusual outside tests) or rejected the factory
            // type (logged at WARN there). Nothing we can safely do here.
            return;
        }

        String scopedRegId = pEvent.getScopedRegistrationId();
        String baseRegId = pEvent.getRegistrationId();
        if (scopedRegId == null || baseRegId == null) {
            LOGGER.log(
                    Level.FINE, "Ignoring OAuth2LoginButtonEnablementEvent with null scoped or base registration ID.");
            return;
        }

        if (pEvent.isEnable()) {
            registerButton(scopedRegId, baseRegId);
        } else {
            unregisterButton(scopedRegId);
        }
    }

    private void registerButton(String scopedRegId, String baseRegId) {
        if (registeredButtons.containsKey(scopedRegId)) {
            // A repeat enable for the same scoped ID happens on every filter rebuild (e.g. each Save); keep the
            // existing entry so identity stays stable.
            return;
        }
        LoginFormInfo info = buildLoginFormInfo(scopedRegId, baseRegId);
        registeredButtons.put(scopedRegId, info);
        LOGGER.log(Level.FINE, "Registered OAuth2 login button: {0}", beanName(scopedRegId));
    }

    private void unregisterButton(String scopedRegId) {
        LoginFormInfo info = registeredButtons.remove(scopedRegId);
        if (info == null) {
            return;
        }
        LOGGER.log(Level.FINE, "Unregistered OAuth2 login button: {0}", beanName(scopedRegId));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LoginFormInfo buildLoginFormInfo(String scopedRegId, String baseRegId) {
        LoginFormInfo info = new LoginFormInfo();
        info.setId(beanName(scopedRegId));
        // Name drives the UI sort order on the login banner; the "oauth2-" prefix groups all OAuth2 buttons
        // together while the scoped registration ID keeps it stable and per-filter unique.
        info.setName("oauth2-" + scopedRegId);
        // Per-instance chain-membership is now gated at registration time by {@link #sweepOAuth2Filters}; the
        // rendering side reads only the standard {@link LoginFormInfo} fields. The scoped registration ID is still
        // recoverable from the loginPath if a consumer needs it.
        // Raw casts mirror the prior static XML declarations (see the deleted openIdConnect*LoginButton
        // beans in applicationContext.xml): LoginFormInfo.filterClass is typed
        // Class<GeoServerSecurityProvider> for historical reasons but the consumer code in
        // GeoServerBasePage only uses its name for chain matching, so the actual type erasure does not
        // matter at runtime.
        info.setFilterClass((Class) GeoServerOAuth2LoginAuthenticationFilter.class);
        info.setComponentClass((Class) OAuth2LoginAuthProviderPanel.class);
        info.setIcon(iconFor(baseRegId));
        info.setTitleKey(titleKeyFor(baseRegId));
        info.setDescriptionKey(descriptionKeyFor(baseRegId));
        info.setLoginPath("/" + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + scopedRegId);
        info.setMethod("GET");
        info.setEnabled(true);
        info.setJustUseExternalLink(true);
        return info;
    }

    private static String beanName(String scopedRegId) {
        return DYNAMIC_BEAN_NAME_PREFIX + scopedRegId;
    }

    /**
     * Extracts the filter name from a scoped registration ID. The convention enforced by
     * {@link GeoServerOAuth2ClientRegistrationId#scopedRegId(String, String)} is
     * {@code <filterName><SCOPED_REG_ID_SEPARATOR><baseRegistrationId>}. When the separator is missing — a defensive
     * case that should not occur in practice — the whole string is treated as the filter name.
     */
    static String filterNameFromScopedRegId(String scopedRegId) {
        int sep = scopedRegId.indexOf(SCOPED_REG_ID_SEPARATOR);
        return sep > 0 ? scopedRegId.substring(0, sep) : scopedRegId;
    }

    private static String iconFor(String baseRegId) {
        String lower = baseRegId.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "google":
                return "google.png";
            case "github":
                return "github.png";
            case "microsoft":
                return "microsoft.png";
            default:
                return DEFAULT_ICON;
        }
    }

    /**
     * Resource-bundle key for the button label. Empty for icon-only providers (Google / GitHub / Microsoft) whose
     * branding speaks for itself; the OIDC custom provider gets a label since its generic OpenID icon benefits from
     * accompanying text — especially when more than one custom-OIDC filter is configured.
     */
    private static String titleKeyFor(String baseRegId) {
        if ("oidc".equalsIgnoreCase(baseRegId)) {
            return "openidconnect.login.button.title";
        }
        return "";
    }

    private static String descriptionKeyFor(String baseRegId) {
        String lower = baseRegId.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "google":
                return "OAuth2LoginAuthProviderPanel.googleDescription";
            case "github":
                return "OAuth2LoginAuthProviderPanel.gitHubDescription";
            case "microsoft":
                return "OAuth2LoginAuthProviderPanel.msDescription";
            default:
                return "OAuth2LoginAuthProviderPanel.oidcDescription";
        }
    }

    /** Read-only view of the registered buttons keyed by scoped registration ID. Visible for testing. */
    Map<String, LoginFormInfo> getRegisteredButtons() {
        return Collections.unmodifiableMap(registeredButtons);
    }

    // ── ExtensionProvider<LoginFormInfo> ────────────────────────────────────
    //
    // Bypass GeoServerExtensions.extensionsCache for our dynamic singletons. That cache (a static
    // ConcurrentHashMap in GeoServerExtensions, keyed by extension-point class) is populated on the
    // first lookup of bean names of a given type, and is only invalidated on a fresh ContextRefreshedEvent
    // — NOT on registerSingleton/destroySingleton calls. So dynamically-registered LoginFormInfo singletons
    // (the heart of this manager's model) are invisible to GeoServerBasePage between context refreshes,
    // which manifests as "I added an OIDC filter and the button doesn't show until I restart GeoServer".
    //
    // ExtensionProvider is queried on EVERY call to GeoServerExtensions.extensions(...), no caching. By
    // returning the live snapshot of our registry here we keep the rendered button list in sync with the
    // current security configuration without requiring a JVM restart or any cache busting at the
    // GeoServerExtensions layer.

    @Override
    public Class<LoginFormInfo> getExtensionPoint() {
        return LoginFormInfo.class;
    }

    @Override
    public List<LoginFormInfo> getExtensions(Class<LoginFormInfo> extensionPoint) {
        // Snapshot — concurrent registry mutations are safe to iterate via the ConcurrentHashMap, and
        // returning a copy decouples consumers from later updates.
        return new ArrayList<>(registeredButtons.values());
    }
}
