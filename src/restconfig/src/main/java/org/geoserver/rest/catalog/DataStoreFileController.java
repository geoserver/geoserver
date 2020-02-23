/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.RESTUploadPathMapper;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vfny.geoserver.util.DataStoreUtils;

@RestController
@ControllerAdvice
@RequestMapping(
    path =
            RestBaseController.ROOT_PATH
                    + "/workspaces/{workspaceName}/datastores/{storeName}/{method}.{format}"
)
public class DataStoreFileController extends AbstractStoreUploadController {

    @Autowired
    public DataStoreFileController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    private static final Pattern H2_FILE_PATTERN = Pattern.compile("(.*?)\\.(?:data.db)");

    protected static final HashMap<String, String> formatToDataStoreFactory = new HashMap();

    static {
        formatToDataStoreFactory.put(
                "shp", "org.geotools.data.shapefile.ShapefileDataStoreFactory");
        formatToDataStoreFactory.put(
                "properties", "org.geotools.data.property.PropertyDataStoreFactory");
        formatToDataStoreFactory.put("h2", "org.geotools.data.h2.H2DataStoreFactory");
        formatToDataStoreFactory.put(
                "spatialite", "org.geotools.data.spatialite.SpatiaLiteDataStoreFactory");
        formatToDataStoreFactory.put(
                "appschema", "org.geotools.data.complex.AppSchemaDataAccessFactory");
        formatToDataStoreFactory.put("gpkg", "org.geotools.geopkg.GeoPkgDataStoreFactory");
    }

    protected static final HashMap<String, Map> dataStoreFactoryToDefaultParams = new HashMap();

    static {
        HashMap map = new HashMap();
        map.put("database", "@DATA_DIR@/@NAME@");
        map.put("dbtype", "h2");

        dataStoreFactoryToDefaultParams.put("org.geotools.data.h2.H2DataStoreFactory", map);

        map = new HashMap();
        map.put("database", "@DATA_DIR@/@NAME@");
        map.put("dbtype", "spatialite");

        dataStoreFactoryToDefaultParams.put(
                "org.geotools.data.spatialite.SpatiaLiteDataStoreFactory", map);
    }

