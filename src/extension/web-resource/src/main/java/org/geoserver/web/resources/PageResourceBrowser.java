/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
 * The ResourceBrowser page.
 *
 * @author Niels Charlier
 */
public class PageResourceBrowser extends GeoServerSecuredPage {

    private static final long serialVersionUID = 3979040405548783679L;

    /** Behaviour for disabled button */
    private static final ClassAppender DISABLED_BEHAVIOR =
            new ClassAppender(new Model<String>("disabled"));

    /**
     * The extension that are recognised as simple text resources (and can be edited with simple
     * text editor).
     */
    private static final String[] TEXTUAL_EXTENSIONS =
            new String[] {
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

    public PageResourceBrowser() {
        // create the root node
        final ResourceNode rootNode = new ResourceNode(store().get(Paths.BASE), expandedStates);
        rootNode.getExpanded().setObject(true);

        // create tree view and clip board
        final TreeView<Resource> treeView = new TreeView<Resource>("treeview", rootNode);
        clipBoard = new ClipBoard(treeView);

        // used for all pop-up dialogs.
        final GeoServerDialog dialog = new GeoServerDialog("dialog");
        dialog.setResizable(false);

        // upload button
        final AjaxLink<Void> btnUpload =
                new AjaxLink<Void>("upload") {
                    private static final long serialVersionUID = -6538820444407766106L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(225);

                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    private static final long serialVersionUID =
                                            1557172478015946688L;
                                    private PanelUpload uploadPanel;

                                    @Override
                                    protected Component getContents(String id) {
                                        uploadPanel =
                                                new PanelUpload(
                                                        id,
                                                        "/"
                                                                + treeView.getSelectedNode()
                                                                        .getObject()
                                                                        .path());
                                        return uploadPanel;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        uploadPanel.getFeedbackMessages().clear();
                                        if (uploadPanel.getFileUpload() == null) {
                                            uploadPanel.error(
                                                    new ParamResourceModel(
                                                                    "fileRequired", getPage())
                                                            .getString());
                                        } else {
                                            String dir = uploadPanel.getDirectory();
                                            Resource dest =
                                                    store().get(
                                                                    Paths.path(
                                                                            dir,
                                                                            uploadPanel
                                                                                    .getFileUpload()
                                                                                    .getClientFileName()));
                                            if (Resources.exists(dest)) {
                                                uploadPanel.error(
                                                        new ParamResourceModel(
                                                                        "resourceExists", getPage())
                                                                .getString()
                                                                .replace("%", "/" + dest.path()));
                                            } else {
                                                try (OutputStream os = dest.out()) {
                                                    IOUtils.copy(
                                                            uploadPanel
                                                                    .getFileUpload()
                                                                    .getInputStream(),
                                                            os);
                                                    treeView.setSelectedNode(
                                                            new ResourceNode(dest, expandedStates),
                                                            target);
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
                };

        // new resource button
        final AjaxLink<Void> btnNew =
                new AjaxLink<Void>("new") {
                    private static final long serialVersionUID = 8112272759002275843L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(525);

                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    private static final long serialVersionUID =
                                            -8898887236980594842L;

                                    private PanelEdit editPanel;

                                    @Override
                                    protected Component getContents(String id) {
                                        // pick a non-existing resource name (can be changed by
                                        // user)
                                        String dest =
                                                "/"
                                                        + Paths.path(
                                                                treeView.getSelectedNode()
                                                                        .getObject()
                                                                        .path(),
                                                                "new.txt");
                                        for (int i = 1; Resources.exists(store().get(dest)); i++) {
                                            dest =
                                                    "/"
                                                            + Paths.path(
                                                                    treeView.getSelectedNode()
                                                                            .getObject()
                                                                            .path(),
                                                                    "new." + i + ".txt");
                                        }
                                        editPanel = new PanelEdit(id, dest, true, "");
                                        return editPanel;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        editPanel.getFeedbackMessages().clear();
                                        Resource dest = store().get(editPanel.getResource());
                                        if (Resources.exists(dest)) {
                                            editPanel.error(
                                                    new ParamResourceModel(
                                                                    "resourceExists", getPage())
                                                            .getString()
                                                            .replace("%", "/" + dest.path()));
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
                                                treeView.setSelectedNode(
                                                        new ResourceNode(dest, expandedStates),
                                                        target);
                                                return true;
                                            } catch (IOException | IllegalStateException e) {
                                                error(e.getMessage());
                                            }
                                        }
                                        target.add(editPanel.getFeedbackPanel());
                                        return false;
                                    }
                                });
                    }
                };

        // download button
        final Link<Void> btnDownload =
                new Link<Void>("download") {
                    private static final long serialVersionUID = 2746429086122117005L;

                    @Override
                    public void onClick() {
                        Resource res = treeView.getSelectedNode().getObject();
                        getRequestCycle()
                                .scheduleRequestHandlerAfterCurrent(
                                        new ResourceStreamRequestHandler(
                                                        new WicketResourceAdaptor(res))
                                                .setFileName(res.name())
                                                .setContentDisposition(
                                                        ContentDisposition.ATTACHMENT));
                    }
                };

        // edit button
        final AjaxLink<Void> btnEdit =
                new AjaxLink<Void>("edit") {
                    private static final long serialVersionUID = 6690936054046040647L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(500);
                        final Resource resource = treeView.getSelectedNode().getObject();
                        final String contents;
                        try (InputStream is = resource.in()) {
                            contents = IOUtils.toString(is);

                            dialog.showOkCancel(
                                    target,
                                    new DialogDelegate() {
                                        private static final long serialVersionUID =
                                                -8898887236980594842L;

                                        private PanelEdit editPanel;

                                        @Override
                                        protected Component getContents(String id) {
                                            editPanel =
                                                    new PanelEdit(
                                                            id,
                                                            "/" + resource.path(),
                                                            false,
                                                            contents);
                                            return editPanel;
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
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
                };

        // paste button
        final AjaxLink<Void> btnPaste =
                new AjaxLink<Void>("paste") {
                    private static final long serialVersionUID = 2647829118342823975L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(240);

                        final List<Resource> sources = new ArrayList<Resource>();
                        for (TreeNode<Resource> node : clipBoard.getItems()) {
                            sources.add(node.getObject());
                        }
                        final List<TreeNode<Resource>> newSelected =
                                new ArrayList<TreeNode<Resource>>();

                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    private static final long serialVersionUID =
                                            -8898887236980594842L;

                                    private PanelPaste pastePanel;

                                    @Override
                                    protected Component getContents(String id) {
                                        pastePanel =
                                                new PanelPaste(
                                                        id,
                                                        listResources(sources),
                                                        "/"
                                                                + treeView.getSelectedNode()
                                                                        .getObject()
                                                                        .path(),
                                                        clipBoard.isCopy());
                                        return pastePanel;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        pastePanel.getFeedbackMessages().clear();

                                        String dir = pastePanel.getDirectory();

                                        Iterator<Resource> it = sources.iterator();
                                        while (it.hasNext()) {
                                            Resource src = it.next();
                                            Resource dest =
                                                    store().get(Paths.path(dir, src.name()));
                                            if (clipBoard.isCopy()
                                                    && Resources.serializable(dest).equals(src)) {
                                                // if we are copying a resource to its own
                                                // directory, we will give it a new name.
                                                for (int i = 1; Resources.exists(dest); i++) {
                                                    dest =
                                                            store().get(
                                                                            Paths.path(
                                                                                    dir,
                                                                                    FilenameUtils
                                                                                                    .getExtension(
                                                                                                            src
                                                                                                                    .name())
                                                                                                    .isEmpty()
                                                                                            ? src
                                                                                                            .name()
                                                                                                    + "."
                                                                                                    + i
                                                                                            : FilenameUtils
                                                                                                            .getBaseName(
                                                                                                                    src
                                                                                                                            .name())
                                                                                                    + "."
                                                                                                    + i
                                                                                                    + "."
                                                                                                    + FilenameUtils
                                                                                                            .getExtension(
                                                                                                                    src
                                                                                                                            .name())));
                                                }
                                            }
                                            if (Resources.exists(dest)) {
                                                pastePanel.error(
                                                        new ParamResourceModel(
                                                                        "resourceExists", getPage())
                                                                .getString()
                                                                .replace("%", "/" + dest.path()));
                                            } else {
                                                try {
                                                    if (clipBoard.isCopy()) {
                                                        try (InputStream is = src.in()) {
                                                            try (OutputStream os = dest.out()) {
                                                                IOUtils.copy(is, os);
                                                            }
                                                        }
                                                    } else {
                                                        if (!store().move(
                                                                        src.path(), dest.path())) {
                                                            throw new IOException(
                                                                    new ParamResourceModel(
                                                                                    "moveFailed",
                                                                                    getPage())
                                                                            .getString()
                                                                            .replace(
                                                                                    "%",
                                                                                    "/"
                                                                                            + dest
                                                                                                    .path()));
                                                        }
                                                    }
                                                    it.remove();
                                                    newSelected.add(
                                                            new ResourceNode(dest, expandedStates));
                                                } catch (IOException | IllegalStateException e) {
                                                    pastePanel.error(e.getMessage());
                                                }
                                            }
                                        }

                                        // we select all the newly created nodes.
                                        treeView.setSelectedNodes(newSelected, target);

                                        // clear clipboard from moved resources (copied resources
                                        // will remain,
                                        // in case user wants to copy them multiple times)
                                        clipBoard.clearRemoved();

                                        // we leave modal only if operation was complete
                                        if (!sources.isEmpty()) {
                                            pastePanel
                                                    .getSourceField()
                                                    .setModelObject(listResources(sources));
                                            target.add(pastePanel.getFeedbackPanel());
                                            target.add(pastePanel.getSourceField());
                                            return false;
                                        }

                                        return true;
                                    }
                                });
                    }
                };

        // copy button
        final AjaxLink<Void> btnCopy =
                new AjaxLink<Void>("copy") {
                    private static final long serialVersionUID = 3883958793500232081L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        clipBoard.setItems(treeView.getSelectedNodes(), true, target);
                        target.add(treeView.getSelectedViews());
                    }
                };

        // cut button
        final AjaxLink<Void> btnCut =
                new AjaxLink<Void>("cut") {
                    private static final long serialVersionUID = 2647829118342823975L;

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
                };

        // rename button
        final AjaxLink<Void> btnRename =
                new AjaxLink<Void>("rename") {
                    private static final long serialVersionUID = 2647829118342823975L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(150);

                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    private static final long serialVersionUID =
                                            -8898887236980594842L;

                                    private PanelRename renamePanel;

                                    @Override
                                    protected Component getContents(String id) {
                                        renamePanel =
                                                new PanelRename(
                                                        id,
                                                        treeView.getSelectedNode()
                                                                .getObject()
                                                                .name());
                                        return renamePanel;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        renamePanel.getFeedbackMessages().clear();

                                        Resource src = treeView.getSelectedNode().getObject();
                                        Resource dest =
                                                treeView.getSelectedNode()
                                                        .getObject()
                                                        .parent()
                                                        .get(renamePanel.getName());
                                        if (Resources.exists(dest)) {
                                            renamePanel.error(
                                                    new ParamResourceModel(
                                                                    "resourceExists", getPage())
                                                            .getString()
                                                            .replace("%", "/" + dest.path()));
                                        } else {
                                            Boolean expandedModel =
                                                    expandedStates
                                                            .getResourceExpandedState(src)
                                                            .getObject();
                                            if (!src.renameTo(dest)) {
                                                renamePanel.error(
                                                        new ParamResourceModel(
                                                                        "renameFailed", getPage())
                                                                .getString());
                                            } else {
                                                if (clipBoard
                                                        .getItems()
                                                        .contains(
                                                                new ResourceNode(
                                                                        src, expandedStates))) {
                                                    clipBoard.clearRemoved();
                                                    clipBoard.addItem(
                                                            new ResourceNode(dest, expandedStates),
                                                            target);
                                                }

                                                // we have a new expanded state. if the original
                                                // node was expanded, we expand this one as well.
                                                // (child nodes might still loose their expanded
                                                // state though)
                                                expandedStates
                                                        .getResourceExpandedState(dest)
                                                        .setObject(expandedModel);
                                                // select the new node
                                                treeView.setSelectedNode(
                                                        new ResourceNode(dest, expandedStates),
                                                        target);
                                                return true;
                                            }
                                        }
                                        target.add(renamePanel.getFeedbackPanel());
                                        return false;
                                    }
                                });
                    }
                };

        // delete button
        final AjaxLink<Void> btnDelete =
                new AjaxLink<Void>("delete") {
                    private static final long serialVersionUID = -7370119488741589880L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.setInitialHeight(100);

                        final List<Resource> toBeDeleted = new ArrayList<Resource>();
                        for (TreeNode<Resource> selectedNode : treeView.getSelectedNodes()) {
                            toBeDeleted.add(selectedNode.getObject());
                        }

                        dialog.showOkCancel(
                                target,
                                new DialogDelegate() {
                                    private static final long serialVersionUID =
                                            1557172478015946688L;

                                    @Override
                                    protected Component getContents(String id) {
                                        return new Label(
                                                id,
                                                new ParamResourceModel("confirmDelete", getPage())
                                                                .getString()
                                                        + " "
                                                        + listResources(toBeDeleted));
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {
                                        for (Resource res : toBeDeleted) {
                                            if (!res.delete()) {
                                                error(
                                                        new ParamResourceModel(
                                                                        "deleteFailed", getPage())
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
                };

        // update menu buttons enabled states according to current selection
        treeView.addSelectionListener(
                target -> {
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
                    enable(
                            btnPaste,
                            node != null && clipBoard.getItems().size() > 0 && !node.isLeaf());
                    enable(btnCopy, nodes.size() > 0 && !containsDir);
                    enable(btnCut, nodes.size() > 0 && !containsRoot);
                    enable(btnDelete, nodes.size() > 0 && !containsRoot);

                    target.add(
                            btnUpload,
                            btnNew,
                            btnDownload,
                            btnEdit,
                            btnCopy,
                            btnCut,
                            btnPaste,
                            btnRename,
                            btnDelete);
                });

        // initialize and add buttons
        initButtons(
                btnUpload,
                btnNew,
                btnDownload,
                btnEdit,
                btnCopy,
                btnCut,
                btnPaste,
                btnRename,
                btnDelete);
        add(
                dialog,
                btnUpload,
                btnNew,
                btnDownload,
                btnEdit,
                btnCopy,
                btnCut,
                btnPaste,
                btnRename,
                btnDelete,
                treeView);
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
            builder.append("/" + res.path());
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
        int i = resource.name().lastIndexOf(".");
        if (i >= 0) {
            String ext = resource.name().substring(i + 1).toLowerCase();
            for (String t : TEXTUAL_EXTENSIONS) {
                if (ext.equals(t)) {
                    return true;
                }
            }
            return false;
        }
        return true; // no extension, assume textual
    }
}
