package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.srsName;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BXMLDecoderUtil;
import org.geoserver.bxml.filter_1_1.AbstractTypeDecoder;
import org.geotools.referencing.CRS;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.util.Assert;

/**
 * The Class AbstractGeometryDecoder is intend to be extended by Geometry decoders. By default, this
 * class parses de crs and dimension attribute of the element to parse.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public abstract class AbstractGeometryDecoder<T> extends AbstractTypeDecoder<T> {

    /** The Constant srsDimension. */
    public static final QName srsDimension = new QName("http://www.opengis.net/gml", "srsDimension");

    /** The crs. */
    private CoordinateReferenceSystem crs;

    /** The dimension. */
    private int dimension = -1;

    /**
     * Instantiates a new abstract geometry decoder.
     * 
     * @param names
     *            the names
     */
    public AbstractGeometryDecoder(final QName... names) {
        super(names);
    }

    /**
     * Instantiates a new abstract geometry decoder.
     * 
     * @param crs
     *            the crs
     * @param dimension
     *            the dimension
     * @param names
     *            the names
     */
    public AbstractGeometryDecoder(final CoordinateReferenceSystem crs, final int dimension,
            final QName... names) {
        this(names);
        this.crs = crs;
        this.dimension = dimension;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the t
     * @throws Exception
     *             the exception
     */
    @Override
    public final T decode(final BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        if (crs == null) {
            String srs = r.getAttributeValue(null, srsName.getLocalPart());
            crs = parseCrs(srs);
        }
        if (dimension == -1) {
            dimension = parseCrsDimension(r.getAttributeValue(null, srsDimension.getLocalPart()));
        }

        T result = decodeInternal(r, name);

        BXMLDecoderUtil.goToEnd(r, name);

        r.require(EventType.END_ELEMENT, name.getNamespaceURI(), name.getLocalPart());
        return result;
    }

    /**
     * Parses the crs.
     * 
     * @param srsName
     *            the srs name
     * @return the coordinate reference system
     * @throws NoSuchAuthorityCodeException
     *             the no such authority code exception
     * @throws FactoryException
     *             the factory exception
     */
    protected CoordinateReferenceSystem parseCrs(final String srsName)
            throws NoSuchAuthorityCodeException, FactoryException {
        if (srsName == null) {
            return null;
        }
        final boolean forceLongitudeFirst = srsName.startsWith("EPSG:");
        CoordinateReferenceSystem crs = CRS.decode(srsName, forceLongitudeFirst);
        return crs;
    }

    /**
     * Parses the crs dimension.
     * 
     * @param srsDimension
     *            the srs dimension
     * @return the int
     */
    protected int parseCrsDimension(String srsDimension) {
        if (srsDimension == null) {
            return 2;
        }
        int dimension = Integer.valueOf(srsDimension);
        return dimension;
    }

    /**
     * Gets the crs.
     * 
     * @return the crs
     */
    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    /**
     * Sets the crs.
     * 
     * @param crs
     *            the new crs
     */
    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /**
     * Gets the dimension.
     * 
     * @return the dimension
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * Sets the dimension.
     * 
     * @param dimension
     *            the new dimension
     */
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
