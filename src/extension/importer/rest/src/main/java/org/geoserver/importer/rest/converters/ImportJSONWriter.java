/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.LogRecord;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONBuilder;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersister.Callback;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.Table;
import org.geoserver.importer.mosaic.Granule;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.importer.rest.ImportWrapper;
import org.geoserver.importer.transform.AttributeComputeTransform;
import org.geoserver.importer.transform.AttributeRemapTransform;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.CreateIndexTransform;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.GdalAddoTransform;
import org.geoserver.importer.transform.GdalTranslateTransform;
import org.geoserver.importer.transform.GdalWarpTransform;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.PostScriptTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * {@link BaseMessageConverter} implementation for writing JSON or HTML responses from {@link
 * ImportContext}, {@link ImportTask} or {@link ImportWrapper} objects.
 */
@Component
public class ImportJSONWriter {

    Importer importer;

    private Callback callback;

    @Autowired
    public ImportJSONWriter(Importer importer) {
        this.importer = importer;
    }

    /**
     * Determines the number of levels to expand the JSON result, by parsing the "expand" parameter
     * from the query map.
     *
     * @param def The default value to fall back on
     */
    public int expand(int def) {
        String ex = null;
        Map<String, String[]> queryMap = RequestInfo.get().getQueryMap();
        if (queryMap != null) {
            String[] params = queryMap.get("expand");
            if (params != null && params.length > 0) {
                ex = params[0];
            }
        }
        return expand(ex, def);
    }

