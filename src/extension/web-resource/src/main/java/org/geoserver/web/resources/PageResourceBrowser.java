/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.treeview.ClassAppender;
import org.geoserver.web.treeview.TreeNode;
import org.geoserver.web.treeview.TreeView;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * The ResourceBrowser page, allows user access to ResourceStore.
 *
 * <p>Only access to ResourceStore contents is provided (absolute and relative paths are not supported).
 *
 * @author Niels Charlier
 */
public class PageResourceBrowser extends GeoServerSecuredPage {

    @Serial
    private static final long serialVersionUID = 3979040405548783679L;

    /** Behaviour for disabled button */
    private static final ClassAppender DISABLED_BEHAVIOR = new ClassAppender(new Model<>("disabled"));

    /** The extension that are recognised as simple text resources (and can be edited with simple text editor). */
    private static final String[] TEXTUAL_EXTENSIONS = {
        "txt",
        "properties",
        "info",
        "xml",
        "sld",
        "rst",
        "log",
        "asc",
        "cfg",
        "css",
        "ftl",
        "htm",
        "html",
        "js",
        "xsd",
        "prj",
        "meta",
        "pgw",
        "pal",
        "tfw",
        "url"
    };

    /** The expanded states model. */
    protected final ResourceExpandedStates expandedStates = new ResourceExpandedStates();

    /** The clip board. */
    protected final ClipBoard clipBoard;

    private final TreeView<Resource> treeView;
    private final GeoServerDialog dialog;

    public PageResourceBrowser() {
        // create the root node
        final ResourceNode rootNode = new ResourceNode(store().get(Paths.BASE), expandedStates);
        rootNode.getExpanded().setObject(true);

        // create tree view and clip board
        treeView = new TreeView<>("treeview", rootNode);
        clipBoard = new ClipBoard(treeView);

        // used for all pop-up dialogs.
        dialog = new GeoServerDialog("dialog");

        // upload button
        final AjaxLink<Void> btnUpload = new UploadButton();

        // new resource button
        final AjaxLink<Void> btnNew = new NewButton();

        // download button
        final Link<Void> btnDownload = new DownloadButton();

        // edit button
        final AjaxLink<Void> btnEdit = new EditButton();

        // paste button
        final AjaxLink<Void> btnPaste = new PasteButton();

        // copy button
        final AjaxLink<Void> btnCopy = new CopyButton();

        // cut button
        final AjaxLink<Void> btnCut = new CutButton(btnPaste);

        // rename button
        final AjaxLink<Void> btnRename = new RenameButton();

        // delete button
        final AjaxLink<Void> btnDelete = new DeleteButton();

        // update menu buttons enabled states according to current selection
        treeView.addSelectionListener(target -> {
            Collection<TreeNode<Resource>> nodes = treeView.getSelectedNodes();
            boolean containsRoot = false;
            boolean containsDir = false;
            for (TreeNode<Resource> node : nodes) {
                if (!node.isLeaf()) {
                    containsDir = true;
                }
                if (node.getObject().path().isEmpty()) {
                    containsRoot = true;
                }
            }
            TreeNode<Resource> node = treeView.getSelectedNode();

            enable(btnUpload, node != null && !node.isLeaf());
            enable(btnNew, node != null && !node.isLeaf());
            enable(btnDownload, node != null && node.isLeaf());
            enable(btnEdit, node != null && node.isLeaf() && isTextual(node.getObject()));
            enable(btnRename, node != null && !node.getObject().path().isEmpty());
            enable(btnPaste, node != null && !clipBoard.getItems().isEmpty() && !node.isLeaf());
            enable(btnCopy, !nodes.isEmpty() && !containsDir);
            enable(btnCut, !nodes.isEmpty() && !containsRoot);
            enable(btnDelete, !nodes.isEmpty() && !containsRoot);

            target.add(btnUpload, btnNew, btnDownload, btnEdit, btnCopy, btnCut, btnPaste, btnRename, btnDelete);
        });

        // initialize and add buttons
        initButtons(btnUpload, btnNew, btnDownload, btnEdit, btnCopy, btnCut, btnPaste, btnRename, btnDelete);
        add(dialog, btnUpload, btnNew, btnDownload, btnEdit, btnCopy, btnCut, btnPaste, btnRename, btnDelete, treeView);
    }

    /**
     * The TreeView
     *
     * @return the TreeView
     */
    @SuppressWarnings("unchecked")
    protected TreeView<Resource> treeView() {
        return (TreeView<Resource>) get("treeview");
    }

    /**
     * The resource store
     *
     * @return resource store
     */
    protected ResourceStore store() {
        return getGeoServerApplication().getResourceLoader();
    }

