/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.ResourceInfo;

import java.util.*;

import static gmx.iderc.geoserver.tjs.catalog.TJSCascadeRemovalReporter.ModificationType.DELETE;

@SuppressWarnings("serial")
public class TJSConfirmRemovalPanel extends Panel {
    List<? extends TJSCatalogObject> roots;

    public TJSConfirmRemovalPanel(String id, TJSCatalogObject... roots) {
        this(id, Arrays.asList(roots));
    }

    public TJSConfirmRemovalPanel(String id, List<? extends TJSCatalogObject> roots) {
        super(id);
        this.roots = roots;

        // track objects that could not be removed
        Map<TJSCatalogObject, StringResourceModel> notRemoved = new HashMap();

        // collect the objects that will be removed (besides the roots)
        TJSCatalog catalog = TJSExtension.getTJSCatalog();
        TJSCascadeRemovalReporter visitor = new TJSCascadeRemovalReporter(catalog);
        for (TJSCatalogObject root : roots) {
            StringResourceModel reason = canRemove(root);
            if (reason != null) {
                notRemoved.put(root, reason);
            } else {
                root.accept(visitor);
            }
        }
        //quitar los roots que no se pueden borrar, Alvaro Javier Fuentes Suarez
        roots.removeAll(notRemoved.keySet());
        //quitar los roots del resultado del visitador (para no borrar dos veces!),
        //Alvaro Javier Fuentes Suarez
        visitor.removeAll(roots);
        //visitor.removeAll(notRemoved.keySet());

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        root.add(new Label("rootObjectNames", names(roots)));
        root.setVisible(!roots.isEmpty());
        add(root);

        // objects that could not be removed
        WebMarkupContainer nr = new WebMarkupContainer("notRemovedObjects");
        nr.add(notRemovedList(notRemoved));
        nr.setVisible(!notRemoved.isEmpty());
        add(nr);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
        List<TJSCatalogObject> cascaded = visitor.getObjects(TJSCatalogObject.class, DELETE);
        // remove the resources, they are cascaded, but won't be show in the UI
        for (Iterator it = cascaded.iterator(); it.hasNext(); ) {
            TJSCatalogObject TJSCatalogObject = (TJSCatalogObject) it.next();
            if (TJSCatalogObject instanceof ResourceInfo)
                it.remove();
        }
        removed.setVisible(cascaded.size() > 0);
        add(removed);

        // removed frameworks
        WebMarkupContainer removedFrameworks = new WebMarkupContainer("frameworksRemoved");
        removed.add(removedFrameworks);
        List<FrameworkInfo> frameworks = visitor.getObjects(FrameworkInfo.class, DELETE);
        if (frameworks.size() == 0)
            removedFrameworks.setVisible(false);
        removedFrameworks.add(new Label("frameworks", names(frameworks)));
        removed.add(removedFrameworks);//agregar donde es!, Alvaro Javier Fuentes Suarez

        // removed datasets
        WebMarkupContainer removedDatasets = new WebMarkupContainer("datasetsRemoved");
        removed.add(removedDatasets);
        List<DatasetInfo> datasets = visitor.getObjects(DatasetInfo.class, DELETE);
        if (datasets.size() == 0)
            removedDatasets.setVisible(false);
        removedDatasets.add(new Label("datasets", names(datasets)));
        removed.add(removedDatasets);//agregar donde es!, Alvaro Javier Fuentes Suarez

        // removed datastores
        WebMarkupContainer removetDatastores = new WebMarkupContainer("datastoresRemoved");
        removed.add(removetDatastores);
        List<DataStoreInfo> datastores = visitor.getObjects(DataStoreInfo.class, DELETE);
        if (datastores.size() == 0)
            removetDatastores.setVisible(false);
        removetDatastores.add(new Label("datasets", names(datastores)));
        removed.add(removetDatastores);//agregar donde es!, Alvaro Javier Fuentes Suarez

    }

    public List<? extends TJSCatalogObject> getRoots() {
        return roots;
    }

    String names(List objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if (i < (objects.size() - 1))
                sb.append(", ");
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return (String) BeanUtils.getProperty(object, "name");
        } catch (Exception e) {
            throw new RuntimeException("A catalog object that does not have " +
                                               "a 'name' property has been used, this is unexpected", e);
        }
    }

    ListView notRemovedList(final Map<TJSCatalogObject, StringResourceModel> notRemoved) {
        List<TJSCatalogObject> items = new ArrayList(notRemoved.keySet());
        ListView lv = new ListView("notRemovedList", items) {

            @Override
            protected void populateItem(ListItem item) {
                TJSCatalogObject object = (TJSCatalogObject) item.getModelObject();
                StringResourceModel reason = notRemoved.get(object);
                item.add(new Label("name", name(object)));
                item.add(new Label("reason", reason));
            }
        };
        return lv;
    }

    /**
     * Determines if a catalog object can be removed or not.
     * <p>
     * This method returns non-null in cases where the object should not be be
     * removed. The return value should be a description or reason of why the
     * object can not be removed.
     * </p>
     *
     * @param info The object to be removed.
     * @return A message stating why the object can not be removed, or null to
     *         indicate that it can be removed.
     */
    protected StringResourceModel canRemove(TJSCatalogObject info) {
        return null;
    }
}
