/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.smartdataloader.data.store.ExclusionsDomainModelVisitor;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataFactory;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadataConfig;
import org.geoserver.smartdataloader.visitors.appschema.AppSchemaVisitor;
import org.geoserver.smartdataloader.visitors.gml.GmlSchemaVisitor;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.Parameter;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.config.AppSchemaDataAccessDTO;
import org.geotools.data.complex.config.DataAccessMap;
import org.geotools.data.complex.config.XMLConfigDigester;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

/** Smart AppSchema DataStore factory. */
public class SmartDataLoaderDataAccessFactory implements DataAccessFactory {

    public static final String GML_SUFFIX = "-gml.xsd";

    public static final String APPSCHEMA_SUFFIX = "-appschema.xml";

    static final Logger LOGGER = Logging.getLogger(SmartDataLoaderDataAccessFactory.class);

    public static final String DBTYPE_STRING = "smart-data-loader";

    public static final Param DBTYPE =
            new Param(
                    "dbtype",
                    String.class,
                    "Fixed value '" + DBTYPE_STRING + "'",
                    true,
                    DBTYPE_STRING,
                    Collections.singletonMap(Parameter.LEVEL, "program"));

    public static final Param NAMESPACE =
            new Param("namespace", URI.class, "Namespace prefix", false);
    public static final Param DATASTORE_NAME =
            new Param("datastorename", String.class, "Name of the datastore", true);
    public static final Param ROOT_ENTITY =
            new Param("root entity", String.class, "Root Entity", true);
    public static final Param DATASTORE_METADATA =
            new Param("datastore", String.class, "JDBC related DataStore", true);
    public static final Param DOMAIN_MODEL_EXCLUSIONS =
            new Param(
                    "excluded objects",
                    String.class,
                    "Excluded comma separated domainmodel object list",
                    false);

    @Override
    public String getDisplayName() {
        return "Smart Data Loader";
    }

    @Override
    public String getDescription() {
        return "AppSchema smart builder tool";
    }

