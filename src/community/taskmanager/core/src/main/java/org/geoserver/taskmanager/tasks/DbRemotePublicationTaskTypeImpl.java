/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.SqlUtil;
import org.springframework.stereotype.Component;

@Component
public class DbRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteDbPublication";

    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_TABLE_NAME = "table-name";

    @PostConstruct
    @Override
    public void initParamInfo() {
        super.initParamInfo();
        ParameterInfo dbInfo = new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true);
        paramInfo.put(PARAM_DB_NAME, dbInfo);
        paramInfo.put(
                PARAM_TABLE_NAME,
                new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName, false).dependsOn(dbInfo));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean createStore(
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            StoreInfo store,
            TaskContext ctx,
            String name)
            throws IOException, TaskException {
        try {
            final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
            final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
            final ExternalGS gs = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
            return restManager
                    .getStoreManager()
                    .create(
                            store.getWorkspace().getName(),
                            db.postProcess(db.getStoreEncoder(name, gs), table));
        } catch (UnsupportedOperationException e) {
            throw new TaskException(
                    "Failed to create store "
                            + store.getWorkspace().getName()
                            + ":"
                            + store.getName(),
                    e);
        }
    }

    @Override
    protected String getStoreName(StoreInfo store, TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
        final String schema = table == null ? null : SqlUtil.schema(table.getTableName());
        return schema == null ? db.getName() : (db.getName() + "_" + schema);
    }

    @Override
    protected boolean neverReuseStore() {
        return false;
    }

    @Override
    protected void postProcess(
            StoreType storeType,
            ResourceInfo resource,
            GSResourceEncoder re,
            TaskContext ctx,
            Finalizer finalizer)
            throws TaskException {
        final DbTable originalTable = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
        if (originalTable != null) {
            final DbTable table =
                    (DbTable)
                            ctx.getBatchContext()
                                    .get(
                                            originalTable,
                                            new BatchContext.Dependency() {
                                                @Override
                                                public void revert() throws TaskException {
                                                    if (resource.getMetadata()
                                                            .containsKey(
                                                                    FeatureTypeInfo
                                                                            .JDBC_VIRTUAL_TABLE)) {
                                                        // virtual table, resource must be attached
                                                        // to
                                                        // SQL query
                                                        // in metadata, rather than just table name
                                                        finalizer
                                                                .getEncoder()
                                                                .setNativeName(
                                                                        resource.getNativeName());
                                                    } else {
                                                        DbTable table =
                                                                (DbTable)
                                                                        ctx.getBatchContext()
                                                                                .get(
                                                                                        ctx.getParameterValues()
                                                                                                .get(
                                                                                                        PARAM_TABLE_NAME));
                                                        finalizer
                                                                .getEncoder()
                                                                .setNativeName(
                                                                        SqlUtil.notQualified(
                                                                                table
                                                                                        .getTableName()));
                                                    }
                                                    finalizer.run();
                                                }
                                            });
            finalizer.setFinalizeAtCommit(table != null && table.equals(originalTable));

            ((GSFeatureTypeEncoder) re).setNativeName(SqlUtil.notQualified(table.getTableName()));
        }
    }
}
