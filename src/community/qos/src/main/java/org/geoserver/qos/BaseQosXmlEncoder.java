/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.geoserver.qos.util.AttributesBuilder;
import org.geoserver.qos.xml.AreaConstraint;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.OperatingInfo;
import org.geoserver.qos.xml.OperatingInfoTime;
import org.geoserver.qos.xml.OwsAbstract;
import org.geoserver.qos.xml.OwsRange;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.QualityOfServiceStatement;
import org.geoserver.qos.xml.QualityOfServiceStatement.ValueType;
import org.geoserver.qos.xml.ReferenceType;

public abstract class BaseQosXmlEncoder {

    public static final String OPERATING_INFO_TAG = "OperatingInfo";
    public static final String OPERATIONAL_STATUS_TAG = "OperationalStatus";
    public static final String BYDAYSOFWEEK_TAG = "ByDaysOfWeek";
    public static final String ON_TAG = "On";
    public static final String TIME_FORMAT = "HH:mm:ssXXX";
    public static final String QOS_STATEMENT_TAG = "QualityOfServiceStatement";
    public static final String operationAnomalyFeed_TAG = "OperationAnomalyFeed";
    public static final String representativeOperation_TAG = "RepresentativeOperation";
    public static final String OWS_VALUE_TAG = "Value";
    public static final String OWS_RANGE_TAG = "Range";
    public static final String OWS_MIN_VALUE = "MinimumValue";
    public static final String OWS_MAX_VALUE = "MaximumValue";

    protected String servicePrefix;

    public BaseQosXmlEncoder(String servicePrefix) {
        super();
        this.servicePrefix = servicePrefix;
    }

    public void encodeQualityOfServiceStatement(
            String tag, Translator tx, QualityOfServiceStatement st) {
        tx.start(tag);
        // <qos:Metric
        final String metricTag = QosSchema.QOS_PREFIX + ":Metric";
        AttributesBuilder ab1 = new AttributesBuilder();
        ab1.add("xlink:href", st.getMetric().getHref());
        ab1.add("xlink:title", st.getMetric().getTitle());
        tx.start(metricTag, ab1.getAttributes());
        tx.end(metricTag);
        // value
        String valueTag = null;
        // <qos:MoreThanOrEqual ...
        if (ValueType.moreThanOrEqual.equals(st.getValueType())) {
            valueTag = QosSchema.QOS_PREFIX + ":MoreThanOrEqual";
        } else if (ValueType.lessThanOrEqual.equals(st.getValueType())) {
            valueTag = QosSchema.QOS_PREFIX + ":LessThanOrEqual";
        } else {
            valueTag = QosSchema.QOS_PREFIX + ":Value";
        }
        // value tag
        if (ValueType.moreThanOrEqual.equals(st.getValueType())
                || ValueType.lessThanOrEqual.equals(st.getValueType())) {
            AttributesBuilder ab = new AttributesBuilder();
            ab.add("uom", st.getMeassure().getUom());
            tx.start(valueTag, ab.getAttributes());
        } else {
            tx.start(valueTag);
        }
        tx.chars(st.getMeassure().getValue());
        tx.end(valueTag);
        // end main tag
        tx.end(tag);
    }

    public void encodeOperatingInfo(String tag, Translator tx, OperatingInfo oinfo) {
        final String ostatusTag = QosSchema.QOS_PREFIX + ":" + OPERATIONAL_STATUS_TAG;
        tx.start(tag);
        // encode operational status
        encodeReferenceType(ostatusTag, tx, oinfo.getOperationalStatus());
        // encode ByDaysOfWeeks tags <qos:ByDaysOfWeek>
        final String bydaysTag = QosSchema.QOS_PREFIX + ":" + BYDAYSOFWEEK_TAG;
        oinfo.getByDaysOfWeek().forEach(o -> encodeOperatingInfoTime(bydaysTag, tx, o));
        // </tag
        tx.end(tag);
    }

