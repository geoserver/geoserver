package org.geoserver.data.geogit;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RefDatabase;
import org.geogit.test.RepositoryTestCase;
import org.geotools.data.SchemaNotFoundException;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

public class GeoGitDataStoreTest extends RepositoryTestCase {

    private GeoGitDataStore dataStore;

    @Override
    protected void setUpInternal() throws Exception {
        dataStore = new GeoGitDataStore(repo);
    }

    public void testCreateSchema() throws IOException {
        final RefDatabase refDatabase = repo.getRefDatabase();
        final Ref initialTypesTreeRef = refDatabase.getRef(GeoGitDataStore.TYPE_NAMES_REF_TREE);
        assertNotNull(initialTypesTreeRef);

        final SimpleFeatureType featureType = super.linesType;
        dataStore.createSchema(featureType);
        assertTypeRefs(Collections.singleton(super.linesType));

        dataStore.createSchema(super.pointsType);

        Set<SimpleFeatureType> expected = new HashSet<SimpleFeatureType>();
        expected.add(super.linesType);
        expected.add(super.pointsType);
        assertTypeRefs(expected);

        try {
            dataStore.createSchema(super.pointsType);
            fail("Expected IOException on existing type");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }

    }

    private void assertTypeRefs(Set<SimpleFeatureType> expectedTypes) throws IOException {
        final RefDatabase refDatabase = repo.getRefDatabase();

        for (SimpleFeatureType featureType : expectedTypes) {
            final Name typeName = featureType.getName();
            final Ref typesTreeRef = refDatabase.getRef(GeoGitDataStore.TYPE_NAMES_REF_TREE);
            assertNotNull(typesTreeRef);

            RevTree typesTree = repo.getTree(typesTreeRef.getObjectId());
            List<String> path = Arrays.asList(typeName.getNamespaceURI(), typeName.getLocalPart());
            ObjectDatabase objectDatabase = repo.getObjectDatabase();

            Ref typeRef = objectDatabase.getTreeChild(typesTree, path);
            assertNotNull(typeRef);
            assertEquals(TYPE.BLOB, typeRef.getType());

            SimpleFeatureType readType = objectDatabase.get(typeRef.getObjectId(),
                    new SimpleFeatureTypeReader(featureType.getName()));

            assertEquals(featureType, readType);

        }
    }

    public void testGetNames() throws IOException {

        assertEquals(0, dataStore.getNames().size());

        dataStore.createSchema(super.linesType);
        assertEquals(1, dataStore.getNames().size());

        dataStore.createSchema(super.pointsType);
        assertEquals(2, dataStore.getNames().size());

        assertTrue(dataStore.getNames().contains(RepositoryTestCase.linesTypeName));
        assertTrue(dataStore.getNames().contains(RepositoryTestCase.pointsTypeName));
    }

    public void testGetTypeNames() throws IOException {

        assertEquals(0, dataStore.getTypeNames().length);

        dataStore.createSchema(super.linesType);
        assertEquals(1, dataStore.getTypeNames().length);

        dataStore.createSchema(super.pointsType);
        assertEquals(2, dataStore.getTypeNames().length);

        List<String> simpleNames = Arrays.asList(dataStore.getTypeNames());

        assertTrue(simpleNames.contains(RepositoryTestCase.linesName));
        assertTrue(simpleNames.contains(RepositoryTestCase.pointsName));
    }

    public void testGetSchemaName() throws IOException {
        try {
            dataStore.getSchema(RepositoryTestCase.linesTypeName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.linesType);
        SimpleFeatureType lines = dataStore.getSchema(RepositoryTestCase.linesTypeName);
        assertEquals(super.linesType, lines);

        try {
            dataStore.getSchema(RepositoryTestCase.pointsTypeName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.pointsType);
        SimpleFeatureType points = dataStore.getSchema(RepositoryTestCase.pointsTypeName);
        assertEquals(super.pointsType, points);
    }

    public void testGetSchemaString() throws IOException {
        try {
            dataStore.getSchema(RepositoryTestCase.linesName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.linesType);
        SimpleFeatureType lines = dataStore.getSchema(RepositoryTestCase.linesName);
        assertEquals(super.linesType, lines);

        try {
            dataStore.getSchema(RepositoryTestCase.pointsName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.pointsType);
        SimpleFeatureType points = dataStore.getSchema(RepositoryTestCase.pointsName);
        assertEquals(super.pointsType, points);
    }

    public void testGetFeatureSourceName() throws IOException {
        try {
            dataStore.getFeatureSource(RepositoryTestCase.linesTypeName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        SimpleFeatureSource source;

        dataStore.createSchema(super.linesType);
        source = dataStore.getFeatureSource(RepositoryTestCase.linesTypeName);
        assertTrue(source instanceof GeoGitFeatureSource);

        try {
            dataStore.getFeatureSource(RepositoryTestCase.pointsTypeName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.pointsType);
        source = dataStore.getFeatureSource(RepositoryTestCase.pointsTypeName);
        assertTrue(source instanceof GeoGitFeatureSource);
    }

    public void testGetFeatureSourceString() throws IOException {
        try {
            dataStore.getFeatureSource(RepositoryTestCase.linesName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        SimpleFeatureSource source;

        dataStore.createSchema(super.linesType);
        source = dataStore.getFeatureSource(RepositoryTestCase.linesName);
        assertTrue(source instanceof GeoGitFeatureSource);

        try {
            dataStore.getFeatureSource(RepositoryTestCase.pointsName);
            fail("Expected SchemaNotFoundException");
        } catch (SchemaNotFoundException e) {
            assertTrue(true);
        }

        dataStore.createSchema(super.pointsType);
        source = dataStore.getFeatureSource(RepositoryTestCase.pointsName);
        assertTrue(source instanceof GeoGitFeatureSource);
    }

}
