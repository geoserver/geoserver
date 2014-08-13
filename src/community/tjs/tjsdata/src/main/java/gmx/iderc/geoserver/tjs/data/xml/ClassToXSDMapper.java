package gmx.iderc.geoserver.tjs.data.xml;

import net.opengis.tjs10.TypeType;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/25/12
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */
public final class ClassToXSDMapper {
    static HashMap<Class, String> map = new HashMap<Class, String>() {
    };

    public ClassToXSDMapper() {
        init();
    }

    public static String map(Class binding) {
        if (map.isEmpty()){
            init();
        }
        if (map.containsKey(binding)) {
            return map.get(binding);
        } else {
            return TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral();
        }
    }

    private static void init() {
        map.put(Integer.class, "http://www.w3.org/TR/xmlschema-2/#short");
        map.put(Long.class, "http://www.w3.org/TR/xmlschema-2/#long");
        //map.put(Integer.valueOf(Types.BINARY), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        //map.put(Integer.valueOf(Types.BLOB), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        map.put(Boolean.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_BOOLEAN_LITERAL.getLiteral());
        map.put(Character.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.CLOB), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.DATALINK), "http://www.w3.org/TR/xmlschema-2/#anyURI");
        map.put(Date.class, "http://www.w3.org/TR/xmlschema-2/#date");

        //map.put(Integer.valueOf(Types.DECIMAL), "http://www.w3.org/TR/xmlschema-2/#decimal");
        map.put(Double.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_DOUBLE_LITERAL.getLiteral());
        map.put(Float.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_FLOAT_LITERAL.getLiteral());
        map.put(Integer.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_INTEGER_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.LONGVARBINARY), "http://www.w3.org/TR/xmlschema-2/#base64Binary");
        map.put(String.class, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.NUMERIC), "http://www.w3.org/TR/xmlschema-2/#decimal");
        //map.put(Integer.valueOf(Types.REAL), "http://www.w3.org/TR/xmlschema-2/#float");
        //map.put(Integer.valueOf(Types.SMALLINT), "http://www.w3.org/TR/xmlschema-2/#short");
        //map.put(Integer.valueOf(Types.TIMESTAMP), "http://www.w3.org/TR/xmlschema-2/#dateTime");
        //map.put(Integer.valueOf(Types.TINYINT), "http://www.w3.org/TR/xmlschema-2/#short");
        //map.put(Integer.valueOf(Types.VARBINARY), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.VARCHAR), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.ARRAY), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.DISTINCT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.JAVA_OBJECT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.NULL), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.OTHER), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.REF), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
        //map.put(Integer.valueOf(Types.STRUCT), TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL.getLiteral());
    }

    public Map<String, Class> getInverseMap(){
        if (map.isEmpty()){
            init();
        }
        HashMap<String, Class> inversemap = new HashMap<String, Class>();
        for(Iterator<Class> iterator = map.keySet().iterator();iterator.hasNext();){
            Class aclass= iterator.next();
            String xsdtype = map.get(aclass);
            inversemap.put(xsdtype, aclass);
        }
        return inversemap;
    }

}