    public void encodeReferenceType(String tag, Translator tx, ReferenceType refType) {
        // atrributes:
        AttributesBuilder ab = new AttributesBuilder();
        ab.add("xlink:href", refType.getHref()).add("xlink:title", refType.getTitle());
        // start tag
        tx.start(tag, ab.getAttributes());
        // ows:Abstract
        if (refType.getAbstracts() != null) {
            refType.getAbstracts()
                    .forEach(
                            a -> {
                                encodeOwsAbstract(owsPrefix() + ":Abstract", tx, a);
                            });
        }
        // ows:Format
        if (StringUtils.isNotEmpty(refType.getFormat())) {
            tx.start(owsPrefix() + ":Format");
            tx.chars(refType.getFormat());
            tx.end(owsPrefix() + ":Format");
        }
        tx.end(tag);
    }

    public void encodeOwsAbstract(String tag, Translator tx, OwsAbstract abs) {
        if (StringUtils.isEmpty(abs.getValue())) return;
        if (!StringUtils.isEmpty(abs.getLang())) {
            AttributesBuilder ab = new AttributesBuilder();
            ab.add("lang", abs.getLang());
        } else {
            tx.start(tag);
        }
        tx.chars(abs.getValue());
        tx.end(tag);
    }

    public void encodeOperatingInfoTime(String tag, Translator tx, OperatingInfoTime otime) {
        tx.start(tag);
        // <qos:On>
        if (otime.getDays() != null && !otime.getDays().isEmpty()) {
            final String onTag = QosSchema.QOS_PREFIX + ":" + ON_TAG;
            tx.start(onTag);
            // days concatenation, with spaces
            StringBuilder ob = new StringBuilder();
            otime.getDays()
                    .forEach(
                            d -> {
                                ob.append(d.value());
                                ob.append(" ");
                            });
            tx.chars(ob.toString().trim());
            tx.end(onTag);
        }
        // <qos:StartTime>10:00:00+03:00</qos:StartTime>
        if (otime.getStartTime() != null) {
            final String startTimeTag = QosSchema.QOS_PREFIX + ":StartTime";
            tx.start(startTimeTag);
            tx.chars(
                    otime.getStartTime()
                            .format(DateTimeFormatter.ofPattern(OperatingInfoTime.TIME_PATTERN)));
            tx.end(startTimeTag);
        }

        // <qos:EndTime>14:59:59+03:00</qos:EndTime>
        if (otime.getEndTime() != null) {
            final String endTimeTag = QosSchema.QOS_PREFIX + ":EndTime";
            tx.start(endTimeTag);
            tx.chars(
                    otime.getEndTime()
                            .format(DateTimeFormatter.ofPattern(OperatingInfoTime.TIME_PATTERN)));
            tx.end(endTimeTag);
        }

        // end main tag
        tx.end(tag);
    }

    public abstract void encodeRepresentativeOperation(
            String tag, Translator tx, QosRepresentativeOperation repOp);

    public void encodeLimitedAreaRequestConstraint(
            String tag, Translator tx, LimitedAreaRequestConstraints arc) {
        tx.start(tag);
        // <qos:AreaConstraint srsName="EPSG:3067">
        if (arc.getAreaConstraint() != null) {
            encodeAreaConstraint("qos:AreaConstraint", tx, arc.getAreaConstraint(), arc.getCrs());
        }
        // <qos:RequestParameterConstraint name="LayerName">
        if (CollectionUtils.isNotEmpty(arc.getLayerNames())) {
            encodeRequestParameterConstraint(
                    tx,
                    "LayerName",
                    (Void) -> {
                        arc.getLayerNames().forEach(x -> encodeOwsValue(tx, x));
                    });
        }
        // <qos:RequestParameterConstraint name="CRS">
        if (StringUtils.isNotEmpty(arc.getCrs())) {
            encodeRequestParameterConstraint(
                    tx,
                    "CRS",
                    (Void) -> {
                        encodeOwsValue(tx, arc.getCrs());
                    });
        }
        // <qos:RequestParameterConstraint name="OutputFormat">
        if (CollectionUtils.isNotEmpty(arc.getOutputFormat())) {
            encodeRequestParameterConstraint(
                    tx,
                    "OutputFormat",
                    (Void) -> {
                        arc.getOutputFormat().forEach(x -> encodeOwsValue(tx, x));
                    });
        }
        // <qos:RequestParameterConstraint name="ImageWidth">
        OwsRange imgWidth = arc.getImageWidth();
        if (imgWidth != null
                && (StringUtils.isNotEmpty(imgWidth.getMinimunValue())
                        || StringUtils.isNotEmpty(imgWidth.getMaximunValue()))) {
            encodeRequestParameterConstraint(
                    tx,
                    "ImageWidth",
                    (Void) -> {
                        encodeOwsRange(tx, imgWidth.getMinimunValue(), imgWidth.getMaximunValue());
                    });
        }
        // <qos:RequestParameterConstraint name="ImageHeight">
        OwsRange imgHeight = arc.getImageHeight();
        if (imgHeight != null
                && (StringUtils.isNotEmpty(imgHeight.getMinimunValue())
                        || StringUtils.isNotEmpty(imgHeight.getMaximunValue()))) {
            encodeRequestParameterConstraint(
                    tx,
                    "ImageHeight",
                    (Void) -> {
                        encodeOwsRange(
                                tx, imgHeight.getMinimunValue(), imgHeight.getMaximunValue());
                    });
        }
        tx.end(tag);
    }

