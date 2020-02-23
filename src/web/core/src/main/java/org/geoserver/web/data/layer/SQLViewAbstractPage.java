/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Base page for SQL view creation/editing
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public abstract class SQLViewAbstractPage extends GeoServerSecuredPage {

    public static final String DATASTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String storeId;

    String typeInfoId;

    String sql;

    String name;

    boolean newView;

    SQLViewAttributeProvider attProvider;

    private TextArea<String> sqlEditor;

    private GeoServerTablePanel<SQLViewAttribute> attributes;

    private GeoServerTablePanel<Parameter> parameters;

    private SQLViewParamProvider paramProvider;

    boolean guessGeometrySrid = false;

    private CheckBox guessCheckbox;

    private boolean escapeSql = true;

    private static final List<Class<? extends Geometry>> GEOMETRY_TYPES =
            Arrays.asList(
                    Geometry.class,
                    GeometryCollection.class,
                    Point.class,
                    MultiPoint.class,
                    LineString.class,
                    MultiLineString.class,
                    Polygon.class,
                    MultiPolygon.class);

    public SQLViewAbstractPage(PageParameters params) throws IOException {
        this(
                params.get(WORKSPACE).toOptionalString(),
                params.get(DATASTORE).toString(),
                null,
                null);
    }

    public SQLViewAbstractPage(
            String workspaceName, String storeName, String typeName, VirtualTable virtualTable)
            throws IOException {
        storeId =
                getCatalog().getStoreByName(workspaceName, storeName, DataStoreInfo.class).getId();

        // build the form and the text area
        Form<SQLViewAbstractPage> form = new Form<>("form", new CompoundPropertyModel<>(this));
        add(form);
        final TextField<String> nameField = new TextField<>("name");
        nameField.setRequired(true);
        nameField.add(new ViewNameValidator());
        form.add(nameField);
        sqlEditor = new TextArea<>("sql");
        form.add(sqlEditor);

        // the parameters and attributes provider
        attProvider = new SQLViewAttributeProvider();
        paramProvider = new SQLViewParamProvider();

        // setting up the providers
        if (typeName != null) {
            newView = false;

            // grab the virtual table
            DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
            FeatureTypeInfo fti =
                    getCatalog().getResourceByStore(store, typeName, FeatureTypeInfo.class);
            // the type can be still not saved
            if (fti != null) {
                typeInfoId = fti.getId();
            }
            if (virtualTable == null) {
                throw new IllegalArgumentException(
                        "The specified feature type does not have a sql view attached to it");
            }

            // get the store
            DataAccess<?, ?> da = store.getDataStore(null);
            if (!(da instanceof JDBCDataStore)) {
                error("Cannot create a SQL view if the store is not database based");
                doReturn(StorePage.class);
                return;
            }

            name = virtualTable.getName();
            sql = virtualTable.getSql();
            escapeSql = virtualTable.isEscapeSql();

            paramProvider.init(virtualTable);
            try {
                SimpleFeatureType ft = testViewDefinition(virtualTable, false);
                attProvider.setFeatureType(ft, virtualTable);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to build feature type for the sql view", e);
            }
        } else {
            newView = true;
        }

        // the links to refresh, add and remove a parameter
        form.add(
                new GeoServerAjaxFormLink("guessParams") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        sqlEditor.processInput();
                        parameters.processInputs();
                        if (sql != null && !"".equals(sql.trim())) {
                            paramProvider.refreshFromSql(sql);
                            parameters.setPageable(false);
                            target.add(parameters);
                        }
                    }
                });
        form.add(
                new GeoServerAjaxFormLink("addNewParam") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        paramProvider.addParameter();
                        target.add(parameters);
                    }
                });
        form.add(
                new GeoServerAjaxFormLink("removeParam") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form<?> form) {
                        paramProvider.removeAll(parameters.getSelection());
                        parameters.clearSelection();
                        target.add(parameters);
                    }
                });

        // the parameters table
        parameters =
                new GeoServerTablePanel<Parameter>("parameters", paramProvider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<Parameter> itemModel, Property<Parameter> property) {
                        Fragment f = new Fragment(id, "text", SQLViewAbstractPage.this);
                        @SuppressWarnings("unchecked")
                        TextField<String> text =
                                new TextField<String>(
                                        "text", (IModel<String>) property.getModel(itemModel));
                        text.setLabel(
                                new ParamResourceModel(
                                        "th." + property.getName(), SQLViewAbstractPage.this));
                        if (property == SQLViewParamProvider.NAME) {
                            text.setRequired(true);
                        } else if (property == SQLViewParamProvider.REGEXP) {
                            text.add(new RegexpValidator());
                        }
                        f.add(text);
                        return f;
                    }
                };
        parameters.setFilterVisible(false);
        parameters.setSortable(false);
        parameters.setPageable(false);
        parameters.setOutputMarkupId(true);
        form.add(parameters);

        // the "refresh attributes" link
        form.add(refreshLink());
        form.add(
                guessCheckbox =
                        new CheckBox(
                                "guessGeometrySrid",
                                new PropertyModel<Boolean>(this, "guessGeometrySrid")));
        form.add(new CheckBox("escapeSql"));

        // the editable attribute table
        attributes =
                new GeoServerTablePanel<SQLViewAttribute>("attributes", attProvider) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<SQLViewAttribute> itemModel,
                            Property<SQLViewAttribute> property) {
                        SQLViewAttribute att = (SQLViewAttribute) itemModel.getObject();
                        boolean isGeometry =
                                att.getType() != null
                                        && Geometry.class.isAssignableFrom(att.getType());
                        if (property == SQLViewAttributeProvider.PK) {
                            // editor for pk status
                            Fragment f = new Fragment(id, "checkbox", SQLViewAbstractPage.this);
                            f.add(new CheckBox("identifier", new PropertyModel<>(itemModel, "pk")));
                            return f;
                        } else if (property == SQLViewAttributeProvider.TYPE && isGeometry) {
                            Fragment f = new Fragment(id, "geometry", SQLViewAbstractPage.this);
                            f.add(
                                    new DropDownChoice<Class<? extends Geometry>>(
                                            "geometry",
                                            new PropertyModel<>(itemModel, "type"),
                                            GEOMETRY_TYPES,
                                            new GeometryTypeRenderer()));
                            return f;
                        } else if (property == SQLViewAttributeProvider.SRID && isGeometry) {
                            Fragment f = new Fragment(id, "text", SQLViewAbstractPage.this);
                            f.add(
                                    new TextField<Integer>(
                                            "text", new PropertyModel<Integer>(itemModel, "srid")));
                            return f;
                        }
                        return null;
                    }
                };
        // just a plain table, no filters, no paging,
        attributes.setFilterVisible(false);
        attributes.setSortable(false);
        attributes.setPageable(false);
        attributes.setOutputMarkupId(true);
        form.add(attributes);

        // save and cancel at the bottom of the page
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        onSave();
                    }
                });
        form.add(
                new Link<Void>("cancel") {

                    @Override
                    public void onClick() {
                        onCancel();
                    }
                });
    }

    private GeoServerAjaxFormLink refreshLink() {
        return new GeoServerAjaxFormLink("refresh") {

            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                sqlEditor.processInput();
                parameters.processInputs();
                guessCheckbox.processInput();
                if (sql != null && !"".equals(sql.trim())) {
                    SimpleFeatureType newSchema = null;
                    try {
                        newSchema = testViewDefinition(guessGeometrySrid);

                        if (newSchema != null) {
                            attProvider.setFeatureType(newSchema, null);
                            target.add(attributes);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Error testing SQL query", e);
                        error(getFirstErrorMessage(e));
                    }
                }
            }
        };
    }

    /**
     * Checks the view definition works as expected and returns the feature type guessed solely by
     * looking at the sql and the first row of its output
     */
    protected SimpleFeatureType testViewDefinition(boolean guessGeometrySrid) throws IOException {
        // check out if the view can be used
        JDBCDataStore ds = (JDBCDataStore) getCatalog().getDataStore(storeId).getDataStore(null);
        String vtName = null;
        try {
            // use a highly random name
            do {
                vtName = UUID.randomUUID().toString();
            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));

            // try adding the vt and see if that works
            VirtualTable vt = new VirtualTable(vtName, sql);
            paramProvider.updateVirtualTable(vt);
            ds.createVirtualTable(vt);
            return guessFeatureType(ds, vt.getName(), guessGeometrySrid);
        } finally {
            if (vtName != null) {
                ds.dropVirtualTable(vtName);
            }
        }
    }

    protected SimpleFeatureType getFeatureType(VirtualTable vt) throws IOException {
        // check out if the view can be used
        JDBCDataStore ds = (JDBCDataStore) getCatalog().getDataStore(storeId).getDataStore(null);
        String vtName = null;
        try {
            // use a highly random name
            do {
                vtName = UUID.randomUUID().toString();
            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));

            // try adding the vt and see if that works
            ds.createVirtualTable(new VirtualTable(vtName, vt));
            return ds.getSchema(vtName);
        } finally {
            if (vtName != null) {
                ds.dropVirtualTable(vtName);
            }
        }
    }

    /**
     * Checks the view definition works as expected and returns the feature type guessed solely by
     * looking at the sql and the first row of its output
     */
    protected SimpleFeatureType testViewDefinition(
            VirtualTable virtualTable, boolean guessGeometrySrid) throws IOException {
        // check out if the view can be used
        JDBCDataStore ds = (JDBCDataStore) getCatalog().getDataStore(storeId).getDataStore(null);
        String vtName = null;
        try {
            // use a highly random name
            do {
                vtName = UUID.randomUUID().toString();
            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));

            // try adding the vt and see if that works
            VirtualTable vt = new VirtualTable(vtName, virtualTable);
            // hide the primary key definitions or we'll loose some columns
            vt.setPrimaryKeyColumns(Collections.emptyList());
            vt.setEscapeSql(escapeSql);
            ds.createVirtualTable(vt);
            return guessFeatureType(ds, vt.getName(), guessGeometrySrid);
        } finally {
            if (vtName != null) {
                ds.dropVirtualTable(name);
            }
        }
    }

    /**
     * Grabs the feature type from the store, but takes a peek at figuring out the geoemtry type and
     * srids
     */
    SimpleFeatureType guessFeatureType(
            JDBCDataStore store, String vtName, boolean guessGeometrySrid) throws IOException {
        SimpleFeatureType base = store.getSchema(vtName);
        List<String> geometries = new ArrayList<String>();
        for (AttributeDescriptor ad : base.getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                geometries.add(ad.getLocalName());
            }
        }

        // no geometries? Or, shall we not try to guess the geometries type and srid?
        if (geometries.size() == 0 || !guessGeometrySrid) {
            return base;
        }

        // build a query to fetch the first rwo, we'll inspect the resulting
        // geometries
        Query q = new Query(vtName);
        q.setPropertyNames(geometries);
        q.setMaxFeatures(1);
        SimpleFeatureIterator it = null;
        SimpleFeature f = null;
        try {
            it = store.getFeatureSource(vtName).getFeatures(q).features();
            if (it.hasNext()) {
                f = it.next();
            }
        } finally {
            if (it != null) {
                it.close();
            }
        }

        // did we get more information?
        if (f == null) {
            return base;
        }

        // if so, try to build an override feature type
        Connection cx = null;
        try {
            store.getConnection(Transaction.AUTO_COMMIT);
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName(base.getName());
            for (AttributeDescriptor ad : base.getAttributeDescriptors()) {
                if (ad instanceof GeometryDescriptor) {
                    GeometryDescriptor gd = (GeometryDescriptor) ad;
                    Geometry g = (Geometry) f.getAttribute(ad.getLocalName());
                    if (g == null) {
                        // nothing new we can learn
                        tb.add(ad);
                    } else {
                        Class<?> binding = g.getClass();
                        CoordinateReferenceSystem crs = null;
                        if (g.getSRID() > 0) {
                            // see if the dialect can handle this one
                            crs = store.getSQLDialect().createCRS(g.getSRID(), cx);
                            tb.userData(JDBCDataStore.JDBC_NATIVE_SRID, g.getSRID());
                        }
                        if (crs == null) {
                            crs = gd.getCoordinateReferenceSystem();
                        }
                        tb.add(ad.getLocalName(), binding, crs);
                    }

                } else {
                    tb.add(ad);
                }
            }
            return tb.buildFeatureType();
        } catch (SQLException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } finally {
            store.closeSafe(cx);
        }
    }

    protected VirtualTable buildVirtualTable() {
        VirtualTable vt = new VirtualTable(name, sql);
        attProvider.fillVirtualTable(vt);
        paramProvider.updateVirtualTable(vt);
        return vt;
    }

    /**
     * Data stores tend to return IOExceptions with no explanation, and the actual error coming from
     * the db is in the cause. This method extracts the first not null message in the cause chain
     */
    protected String getFirstErrorMessage(Throwable t) {
        Throwable original = t;

        while (!(t instanceof SQLException)) {
            t = t.getCause();
            if (t == null) {
                break;
            }
        }

        if (t == null) {
            return original.getMessage();
        } else {
            return t.getMessage();
        }
    }

    protected abstract void onSave();

    protected abstract void onCancel();

    /**
     * Displays the geometry type in the geom type drop down
     *
     * @author Andrea Aime - OpenGeo
     */
    static class GeometryTypeRenderer extends ChoiceRenderer<Class<? extends Geometry>> {

        public Object getDisplayValue(Class<? extends Geometry> object) {
            return object.getSimpleName();
        }

        public String getIdValue(Class<? extends Geometry> object, int index) {
            return (String) getDisplayValue(object);
        }
    }

    /** Validates the regular expression syntax */
    static class RegexpValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> iv) {
            String value = iv.getValue();
            if (value != null) {
                try {
                    Pattern.compile(value);
                } catch (PatternSyntaxException e) {
                    ValidationError error = new ValidationError(this);
                    error.setVariable("regexp", value);
                    error.setVariable("error", e.getMessage().replaceAll("\\^?", ""));
                    iv.error(error);
                }
            }
        }
    }

    /** Checks the SQL view name is unique */
    class ViewNameValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            String vtName = validatable.getValue();

            final DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
            List<FeatureTypeInfo> ftis =
                    getCatalog().getResourcesByStore(store, FeatureTypeInfo.class);
            for (FeatureTypeInfo curr : ftis) {
                VirtualTable currvt =
                        curr.getMetadata()
                                .get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class);
                if (currvt != null) {
                    if (typeInfoId == null || !typeInfoId.equals(curr.getId())) {
                        if (currvt.getName().equals(vtName)) {
                            IValidationError err =
                                    new ValidationError("duplicateSqlViewName")
                                            .addKey("duplicateSqlViewName")
                                            .setVariable("name", vtName)
                                            .setVariable("typeName", curr.getName());
                            validatable.error(err);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
