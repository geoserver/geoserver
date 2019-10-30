/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import java.sql.Timestamp;
import java.util.Optional;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

class ImportContextMapper {

    static SimpleFeatureType CTX_FEATURE_TYPE;

    public static final String CONTEXT = "context";

    public static final String CREATED = "created";

    public static final String UPDATED = "updated";

    public static final String STATE = "state";

    public static final String USER = "user";

    static {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.length(32767); // import contexts can be pretty bulky
        builder.add(CONTEXT, String.class);
        // these fields are added to speed up lookups
        builder.add(CREATED, Timestamp.class);
        builder.add(UPDATED, Timestamp.class);
        builder.add(USER, String.class);
        builder.add(STATE, String.class);
        builder.setName("import_context");
        CTX_FEATURE_TYPE = builder.buildFeatureType();
    }

    /** The persister used to store the entire context as a XML string */
    private final XStreamPersister xp;

    public ImportContextMapper(Importer importer) {
        this.xp = importer.createXStreamPersisterXML();
    }

    public SimpleFeature toFeature(ImportContext ctx) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(CTX_FEATURE_TYPE);
        String xml = xp.getXStream().toXML(ctx);
        fb.set(CONTEXT, xml);
        fb.set(CREATED, ctx.getCreated());
        fb.set(UPDATED, ctx.getUpdated());
        fb.set(USER, ctx.getUser());
        fb.set(STATE, Optional.ofNullable(ctx.getState()).map(s -> s.name()).orElse(null));
        String id = ctx.getId() == null ? null : CTX_FEATURE_TYPE.getTypeName() + "." + ctx.getId();
        return fb.buildFeature(id);
    }

    public ImportContext toContext(SimpleFeature feature) {
        String xml = (String) feature.getAttribute(CONTEXT);
        ImportContext ctx = (ImportContext) xp.getXStream().fromXML(xml);
        ctx.setId(getContextId(feature.getID()));
        return ctx;
    }

    Long getContextId(String id) {
        if (id == null) {
            return null;
        }
        if (!id.startsWith(CTX_FEATURE_TYPE.getTypeName() + ".")) {
            throw new IllegalArgumentException(
                    "Was expecting a feature type " + CTX_FEATURE_TYPE.getTypeName());
        }
        return Long.parseLong(id.substring(CTX_FEATURE_TYPE.getTypeName().length() + 1));
    }
}
