/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.io.StringReader;
import java.util.Iterator;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.RangeSubsetType;
import net.opengis.wcs11.Wcs111Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs.kvp.rangesubset.ASTAxisId;
import org.geoserver.wcs.kvp.rangesubset.ASTAxisSubset;
import org.geoserver.wcs.kvp.rangesubset.ASTFieldId;
import org.geoserver.wcs.kvp.rangesubset.ASTFieldSubset;
import org.geoserver.wcs.kvp.rangesubset.ASTInterpolation;
import org.geoserver.wcs.kvp.rangesubset.ASTKey;
import org.geoserver.wcs.kvp.rangesubset.ASTRangeSubset;
import org.geoserver.wcs.kvp.rangesubset.Node;
import org.geoserver.wcs.kvp.rangesubset.RangeSubsetParser;
import org.geoserver.wcs.kvp.rangesubset.RangeSubsetParserVisitor;
import org.geoserver.wcs.kvp.rangesubset.SimpleNode;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parses the RangeSubset parameter of a GetFeature KVP request
 *
 * @author Andrea Aime
 */
public class RangeSubsetKvpParser extends KvpParser {

    public RangeSubsetKvpParser() {
        super("RangeSubset", RangeSubsetType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        RangeSubsetParser parser = new RangeSubsetParser(new StringReader(value));
        SimpleNode root = parser.RangeSubset();
        RangeSubsetType result =
                (RangeSubsetType) root.jjtAccept(new RangeSubsetKvpParserVisitor(), null);

        for (Iterator it = result.getFieldSubset().iterator(); it.hasNext(); ) {
            FieldSubsetType type = (FieldSubsetType) it.next();
            String interpolationType = type.getInterpolationType();
            if (interpolationType != null) {
                try {
                    InterpolationMethod.valueOf(interpolationType);
                } catch (IllegalArgumentException e) {
                    throw new WcsException(
                            "Unknown interpolation method " + interpolationType,
                            InvalidParameterValue,
                            "RangeSubset");
                }
            }
        }

        return result;
    }

    private static class RangeSubsetKvpParserVisitor implements RangeSubsetParserVisitor {
        Wcs111Factory wcsf = Wcs111Factory.eINSTANCE;
        Ows11Factory owsf = Ows11Factory.eINSTANCE;

        public Object visit(SimpleNode node, Object data) {
            throw new UnsupportedOperationException("This method should never be reached");
        }

        public Object visit(ASTRangeSubset node, Object data) {
            RangeSubsetType rs = wcsf.createRangeSubsetType();
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                ASTFieldSubset fs = (ASTFieldSubset) node.jjtGetChild(i);
                FieldSubsetType fst = (FieldSubsetType) fs.jjtAccept(this, data);
                rs.getFieldSubset().add(fst);
            }
            return rs;
        }

        public Object visit(ASTFieldSubset node, Object data) {
            FieldSubsetType fs = wcsf.createFieldSubsetType();

            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                Node child = node.jjtGetChild(i);
                if (child instanceof ASTFieldId) {
                    CodeType id = owsf.createCodeType();
                    id.setValue((String) child.jjtAccept(this, null));
                    fs.setIdentifier(id);
                } else if (child instanceof ASTInterpolation) {
                    fs.setInterpolationType((String) child.jjtAccept(this, null));
                } else if (child instanceof ASTAxisSubset) {
                    fs.getAxisSubset().add(child.jjtAccept(this, null));
                }
            }
            return fs;
        }

        public Object visit(ASTAxisSubset node, Object data) {
            AxisSubsetType as = wcsf.createAxisSubsetType();
            as.setIdentifier(((SimpleNode) node.jjtGetChild(0)).getContent());
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                as.getKey().add(node.jjtGetChild(i).jjtAccept(this, null));
            }
            return as;
        }

        public Object visit(ASTFieldId node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTAxisId node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTInterpolation node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTKey node, Object data) {
            return node.getContent();
        }
    }
}