    /**
     * Determines the number of levels to expand the JSON result
     *
     * @param expand The value of the "expand" parameter
     * @param def The default value to fall back on
     */
    public int expand(String expand, int def) {
        if (expand == null) {
            return def;
        }

        try {
            return "self".equalsIgnoreCase(expand)
                    ? 1
                    : "all".equalsIgnoreCase(expand)
                            ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(expand) ? 0 : Integer.parseInt(expand);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void contexts(FlushableJSONBuilder json, Iterator<ImportContext> contexts, int expand)
            throws IOException {
        json.object().key("imports").array();
        while (contexts.hasNext()) {
            ImportContext context = contexts.next();
            context(json, context, false, expand);
        }
        json.endArray().endObject();
        json.flush();
    }

    public void context(FlushableJSONBuilder json, ImportContext context, boolean top, int expand)
            throws IOException {
        if (top) {
            json.object().key("import");
        }
        json.object();
        json.key("id").value(context.getId());
        json.key("href").value(RequestInfo.get().servletURI(pathTo(context)));
        json.key("state").value(context.getState());
        if (context.getMessage() != null) {
            json.key("message").value(context.getMessage());
        }

        if (expand > 0) {
            json.key("archive").value(context.isArchive());
            if (context.getTargetWorkspace() != null) {
                json.key("targetWorkspace").value(toJSON(context.getTargetWorkspace()));
            }
            if (context.getTargetStore() != null) {
                json.key("targetStore");
                store(json, context.getTargetStore(), null, false, expand - 1);
                // value(toJSON(context.getTargetStore()));
            }

            if (context.getData() != null) {
                json.key("data");
                data(json, context.getData(), context, expand - 1);
            }
            if (!context.getDefaultTransforms().isEmpty()) {
                transforms(json, context, expand - 1, context.getDefaultTransforms());
            }
            tasks(json, context.getTasks(), false, expand - 1);
        }

        json.endObject();

        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void tasks(FlushableJSONBuilder json, List<ImportTask> tasks, boolean top, int expand)
            throws IOException {

        if (top) {
            json.object();
        }

        json.key("tasks").array();
        for (ImportTask task : tasks) {
            task(json, task, false, expand);
        }
        json.endArray();

        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void task(FlushableJSONBuilder json, ImportTask task, boolean top, int expand)
            throws IOException {

        long id = task.getId();
        String href = RequestInfo.get().servletURI(pathTo(task));
        if (top) {
            json.object().key("task");
        }
        json.object();
        json.key("id").value(id);
        json.key("href").value(href);
        json.key("state").value(task.getState());

        if (expand > 0) {
            json.key("updateMode").value(task.getUpdateMode().name());

            // data (used to be source)
            ImportData data = task.getData();
            if (data != null) {
                json.key("data");
                data(json, data, task, expand - 1);
            }

            // target
            StoreInfo store = task.getStore();
            if (store != null) {
                json.key("target");
                store(json, store, task, false, expand - 1);
            }

            json.key("progress").value(href + "/progress");

            LayerInfo layer = task.getLayer();
            if (layer != null) {
                // @todo don't know why catalog isn't set here, thought this was set during load
                // from BDBImportStore
                layer.getResource().setCatalog(importer.getCatalog());

                json.key("layer");
                layer(json, task, false, expand - 1);
            }

            if (task.getError() != null) {
                json.key("errorMessage").value(concatErrorMessages(task.getError()));
            }

            transformChain(json, task, false, expand - 1);
            messages(json, task.getMessages());
        }

        json.endObject();
        if (top) {
            json.endObject();
        }

        json.flush();
    }

    public void store(
            FlushableJSONBuilder json, StoreInfo store, ImportTask task, boolean top, int expand)
            throws IOException {

        String type =
                store instanceof DataStoreInfo
                        ? "dataStore"
                        : store instanceof CoverageStoreInfo ? "coverageStore" : "store";

        json.object();
        if (task != null) {
            json.key("href").value(RequestInfo.get().servletURI(pathTo(task) + "/target"));
        }

        if (expand > 0) {
            JSONObject obj = toJSON(store);
            json.key(type).value(obj.get(type));
        } else {
            json.key(type)
                    .object()
                    .key("name")
                    .value(store.getName())
                    .key("type")
                    .value(store.getType())
                    .endObject();
        }

        json.endObject();
        json.flush();
    }

    public void layer(FlushableJSONBuilder json, ImportTask task, boolean top, int expand)
            throws IOException {

        if (top) {
            json.object().key("layer");
        }

        LayerInfo layer = task.getLayer();
        ResourceInfo r = layer.getResource();

        json.object()
                .key("name")
                .value(layer.getName())
                .key("href")
                .value(RequestInfo.get().servletURI(pathTo(task) + "/layer"));

        if (expand > 0) {
            if (r.getTitle() != null) {
                json.key("title").value(r.getTitle());
            }
            if (r.getAbstract() != null) {
                json.key("abstract").value(r.getAbstract());
            }
            if (r.getDescription() != null) {
                json.key("description").value(r.getDescription());
            }
            json.key("originalName").value(task.getOriginalLayerName());
            if (r != null) {
                json.key("nativeName").value(r.getNativeName());

                if (r.getSRS() != null) {
                    json.key("srs").value(r.getSRS());
                }
                if (r.getNativeBoundingBox() != null) {
                    json.key("bbox");
                    bbox(json, r.getNativeBoundingBox());
                }
            }
            if (r instanceof FeatureTypeInfo) {
                featureType(json, (FeatureTypeInfo) r);
            }
            StyleInfo s = layer.getDefaultStyle();
            if (s != null) {
                style(json, s, task, false, expand - 1);
            }
        }

        json.endObject();
        if (top) {
            json.endObject();
        }
        json.flush();
    }

    void featureType(FlushableJSONBuilder json, FeatureTypeInfo featureTypeInfo)
            throws IOException {
        json.key("attributes").array();
        List<AttributeTypeInfo> attributes = featureTypeInfo.attributes();
        for (int i = 0; i < attributes.size(); i++) {
            AttributeTypeInfo att = attributes.get(i);
            json.object();
            json.key("name").value(att.getName());
            json.key("binding").value(att.getBinding().getName());
            json.endObject();
        }
        json.endArray();
    }

    void style(FlushableJSONBuilder json, StyleInfo style, ImportTask task, boolean top, int expand)
            throws IOException {

        if (top) {
            json.object();
        }

        String href = RequestInfo.get().servletURI(pathTo(task) + "/layer/style");

        json.key("style");
        if (expand > 0) {
            JSONObject obj = toJSON(style).getJSONObject("style");
            obj.put("href", href);
            json.value(obj);
        } else {
            json.object();
            json.key("name").value(style.getName());
            json.key("href").value(href);
            json.endObject();
        }

        if (top) {
            json.endObject();
        }
    }

    public void transformChain(FlushableJSONBuilder json, ImportTask task, boolean top, int expand)
            throws IOException {

        if (top) {
            json.object();
        }

        TransformChain<? extends ImportTransform> txChain = task.getTransform();

        json.key("transformChain").object();
        json.key("type").value(txChain instanceof VectorTransformChain ? "vector" : "raster");

        transforms(
                json,
                task,
                expand,
                txChain != null ? txChain.getTransforms() : new ArrayList<ImportTransform>());
        json.endObject();

        if (top) {
            json.endObject();
        }

        json.flush();
    }

    private void transforms(
            FlushableJSONBuilder json,
            Object parent,
            int expand,
            List<? extends ImportTransform> transforms)
            throws IOException {
        json.key("transforms").array();

        for (int i = 0; i < transforms.size(); i++) {
            transform(json, transforms.get(i), i, parent, false, expand);
        }

        json.endArray();
    }

    public void transform(
            FlushableJSONBuilder json,
            ImportTransform transform,
            int index,
            Object parent,
            boolean top,
            int expand)
            throws IOException {
        json.object();
        json.key("type").value(transform.getClass().getSimpleName());
        json.key("href")
                .value(RequestInfo.get().servletURI(pathTo(parent) + "/transforms/" + index));
        if (expand > 0) {
            if (transform instanceof DateFormatTransform) {
                DateFormatTransform df = (DateFormatTransform) transform;
                json.key("field").value(df.getField());
                if (df.getDatePattern() != null) {
                    json.key("format").value(df.getDatePattern().dateFormat().toPattern());
                }

                if (df.getEnddate() != null) {
                    json.key("enddate").value(df.getEnddate());
                }

                if (df.getPresentation() != null) {
                    json.key("presentation").value(df.getPresentation());
                }
            } else if (transform instanceof IntegerFieldToDateTransform) {
                IntegerFieldToDateTransform df = (IntegerFieldToDateTransform) transform;
                json.key("field").value(df.getField());
            } else if (transform instanceof CreateIndexTransform) {
                CreateIndexTransform df = (CreateIndexTransform) transform;
                json.key("field").value(df.getField());
            } else if (transform instanceof AttributeRemapTransform) {
                AttributeRemapTransform art = (AttributeRemapTransform) transform;
                json.key("field").value(art.getField());
                json.key("target").value(art.getType().getName());
            } else if (transform instanceof AttributeComputeTransform) {
                AttributeComputeTransform act = (AttributeComputeTransform) transform;
                json.key("field").value(act.getField());
                json.key("fieldType").value(act.getType().getName());
                json.key("cql").value(act.getCql());
            } else if (transform.getClass() == AttributesToPointGeometryTransform.class) {
                AttributesToPointGeometryTransform atpgt =
                        (AttributesToPointGeometryTransform) transform;
                json.key("latField").value(atpgt.getLatField());
                json.key("lngField").value(atpgt.getLngField());
            } else if (transform.getClass() == ReprojectTransform.class) {
                ReprojectTransform rt = (ReprojectTransform) transform;
                json.key("source").value(srs(rt.getSource()));
                json.key("target").value(srs(rt.getTarget()));
            } else if (transform.getClass().equals(GdalTranslateTransform.class)) {
                GdalTranslateTransform gtx = (GdalTranslateTransform) transform;
                List<String> options = gtx.getOptions();
                buildJsonOptions(json, "options", options);
            } else if (transform.getClass().equals(GdalWarpTransform.class)) {
                GdalWarpTransform gw = (GdalWarpTransform) transform;
                List<String> options = gw.getOptions();
                buildJsonOptions(json, "options", options);
            } else if (transform.getClass().equals(GdalAddoTransform.class)) {
                GdalAddoTransform gad = (GdalAddoTransform) transform;
                List<String> options = gad.getOptions();
                buildJsonOptions(json, "options", options);
                JSONBuilder arrayBuilder = json.key("levels").array();
                for (Integer level : gad.getLevels()) {
                    arrayBuilder.value(level);
                }
                arrayBuilder.endArray();
            } else if (transform.getClass().equals(PostScriptTransform.class)) {
                PostScriptTransform pst = (PostScriptTransform) transform;
                List<String> options = pst.getOptions();
                json.key("name").value(pst.getName());
                buildJsonOptions(json, "options", options);
            } else {
                throw new IOException(
                        "Serializaiton of " + transform.getClass() + " not implemented");
            }
        }
        json.endObject();
        json.flush();
    }

    private void buildJsonOptions(FlushableJSONBuilder json, String key, List<String> options) {
        if (options != null) {
            JSONBuilder arrayBuilder = json.key(key).array();
            for (String option : options) {
                arrayBuilder.value(option);
            }
            arrayBuilder.endArray();
        }
    }

    void bbox(JSONBuilder json, ReferencedEnvelope bbox) {
        json.object()
                .key("minx")
                .value(bbox.getMinX())
                .key("miny")
                .value(bbox.getMinY())
                .key("maxx")
                .value(bbox.getMaxX())
                .key("maxy")
                .value(bbox.getMaxY());

        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        if (crs != null) {
            json.key("crs").value(crs.toWKT());
        }

        json.endObject();
    }

    public void data(FlushableJSONBuilder json, ImportData data, Object parent, int expand)
            throws IOException {
        if (data instanceof FileData) {
            if (data instanceof Directory) {
                if (data instanceof Mosaic) {
                    mosaic(json, (Mosaic) data, parent, expand);
                } else {
                    directory(json, (Directory) data, parent, expand);
                }
            } else {
                file(json, (FileData) data, parent, expand, false);
            }
        } else if (data instanceof Database) {
            database(json, (Database) data, parent, expand);
        } else if (data instanceof Table) {
            table(json, (Table) data, parent, expand);
        } else if (data instanceof RemoteData) {
            remote(json, (RemoteData) data, parent, expand);
        } else {
            throw new IllegalArgumentException(
                    "Unable to serialize " + data.getClass().getSimpleName() + " as ImportData");
        }
        json.flush();
    }

    public void remote(FlushableJSONBuilder json, RemoteData data, Object parent, int expand)
            throws IOException {

        json.object();

        json.key("type").value("remote");
        json.key("location").value(data.getLocation());
        if (data.getUsername() != null) {
            json.key("username").value(data.getUsername());
        }
        if (data.getPassword() != null) {
            json.key("password").value(data.getPassword());
        }
        if (data.getDomain() != null) {
            json.key("domain").value(data.getDomain());
        }

        json.endObject();
        json.flush();
    }

    public void file(
            FlushableJSONBuilder json, FileData data, Object parent, int expand, boolean href)
            throws IOException {

        json.object();

        json.key("type").value("file");
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        if (href) {
            json.key("href").value(RequestInfo.get().servletURI(pathTo(data, parent)));
        }

        String location = null;
        try {
            location = data.getFile().getParentFile().getPath();
        } catch (Exception e) {
            location = "";
        }

        if (expand > 0) {
            json.key("location").value(location);
            if (data.getCharsetEncoding() != null) {
                json.key("charset").value(data.getCharsetEncoding());
            }
            fileContents(json, data, parent, expand);
            message(json, data);
        } else {
            json.key("file").value(data.getFile().getName());
        }

        json.endObject();
        json.flush();
    }

    void fileContents(FlushableJSONBuilder json, FileData data, Object parent, int expand)
            throws IOException {
        // TODO: we should probably url encode to handle spaces and other chars
        String filename = data.getFile().getName();
        json.key("file").value(filename);
        json.key("href")
                .value(RequestInfo.get().servletURI(pathTo(data, parent) + "/files/" + filename));
        if (expand > 0) {
            if (data instanceof SpatialFile) {
                SpatialFile sf = (SpatialFile) data;
                json.key("prj").value(sf.getPrjFile() != null ? sf.getPrjFile().getName() : null);
                json.key("other").array();
                for (File supp : ((SpatialFile) data).getSuppFiles()) {
                    json.value(supp.getName());
                }
                json.endArray();

                if (sf instanceof Granule) {
                    Granule g = (Granule) sf;
                    if (g.getTimestamp() != null) {
                        json.key("timestamp").value(getISODateFormat().format(g.getTimestamp()));
                    }
                }
            }
        }
    }

    public void mosaic(FlushableJSONBuilder json, Mosaic data, Object parent, int expand)
            throws IOException {
        directory(json, data, "mosaic", parent, expand);
    }

    public void directory(FlushableJSONBuilder json, Directory data, Object parent, int expand)
            throws IOException {
        directory(json, data, "directory", parent, expand);
    }

    public void directory(
            FlushableJSONBuilder json, Directory data, String typeName, Object parent, int expand)
            throws IOException {

        json.object();
        json.key("type").value(typeName);
        if (data.getFormat() != null) {
            json.key("format").value(data.getFormat().getName());
        }

        json.key("location").value(data.getFile().getPath());
        json.key("href").value(RequestInfo.get().servletURI(pathTo(data, parent)));

        if (expand > 0) {
            if (data.getCharsetEncoding() != null) {
                json.key("charset").value(data.getCharsetEncoding());
            }

            json.key("files");
            files(json, data, parent, false, expand - 1);
            message(json, data);
        }
        json.endObject();
        json.flush();
    }

    public void files(
            FlushableJSONBuilder json, Directory data, Object parent, boolean top, int expand)
            throws IOException {

        if (top) {
            json.object().key("files");
        }
        json.array();
        for (FileData file : data.getFiles()) {
            json.object();
            fileContents(json, file, parent, expand - 1);
            json.endObject();
        }
        json.endArray();
        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void database(FlushableJSONBuilder json, Database data, Object parent, int expand)
            throws IOException {
        json.object();
        json.key("type").value("database");
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        json.key("href").value(RequestInfo.get().servletURI(pathTo(data, parent)));

        if (expand > 0) {
            json.key("parameters").object();
            for (Map.Entry<String, Serializable> e : data.getParameters().entrySet()) {
                json.key(e.getKey()).value(e.getValue());
            }

            json.endObject();

            json.key("tables").array();
            for (Table t : data.getTables()) {
                json.value(t.getName());
            }

            message(json, data);
            json.endArray();
        }

        json.endObject();
    }

    void table(FlushableJSONBuilder json, Table data, Object parent, int expand)
            throws IOException {
        json.object();
        json.key("type").value("table");
        json.key("name").value(data.getName());
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        json.key("href").value(RequestInfo.get().servletURI(pathTo(data, parent)));
        json.endObject();
    }

    void message(FlushableJSONBuilder json, ImportData data) throws IOException {
        if (data.getMessage() != null) {
            json.key("message").value(data.getMessage());
        }
    }

    void messages(FlushableJSONBuilder json, List<LogRecord> records) {
        if (!records.isEmpty()) {
            json.key("messages");
            json.array();
            for (int i = 0; i < records.size(); i++) {
                LogRecord record = records.get(i);
                json.object();
                json.key("level").value(record.getLevel().toString());
                json.key("message").value(record.getMessage());
                json.endObject();
            }
            json.endArray();
        }
    }

    String concatErrorMessages(Throwable ex) {
        StringBuilder buf = new StringBuilder();
        while (ex != null) {
            if (buf.length() > 0) {
                buf.append('\n');
            }
            if (ex.getMessage() != null) {
                buf.append(ex.getMessage());
            }
            ex = ex.getCause();
        }
        return buf.toString();
    }

    FlushableJSONBuilder builder(OutputStream out) {
        return new FlushableJSONBuilder(out);
    }

    JSONObject toJSON(Object o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        toJSON(o, out);
        return (JSONObject) JSONSerializer.toJSON(new String(out.toByteArray()));
    }

    void toJSON(Object o, OutputStream out) throws IOException {
        XStreamPersister xp = persister();
        if (callback != null) {
            xp.setCallback(callback);
        }
        xp.save(o, out);
        out.flush();
    }

    XStreamPersister persister() {
        XStreamPersister xp =
                importer.initXStreamPersister(new XStreamPersisterFactory().createJSONPersister());

        xp.setReferenceByName(true);
        xp.setExcludeIds();

        // xp.setCatalog(importer.getCatalog());
        xp.setHideFeatureTypeAttributes();
        // @todo this is copy-and-paste from org.geoserver.catalog.rest.FeatureTypeResource
        xp.setCallback(
                new XStreamPersister.Callback() {

                    @Override
                    protected void postEncodeFeatureType(
                            FeatureTypeInfo ft,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        try {
                            writer.startNode("attributes");
                            context.convertAnother(ft.attributes());
                            writer.endNode();
                        } catch (IOException e) {
                            throw new RuntimeException("Could not get native attributes", e);
                        }
                    }
                });
        return xp;
    }

    String srs(CoordinateReferenceSystem crs) {
        return CRS.toSRS(crs);
    }

    static String pathTo(ImportContext context) {
        return "/imports/" + context.getId();
    }

    static String pathTo(ImportTask task) {
        return pathTo(task.getContext()) + "/tasks/" + task.getId();
    }

    String pathTo(Object parent) {
        if (parent instanceof ImportContext) {
            return pathTo((ImportContext) parent);
        } else if (parent instanceof ImportTask) {
            return pathTo((ImportTask) parent);
        } else {
            throw new IllegalArgumentException("Don't recognize: " + parent);
        }
    }

    String pathTo(ImportData data, Object parent) {
        return pathTo(parent) + "/data";
    }

    public static RestException badRequest(String error) {
        JSONObject errorResponse = new JSONObject();
        JSONArray errors = new JSONArray();
        errors.add(error);
        errorResponse.put("errors", errors);

        return new RestException(errorResponse.toString(), HttpStatus.BAD_REQUEST);
    }

    public static class FlushableJSONBuilder extends JSONBuilder {

        public FlushableJSONBuilder(Writer w) {
            super(w);
        }

        public FlushableJSONBuilder(OutputStream out) {
            this(new OutputStreamWriter(out));
        }

        public void flush() throws IOException {
            writer.flush();
        }
    }

    /** @return the callback */
    public Callback getCallback() {
        return callback;
    }

    /** @param callback the callback to set */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private SimpleDateFormat getISODateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format;
    }
}