    /**
     * <ows:Range> <ows:MaximumValue>256</ows:MaximumValue> <ows:MinimumValue>256</ows:MinimumValue>
     */
    public void encodeOwsRange(Translator tx, String min, String max) {
        tx.start(owsPrefix() + ":" + OWS_RANGE_TAG);
        if (StringUtils.isNotEmpty(min)) {
            tx.start(owsPrefix() + ":" + OWS_MIN_VALUE);
            tx.chars(min);
            tx.end(owsPrefix() + ":" + OWS_MIN_VALUE);
        }
        if (StringUtils.isNotEmpty(max)) {
            tx.start(owsPrefix() + ":" + OWS_MAX_VALUE);
            tx.chars(max);
            tx.end(owsPrefix() + ":" + OWS_MAX_VALUE);
        }
        tx.end(owsPrefix() + ":" + OWS_RANGE_TAG);
    }

    /** <ows:Value>Vesienhoitoalueet</ows:Value> */
    public void encodeOwsValue(Translator tx, String value) {
        tx.start(owsPrefix() + ":" + OWS_VALUE_TAG);
        tx.chars(value);
        tx.end(owsPrefix() + ":" + OWS_VALUE_TAG);
    }

    public void encodeRequestParameterConstraint(
            Translator tx, String name, Consumer<Void> valuesCallback) {
        final String reqParamConstTag = "qos:RequestParameterConstraint";
        AttributesBuilder ab = new AttributesBuilder();
        ab.add("name", name);
        tx.start(reqParamConstTag, ab.getAttributes());
        final String allowedValuesTag = owsPrefix() + ":AllowedValues";
        tx.start(allowedValuesTag);
        valuesCallback.accept(null);
        tx.end(allowedValuesTag);
        tx.end(reqParamConstTag);
    }

    public void encodeAreaConstraint(String tag, Translator tx, AreaConstraint ac, String crs) {
        // <qos:AreaConstraint srsName="EPSG:3067">
        AttributesBuilder ab = new AttributesBuilder();
        ab.add("srsName", crs);
        tx.start(tag, ab.getAttributes());
        // <qos:LowerCorner>64934.103000 6626229.791000</qos:LowerCorner>
        String lowerTag = "qos:LowerCorner";
        tx.start(lowerTag);
        tx.chars(ac.getMinX().toString() + " " + ac.getMinY().toString());
        tx.end(lowerTag);
        // <qos:UpperCorner>732333.567000 7776461.100000</qos:UpperCorner>
        String upperTag = "qos:UpperCorner";
        tx.start(upperTag);
        tx.chars(ac.getMaxX() + " " + ac.getMaxY());
        tx.end(upperTag);
        // end
        tx.end(tag);
    }

    public void encodeHttpMethod(Translator tx, String httpMethod) {
        final String dcpTag = owsPrefix() + ":DCP";
        final String httpTag = owsPrefix() + ":HTTP";
        tx.start(dcpTag);
        tx.start(httpTag);
        // select method tag
        String methodTag = owsPrefix() + ":Get";
        if (httpMethod.toLowerCase().equals("post")) methodTag = owsPrefix() + ":Post";
        if (httpMethod.toLowerCase().equals("put")) methodTag = owsPrefix() + ":Put";
        if (httpMethod.toLowerCase().equals("delete")) methodTag = owsPrefix() + ":Delete";
        tx.start(methodTag);
        // end all tags
        tx.end(methodTag);
        tx.end(httpTag);
        tx.end(dcpTag);
    }

    public String owsPrefix() {
        return "ows";
    }
}
