/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A panel showing the path between the root directory and the current directory as a set of links
 * separated by "/", much like breadcrumbs in a web site.
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class FileBreadcrumbs extends Panel {
    private static final long serialVersionUID = 2821319341957784628L;

    IModel<File> rootFile;

    public FileBreadcrumbs(String id, IModel<File> rootFile, IModel<File> currentFile) {
        super(id, currentFile);

        this.rootFile = rootFile;
        add(
                new ListView<File>("path", new BreadcrumbModel(rootFile, currentFile)) {

                    private static final long serialVersionUID = -855582301247703291L;

                    @Override
                    protected void populateItem(ListItem<File> item) {
                        File file = item.getModelObject();
                        boolean last = item.getIndex() == getList().size() - 1;

                        // the link to the current path item
                        Label name = new Label("pathItem", file.getName() + "/");
                        Link<File> link =
                                new IndicatingAjaxFallbackLink<File>(
                                        "pathItemLink", item.getModel()) {

                                    private static final long serialVersionUID =
                                            4295991386838610752L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        pathItemClicked((File) getModelObject(), target);
                                    }
                                };
                        link.add(name);
                        item.add(link);
                        link.setEnabled(!last);
                    }
                });
    }

    public void setRootFile(File root) {
        rootFile.setObject(root);
    }

    public void setSelection(File selection) {
        setDefaultModelObject(selection);
    }

    protected abstract void pathItemClicked(File file, AjaxRequestTarget target);

    static class BreadcrumbModel implements IModel<List<File>> {
        private static final long serialVersionUID = -3497123851146725406L;

        IModel<File> rootFileModel;

        IModel<File> currentFileModel;

        public BreadcrumbModel(IModel<File> rootFileModel, IModel<File> currentFileModel) {
            this.rootFileModel = rootFileModel;
            this.currentFileModel = currentFileModel;
        }

        public List<File> getObject() {
            File root = rootFileModel.getObject();
            File current = currentFileModel.getObject();

            // get all directories between current and root
            List<File> files = new ArrayList<File>();
            while (current != null && !current.equals(root)) {
                files.add(current);
                current = current.getParentFile();
            }
            if (current != null && current.equals(root)) files.add(root);
            // reverse the order, we want them ordered from root
            // to current
            Collections.reverse(files);

            return files;
        }

        public void setObject(List<File> object) {
            throw new UnsupportedOperationException("This model cannot be set!");
        }

        public void detach() {
            // nothing to do here
        }
    }
}