    public static DataAccessFactory lookupDataStoreFactory(String format) {
        // first try and see if we know about this format directly
        String factoryClassName = formatToDataStoreFactory.get(format);
        if (factoryClassName != null) {
            try {
                Class factoryClass = Class.forName(factoryClassName);
                return (DataAccessFactory) factoryClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RestException(
                        "Datastore format unavailable: " + factoryClassName,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // if not, let's see if we have a file data store factory that knows about the extension
        String extension = "." + format;
        for (DataAccessFactory dataAccessFactory :
                DataStoreUtils.getAvailableDataStoreFactories()) {
            if (dataAccessFactory instanceof FileDataStoreFactorySpi) {
                FileDataStoreFactorySpi factory = (FileDataStoreFactorySpi) dataAccessFactory;
                for (String handledExtension : factory.getFileExtensions()) {
                    if (extension.equalsIgnoreCase(handledExtension)) {
                        return factory;
                    }
                }
            }
        }

        throw new RestException("Unsupported format: " + format, HttpStatus.BAD_REQUEST);
    }

    public static String lookupDataStoreFactoryFormat(String type) {
        for (DataAccessFactory factory : DataStoreUtils.getAvailableDataStoreFactories()) {
            if (factory == null) {
                continue;
            }

            if (factory.getDisplayName() != null && factory.getDisplayName().equals(type)) {
                for (Map.Entry e : formatToDataStoreFactory.entrySet()) {
                    if (e.getValue().equals(factory.getClass().getCanonicalName())) {
                        return (String) e.getKey();
                    }
                }

                return factory.getDisplayName();
            }
        }

        return null;
    }

    @GetMapping
    public ResponseEntity dataStoresGet(
            @PathVariable String workspaceName, @PathVariable String storeName) throws IOException {

        // find the directory from teh datastore connection parameters
        DataStoreInfo info = catalog.getDataStoreByName(workspaceName, storeName);
        if (info == null) {
            throw new RestException("No such datastore " + storeName, HttpStatus.NOT_FOUND);
        }
        ResourcePool rp = info.getCatalog().getResourcePool();
        GeoServerResourceLoader resourceLoader = info.getCatalog().getResourceLoader();
        Map<String, Serializable> rawParamValues = info.getConnectionParameters();
        Map<String, Serializable> paramValues = rp.getParams(rawParamValues, resourceLoader);
        File directory = null;
        try {
            DataAccessFactory factory = rp.getDataStoreFactory(info);
            for (DataAccessFactory.Param param : factory.getParametersInfo()) {
                if (File.class.isAssignableFrom(param.getType())) {
                    Object result = param.lookUp(paramValues);
                    if (result instanceof File) {
                        directory = (File) result;
                    }
                } else if (URL.class.isAssignableFrom(param.getType())) {
                    Object result = param.lookUp(paramValues);
                    if (result instanceof URL) {
                        directory = URLs.urlToFile((URL) result);
                    }
                }

                if (directory != null && !"directory".equals(param.key)) {
                    directory = directory.getParentFile();
                }

                if (directory != null) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RestException(
                    "Failed to lookup source directory for store " + storeName,
                    HttpStatus.NOT_FOUND,
                    e);
        }

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new RestException("No files for datastore " + storeName, HttpStatus.NOT_FOUND);
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            BufferedOutputStream bufferedOutputStream =
                    new BufferedOutputStream(byteArrayOutputStream);
            ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);

            // packing files
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    // new zip entry and copying inputstream with file to zipOutputStream, after all
                    // closing streams
                    zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
                    FileInputStream fileInputStream = new FileInputStream(file);

                    IOUtils.copy(fileInputStream, zipOutputStream);

                    fileInputStream.close();
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.finish();
            zipOutputStream.flush();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(
                    "content-disposition", "attachment; filename=" + info.getName() + ".zip");
            responseHeaders.add("Content-Type", "application/zip");
            return new ResponseEntity(
                    byteArrayOutputStream.toByteArray(), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping
    public void dataStorePut(
            @PathVariable String workspaceName,
            @PathVariable String storeName,
            @PathVariable UploadMethod method,
            @PathVariable String format,
            @RequestParam(name = "configure", required = false) String configure,
            @RequestParam(name = "target", required = false) String target,
            @RequestParam(name = "update", required = false) String update,
            @RequestParam(name = "charset", required = false) String characterset,
            @RequestParam(name = "filename", required = false) String filename,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpStatus.ACCEPTED.value());

        // doFileUpload returns a List of File but in the case of a Put operation the list contains
        // only a value
        List<Resource> files =
                doFileUpload(method, workspaceName, storeName, filename, format, request);
        final Resource uploadedFile = files.get(0);

        DataAccessFactory factory = lookupDataStoreFactory(format);

        // look up the target datastore type specified by user
        String sourceDataStoreFormat = format;
        String targetDataStoreFormat = target;
        if (targetDataStoreFormat == null) {
            // set the same type as the source
            targetDataStoreFormat = sourceDataStoreFormat;
        }

        sourceDataStoreFormat = sourceDataStoreFormat.toLowerCase();
        targetDataStoreFormat = targetDataStoreFormat.toLowerCase();

        // create a builder to help build catalog objects
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(catalog.getWorkspaceByName(workspaceName));

        // does the target datastore already exist?
        DataStoreInfo info = catalog.getDataStoreByName(workspaceName, storeName);

        // set the namespace uri
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceName);

        boolean add = false;
        boolean save = false;
        boolean canRemoveFiles = false;

        if (info == null) {
            LOGGER.info("Auto-configuring datastore: " + storeName);

            info = builder.buildDataStore(storeName);
            add = true;

            // TODO: should check if the store actually supports charset
            if (characterset != null && characterset.length() > 0) {
                info.getConnectionParameters().put("charset", characterset);
            }
            DataAccessFactory targetFactory = factory;
            if (!targetDataStoreFormat.equals(sourceDataStoreFormat)) {
                // target is different, we need to create it
                targetFactory = lookupDataStoreFactory(targetDataStoreFormat);
                if (targetFactory == null) {
                    throw new RestException(
                            "Unable to create data store of type " + targetDataStoreFormat,
                            HttpStatus.BAD_REQUEST);
                }

                autoCreateParameters(info, namespace, targetFactory);
                canRemoveFiles = true;
            } else {
                updateParameters(info, namespace, targetFactory, uploadedFile);
            }

            info.setType(targetFactory.getDisplayName());
        } else {
            LOGGER.info("Using existing datastore: " + storeName);

            // look up the target data store factory
            targetDataStoreFormat = lookupDataStoreFactoryFormat(info.getType());
            if (targetDataStoreFormat == null) {
                throw new RuntimeException(
                        "Unable to locate data store factory of type " + info.getType());
            }

            if (targetDataStoreFormat.equals(sourceDataStoreFormat)) {
                save = true;
                updateParameters(info, namespace, factory, uploadedFile);
            } else {
                canRemoveFiles = true;
            }
        }
        builder.setStore(info);

        // add or update the datastore info
        if (add) {
            catalog.validate(info, true).throwIfInvalid();
            catalog.add(info);
        } else if (save) {
            catalog.validate(info, false).throwIfInvalid();
            catalog.save(info);
        }

        boolean createNewSource;
        DataAccess<?, ?> source;
        try {
            HashMap params = new HashMap();
            if (characterset != null && characterset.length() > 0) {
                params.put("charset", characterset);
            }
            params.put("namespace", namespace.getURI());
            updateParameters(params, factory, uploadedFile);

            createNewSource = !sameTypeAndUrl(params, info.getConnectionParameters());
            // this is a bit hacky, but makes sure that a datastore is not created
            // twice with the same "dbtype" and "url" connection parameters, which
            // would result in a "duplicated mapping" exception for an app-schema
            // datastore.
            source = (createNewSource) ? factory.createDataStore(params) : info.getDataStore(null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create source data store", e);
        }

        try {
            DataAccess ds = info.getDataStore(null);
            // synchronized(ds) {
            // if it is the case that the source does not match the target we need to
            // copy the data into the target
            if (!targetDataStoreFormat.equals(sourceDataStoreFormat)
                    && (source instanceof DataStore && ds instanceof DataStore)) {
                // copy over the feature types
                DataStore sourceDataStore = (DataStore) source;
                DataStore targetDataStore = (DataStore) ds;
                for (String featureTypeName : sourceDataStore.getTypeNames()) {

                    // does the feature type already exist in the target?
                    try {
                        targetDataStore.getSchema(featureTypeName);
                    } catch (Exception e) {
                        LOGGER.info(
                                featureTypeName
                                        + " does not exist in data store "
                                        + storeName
                                        + ". Attempting to create it");

                        // schema does not exist, create it by first creating an instance
                        // of the source datastore and copying over its schema
                        targetDataStore.createSchema(sourceDataStore.getSchema(featureTypeName));
                        sourceDataStore.getSchema(featureTypeName);
                    }

                    FeatureSource featureSource = targetDataStore.getFeatureSource(featureTypeName);
                    if (!(featureSource instanceof FeatureStore)) {
                        LOGGER.warning(featureTypeName + " is not writable, skipping");
                        continue;
                    }

                    Transaction tx = new DefaultTransaction();
                    FeatureStore featureStore = (FeatureStore) featureSource;
                    featureStore.setTransaction(tx);

                    try {
                        // figure out update mode, whether we should kill existing data or append
                        if ("overwrite".equalsIgnoreCase(update)) {
                            LOGGER.fine("Removing existing features from " + featureTypeName);
                            // kill all features
                            featureStore.removeFeatures(Filter.INCLUDE);
                        }

                        LOGGER.fine("Adding features to " + featureTypeName);
                        FeatureCollection features =
                                sourceDataStore.getFeatureSource(featureTypeName).getFeatures();
                        featureStore.addFeatures(features);

                        tx.commit();
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Failed to import data, rolling back the transaction",
                                e);
                        tx.rollback();
                    } finally {
                        tx.close();
                    }
                }
            }

            // check configure parameter, if set to none do not try to configure
            // data feature types
            // String configure = form.getFirstValue( "configure" );
            if ("none".equalsIgnoreCase(configure)) {
                response.setStatus(HttpStatus.CREATED.value());
                return;
            }

            // load the target datastore
            // DataStore ds = (DataStore) info.getDataStore(null);
            Map<String, FeatureTypeInfo> featureTypesByNativeName = new HashMap<>();
            for (FeatureTypeInfo ftInfo : catalog.getFeatureTypesByDataStore(info)) {
                featureTypesByNativeName.put(ftInfo.getNativeName(), ftInfo);
            }

            List<Name> featureTypeNames = source.getNames();
            for (int i = 0; i < featureTypeNames.size(); i++) {

                // unless configure specified "all", only configure the first feature type
                if (!"all".equalsIgnoreCase(configure) && i > 0) {
                    break;
                }

                FeatureSource fs = ds.getFeatureSource(featureTypeNames.get(i));
                FeatureTypeInfo ftinfo =
                        featureTypesByNativeName.get(featureTypeNames.get(i).getLocalPart());

                if (ftinfo == null) {
                    // auto configure the feature type as well
                    ftinfo = builder.buildFeatureType(fs);
                    builder.lookupSRS(ftinfo, true);
                    builder.setupBounds(ftinfo);
                }

                // update the bounds
                ReferencedEnvelope bounds = fs.getBounds();
                ftinfo.setNativeBoundingBox(bounds);

                // TODO: set lat lon bounding box

                if (ftinfo.getId() == null) {
                    // do a check for a type already named this name in the catalog, if it is
                    // already
                    // there try to rename it
                    if (catalog.getFeatureTypeByName(namespace, ftinfo.getName()) != null) {
                        LOGGER.warning(
                                String.format(
                                        "Feature type %s already exists in namespace %s, "
                                                + "attempting to rename",
                                        ftinfo.getName(), namespace.getPrefix()));
                        int x = 1;
                        String originalName = ftinfo.getName();
                        do {
                            ftinfo.setName(originalName + x);
                            x++;
                        } while (catalog.getFeatureTypeByName(namespace, ftinfo.getName()) != null);
                    }
                    catalog.validate(ftinfo, true).throwIfInvalid();
                    catalog.add(ftinfo);

                    // add a layer for the feature type as well
                    LayerInfo layer = builder.buildLayer(ftinfo);

                    boolean valid = true;
                    try {
                        if (!catalog.validate(layer, true).isValid()) {
                            valid = false;
                        }
                    } catch (Exception e) {
                        valid = false;
                    }

                    layer.setEnabled(valid);
                    catalog.add(layer);

                    LOGGER.info("Added feature type " + ftinfo.getName());

                } else {
                    LOGGER.info("Updated feature type " + ftinfo.getName());
                    catalog.validate(ftinfo, false).throwIfInvalid();
                    catalog.save(ftinfo);
                }

                response.setStatus(HttpStatus.CREATED.value());
            }
        } catch (Exception e) {
            // TODO: report a proper error code
            throw new RuntimeException(e);
        } finally {
            // dispose the source datastore, if needed
            if (createNewSource) {
                source.dispose();
            }

            // clean up the files if we can
            if (method.isInline() && canRemoveFiles) {
                if (uploadedFile.getType() == Resource.Type.RESOURCE) {
                    if (!uploadedFile.parent().delete()) {
                        LOGGER.info("Unable to delete " + uploadedFile.path());
                    }
                } else if (uploadedFile.getType() == Resource.Type.DIRECTORY) {
                    for (Resource file : files) {
                        if (file.getType() == Resource.Type.RESOURCE) {
                            if (!file.delete()) {
                                LOGGER.info("Unable to delete " + file.path());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Does the file upload based on the specified method.
     *
     * @param method The method, one of 'file.' (inline), 'url.' (via url), or 'external.' (already
     *     on server)
     * @param storeName The name of the store being added
     * @param format The store format.
     */
    protected List<Resource> doFileUpload(
            UploadMethod method,
            String workspaceName,
            String storeName,
            String filename,
            String format,
            HttpServletRequest request)
            throws IOException {
        Resource directory = null;

        boolean postRequest =
                request != null && HttpMethod.POST.name().equalsIgnoreCase(request.getMethod());

        // Prepare the directory only in case this is not an external upload
        if (method.isInline()) {
            // Mapping of the input directory
            if (method == UploadMethod.url) {
                // For URL upload method, workspace and StoreName are not considered
                directory = createFinalRoot(null, null, postRequest);
            } else {
                directory = createFinalRoot(workspaceName, storeName, postRequest);
            }
        }
        return handleFileUpload(
                storeName, workspaceName, filename, method, format, directory, request);
    }

    private Resource createFinalRoot(String workspaceName, String storeName, boolean isPost)
            throws IOException {
        // Check if the Request is a POST request, in order to search for an existing coverage
        Resource directory = null;
        if (isPost && storeName != null) {
            // Check if the coverage already exists
            CoverageStoreInfo coverage = catalog.getCoverageStoreByName(storeName);
            if (coverage != null) {
                if (workspaceName == null
                        || coverage.getWorkspace().getName().equalsIgnoreCase(workspaceName)) {
                    // If the coverage exists then the associated directory is defined by its URL
                    directory =
                            Resources.fromPath(
                                    URLs.urlToFile(new URL(coverage.getURL())).getPath(),
                                    catalog.getResourceLoader().get(""));
                }
            }
        }
        // If the directory has not been found then it is created directly
        if (directory == null) {
            directory =
                    catalog.getResourceLoader().get(Paths.path("data", workspaceName, storeName));
        }

        // Selection of the original ROOT directory path
        StringBuilder root = new StringBuilder(directory.path());
        // StoreParams to use for the mapping.
        Map<String, String> storeParams = new HashMap<>();
        // Listing of the available pathMappers
        List<RESTUploadPathMapper> mappers =
                GeoServerExtensions.extensions(RESTUploadPathMapper.class);
        // Mapping of the root directory
        for (RESTUploadPathMapper mapper : mappers) {
            mapper.mapStorePath(root, workspaceName, storeName, storeParams);
        }
        directory = Resources.fromPath(root.toString());
        return directory;
    }

    @Override
    protected Resource findPrimaryFile(Resource directory, String format) {
        if ("shp".equalsIgnoreCase(format) || "h2".equalsIgnoreCase(format)) {
            // special case for shapefiles, since shapefile datastore can handle directories just
            // return the directory, this handles the case of a user uploading a zip with multiple
            // shapefiles in it and the same happens for H2
            return directory;
        } else {
            return super.findPrimaryFile(directory, format);
        }
    }

    void updateParameters(
            DataStoreInfo info,
            NamespaceInfo namespace,
            DataAccessFactory factory,
            Resource uploadedFile) {
        Map connectionParameters = info.getConnectionParameters();
        updateParameters(connectionParameters, factory, uploadedFile);

        connectionParameters.put("namespace", namespace.getURI());
        // ensure the parameters are valid
        if (!factory.canProcess(connectionParameters)) {
            // TODO: log the parameters at the debug level
            throw new RestException(
                    "Unable to configure datastore, bad parameters.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    void updateParameters(
            Map connectionParameters, DataAccessFactory factory, Resource uploadedFile) {
        File f = Resources.find(uploadedFile);
        for (DataAccessFactory.Param p : factory.getParametersInfo()) {
            // the nasty url / file hack
            if (File.class == p.type || URL.class == p.type) {

                if ("directory".equals(p.key)) {
                    // set the value to be the directory
                    f = f.getParentFile();
                }

                // convert to the required type
                // TODO: use geotools converter
                Object converted = null;
                if (URI.class.equals(p.type)) {
                    converted = f.toURI();
                } else if (URL.class.equals(p.type)) {
                    converted = URLs.fileToUrl(f);
                }

                if (converted != null) {
                    connectionParameters.put(p.key, converted);
                } else {
                    connectionParameters.put(p.key, f);
                }

                continue;
            }

            if (p.required) {
                try {
                    p.lookUp(connectionParameters);
                } catch (Exception e) {
                    // set the sample value
                    connectionParameters.put(p.key, p.sample);
                }
            }
        }

        // handle H2 and SpatiaLite special cases
        if (factory.getDisplayName().equalsIgnoreCase("SpatiaLite")) {
            connectionParameters.put(JDBCDataStoreFactory.DATABASE.getName(), f.getAbsolutePath());
        } else if (factory.getDisplayName().equalsIgnoreCase("H2")) {
            // we need to extract the H2 database name
            String databaseFile = f.getAbsolutePath();
            if (f.isDirectory()) {
                // if the user uploaded a ZIP file we need to get the database file inside
                Optional<Resource> found =
                        Resources.list(
                                        uploadedFile,
                                        resource -> resource.name().endsWith("data.db"))
                                .stream()
                                .findFirst();
                if (!found.isPresent()) {
                    // ouch no database file found just throw an exception
                    throw new RestException(
                            String.format(
                                    "H2 database file could not be found in directory '%s'.",
                                    f.getAbsolutePath()),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
                // we found the database file get the absolute path
                databaseFile = found.get().file().getAbsolutePath();
            }
            // apply the H2 file regex pattern
            Matcher matcher = H2_FILE_PATTERN.matcher(databaseFile);
            if (!matcher.matches()) {
                // strange the database file is not ending in data.db
                throw new RestException(
                        String.format("Invalid H2 database file '%s'.", databaseFile),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            connectionParameters.put(JDBCDataStoreFactory.DATABASE.getName(), matcher.group(1));
        }
    }

    void autoCreateParameters(
            DataStoreInfo info, NamespaceInfo namespace, DataAccessFactory factory) {
        Map defaultParams =
                dataStoreFactoryToDefaultParams.get(factory.getClass().getCanonicalName());
        if (defaultParams == null) {
            throw new RuntimeException(
                    "Unable to auto create parameters for " + factory.getDisplayName());
        }

        HashMap params = new HashMap(defaultParams);

        // replace any replacable parameters
        String dataDirRoot = catalog.getResourceLoader().getBaseDirectory().getAbsolutePath();
        for (Object o : params.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            if (e.getValue() instanceof String) {
                String string = (String) e.getValue();
                string =
                        string.replace("@NAME@", info.getName()).replace("@DATA_DIR@", dataDirRoot);
                e.setValue(string);
            }
        }

        // TODO: namespace?
        params.put("namespace", namespace.getURI());
        info.getConnectionParameters().putAll(params);
    }

    private boolean sameTypeAndUrl(Map sourceParams, Map targetParams) {
        boolean sameType =
                sourceParams.get("dbtype") != null
                        && targetParams.get("dbtype") != null
                        && sourceParams.get("dbtype").equals(targetParams.get("dbtype"));
        boolean sameUrl =
                sourceParams.get("url") != null
                        && targetParams.get("url") != null
                        && sourceParams.get("url").equals(targetParams.get("url"));

        return sameType && sameUrl;
    }
}
