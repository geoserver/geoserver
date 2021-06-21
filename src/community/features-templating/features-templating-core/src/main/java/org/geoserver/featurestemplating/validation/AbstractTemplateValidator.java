package org.geoserver.featurestemplating.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.expressions.XpathFunction;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/** Base class for template validation against a target feature type */
public abstract class AbstractTemplateValidator {

    private static final Logger LOGGER = Logging.getLogger(AbstractTemplateValidator.class);

    private String failingAttribute;

    private List<String> sourcesFound = new ArrayList<>();

    public boolean validateTemplate(RootBuilder root) {
        try {
            String source = null;

            ValidateExpressionVisitor validateVisitor =
                    new ValidateExpressionVisitor(new TemplateBuilderContext(getFeatureType()));
            return validateExpressions(root, validateVisitor, source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** The feature type against which the template is being validated */
    protected abstract FeatureType getFeatureType() throws IOException;

    /**
     * The target type name against which the template is being validated. By default, <code>
     * getFeatureType().getName().getLocalPart()</code> is returned.
     *
     * @throws IOException
     */
    public String getTypeName() throws IOException {
        return getFeatureType().getName().getLocalPart();
    }

    private boolean validateExpressions(
            TemplateBuilder builder, ValidateExpressionVisitor visitor, String source)
            throws IOException {
        for (TemplateBuilder jb : builder.getChildren()) {
            String siblingSource = null;
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                Expression toValidate = getExpressionToValidate(djb, source, djb.getContextPos());
                if (!validate(toValidate, visitor)) {
                    if (djb.getCql() != null) {
                        this.failingAttribute =
                                "Key: " + djb.getKey() + " Value: " + CQL.toCQL(djb.getCql());
                        return false;
                    } else if (djb.getXpath() != null) {
                        this.failingAttribute =
                                "Key: " + djb.getKey() + " Value: " + CQL.toCQL(djb.getXpath());
                        return false;
                    }
                }
            } else if (jb instanceof SourceBuilder) {
                SourceBuilder sb = ((SourceBuilder) jb);
                if (sb.getSource() != null && sb.getStrSource() != null) {
                    boolean topLevelFeature = sb.isTopLevelFeature();
                    boolean isValid = true;
                    if (!topLevelFeature) {
                        PropertyName pn = getSourceToValidate(sb, source);
                        isValid = validate(pn, visitor);
                        siblingSource = pn.getPropertyName();
                    } else if (LOGGER.isLoggable(Level.WARNING))
                        // topLevelSource validation if fails will
                        // log a failing warn but will not make the entire validation to fail.
                        // this is because we are matching at hand feature name and featureType name
                        // and the matching cannot cover all the possible cases of mismatch between
                        // names
                        validateTopLevelSource(sb);
                    if (!isValid) {
                        failingAttribute = "Source: " + sb.getStrSource();
                        return false;
                    }
                }
                if (!validateExpressions(
                        jb, visitor, siblingSource != null ? siblingSource : source)) {
                    return false;
                }
            }
            if (jb instanceof AbstractTemplateBuilder) {
                Filter filter =
                        getFilterToValidate(
                                (AbstractTemplateBuilder) jb,
                                siblingSource != null ? siblingSource : source);
                if (filter != null && !validate(filter, visitor)) {
                    failingAttribute = "Filter: " + CQL.toCQL(filter);
                    return false;
                }
            }
        }
        return true;
    }

    public String getFailingAttribute() {
        return failingAttribute;
    }

    public boolean validate(Object toValidate, ValidateExpressionVisitor visitor) {

        if (toValidate instanceof Expression) {
            ((Expression) toValidate).accept(visitor, null);
        } else {
            ((Filter) toValidate).accept(visitor, null);
        }
        return visitor.isValid();
    }

    private void validateTopLevelSource(SourceBuilder builder) {
        try {
            boolean result;
            String strSource = builder.getStrSource();
            Name name = getFeatureType().getName();
            if (strSource.indexOf(":") != -1) {
                String[] nameAr = strSource.split(":");
                if (builder.getNamespaces() != null) {
                    String prefix = nameAr[0];
                    String uri = builder.getNamespaces().getURI(prefix);
                    result =
                            name.getNamespaceURI().equals(uri)
                                    && localPartMatches(name.getLocalPart(), nameAr[1]);
                } else {
                    result = localPartMatches(name.getLocalPart(), nameAr[1]);
                }
            } else result = name.getLocalPart().equals(strSource);
            if (!result)
                LOGGER.warning(
                        "Failed to validate the topLevel Feature source against the FeatureType. "
                                + "The source is "
                                + strSource
                                + " and the FeatureType name is "
                                + name.toString()
                                + ". The top level source"
                                + " might be still valid anyway");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean localPartMatches(String typeName, String sourceName) {
        boolean matchName = typeName.equals(sourceName);
        if (!matchName) {
            String withType = sourceName + "Type";
            matchName = typeName.equalsIgnoreCase(withType);
        }
        if (!matchName) {
            // last attempt we might have a SmartDataLoader complex features which add to
            // the Feature the suffix 'Feature'
            sourceName = sourceName.replaceAll("Feature", "");
            matchName = typeName.equalsIgnoreCase(sourceName);
        }
        return matchName;
    }

    /**
     * Produce an AttributeExpressionImpl from the source attribute, suitable to be validated, eg.
     * taking cares of handling properly changes of context
     */
    private AttributeExpressionImpl getSourceToValidate(SourceBuilder sb, String strSource) {
        AttributeExpressionImpl source = (AttributeExpressionImpl) sb.getSource();
        String sourcePart = sb.getStrSource();
        if (strSource == null) {
            strSource = sourcePart;
        } else {
            strSource += "/" + sourcePart;
        }
        sourcesFound.add(sourcePart);
        return new AttributeExpressionImpl(strSource, source.getNamespaceContext());
    }

    /**
     * Produce a Filter from the filter attribute, suitable to be validated eg. taking cares of
     * handling properly ../ and changes of context
     */
    private Filter getFilterToValidate(AbstractTemplateBuilder ab, String source) {
        if (ab.getFilter() != null) {
            return (Filter)
                    completeXpathWithVisitor(ab.getFilter(), source, ab.getFilterContextPos());
        }
        return null;
    }

    /**
     * Produce an expression from the xpath or the cql expression hold by the DynamicBuilder,
     * suitable to be validated, eg. taking care of handling properly ../ and changes of context
     */
    private Expression getExpressionToValidate(
            DynamicValueBuilder db, String source, int contextPos) {
        if (db.getXpath() != null) {
            return completeXpathForValidation(db.getXpath(), source, contextPos);
        } else {
            return (Expression) completeXpathWithVisitor(db.getCql(), source, contextPos);
        }
    }

    private PropertyName completeXpathForValidation(
            PropertyName pn, String source, int contextPos) {
        String strXpath = pn.getPropertyName();
        int i = 0;
        String newSource = source;
        if (newSource != null) {
            int lastSource = sourcesFound.size() - 1;
            while (i < contextPos) {
                String toReplace = sourcesFound.get(lastSource - i);
                strXpath = strXpath.replaceFirst("\\.\\./", "");
                if (newSource.lastIndexOf('/') != -1) {
                    newSource = source.replace("/" + toReplace, "");
                } else {
                    newSource = "";
                }
                i++;
            }
            String newXpath;
            if (!newSource.equals("") && !strXpath.startsWith(newSource)) {
                newXpath = newSource + "/" + pn.getPropertyName();
            } else {
                newXpath = pn.getPropertyName();
            }
            if (pn instanceof AttributeExpressionImpl) {
                pn = new AttributeExpressionImpl(newXpath, pn.getNamespaceContext());
            } else if (pn instanceof XpathFunction) {
                ((XpathFunction) pn).setPropertyName(newXpath);
            }
        }
        return pn;
    }

    private Object completeXpathWithVisitor(Object cql, String source, int contextPos) {
        DuplicatingFilterVisitor visitor =
                new DuplicatingFilterVisitor() {
                    @Override
                    public Object visit(PropertyName filter, Object extraData) {
                        filter = (PropertyName) super.visit(filter, extraData);
                        Object result = completeXpathForValidation(filter, source, contextPos);
                        if (result != null) return result;
                        return filter;
                    }
                };
        if (cql instanceof Expression) {
            return ((Expression) cql).accept(visitor, null);
        } else {
            return ((Filter) cql).accept(visitor, null);
        }
    }
}
