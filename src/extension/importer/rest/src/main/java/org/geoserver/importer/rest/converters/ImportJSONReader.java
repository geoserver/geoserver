/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.importer.Archive;
import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportContext.State;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.UpdateMode;
import org.geoserver.importer.ValidationException;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.importer.mosaic.TimeMode;
import org.geoserver.importer.rest.ImportLayer;
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
import org.geoserver.importer.transform.RasterTransformChain;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@link BaseMessageConverter} implementation for reading {@link ImportContext} and {@link
 * ImportTask} objects from JSON
 */
@Component
public class ImportJSONReader {

    Importer importer;

    @Autowired
    public ImportJSONReader(Importer importer) {
        this.importer = importer;
    }

    //    public ImportContextJSONConverterReader(Importer importer, InputStream in) throws
    // IOException {
    //        super(MediaType.APPLICATION_JSON,AbstractCatalogController.TEXT_JSON);
    //        this.importer = importer;
    //        JSONObject json = parse(in);
    //    }

    //    @Override
    //    protected boolean supports(Class<?> clazz) {
    //        return (ImportContext.class.isAssignableFrom(clazz) ||
    // ImportTask.class.isAssignableFrom(clazz) ||
    //                ImportTransform.class.isAssignableFrom(clazz) ||
    // TransformChain.class.isAssignableFrom(clazz));
    //    }

    //    @Override
    //    protected boolean canWrite(MediaType mediaType) {
    //        return false; // write not supported
    //    }

    //    @Override
    //    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage
    // inputMessage)
    //            throws IOException, HttpMessageNotReadableException {
    //        InputStream in = inputMessage.getBody();
    //        JSONObject json = parse(in);
    //        if (ImportContext.class.isAssignableFrom(clazz)) {
    //            return context(json);
    //        } else if (ImportTask.class.isAssignableFrom(clazz)) {
    //            return task(json);
    //        } else if (ImportTransform.class.isAssignableFrom(clazz) ||
    // TransformChain.class.isAssignableFrom(clazz)) {
    //            return transform(json);
    //        }
    //        return null;
    //    }

    public ImportContext context(JSONObject json) throws IOException {
        ImportContext context = null;
        if (json.has("import")) {
            context = new ImportContext();

            json = json.getJSONObject("import");
            if (json.has("id")) {
                context.setId(json.getLong("id"));
            }
            if (json.has("state")) {
                context.setState(State.valueOf(json.getString("state")));
            }
            if (json.has("user")) {
                context.setUser(json.getString("user"));
            }
            if (json.has("archive")) {
                context.setArchive(json.getBoolean("archive"));
            }
            if (json.has("targetWorkspace")) {
                context.setTargetWorkspace(
                        fromJSON(json.getJSONObject("targetWorkspace"), WorkspaceInfo.class));
            }
            if (json.has("targetStore")) {
                context.setTargetStore(
                        fromJSON(json.getJSONObject("targetStore"), StoreInfo.class));
            }
            if (json.has("data")) {
                context.setData(data(json.getJSONObject("data")));
            }
            if (json.has("transforms")) {
                context.getDefaultTransforms().addAll(transforms(json.getJSONArray("transforms")));
            }
        }
        return context;
    }

