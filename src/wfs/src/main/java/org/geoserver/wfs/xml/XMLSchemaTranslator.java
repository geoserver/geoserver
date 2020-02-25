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
 * <p>This instance of the NameSpaceTranslator should be used with http://www.w3.org/2001/XMLSchema
 * namespace.
 *
 * <p>Instances of this object should always be retrieved through the NameSpaceTranslatorFactory.
 *
 * <p>Added a bit of a hack to get the right default mappings. Added isDefault to the classes we
 * want. Note that this list comes from org.geotools.gml.producer.FeatureTypeTransformer.
 *
 * @see NameSpaceTranslatorFactory
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public class XMLSchemaTranslator extends NameSpaceTranslator {
    private HashSet elements;

    /**
     * XMLSchemaTranslator constructor.
     *
     * <p>Description
     */
    public XMLSchemaTranslator(String prefix) {
        super(prefix);
        elements = new HashSet();
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
    public Set getElements() {
        return elements;
    }

    /**
     * Implementation of getNameSpace.
     *
     * @see org.vfny.geoserver.global.xml.NameSpaceTranslator#getNameSpace()
     */
    public String getNameSpace() {
        return "http://www.w3.org/2001/XMLSchema";
    }
}

class BooleanElement extends NameSpaceElement {
    public BooleanElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "boolean";
    }

    public String getTypeRefName() {
        return "boolean";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":boolean";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":boolean";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":boolean";
        }

        if (this.prefix != null) {
            return this.prefix + ":boolean";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":boolean";
        }

        if (this.prefix != null) {
            return this.prefix + ":boolean";
        }

        return null;
    }

    public Class getJavaClass() {
        return Boolean.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class DecimalElement extends NameSpaceElement {
    public DecimalElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "decimal";
    }

    public String getTypeRefName() {
        return "decimal";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":decimal";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":decimal";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":decimal";
        }

        if (this.prefix != null) {
            return this.prefix + ":decimal";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":decimal";
        }

        if (this.prefix != null) {
            return this.prefix + ":decimal";
        }

        return null;
    }

    public Class getJavaClass() {
        return BigDecimal.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class IntegerElement extends NameSpaceElement {
    public IntegerElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "integer";
    }

    public String getTypeRefName() {
        return "integer";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":integer";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":integer";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":integer";
        }

        if (this.prefix != null) {
            return this.prefix + ":integer";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":integer";
        }

        if (this.prefix != null) {
            return this.prefix + ":integer";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NegativeIntegerElement extends NameSpaceElement {
    public NegativeIntegerElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "negativeInteger";
    }

    public String getTypeRefName() {
        return "negativeInteger";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":negativeInteger";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":negativeInteger";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":negativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":negativeInteger";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":negativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":negativeInteger";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NonNegativeIntegerElement extends NameSpaceElement {
    public NonNegativeIntegerElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "nonNegativeInteger";
    }

    public String getTypeRefName() {
        return "nonNegativeInteger";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":nonNegativeInteger";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":nonNegativeInteger";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":nonNegativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":nonNegativeInteger";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":nonNegativeInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":nonNegativeInteger";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class PositiveIntegerElement extends NameSpaceElement {
    public PositiveIntegerElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "positiveInteger";
    }

    public String getTypeRefName() {
        return "positiveInteger";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":positiveInteger";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":positiveInteger";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":positiveInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":positiveInteger";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":positiveInteger";
        }

        if (this.prefix != null) {
            return this.prefix + ":positiveInteger";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class LongElement extends NameSpaceElement {
    public LongElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "long";
    }

    public String getTypeRefName() {
        return "long";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":long";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":long";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":long";
        }

        if (this.prefix != null) {
            return this.prefix + ":long";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":long";
        }

        if (this.prefix != null) {
            return this.prefix + ":long";
        }

        return null;
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class IntElement extends NameSpaceElement {
    public IntElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "int";
    }

    public String getTypeRefName() {
        return "int";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":int";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":int";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":int";
        }

        if (this.prefix != null) {
            return this.prefix + ":int";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":int";
        }

        if (this.prefix != null) {
            return this.prefix + ":int";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class ShortElement extends NameSpaceElement {
    public ShortElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "short";
    }

    public String getTypeRefName() {
        return "short";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":short";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":short";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":short";
        }

        if (this.prefix != null) {
            return this.prefix + ":short";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":short";
        }

        if (this.prefix != null) {
            return this.prefix + ":short";
        }

        return null;
    }

    public Class getJavaClass() {
        return Short.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class ByteElement extends NameSpaceElement {
    public ByteElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "byte";
    }

    public String getTypeRefName() {
        return "byte";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":byte";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":byte";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":byte";
        }

        if (this.prefix != null) {
            return this.prefix + ":byte";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":byte";
        }

        if (this.prefix != null) {
            return this.prefix + ":byte";
        }

        return null;
    }

    public Class getJavaClass() {
        return Byte.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class UnsignedLongElement extends NameSpaceElement {
    public UnsignedLongElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "unsignedLong";
    }

    public String getTypeRefName() {
        return "unsignedLong";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedLong";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedLong";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedLong";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedLong";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedLong";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedLong";
        }

        return null;
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class UnsignedShortElement extends NameSpaceElement {
    public UnsignedShortElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "unsignedShort";
    }

    public String getTypeRefName() {
        return "unsignedShort";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedShort";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedShort";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedShort";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedShort";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedShort";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedShort";
        }

        return null;
    }

    public Class getJavaClass() {
        return Short.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class UnsignedIntElement extends NameSpaceElement {
    public UnsignedIntElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "unsignedInt";
    }

    public String getTypeRefName() {
        return "unsignedInt";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedInt";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedInt";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedInt";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedInt";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedInt";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedInt";
        }

        return null;
    }

    public Class getJavaClass() {
        return Integer.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class UnsignedByteElement extends NameSpaceElement {
    public UnsignedByteElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "unsignedByte";
    }

    public String getTypeRefName() {
        return "unsignedByte";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":unsignedByte";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":unsignedByte";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedByte";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedByte";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":unsignedByte";
        }

        if (this.prefix != null) {
            return this.prefix + ":unsignedByte";
        }

        return null;
    }

    public Class getJavaClass() {
        return Byte.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class FloatElement extends NameSpaceElement {
    public FloatElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "float";
    }

    public String getTypeRefName() {
        return "float";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":float";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":float";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":float";
        }

        if (this.prefix != null) {
            return this.prefix + ":float";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":float";
        }

        if (this.prefix != null) {
            return this.prefix + ":float";
        }

        return null;
    }

    public Class getJavaClass() {
        return Float.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class DoubleElement extends NameSpaceElement {
    public DoubleElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "double";
    }

    public String getTypeRefName() {
        return "double";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":double";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":double";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":double";
        }

        if (this.prefix != null) {
            return this.prefix + ":double";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":double";
        }

        if (this.prefix != null) {
            return this.prefix + ":double";
        }

        return null;
    }

    public Class getJavaClass() {
        return Double.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class DateElement extends NameSpaceElement {
    public DateElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "date";
    }

    public String getTypeRefName() {
        return "date";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":date";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":date";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":date";
        }

        if (this.prefix != null) {
            return this.prefix + ":date";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":date";
        }

        if (this.prefix != null) {
            return this.prefix + ":date";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class DateTimeElement extends NameSpaceElement {
    public DateTimeElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "dateTime";
    }

    public String getTypeRefName() {
        return "dateTime";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":dateTime";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":dateTime";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":dateTime";
        }

        if (this.prefix != null) {
            return this.prefix + ":dateTime";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":dateTime";
        }

        if (this.prefix != null) {
            return this.prefix + ":dateTime";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class DurationElement extends NameSpaceElement {
    public DurationElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "duration";
    }

    public String getTypeRefName() {
        return "duration";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":duration";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":duration";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":duration";
        }

        if (this.prefix != null) {
            return this.prefix + ":duration";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":duration";
        }

        if (this.prefix != null) {
            return this.prefix + ":duration";
        }

        return null;
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class GDayElement extends NameSpaceElement {
    public GDayElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "gDay";
    }

    public String getTypeRefName() {
        return "gDay";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":gDay";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":gDay";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gDay";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gDay";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class GMonthElement extends NameSpaceElement {
    public GMonthElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "gMonth";
    }

    public String getTypeRefName() {
        return "gMonth";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":gMonth";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":gMonth";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonth";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonth";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class GMonthDayElement extends NameSpaceElement {
    public GMonthDayElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "gMonthDay";
    }

    public String getTypeRefName() {
        return "gMonthDay";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":gMonthDay";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":gMonthDay";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonthDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonthDay";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gMonthDay";
        }

        if (this.prefix != null) {
            return this.prefix + ":gMonthDay";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class GYearElement extends NameSpaceElement {
    public GYearElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "gYear";
    }

    public String getTypeRefName() {
        return "gYear";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":gYear";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":gYear";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYear";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYear";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYear";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYear";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class GYearMonthElement extends NameSpaceElement {
    public GYearMonthElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "gYearMonth";
    }

    public String getTypeRefName() {
        return "gYearMonth";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":gYearMonth";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":gYearMonth";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYearMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYearMonth";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":gYearMonth";
        }

        if (this.prefix != null) {
            return this.prefix + ":gYearMonth";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class TimeElement extends NameSpaceElement {
    public TimeElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "time";
    }

    public String getTypeRefName() {
        return "time";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":time";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":time";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":time";
        }

        if (this.prefix != null) {
            return this.prefix + ":time";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":time";
        }

        if (this.prefix != null) {
            return this.prefix + ":time";
        }

        return null;
    }

    public Class getJavaClass() {
        return Date.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class IDElement extends NameSpaceElement {
    public IDElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "ID";
    }

    public String getTypeRefName() {
        return "ID";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":ID";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":ID";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ID";
        }

        if (this.prefix != null) {
            return this.prefix + ":ID";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ID";
        }

        if (this.prefix != null) {
            return this.prefix + ":ID";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class IDREFElement extends NameSpaceElement {
    public IDREFElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "IDREF";
    }

    public String getTypeRefName() {
        return "IDREF";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":IDREF";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":IDREF";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREF";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREF";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREF";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREF";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class IDREFSElement extends NameSpaceElement {
    public IDREFSElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "IDREFS";
    }

    public String getTypeRefName() {
        return "IDREFS";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":IDREFS";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":IDREFS";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREFS";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREFS";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":IDREFS";
        }

        if (this.prefix != null) {
            return this.prefix + ":IDREFS";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class ENTITYElement extends NameSpaceElement {
    public ENTITYElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "ENTITY";
    }

    public String getTypeRefName() {
        return "ENTITY";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":ENTITY";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":ENTITY";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITY";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITY";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITY";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITY";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class ENTITIESElement extends NameSpaceElement {
    public ENTITIESElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "ENTITIES";
    }

    public String getTypeRefName() {
        return "ENTITIES";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":ENTITIES";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":ENTITIES";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITIES";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITIES";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":ENTITIES";
        }

        if (this.prefix != null) {
            return this.prefix + ":ENTITIES";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NMTOKENElement extends NameSpaceElement {
    public NMTOKENElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "NMTOKEN";
    }

    public String getTypeRefName() {
        return "NMTOKEN";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":NMTOKEN";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":NMTOKEN";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKEN";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKEN";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKEN";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKEN";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NMTOKENSElement extends NameSpaceElement {
    public NMTOKENSElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "NMTOKENS";
    }

    public String getTypeRefName() {
        return "NMTOKENS";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":NMTOKENS";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":NMTOKENS";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKENS";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKENS";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NMTOKENS";
        }

        if (this.prefix != null) {
            return this.prefix + ":NMTOKENS";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NOTATIONElement extends NameSpaceElement {
    public NOTATIONElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "NOTATION";
    }

    public String getTypeRefName() {
        return "NOTATION";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":NOTATION";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":NOTATION";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NOTATION";
        }

        if (this.prefix != null) {
            return this.prefix + ":NOTATION";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NOTATION";
        }

        if (this.prefix != null) {
            return this.prefix + ":NOTATION";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class StringElement extends NameSpaceElement {
    public StringElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "string";
    }

    public String getTypeRefName() {
        return "string";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":string";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":string";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":string";
        }

        if (this.prefix != null) {
            return this.prefix + ":string";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":string";
        }

        if (this.prefix != null) {
            return this.prefix + ":string";
        }

        return null;
    }

    public Class getJavaClass() {
        return String.class;
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isDefault() {
        return true;
    }
}

class NormalizedStringElement extends NameSpaceElement {
    public NormalizedStringElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "normalizedString";
    }

    public String getTypeRefName() {
        return "normalizedString";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":normalizedString";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":normalizedString";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":normalizedString";
        }

        if (this.prefix != null) {
            return this.prefix + ":normalizedString";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":normalizedString";
        }

        if (this.prefix != null) {
            return this.prefix + ":normalizedString";
        }

        return null;
    }

    public Class getJavaClass() {
        return String.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class TokenElement extends NameSpaceElement {
    public TokenElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "token";
    }

    public String getTypeRefName() {
        return "token";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":token";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":token";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":token";
        }

        if (this.prefix != null) {
            return this.prefix + ":token";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":token";
        }

        if (this.prefix != null) {
            return this.prefix + ":token";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class QNameElement extends NameSpaceElement {
    public QNameElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "QName";
    }

    public String getTypeRefName() {
        return "QName";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":QName";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":QName";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":QName";
        }

        if (this.prefix != null) {
            return this.prefix + ":QName";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":QName";
        }

        if (this.prefix != null) {
            return this.prefix + ":QName";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NameElement extends NameSpaceElement {
    public NameElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "Name";
    }

    public String getTypeRefName() {
        return "Name";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":Name";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":Name";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":Name";
        }

        if (this.prefix != null) {
            return this.prefix + ":Name";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":Name";
        }

        if (this.prefix != null) {
            return this.prefix + ":Name";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}

class NCNameElement extends NameSpaceElement {
    public NCNameElement(String prefix) {
        super(prefix);
    }

    public String getTypeDefName() {
        return "NCName";
    }

    public String getTypeRefName() {
        return "NCName";
    }

    public String getQualifiedTypeDefName() {
        return prefix + ":NCName";
    }

    public String getQualifiedTypeRefName() {
        return prefix + ":NCName";
    }

    public String getQualifiedTypeDefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NCName";
        }

        if (this.prefix != null) {
            return this.prefix + ":NCName";
        }

        return null;
    }

    public String getQualifiedTypeRefName(String prefix) {
        if (prefix != null) {
            return prefix + ":NCName";
        }

        if (this.prefix != null) {
            return this.prefix + ":NCName";
        }

        return null;
    }

    public Class getJavaClass() {
        return Object.class;
    }

    public boolean isAbstract() {
        return false;
    }
}
