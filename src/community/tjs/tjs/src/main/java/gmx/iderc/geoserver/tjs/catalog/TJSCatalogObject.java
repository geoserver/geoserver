/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

import org.opengis.feature.type.Name;

/**
 * @author root
 */
public interface TJSCatalogObject {

    String getId();

    void setId(String id);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean getEnabled();

    void setEnabled(boolean enabled);

    Name getQualifiedName();

    void accept(TJSCatalogVisitor visitor);

    public void loadDefault();

    TJSCatalog getCatalog();

    void setCatalog(TJSCatalog catalog);
}