    /** Initialize the buttons (start with all disabled) */
    protected static void initButtons(AbstractLink... buttons) {
        for (AbstractLink button : buttons) {
            button.setEnabled(false);
            button.add(DISABLED_BEHAVIOR);
            button.setOutputMarkupId(true);
        }
    }

    /**
     * Enable/disable a button
     *
     * @param button the button
     * @param enabled enabled state
     */
    protected static void enable(AbstractLink button, boolean enabled) {
        if (enabled != button.isEnabled()) {
            button.setEnabled(enabled);
            if (enabled) {
                button.remove(DISABLED_BEHAVIOR);
            } else {
                button.add(DISABLED_BEHAVIOR);
            }
        }
    }

    private static String listResources(Collection<Resource> resources) {
        if (resources.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Resource res : resources) {
            builder.append(res.path());
            builder.append(", ");
        }
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    /**
     * Guess if a resource is textual or not (by extension)
     *
     * @param resource the resource
     * @return whether that resource is likely textual or not.
     */
    private static boolean isTextual(Resource resource) {
        if (resource.getType() != Resource.Type.RESOURCE) {
            return false;
        }
        String ext = Paths.extension(resource.name());
        if (ext != null) {
            return Arrays.stream(TEXTUAL_EXTENSIONS).anyMatch(ext::equals);
        }
        return true; // no extension, assume textual
    }

    private class UploadButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = -6538820444407766106L;

        public UploadButton() {
            super("upload");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(225);

            dialog.showOkCancel(target, new DialogDelegate() {
                @Serial
                private static final long serialVersionUID = 1557172478015946688L;

                private PanelUpload uploadPanel;

                @Override
                protected Component getContents(String id) {
                    uploadPanel = new PanelUpload(
                            id, treeView.getSelectedNode().getObject().path());
                    return uploadPanel;
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    uploadPanel.getFeedbackMessages().clear();
                    if (uploadPanel.getFileUpload() == null) {
                        uploadPanel.error(new ParamResourceModel("fileRequired", getPage()).getString());
                    } else {
                        Resource dest = getUploadPanelResource(uploadPanel);
                        if (Resources.exists(dest)) {
                            uploadPanel.error(new ParamResourceModel("resourceExists", getPage())
                                    .getString()
                                    .replace("%", dest.path()));
                        } else {
                            try (OutputStream os = dest.out()) {
                                IOUtils.copy(uploadPanel.getFileUpload().getInputStream(), os);
                                treeView.setSelectedNode(new ResourceNode(dest, expandedStates), target);
                                return true;
                            } catch (IOException | IllegalStateException e) {
                                uploadPanel.error(e.getMessage());
                            }
                        }
                    }
                    target.add(uploadPanel.getFeedbackPanel());
                    return false;
                }
            });
        }
    }

    private Resource getUploadPanelResource(PanelUpload uploadPanel) {
        String dir = uploadPanel.getDirectory();
        Resource dest = store().get(Paths.path(dir, uploadPanel.getFileUpload().getClientFileName()));
        return dest;
    }

    private class NewButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 8112272759002275843L;

