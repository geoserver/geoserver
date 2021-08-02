package org.geoserver.smartdataloader.metadata.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.DatabaseMetaData;
import java.util.List;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;
import org.junit.Test;

/** Tests in Smart AppSchema related to use of a DataStoreMetadata linked to a JDBC connection. */
public abstract class JDBCDataStoreMetadataTest extends AbstractJDBCSmartDataLoaderTestSupport {

    public JDBCDataStoreMetadataTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Test
    public void testJdbcDataStoreMetadataLoad() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dm = getDataStoreMetadata(metaData);
        List<EntityMetadata> entities = dm.getDataStoreEntities();

        List<RelationMetadata> relations = dm.getDataStoreRelations();

        assertEquals(5, entities.size());
        assertEquals(8, relations.size());

        metaData.getConnection().close();
    }

    @Test
    public void testMeteoObservationsEntityAttributes() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        EntityMetadata entity =
                new JdbcTableMetadata(
                        metaData.getConnection(), null, ONLINE_DB_SCHEMA, "meteo_observations");

        assertEquals(6, entity.getAttributes().size());

        metaData.getConnection().close();
    }

    @Test
    public void testMeteoObservationsEntityRelations() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        EntityMetadata entity =
                new JdbcTableMetadata(
                        metaData.getConnection(), null, ONLINE_DB_SCHEMA, "meteo_observations");
        assertEquals(4, entity.getRelations().size());

        metaData.getConnection().close();
    }
}