    ImportLayer layer(JSONObject json) throws IOException {
        CatalogFactory f = importer.getCatalog().getFactory();

        if (json.has("layer")) {
            json = json.getJSONObject("layer");
        }

        // TODO: what about coverages?
        ResourceInfo r = f.createFeatureType();
        if (json.has("name")) {
            r.setName(json.getString("name"));
        }
        if (json.has("nativeName")) {
            r.setNativeName(json.getString("nativeName"));
        }
        if (json.has("srs")) {
            r.setSRS(json.getString("srs"));
            try {
                r.setNativeCRS(CRS.decode(json.getString("srs")));
            } catch (Exception e) {
                // should fail later
            }
        }
        if (json.has("bbox")) {
            r.setNativeBoundingBox(bbox(json.getJSONObject("bbox")));
        }
        if (json.has("title")) {
            r.setTitle(json.getString("title"));
        }
        if (json.has("abstract")) {
            r.setAbstract(json.getString("abstract"));
        }
        if (json.has("description")) {
            r.setDescription(json.getString("description"));
        }

        LayerInfo l = f.createLayer();
        l.setResource(r);
        // l.setName(); don't need to this, layer.name just forwards to name of underlying resource

        if (json.has("style")) {
            JSONObject sobj = new JSONObject();
            sobj.put("defaultStyle", json.get("style"));

            JSONObject lobj = new JSONObject();
            lobj.put("layer", sobj);

            LayerInfo tmp = fromJSON(lobj, LayerInfo.class);
            if (tmp.getDefaultStyle() != null) {
                l.setDefaultStyle(tmp.getDefaultStyle());
            } else {
                sobj = new JSONObject();
                sobj.put("style", json.get("style"));

                l.setDefaultStyle(fromJSON(sobj, StyleInfo.class));
            }
        }
        return new ImportLayer(l);
    }

    public ImportTask task(InputStream inputStream) throws IOException {
        JSONObject json = parse(inputStream);
        return task(json);
    }

    public ImportTask task(JSONObject json) throws IOException {

        if (json.has("task")) {
            json = json.getJSONObject("task");
        }

        ImportTask task = new ImportTask();

        if (json.has("id")) {
            task.setId(json.getInt("id"));
        }
        if (json.has("updateMode")) {
            task.setUpdateMode(UpdateMode.valueOf(json.getString("updateMode").toUpperCase()));
        } else {
            // if it hasn't been specified by the request, set this to null
            // or else it's possible to overwrite an existing setting
            task.setUpdateMode(null);
        }

        JSONObject data = null;
        if (json.has("data")) {
            data = json.getJSONObject("data");
        } else if (json.has("source")) { // backward compatible check for source
            data = json.getJSONObject("source");
        }

        if (data != null) {
            // we only support updating the charset
            if (data.has("charset")) {
                if (task.getData() == null) {
                    task.setData(new ImportData.TransferObject());
                }
                task.getData().setCharsetEncoding(data.getString("charset"));
            }
        }
        if (json.has("target")) {
            task.setStore(fromJSON(json.getJSONObject("target"), StoreInfo.class));
        }

        LayerInfo layer = null;
        if (json.has("layer")) {
            layer = layer(json.getJSONObject("layer")).getLayer();
        } else {
            layer = importer.getCatalog().getFactory().createLayer();
        }
        task.setLayer(layer);

        if (json.has("transformChain")) {
            task.setTransform(transformChain(json.getJSONObject("transformChain")));
        }

        return task;
    }

    TransformChain transformChain(JSONObject json) throws IOException {
        String type = json.getString("type");
        TransformChain chain = null;
        if ("vector".equalsIgnoreCase(type) || "VectorTransformChain".equalsIgnoreCase(type)) {
            chain = new VectorTransformChain();
        } else if ("raster".equalsIgnoreCase(type)
                || "RasterTransformChain".equalsIgnoreCase(type)) {
            chain = new RasterTransformChain();
        } else {
            throw new IOException("Unable to parse transformChain of type " + type);
        }
        JSONArray transforms = json.getJSONArray("transforms");
        for (int i = 0; i < transforms.size(); i++) {
            chain.add(transform(transforms.getJSONObject(i)));
        }
        return chain;
    }

    List<ImportTransform> transforms(JSONArray transforms) throws IOException {
        List<ImportTransform> result = new ArrayList<>();
        for (int i = 0; i < transforms.size(); i++) {
            result.add(transform(transforms.getJSONObject(i)));
        }
        return result;
    }

    public ImportTransform transform(String json) throws IOException {
        return transform(IOUtils.toInputStream(json, "UTF-8"));
    }

    public ImportTransform transform(InputStream inputStream) throws IOException {
        JSONObject json = parse(inputStream);
        return transform(json);
    }

