/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.BatchContext.Dependency;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.PlaceHolderUtil;
import org.geoserver.taskmanager.util.SqlUtil;
import org.opengis.feature.type.Name;
import org.springframework.stereotype.Component;

@Component
public class AppSchemaLocalPublicationTaskTypeImpl extends FileLocalPublicationTaskTypeImpl {

    public static final String NAME = "LocalAppSchemaPublication";

    public static final String PARAM_DB = "database";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    @PostConstruct
    public void initParamInfo() {
        super.initParamInfo();
        paramInfo.put(PARAM_DB, new ParameterInfo(PARAM_DB, extTypes.dbName, true));
    }

    private PlaceHolderUtil.ObjectTransform getTableTransform(
            DbSource db, TaskContext ctx, String content, Resource res) throws TaskException {
        final Name layerName = (Name) ctx.getParameterValues().get(PARAM_LAYER);

        return tableName ->
                SqlUtil.notQualified(
                        ((DbTableImpl)
                                        ctx.getBatchContext()
                                                .get(
                                                        new DbTableImpl(db, tableName),
                                                        new Dependency() {
                                                            @Override
                                                            public void revert()
                                                                    throws TaskException {
                                                                String newContent =
                                                                        PlaceHolderUtil
                                                                                .replaceObjectPlaceHolder(
                                                                                        content,
                                                                                        tableName ->
                                                                                                SqlUtil
                                                                                                        .notQualified(
                                                                                                                tableName));
                                                                try (OutputStream os = res.out()) {
                                                                    os.write(newContent.getBytes());
                                                                } catch (IOException e) {
                                                                    throw new TaskException(e);
                                                                }
                                                                DataStoreInfo store =
                                                                        catalog.getStoreByName(
                                                                                catalog.getNamespaceByURI(
                                                                                                layerName
                                                                                                        .getNamespaceURI())
                                                                                        .getName(),
                                                                                layerName
                                                                                        .getLocalPart(),
                                                                                DataStoreInfo
                                                                                        .class);
                                                                if (store != null) {
                                                                    catalog.getResourcePool()
                                                                            .clear(store);
                                                                }
                                                            }
                                                        }))
                                .getTableName());
    }

    @Override
    protected URI process(URI uri, TaskContext ctx) throws TaskException {

        if (!"file".equals(uri.getScheme()) && !"resource".equals(uri.getScheme())) {
            throw new TaskException("Mapping files must be local!");
        }
        String path = uri.getSchemeSpecificPart();
        if ("resource".equals(uri.getScheme()) && path.startsWith("/")) {
            path = path.substring(1);
        }
        String newPath;
        try {
            if (path.toUpperCase().endsWith("ZIP")) {
                newPath = processZip(path, ctx);
            } else {
                newPath = processSingle(path, ctx);
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }

        try {
            return new URI("file:" + newPath);
        } catch (URISyntaxException e) {
            throw new TaskException(e);
        }
    }

    private String processZip(String path, TaskContext ctx) throws IOException, TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB);

        try (ZipInputStream is = new ZipInputStream(Resources.fromPath(path).in())) {
            for (ZipEntry entry = is.getNextEntry(); entry != null; entry = is.getNextEntry()) {
                Resource res =
                        Resources.fromPath(
                                FilenameUtils.getPath(path) + "local/" + entry.getName());

                String template = IOUtils.toString(is, "UTF-8");
                String pub = PlaceHolderUtil.replacePlaceHolders(template, db.getParameters());
                pub =
                        PlaceHolderUtil.replaceObjectPlaceHolder(
                                pub, getTableTransform(db, ctx, pub, res));

                try (OutputStream os = res.out()) {
                    os.write(pub.getBytes());
                }
            }
        }
        String newPath =
                FilenameUtils.getPath(path) + "local/" + FilenameUtils.getBaseName(path) + ".xml";

        if (!Resources.exists(Resources.fromPath(newPath))) {
            throw new TaskException("Zip file must include xml file with same name.");
        }

        return newPath;
    }

    private String processSingle(String path, TaskContext ctx) throws IOException, TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB);

        String newPath = FilenameUtils.getPath(path) + "local/" + FilenameUtils.getName(path);
        Resource res = Resources.fromPath(newPath);

        try (InputStream is = Resources.fromPath(path).in()) {
            String template = IOUtils.toString(is, "UTF-8");
            String pub = PlaceHolderUtil.replacePlaceHolders(template, db.getParameters());
            pub =
                    PlaceHolderUtil.replaceObjectPlaceHolder(
                            pub, getTableTransform(db, ctx, pub, res));

            try (OutputStream os = res.out()) {
                os.write(pub.getBytes());
            }
        }

        return newPath;
    }
}