    @Override
    public final Param[] getParametersInfo() {
        LinkedHashMap<String, DataAccessFactory.Param> map = new LinkedHashMap<>();
        setupParameters(map);
        return map.values().toArray(new Param[map.size()]);
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName(getDriverClassName());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean canProcess(Map<String, ?> params) {
        try {
            Object dbType = DBTYPE.lookUp(params);
            return DBTYPE_STRING.equals(dbType)
                    && DataUtilities.canProcess(params, getParametersInfo());
        } catch (Exception e) {
            // do nothing. based on AppSchemaDataAccessFactory code
        }
        return false;
    }

    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    private void setupParameters(Map<String, Param> parameters) {
        parameters.put(DBTYPE.key, DBTYPE);
        parameters.put(NAMESPACE.key, NAMESPACE);
        parameters.put(DATASTORE_NAME.key, DATASTORE_NAME);
        parameters.put(DATASTORE_METADATA.key, DATASTORE_METADATA);
        parameters.put(ROOT_ENTITY.key, ROOT_ENTITY);
        parameters.put(DOMAIN_MODEL_EXCLUSIONS.key, DOMAIN_MODEL_EXCLUSIONS);
    }

    private String getFilenamePrefix(Map<String, Serializable> params) throws IOException {
        String rootEntity = lookup(ROOT_ENTITY, params, String.class);
        String filenamePrefix = rootEntity;
        return filenamePrefix;
    }
    /**
     * Helper method that based on parameters, builds domainmodel, generates associated mapping
     * files and saves them in the workspace, returning the resulting DataStore.
     */
    private DataAccess<FeatureType, Feature> createDataStore(
            Map<String, Serializable> params,
            boolean hidden,
            DataAccessMap sourceDataStoreMap,
            final Set<AppSchemaDataAccess> registeredAppSchemaStores)
            throws IOException {
        // get parameters
        String datastoreName = lookup(DATASTORE_NAME, params, String.class);
        String excludedObjects = lookup(DOMAIN_MODEL_EXCLUSIONS, params, String.class);
        URI namespace = lookup(NAMESPACE, params, URI.class);
        // build domainmodel, and exclude elements based on excludedObjects list
        DomainModel dm = buildDomainModel(params, getExcludedObjectAsList(excludedObjects));
        // define filenames naming convention for documents to be saved
        String gmlFilename = getFilenamePrefix(params) + GML_SUFFIX;
        String appschemaFilename = getFilenamePrefix(params) + APPSCHEMA_SUFFIX;
        String target_namespace = namespace.toASCIIString();
        Catalog catalog = getGeoServer().getCatalog();
        String namespace_prefix = catalog.getNamespaceByURI(target_namespace).getPrefix();

        // populate appschema model visitor
        AppSchemaVisitor appSchemaDmv =
                new AppSchemaVisitor(namespace_prefix, target_namespace, "./" + gmlFilename);
        dm.accept(appSchemaDmv);
        // populate gml model visitor
        GmlSchemaVisitor gmlDmv = new GmlSchemaVisitor(namespace_prefix, target_namespace);
        dm.accept(gmlDmv);

        String pathToAppSchemaFolder =
                createAppSchemaFolder(target_namespace, catalog, datastoreName);
        // save datamodel related files
        File appschemaFile =
                saveMappingDocument(
                        pathToAppSchemaFolder, appschemaFilename, appSchemaDmv.getDocument());
        saveMappingDocument(pathToAppSchemaFolder, gmlFilename, gmlDmv.getDocument());

        // define datastore mappings and save datastore
        Set<FeatureTypeMapping> mappings;
        AppSchemaDataAccess dataStore;
        URL configFileUrl = URLs.fileToUrl(appschemaFile);
        XMLConfigDigester configReader = new XMLConfigDigester();
        AppSchemaDataAccessDTO config = configReader.parse(configFileUrl);
        List<String> includes = config.getIncludes();
        for (String include : includes) {
            params.put("url", buildIncludeUrl(configFileUrl, include));
            createDataStore(params, true, sourceDataStoreMap, registeredAppSchemaStores);
        }
        mappings = AppSchemaDataAccessConfigurator.buildMappings(config, sourceDataStoreMap);
        dataStore = new AppSchemaDataAccess(mappings, hidden);
        registeredAppSchemaStores.add(dataStore);
        return dataStore;
    }

    private String createAppSchemaFolder(
            String target_namespace, Catalog catalog, String datastoreName) {

        NamespaceInfo ni = catalog.getNamespaceByURI(target_namespace);
        WorkspaceInfo wi = catalog.getWorkspaceByName(ni.getName());
        GeoServerDataDirectory gdd =
                ((GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory"));
        Resource wiFolder = gdd.get(wi, "");
        // create folder called appschema-smart inside the datastore folder
        String pathname = wiFolder.toString() + "/" + datastoreName + "/appschema-mappings/";
        return pathname;
    }

    private List<String> getExcludedObjectAsList(String excludedObjects) {
        // convert excluded objects in a list
        String[] elements = {};
        if (excludedObjects != null) {
            elements = excludedObjects.split(",");
        }
        return Arrays.asList(elements);
    }

    /** Helper method that allows to create the DomainModel. */
    private DomainModel buildDomainModel(Map<String, Serializable> params, List<String> exclusions)
            throws IOException {
        DataStoreInfo jdbcDataStoreInfo = this.getDataStoreInfo(params);
        String rootEntity = lookup(ROOT_ENTITY, params, String.class);
        JDBCDataStoreFactory factory =
                new JDBCDataStoreFactoryFinder().getFactoryFromType(jdbcDataStoreInfo.getType());
        JDBCDataStore jdbcDataStore = null;
        DataStoreMetadata dsm = null;
        try {
            // TODO need to review (since it's forcing to get a JDBC datastore based on parameters.
            // Not sure what happen with JNDI)
            jdbcDataStore = factory.createDataStore(jdbcDataStoreInfo.getConnectionParameters());
            DataStoreMetadataConfig config =
                    new JdbcDataStoreMetadataConfig(
                            jdbcDataStore,
                            jdbcDataStoreInfo.getConnectionParameters().get("passwd").toString());
            dsm = (new DataStoreMetadataFactory()).getDataStoreMetadata(config);
        } catch (SQLException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Sql exception while retrieving metadata from the DB " + e.getMessage());
            StringBuilder sb = new StringBuilder("Error while acquiring JDBC connection");
            if (jdbcDataStoreInfo.getName() != null)
                sb.append(" from data store with name " + jdbcDataStoreInfo.getName());
            sb.append(" with message " + e.getMessage());
            throw new RuntimeException(sb.toString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving metadata from DB.");
        }

        if (dsm == null) {
            // cannot get datastoremetadata connected from which obtain db metadata
            throw new RuntimeException("Cannot connect to DB with defined parameters.");
        }
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName(rootEntity);
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        // apply exclusions to original model
        DomainModel newDomainModel = ExclusionsDomainModelVisitor.buildDomainModel(dm, exclusions);
        // release datastore before returning model
        jdbcDataStore.dispose();
        return newDomainModel;
    }

    /**
     * Helper method that allows to save an xml document representing a mapping in smart-appschema
     * folder.
     */
    private File saveMappingDocument(String pathname, String filename, Document mapping) {
        // create smart-appschema folder in workspace folder if it does not exists
        File directory = new File(pathname);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        // save document on filename
        File file = null;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transf = transformerFactory.newTransformer();
            transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transf.setOutputProperty(OutputKeys.INDENT, "yes");
            transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(mapping);
            file = new File(pathname + filename);
            StreamResult stream = new StreamResult(file);
            transf.transform(source, stream);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot save generated mapping in the workspace related folder.");
        }
        return file;
    }

    /** Method that allows to get a DataStoreInfo based on a set of parameters. */
    private DataStoreInfo getDataStoreInfo(Map<String, Serializable> params) throws IOException {
        String jdbcDataStoreId = lookup(DATASTORE_METADATA, params, String.class);
        Catalog c = getGeoServer().getCatalog();
        DataStoreInfo ds = c.getDataStore(jdbcDataStoreId);
        return ds;
    }

    /** Helper method to build urls in the context of a new AppSchemaDataAccess instance. */
    private String buildIncludeUrl(URL parentUrl, String include) {
        // first check if the include is already an URL
        String includeLowerCase = include.toLowerCase();
        if (includeLowerCase.startsWith("http:") || includeLowerCase.startsWith("file:")) {
            // we already have an URL, return it has is
            return include;
        }
        // we need to build an URL using the parent URL as a basis
        String url = parentUrl.toString();
        int index = url.lastIndexOf("/");
        if (index <= 0) {
            // we can't handle this situation let's raise an exception
            throw new RuntimeException(
                    String.format(
                            "Can't build include types '%s' URL using parent '%s' URL.",
                            include, url));
        }
        // build the include types URL
        url = url.substring(0, index + 1) + include;
        LOGGER.fine(
                String.format("Using URL '%s' to retrieve include types with '%s'.", url, include));
        return url;
    }

    /** Helper for getting values on parameters mappings. */
    <T> T lookup(Param param, Map<String, Serializable> params, Class<T> target)
            throws IOException {
        T result = target.cast(param.lookUp(params));
        if (result == null) {
            result = target.cast(param.getDefaultValue());
        }
        return result;
    }

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> createDataStore(
            Map<String, ?> params) throws IOException {
        final Set<AppSchemaDataAccess> registeredAppSchemaStores = new HashSet<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Serializable> parameters = (Map<String, Serializable>) params;
            return createDataStore(
                    parameters, false, new DataAccessMap(), registeredAppSchemaStores);
        } catch (Exception ex) {
            // dispose every already registered included datasource
            for (AppSchemaDataAccess appSchemaDataAccess : registeredAppSchemaStores) {
                appSchemaDataAccess.dispose();
            }
            throw ex;
        }
    }

    private GeoServer getGeoServer() {
        return (GeoServer) GeoServerExtensions.bean("geoServer");
    }
}
