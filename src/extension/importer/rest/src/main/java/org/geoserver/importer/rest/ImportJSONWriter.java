/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import org.geoserver.importer.transform.AttributeRemapTransform;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.CreateIndexTransform;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.GdalAddoTransform;
import org.geoserver.importer.transform.GdalTranslateTransform;
import org.geoserver.importer.transform.GdalWarpTransform;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Utility class for reading/writing import/tasks/etc... to/from JSON.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportJSONWriter {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    Importer importer;
    PageInfo page;
    FlushableJSONBuilder json;

    public ImportJSONWriter(Importer importer, PageInfo page) {
        this(importer, page, new ByteArrayOutputStream());
    }

    public ImportJSONWriter(Importer importer, PageInfo page, OutputStream out) {
        this(importer, page, new OutputStreamWriter(out));
    }

    public ImportJSONWriter(Importer importer, PageInfo page, Writer w) {
        this.importer = importer;
        this.page = page;
        this.json = new FlushableJSONBuilder(w);
    }

    public void contexts(Iterator<ImportContext> contexts, int expand) throws IOException {
        json.object().key("imports").array();
        while (contexts.hasNext()) {
            ImportContext context = contexts.next();
            context(context, false, expand);
        }
        json.endArray().endObject();
        json.flush();
    }

    public void context(ImportContext context, boolean top, int expand) throws IOException {
        if (top) {
            json.object().key("import");
        }

        json.object();
        json.key("id").value(context.getId());
        json.key("href").value(page.rootURI(pathTo(context)));
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
                store(context.getTargetStore(), null, false, expand-1);
                //value(toJSON(context.getTargetStore()));
            }
    
            if (context.getData() != null) {
                json.key("data");
                data(context.getData(), context, expand-1);
            }
            if (!context.getDefaultTransforms().isEmpty()) {
                transforms(context, expand - 1, context.getDefaultTransforms());
            }
            tasks(context.getTasks(), false, expand-1);
        }

        json.endObject();

        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void tasks(List<ImportTask> tasks, boolean top, int expand) throws IOException {

        if (top) {
            json.object();
        }

        json.key("tasks").array();
        for (ImportTask task : tasks) {
            task(task, false, expand);
        }
        json.endArray();
        
        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void task(ImportTask task, boolean top, int expand) throws IOException {

        long id = task.getId();
        String href = page.rootURI(pathTo(task));
        if (top) {
            json.object().key("task");
        }
        json.object();
        json.key("id").value(id);
        json.key("href").value(href);
        json.key("state").value(task.getState());

        if (expand > 0) {
            json.key("updateMode").value(task.getUpdateMode().name());
    
            //data (used to be source)
            ImportData data = task.getData();
            if (data != null) {
                json.key("data");
                data(data, task, expand-1);
            }

            //target
            StoreInfo store = task.getStore();
            if (store != null) {
                json.key("target");
                store(store, task, false, expand-1);
            }

            json.key("progress").value(href + "/progress");

            LayerInfo layer = task.getLayer();
            if (layer != null) {
                // @todo don't know why catalog isn't set here, thought this was set during load from BDBImportStore
                layer.getResource().setCatalog(importer.getCatalog());

                json.key("layer");
                layer(task, false, expand-1);
            }

            if (task.getError() != null) {
                json.key("errorMessage").value(concatErrorMessages(task.getError()));
            }

            transformChain(task, false, expand-1);
            messages(task.getMessages());
        }
        
        json.endObject();
        if (top) {
            json.endObject();
        }

        json.flush();
    }

    void store(StoreInfo store, ImportTask task, boolean top, int expand) throws IOException {
            
        String type = store instanceof DataStoreInfo ? "dataStore" : 
                      store instanceof CoverageStoreInfo ? "coverageStore" : "store";

        json.object();
        if (task != null) {
            json.key("href").value(page.rootURI(pathTo(task) + "/target"));
        }

        if (expand > 0) {
            JSONObject obj = toJSON(store);
            json.key(type).value(obj.get(type));
        }
        else {
            json.key(type).object()
                .key("name").value(store.getName())
                .key("type").value(store.getType())
                .endObject();
        }

        json.endObject();
        json.flush();
    }

    void layer(ImportTask task, boolean top, int expand) throws IOException {

        if (top) {
            json.object().key("layer");
        }

        LayerInfo layer = task.getLayer();
        ResourceInfo r = layer.getResource();

        json.object()
            .key("name").value(layer.getName())
            .key("href").value(page.rootURI(pathTo(task) + "/layer"));
        
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
                featureType((FeatureTypeInfo) r);
            }
            StyleInfo s = layer.getDefaultStyle();
            if (s != null) {
                style(s, task, false, expand-1);
            }
        }

        json.endObject();
        if (top) {
            json.endObject();
        }
        json.flush();
    }

    void featureType(FeatureTypeInfo featureTypeInfo) throws IOException {
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

    void style(StyleInfo style, ImportTask task, boolean top, int expand) throws IOException {
        
        if (top) {
            json.object();
        }

        String href = page.rootURI(pathTo(task) + "/layer/style");

        json.key("style");
        if (expand > 0) {
            JSONObject obj = toJSON(style).getJSONObject("style");
            obj.put("href", href);
            json.value(obj);
        }
        else {
            json.object();
            json.key("name").value(style.getName());
            json.key("href").value(href);
            json.endObject();
        }
        
        if (top) {
            json.endObject();
        }
    }

    void transformChain(ImportTask task, boolean top, int expand) throws IOException {

        if (top) {
            json.object();
        }

        TransformChain<? extends ImportTransform> txChain = task.getTransform();

        json.key("transformChain").object();
        json.key("type").value(txChain instanceof VectorTransformChain ? "vector" : "raster");

        transforms(task, expand, txChain != null ? txChain.getTransforms()
                : new ArrayList<ImportTransform>());
        json.endObject();

        if (top) {
            json.endObject();
        }
        
        json.flush();
    }

    private void transforms(Object parent, int expand, List<? extends ImportTransform> transforms)
            throws IOException {
        json.key("transforms").array();

        for (int i = 0; i < transforms.size(); i++) {
            transform(transforms.get(i), i, parent, false, expand);
        }

        json.endArray();
    }

    public void transform(ImportTransform transform, int index, Object parent, boolean top,
        int expand) throws IOException {
        json.object();
        json.key("type").value(transform.getClass().getSimpleName());
        json.key("href").value(page.rootURI(pathTo(parent) + "/transforms/" + index));
        if (expand > 0) {
            if (transform instanceof DateFormatTransform) {
                DateFormatTransform df = (DateFormatTransform) transform;
                json.key("field").value(df.getField());
                if (df.getDatePattern() != null) {
                    json.key("format").value(df.getDatePattern().dateFormat().toPattern());
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
            } else if (transform.getClass() == AttributesToPointGeometryTransform.class) {
                AttributesToPointGeometryTransform atpgt = (AttributesToPointGeometryTransform) transform;
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
            } else {
                throw new IOException("Serializaiton of " + transform.getClass() + " not implemented");
            }
        }
        json.endObject();
        json.flush();
    }

    private void buildJsonOptions(FlushableJSONBuilder json, String key, List<String> options) {
        JSONBuilder arrayBuilder = json.key(key).array();
        for (String option : options) {
            arrayBuilder.value(option);
        }
        arrayBuilder.endArray();

    }

    void bbox(JSONBuilder json, ReferencedEnvelope bbox) {
        json.object()
            .key("minx").value(bbox.getMinX())
            .key("miny").value(bbox.getMinY())
            .key("maxx").value(bbox.getMaxX())
            .key("maxy").value(bbox.getMaxY());

        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem(); 
        if (crs != null) {
            json.key("crs").value(crs.toWKT());
        }

        json.endObject();
    }

    public void data(ImportData data, Object parent, int expand) throws IOException {
        if (data instanceof FileData) {
            if (data instanceof Directory) {
                if (data instanceof Mosaic) {
                    mosaic((Mosaic) data, parent ,expand);
                }
                else {
                    directory((Directory) data, parent, expand);
                }
            } else {
                file((FileData) data, parent, expand, false);
            }
        } else if (data instanceof Database) {
            database((Database) data, parent, expand);
        } else if (data instanceof Table) {
            table((Table)data, parent, expand);
        } else if (data instanceof RemoteData) {
            remote((RemoteData) data, parent, expand);
        }
        json.flush();
    }

    public void remote(RemoteData data, Object parent, int expand) throws IOException {

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

    public void file(FileData data, Object parent, int expand, boolean href) throws IOException {
        
        json.object();
        
        json.key("type").value("file");
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        if (href) {
            json.key("href").value(page.rootURI(pathTo(data, parent)));
        }
        
        if (expand > 0) {
            json.key("location").value(data.getFile().parent().path());
            if (data.getCharsetEncoding() != null) {
                json.key("charset").value(data.getCharsetEncoding());
            }
            fileContents(data, parent, expand);
            message(data);
        }
        else {
            json.key("file").value(data.getFile().name());
        }

        json.endObject();
        json.flush();
    }

    void fileContents(FileData data, Object parent, int expand) throws IOException {
        //TODO: we should probably url encode to handle spaces and other chars
        String filename = data.getFile().name();
        json.key("file").value(filename);
        json.key("href").value(page.rootURI(pathTo(data, parent)+"/files/"+filename));
        if (expand > 0) {
            if (data instanceof SpatialFile) {
                SpatialFile sf = (SpatialFile) data;
                json.key("prj").value(sf.getPrjFile() != null ? sf.getPrjFile().name() : null);
                json.key("other").array();
                for (Resource supp : ((SpatialFile) data).getSuppFiles()) {
                    json.value(supp.name());
                }
                json.endArray();
    
                if (sf instanceof Granule) {
                    Granule g = (Granule) sf;
                    if (g.getTimestamp() != null) {
                        json.key("timestamp").value(DATE_FORMAT.format(g.getTimestamp()));
                    }
                }
            }
        }
    }


    public void mosaic(Mosaic data, Object parent, int expand) 
        throws IOException {
        directory(data, "mosaic", parent, expand);
    }

    public void directory(Directory data, Object parent, int expand) throws IOException {
        directory(data, "directory", parent, expand);
    }

    public void directory(Directory data, String typeName, Object parent, int expand) 
        throws IOException {

        json.object();
        json.key("type").value(typeName);
        if (data.getFormat() != null) {
            json.key("format").value(data.getFormat().getName());
        }

        json.key("location").value(data.getFile().path());
        json.key("href").value(page.rootURI(pathTo(data, parent)));

        if (expand > 0) {
            if (data.getCharsetEncoding() != null) {
                json.key("charset").value(data.getCharsetEncoding());
            }

            json.key("files");
            files(data, parent, false, expand-1);
            message(data);
        }
        json.endObject();
        json.flush();
    }

    public void files(Directory data, Object parent, boolean top, int expand) throws IOException {

        if (top) {
            json.object().key("files");
        }
        json.array();
        for (FileData file : data.getFiles()) {
            json.object();
            fileContents(file, parent, expand-1);
            json.endObject();
        }
        json.endArray();
        if (top) {
            json.endObject();
        }
        json.flush();
    }

    public void database(Database data, Object parent, int expand) throws IOException {
        json.object();
        json.key("type").value("database");
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        json.key("href").value(page.rootURI(pathTo(data, parent)));

        if (expand > 0) {
            json.key("parameters").object();
            for (Map.Entry e : data.getParameters().entrySet()) {
                json.key((String) e.getKey()).value(e.getValue());
            }
    
            json.endObject();
            
            json.key("tables").array();
            for (Table t : data.getTables()) {
                json.value(t.getName());
            }
    
            message(data);
            json.endArray();
        }

        json.endObject();
    }

    void table(Table data, Object parent, int expand) throws IOException {
        json.object();
        json.key("type").value("table");
        json.key("name").value(data.getName());
        json.key("format").value(data.getFormat() != null ? data.getFormat().getName() : null);
        json.key("href").value(page.rootURI(pathTo(data, parent)));
        json.endObject();
    }

    void message(ImportData data) throws IOException {
        if (data.getMessage() != null) {
            json.key("message").value(data.getMessage());
        }
    }

    void messages(List<LogRecord> records) {
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
        return new FlushableJSONBuilder(new OutputStreamWriter(out));
    }

    JSONObject toJSON(Object o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        toJSON(o, out);
        return (JSONObject) JSONSerializer.toJSON(new String(out.toByteArray()));
    }

    void toJSON(Object o, OutputStream out) throws IOException {
        toJSON(o, out, null);
    }
    
    void toJSON(Object o, OutputStream out, Callback callback) throws IOException {
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

        //xp.setCatalog(importer.getCatalog());
        xp.setHideFeatureTypeAttributes();
        // @todo this is copy-and-paste from org.geoserver.catalog.rest.FeatureTypeResource
        xp.setCallback(new XStreamPersister.Callback() {

            @Override
            protected void postEncodeFeatureType(FeatureTypeInfo ft,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
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
        return pathTo(task.getContext()) +  "/tasks/" + task.getId();
    }

    String pathTo(Object parent) {
        if (parent instanceof ImportContext) {
            return pathTo((ImportContext)parent);
        }
        else if (parent instanceof ImportTask) {
            return pathTo((ImportTask)parent);
        }
        else {
            throw new IllegalArgumentException("Don't recognize: " + parent);
        }
    }

    String pathTo(ImportData data, Object parent) {
        return pathTo(parent) + "/data";
    }

    static RestletException badRequest(String error) {
        JSONObject errorResponse = new JSONObject();
        JSONArray errors = new JSONArray();
        errors.add(error);
        errorResponse.put("errors", errors);
        
        JSONRepresentation rep = new JSONRepresentation(errorResponse);
        return new RestletException(rep, Status.CLIENT_ERROR_BAD_REQUEST);
    }

    public static class FlushableJSONBuilder extends JSONBuilder {

        public FlushableJSONBuilder(Writer w) {
            super(w);
        }

        public void flush() throws IOException {
            writer.flush();
        }
    }
}
