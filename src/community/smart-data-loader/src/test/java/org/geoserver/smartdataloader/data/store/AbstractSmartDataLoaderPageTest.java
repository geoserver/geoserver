package org.geoserver.smartdataloader.data.store;

import static org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport.ONLINE_DB_SCHEMA;
import static org.geotools.data.postgis.PostgisNGDataStoreFactory.SSL_MODE;
import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.DBTYPE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PORT;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.data.SmartDataLoaderDataAccessFactory;
import org.geoserver.smartdataloader.metadata.jdbc.utils.JdbcUrlSplitter;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.test.FixtureUtilities;
import org.junit.Test;
import org.postgresql.jdbc.SslMode;

/**
 * Implementation of GeoServerWicketTestSupport for SmartAppSchema Postgis tests, including
 * Geoserver and Wicket support.
 */
public abstract class AbstractSmartDataLoaderPageTest extends GeoServerWicketTestSupport {

    private DataAccessFactory dataStoreFactory = new SmartDataLoaderDataAccessFactory();

    protected Properties fixture;

    private DataStoreInfo dataStore;

    private JDBCTestSetup setup;

    private static final String SIMPLE_DATA_STORE_NAME = "simple-data-store";

    private JDBCFixtureHelper fixtureHelper;

    public AbstractSmartDataLoaderPageTest(JDBCFixtureHelper fixtureHelper) {
        this.fixtureHelper = fixtureHelper;
    }

    protected DataAccessNewPage startPage() {
        login();
        final DataAccessNewPage page = new DataAccessNewPage(dataStoreFactory.getDisplayName());
        tester.startPage(page);
        return page;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        this.fixture = getFixture();
        assumeNotNull(fixture);
        this.setup = createTestSetup();
        setup.setFixture(fixture);
        setup.setUp();
        // setup geoserver datadir
        setupGeoServerTestData();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        if (dataStore != null) getCatalog().remove(dataStore);
        if (setup != null) setup.tearDown();
    }

    protected Properties getFixture() {
        File fixtureFile =
                FixtureUtilities.getFixtureFile(
                        getFixtureDirectory(), fixtureHelper.getFixtureId());
        if (fixtureFile.exists()) {
            return FixtureUtilities.loadProperties(fixtureFile);
        } else {
            Properties exampleFixture = fixtureHelper.createExampleFixture();
            if (exampleFixture != null) {
                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                if (!exFixtureFile.exists()) {
                    createExampleFixture(exFixtureFile, exampleFixture);
                }
            }
            FixtureUtilities.printSkipNotice(fixtureHelper.getFixtureId(), fixtureFile);
            return null;
        }
    }

    private void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            try (FileOutputStream fout = new FileOutputStream(exFixtureFile)) {

                exampleFixture.store(
                        fout,
                        "This is an example fixture. Update the "
                                + "values and remove the .example suffix to enable the test");
                fout.flush();
            }
            // System.out.println("Wrote example fixture file to " + exFixtureFile);
        } catch (IOException ioe) {
            // System.out.println("Unable to write out example fixture " + exFixtureFile);
            java.util.logging.Logger.getGlobal().log(java.util.logging.Level.INFO, "", ioe);
        }
    }

    File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    protected abstract JDBCTestSetup createTestSetup();

    protected void setFormValues(FormTester ft, String datastoreName) {
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", datastoreName);
        tester.executeAjaxEvent(
                "dataStoreForm:dataStoreNamePanel:border:border_body:paramValue", "change");
        ft.select("parametersPanel:postgisdatastore", 0);
        tester.executeAjaxEvent("dataStoreForm:parametersPanel:postgisdatastore", "click");
        ft.select("parametersPanel:rootentities:border:border_body:paramValue", 0);
        tester.executeAjaxEvent(
                "dataStoreForm:parametersPanel:rootentities:border:border_body:paramValue",
                "change");
    }

    private void setupGeoServerTestData() {
        // insert workspace with defined prefix into geoserver
        Catalog catalog = getCatalog();
        // use default workspace
        WorkspaceInfo workspace = catalog.getDefaultWorkspace();
        DataStoreInfo storeInfo = catalog.getDataStoreByName(workspace, SIMPLE_DATA_STORE_NAME);
        if (storeInfo == null) {
            storeInfo = catalog.getFactory().createDataStore();
            storeInfo.setWorkspace(workspace);
            PostgisNGDataStoreFactory storeFactory = new PostgisNGDataStoreFactory();
            storeInfo.setName(SIMPLE_DATA_STORE_NAME);
            storeInfo.setType(storeFactory.getDisplayName());
            storeInfo.setDescription(storeFactory.getDescription());
            Map<String, Serializable> params = storeInfo.getConnectionParameters();
            setUpParameters(params);
            catalog.save(storeInfo);
            this.dataStore = storeInfo;
        }
    }

    @Test
    public void testPageElements() {
        startPage();
        tester.assertLabel("dataStoreForm:storeType", dataStoreFactory.getDisplayName());
        tester.assertLabel("dataStoreForm:storeTypeDescription", dataStoreFactory.getDescription());
        tester.assertComponent("dataStoreForm:workspacePanel", WorkspacePanel.class);
        tester.assertLabel("dataStoreForm:dataStoreNamePanel:paramName", "Data Source Name *");
        tester.assertComponent(
                "dataStoreForm:dataStoreNamePanel:border:border_body:paramValue", TextField.class);
        String datastoreName = "smart-store";
        // set some form values to make all the elements rendered
        setFormValues(tester.newFormTester("dataStoreForm"), datastoreName);
        tester.assertLabel("dataStoreForm:parametersPanel:dataStoreName", "Data store name *");
        tester.assertComponent(
                "dataStoreForm:parametersPanel:postgisdatastore", DropDownChoice.class);
        tester.assertLabel("dataStoreForm:parametersPanel:rootentities:paramName", "Root entity *");
        tester.debugComponentTrees();
        tester.assertComponent(
                "dataStoreForm:parametersPanel:rootentities:border:border_body:paramValue",
                DropDownChoice.class);
        tester.assertComponent(
                "dataStoreForm:parametersPanel:domainmodel:paramValue:subtree:branches:1:node:content:checkbox",
                AjaxCheckBox.class);
        logout();
    }

    @Test
    public void testNewDataStoreSave() {
        startPage();
        FormTester ft = tester.newFormTester("dataStoreForm");
        String datastoreName = "smart-store";
        setFormValues(ft, datastoreName);
        ft.submit("save");
        tester.assertNoErrorMessage();
        logout();
    }

    protected Map<String, Serializable> setUpParameters(Map<String, Serializable> params) {
        JdbcUrlSplitter jdbcUrl = new JdbcUrlSplitter(fixture.getProperty("url"));
        params.put(HOST.key, jdbcUrl.host);
        params.put(DATABASE.key, jdbcUrl.database);
        String port = jdbcUrl.port != null ? jdbcUrl.port : fixture.getProperty("port");
        params.put(PORT.key, port);
        params.put(USER.key, fixture.getProperty(USER.key));
        params.put(PASSWD.key, fixture.getProperty("passwd"));
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, ONLINE_DB_SCHEMA);
        params.put(SSL_MODE.key, SslMode.DISABLE);
        params.put(DBTYPE.key, fixture.getProperty("dbtype"));
        return params;
    }
}
