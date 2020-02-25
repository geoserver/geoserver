/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.*;
import java.util.List;
import net.opengis.wfs20.StoredQueryDescriptionType;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StoredQueryProviderTest {

    public static final String MY_STORED_QUERY = "MyStoredQuery";

    @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

    public static final String MY_STORED_QUERY_DEFINITION =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<wfs:StoredQueryDescription id='MyStoredQuery'"
                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                    + " xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                    + " xmlns:gml=\"http://www.opengis.net/gml/3.2\""
                    + " xmlns:wfs=\"http://www.opengis.net/wfs/2.0\""
                    + " xmlns:fes=\"http://www.opengis.net/fes/2.0\">>\n"
                    + "  <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/>\n"
                    + "  <wfs:QueryExpressionText\n"
                    + "   returnFeatureTypes='topp:states'\n"
                    + "   language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression'\n"
                    + "   isPrivate='false'>\n"
                    + "    <wfs:Query typeNames='topp:states'>\n"
                    + "      <fes:Filter>\n"
                    + "        <fes:Within>\n"
                    + "          <fes:ValueReference>the_geom</fes:ValueReference>\n"
                    + "           ${AreaOfInterest}\n"
                    + "        </fes:Within>\n"
                    + "      </fes:Filter>\n"
                    + "    </wfs:Query>\n"
                    + "  </wfs:QueryExpressionText>\n"
                    + "</wfs:StoredQueryDescription>";

    private StoredQueryProvider storedQueryProvider;

    private File baseDirectory;

    private Catalog catalog;

    private GeoServerResourceLoader loader;

    @Before
    public void setup() throws IOException {
        baseDirectory = tmpFolder.newFolder();
        catalog = createMock(Catalog.class);
        loader = new GeoServerResourceLoader(baseDirectory);
        expect(catalog.getResourceLoader()).andReturn(loader);
        replay(catalog);
        storedQueryProvider = new StoredQueryProvider(catalog, new WFSInfoImpl(), false);
    }

    @Test
    public void whenNoStoredQueriesDefinedAGetFeatureByIdQueryIsReturned() {
        List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
        assertThat(queries, hasSize(1));
        assertThat(
                queries.get(0).getName(), is(equalTo("urn:ogc:def:query:OGC-WFS::GetFeatureById")));
        assertThat(
                storedQueryProvider.getStoredQuery("urn:ogc:def:query:OGC-WFS::GetFeatureById"),
                is(notNullValue()));
    }

    @Test
    public void whenBogusStoredQueryDefinitionCreatedItIsNotReturnedInTheListOfStoredQueries()
            throws IOException {
        createMyStoredQueryDefinitionFile(storedQueryProvider.storedQueryDir().dir());
        createMyBogusStoredQueryDefinition();
        List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
        assertThat(queries, hasSize(2));
        assertThat(
                storedQueryProvider.getStoredQuery("urn:ogc:def:query:OGC-WFS::GetFeatureById"),
                is(notNullValue()));
        assertThat(storedQueryProvider.getStoredQuery(MY_STORED_QUERY), is(notNullValue()));
    }

    @Test
    public void whenStoredQueryDefinitionCreatedByFileItIsReturnedInTheListOfStoredQueries()
            throws IOException {
        createMyStoredQueryDefinitionFile(storedQueryProvider.storedQueryDir().dir());
        List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
        assertThat(queries, hasSize(2));
        assertThat(
                storedQueryProvider.getStoredQuery("urn:ogc:def:query:OGC-WFS::GetFeatureById"),
                is(notNullValue()));
        assertThat(
                storedQueryProvider.getStoredQuery(MY_STORED_QUERY).getName(), is(MY_STORED_QUERY));
    }

    @Test
    public void whenStoredQueryDefinitionCreatedByDescriptionItIsReturnedInTheListOfStoredQueries()
            throws Exception {
        StoredQueryDescriptionType storedQueryDescriptionType =
                createMyStoredQueryDefinitionInStoredQueryDescriptionType();
        StoredQuery result = storedQueryProvider.createStoredQuery(storedQueryDescriptionType);
        assertThat(result.getName(), is(MY_STORED_QUERY));
        assertThat(
                storedQueryProvider.getStoredQuery(MY_STORED_QUERY).getName(), is(MY_STORED_QUERY));
    }

    @Test
    public void storedQueryDefinitionIsNotRewrittenByListingTheQueries() throws IOException {
        // c.f. GEOS-7297
        File myStoredQueryDefinition =
                createMyStoredQueryDefinitionFile(storedQueryProvider.storedQueryDir().dir());
        try {
            myStoredQueryDefinition.setReadOnly();
            List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
            assertThat(queries, hasSize(2));
            assertThat(
                    storedQueryProvider.getStoredQuery("urn:ogc:def:query:OGC-WFS::GetFeatureById"),
                    is(notNullValue()));
            assertThat(
                    storedQueryProvider.getStoredQuery(MY_STORED_QUERY).getName(),
                    is(MY_STORED_QUERY));
        } finally {
            myStoredQueryDefinition.setWritable(true);
        }
    }

    @Test
    public void canRemoveStoredQueryDefinition() throws IOException {
        File myStoredQueryDefinition =
                createMyStoredQueryDefinitionFile(storedQueryProvider.storedQueryDir().dir());
        List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
        assertThat(queries, hasSize(2));
        StoredQuery myStoredQuery = storedQueryProvider.getStoredQuery(MY_STORED_QUERY);
        assertThat(myStoredQuery.getName(), is(MY_STORED_QUERY));
        storedQueryProvider.removeStoredQuery(myStoredQuery);
        assertThat(myStoredQueryDefinition.exists(), is(false));
        assertThat(storedQueryProvider.getStoredQuery(myStoredQuery.getName()), is(nullValue()));
    }

    @Test
    public void canRemoveAllStoredQueryDefinitions() throws IOException {
        File myStoredQueryDefinition =
                createMyStoredQueryDefinitionFile(storedQueryProvider.storedQueryDir().dir());
        List<StoredQuery> queries = storedQueryProvider.listStoredQueries();
        assertThat(queries, hasSize(2));
        storedQueryProvider.removeAll();
        assertThat(myStoredQueryDefinition.exists(), is(false));
        assertThat(storedQueryProvider.getStoredQuery(MY_STORED_QUERY), is(nullValue()));
    }

    @Test
    public void testGetLanguage() {
        assertThat(storedQueryProvider.getLanguage(), is(equalTo(StoredQueryProvider.LANGUAGE_20)));
    }

    private File createMyStoredQueryDefinitionFile(File storedQueryDir) throws IOException {
        File storedQueryDefinition = new File(storedQueryDir, MY_STORED_QUERY + ".xml");
        Writer writer = new FileWriter(storedQueryDefinition);
        writer.write(MY_STORED_QUERY_DEFINITION);
        writer.close();
        return storedQueryDefinition;
    }

    private StoredQueryDescriptionType createMyStoredQueryDefinitionInStoredQueryDescriptionType()
            throws Exception {
        Parser p = new Parser(new WFSConfiguration());
        p.setRootElementType(WFS.StoredQueryDescriptionType);
        StringReader reader = new StringReader(MY_STORED_QUERY_DEFINITION);
        try {

            return (StoredQueryDescriptionType) p.parse(reader);
        } finally {
            reader.close();
        }
    }

    private File createMyBogusStoredQueryDefinition() throws IOException {
        File storedQueryDir = storedQueryProvider.storedQueryDir().dir();
        File storedQueryDefinition = new File(storedQueryDir, "MyBogusStoredQuery.xml");
        Writer writer = new FileWriter(storedQueryDefinition);
        writer.write("This is not a well-formed query");
        writer.close();
        return storedQueryDefinition;
    }
}
