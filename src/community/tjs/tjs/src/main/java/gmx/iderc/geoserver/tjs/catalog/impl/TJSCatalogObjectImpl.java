/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogObject;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogVisitor;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import java.io.Serializable;

/**
 * @author capote
 */
public class TJSCatalogObjectImpl implements TJSCatalogObject {

    String id;
    String name;
    String description;
    boolean enabled;

    transient TJSCatalog catalog;

    public TJSCatalogObjectImpl() {
        // dummy implementation
    }

    public TJSCatalogObjectImpl(TJSCatalog catalog) {
        this.catalog = catalog;
    }

    public String getId() {
        if (id == null) {
            id = TJSCatalogFactoryImpl.getIdForObject(this);
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Name getQualifiedName() {
        return new NameImpl(getName());
    }

    public void accept(TJSCatalogVisitor visitor) {
        visitor.visit(this);
    }

    public void loadDefault() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public TJSCatalog getCatalog() {
        return catalog;
    }

    public void setCatalog(TJSCatalog catalog) {
        this.catalog = catalog;
    }

}
