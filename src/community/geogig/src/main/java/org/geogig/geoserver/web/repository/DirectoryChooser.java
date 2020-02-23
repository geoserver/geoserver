/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.convert.IConverter;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.FileBreadcrumbs;
import org.geoserver.web.wicket.browser.FileProvider;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

/**
 * A geogig directory file chooser compoenent
 *
 * <p>Adapted from {@link GeoServerFileChooser}
 */
public class DirectoryChooser extends Panel {

    private static final long serialVersionUID = -5587014542924822012L;

    private final IModel<DirectoryFilter> fileFilter = new Model<>(new DirectoryFilter());

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

    private static final MetaDataKey<File> LAST_VISITED_DIRECTORY =
            new MetaDataKey<File>() {

                private static final long serialVersionUID = 1L;
            };

    private FileBreadcrumbs breadcrumbs;

    private DirectoryDataView directoryListingTable;

    private final IModel<File> directory;

    private final boolean makeRepositoriesSelectable;

    private AjaxLink<?> accepdDirectoryLink;

    public DirectoryChooser(String contentId, IModel<File> directory) {
        this(contentId, directory, true);
    }

    public DirectoryChooser(
            final String contentId,
            IModel<File> initialDirectory,
            final boolean makeRepositoriesSelectable) {
        super(contentId, initialDirectory);
        getSession().bind(); // so we can store the last visited directory as a session object
        this.makeRepositoriesSelectable = makeRepositoriesSelectable;

        if (initialDirectory.getObject() == null) {
            File lastUsed = getSession().getMetaData(LAST_VISITED_DIRECTORY);
            initialDirectory.setObject(lastUsed);
        }

        // build the roots
        ArrayList<File> roots = Lists.newArrayList(File.listRoots());
        Collections.sort(roots);

        // TODO: find a better way to deal with the data dir
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File dataDirectory = loader.getBaseDirectory();

        roots.add(0, dataDirectory);

        // add the home directory as well if it was possible to determine it at all
        if (USER_HOME != null) {
            roots.add(1, USER_HOME);
        }

        // find under which root the selection should be placed
        File selection = initialDirectory.getObject();

        // first check if the file is a relative reference into the data dir
        if (selection != null) {
            File relativeToDataDir = loader.url(selection.getPath());
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
                initialDirectory = new Model<>(selectionRoot);
            } else {
                if (!selection.isDirectory()) {
                    initialDirectory = new Model<>(selection.getParentFile());
                } else {
                    initialDirectory = new Model<>(selection);
                }
            }
        } else {
            selectionRoot = dataDirectory;
            initialDirectory = new Model<>(selectionRoot);
        }
        this.directory = initialDirectory;
        setDefaultModel(initialDirectory);

