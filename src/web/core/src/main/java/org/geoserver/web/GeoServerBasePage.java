/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.INamedParameters.Type;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Base class for web pages in GeoServer web application.
 *
 * <ul>
 *   <li>The basic layout
 *   <li>An OO infrastructure for common elements location
 *   <li>An infrastructure for locating subpages in the Spring context and creating links
 * </ul>
 *
 * @author Andrea Aaime, The Open Planning Project
 * @author Justin Deoliveira, The Open Planning Project
 */
public class GeoServerBasePage extends WebPage implements IAjaxIndicatorAware {

    /** The id of the panel sitting in the page-header, right below the page description */
    protected static final String HEADER_PANEL = "headerPanel";

    protected static final Logger LOGGER = Logging.getLogger(GeoServerBasePage.class);

    protected static volatile GeoServerNodeInfo NODE_INFO;

    /** feedback panels to report errors and information. */
    protected FeedbackPanel topFeedbackPanel;

    protected FeedbackPanel bottomFeedbackPanel;

    /** page for this page to return to when the page is finished, could be null. */
    protected Page returnPage;

    /** page class for this page to return to when the page is finished, could be null. */
    protected Class<? extends Page> returnPageClass;

    @SuppressWarnings("serial")
    public GeoServerBasePage() {
        // lookup for a pluggable favicon
        PackageResourceReference faviconReference = null;
        List<HeaderContribution> cssContribs =
                getGeoServerApplication().getBeansOfType(HeaderContribution.class);
        for (HeaderContribution csscontrib : cssContribs) {
            try {
                if (csscontrib.appliesTo(this)) {
                    PackageResourceReference ref = csscontrib.getFavicon();
                    if (ref != null) {
                        faviconReference = ref;
                    }
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Problem adding header contribution", t);
            }
        }

        // favicon
        if (faviconReference == null) {
            faviconReference = new PackageResourceReference(GeoServerBasePage.class, "favicon.ico");
        }
        String faviconUrl = RequestCycle.get().urlFor(faviconReference, null).toString();
        add(new ExternalLink("faviconLink", faviconUrl, null));

        // page title
        add(
                new Label(
                        "pageTitle",
                        new LoadableDetachableModel<String>() {

                            @Override
                            protected String load() {
                                return getPageTitle();
                            }
                        }));

        // login / logout stuff
        List<String> securityFilters =
                getGeoServerApplication()
                        .getSecurityManager()
                        .getSecurityConfig()
                        .getFilterChain()
                        .filtersFor("/web/**");

        final Authentication user = GeoServerSession.get().getAuthentication();
        final boolean anonymous = user == null || user instanceof AnonymousAuthenticationToken;

        // login forms
        List<LoginFormInfo> loginforms =
                filterByAuth(getGeoServerApplication().getBeansOfType(LoginFormInfo.class));

        add(
                new ListView<LoginFormInfo>("loginforms", loginforms) {
                    public void populateItem(ListItem<LoginFormInfo> item) {
                        LoginFormInfo info = item.getModelObject();

                        WebMarkupContainer loginForm =
                                new WebMarkupContainer("loginform") {
                                    protected void onComponentTag(
                                            org.apache.wicket.markup.ComponentTag tag) {
                                        String path = getRequest().getUrl().getPath();
                                        StringBuilder loginPath = new StringBuilder();
                                        if (path.isEmpty()) {
                                            // home page
                                            loginPath.append("../" + info.getLoginPath());
                                        } else {
                                            // boomarkable page of sorts
                                            String[] pathElements = path.split("/");
                                            for (String pathElement : pathElements) {
                                                if (!pathElement.isEmpty()) {
                                                    loginPath.append("../");
                                                }
                                            }
                                            loginPath.append(info.getLoginPath());
                                        }
                                        tag.put("action", loginPath);
                                    };
                                };

                        Image image;
                        if (info.getIcon() != null) {
                            image =
                                    new Image(
                                            "link.icon",
                                            new PackageResourceReference(
                                                    info.getComponentClass(), info.getIcon()));
                        } else {
                            image =
                                    new Image(
                                            "link.icon",
                                            new PackageResourceReference(
                                                    GeoServerBasePage.class,
                                                    "img/icons/silk/door-in.png"));
                        }

                        loginForm.add(image);
                        if (info.getTitleKey() != null && !info.getTitleKey().isEmpty()) {
                            loginForm.add(
                                    new Label(
                                            "link.label",
                                            new StringResourceModel(
                                                    info.getTitleKey(), (Component) null, null)));
                            image.add(
                                    AttributeModifier.replace(
                                            "alt",
                                            new ParamResourceModel(info.getTitleKey(), null)));
                        } else {
                            loginForm.add(new Label("link.label", ""));
                        }

                        LoginFormHTMLInclude include;
                        if (info.getInclude() != null) {
                            include =
                                    new LoginFormHTMLInclude(
                                            "login.include",
                                            new PackageResourceReference(
                                                    info.getComponentClass(), info.getInclude()));
                        } else {
                            include = new LoginFormHTMLInclude("login.include", null);
                        }
                        loginForm.add(include);

                        item.add(loginForm);

                        boolean filterInChain = false;
                        List<GeoServerSecurityProvider> securityProviders =
                                getGeoServerApplication()
                                        .getBeansOfType(GeoServerSecurityProvider.class);
                        for (GeoServerSecurityProvider securityProvider : securityProviders) {
                            if (securityProvider.getFilterClass() != null
                                    && securityProvider
                                            .getFilterClass()
                                            .equals(info.getFilterClass())) {
                                filterInChain = true;
                                break;
                            }
                        }
                        loginForm.setVisible(anonymous && filterInChain);
                    }
                });

        // logout forms
        WebMarkupContainer loggedInAsForm = new WebMarkupContainer("loggedinasform");
        loggedInAsForm.add(new Label("loggedInUsername", anonymous ? "Nobody" : user.getName()));
        loggedInAsForm.setVisible(!anonymous);
        add(loggedInAsForm);

        List<LogoutFormInfo> logoutforms =
                filterByAuth(getGeoServerApplication().getBeansOfType(LogoutFormInfo.class));

        add(
                new ListView<LogoutFormInfo>("logoutforms", logoutforms) {
                    public void populateItem(ListItem<LogoutFormInfo> item) {
                        LogoutFormInfo info = item.getModelObject();

                        WebMarkupContainer logoutForm =
                                new WebMarkupContainer("logoutform") {
                                    protected void onComponentTag(
                                            org.apache.wicket.markup.ComponentTag tag) {
                                        String path = getRequest().getUrl().getPath();
                                        StringBuilder logoutPath = new StringBuilder();
                                        if (path.isEmpty()) {
                                            // home page
                                            logoutPath.append("../" + info.getLogoutPath());
                                        } else {
                                            // boomarkable page of sorts
                                            String[] pathElements = path.split("/");
                                            for (String pathElement : pathElements) {
                                                if (!pathElement.isEmpty()) {
                                                    logoutPath.append("../");
                                                }
                                            }
                                            logoutPath.append(info.getLogoutPath());
                                        }
                                        tag.put("action", logoutPath);
                                    };
                                };

                        Image image;
                        if (info.getIcon() != null) {
                            image =
                                    new Image(
                                            "link.icon",
                                            new PackageResourceReference(
                                                    info.getComponentClass(), info.getIcon()));
                        } else {
                            image =
                                    new Image(
                                            "link.icon",
                                            new PackageResourceReference(
                                                    GeoServerBasePage.class,
                                                    "img/icons/silk/door-out.png"));
                        }

                        logoutForm.add(image);
                        if (info.getTitleKey() != null && !info.getTitleKey().isEmpty()) {
                            logoutForm.add(
                                    new Label(
                                            "link.label",
                                            new StringResourceModel(
                                                    info.getTitleKey(), (Component) null, null)));
                            image.add(
                                    AttributeModifier.replace(
                                            "alt",
                                            new ParamResourceModel(info.getTitleKey(), null)));
                        } else {
                            logoutForm.add(new Label("link.label", ""));
                        }

                        item.add(logoutForm);

                        boolean filterInChain = false;
                        for (String filterName : securityFilters) {
                            if (filterName.toLowerCase().contains(info.getName())) {
                                filterInChain = true;
                                break;
                            }
                        }
                        logoutForm.setVisible(!anonymous && filterInChain);
                    }
                });

        // home page link
        add(
                new BookmarkablePageLink("home", GeoServerHomePage.class)
                        .add(
                                new Label(
                                        "label",
                                        new StringResourceModel("home", (Component) null, null))));

        // dev buttons
        DeveloperToolbar devToolbar = new DeveloperToolbar("devButtons");
        add(devToolbar);
        devToolbar.setVisible(
                RuntimeConfigurationType.DEVELOPMENT.equals(
                        getApplication().getConfigurationType()));

        final Map<Category, List<MenuPageInfo>> links =
                splitByCategory(
                        filterByAuth(getGeoServerApplication().getBeansOfType(MenuPageInfo.class)));

        List<MenuPageInfo> standalone =
                links.containsKey(null) ? links.get(null) : new ArrayList<MenuPageInfo>();
        links.remove(null);

        List<Category> categories = new ArrayList<>(links.keySet());
        Collections.sort(categories);

        add(
                new ListView<Category>("category", categories) {
                    public void populateItem(ListItem<Category> item) {
                        Category category = item.getModelObject();
                        item.add(
                                new Label(
                                        "category.header",
                                        new StringResourceModel(
                                                category.getNameKey(), (Component) null, null)));
                        item.add(
                                new ListView<MenuPageInfo>("category.links", links.get(category)) {
                                    public void populateItem(ListItem<MenuPageInfo> item) {
                                        MenuPageInfo info = item.getModelObject();
                                        BookmarkablePageLink<Page> link =
                                                new BookmarkablePageLink<Page>(
                                                        "link", info.getComponentClass()) {

                                                    @Override
                                                    public PageParameters getPageParameters() {
                                                        PageParameters pageParams =
                                                                super.getPageParameters();
                                                        pageParams.add(
                                                                GeoServerTablePanel.FILTER_PARAM,
                                                                false,
                                                                Type.PATH);
                                                        return pageParams;
                                                    }
                                                };

                                        link.add(
                                                AttributeModifier.replace(
                                                        "title",
                                                        new StringResourceModel(
                                                                info.getDescriptionKey(),
                                                                (Component) null,
                                                                null)));
                                        link.add(
                                                new Label(
                                                        "link.label",
                                                        new StringResourceModel(
                                                                info.getTitleKey(),
                                                                (Component) null,
                                                                null)));
                                        Image image;
                                        if (info.getIcon() != null) {
                                            image =
                                                    new Image(
                                                            "link.icon",
                                                            new PackageResourceReference(
                                                                    info.getComponentClass(),
                                                                    info.getIcon()));
                                        } else {
                                            image =
                                                    new Image(
                                                            "link.icon",
                                                            new PackageResourceReference(
                                                                    GeoServerBasePage.class,
                                                                    "img/icons/silk/wrench.png"));
                                        }
                                        image.add(
                                                AttributeModifier.replace(
                                                        "alt",
                                                        new ParamResourceModel(
                                                                info.getTitleKey(), null)));

                                        link.add(image);
                                        item.add(link);
                                    }
                                });
                    }
                });

        add(
                new ListView<MenuPageInfo>("standalone", standalone) {
                    public void populateItem(ListItem<MenuPageInfo> item) {
                        MenuPageInfo info = item.getModelObject();
                        BookmarkablePageLink<Page> link =
                                new BookmarkablePageLink<>("link", info.getComponentClass());
                        link.add(
                                AttributeModifier.replace(
                                        "title",
                                        new StringResourceModel(
                                                info.getDescriptionKey(), (Component) null, null)));
                        link.add(
                                new Label(
                                        "link.label",
                                        new StringResourceModel(
                                                info.getTitleKey(), (Component) null, null)));
                        item.add(link);
                    }
                });

        add(topFeedbackPanel = new FeedbackPanel("topFeedback"));
        topFeedbackPanel.setOutputMarkupId(true);
        add(bottomFeedbackPanel = new FeedbackPanel("bottomFeedback"));
        bottomFeedbackPanel.setOutputMarkupId(true);

        // ajax feedback image
        add(
                new Image(
                        "ajaxFeedbackImage",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/ajax-loader.gif")));

        add(new WebMarkupContainer(HEADER_PANEL));

        // allow the subclasses to initialize before getTitle/getDescription are called
        add(
                new Label(
                        "gbpTitle",
                        new LoadableDetachableModel<String>() {

                            @Override
                            protected String load() {
                                return getTitle();
                            }
                        }));
        add(
                new Label(
                        "gbpDescription",
                        new LoadableDetachableModel<String>() {

                            @Override
                            protected String load() {
                                return getDescription();
                            }
                        }));

        // node id handling
        WebMarkupContainer container = new WebMarkupContainer("nodeIdContainer");
        add(container);
        String id = getNodeInfo().getId();
        Label label = new Label("nodeId", id);
        container.add(label);
        NODE_INFO.customize(container);
        if (id == null) {
            container.setVisible(false);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {

        // includes jquery, required by the placeholder plugin (wicket only include jquery if he
        // need it)
        response.render(
                new PriorityHeaderItem(
                        JavaScriptHeaderItem.forReference(
                                getApplication()
                                        .getJavaScriptLibrarySettings()
                                        .getJQueryReference())));

        List<HeaderContribution> cssContribs =
                getGeoServerApplication().getBeansOfType(HeaderContribution.class);
        for (HeaderContribution csscontrib : cssContribs) {
            try {
                if (csscontrib.appliesTo(this)) {
                    PackageResourceReference ref = csscontrib.getCSS();
                    if (ref != null) {
                        response.render(CssReferenceHeaderItem.forReference(ref));
                    }

                    ref = csscontrib.getJavaScript();
                    if (ref != null) {
                        response.render(JavaScriptHeaderItem.forReference(ref));
                    }

                    ref = csscontrib.getFavicon();
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Problem adding header contribution", t);
            }
        }
    }

    private GeoServerNodeInfo getNodeInfo() {
        // we don't synch on this one, worst it can happen, we create
        // two instances of DefaultGeoServerNodeInfo, and one wil be gc-ed soon
        if (NODE_INFO == null) {
            // see if someone plugged a custom node info bean, otherwise use the default one
            GeoServerNodeInfo info = GeoServerExtensions.bean(GeoServerNodeInfo.class);
            if (info == null) {
                info = new DefaultGeoServerNodeInfo();
            }
            NODE_INFO = info;
        }

        return NODE_INFO;
    }

    protected String getTitle() {
        return new ParamResourceModel("title", this).getString();
    }

    protected String getDescription() {
        return new ParamResourceModel("description", this).getString();
    }

    /**
     * Gets the page title from the PageName.title resource, falling back on "GeoServer" if not
     * found
     */
    String getPageTitle() {
        try {
            return "GeoServer: " + getTitle();
        } catch (Exception e) {
            LOGGER.warning(getClass().getSimpleName() + " does not have a title set");
        }
        return "GeoServer";
    }

    /**
     * The base page is built with an empty panel in the page-header section that can be filled by
     * subclasses calling this method
     *
     * @param component The component to be placed at the bottom of the page-header section. The
     *     component must have "page-header" id
     */
    protected void setHeaderPanel(Component component) {
        if (!HEADER_PANEL.equals(component.getId()))
            throw new IllegalArgumentException(
                    "The header panel component must have 'headerPanel' id");
        remove(HEADER_PANEL);
        add(component);
    }

    /** Returns the application instance. */
    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }

    @Override
    public GeoServerSession getSession() {
        return (GeoServerSession) super.getSession();
    }

    /** Convenience method for pages to get access to the geoserver configuration. */
    protected GeoServer getGeoServer() {
        return getGeoServerApplication().getGeoServer();
    }

    /** Convenience method for pages to get access to the catalog. */
    protected Catalog getCatalog() {
        return getGeoServerApplication().getCatalog();
    }

    /** Splits up the pages by category, turning the list into a map keyed by category */
    private Map<Category, List<MenuPageInfo>> splitByCategory(List<MenuPageInfo> pages) {
        Collections.sort(pages);
        HashMap<Category, List<MenuPageInfo>> map = new HashMap<Category, List<MenuPageInfo>>();

        for (MenuPageInfo page : pages) {
            Category cat = page.getCategory();

            if (!map.containsKey(cat)) map.put(cat, new ArrayList<MenuPageInfo>());

            map.get(cat).add(page);
        }

        return map;
    }

    /** Filters a set of component descriptors based on the current authenticated user. */
    protected <T extends ComponentInfo> List<T> filterByAuth(List<T> list) {
        Authentication user = getSession().getAuthentication();
        List<T> result = new ArrayList<T>();
        for (T component : list) {
            if (component.getAuthorizer() == null) {
                continue;
            }

            final Class<?> clazz = component.getComponentClass();
            if (!component.getAuthorizer().isAccessAllowed(clazz, user)) continue;
            result.add(component);
        }
        return result;
    }

    /**
     * Returns the id for the component used as a veil for the whole page while Wicket is processing
     * an ajax request, so it is impossible to trigger the same ajax action multiple times (think of
     * saving/deleting a resource, etc)
     *
     * @see IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
     */
    public String getAjaxIndicatorMarkupId() {
        return "ajaxFeedback";
    }

    /**
     * Sets the return page to navigate to when this page is done its task.
     *
     * @see #doReturn()
     */
    public GeoServerBasePage setReturnPage(Page returnPage) {
        this.returnPage = returnPage;
        return this;
    }

    /**
     * Sets the return page class to navigate to when this page is done its task.
     *
     * @see #doReturn()
     */
    public GeoServerBasePage setReturnPage(Class<? extends Page> returnPageClass) {
        this.returnPageClass = returnPageClass;
        return this;
    }

    /**
     * Returns from the page by navigating to one of {@link #returnPage} or {@link
     * #returnPageClass}, processed in that order.
     *
     * <p>This method should be called by pages that must return after doing some task on a form
     * submit such as a save or a cancel. If no return page has been set via {@link
     * #setReturnPage(Page)} or {@link #setReturnPageClass(Class)} then {@link GeoServerHomePage} is
     * used.
     */
    protected void doReturn() {
        doReturn(null);
    }

    /**
     * Returns from the page by navigating to one of {@link #returnPage} or {@link
     * #returnPageClass}, processed in that order.
     *
     * <p>This method accepts a parameter to use as a default in cases where {@link #returnPage} and
     * {@link #returnPageClass} are not set and a default other than {@link GeoServerHomePage}
     * should be used.
     *
     * <p>This method should be called by pages that must return after doing some task on a form
     * submit such as a save or a cancel. If no return page has been set via {@link
     * #setReturnPage(Page)} or {@link #setReturnPageClass(Class)} then {@link GeoServerHomePage} is
     * used.
     */
    protected void doReturn(Class<? extends Page> defaultPageClass) {
        if (returnPage != null) {
            setResponsePage(returnPage);
            return;
        }
        if (returnPageClass != null) {
            setResponsePage(returnPageClass);
            return;
        }

        defaultPageClass = defaultPageClass != null ? defaultPageClass : GeoServerHomePage.class;
        setResponsePage(defaultPageClass);
    }

    public void addFeedbackPanels(AjaxRequestTarget target) {
        target.add(topFeedbackPanel);
        target.add(bottomFeedbackPanel);
    }
}