    public ImportTransform transform(JSONObject json) throws IOException {
        ImportTransform transform;
        String type = json.getString("type");
        if ("DateFormatTransform".equalsIgnoreCase(type)) {
            transform =
                    new DateFormatTransform(
                            json.getString("field"),
                            json.optString("format", null),
                            json.optString("enddate", null),
                            json.optString("presentation", null));
        } else if ("IntegerFieldToDateTransform".equalsIgnoreCase(type)) {
            transform = new IntegerFieldToDateTransform(json.getString("field"));
        } else if ("CreateIndexTransform".equalsIgnoreCase(type)) {
            transform = new CreateIndexTransform(json.getString("field"));
        } else if ("AttributeRemapTransform".equalsIgnoreCase(type)) {
            Class clazz;
            try {
                clazz = Class.forName(json.getString("target"));
            } catch (ClassNotFoundException cnfe) {
                throw new ValidationException(
                        "unable to locate target class " + json.getString("target"));
            }
            transform = new AttributeRemapTransform(json.getString("field"), clazz);
        } else if ("AttributeComputeTransform".equalsIgnoreCase(type)) {
            Class clazz;
            try {
                clazz = Class.forName(json.getString("fieldType"));
            } catch (ClassNotFoundException cnfe) {
                throw new ValidationException(
                        "unable to locate target class " + json.getString("type"));
            }
            try {
                transform =
                        new AttributeComputeTransform(
                                json.getString("field"), clazz, json.getString("cql"));
            } catch (CQLException e) {
                throw new IOException(e);
            }
        } else if ("AttributesToPointGeometryTransform".equalsIgnoreCase(type)) {
            String latField = json.getString("latField");
            String lngField = json.getString("lngField");
            transform = new AttributesToPointGeometryTransform(latField, lngField);
        } else if ("ReprojectTransform".equalsIgnoreCase(type)) {
            CoordinateReferenceSystem source =
                    json.has("source") ? crs(json.getString("source")) : null;
            CoordinateReferenceSystem target =
                    json.has("target") ? crs(json.getString("target")) : null;

            try {
                transform = new ReprojectTransform(source, target);
            } catch (Exception e) {
                throw new ValidationException("Error parsing reproject transform", e);
            }
        } else if ("GdalTranslateTransform".equalsIgnoreCase(type)) {
            List<String> options = getOptions(json);
            transform = new GdalTranslateTransform(options);
        } else if ("GdalWarpTransform".equalsIgnoreCase(type)) {
            List<String> options = getOptions(json);
            transform = new GdalWarpTransform(options);
        } else if ("GdalAddoTransform".equalsIgnoreCase(type)) {
            List<String> options = getOptions(json);
            JSONArray array = json.getJSONArray("levels");
            List<Integer> levels = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                int level = array.getInt(i);
                levels.add(level);
            }
            transform = new GdalAddoTransform(options, levels);
        } else if ("PostScriptTransform".equalsIgnoreCase(type)) {
            String name = json.getString("name");
            List<String> options = getOptions(json);
            transform = new PostScriptTransform(name, options);
        } else {
            throw new ValidationException("Invalid transform type '" + type + "'");
        }
        return transform;
    }

    List<String> getOptions(JSONObject json) {
        if (!json.containsKey("options")) {
            return new ArrayList<>();
        }
        JSONArray array = json.getJSONArray("options");
        List<String> options = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String option = array.getString(i);
            options.add(option);
        }
        return options;
    }

    ImportData data(JSONObject json) throws IOException {
        String type = json.getString("type");
        if (type == null) {
            throw new IOException("Data object must specify 'type' property");
        }

        if ("file".equalsIgnoreCase(type)) {
            return file(json);
        } else if ("directory".equalsIgnoreCase(type)) {
            return directory(json);
        } else if ("mosaic".equalsIgnoreCase(type)) {
            return mosaic(json);
        } else if ("archive".equalsIgnoreCase(type)) {
            return archive(json);
        } else if ("database".equalsIgnoreCase(type)) {
            return database(json);
        } else if ("remote".equalsIgnoreCase(type)) {
            return remote(json);
        } else {
            throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }

    FileData file(JSONObject json) throws IOException {
        if (json.has("file")) {
            // TODO: find out if spatial or not
            String file = json.getString("file");
            FileData importFileData = FileData.createFromFile(new File(file));
            // check if charsetEncoding configured (Geos-9073)
            if (json.has("charsetEncoding")) {
                // check if charsetEncoding is supported
                // dont upload a charset which not supported
                Charset.isSupported(json.getString("charsetEncoding"));
                importFileData.setCharsetEncoding(json.getString("charsetEncoding"));
            }
            return importFileData;
            // return FileData.createFromFile(new File(file));
            // return new FileData(new File(file));
        } else {
            throw new IOException(
                    "Could not find 'file' entry in data, mandatory for file type data");
        }
    }

    RemoteData remote(JSONObject json) throws IOException {
        if (json.has("location")) {
            String location = json.getString("location");
            RemoteData data = new RemoteData(location);
            if (json.has("username")) {
                data.setUsername(json.getString("username"));
            }
            if (json.has("password")) {
                data.setPassword(json.getString("password"));
            }
            if (json.has("domain")) {
                data.setDomain(json.getString("domain"));
            }
            return data;
        } else {
            throw new IOException(
                    "Could not find 'location' entry in data, mandatory for remote type data");
        }
    }

    Mosaic mosaic(JSONObject json) throws IOException {
        Mosaic m =
                new Mosaic(
                        json.has("location")
                                ? new File(json.getString("location"))
                                : Directory.createNew(importer.getUploadRoot()).getFile());
        if (json.has("name")) {
            m.setName(json.getString("name"));
        }
        if (json.containsKey("time")) {
            JSONObject time = json.getJSONObject("time");
            if (!time.containsKey("mode")) {
                throw new IllegalArgumentException(
                        "time object must specific mode property as "
                                + "one of "
                                + Arrays.asList(TimeMode.values()));
            }

            m.setTimeMode(TimeMode.valueOf(time.getString("mode").toUpperCase()));
            m.getTimeHandler().init(time);
        }
        return m;
    }

    Archive archive(JSONObject json) throws IOException {
        throw new UnsupportedOperationException("TODO: implement");
    }

    public Directory directory(JSONObject json) throws IOException {
        if (json.has("location")) {
            return new Directory(new File(json.getString("location")));
        } else {
            return Directory.createNew(importer.getUploadRoot());
        }
    }

    Database database(JSONObject json) {
        throw new UnsupportedOperationException("TODO: implement");
    }

    ReferencedEnvelope bbox(JSONObject json) {
        CoordinateReferenceSystem crs = null;
        if (json.has("crs")) {
            crs =
                    (CoordinateReferenceSystem)
                            new XStreamPersister.CRSConverter().fromString(json.getString("crs"));
        }

        return new ReferencedEnvelope(
                json.getDouble("minx"),
                json.getDouble("maxx"),
                json.getDouble("miny"),
                json.getDouble("maxy"),
                crs);
    }

    CoordinateReferenceSystem crs(String srs) {
        try {
            return CRS.decode(srs);
        } catch (Exception e) {
            throw new RuntimeException("Failing parsing srs: " + srs, e);
        }
    }

    public JSONObject parse(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(in, bout);
        return JSONObject.fromObject(new String(bout.toByteArray()));
    }

    Object read(InputStream in) throws IOException {
        Object result = null;
        JSONObject json = parse(in);
        // @hack - this should return a ImportTask
        if (json.containsKey("target")) {
            result = fromJSON(json.getJSONObject("target"), DataStoreInfo.class);
        }
        return result;
    }

    <T> T fromJSON(JSONObject json, Class<T> clazz) throws IOException {
        XStreamPersister xp = importer.createXStreamPersisterJSON();
        return xp.load(new ByteArrayInputStream(json.toString().getBytes()), clazz);
    }
}
