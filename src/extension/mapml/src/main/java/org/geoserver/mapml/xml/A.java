package org.geoserver.mapml.xml;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "A",
        propOrder = {"geometryContent"})
public class A {
    @XmlAttribute(name = "href")
    protected String href;

    @XmlAttribute(name = "type")
    protected String type;

    @XmlAttribute(name = "target")
    protected String target;

    @XmlAttribute(name = "inplace")
    protected String inplace;

    @XmlAttribute(name = "title")
    protected String title;

    @XmlAttribute(name = "aria-label")
    protected String ariaLabel;

    @XmlElementRef(name = "GeometryContent", type = JAXBElement.class, namespace = "http://www.w3.org/1999/xhtml")
    protected JAXBElement<?> geometryContent;

    public String getHref() {
        return href;
    }

    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Gets the value of the geometryContent property.
     *
     * @return possible object is {@link JAXBElement }{@code <}{@link MultiPolygon }{@code >} {@link JAXBElement
     *     }{@code <}{@link LineString }{@code >} {@link JAXBElement }{@code <}{@link GeometryCollection }{@code >}
     *     {@link JAXBElement }{@code <}{@link MultiPoint }{@code >} {@link JAXBElement }{@code <}{@link Object
     *     }{@code >} {@link JAXBElement }{@code <}{@link Point }{@code >} {@link JAXBElement
     *     }{@code <}{@link MultiLineString }{@code >} {@link JAXBElement }{@code <}{@link Polygon }{@code >}
     */
    public JAXBElement<?> getGeometryContent() {
        return geometryContent;
    }

    /**
     * Sets the value of the geometryContent property.
     *
     * @param value allowed object is {@link JAXBElement }{@code <}{@link MultiPolygon }{@code >} {@link JAXBElement
     *     }{@code <}{@link LineString }{@code >} {@link JAXBElement }{@code <}{@link GeometryCollection }{@code >}
     *     {@link JAXBElement }{@code <}{@link MultiPoint }{@code >} {@link JAXBElement }{@code <}{@link Object
     *     }{@code >} {@link JAXBElement }{@code <}{@link Point }{@code >} {@link JAXBElement
     *     }{@code <}{@link MultiLineString }{@code >} {@link JAXBElement }{@code <}{@link Polygon }{@code >}
     */
    public void setGeometryContent(JAXBElement<?> value) {
        this.geometryContent = value;
    }
}
