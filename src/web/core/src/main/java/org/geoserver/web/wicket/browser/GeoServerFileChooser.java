/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

public class GeoServerFileChooser extends Panel {

    private static final long serialVersionUID = -6246944669686555266L;
    static Boolean HIDE_FS = null;

    static {
        HIDE_FS = Boolean.valueOf(GeoServerExtensions.getProperty("GEOSERVER_FILEBROWSER_HIDEFS"));
    }

    static File USER_HOME = null;

    static {
        // try to safely determine the user home location
        try {
            File hf = null;
            String home = System.getProperty("user.home");
            if (home != null) {
                hf = new File(home);
            }
            if (hf != null && hf.exists()) {
                USER_HOME = hf;
            }
        } catch (Throwable t) {
            // that's ok, we might not be able to get the user home
        }
    }

    static final Logger LOGGER = Logging.getLogger(GeoServerFileChooser.class);

    FileBreadcrumbs breadcrumbs;
    FileDataView fileTable;
    boolean hideFileSystem = false;
    IModel<File> file;

    public GeoServerFileChooser(String id, IModel<File> file) {
        this(id, file, HIDE_FS);
    }

    /**
     * Constructor with optional flag to control how file system resources are exposed.
     *
     * <p>When <tt>hideFileSyste</tt> is set to <tt>true</tt> only the data directory is exposed in
     * the file browser.
     */
    public GeoServerFileChooser(String id, IModel<File> file, boolean hideFileSystem) {
        super(id, file);

        this.file = file;
        this.hideFileSystem = hideFileSystem;
        FileRootsFinder fileRootsFinder = new FileRootsFinder(hideFileSystem, true);
        ArrayList<File> roots = fileRootsFinder.getRoots();
        GeoServerResourceLoader loader = fileRootsFinder.getLoader();
        File dataDirectory = fileRootsFinder.getDataDirectory();

        // find under which root the selection should be placed
        File selection = (File) file.getObject();

        // first check if the file is a relative reference into the data dir
        if (selection != null) {
            File relativeToDataDir =
                    Resources.find(
                            Resources.fromURL(
                                    Files.asResource(loader.getBaseDirectory()),
                                    selection.getPath()),
                            true);
            if (relativeToDataDir != null) {
                selection = relativeToDataDir;
            }
        }

        // select the proper root
        File selectionRoot = null;
        if (selection != null && selection.exists()) {
            for (File root : roots) {
                if (isSubfile(root, selection.getAbsoluteFile())) {
                    selectionRoot = root;
                    break;
                }
            }

            // if the file is not part of the known search paths, give up
            // and switch back to the data directory
            if (selectionRoot == null) {
                selectionRoot = dataDirectory;
                file = new Model<File>(selectionRoot);
            } else {
                if (!selection.isDirectory()) {
                    file = new Model<File>(selection.getParentFile());
                } else {
                    file = new Model<File>(selection);
                }
            }
        } else {
            selectionRoot = dataDirectory;
            file = new Model<File>(selectionRoot);
        }
        this.file = file;
        setDefaultModel(file);

        // the root chooser
        final DropDownChoice<File> choice =
                new DropDownChoice<File>(
                        "roots",
                        new Model<File>(selectionRoot),
                        new Model<ArrayList<File>>(roots),
                        new FileRootsRenderer(this));
        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = -1527567847101388940L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        File selection = choice.getModelObject();
                        breadcrumbs.setRootFile(selection);
                        updateFileBrowser(selection, target);
                    }
                });
        choice.setOutputMarkupId(true);
        add(choice);

        // the breadcrumbs
        breadcrumbs =
                new FileBreadcrumbs("breadcrumbs", new Model<File>(selectionRoot), file) {

                    private static final long serialVersionUID = -6995769189316700797L;

                    @Override
                    protected void pathItemClicked(File file, AjaxRequestTarget target) {
                        updateFileBrowser(file, target);
                    }
                };
        breadcrumbs.setOutputMarkupId(true);
        add(breadcrumbs);

        // the file tables
        fileTable =
                new FileDataView("fileTable", new FileProvider(file)) {

                    private static final long serialVersionUID = -5481794219862786117L;

                    @Override
                    protected void linkNameClicked(File file, AjaxRequestTarget target) {
                        updateFileBrowser(file, target);
                    }
                };
        fileTable.setOutputMarkupId(true);
        add(fileTable);
    }

    void updateFileBrowser(File file, AjaxRequestTarget target) {
        if (file.isDirectory()) {
            directoryClicked(file, target);
        } else if (file.isFile()) {
            fileClicked(file, target);
        }
    }

    /** Called when a file name is clicked. By default it does nothing */
    protected void fileClicked(File file, AjaxRequestTarget target) {
        // do nothing, subclasses will override
    }

    /**
     * Action undertaken as a directory is clicked. Default behavior is to drill down into the
     * directory.
     */
    protected void directoryClicked(File file, AjaxRequestTarget target) {
        // explicitly change the root model, inform the other components the model has changed
        this.file.setObject(file);
        fileTable.getProvider().setDirectory(new Model<File>(file));
        breadcrumbs.setSelection(file);

        target.add(fileTable);
        target.add(breadcrumbs);
    }

    private boolean isSubfile(File root, File selection) {
        if (selection == null || "".equals(selection.getPath())) return false;
        if (selection.equals(root)) return true;

        return isSubfile(root, selection.getParentFile());
    }

    /** @param fileFilter */
    public void setFilter(IModel<? extends FileFilter> fileFilter) {
        fileTable.provider.setFileFilter(fileFilter);
    }

    /**
     * Set the file table fixed height. Set it to null if you don't want fixed height with overflow,
     * and to a valid CSS measure if you want it instead. Default value is "25em"
     */
    public void setFileTableHeight(String height) {
        fileTable.setTableHeight(height);
    }

    //    /**
    //     * If the file is in the data directory builds a data dir relative path, otherwise
    //     * returns an absolute path
    //     *
    //     */
    //    public String getRelativePath(File file) {
    //        File dataDirectory = GeoserverDataDirectory.getGeoserverDataDirectory();
    //        if(isSubfile(dataDirectory, file)) {
    //            File curr = file;
    //            String path = null;
    //            // paranoid check to avoid infinite loops
    //            while(curr != null && !curr.equals(dataDirectory)){
    //                if(path == null) {
    //                    path = curr.getName();
    //                } else {
    //                    path = curr.getName() + "/" + path;
    //                }
    //                curr = curr.getParentFile();
    //            }
    //            return "file:" + path;
    //        }
    //
    //        return "file://" + file.getAbsolutePath();
    //    }
    //
    static class FileRootsRenderer extends ChoiceRenderer<File> {

        private static final long serialVersionUID = 1389015915737006638L;

        Component component;

        public FileRootsRenderer(Component component) {
            this.component = component;
        }

        public Object getDisplayValue(File f) {

            if (f == USER_HOME) {
                return new ParamResourceModel("userHome", component).getString();
            } else {
                GeoServerResourceLoader loader =
                        GeoServerExtensions.bean(GeoServerResourceLoader.class);

                if (f.equals(loader.getBaseDirectory())) {
                    return new ParamResourceModel("dataDirectory", component).getString();
                }
            }

            try {
                final String displayName =
                        FileSystemView.getFileSystemView().getSystemDisplayName(f);
                if (displayName != null && !displayName.trim().isEmpty()) {
                    return displayName.trim();
                }
                return FilenameUtils.getPrefix(f.getAbsolutePath());
            } catch (Exception | AWTError e) {
                LOGGER.log(
                        Level.FINE,
                        "Failed to get file display name, "
                                + "on Windows this might be related to a known java bug http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6973685",
                        e);
                // on windows we can get the occasional NPE due to
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6973685
            }
            return f.getName();
        }

        public String getIdValue(File f, int count) {
            return "" + count;
        }
    }
}
