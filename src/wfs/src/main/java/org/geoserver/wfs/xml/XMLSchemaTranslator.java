/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * XMLSchemaTranslator purpose.
 *
 * <p>This instance of the NameSpaceTranslator should be used with http://www.w3.org/2001/XMLSchema namespace.
 *
 * <p>Instances of this object should always be retrieved through the NameSpaceTranslatorFactory.
 *
 * <p>Added a bit of a hack to get the right default mappings. Added isDefault to the classes we want. Note that this
 * list comes from org.geotools.gml.producer.FeatureTypeTransformer.
 *
 * @see NameSpaceTranslatorFactory
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 */
public class XMLSchemaTranslator extends NameSpaceTranslator {
    private Set<NameSpaceElement> elements;

    /**
     * XMLSchemaTranslator constructor.
     *
     * <p>Description
     */
    public XMLSchemaTranslator(String prefix) {
        super(prefix);
        elements = new HashSet<>();
        elements.add(new BooleanElement(prefix));
        elements.add(new DecimalElement(prefix));
        elements.add(new IntegerElement(prefix));
        elements.add(new NegativeIntegerElement(prefix));
        elements.add(new NonNegativeIntegerElement(prefix));
        elements.add(new PositiveIntegerElement(prefix));
        elements.add(new LongElement(prefix));
        elements.add(new IntElement(prefix));
        elements.add(new ShortElement(prefix));
        elements.add(new ByteElement(prefix));
        elements.add(new UnsignedLongElement(prefix));
        elements.add(new UnsignedShortElement(prefix));
        elements.add(new UnsignedIntElement(prefix));
        elements.add(new UnsignedByteElement(prefix));
        elements.add(new FloatElement(prefix));
        elements.add(new DoubleElement(prefix));
        elements.add(new DateElement(prefix));
        elements.add(new DateTimeElement(prefix));
        elements.add(new DurationElement(prefix));
        elements.add(new GDayElement(prefix));
        elements.add(new GMonthElement(prefix));
        elements.add(new GMonthDayElement(prefix));
        elements.add(new GYearElement(prefix));
        elements.add(new GYearMonthElement(prefix));
        elements.add(new TimeElement(prefix));
        elements.add(new IDElement(prefix));
        elements.add(new IDREFElement(prefix));
        elements.add(new IDREFSElement(prefix));
        elements.add(new ENTITYElement(prefix));
        elements.add(new ENTITIESElement(prefix));
        elements.add(new NMTOKENElement(prefix));
        elements.add(new NMTOKENSElement(prefix));
        elements.add(new NOTATIONElement(prefix));
        elements.add(new StringElement(prefix));
        elements.add(new NormalizedStringElement(prefix));
        elements.add(new TokenElement(prefix));
        elements.add(new QNameElement(prefix));
        elements.add(new NameElement(prefix));
        elements.add(new NCNameElement(prefix));
    }

    /**
     * Implementation of getElements.
     *
     * @see org.vfny.geoserver.global.xml.NameSpaceTranslator#getElements()
     */
    @Override
    public Set<NameSpaceElement> getElements() {
        return elements;
    }

    /**
     * Implementation of getNameSpace.
     *
     * @see org.vfny.geoserver.global.xml.NameSpaceTranslator#getNameSpace()
     */
    @Override
    public String getNameSpace() {
        return "http://www.w3.org/2001/XMLSchema";
    }
}

