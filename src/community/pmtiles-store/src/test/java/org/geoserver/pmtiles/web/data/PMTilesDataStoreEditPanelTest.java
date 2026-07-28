/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.panel.ParamPanel;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.junit.Test;

/** Tests for {@link PMTilesDataStoreEditPanel} */
public class PMTilesDataStoreEditPanelTest extends GeoServerWicketTestSupport {

    private static final String PARAMS_PATH = "dataStoreForm:parametersPanel:parameters";

    private static final String PROVIDER = "storage.provider";
    private static final String CACHING_ENABLED = "storage.caching.enabled";
    private static final String HTTP_TIMEOUT = "storage.http.timeout-millis";
    private static final String S3_REGION = "storage.s3.region";
    private static final String S3_ENDPOINT = "storage.s3.endpoint";
    private static final String AZURE_ENDPOINT = "storage.azure.endpoint";
    private static final String GCS_PROJECT_ID = "storage.gcs.project-id";

    private static final List<String> PROVIDER_REPRESENTATIVE_PARAMS =
            List.of(HTTP_TIMEOUT, S3_REGION, AZURE_ENDPOINT, GCS_PROJECT_ID);

    @Test
    public void newStoreGetsNoDefaultProvider() {
        startNewPage();
        Form<?> form = (Form<?>) tester.getComponentFromLastRenderedPage("dataStoreForm");
        DataStoreInfo info = (DataStoreInfo) form.getModelObject();

        assertNull(info.getConnectionParameters().get(PROVIDER));
    }

    @Test
    public void noProviderSelectedShowsOnlyCommonParams() {
        startNewPage();
        Map<String, Component> panels = paramPanelsByName();

        assertVisibility(panels, "namespace", true);
        assertVisibility(panels, "pmtiles", true);
        assertVisibility(panels, PROVIDER, true);
        assertVisibility(panels, CACHING_ENABLED, false);
        PROVIDER_REPRESENTATIVE_PARAMS.forEach(param -> assertVisibility(panels, param, false));
    }

    @Test
    public void editPageShowsOnlySelectedProviderParams() {
        Map<String, String> representativeParam = Map.of(
                "http", HTTP_TIMEOUT,
                "s3", S3_REGION,
                "azure", AZURE_ENDPOINT,
                "gcs", GCS_PROJECT_ID);

        for (String provider : List.of("file", "http", "s3", "azure", "gcs")) {
            DataStoreInfo store = addStore("pmtiles-" + provider, Map.of(PROVIDER, provider));
            login();
            tester.startPage(new DataAccessEditPage(store.getId()));
            Map<String, Component> panels = paramPanelsByName();

            boolean cacheable = !"file".equals(provider);
            assertVisibility(panels, CACHING_ENABLED, cacheable);
            for (String param : PROVIDER_REPRESENTATIVE_PARAMS) {
                boolean expected = param.equals(representativeParam.get(provider));
                assertVisibility(panels, param, expected);
            }
        }
    }

    @Test
    public void legacyKeysRewrittenAndWidgetsPopulatedOnEdit() {
        DataStoreInfo store = addStore(
                "pmtiles-legacy",
                Map.of(
                        "io.tileverse.rangereader.provider", "s3",
                        "io.tileverse.rangereader.s3.region", "us-west-2"));
        login();
        tester.startPage(new DataAccessEditPage(store.getId()));

        Form<?> form = (Form<?>) tester.getComponentFromLastRenderedPage("dataStoreForm");
        DataStoreInfo edited = (DataStoreInfo) form.getModelObject();
        Map<String, Serializable> params = edited.getConnectionParameters();

        assertFalse(params.containsKey("io.tileverse.rangereader.provider"));
        assertEquals("s3", params.get(PROVIDER));
        assertEquals("us-west-2", params.get(S3_REGION));

        Map<String, Component> panels = paramPanelsByName();
        ParamPanel<?> regionPanel = (ParamPanel<?>) panels.get(S3_REGION);
        assertEquals("us-west-2", regionPanel.getFormComponent().getDefaultModelObject());
        assertVisibility(panels, S3_REGION, true);
        assertVisibility(panels, AZURE_ENDPOINT, false);
    }

    @Test
    public void namespaceFollowsWorkspaceOnEdit() {
        Catalog catalog = getCatalog();
        WorkspaceInfo workspace = catalog.getFactory().createWorkspace();
        workspace.setName("pmtiles-ws");
        NamespaceInfo namespace = catalog.getFactory().createNamespace();
        namespace.setPrefix("pmtiles-ws");
        namespace.setURI("http://pmtiles-ws.example.com");
        catalog.add(workspace);
        catalog.add(namespace);

        DataStoreInfo store = catalog.getStore(
                addStore("pmtiles-stale-ns", Map.of(PROVIDER, "file")).getId(), DataStoreInfo.class);
        store.setWorkspace(workspace);
        catalog.save(store);
        String staleNamespace = catalog.getDefaultNamespace().getURI();
        assertEquals(staleNamespace, store.getConnectionParameters().get("namespace"));

        login();
        tester.startPage(new DataAccessEditPage(store.getId()));

        Form<?> form = (Form<?>) tester.getComponentFromLastRenderedPage("dataStoreForm");
        DataStoreInfo edited = (DataStoreInfo) form.getModelObject();
        assertEquals(
                "http://pmtiles-ws.example.com",
                edited.getConnectionParameters().get("namespace"));
    }

