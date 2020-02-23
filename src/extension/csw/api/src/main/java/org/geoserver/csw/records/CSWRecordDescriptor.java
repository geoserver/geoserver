/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.csw.util.NamespaceQualifier;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.data.Query;
import org.geotools.data.complex.feature.type.FeatureTypeRegistry;
import org.geotools.data.complex.util.EmfComplexFeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.feature.type.Types;
import org.geotools.filter.SortByImpl;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.v1_0.OGC;
import org.geotools.xsd.SchemaIndex;
import org.geotools.xsd.ows.OWS;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes the CSW records and provides some handy constants to help building features
 * representing CSW:Record.
 *
 * <p>A few remarks about the {@link #RECORD_TYPE} feature type:
 *
 * <ul>
 *   <li>The SimpleLiterals are complex elements with simple contents, which we cannot properly
 *       represent in GeoTools as the moment, the adopted solution is to have SimpleLiteral sport
 *       two properties, value and scheme, which means the property paths in filters and sort
 *       operations have to be adapted from <code>dc(t):elementName</code> to <code>
 *       dc(t):elementName/dc:value</code>
 *   <li>The ows:BoundingBox element can be repeated multiple times and can have different SRS in
 *       each instance, to deal with that we build a single geometry, a multipolygon, and keep the
 *       original bounding boxes in the attribute user data, under the {@link
 *       #RECORD_BBOX_DESCRIPTOR} key
 * </ul>
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordDescriptor extends AbstractRecordDescriptor {

    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /** Contains the declarations of common namespaces and prefixes used in the CSW world */
    public static final Name SIMPLE_LITERAL_SCHEME = new NameImpl(DC.NAMESPACE, "scheme");

    public static final Name SIMPLE_LITERAL_VALUE = new NameImpl(DC.NAMESPACE, "value");

    public static final Name DC_ELEMENT_NAME =
            new NameImpl(DC.NAMESPACE, DC.DCelement.getLocalPart());

    public static final NameImpl RECORD_BBOX_NAME = new NameImpl(OWS.NAMESPACE, "BoundingBox");

    public static final List<Name> BRIEF_ELEMENTS;

    public static final List<Name> SUMMARY_ELEMENTS;

    public static final NamespaceSupport NAMESPACES;

    public static final ComplexType SIMPLE_LITERAL;

    public static final AttributeDescriptor DC_ELEMENT;

    public static final AttributeDescriptor RECORD_BBOX_DESCRIPTOR;

    public static final FeatureType RECORD_TYPE;

    public static final AttributeDescriptor RECORD_DESCRIPTOR;

    static final CRSRecordProjectyPathAdapter PATH_EXTENDER;

    static final NamespaceQualifier NSS_QUALIFIER;

    static final DefaultCRSFilterVisitor CRS_DEFAULTER;

    static final ReprojectingFilterVisitor CRS_REPROJECTOR;

    public static final List<Name> QUERIABLES;

    static {
        // prepare the common namespace support
        NAMESPACES = new NamespaceSupport();
        NAMESPACES.declarePrefix("csw", CSW.NAMESPACE);
        NAMESPACES.declarePrefix("rim", "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
        NAMESPACES.declarePrefix("dc", DC.NAMESPACE);
        NAMESPACES.declarePrefix("dct", DCT.NAMESPACE);
        NAMESPACES.declarePrefix("ows", OWS.NAMESPACE);
        NAMESPACES.declarePrefix("ogc", OGC.NAMESPACE);

        // prepare the CSW record related types
        FeatureTypeFactory typeFactory = new FeatureTypeFactoryImpl();
        TypeBuilder builder = new TypeBuilder(typeFactory);

        // create the SimpleLiteral type
        builder.setNamespaceURI(DC.SimpleLiteral.getNamespaceURI());
        builder.name("scheme");
        builder.bind(URI.class);
        AttributeType schemeType = builder.attribute();
        builder.setNamespaceURI(DC.SimpleLiteral.getNamespaceURI());
        builder.name("value");
        builder.bind(Object.class);
        AttributeType valueType = builder.attribute();
        builder.setNillable(true);
        builder.addAttribute("scheme", schemeType);
        builder.addAttribute("value", valueType);
        builder.setName("SimpleLiteral");
        SIMPLE_LITERAL = builder.complex();

        builder.setNamespaceURI(OWS.NAMESPACE);
        builder.setName("BoundingBoxType");
        builder.setBinding(MultiPolygon.class);
        builder.crs(DEFAULT_CRS);
        AttributeType bboxType = builder.geometry();

        EmfComplexFeatureReader reader = EmfComplexFeatureReader.newInstance();

        SchemaIndex index = null;
        try {
            index = reader.parse(new URL("http://schemas.opengis.net/csw/2.0.2/record.xsd"));
        } catch (IOException e) {
            // this is fatal
            throw new RuntimeException("Failed to parse CSW Record Schemas", e);
        }

        FeatureTypeRegistry featureTypeRegistry =
                new FeatureTypeRegistry(
                        NAMESPACES,
                        typeFactory,
                        new RecordFeatureTypeRegistryConfiguration("RecordType"));

        featureTypeRegistry.register(SIMPLE_LITERAL);

        featureTypeRegistry.register(bboxType);

        featureTypeRegistry.addSchemas(index);

        RECORD_TYPE =
                (FeatureType)
                        featureTypeRegistry.getAttributeType(
                                new NameImpl(CSW.NAMESPACE, "RecordType"));

        RECORD_DESCRIPTOR =
                featureTypeRegistry.getDescriptor(new NameImpl(CSW.NAMESPACE, "Record"), null);

        RECORD_BBOX_DESCRIPTOR = (AttributeDescriptor) RECORD_TYPE.getDescriptor(RECORD_BBOX_NAME);
        DC_ELEMENT = (AttributeDescriptor) RECORD_TYPE.getDescriptor(DC_ELEMENT_NAME);

        // ---

        // setup the list of names for brief and summary records
        BRIEF_ELEMENTS =
                createNameList(
                        NAMESPACES, "dc:identifier", "dc:title", "dc:type", "ows:BoundingBox");
        SUMMARY_ELEMENTS =
                createNameList(
                        NAMESPACES,
                        "dc:identifier",
                        "dc:title",
                        "dc:type",
                        "dc:subject",
                        "dc:format",
                        "dc:relation",
                        "dct:modified",
                        "dct:abstract",
                        "dct:spatial",
                        "ows:BoundingBox");

        // create the xpath extender that fill adapt dc:title to dc:title/dc:value
        PATH_EXTENDER = new CRSRecordProjectyPathAdapter(NAMESPACES);
        // qualified the xpath in the filters
        NSS_QUALIFIER = new NamespaceQualifier(NAMESPACES);
        // applies the default CRS to geometry filters coming from the outside
        CRS_DEFAULTER = new DefaultCRSFilterVisitor(FF, DEFAULT_CRS);
        // transforms geometry filters into the internal representation
        CRS_REPROJECTOR = new ReprojectingFilterVisitor(FF, RECORD_TYPE);

        // build queriables list
        QUERIABLES =
                createNameList(
                        NAMESPACES,
                        "dc:contributor",
                        "dc:source",
                        "dc:language",
                        "dc:title",
                        "dc:subject",
                        "dc:creator",
                        "dc:type",
                        "ows:BoundingBox",
                        "dct:modified",
                        "dct:abstract",
                        "dc:relation",
                        "dc:date",
                        "dc:identifier",
                        "dc:publisher",
                        "dc:format",
                        "csw:AnyText",
                        "dc:rights");
    }

    /** Checks if a field is public static final */
    static boolean isConstant(Field field) {
        int modifier = field.getModifiers();
        return Modifier.isStatic(modifier)
                && Modifier.isPublic(modifier)
                && Modifier.isFinal(modifier);
    }

    @Override
    public AttributeDescriptor getFeatureDescriptor() {
        return RECORD_DESCRIPTOR;
    }

    @Override
    public String getOutputSchema() {
        return CSW.NAMESPACE;
    }

    @Override
    public List<Name> getPropertiesForElementSet(ElementSetType elementSet) {
        switch (elementSet) {
            case BRIEF:
                return CSWRecordDescriptor.BRIEF_ELEMENTS;
            case SUMMARY:
                return CSWRecordDescriptor.SUMMARY_ELEMENTS;
            default:
                return null;
        }
    }

    @Override
    public NamespaceSupport getNamespaceSupport() {
        return NAMESPACES;
    }

    @Override
    public Query adaptQuery(Query query) {
        Filter filter = query.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            Filter qualified = (Filter) filter.accept(NSS_QUALIFIER, null);
            Filter extended = (Filter) qualified.accept(PATH_EXTENDER, null);
            query.setFilter(extended);
        }

        SortBy[] sortBy = query.getSortBy();
        if (sortBy != null && sortBy.length > 0) {
            CSWPropertyPathExtender extender = new CSWPropertyPathExtender();
            for (int i = 0; i < sortBy.length; i++) {
                SortBy sb = sortBy[i];
                if (!SortBy.NATURAL_ORDER.equals(sb) && !SortBy.REVERSE_ORDER.equals(sb)) {
                    PropertyName name = sb.getPropertyName();
                    PropertyName extended = extender.extendProperty(name, FF, NAMESPACES);
                    sortBy[i] = new SortByImpl(extended, sb.getSortOrder());
                }
            }
            query.setSortBy(sortBy);
        }

        return query;
    }

    @Override
    public String getBoundingBoxPropertyName() {
        return "BoundingBox";
    }

    /** Locates the AttributeDescriptor corresponding to the specified element name */
    public static AttributeDescriptor getDescriptor(String elementName) {
        return (AttributeDescriptor) Types.findDescriptor(RECORD_TYPE, elementName);
    }

    @Override
    public List<Name> getQueryables() {
        return QUERIABLES;
    }

    @Override
    public String getQueryablesDescription() {
        return "SupportedDublinCoreQueryables";
    }

    @Override
    public PropertyName translateProperty(Name name) {
        return new CSWPropertyPathExtender()
                .extendProperty(buildPropertyName(NAMESPACES, name), FF, NAMESPACES);
    }

    public void verifySpatialFilters(Filter filter) {
        filter.accept(new SpatialFilterChecker(getFeatureType()), null);
    }

    // singleton

    private CSWRecordDescriptor() {}

    private static CSWRecordDescriptor INSTANCE;

    public static CSWRecordDescriptor getInstance() {
        if (INSTANCE == null) {
            // if there is a bean available, use the bean otherwise create other
            INSTANCE = GeoServerExtensions.bean(CSWRecordDescriptor.class);
            if (INSTANCE == null) {
                INSTANCE = new CSWRecordDescriptor();
            }
        }
        return INSTANCE;
    }
}