class BooleanElement extends NameSpaceElement {
    public BooleanElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "boolean";
    }

    @Override
    public String getTypeRefName() {
        return "boolean";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":boolean";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":boolean";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":boolean";
        }

        if (this.prefix != null) {
            return this.prefix + ":boolean";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":boolean";
        }

        if (this.prefix != null) {
            return this.prefix + ":boolean";
        }

        return null;
    }

    @Override
    public Class<Boolean> getJavaClass() {
        return Boolean.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class DecimalElement extends NameSpaceElement {
    public DecimalElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "decimal";
    }

    @Override
    public String getTypeRefName() {
        return "decimal";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":decimal";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":decimal";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":decimal";
        }

        if (this.prefix != null) {
            return this.prefix + ":decimal";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":decimal";
        }

        if (this.prefix != null) {
            return this.prefix + ":decimal";
        }

        return null;
    }

    @Override
    public Class<BigDecimal> getJavaClass() {
        return BigDecimal.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class IntegerElement extends NameSpaceElement {
    public IntegerElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "integer";
    }

    @Override
    public String getTypeRefName() {
        return "integer";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":integer";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":integer";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":integer";
        }

        if (this.prefix != null) {
            return this.prefix + ":integer";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":integer";
        }

        if (this.prefix != null) {
            return this.prefix + ":integer";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NegativeIntegerElement extends NameSpaceElement {
    public NegativeIntegerElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "negativeInteger";
    }

    @Override
    public String getTypeRefName() {
        return "negativeInteger";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":negativeInteger";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":negativeInteger";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":negativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":negativeInteger";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":negativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":negativeInteger";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NonNegativeIntegerElement extends NameSpaceElement {
    public NonNegativeIntegerElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "nonNegativeInteger";
    }

    @Override
    public String getTypeRefName() {
        return "nonNegativeInteger";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":nonNegativeInteger";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":nonNegativeInteger";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":nonNegativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":nonNegativeInteger";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":nonNegativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":nonNegativeInteger";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class PositiveIntegerElement extends NameSpaceElement {
    public PositiveIntegerElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "positiveInteger";
    }

    @Override
    public String getTypeRefName() {
        return "positiveInteger";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":positiveInteger";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":positiveInteger";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":positiveInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":positiveInteger";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":positiveInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":positiveInteger";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class LongElement extends NameSpaceElement {
    public LongElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "long";
    }

    @Override
    public String getTypeRefName() {
        return "long";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":long";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":long";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":long";
        }

        if (this.prefix != null) {
            return this.prefix + ":long";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":long";
        }

        if (this.prefix != null) {
            return this.prefix + ":long";
        }

        return null;
    }

    @Override
    public Class<Long> getJavaClass() {
        return Long.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class IntElement extends NameSpaceElement {
    public IntElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "int";
    }

    @Override
    public String getTypeRefName() {
        return "int";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":int";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":int";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":int";
        }

        if (this.prefix != null) {
            return this.prefix + ":int";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":int";
        }

        if (this.prefix != null) {
            return this.prefix + ":int";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class ShortElement extends NameSpaceElement {
    public ShortElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "short";
    }

    @Override
    public String getTypeRefName() {
        return "short";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":short";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":short";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":short";
        }

        if (this.prefix != null) {
            return this.prefix + ":short";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":short";
        }

        if (this.prefix != null) {
            return this.prefix + ":short";
        }

        return null;
    }

    @Override
    public Class<Short> getJavaClass() {
        return Short.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class ByteElement extends NameSpaceElement {
    public ByteElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "byte";
    }

    @Override
    public String getTypeRefName() {
        return "byte";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":byte";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":byte";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":byte";
        }

        if (this.prefix != null) {
            return this.prefix + ":byte";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":byte";
        }

        if (this.prefix != null) {
            return this.prefix + ":byte";
        }

        return null;
    }

    @Override
    public Class<Byte> getJavaClass() {
        return Byte.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class UnsignedLongElement extends NameSpaceElement {
    public UnsignedLongElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "unsignedLong";
    }

    @Override
    public String getTypeRefName() {
        return "unsignedLong";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedLong";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedLong";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedLong";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedLong";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedLong";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedLong";
        }

        return null;
    }

    @Override
    public Class<Long> getJavaClass() {
        return Long.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class UnsignedShortElement extends NameSpaceElement {
    public UnsignedShortElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "unsignedShort";
    }

    @Override
    public String getTypeRefName() {
        return "unsignedShort";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedShort";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedShort";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedShort";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedShort";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedShort";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedShort";
        }

        return null;
    }

    @Override
    public Class<Short> getJavaClass() {
        return Short.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class UnsignedIntElement extends NameSpaceElement {
    public UnsignedIntElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "unsignedInt";
    }

    @Override
    public String getTypeRefName() {
        return "unsignedInt";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedInt";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedInt";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedInt";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedInt";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedInt";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedInt";
        }

        return null;
    }

    @Override
    public Class<Integer> getJavaClass() {
        return Integer.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class UnsignedByteElement extends NameSpaceElement {
    public UnsignedByteElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "unsignedByte";
    }

    @Override
    public String getTypeRefName() {
        return "unsignedByte";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedByte";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedByte";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedByte";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedByte";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedByte";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedByte";
        }

        return null;
    }

    @Override
    public Class<Byte> getJavaClass() {
        return Byte.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class FloatElement extends NameSpaceElement {
    public FloatElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "float";
    }

    @Override
    public String getTypeRefName() {
        return "float";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":float";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":float";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":float";
        }

        if (this.prefix != null) {
            return this.prefix + ":float";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":float";
        }

        if (this.prefix != null) {
            return this.prefix + ":float";
        }

        return null;
    }

    @Override
    public Class<Float> getJavaClass() {
        return Float.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class DoubleElement extends NameSpaceElement {
    public DoubleElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "double";
    }

    @Override
    public String getTypeRefName() {
        return "double";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":double";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":double";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":double";
        }

        if (this.prefix != null) {
            return this.prefix + ":double";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":double";
        }

        if (this.prefix != null) {
            return this.prefix + ":double";
        }

        return null;
    }

    @Override
    public Class<Double> getJavaClass() {
        return Double.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class DateElement extends NameSpaceElement {
    public DateElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "date";
    }

    @Override
    public String getTypeRefName() {
        return "date";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":date";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":date";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":date";
        }

        if (this.prefix != null) {
            return this.prefix + ":date";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":date";
        }

        if (this.prefix != null) {
            return this.prefix + ":date";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class DateTimeElement extends NameSpaceElement {
    public DateTimeElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "dateTime";
    }

    @Override
    public String getTypeRefName() {
        return "dateTime";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":dateTime";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":dateTime";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":dateTime";
        }

        if (this.prefix != null) {
            return this.prefix + ":dateTime";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":dateTime";
        }

        if (this.prefix != null) {
            return this.prefix + ":dateTime";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class DurationElement extends NameSpaceElement {
    public DurationElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "duration";
    }

    @Override
    public String getTypeRefName() {
        return "duration";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":duration";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":duration";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":duration";
        }

        if (this.prefix != null) {
            return this.prefix + ":duration";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":duration";
        }

        if (this.prefix != null) {
            return this.prefix + ":duration";
        }

        return null;
    }

    @Override
    public Class<Long> getJavaClass() {
        return Long.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class GDayElement extends NameSpaceElement {
    public GDayElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "gDay";
    }

    @Override
    public String getTypeRefName() {
        return "gDay";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":gDay";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":gDay";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gDay";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gDay";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class GMonthElement extends NameSpaceElement {
    public GMonthElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "gMonth";
    }

    @Override
    public String getTypeRefName() {
        return "gMonth";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":gMonth";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":gMonth";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonth";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonth";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class GMonthDayElement extends NameSpaceElement {
    public GMonthDayElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "gMonthDay";
    }

    @Override
    public String getTypeRefName() {
        return "gMonthDay";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":gMonthDay";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":gMonthDay";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonthDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonthDay";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonthDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonthDay";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class GYearElement extends NameSpaceElement {
    public GYearElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "gYear";
    }

    @Override
    public String getTypeRefName() {
        return "gYear";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":gYear";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":gYear";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYear";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYear";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYear";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYear";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class GYearMonthElement extends NameSpaceElement {
    public GYearMonthElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "gYearMonth";
    }

    @Override
    public String getTypeRefName() {
        return "gYearMonth";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":gYearMonth";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":gYearMonth";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYearMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYearMonth";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYearMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYearMonth";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class TimeElement extends NameSpaceElement {
    public TimeElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "time";
    }

    @Override
    public String getTypeRefName() {
        return "time";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":time";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":time";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":time";
        }

        if (this.prefix != null) {
            return this.prefix + ":time";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":time";
        }

        if (this.prefix != null) {
            return this.prefix + ":time";
        }

        return null;
    }

    @Override
    public Class<Date> getJavaClass() {
        return Date.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class IDElement extends NameSpaceElement {
    public IDElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "ID";
    }

    @Override
    public String getTypeRefName() {
        return "ID";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":ID";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":ID";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ID";
        }

        if (this.prefix != null) {
            return this.prefix + ":ID";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ID";
        }

        if (this.prefix != null) {
            return this.prefix + ":ID";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class IDREFElement extends NameSpaceElement {
    public IDREFElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "IDREF";
    }

    @Override
    public String getTypeRefName() {
        return "IDREF";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":IDREF";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":IDREF";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREF";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREF";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREF";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREF";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class IDREFSElement extends NameSpaceElement {
    public IDREFSElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "IDREFS";
    }

    @Override
    public String getTypeRefName() {
        return "IDREFS";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":IDREFS";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":IDREFS";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREFS";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREFS";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREFS";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREFS";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class ENTITYElement extends NameSpaceElement {
    public ENTITYElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "ENTITY";
    }

    @Override
    public String getTypeRefName() {
        return "ENTITY";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":ENTITY";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":ENTITY";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITY";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITY";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITY";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITY";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class ENTITIESElement extends NameSpaceElement {
    public ENTITIESElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "ENTITIES";
    }

    @Override
    public String getTypeRefName() {
        return "ENTITIES";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":ENTITIES";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":ENTITIES";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITIES";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITIES";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITIES";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITIES";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NMTOKENElement extends NameSpaceElement {
    public NMTOKENElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "NMTOKEN";
    }

    @Override
    public String getTypeRefName() {
        return "NMTOKEN";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":NMTOKEN";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":NMTOKEN";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKEN";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKEN";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKEN";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKEN";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NMTOKENSElement extends NameSpaceElement {
    public NMTOKENSElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "NMTOKENS";
    }

    @Override
    public String getTypeRefName() {
        return "NMTOKENS";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":NMTOKENS";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":NMTOKENS";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKENS";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKENS";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKENS";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKENS";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NOTATIONElement extends NameSpaceElement {
    public NOTATIONElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "NOTATION";
    }

    @Override
    public String getTypeRefName() {
        return "NOTATION";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":NOTATION";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":NOTATION";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NOTATION";
        }

        if (this.prefix != null) {
            return this.prefix + ":NOTATION";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NOTATION";
        }

        if (this.prefix != null) {
            return this.prefix + ":NOTATION";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class StringElement extends NameSpaceElement {
    public StringElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "string";
    }

    @Override
    public String getTypeRefName() {
        return "string";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":string";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":string";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":string";
        }

        if (this.prefix != null) {
            return this.prefix + ":string";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":string";
        }

        if (this.prefix != null) {
            return this.prefix + ":string";
        }

        return null;
    }

    @Override
    public Class<String> getJavaClass() {
        return String.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}

class NormalizedStringElement extends NameSpaceElement {
    public NormalizedStringElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "normalizedString";
    }

    @Override
    public String getTypeRefName() {
        return "normalizedString";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":normalizedString";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":normalizedString";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":normalizedString";
        }

        if (this.prefix != null) {
            return this.prefix + ":normalizedString";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":normalizedString";
        }

        if (this.prefix != null) {
            return this.prefix + ":normalizedString";
        }

        return null;
    }

    @Override
    public Class<String> getJavaClass() {
        return String.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class TokenElement extends NameSpaceElement {
    public TokenElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "token";
    }

    @Override
    public String getTypeRefName() {
        return "token";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":token";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":token";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":token";
        }

        if (this.prefix != null) {
            return this.prefix + ":token";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":token";
        }

        if (this.prefix != null) {
            return this.prefix + ":token";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class QNameElement extends NameSpaceElement {
    public QNameElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "QName";
    }

    @Override
    public String getTypeRefName() {
        return "QName";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":QName";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":QName";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":QName";
        }

        if (this.prefix != null) {
            return this.prefix + ":QName";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":QName";
        }

        if (this.prefix != null) {
            return this.prefix + ":QName";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NameElement extends NameSpaceElement {
    public NameElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "Name";
    }

    @Override
    public String getTypeRefName() {
        return "Name";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":Name";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":Name";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":Name";
        }

        if (this.prefix != null) {
            return this.prefix + ":Name";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":Name";
        }

        if (this.prefix != null) {
            return this.prefix + ":Name";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}

class NCNameElement extends NameSpaceElement {
    public NCNameElement(String prefix) {
        super(prefix);
    }

    @Override
    public String getTypeDefName() {
        return "NCName";
    }

    @Override
    public String getTypeRefName() {
        return "NCName";
    }

    @Override
    public String getQualifiedTypeDefName() {
        return prefix + ":NCName";
    }

    @Override
    public String getQualifiedTypeRefName() {
        return prefix + ":NCName";
    }

    @Override
    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NCName";
        }

        if (this.prefix != null) {
            return this.prefix + ":NCName";
        }

        return null;
    }

    @Override
    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NCName";
        }

        if (this.prefix != null) {
            return this.prefix + ":NCName";
        }

        return null;
    }

    @Override
    public Class<Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}
