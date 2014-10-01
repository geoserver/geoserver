/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.catalog;

/**
 * Visitor for catalog objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface TJSCatalogVisitor {

    /**
     * Visits the catalog
     */
    void visit(TJSCatalog catalog);

    /**
     * Visits a framework.
     */
    void visit(FrameworkInfo framework);

    /**
     * Visits a dataStore.
     */
    void visit(DataStoreInfo dataStore);

    /**
     * Visits a dataset.
     */
    void visit(DatasetInfo dataset);

    /**
     * Visita la informacion sobre un mapa previamente creado
     *
     * @param joinedMap
     * @author Alvaro Javier
     */
    void visit(JoinedMapInfo joinedMap);

    void visit(TJSCatalogObject object);
}
