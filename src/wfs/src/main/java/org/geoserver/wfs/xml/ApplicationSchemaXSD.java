/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDCompositor;
import org.eclipse.xsd.XSDDerivationMethod;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDForm;
import org.eclipse.xsd.XSDImport;
import org.eclipse.xsd.XSDModelGroup;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDSchemaContent;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.util.XSDConstants;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.gml2.GML;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.WFS;
import org.geotools.xsd.SchemaIndex;
import org.geotools.xsd.Schemas;
import org.geotools.xsd.XSD;
import org.geotools.xsd.impl.SchemaIndexImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;

/**
 * XSD for an application schema of a geoserver feature type.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ApplicationSchemaXSD extends XSD {

    static Logger LOGGER = Logging.getLogger("org.geoserver.wfs");

    /** namespace for the app schema */
    NamespaceInfo ns;

    /** the catalog */
    Catalog catalog;
    /** base url used for schemaLocation */
    String baseURL;

    /** wfs schema */
    WFS wfs;

    /** list of types to be included in the app schema. */
    Collection<FeatureTypeInfo> types;

    /** type mapping profile */
    TypeMappingProfile typeMappingProfile;

    public ApplicationSchemaXSD(NamespaceInfo ns, Catalog catalog, String baseURL, WFS wfs) {
        this(ns, catalog, baseURL, wfs, Collections.EMPTY_LIST);
    }

    public ApplicationSchemaXSD(
            NamespaceInfo ns,
            Catalog catalog,
            String baseURL,
            WFS wfs,
            Collection<FeatureTypeInfo> types) {
        this.ns = ns;
        this.catalog = catalog;
        this.baseURL = baseURL;
        this.wfs = wfs;
        this.types = types;

        if (this.types == null) {
            types = Collections.EMPTY_LIST;
        }

        if (this.ns == null) {
            // ns set to null, if all given types are from the same namespace, us that one
            if (!types.isEmpty()) {
                Iterator<FeatureTypeInfo> i = types.iterator();
                NamespaceInfo namespace = i.next().getNamespace();
                while (i.hasNext()) {
                    FeatureTypeInfo type = i.next();
                    if (!namespace.equals(type.getNamespace())) {
                        namespace = null;
                        break;
                    }
                }

                if (namespace != null) {
                    this.ns = namespace;
                }
            }
        }

        // set up type mapping profiles
        Set<Schema> profiles = new LinkedHashSet<Schema>();
        for (XSD xsd : wfs.getAllDependencies()) {
            profiles.add(xsd.getTypeMappingProfile());
        }
        typeMappingProfile = new TypeMappingProfile(profiles);
    }

    @Override
    protected void addDependencies(Set dependencies) {
        dependencies.add(wfs);
    }

    @Override
    public String getNamespaceURI() {
        // namespace will be null in case where types from different namespaces are
        // included in this schema
        if (ns != null) {
            return ns.getURI();
        }
        return null;
    }

    @Override
    public String getSchemaLocation() {
        String schemaLocation =
                ResponseUtils.appendQueryString(
                        ResponseUtils.appendPath(baseURL, "wfs"),
                        "request=DescribeFeatureType&service=WFS&version=" + wfs.getVersion());

        if (types.isEmpty()) {
            schemaLocation =
                    ResponseUtils.appendQueryString(schemaLocation, "namespace=" + ns.getURI());
        } else {
            StringBuffer sb = new StringBuffer("typeName=");
            for (FeatureTypeInfo type : types) {
                sb.append(type.prefixedName()).append(",");
            }
            sb.setLength(sb.length() - 1);
            schemaLocation = ResponseUtils.appendQueryString(schemaLocation, sb.toString());
        }

        return schemaLocation;
    }

    @Override
    protected XSDSchema buildSchema() throws IOException {
        XSDFactory factory = XSDFactory.eINSTANCE;
        XSDSchema schema = factory.createXSDSchema();

        schema.setSchemaForSchemaQNamePrefix("xsd");
        schema.getQNamePrefixToNamespaceMap().put("xsd", XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
        schema.setElementFormDefault(XSDForm.get(XSDForm.QUALIFIED));

        if (ns == null) {
            // there must be types from different namespaces included, build a schema
            // with imports
            if (types.isEmpty()) {
                // build for all namespaces
                buildSchemaImports(catalog.getNamespaces(), schema, factory);
            } else {
                Set<NamespaceInfo> namespaces = new HashSet<NamespaceInfo>();
                for (FeatureTypeInfo type : types) {
                    namespaces.add(type.getNamespace());
                }
                buildSchemaImports(namespaces, schema, factory);
            }
        } else {
            schema.setTargetNamespace(ns.getURI());
            schema.getQNamePrefixToNamespaceMap().put(ns.getPrefix(), ns.getURI());

            schema.getQNamePrefixToNamespaceMap().put("wfs", WFS.NAMESPACE);
            schema.getQNamePrefixToNamespaceMap().put("gml", GML.NAMESPACE);

            // import wfs schema
            synchronized (Schemas.class) {
                Schemas.importSchema(schema, wfs.getSchema());
            }
            schema.resolveElementDeclaration(WFS.NAMESPACE, "FeatureCollection");
            /*
            XSDImport imprt = factory.createXSDImport();
            imprt.setNamespace(WFS.NAMESPACE);

            String location = ResponseUtils.appendPath( baseURL, "schemas/wfs/");
            location += wfs.getVersion() + "/";
            location += new File( new URL(wfs.getSchemaLocation()).getFile() ).getName();
            imprt.setNamespace(location);
            imprt.setResolvedSchema(wfs.getSchema());
            schema.getContents().add(imprt);
            */

            Collection<FeatureTypeInfo> featureTypes = types;
            if (featureTypes == null || featureTypes.isEmpty()) {
                // grab all from catalog
                featureTypes = catalog.getFeatureTypesByNamespace(ns);
            }

            SchemaIndexImpl index = new SchemaIndexImpl(new XSDSchema[] {schema});
            // all types in same namespace, write out the schema
            for (FeatureTypeInfo featureType : featureTypes) {
                buildSchemaContent(featureType, schema, factory, index);
            }
        }

        return schema;
    }

    void buildSchemaImports(
            Collection<NamespaceInfo> namespaces, XSDSchema schema, XSDFactory factory) {

        Map<String, String> params =
                params(
                        "service",
                        "wfs",
                        "request",
                        "DescribeFeatureType",
                        "version",
                        wfs.getVersion());

        //        String schemaLocation = ResponseUtils.appendQueryString(ResponseUtils.appendPath(
        // baseURL, "wfs"),
        //            "service=wfs&request=DescribeFeatureType&version=" + wfs.getVersion() +
        // "&namespace=");

        for (NamespaceInfo namespace : namespaces) {
            XSDImport imprt = factory.createXSDImport();
            imprt.setNamespace(namespace.getURI());
            params.put("namespace", namespace.getPrefix());
            imprt.setSchemaLocation(buildURL(baseURL, "wfs", params, URLType.SERVICE));

            synchronized (Schemas.class) {
                schema.getContents().add(imprt);
            }
        }
    }

    void buildSchemaContent(
            FeatureTypeInfo featureTypeInfo,
            XSDSchema schema,
            XSDFactory factory,
            SchemaIndex index)
            throws IOException {

        // look if the schema for the type is already defined
        String prefix = featureTypeInfo.getNamespace().getPrefix();
        String store = featureTypeInfo.getStore().getName();
        String name = featureTypeInfo.getName();

        Resource schemaFile =
                catalog.getResourceLoader()
                        .get(
                                "workspaces"
                                        + "/"
                                        + prefix
                                        + "/"
                                        + store
                                        + "/"
                                        + name
                                        + "/schema.xsd");

        if (schemaFile.getType() == Type.RESOURCE) {
            // schema file found, parse it and lookup the complex type
            List locators = new ArrayList();
            for (XSD xsd : wfs.getAllDependencies()) {
                locators.add(xsd.createSchemaLocator());
            }
            XSDSchema ftSchema = null;

            try {
                ftSchema = Schemas.parse(schemaFile.file().getAbsolutePath(), locators, null);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to parse schema: " + schemaFile.file().getAbsolutePath(),
                        e);
            }

            if (ftSchema != null) {
                // add the contents of this schema to the schema being built
                // look up the complex type
                List contents = ftSchema.getContents();

                for (Iterator i = contents.iterator(); i.hasNext(); ) {
                    XSDSchemaContent content = (XSDSchemaContent) i.next();
                    content.setElement(null);
                }

                schema.getContents().addAll(contents);
                schema.updateElement();

                return;
            }
        }

        // build the type manually
        SimpleFeatureType featureType = (SimpleFeatureType) featureTypeInfo.getFeatureType();

        XSDComplexTypeDefinition complexType = factory.createXSDComplexTypeDefinition();
        complexType.setName(name + "Type");

        complexType.setDerivationMethod(XSDDerivationMethod.EXTENSION_LITERAL);
        complexType.setBaseTypeDefinition(
                schema.resolveComplexTypeDefinition(GML.NAMESPACE, "AbstractFeatureType"));

        XSDModelGroup group = factory.createXSDModelGroup();
        group.setCompositor(XSDCompositor.SEQUENCE_LITERAL);

        List attributes = featureType.getAttributeDescriptors();

        for (int i = 0; i < attributes.size(); i++) {
            AttributeDescriptor attribute = (AttributeDescriptor) attributes.get(i);
            if (filterAttributeType(attribute)) {
                continue;
            }

            XSDElementDeclaration element = factory.createXSDElementDeclaration();
            element.setName(attribute.getLocalName());
            element.setNillable(attribute.isNillable());

            Class binding = attribute.getType().getBinding();
            Name typeName = findTypeName(binding);

            if (typeName == null) {
                throw new NullPointerException(
                        "Could not find a type for property: "
                                + attribute.getName()
                                + " of type: "
                                + binding.getName());
            }

            XSDTypeDefinition type =
                    index.getTypeDefinition(
                            new QName(typeName.getNamespaceURI(), typeName.getLocalPart()));
            if (type == null) {
                throw new IllegalStateException("Could not find type: " + typeName);
            }
            // XSDTypeDefinition type = schema.resolveTypeDefinition(typeName.getNamespaceURI(),
            //        typeName.getLocalPart());
            element.setTypeDefinition(type);

            XSDParticle particle = factory.createXSDParticle();
            particle.setMinOccurs(attribute.getMinOccurs());
            particle.setMaxOccurs(attribute.getMaxOccurs());
            particle.setContent(element);
            group.getContents().add(particle);
        }

        XSDParticle particle = factory.createXSDParticle();
        particle.setContent(group);

        complexType.setContent(particle);

        schema.getContents().add(complexType);

        XSDElementDeclaration element = factory.createXSDElementDeclaration();
        element.setName(name);

        element.setSubstitutionGroupAffiliation(
                schema.resolveElementDeclaration(GML.NAMESPACE, "_Feature"));
        element.setTypeDefinition(complexType);

        schema.getContents().add(element);
        schema.updateElement();
    }

    boolean filterAttributeType(AttributeDescriptor attribute) {
        return "name".equals(attribute.getName().getLocalPart())
                || "description".equals(attribute.getName().getLocalPart())
                || "boundedBy".equals(attribute.getName().getLocalPart());
    }

    Name findTypeName(Class binding) {
        return typeMappingProfile.name(binding);
    }
}
