package org.geoserver.metadata;

import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_DISABLED;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.metadata.data.service.impl.ConfigurationServiceImpl;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractWicketMetadataTest extends AbstractMetadataTest {

    @Autowired
    protected GeoServerApplication app;

    @Autowired
    private ConfigurationServiceImpl configService;

    protected WicketTester tester;

    @BeforeClass
    public static void initialize() {
        // disable browser detection, makes testing harder for nothing
        GeoServerApplication.DETECT_BROWSER = false;
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GEOSERVER_CSRF_DISABLED, "true");

        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
    }

    @Before
    public void start() throws Exception {
        // Defensive: ensure metadata configuration is loaded before creating the WicketTester.
        // While JUnit 4 guarantees parent @Before (setupAndLoadDataDirectory) runs first,
        // the config can still be null if readConfiguration() failed silently (YAML files
        // not yet copied to the data directory, or parse errors logged at FINE level).
        if (configService.getMetadataConfiguration() == null) {
            configService.reload();
        }
        tester = new WicketTester(app, true);
    }

    @After
    public void stop() throws Exception {
        tester.destroy();
    }

    /**
     * Navigates to the Metadata tab on the ResourceConfigurationPage by finding it dynamically based on its title. This
     * avoids hardcoding the tab index which varies depending on which extensions are loaded in the test context.
     */
    protected void navigateToMetadataTab() {
        TabbedPanel<?> tabbedPanel = (TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs");
        int metadataTabIndex = findMetadataTabIndex(tabbedPanel);
        tabbedPanel.setSelectedTab(metadataTabIndex);
        tester.submitForm("publishedinfo");
        // verify we actually landed on the metadata tab — the panel contains either a TabbedPanel
        // (when metadata-tabs.yaml is configured) or directly a MetadataPanel (when no sub-tabs)
        org.apache.wicket.Component metadataComponent =
                tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel");
        assertNotNull("Metadata panel not found after navigating to Metadata tab", metadataComponent);
    }

    /**
     * Finds the index of the metadata tab by matching the tab title. The title is resolved from the resource key
     * "MetadataTabPanelInfo.title" which defaults to "Metadata".
     */
    private int findMetadataTabIndex(TabbedPanel<?> tabbedPanel) {
        java.util.List<? extends ITab> tabs = tabbedPanel.getTabs();
        for (int i = 0; i < tabs.size(); i++) {
            String title = tabs.get(i).getTitle().getObject();
            if ("Metadata".equals(title)) {
                return i;
            }
        }
        // provide a helpful error message listing available tabs
        StringBuilder available = new StringBuilder();
        for (int i = 0; i < tabs.size(); i++) {
            available
                    .append(i)
                    .append(": ")
                    .append(tabs.get(i).getTitle().getObject())
                    .append(", ");
        }
        throw new AssertionError("Could not find 'Metadata' tab. Available tabs: " + available);
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /** Checks for existence of a system property named "quietTests". */
    public static boolean isQuietTests() {
        String quietTests = System.getProperty("quietTests");
        return quietTests != null && !"false".equalsIgnoreCase(quietTests);
    }
}
