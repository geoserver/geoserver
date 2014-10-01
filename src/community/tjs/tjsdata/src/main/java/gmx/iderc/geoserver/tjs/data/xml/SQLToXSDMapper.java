package gmx.iderc.geoserver.tjs.data.xml;


import net.opengis.tjs10.TypeType;

import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 9/11/12
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SQLToXSDMapper {
    HashMap<Integer, String> map = new HashMap<Integer, String>();

    public SQLToXSDMapper() {
        map.put(Integer.valueOf(Types.BIT), "http://www.w3.org/TR/xmlschema-2/#short");
        map.put(Integer.valueOf(Types.BIGINT), "http://www.w3.org/TR/xmlschema-2/#long");
        map.put(Integer.valueOf(Types.BINARY), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        map.put(Integer.valueOf(Types.BLOB), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        map.put(Integer.valueOf(Types.BOOLEAN), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_BOOLEAN_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.CHAR), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.CLOB), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.DATALINK), "http://www.w3.org/TR/xmlschema-2/#anyURI");
        map.put(Integer.valueOf(Types.DATE), "http://www.w3.org/TR/xmlschema-2/#date");

        map.put(Integer.valueOf(Types.DECIMAL), "http://www.w3.org/TR/xmlschema-2/#decimal");
        map.put(Integer.valueOf(Types.DOUBLE), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_DOUBLE_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.FLOAT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_FLOAT_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.INTEGER), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_INTEGER_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.LONGVARBINARY), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        map.put(Integer.valueOf(Types.LONGVARCHAR), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.NUMERIC), "http://www.w3.org/TR/xmlschema-2/#decimal");
        map.put(Integer.valueOf(Types.REAL), "http://www.w3.org/TR/xmlschema-2/#float");
        map.put(Integer.valueOf(Types.SMALLINT), "http://www.w3.org/TR/xmlschema-2/#short");
        map.put(Integer.valueOf(Types.TIMESTAMP), "http://www.w3.org/TR/xmlschema-2/#dateTime");
        map.put(Integer.valueOf(Types.TINYINT), "http://www.w3.org/TR/xmlschema-2/#short");
        map.put(Integer.valueOf(Types.VARBINARY), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.VARCHAR), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.ARRAY), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.DISTINCT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.JAVA_OBJECT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.NULL), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.OTHER), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.REF), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        map.put(Integer.valueOf(Types.STRUCT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
    }

    public String map(int type) {
        if (map.containsKey(Integer.valueOf(type))) {
            return map.get(Integer.valueOf(type));
        } else {
            return TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral();
        }
    }

    public Map<String, Integer> getInverseMap(){
        HashMap<String, Integer> inversemap = new HashMap<String, Integer>();
        for(Iterator<Integer> iterator = map.keySet().iterator();iterator.hasNext();){
            Integer sqltype= iterator.next();
            String xsdtype = map.get(sqltype);
            inversemap.put(xsdtype, sqltype);
        }
        return inversemap;
    }

}