        public NewButton() {
            super("new");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(525);

            dialog.showOkCancel(target, new DialogDelegate() {
                @Serial
                private static final long serialVersionUID = -8898887236980594842L;

                private PanelEdit editPanel;

                @Override
                protected Component getContents(String id) {
                    // pick a non-existing resource name (can be changed by
                    // user)
                    String dest = getPath();
                    for (int i = 1; Resources.exists(store().get(dest)); i++) {
                        dest = getIthPath(i);
                    }
                    editPanel = new PanelEdit(id, store().get(dest), true, "");
                    return editPanel;
                }

                private String getIthPath(int i) {
                    return Paths.path(treeView.getSelectedNode().getObject().path(), "new." + i + ".txt");
                }

                private String getPath() {
                    return Paths.path(treeView.getSelectedNode().getObject().path(), "new.txt");
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    editPanel.getFeedbackMessages().clear();
                    String resourcePath = editPanel.getResource();
                    if (!Paths.isValid(resourcePath)) {
                        try {
                            Paths.valid(resourcePath);
                        } catch (IllegalArgumentException reason) {
                            error(reason.getMessage());
                            return false;
                        }
                    } else {
                        Resource dest = store().get(resourcePath);
                        if (Resources.exists(dest)) {
                            editPanel.error(new ParamResourceModel("resourceExists", getPage())
                                    .getString()
                                    .replace("%", dest.path()));
                        } else {
                            try (OutputStream os = dest.out()) {
                                String newContents = editPanel.getContents();
                                if (newContents != null) {
                                    os.write(newContents.getBytes());
                                    if (!newContents.endsWith("\n")) {
                                        os.write(System.lineSeparator().getBytes());
                                    }
                                }
                                // select newly created node
                                treeView.setSelectedNode(new ResourceNode(dest, expandedStates), target);
                                return true;
                            } catch (IOException | IllegalStateException e) {
                                error(e.getMessage());
                            }
                        }
                    }
                    target.add(editPanel.getFeedbackPanel());
                    return false;
                }
            });
        }
    }

    private class DownloadButton extends Link<Void> {
        @Serial
        private static final long serialVersionUID = 2746429086122117005L;

        public DownloadButton() {
            super("download");
        }

        @Override
        public void onClick() {
            Resource res = treeView.getSelectedNode().getObject();
            getRequestCycle()
                    .scheduleRequestHandlerAfterCurrent(new ResourceStreamRequestHandler(new WicketResourceAdaptor(res))
                            .setFileName(res.name())
                            .setContentDisposition(ContentDisposition.ATTACHMENT));
        }
    }

    private class EditButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 6690936054046040647L;

        public EditButton() {
            super("edit");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(500);
            final Resource resource = treeView.getSelectedNode().getObject();
            final String contents;
            try (InputStream is = resource.in()) {
                contents = IOUtils.toString(is, StandardCharsets.UTF_8);

                dialog.showOkCancel(target, new DialogDelegate() {
                    @Serial
                    private static final long serialVersionUID = -8898887236980594842L;

                    private PanelEdit editPanel;

                    @Override
                    protected Component getContents(String id) {
                        editPanel = new PanelEdit(id, resource, false, contents);
                        return editPanel;
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        editPanel.getFeedbackMessages().clear();
                        try (OutputStream os = resource.out()) {
                            String newContents = editPanel.getContents();
                            if (newContents != null) {
                                os.write(newContents.getBytes());
                                if (!newContents.endsWith("\n")) {
                                    os.write(System.lineSeparator().getBytes());
                                }
                            }
                            return true;
                        } catch (IOException | IllegalStateException e) {
                            error(e.getMessage());
                        }
                        target.add(editPanel.getFeedbackPanel());
                        return false;
                    }
                });
            } catch (IOException | IllegalStateException e) {
                error(e.getMessage());
                target.add(bottomFeedbackPanel);
                target.add(topFeedbackPanel);
            }
        }
    }

    private class PasteButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 2647829118342823975L;

        public PasteButton() {
            super("paste");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(240);

            final List<Resource> sources = new ArrayList<>();
            for (TreeNode<Resource> node : clipBoard.getItems()) {
                sources.add(node.getObject());
            }
            final List<TreeNode<Resource>> newSelected = new ArrayList<>();

            dialog.showOkCancel(target, new DialogDelegate() {
                @Serial
                private static final long serialVersionUID = -8898887236980594842L;

                private PanelPaste pastePanel;

                @Override
                protected Component getContents(String id) {
                    pastePanel = new PanelPaste(
                            id,
                            listResources(sources),
                            treeView.getSelectedNode().getObject().path(),
                            clipBoard.isCopy());
                    return pastePanel;
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    pastePanel.getFeedbackMessages().clear();

                    String dir = pastePanel.getDirectory();

                    Iterator<Resource> it = sources.iterator();
                    while (it.hasNext()) {
                        Resource src = it.next();
                        pasteResource(dir, it, src);
                    }

                    // we select all the newly created nodes.
                    treeView.setSelectedNodes(newSelected, target);

                    // clear clipboard from moved resources (copied resources
                    // will remain,
                    // in case user wants to copy them multiple times)
                    clipBoard.clearRemoved();

                    // we leave modal only if operation was complete
                    if (!sources.isEmpty()) {
                        pastePanel.getSourceField().setModelObject(listResources(sources));
                        target.add(pastePanel.getFeedbackPanel());
                        target.add(pastePanel.getSourceField());
                        return false;
                    }

                    return true;
                }

                private void pasteResource(String dir, Iterator<Resource> it, Resource src) {
                    Resource dest = store().get(Paths.path(dir, src.name()));
                    if (clipBoard.isCopy() && Resources.serializable(dest).equals(src)) {
                        // if we are copying a resource to its own
                        // directory, we will give it a new name.
                        for (int i = 1; Resources.exists(dest); i++) {
                            dest = store().get(getPath(dir, src, i));
                        }
                    }
                    if (Resources.exists(dest)) {
                        pastePanel.error(new ParamResourceModel("resourceExists", getPage())
                                .getString()
                                .replace("%", dest.path()));
                    } else {
                        try {
                            if (clipBoard.isCopy()) {
                                Resources.copy(src, dest);
                            } else {
                                if (!store().move(src.path(), dest.path())) {
                                    moveFailed(dest);
                                }
                            }
                            it.remove();
                            newSelected.add(new ResourceNode(dest, expandedStates));
                        } catch (IOException | IllegalStateException e) {
                            pastePanel.error(e.getMessage());
                        }
                    }
                }

                private void moveFailed(Resource dest) throws IOException {
                    throw new IOException(new ParamResourceModel("moveFailed", getPage())
                            .getString()
                            .replace("%", dest.path()));
                }

                private String getPath(String dir, Resource src, int i) {
                    return Paths.path(
                            dir,
                            FilenameUtils.getExtension(src.name()).isEmpty()
                                    ? src.name() + "." + i
                                    : FilenameUtils.getBaseName(src.name())
                                            + "."
                                            + i
                                            + "."
                                            + FilenameUtils.getExtension(src.name()));
                }
            });
        }
    }

    private class CopyButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 3883958793500232081L;

        public CopyButton() {
            super("copy");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            clipBoard.setItems(treeView.getSelectedNodes(), true, target);
            target.add(treeView.getSelectedViews());
        }
    }

    private class CutButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 2647829118342823975L;

        private final AjaxLink<Void> btnPaste;

        public CutButton(AjaxLink<Void> btnPaste) {
            super("cut");
            this.btnPaste = btnPaste;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            clipBoard.setItems(treeView.getSelectedNodes(), false, target);
            enable(
                    btnPaste,
                    treeView.getSelectedNode() != null
                            && !treeView.getSelectedNode().isLeaf());
            target.add(treeView.getSelectedViews());
            target.add(btnPaste);
        }
    }

    private class RenameButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = 2647829118342823975L;

        public RenameButton() {
            super("rename");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(150);

            dialog.showOkCancel(target, new DialogDelegate() {
                @Serial
                private static final long serialVersionUID = -8898887236980594842L;

                private PanelRename renamePanel;

                @Override
                protected Component getContents(String id) {
                    renamePanel = new PanelRename(
                            id, treeView.getSelectedNode().getObject().name());
                    return renamePanel;
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    renamePanel.getFeedbackMessages().clear();

                    Resource src = treeView.getSelectedNode().getObject();
                    Resource dest =
                            treeView.getSelectedNode().getObject().parent().get(renamePanel.getName());
                    if (Resources.exists(dest)) {
                        renamePanel.error(new ParamResourceModel("resourceExists", getPage())
                                .getString()
                                .replace("%", dest.path()));
                    } else {
                        Boolean expandedModel =
                                expandedStates.getResourceExpandedState(src).getObject();
                        if (!src.renameTo(dest)) {
                            renamePanel.error(new ParamResourceModel("renameFailed", getPage()).getString());
                        } else {
                            if (clipBoard.getItems().contains(new ResourceNode(src, expandedStates))) {
                                clipBoard.clearRemoved();
                                clipBoard.addItem(new ResourceNode(dest, expandedStates), target);
                            }

                            // we have a new expanded state. if the original
                            // node was expanded, we expand this one as well.
                            // (child nodes might still loose their expanded
                            // state though)
                            expandedStates.getResourceExpandedState(dest).setObject(expandedModel);
                            // select the new node
                            treeView.setSelectedNode(new ResourceNode(dest, expandedStates), target);
                            return true;
                        }
                    }
                    target.add(renamePanel.getFeedbackPanel());
                    return false;
                }
            });
        }
    }

    private class DeleteButton extends AjaxLink<Void> {
        @Serial
        private static final long serialVersionUID = -7370119488741589880L;

        public DeleteButton() {
            super("delete");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(100);

            final List<Resource> toBeDeleted = new ArrayList<>();
            for (TreeNode<Resource> selectedNode : treeView.getSelectedNodes()) {
                toBeDeleted.add(selectedNode.getObject());
            }

            dialog.showOkCancel(target, new DialogDelegate() {
                @Serial
                private static final long serialVersionUID = 1557172478015946688L;

                @Override
                protected Component getContents(String id) {
                    return new Label(
                            id,
                            new ParamResourceModel("confirmDelete", getPage()).getString()
                                    + " "
                                    + listResources(toBeDeleted));
                }

                @Override
                protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                    for (Resource res : toBeDeleted) {
                        if (!res.delete()) {
                            error(new ParamResourceModel("deleteFailed", getPage())
                                    .getString()
                                    .replace("%", res.path()));
                            target.add(bottomFeedbackPanel);
                            target.add(topFeedbackPanel);
                        }
                    }
                    // if deleted node was on clipboard, remove it form the
                    // clipboard
                    clipBoard.clearRemoved();

                    // remove selection
                    treeView.setSelectedNodes(Collections.emptySet(), target);

                    return true;
                }
            });
        }
    }
}