    @Test
    public void ajaxProviderSwitchTogglesVisibility() {
        startNewPage();

        selectProvider("s3");
        Map<String, Component> panels = paramPanelsByName();
        assertVisibility(panels, S3_REGION, true);
        assertVisibility(panels, CACHING_ENABLED, true);
        assertVisibility(panels, AZURE_ENDPOINT, false);

        selectProvider("file");
        panels = paramPanelsByName();
        assertVisibility(panels, S3_REGION, false);
        assertVisibility(panels, CACHING_ENABLED, false);
    }

    /**
     * URI-typed params like storage.s3.endpoint go through GeoServer's data-directory Wicket converter, which turns
     * anything that isn't a data-directory file into null. The panel keeps them String-typed; this guards against the
     * endpoint silently vanishing on save.
     */
    @Test
    public void savePreservesUriTypedParams() {
        startNewPage();
        selectProvider("s3");

        FormTester form = tester.newFormTester("dataStoreForm", false);
        form.setValue("dataStoreNamePanel:border:border_body:paramValue", "s3-endpoint-store");
        setParam(form, "pmtiles", "s3://shortbread/europe.pmtiles");
        setParam(form, S3_ENDPOINT, "http://localhost:1");
        setParam(form, "storage.s3.aws-access-key-id", "demo");
        setParam(form, "storage.s3.aws-secret-access-key", "demo");

        tester.executeAjaxEvent("dataStoreForm:save", "click");

        Form<?> dataStoreForm = (Form<?>) tester.getComponentFromLastRenderedPage("dataStoreForm");
        DataStoreInfo info = (DataStoreInfo) dataStoreForm.getModelObject();
        assertEquals(
                "http://localhost:1",
                String.valueOf(info.getConnectionParameters().get(S3_ENDPOINT)));
    }

    private void setParam(FormTester form, String paramName, String value) {
        Component panel = paramPanelsByName().get(paramName);
        FormComponent<?> formComponent = ((ParamPanel<?>) panel).getFormComponent();
        String relativePath = formComponent.getPageRelativePath().substring("dataStoreForm:".length());
        form.setValue(relativePath, value);
    }

    private void startNewPage() {
        login();
        tester.startPage(new DataAccessNewPage(new PMTilesDataStoreFactory().getDisplayName()));
    }

    private DataStoreInfo addStore(String name, Map<String, String> connectionParams) {
        Catalog catalog = getCatalog();
        DataStoreInfo store = catalog.getFactory().createDataStore();
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.setName(name);
        store.setType(new PMTilesDataStoreFactory().getDisplayName());
        store.getConnectionParameters()
                .put("namespace", catalog.getDefaultNamespace().getURI());
        store.getConnectionParameters().put("pmtiles", "file:/data/test.pmtiles");
        store.getConnectionParameters().putAll(connectionParams);
        catalog.add(store);
        return store;
    }

    /** Maps each connection parameter name to the form panel editing it */
    private Map<String, Component> paramPanelsByName() {
        MarkupContainer paramsList = (MarkupContainer) tester.getComponentFromLastRenderedPage(PARAMS_PATH);
        Map<String, Component> panels = new LinkedHashMap<>();
        paramsList.visitChildren(ListItem.class, (item, visit) -> {
            panels.put(item.getDefaultModelObjectAsString(), ((ListItem<?>) item).get("parameterPanel"));
            visit.dontGoDeeper();
        });
        return panels;
    }

    private void assertVisibility(Map<String, Component> panels, String paramName, boolean expectedVisible) {
        Component panel = panels.get(paramName);
        assertNotNull("no panel for param " + paramName, panel);
        assertEquals("visibility of " + paramName, expectedVisible, panel.isVisible());
    }

    private void selectProvider(String providerId) {
        Component providerPanel = paramPanelsByName().get(PROVIDER);
        RadioGroup<?> group = (RadioGroup<?>) providerPanel.get("group");

        List<Radio<?>> radios = new ArrayList<>();
        group.visitChildren(Radio.class, (radio, visit) -> radios.add((Radio<?>) radio));
        int selectionIndex = -1;
        for (int i = 0; i < radios.size(); i++) {
            if (providerId.equals(radios.get(i).getDefaultModelObject())) {
                selectionIndex = i;
            }
        }
        assertTrue("no radio for provider " + providerId, selectionIndex >= 0);

        FormTester form = tester.newFormTester("dataStoreForm", false);
        String groupPath = group.getPageRelativePath().substring("dataStoreForm:".length());
        form.select(groupPath, selectionIndex);
        AjaxFormChoiceComponentUpdatingBehavior behavior = group.getBehaviors(
                        AjaxFormChoiceComponentUpdatingBehavior.class)
                .get(0);
        tester.executeBehavior(behavior);
        assertEquals("provider not applied", providerId, group.getDefaultModelObject());
    }
}
