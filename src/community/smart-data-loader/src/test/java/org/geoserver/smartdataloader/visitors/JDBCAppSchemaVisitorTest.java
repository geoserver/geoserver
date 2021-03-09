package org.geoserver.smartdataloader.visitors;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.sql.DatabaseMetaData;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.visitors.appschema.AppSchemaVisitor;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests related to SmartAppSchemaDomainModelVisitor */
public abstract class JDBCAppSchemaVisitorTest extends AbstractJDBCSmartDataLoaderTestSupport {

    public JDBCAppSchemaVisitorTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
        NAMESPACE_PREFIX = "mt";
        TARGET_NAMESPACE = "http://www.geo-solutions.it/smartappschema/1.0";
    }

    @Test
    public void testObservationsRootEntity() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_observations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        AppSchemaVisitor dmv =
                new AppSchemaVisitor(
                        NAMESPACE_PREFIX, TARGET_NAMESPACE, "meteo-observations-gml.xsd");
        dm.accept(dmv);

        try (InputStream is =
                JDBCAppSchemaVisitorTest.class.getResourceAsStream(
                        "meteo-observations-appschema.xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);

            // clean sourceDataStores nodes from control and dmv doc to allow assertion based on xml
            // comparision
            removeSourceDataStoresNode(control);
            removeSourceDataStoresNode(dmv.getDocument());

            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertEquals(true, d.similar());
        }
    }

    @Test
    public void testStationsRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        AppSchemaVisitor dmv =
                new AppSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE, "meteo-stations-gml.xsd");
        dm.accept(dmv);

        try (InputStream is =
                JDBCAppSchemaVisitorTest.class.getResourceAsStream(
                        "meteo-stations-appschema.xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);
            // clean sourceDataStores nodes from control and dmv doc to allow assertion based on xml
            // comparision
            removeSourceDataStoresNode(control);
            removeSourceDataStoresNode(dmv.getDocument());

            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertEquals(true, d.similar());
        }
    }

    @Test
    public void testParametersRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_parameters");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        AppSchemaVisitor dmv =
                new AppSchemaVisitor(
                        NAMESPACE_PREFIX, TARGET_NAMESPACE, "meteo-parameters-gml.xsd");
        dm.accept(dmv);

        try (InputStream is =
                JDBCAppSchemaVisitorTest.class.getResourceAsStream(
                        "meteo-parameters-appschema.xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);

            // clean sourceDataStores nodes from control and dmv doc to allow assertion based on xml
            // comparision
            removeSourceDataStoresNode(control);
            removeSourceDataStoresNode(dmv.getDocument());

            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertEquals(true, d.similar());
        }
    }

    @Test
    public void testMaintainersRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_maintainers");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        AppSchemaVisitor dmv =
                new AppSchemaVisitor(
                        NAMESPACE_PREFIX, TARGET_NAMESPACE, "meteo-maintainers-gml.xsd");
        dm.accept(dmv);

        try (InputStream is =
                JDBCAppSchemaVisitorTest.class.getResourceAsStream(
                        "meteo-maintainers-appschema.xml")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);

            // clean sourceDataStores nodes from control and dmv doc to allow assertion based on xml
            // comparision
            removeSourceDataStoresNode(control);
            removeSourceDataStoresNode(dmv.getDocument());

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertEquals(true, d.similar());
        }
    }
}
