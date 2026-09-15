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

import io.tileverse.geoserver.web.storage.Select2ChoiceParamPanel;
import io.tileverse.geoserver.web.storage.StorageParamsPanel;
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
        assertStorageVisibility(CACHING_ENABLED, false);
        PROVIDER_REPRESENTATIVE_PARAMS.forEach(param -> assertStorageVisibility(param, false));
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

            boolean cacheable = !"file".equals(provider);
            assertStorageVisibility(CACHING_ENABLED, cacheable);
            for (String param : PROVIDER_REPRESENTATIVE_PARAMS) {
                boolean expected = param.equals(representativeParam.get(provider));
                assertStorageVisibility(param, expected);
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

        ParamPanel<?> regionPanel = (ParamPanel<?>) storageSection().fieldFor(S3_REGION);
        assertEquals("us-west-2", regionPanel.getFormComponent().getDefaultModelObject());
        assertStorageVisibility(S3_REGION, true);
        assertStorageVisibility(AZURE_ENDPOINT, false);
    }

    /** The group headers come from the storage-web bundle and the widgets from its factory, not from this module. */
    @Test
    public void storageSectionUsesTheSharedLabelsAndWidgets() {
        DataStoreInfo store = addStore("pmtiles-labels", Map.of(PROVIDER, "s3"));
        login();
        tester.startPage(new DataAccessEditPage(store.getId()));

        assertTrue(
                "missing the S3 group header", tester.getLastResponseAsString().contains("AWS S3 parameters"));
        assertTrue(
                "the region is not a searchable dropdown",
                storageSection().fieldFor(S3_REGION) instanceof Select2ChoiceParamPanel);
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
        assertStorageVisibility(S3_REGION, true);
        assertStorageVisibility(CACHING_ENABLED, true);
        assertStorageVisibility(AZURE_ENDPOINT, false);

        selectProvider("file");
        assertStorageVisibility(S3_REGION, false);
        assertStorageVisibility(CACHING_ENABLED, false);
    }

    /**
     * URI-typed params like the PMTiles location go through GeoServer's data-directory Wicket converter, which turns
     * anything that isn't a data-directory file into null. The panel keeps them String-typed; this guards against the
     * location and the endpoint silently vanishing on save.
     */
    @Test
    public void savePreservesUriTypedParams() {
        startNewPage();
        selectProvider("s3");

        FormTester form = tester.newFormTester("dataStoreForm", false);
        form.setValue("dataStoreNamePanel:border:border_body:paramValue", "s3-endpoint-store");
        setParam(form, paramPanelsByName().get("pmtiles"), "s3://shortbread/europe.pmtiles");
        setParam(form, storageSection().fieldFor(S3_ENDPOINT), "http://localhost:1");
        setParam(form, storageSection().fieldFor("storage.s3.aws-access-key-id"), "demo");
        setParam(form, storageSection().fieldFor("storage.s3.aws-secret-access-key"), "demo");

        tester.executeAjaxEvent("dataStoreForm:save", "click");

        Form<?> dataStoreForm = (Form<?>) tester.getComponentFromLastRenderedPage("dataStoreForm");
        DataStoreInfo info = (DataStoreInfo) dataStoreForm.getModelObject();
        assertEquals(
                "s3://shortbread/europe.pmtiles",
                String.valueOf(info.getConnectionParameters().get("pmtiles")));
        assertEquals(
                "http://localhost:1",
                String.valueOf(info.getConnectionParameters().get(S3_ENDPOINT)));
    }

    private void setParam(FormTester form, Component panel, String value) {
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

    /** The shared storage section, hosted by the row of the first {@code storage.*} parameter. */
    private StorageParamsPanel storageSection() {
        MarkupContainer paramsList = (MarkupContainer) tester.getComponentFromLastRenderedPage(PARAMS_PATH);
        StorageParamsPanel section = paramsList.visitChildren(
                StorageParamsPanel.class, (panel, visit) -> visit.stop((StorageParamsPanel) panel));
        assertNotNull("no storage section on the page", section);
        return section;
    }

    private void assertVisibility(Map<String, Component> panels, String paramName, boolean expectedVisible) {
        Component panel = panels.get(paramName);
        assertNotNull("no panel for param " + paramName, panel);
        assertEquals("visibility of " + paramName, expectedVisible, panel.isVisible());
    }

    private void assertStorageVisibility(String paramName, boolean expectedVisible) {
        Component row = storageSection().rowFor(paramName);
        assertNotNull("no row for param " + paramName, row);
        assertEquals("visibility of " + paramName, expectedVisible, row.isVisible());
    }

    private void selectProvider(String providerId) {
        RadioGroup<?> group = storageSection()
                .visitChildren(RadioGroup.class, (radioGroup, visit) -> visit.stop((RadioGroup<?>) radioGroup));
        assertNotNull("no provider selector on the page", group);

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