        // the root chooser
        final DropDownChoice<File> choice =
                new DropDownChoice<>(
                        "roots",
                        new Model<>(selectionRoot),
                        new Model<ArrayList<File>>(roots),
                        new FileRootsRenderer());
        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = -1113141016446727615L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        File selection = (File) choice.getModelObject();
                        breadcrumbs.setRootFile(selection);
                        updateFileBrowser(selection, target);
                    }
                });
        choice.setOutputMarkupId(true);
        add(choice);

        // the breadcrumbs
        breadcrumbs =
                new FileBreadcrumbs("breadcrumbs", new Model<>(selectionRoot), initialDirectory) {
                    private static final long serialVersionUID = 3637173832581301482L;

                    @Override
                    protected void pathItemClicked(File file, AjaxRequestTarget target) {
                        updateFileBrowser(file, target);
                    }
                };
        breadcrumbs.setOutputMarkupId(true);
        add(breadcrumbs);

        // the file tables
        directoryListingTable =
                new DirectoryDataView(
                        "fileTable",
                        new FileProvider(initialDirectory),
                        this.makeRepositoriesSelectable) {

                    private static final long serialVersionUID = -1559299096797421815L;

                    @Override
                    protected void linkNameClicked(File file, AjaxRequestTarget target) {
                        updateFileBrowser(file, target);
                    }
                };
        directoryListingTable.setOutputMarkupId(true);
        directoryListingTable.setFileFilter(fileFilter);
        add(directoryListingTable);

        accepdDirectoryLink =
                new AjaxLink<File>("ok", this.directory) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // must of been set by #directoryClicked()
                        File dir = getModelObject();
                        getSession().setMetaData(LAST_VISITED_DIRECTORY, dir);
                        directorySelected(dir, target);
                    }
                };
        add(accepdDirectoryLink);
        accepdDirectoryLink.setVisible(!this.makeRepositoriesSelectable);
    }

    void updateFileBrowser(File file, AjaxRequestTarget target) {
        if (RepositoryManager.isGeogigDirectory(file)) {
            geogigDirectoryClicked(file, target);
        } else {
            getSession().setMetaData(LAST_VISITED_DIRECTORY, file);
            directoryClicked(file, target);
        }
    }

    /**
     * Called when a file name is clicked. By default it does nothing
     *
     */
    protected void geogigDirectoryClicked(File file, AjaxRequestTarget target) {
        // do nothing, subclasses will override
    }

    /**
     * Action undertaken as a directory is clicked. Default behavior is to drill down into the
     * directory.
     *
     */
    protected void directoryClicked(File file, AjaxRequestTarget target) {
        // explicitly change the root model, inform the other components the model has changed
        DirectoryChooser.this.directory.setObject(file);
        directoryListingTable.setDirectory(new Model<>(file));
        breadcrumbs.setSelection(file);

        target.add(directoryListingTable);
        target.add(breadcrumbs);
    }

    protected void directorySelected(File file, AjaxRequestTarget target) {
        // to be overriden
    }

    private boolean isSubfile(File root, File selection) {
        if (selection == null || "".equals(selection.getPath())) return false;
        if (selection.equals(root)) return true;

        return isSubfile(root, selection.getParentFile());
    }

    /**
     * Set the file table fixed height. Set it to null if you don't want fixed height with overflow,
     * and to a valid CSS measure if you want it instead. Default value is "25em"
     *
     */
    public void setFileTableHeight(String height) {
        directoryListingTable.setTableHeight(height);
    }

    class FileRootsRenderer extends ChoiceRenderer<File> {
        private static final long serialVersionUID = -5804668199121599078L;

        @Override
        public Object getDisplayValue(File f) {
            if (f == USER_HOME) {
                return new ParamResourceModel("userHome", DirectoryChooser.this).getString();
            } else {
                GeoServerResourceLoader loader =
                        GeoServerExtensions.bean(GeoServerResourceLoader.class);

                if (f.equals(loader.getBaseDirectory())) {
                    return new ParamResourceModel("dataDirectory", DirectoryChooser.this)
                            .getString();
                }
            }

            try {
                final String displayName =
                        FileSystemView.getFileSystemView().getSystemDisplayName(f);
                if (displayName != null && displayName.length() > 0) {
                    return displayName;
                }
                return FilenameUtils.getPrefix(f.getAbsolutePath());
            } catch (Exception e) {
                // on windows we can get the occasional NPE due to
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6973685
            }
            return f.getName();
        }

        @Override
        public String getIdValue(File f, int count) {
            return "" + count;
        }
    }

    private static final class DirectoryFilter implements FileFilter, Serializable {
        private static final long serialVersionUID = -2280505390702552L;

        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    abstract static class DirectoryDataView extends Panel {

        private static final PackageResourceReference FOLDER =
                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/folder.png");

        private static final PackageResourceReference GEOGIG_FOLDER =
                new PackageResourceReference(
                        DirectoryDataView.class, "../geogig_16x16_babyblue.png");

        private static final long serialVersionUID = -2932107412054607607L;

        private final boolean allowSelectingRepositories;

        private final IConverter FILE_NAME_CONVERTER =
                new StringConverter() {

                    private static final long serialVersionUID = 2050812486536366790L;

                    @Override
                    public String convertToString(Object value, Locale locale) {
                        File file = (File) value;
                        if (file.isDirectory()) {
                            if (RepositoryManager.isGeogigDirectory(file)) {
                                return file.getName();
                            }
                            return file.getName() + "/";
                        } else {
                            return file.getName();
                        }
                    }
                };

        private static final IConverter FILE_LASTMODIFIED_CONVERTER =
                new StringConverter() {

                    private static final long serialVersionUID = 7862772890388011374L;

                    @Override
                    public String convertToString(Object value, Locale locale) {
                        File file = (File) value;
                        long lastModified = file.lastModified();
                        if (lastModified == 0L) return null;
                        else {
                            return DateFormat.getDateTimeInstance(
                                            DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(new Date(file.lastModified()));
                        }
                    }
                };

        private SortableDataProvider<File, String> provider;

        private final WebMarkupContainer fileContent;

        private String tableHeight = "25em";

        @SuppressWarnings("unchecked")
        public DirectoryDataView(
                final String id,
                final FileProvider fileProvider,
                final boolean allowSelectingRepositories) {
            super(id);
            this.provider = fileProvider;
            this.allowSelectingRepositories = allowSelectingRepositories;

            final WebMarkupContainer table = new WebMarkupContainer("fileTable");
            table.setOutputMarkupId(true);
            add(table);

            List<RepositoryInfo> all = RepositoryManager.get().getAll();

            // maps canonical files to configured file to identify duplicates due to symlinks
            final Map<File, File> existingPaths = new HashMap<>();
            for (RepositoryInfo info : all) {
                final URI uri = info.getLocation();
                if ("file".equals(uri.getScheme())) {
                    File configured = new File(uri);
                    File canonical;
                    try {
                        canonical = configured.getCanonicalFile();
                    } catch (IOException e) {
                        canonical = configured;
                    }
                    existingPaths.put(canonical, configured);
                }
            }

            DataView<File> fileTable =
                    new DataView<File>("files", fileProvider) {
                        private static final long serialVersionUID = 1345694542339080271L;

                        @Override
                        protected void populateItem(final Item<File> item) {
                            // odd/even alternate style
                            item.add(
                                    AttributeModifier.replace(
                                            "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                            final File file = item.getModelObject();
                            final boolean isGeogigDirectory =
                                    RepositoryManager.isGeogigDirectory(file);
                            PackageResourceReference icon =
                                    isGeogigDirectory ? GEOGIG_FOLDER : FOLDER;
                            item.add(new Image("icon", icon));

                            // navigation/selection links
                            AjaxFallbackLink<File> link =
                                    new AjaxFallbackLink<File>("nameLink") {
                                        private static final long serialVersionUID =
                                                -644973941443812893L;

                                        @Override
                                        public void onClick(AjaxRequestTarget target) {
                                            linkNameClicked((File) item.getModelObject(), target);
                                        }
                                    };
                            Label nameLabel =
                                    new Label("name", item.getModel()) {

                                        private static final long serialVersionUID =
                                                -4028081066393114129L;

                                        @Override
                                        public IConverter getConverter(Class type) {
                                            return FILE_NAME_CONVERTER;
                                        }
                                    };
                            link.add(nameLabel);

                            final Map<File, File> existing = existingPaths;
                            File canonicalFile;
                            try {
                                canonicalFile = file.getCanonicalFile();
                            } catch (IOException e) {
                                canonicalFile = file;
                            }

                            final boolean alreadyImported =
                                    isGeogigDirectory && existing.containsKey(canonicalFile);
                            if (isGeogigDirectory) {
                                if (alreadyImported) {
                                    link.setEnabled(false);
                                    File dupicate = existing.get(canonicalFile);
                                    nameLabel.add(
                                            AttributeModifier.replace(
                                                    "title",
                                                    new ParamResourceModel(
                                                                    "DirectoryChooser$DirectoryDataView.repoExists",
                                                                    DirectoryDataView.this,
                                                                    dupicate.getAbsolutePath())
                                                            .getObject()));
                                } else {
                                    link.setEnabled(
                                            DirectoryDataView.this.allowSelectingRepositories);
                                }
                            }

                            item.add(link);

                            // last modified and size labels
                            item.add(
                                    new Label("lastModified", item.getModel()) {

                                        private static final long serialVersionUID =
                                                -4706544449170830483L;

                                        @Override
                                        public IConverter getConverter(Class type) {
                                            return FILE_LASTMODIFIED_CONVERTER;
                                        }
                                    });
                        }
                    };

            fileContent =
                    new WebMarkupContainer("fileContent") {

                        private static final long serialVersionUID = -4197754944388542068L;

                        @Override
                        protected void onComponentTag(ComponentTag tag) {
                            if (tableHeight != null) {
                                tag.getAttributes()
                                        .put("style", "overflow:auto; height:" + tableHeight);
                            }
                        }
                    };

            fileContent.add(fileTable);

            table.add(fileContent);
        }

        protected abstract void linkNameClicked(File file, AjaxRequestTarget target);

        private abstract static class StringConverter implements IConverter {

            private static final long serialVersionUID = -6464669942374870999L;

            @Override
            public Object convertToObject(String value, Locale locale) {
                throw new UnsupportedOperationException("This converter works only for strings");
            }
        }

        public SortableDataProvider<File, String> getProvider() {
            return provider;
        }

        public void setTableHeight(String tableHeight) {
            this.tableHeight = tableHeight;
        }

        public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
            ((FileProvider) provider).setFileFilter(fileFilter);
        }

        public void setDirectory(Model<File> model) {
            ((FileProvider) provider).setDirectory(model);
        }
    }
}
