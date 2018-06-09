/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class AbstractConfirmRemovalPanel<T> extends Panel {

    private static final long serialVersionUID = 1L;

    List<T> roots;
    List<IModel<String>> problems;

    public AbstractConfirmRemovalPanel(String id, T... roots) {
        this(id, null, Arrays.asList(roots));
    }

    public AbstractConfirmRemovalPanel(String id, Model<?> model, T... roots) {
        this(id, model, Arrays.asList(roots));
    }

    public AbstractConfirmRemovalPanel(String id, List<T> roots) {
        this(id, null, roots);
    }

    public AbstractConfirmRemovalPanel(String id, Model<?> model, List<T> rootObjects) {
        super(id, model);
        setRootObjectsAndProblems(rootObjects);

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        // root.add(new Label("rootObjectNames", names(roots)));
        // root.setVisible(!roots.isEmpty());
        add(root);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
        add(removed);

        // removed
        WebMarkupContainer rulesRemoved = new WebMarkupContainer("rulesRemoved");
        removed.add(rulesRemoved);
        if (roots.size() == 0) removed.setVisible(false);
        else {
            rulesRemoved.add(
                    new ListView<String>("rules", names(roots)) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Label("name", item.getModelObject()));
                        }
                    });
        }

        WebMarkupContainer problematic = new WebMarkupContainer("problematicObjects");
        add(problematic);

        WebMarkupContainer rulesNotRemoved = new WebMarkupContainer("rulesNotRemoved");
        problematic.add(rulesNotRemoved);
        if (problems.size() == 0) problematic.setVisible(false);
        else {
            rulesNotRemoved.add(
                    new ListView<String>("problems", problems(problems)) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(new Label("name", item.getModelObject()));
                        }
                    });
        }
    }

    void setRootObjectsAndProblems(List<T> rootObjects) {
        roots = new ArrayList<T>();
        problems = new ArrayList<IModel<String>>();
        for (T obj : rootObjects) {
            IModel<String> model = canRemove(obj);
            if (model == null) roots.add(obj);
            else problems.add(model);
        }
    }

    List<String> problems(List<IModel<String>> objects) {
        List<String> l = new ArrayList<String>();
        for (IModel<String> m : objects) {
            l.add(m.getObject());
        }
        return l;
    }

    List<String> names(List<T> objects) {
        List<String> l = new ArrayList<String>();
        for (T obj : objects) {
            l.add(name(obj));
        }
        return l;
    }

    String name(T object) {
        try {
            return getConfirmationMessage(object);
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        } catch (Exception e) {
            throw new RuntimeException(
                    "A data object that does not have "
                            + "a 'name' property has been used, this is unexpected",
                    e);
        }
    }

    protected IModel<String> canRemove(T data) {
        return null;
    }

    protected abstract String getConfirmationMessage(T object) throws Exception;

    public List<T> getRoots() {
        return roots;
    }
}
