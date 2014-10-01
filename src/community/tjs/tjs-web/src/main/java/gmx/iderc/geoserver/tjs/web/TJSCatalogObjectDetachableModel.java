/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogObject;
import org.apache.wicket.model.IModel;

/**
 * @author root
 */
public class TJSCatalogObjectDetachableModel implements IModel {
    transient TJSCatalogObject catalogObject;
    String id;

    public TJSCatalogObjectDetachableModel(TJSCatalogObject catalogObject) {
        setObject(catalogObject);
    }

    public Object getObject() {
        if (catalogObject == null) {
            catalogObject = TJSExtension.getTJSCatalog().getCatalogObject(id);
        }
        return catalogObject;
    }

    public void setObject(Object paramT) {
        this.catalogObject = (TJSCatalogObject) paramT;
        this.id = this.catalogObject.getId();
    }

    public void detach() {
        this.catalogObject = null;
    }

}
