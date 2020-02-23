/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * The GeoServer catalog which provides access to meta information about the data served by
 * GeoServer.
 *
 * <p>The following types of metadata are stored:
 *
 * <ul>
 *   <li>namespaces and workspaces
 *   <li>coverage (raster) and data (vector) stores
 *   <li>coverages and feature resoures
 *   <li>styles
 * </ul>
 *
 * <h2>Workspaces</h2>
 *
 * <p>A workspace is a container for any number of stores. All workspaces can be obtained with the
 * {@link #getWorkspaces()}. A workspace is identified by its name ({@link
 * WorkspaceInfo#getName()}). A workspace can be looked up by its name with the {@link
 * #getWorkspaceByName(String)} method.
 *
 * <h2>Stores</h2>
 *
 * <p>The {@link #getStores(Class)} method provides access to all the stores in the catalog of a
 * specific type. For instance, the following would obtain all datstores from the catalog:
 *
 * <pre>
 *  //get all datastores
 *  List<DataStoreInfo> dataStores = catalog.getStores( DataStoreInfo.class );
 *  </pre>
 *
 * The methods {@link #getDataStores()} and {@link #getCoverageStores()} provide a convenience for
 * the two well known types.
 *
 * <p>A store is contained within a workspace (see {@link StoreInfo#getWorkspace()}). The {@link
 * #getStoresByWorkspace(WorkspaceInfo, Class)} method for only stores contained with a specific
 * workspace. For instance, the following would obtain all datastores store within a particular
 * workspace:
 *
 * <pre>
 *  //get a workspace
 *  WorkspaceInfo workspace = catalog.getWorkspace( "myWorkspace" );
 *
 *  //get all datastores in that workspace
 *  List<DataStoreInfo> dataStores = catalog.getStoresByWorkspace( workspace, DataStoreInfo.class );
 *  </pre>
 *
 * <h2>Resources</h2>
 *
 * <p>The {@link #getResources(Class)} method provides access to all resources in the catalog of a
 * particular type. For instance, to acess all feature types in the catalog:
 *
 * <pre>
 *  List<FeatureTypeInfo> featureTypes = catalog.getResources( FeatureTypeInfo.class );
 * </pre>
 *
 * The {@link #getFeatureTypes()} and {@link #getCoverages()} methods are a convenience for the well
 * known types.
 *
 * <p>A resource is contained within a namespace, therefore it is identified by a namespace uri,
 * local name pair. The {@link #getResourceByName(String, String, Class)} method provides access to
 * a resource by its namespace qualified name. The method {@link #getResourceByName(String, Class)}
 * provides access to a resource by its unqualified name. The latter method will do an exhaustive
 * search of all namespaces for a resource with the specified name. If only a single resoure with
 * the name is found it is returned. Some examples:
 *
 * <pre>
 *   //get a feature type by its qualified name
 *   FeatureTypeInfo ft = catalog.getResourceByName(
 *       "http://myNamespace.org", "myFeatureType", FeatureTypeInfo.class );
 *
 *   //get a feature type by its unqualified name
 *   ft = catalog.getResourceByName( "myFeatureType", FeatureTypeInfo.class );
 *
 *   //get all feature types in a namespace
 *   NamespaceInfo ns = catalog.getNamespaceByURI( "http://myNamespace.org" );
 *   List<FeatureTypeInfo> featureTypes = catalog.getResourcesByNamespace( ns, FeatureTypeINfo.class );
 *  </pre>
 *
 * <h2>Layers</h2>
 *
 * <p>A layers is used to publish a resource. The {@link #getLayers()} provides access to all layers
 * in the catalog. A layer is uniquely identified by its name. The {@link #getLayerByName(String)}
 * method provides access to a layer by its name. The {@link #getLayers(ResourceInfo)} return all
 * the layers publish a specific resource. Some examples:
 *
 * <pre>
 *  //get a layer by its name
 *  LayerInfo layer = catalog.getLayer( "myLayer" );
 *
 *  //get all the layers for a particualr feature type
 *  FeatureTypeInfo ft = catalog.getFeatureType( "http://myNamespace", "myFeatureType" );
 *  List<LayerInfo> layers = catalog.getLayers( ft );
 *
 * </pre>
 *
 * <h2>Modifing the Catalog</h2>
 *
 * <p>Catalog objects such as stores and resoures are mutable and can be modified. However, any
 * modifications made on an object do not apply until they are saved. For instance, consider the
 * following example of modifying a feature type:
 *
 * <pre>
 *  //get a feature type
 *  FeatureTypeInfo featureType = catalog.getFeatureType( "http://myNamespace.org", "myFeatureType" );
 *
 *  //modify it
 *  featureType.setBoundingBox( new Envelope(...) );
 *
 *  //save it
 *  catalog.save( featureType );
 * </pre>
 *
 * <p>
 *
 * <h2>Isolated Workspaces</h2>
 *
 * Is possible to request a catalog object using its workspace prefix or its namespace URI, the last
 * method will not work to retrieve the content of an isolated workspace unless in the context of a
 * virtual service belonging to that workspace.
 *
 * @author Justin Deoliveira, The Open Planning project
 */
@ParametersAreNonnullByDefault
public interface Catalog extends CatalogInfo {

    /** The reserved keyword used to identify the default workspace or the default store */
    public static String DEFAULT = "default";

    /** The data access facade. */
    CatalogFacade getFacade();

    /** The factory used to create catalog objects. */
    CatalogFactory getFactory();

    /** Adds a new store. */
    void add(StoreInfo store);

    /**
     * Validate a store.
     *
     * @param store the StoreInfo to be validated
     * @param isNew a boolean; if true then an existing store with the same name and workspace will
     *     cause a validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(StoreInfo store, boolean isNew);

    /** Removes an existing store. */
    void remove(StoreInfo store);

    /** Saves a store that has been modified. */
    void save(StoreInfo store);

    /**
     * Detaches the store from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    <T extends StoreInfo> T detach(T store);

    /**
     * Returns the store with the specified id.
     *
     * <p><tt>clazz</td> is used to determine the implementation of StoreInfo which should be
     * returned. An example which would return a data store.
     *
     * <pre>
     *   <code>
     * DataStoreInfo dataStore = catalog.getStore(&quot;id&quot;, DataStoreInfo.class);
     * </code>
     * </pre>
     *
     * @param id The id of the store.
     * @param clazz The class of the store to return.
     * @return The store matching id, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStore(String id, Class<T> clazz);

    /**
     * Returns the store with the specified name.
     *
     * <p><tt>clazz</td> is used to determine the implementation of StoreInfo which should be
     * returned. An example which would return a data store.
     *
     * <p>
     *
     * <pre>
     *   getStoreByName(null,name,clazz);
     * </pre>
     *
     * @param name The name of the store. The name can be {@code null} or {@link #DEFAULT} to
     *     retrieve the default store in the default workspace.
     * @param clazz The class of the store to return.
     * @return The store matching name, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz);

    /**
     * Returns the store with the specified name in the specified workspace.
     *
     * <p><tt>clazz</td> is used to determine the implementation of StoreInfo which should be
     * returned. An example which would return a data store.
     *
     * <p>
     *
     * <pre>
     *   <code>
     *   DataStoreInfo dataStore = catalog.getStore(&quot;workspaceName&quot;, &quot;name&quot;, DataStoreInfo.class);
     * </code>
     * </pre>
     *
     * @param workspaceName The name of the workspace containing the store. The name can be {@code
     *     null} or {@link #DEFAULT} to identify the default workspace.
     * @param name The name of the store. The name can be {@code null} or {@link #DEFAULT} to
     *     retrieve the default store in the specified workspace.
     * @param clazz The class of store to return.
     * @return The store matching name, or <code>null</code> if no such store e xists.
     */
    <T extends StoreInfo> T getStoreByName(String workspaceName, String name, Class<T> clazz);

    /**
     * Returns the store with the specified name in the specified workspace.
     *
     * <p><tt>clazz</td> is used to determine the implementation of StoreInfo which should be
     * returned. An example which would return a data store.
     *
     * <p>
     *
     * <pre>
     *   <code>
     *   WorkspaceInfo workspace = ...;
     *   DataStoreInfo dataStore = catalog.getStore(workspace, &quot;name&quot; , DataStoreInfo.class);
     * </code>
     * </pre>
     *
     * @param workspace The workspace containing the store.
     * @param name The name of the store. The name can be {@code null} or {@link #DEFAULT} to
     *     retrieve the default store in the default workspace.
     * @param clazz The class of store to return.
     * @return The store matching name, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz);

    /**
     * All stores in the catalog of the specified type.
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of stores returned. An example
     * which would return all data stores:
     *
     * <pre>
     *   <code>
     * catalog.getStores(DataStoreInfo.class);
     * </code>
     * </pre>
     */
    <T extends StoreInfo> List<T> getStores(Class<T> clazz);

    /**
     * All stores in the specified workspace of the given type.
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of stores returned. An example
     * which would return all data stores in a specific workspace:
     *
     * <pre>
     *   <code>
     * WorkspaceInfo workspace = ...;
     * List<DataStoreInfo> dataStores =
     *     catalog.getStores(workspace, DataStoreInfo.class);
     * </code>
     * </pre>
     *
     * @param workspace The workspace containing returned stores, may be null to specify the default
     *     workspace.
     * @param clazz The type of stores to lookup.
     */
    <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace, Class<T> clazz);

    /**
     * All stores in the specified workspace of the given type.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspaceByName( workspaceName );
     *   getStoresByWorkspace( ws , clazz );
     * </pre>
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of stores returned.
     *
     * @param workspaceName The name of the workspace containing returned store s, may be null to
     *     specify the default workspace.
     * @param clazz The type of stores to lookup.
     */
    <T extends StoreInfo> List<T> getStoresByWorkspace(String workspaceName, Class<T> clazz);

    /**
     * Returns a datastore matching a particular id, or <code>null</code> if no such data store
     * could be found.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getStore( id, DataStoreInfo.class );
     * </pre>
     */
    DataStoreInfo getDataStore(String id);

    /**
     * Returns a datastore matching a particular name in the default workspace,o * or <code>null
     * </code> if no such data store could be found.
     *
     * <p>This method is a convenience for:
     *
     * <pre>
     *  getDataStoreStoreByName(null,name);
     * </pre>
     */
    DataStoreInfo getDataStoreByName(String name);

    /**
     * Returns the datastore matching a particular name in the specified worksp ace, or <code>null
     * </code> if no such datastore could be found.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspace( workspaceName );
     *   return catalog.getDataStoreByName(ws,name);
     * </pre>
     *
     * @param name The name of the datastore.
     * @param workspaceName The name of the workspace containing the datastore, may be <code>null
     *     </code> to specify the default workspace.
     * @return The store matching the name, or null if no such store could be f ound.
     */
    DataStoreInfo getDataStoreByName(String workspaceName, String name);

    /**
     * Returns the datastore matching a particular name in the specified worksp ace, or <code>null
     * </code> if no such datastore could be found.
     *
     * @param name The name of the datastore.
     * @param workspace The workspace containing the datastore, may be <code>nu ll</code> to specify
     *     the default workspace.
     * @return The store matching the name, or null if no such store could be f ound.
     */
    DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name);

    /**
     * All data stores in the specified workspace.
     *
     * <p>This method is equivalent to:
     *
     * <pre>
     * getStoresByWorkspace( workspaceName, DataStoreInfo.class );
     * </pre>
     *
     * @param workspaceName The name of the workspace.
     */
    List<DataStoreInfo> getDataStoresByWorkspace(String workspaceName);

    /**
     * All data stores in the specified workspace.
     *
     * <p>This method is equivalent to:
     *
     * <pre>
     * getStoresByWorkspace( workspace, DataStoreInfo.class );
     * </pre>
     *
     * @param workspace The name of the workspace.
     */
    List<DataStoreInfo> getDataStoresByWorkspace(WorkspaceInfo workspace);

    /**
     * All data stores in the catalog.
     *
     * <p>The resulting list should not be modified to add or remove stores, the {@link
     * #add(StoreInfo)} and {@link #remove(StoreInfo)} are used for this purpose.
     */
    List<DataStoreInfo> getDataStores();

    /** The default datastore for the specified workspace */
    DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace);

    /** Sets the default data store in the specified workspace */
    void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore);

    /**
     * Returns a coverage store matching a particular id, or <code>null</code> if no such coverage
     * store could be found.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getStore( id, CoverageStoreInfo.class );
     * </pre>
     */
    CoverageStoreInfo getCoverageStore(String id);

    /**
     * Returns a coverage store matching a particular name, or <code>null</code> if no such coverage
     * store could be found.
     *
     * <p>This method is a convenience for: <code>
     *   <pre>
     * getCoverageStoreByName(name, CoverageStoreInfo.class)
     * </pre>
     * </code>
     */
    CoverageStoreInfo getCoverageStoreByName(String name);

    /**
     * Returns the coveragestore matching a particular name in the specified workspace, or <code>
     * null</code> if no such coveragestore could be found.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspace( workspaceName );
     *   return catalog.getCoverageStoreByName(ws,name);
     * </pre>
     *
     * @param name The name of the coveragestore.
     * @param workspaceName The name of the workspace containing the coveragestore, may be <code>
     *     null</code> to specify the default workspace.
     * @return The store matching the name, or null if no such store could be found.
     */
    CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name);

    /**
     * Returns the coverageStore matching a particular name in the specified workspace, or <code>
     * null</code> if no such coverageStore could be found.
     *
     * @param name The name of the coverageStore.
     * @param workspace The workspace containing the coverageStore, may be <code>null</code> to
     *     specify the default workspace.
     * @return The store matching the name, or null if no such store could be found.
     */
    CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name);

    /**
     * All coverage stores in the specified workspace.
     *
     * <p>This method is equivalent to:
     *
     * <pre>
     * getStoresByWorkspace( workspaceName, CoverageStoreInfo.class );
     * </pre>
     *
     * @param workspaceName The name of the workspace.
     */
    List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName);

    /**
     * All coverage stores in the specified workspace.
     *
     * <p>This method is equivalent to:
     *
     * <pre>
     * getStoresByWorkspace( workspace, CoverageStoreInfo.class );
     * </pre>
     *
     * @param workspace The name of the workspace.
     */
    List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace);

    /**
     * All coverage stores in the catalog.
     *
     * <p>The resulting list should not be modified to add or remove stores, the {@link
     * #add(StoreInfo)} and {@link #remove(StoreInfo)} are used for this purpose.
     */
    List<CoverageStoreInfo> getCoverageStores();

    /**
     * Returns the resource with the specified id.
     *
     * <p><tt>clazz</td> is used to determine the implementation of ResourceInfo which should be
     * returned. An example which would return a feature type.
     *
     * <pre>
     *   <code>
     * FeatureTypeInfo ft = catalog.getResource(&quot;id&quot;, FeatureTypeInfo.class);
     * </code>
     * </pre>
     *
     * @param id The id of the resource.
     * @param clazz The class of the resource to return.
     * @return The resource matching id, or <code>null</code> if no such resource exists.
     */
    <T extends ResourceInfo> T getResource(String id, Class<T> clazz);

    /**
     * Looks up a resource by qualified name.
     *
     * <p><tt>ns</tt> may be specified as a namespace prefix or uri.
     *
     * <p><tt>clazz</td> is used to determine the implementation of ResourceInfo which should be
     * returned.
     *
     * @param ns The prefix or uri to which the resource belongs, may be <code>null</code>.
     * @param name The name of the resource.
     * @param clazz The class of the resource.
     * @return The resource matching the name, or <code>null</code> if no such resource exists.
     */
    <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz);

    /**
     * Looks up a resource by qualified name.
     *
     * <p><tt>clazz</td> is used to determine the implementation of ResourceInfo which should be
     * returned.
     *
     * @param ns The namespace to which the resource belongs, may be <code>null</code> to specify
     *     the default namespace.
     * @param name The name of the resource.
     * @param clazz The class of the resource.
     * @return The resource matching the name, or <code>null</code> if no such resource exists.
     */
    <T extends ResourceInfo> T getResourceByName(NamespaceInfo ns, String name, Class<T> clazz);

    /**
     * Looks up a resource by qualified name.
     *
     * <p><tt>clazz</td> is used to determine the implementation of ResourceInfo which should be
     * returned. Isolated workspaces content will be ignored unless in the context of a matching
     * virtual service.
     *
     * @param <T>
     * @param name The qualified name.
     */
    <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz);

    /**
     * Looks up a resource by its unqualified name.
     *
     * <p>The lookup rules used by this method are as follows:
     *
     * <ul>
     *   <li>If a resource in the default namespace is found matching the specified name, it is
     *       returned.
     *   <li>If a single resource among all non-default namespaces is found matching the the
     *       specified name, it is returned.
     * </ul>
     *
     * Care should be taken when using this method, use of {@link #getResourceByName(String, String,
     * Class)} is preferred.
     *
     * @param name The name of the resource.
     * @param clazz The type of the resource.
     */
    <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz);

    /** Adds a new resource. */
    void add(ResourceInfo resource);

    /**
     * Validate a resource.
     *
     * @param resource the ResourceInfo to be validated
     * @param isNew a boolean; if true then an existing resource with the same name and store will
     *     cause a validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(ResourceInfo resource, boolean isNew);

    /** Removes an existing resource. */
    void remove(ResourceInfo resource);

    /** Saves a resource which has been modified. */
    void save(ResourceInfo resource);

    /**
     * Detatches the resource from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    <T extends ResourceInfo> T detach(T resource);

    /**
     * All resources in the catalog of the specified type.
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of resources returned. An example
     * which would return all feature types:
     *
     * <pre>
     *   <code>
     * catalog.getResources(FeatureTypeInfo.class);
     * </code>
     * </pre>
     */
    <T extends ResourceInfo> List<T> getResources(Class<T> clazz);

    /**
     * All resources in the specified namespace of the specified type.
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of resources returned. An example
     * which would return all feature types:
     *
     * @param namespace The namespace.
     * @param clazz The class of resources returned.
     * @return List of resources of the specified type in the specified namespace.
     */
    <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz);

    /**
     * All resources in the specified namespace of the specified type.
     *
     * <p>The <tt>namespace</tt> may specify the prefix, or the uri of the namespace.
     *
     * <p>The <tt>clazz</tt> parameter is used to filter the types of resources returned. An example
     * which would return all feature types:
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * NamespaceInfo ns = getNamespace( namespace );
     * return getResourcesByNamespace(ns,clazz);
     * </pre>
     *
     * Isolated workspaces content will be ignored unless in the context of a matching virtual
     * service.
     *
     * @param namespace The namespace.
     * @param clazz The class of resources returned.
     * @return List of resources of the specified type in the specified namespace.
     */
    <T extends ResourceInfo> List<T> getResourcesByNamespace(String namespace, Class<T> clazz);

    /**
     * Returns the resource with the specified name originating from the store.
     *
     * @param store The store.
     * @param name The name of the resource.
     * @param clazz The class of resource.
     */
    <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz);

    /**
     * All resources which originate from the specified store, of the specified type.
     *
     * @param store The store to obtain resources from.
     * @param clazz The class of resources returned.
     * @return List of resources of the specified type from the specified store
     */
    <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz);

    /**
     * Returns the feature type matching a particular id, or <code>null</code> if no such feature
     * type could be found.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResource( id, FeatureTypeInfo.class );
     * </pre>
     *
     * @return The feature type matching the id, or <code>null</code> if no such resource exists.
     */
    FeatureTypeInfo getFeatureType(String id);

    /**
     * Looks up a feature type by qualified name.
     *
     * <p><tt>ns</tt> may be specified as a namespace prefix or uri.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName( ns, name, FeatureTypeInfo.class );
     * </pre>
     *
     * @param ns The prefix or uri to which the feature type belongs, may be <code>null</code> to
     *     specify the default namespace.
     * @param name The name of the feature type.
     * @return The feature type matching the name, or <code>null</code> if no such resource exists.
     */
    FeatureTypeInfo getFeatureTypeByName(String ns, String name);

    /**
     * Looks up a feature type by qualified name.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName( ns, name, FeatureTypeInfo.class );
     * </pre>
     *
     * @param ns The namespace to which the feature type belongs, may be <code>null</code> to
     *     specify the default namespace.
     * @param name The name of the feature type.
     * @return The feature type matching the name, or <code>null</code> if no such resource exists.
     */
    FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name);

    /**
     * Looks up a feature type by qualified name.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName( name, FeatureTypeInfo.class );
     * </pre>
     *
     * Isolated workspaces content will be ignored unless in the context of a matching virtual
     * service.
     *
     * @param name The qualified name.
     */
    FeatureTypeInfo getFeatureTypeByName(Name name);

    /**
     * Looks up a feature type by an unqualified name.
     *
     * <p>The lookup rules used by this method are as follows:
     *
     * <ul>
     *   <li>If a feature type in the default namespace is found matching the specified name, it is
     *       returned.
     *   <li>If a single feature type among all non-default namespaces is found matching the the
     *       specified name, it is returned.
     * </ul>
     *
     * Care should be taken when using this method, use of {@link #getFeatureTypeByName(String,
     * String)} is preferred.
     *
     * @param name The name of the feature type.
     * @return The single feature type matching the specified name, or <code>null</code> if either
     *     none could be found or multiple were found.
     */
    FeatureTypeInfo getFeatureTypeByName(String name);

    /**
     * ALl feature types in the catalog.
     *
     * <p>This method is a convenience for:
     *
     * <pre>
     * 	<code>
     * getResources(FeatureTypeInfo.class);
     * </code>
     * </pre>
     *
     * <p>The resulting list should not be used to add or remove resources from the catalog, the
     * {@link #add(ResourceInfo)} and {@link #remove(ResourceInfo)} methods are used for this
     * purpose.
     */
    List<FeatureTypeInfo> getFeatureTypes();

    /**
     * All feature types in the specified namespace.
     *
     * <p>This method is a convenience for: <code>
     *   <pre>
     * getResourcesByNamespace(namespace, FeatureTypeInfo.class);
     * </pre>
     * </code>
     *
     * @param namespace The namespace of feature types to return.
     * @return All the feature types in the specified namespace.
     */
    List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace);

    /**
     * Returns the feature type with the specified name which is part of the sp ecified data store.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     *  return getResourceByStore(dataStore,name,FeatureTypeInfo.class);
     *  </pre>
     *
     * @param dataStore The data store.
     * @param name The feature type name.
     * @return The feature type, or <code>null</code> if no such feature type e xists.
     */
    FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name);

    /**
     * All feature types which originate from the specified datastore.
     *
     * @param store The datastore.
     * @return A list of feature types which originate from the datastore.
     */
    List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store);

    /**
     * Returns the coverage matching a particular id, or <code>null</code> if no such coverage could
     * be found.
     *
     * <p>This method is a convenience for:
     *
     * <pre>
     * getResource( id, CoverageInfo.class );
     * </pre>
     */
    CoverageInfo getCoverage(String id);

    /**
     * Looks up a coverage by qualified name.
     *
     * <p><tt>ns</tt> may be specified as a namespace prefix or uri.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName(ns,name,CoverageInfo.class);
     * </pre>
     *
     * Isolated workspaces content will be ignored unless in the context of a matching virtual
     * service.
     *
     * @param ns The prefix or uri to which the coverage belongs, may be <code>null</code>.
     * @param name The name of the coverage.
     * @return The coverage matching the name, or <code>null</code> if no such resource exists.
     */
    CoverageInfo getCoverageByName(String ns, String name);

    /**
     * Looks up a coverage by qualified name.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName(ns,name,CoverageInfo.class);
     * </pre>
     *
     * @param ns The namespace to which the coverage belongs, may be <code>null</code>.
     * @param name The name of the coverage.
     * @return The coverage matching the name, or <code>null</code> if no such resource exists.
     */
    CoverageInfo getCoverageByName(NamespaceInfo ns, String name);

    /**
     * Looks up a coverage by qualified name.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * getResourceByName(name,CoverageInfo.class);
     * </pre>
     *
     * Isolated workspaces content will be ignored unless in the context of a matching virtual
     * service.
     *
     * @param name The qualified name.
     */
    CoverageInfo getCoverageByName(Name name);

    /**
     * Looks up a coverage by an unqualified name.
     *
     * <p>The lookup rules used by this method are as follows:
     *
     * <ul>
     *   <li>If a coverage in the default namespace is found matching the specified name, it is
     *       returned.
     *   <li>If a single coverage among all non-default namespaces is found matching the the
     *       specified name, it is returned.
     * </ul>
     *
     * Care should be taken when using this method, use of {@link #getCoverageByName(String,
     * String)} is preferred.
     *
     * @param name The name of the coverage.
     * @return The single coverage matching the specified name, or <code>null</code> if either none
     *     could be found or multiple were found.
     */
    CoverageInfo getCoverageByName(String name);

    /**
     * All coverages in the catalog.
     *
     * <p>This method is a convenience for:
     *
     * <pre>
     * 	<code>
     * getResources(CoverageInfo.class);
     * </code>
     * </pre>
     *
     * <p>This method should not be used to add or remove coverages from the catalog. The {@link
     * #add(ResourceInfo)} and {@link #remove(ResourceInfo)} methods are used for this purpose.
     */
    List<CoverageInfo> getCoverages();

    /**
     * All coverages in the specified namespace.
     *
     * <p>This method is a convenience for: <code>
     *   <pre>
     * getResourcesByNamespace(namespace, CoverageInfo.class);
     * </pre>
     * </code>
     *
     * @param namespace The namespace of coverages to return.
     * @return All the coverages in the specified namespace.
     */
    List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace);

    /**
     * Returns the coverage with the specified name which is part of the specified coverage store.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     *  return getResourceByStore(coverageStore,name,CoverageInfo.class);
     *  </pre>
     *
     * @param coverageStore The coverage store.
     * @param name The coverage name.
     * @return The coverage, or <code>null</code> if no such coverage exists.
     */
    CoverageInfo getCoverageByCoverageStore(CoverageStoreInfo coverageStore, String name);

    /**
     * All coverages which originate from the specified coveragestore.
     *
     * @param store The coveragestore.
     * @return A list of coverages which originate from the coveragestore.
     */
    List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store);

    /** Adds a new layer. */
    void add(LayerInfo layer);

    /**
     * Validate a layer.
     *
     * @param layer the LayerInfo to be validated
     * @param isNew a boolean; if true then an existing layer with the same name will cause a
     *     validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(LayerInfo layer, boolean isNew);

    /** Removes an existing layer. */
    void remove(LayerInfo layer);

    /** Saves a layer which has been modified. */
    void save(LayerInfo layer);

    /**
     * Detatches the layer from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    LayerInfo detach(LayerInfo layer);

    /** All coverages which are part of the specified store. */
    List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store);

    /**
     * Returns the layer matching a particular id, or <code>null</code> if no such layer could be
     * found.
     */
    LayerInfo getLayer(String id);

    /**
     * Returns the layer matching a particular name, or <code>null</code> if no such layer could be
     * found.
     */
    LayerInfo getLayerByName(String name);

    /**
     * Returns the layer matching a particular qualified name.
     *
     * <p>Isolated workspaces content will be ignored unless in the context of a matching virtual
     * service.
     */
    LayerInfo getLayerByName(Name name);

    /**
     * All layers in the catalog.
     *
     * <p>The resulting list should not be used to add or remove layers to or from the catalog, the
     * {@link #add(LayerInfo)} and {@link #remove(LayerInfo)} methods are used for this purpose.
     */
    List<LayerInfo> getLayers();

    /**
     * All layers in the catalog that publish the specified resource.
     *
     * @param resource The resource.
     * @return A list of layers for the resource, or an empty list.
     */
    List<LayerInfo> getLayers(ResourceInfo resource);

    /**
     * All layers which reference the specified style.
     *
     * @param style The style.
     * @return A list of layers which reference the style, or an empty list.
     */
    List<LayerInfo> getLayers(StyleInfo style);

    /** Adds a new map. */
    void add(MapInfo map);

    /** Removes an existing map. */
    void remove(MapInfo map);

    /** Saves a map which has been modified. */
    void save(MapInfo map);

    /**
     * Detatches the map from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    MapInfo detach(MapInfo map);

    /**
     * All maps in the catalog.
     *
     * <p>The resulting list should not be used to add or remove maps to or from the catalog, the
     * {@link #add(MapInfo)} and {@link #remove(MapInfo)} methods are used for this purpose.
     */
    List<MapInfo> getMaps();

    /**
     * Returns the map matching a particular id, or <code>null</code> if no such map could be found.
     */
    MapInfo getMap(String id);

    /**
     * Returns the map matching a particular name, or <code>null</code> if no such map could be
     * found.
     */
    MapInfo getMapByName(String name);

    /** Adds a layer group to the catalog. */
    void add(LayerGroupInfo layerGroup);

    /**
     * Validate a layergroup.
     *
     * @param layerGroup the LayerGroupInfo to be validated
     * @param isNew a boolean; if true then an existing layergroup with the same name will cause a
     *     validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(LayerGroupInfo layerGroup, boolean isNew);

    /** Removes a layer group from the catalog. */
    void remove(LayerGroupInfo layerGroup);

    /** Saves changes to a modified layer group. */
    void save(LayerGroupInfo layerGroup);

    /**
     * Detatches the layer group from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    LayerGroupInfo detach(LayerGroupInfo layerGroup);

    /** All layer groups in the catalog. */
    List<LayerGroupInfo> getLayerGroups();

    /**
     * All layer groups in the specified workspace.
     *
     * @param workspaceName The name of the workspace containing layer groups.
     */
    List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName);

    /**
     * All layer groups in the specified workspace.
     *
     * @param workspace The workspace containing layer groups.
     */
    List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace);

    /**
     * Returns the layer group matching a particular id, or <code>null</code> if no such group could
     * be found.
     */
    LayerGroupInfo getLayerGroup(String id);

    /**
     * Returns the global layer group matching a particular name, or <code>null</code> if no such
     * style could be found.
     *
     * <p>If {@code prefixedName} contains a workspace name prefix (like in {@code topp:tasmania},
     * the layer group will be looked up on that specific workspace ({@code topp}), otherwise it is
     * assumed a global (with no workspace) layer group.
     *
     * @param name the name of the layer group, may include a workspace name prefix or not.
     * @return the global layer group matching a particular name, or <code>null</code> if no such
     *     group could be found.
     */
    LayerGroupInfo getLayerGroupByName(String name);

    /**
     * Returns the layer group matching a particular name in the specified workspace, or <code>null
     * </code> if no such layer group could be found.
     *
     * @param workspaceName The name of the workspace containing the layer group, {@code null} is
     *     allowed, meaning to look up for a global layer group
     * @param name The name of the layer group to return.
     */
    LayerGroupInfo getLayerGroupByName(String workspaceName, String name);

    /**
     * Returns the layer group matching a particular name in the specified workspace, or <code>null
     * </code> if no such layer group could be found.
     *
     * @param workspace The workspace containing the layer group, {@code null} is allowed, meaning
     *     to look up for a global layer group.
     * @param name The name of the layer group to return.
     */
    LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name);

    /** Adds a new style. */
    void add(StyleInfo style);

    /**
     * Validate a style.
     *
     * @param style the StyleInfo to be validated
     * @param isNew a boolean; if true then an existing style with the same name will cause a
     *     validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(StyleInfo style, boolean isNew);

    /** Removes a style. */
    void remove(StyleInfo style);

    /** Saves a style which has been modified. */
    void save(StyleInfo style);

    /**
     * Detatches the style from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    StyleInfo detach(StyleInfo style);

    /**
     * Returns the style matching a particular id, or <code>null</code> if no such style could be
     * found.
     */
    StyleInfo getStyle(String id);

    /**
     * Returns the style matching a particular name in the specified workspace, or <code>null</code>
     * if no such style could be found.
     *
     * @param workspaceName The name of the workspace containing the style, {@code null} stands for
     *     a global style.
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(String workspaceName, String name);

    /**
     * Returns the style matching a particular name in the specified workspace, or <code>null</code>
     * if no such style could be found.
     *
     * @param workspace The workspace containing the style, {@code null} stands for a global style.
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(WorkspaceInfo workspace, String name);

    /**
     * Returns the global style matching a particular name, or <code>null</code> if no such style
     * could be found.
     *
     * <p>Note this is a convenient method for {@link #getStyleByName(WorkspaceInfo, String)} with a
     * {@code null} workspace argument.
     *
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(String name);

    /**
     * All styles in the catalog.
     *
     * <p>The resulting list should not be used to add or remove styles, the methods {@link
     * #add(StyleInfo)} and {@link #remove(StyleInfo)} are used for that purpose.
     */
    List<StyleInfo> getStyles();

    /**
     * All styles in the specified workspace.
     *
     * @param workspaceName The name of the workspace containing styles.
     */
    List<StyleInfo> getStylesByWorkspace(String workspaceName);

    /**
     * All styles in the specified workspace.
     *
     * @param workspace The workspace containing styles.
     */
    List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace);

    /** Adds a new namespace. */
    void add(NamespaceInfo namespace);

    /**
     * Validate a namespace.
     *
     * @param namespace the NamespaceInfo to be validated
     * @param isNew a boolean; if true then an existing namespace with the same prefix will cause a
     *     validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(NamespaceInfo namespace, boolean isNew);

    /** Removes an existing namespace. */
    void remove(NamespaceInfo namespace);

    /** Saves a namespace which has been modified. */
    void save(NamespaceInfo namespace);

    /**
     * Detatches the namespace from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    NamespaceInfo detach(NamespaceInfo namespace);

    /** Returns the namespace matching the specified id. */
    NamespaceInfo getNamespace(String id);

    /**
     * Looks up a namespace by its prefix.
     *
     * @see NamespaceInfo#getPrefix()
     * @param prefix The namespace prefix, or {@code null} or {@link #DEFAULT} to get the default
     *     namespace
     */
    NamespaceInfo getNamespaceByPrefix(String prefix);

    /**
     * Looks up a namespace by its uri.
     *
     * @see NamespaceInfo#getURI()
     */
    NamespaceInfo getNamespaceByURI(String uri);

    /** The default namespace of the catalog. */
    NamespaceInfo getDefaultNamespace();

    /**
     * Sets the default namespace of the catalog.
     *
     * @param defaultNamespace The defaultNamespace to set.
     */
    void setDefaultNamespace(NamespaceInfo defaultNamespace);

    /**
     * All namespaces in the catalog.
     *
     * <p>The resulting list should not be used to add or remove namespaces from the catalog, the
     * methods {@link #add(NamespaceInfo)} and {@link #remove(NamespaceInfo)} should be used for
     * that purpose.
     */
    List<NamespaceInfo> getNamespaces();

    /** Adds a new workspace */
    void add(WorkspaceInfo workspace);

    /**
     * Validate a workspace.
     *
     * @param workspace the WorkspaceInfo to be validated
     * @param isNew a boolean; if true then an existing workspace with the same name will cause a
     *     validation error.
     * @returns List<RuntimeException> non-empty if validation fails
     */
    ValidationResult validate(WorkspaceInfo workspace, boolean isNew);

    /** Removes an existing workspace. */
    void remove(WorkspaceInfo workspace);

    /** Saves changes to an existing workspace. */
    void save(WorkspaceInfo workspace);

    /**
     * Detatches the workspace from the catalog.
     *
     * <p>This method does not remove the object from the catalog, it "unnattaches" the object
     * resolving any proxies.
     *
     * <p>In the even the specified object does not exist in the catalog it itself should be
     * returned, this method should never return null.
     */
    WorkspaceInfo detach(WorkspaceInfo workspace);

    /** The default workspace for the catalog. */
    WorkspaceInfo getDefaultWorkspace();

    /** Sets the default workspace for the catalog. */
    void setDefaultWorkspace(WorkspaceInfo workspace);

    /**
     * All workspaces in the catalog.
     *
     * <p>The resulting list should not be used to add or remove workspaces from the catalog, the
     * methods {@link #add(WorkspaceInfo)} and {@link #remove(WorkspaceInfo)} should be used for
     * that purpose.
     */
    List<WorkspaceInfo> getWorkspaces();

    /** Returns a workspace by id, or <code>null</code> if no such workspace exists. */
    WorkspaceInfo getWorkspace(String id);

    /**
     * Returns a workspace by name, or <code>null</code> if no such workspace exists.
     *
     * @param name The name of the store, or {@code null} or {@link #DEFAULT} to get the default
     *     workspace
     */
    WorkspaceInfo getWorkspaceByName(String name);

    /** catalog listeners. */
    Collection<CatalogListener> getListeners();

    /** Adds a listener to the catalog. */
    void addListener(CatalogListener listener);

    /** Removes a listener from the catalog. */
    void removeListener(CatalogListener listener);

    /**
     * Fires the event for an object being added to the catalog.
     *
     * <p>This method should not be called by client code. It is meant to be called interally by the
     * catalog subsystem.
     */
    void fireAdded(CatalogInfo object);

    /**
     * Fires the event for an object being modified in the catalog.
     *
     * <p>This method should not be called by client code. It is meant to be called interally by the
     * catalog subsystem.
     */
    void fireModified(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues);

    /**
     * Fires the event for an object that was modified in the catalog.
     *
     * <p>This method should not be called by client code. It is meant to be called interally by the
     * catalog subsystem.
     */
    void firePostModified(
            CatalogInfo object, List<String> propertyNames, List oldValues, List newValues);

    /**
     * Fires the event for an object being removed from the catalog.
     *
     * <p>This method should not be called by client code. It is meant to be called interally by the
     * catalog subsystem.
     */
    void fireRemoved(CatalogInfo object);

    /**
     * Returns the pool or cache for resources.
     *
     * <p>This object is used to load physical resources like data stores, feature types, styles,
     * etc...
     */
    ResourcePool getResourcePool();

    /** Sets the resource pool reference. */
    void setResourcePool(ResourcePool resourcePool);

    /** Returns the loader for resources. */
    GeoServerResourceLoader getResourceLoader();

    /** Sets the resource loader reference. */
    void setResourceLoader(GeoServerResourceLoader resourceLoader);

    /** Disposes the catalog, freeing up any resources. */
    void dispose();

    /**
     * Returns the number of catalog objects of the requested type that match the given query
     * predicate.
     *
     * @param of the type of catalog objects to return. Super interfaces of concrete catalog objects
     *     are allowed (such as {@code StoreInfo.class} and {@code ResourceInfo.class}, although the
     *     more generic {@code Info.class} and {@code CatalogInfo.class} are not.
     * @param filter the query predicate, use {@link Filter#INCLUDE} if needed, {@code null} is not
     *     accepted.
     * @return the total number of catalog objects of the requested type that match the query
     *     predicate.
     */
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter);

    /**
     * Access to a single configuration object by the given predicate filter, fails if more than one
     * object satisfies the filter criteria.
     *
     * <p>Generally useful for query by id or name where name is known to be unique, either globally
     * or per workspace, although usage is not limited to those cases.
     *
     * <p>Examples:
     *
     * <pre>
     * <code>
     * import static org.geoserver.catalog.Predicates.propertyEquals;
     * import static org.geoserver.catalog.Predicates.and;
     * import static org.geoserver.catalog.Predicates.isNull;
     * ...
     * Catalog catalog = ...
     * LayerInfo layer = catalog.get(LayerInfo.class, propertyEquals("id", "layer1");
     * WorkspaceInfo ws = catalog.get(WorkspaceInfo.class, propertyEquals("resource.store.workspace.name", layer.getResource().getStore().getWorkspace().getName);
     * LayerGroupInfo wslg = catalog.get(LayerGroupInfo.class, and(propertyEquals("name", "lg1"), propertyEquals("workspace.name", "ws1"));
     * LayerGroupInfo globallg = catalog.get(LayerGroupInfo.class, and(propertyEquals("name", "lg1"), isNull("workspace.id"));
     * </code>
     * </pre>
     *
     * @return the single object of the given {@code type} that matches the given filter, or {@code
     *     null} if no object matches the provided filter.
     * @throws IllegalArgumentException if more than one object of type {@code T} match the provided
     *     filter.
     */
    <T extends CatalogInfo> T get(Class<T> type, Filter filter) throws IllegalArgumentException;

    /**
     * Returns an {@link Iterator} over the catalog objects of the requested type that match the
     * given query predicate, positioned at the specified {@code offset} and limited to the number
     * requested number of elements.
     *
     * <p>The returned iterator <strong>shall</strong> be closed once it is no longer needed, to
     * account for streaming implementations of this interface to release any needed resource such
     * as database or remote service connections. Example usage:
     *
     * <pre>
     * <code>
     * Catalog catalog = ...
     * Filter filter = ...
     * CloseableIterator<LayerInfo> iterator = catalog.list(LayerInfo.class, filter);
     * try{
     *   while(iterator.hasNext()){
     *     iterator.next();
     *   }
     * }finally{
     *   iterator.close();
     * }
     * </code>
     * </pre>
     *
     * @param of the type of catalog objects to return. Super interfaces of concrete catalog objects
     *     are allowed (such as {@code StoreInfo.class} and {@code ResourceInfo.class}, although the
     *     more generic {@code Info.class} and {@code CatalogInfo.class} are not.
     * @param filter the query predicate, use {@link Filter#INCLUDE} if needed, {@code null} is not
     *     accepted.
     * @return an iterator over the predicate matching catalog objects that must be closed once
     *     consumed
     * @throws IllegalArgumentException if {@code sortOrder != null} and {code !canSort(of,
     *     sortOrder)}
     */
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of, final Filter filter);

    /**
     * Returns an {@link Iterator} over the catalog objects of the requested type that match the
     * given query predicate, positioned at the specified {@code offset} and limited to the number
     * requested number of elements.
     *
     * <p>Through the optional {@code offset} and {@code count} arguments, this method allows for
     * paged queries over the catalog contents. Note that although there's no prescribed sort order,
     * catalog back end implementations must provide a natural sort order (either based on id or
     * otherwise), in order for paged queries to be consistent between calls for the same predicate.
     *
     * <p>The returned iterator <strong>shall</strong> be closed once it is no longer needed, to
     * account for streaming implementations of this interface to release any needed resource such
     * as database or remote service connections. Example usage:
     *
     * <pre>
     * <code>
     * Catalog catalog = ...
     * Filter filter = ...
     * CloseableIterator<LayerInfo> iterator = catalog.list(LayerInfo.class, filter);
     * try{
     *   while(iterator.hasNext()){
     *     iterator.next();
     *   }
     * }finally{
     *   iterator.close();
     * }
     * </code>
     * </pre>
     *
     * @param of the type of catalog objects to return. Super interfaces of concrete catalog objects
     *     are allowed (such as {@code StoreInfo.class} and {@code ResourceInfo.class}, although the
     *     more generic {@code Info.class} and {@code CatalogInfo.class} are not.
     * @param filter the query predicate, use {@link Predicates#ANY_TEXT} if needed, {@code null} is
     *     not accepted.
     * @param offset {@code null} to return an iterator starting at the first matching object,
     *     otherwise an integer {@code >= 0} to return an iterator positioned at the specified
     *     offset.
     * @param count {@code null} to return a non limited in number of elements iterator, an integer
     *     {@code >= 0} otherwise to specify the maximum number of elements the iterator shall
     *     return.
     * @param sortBy order for sorting
     * @return an iterator over the predicate matching catalog objects that must be closed once
     *     consumed
     * @throws IllegalArgumentException if {@code sortOrder != null} and [@code !canSort(of,
     *     sortOrder)}
     */
    public <T extends CatalogInfo> CloseableIterator<T> list(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy sortBy);

    /** Removes all the listeners which are instances of the specified class */
    public void removeListeners(Class listenerClass);

    /**
     * Return the catalog capabilities supported by this catalog. Normally this will correspond to
     * the capabilities supported by the used catalog facade.
     *
     * @return catalog supported capabilities
     */
    default CatalogCapabilities getCatalogCapabilities() {
        // return catalog default capabilities
        return new CatalogCapabilities();
    }
}
