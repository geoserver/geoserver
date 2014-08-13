package gmx.iderc.geoserver.tjs.data;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;

import javax.sql.RowSet;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 11/1/12
 * Time: 8:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class RowSetPropertyAccessorFactory implements PropertyAccessorFactory {

    static PropertyAccessor ROWSET = new RowSetPropertyAccessor();

    public PropertyAccessor createPropertyAccessor(Class type, String xpath,
                                                   Class target, Hints hints) {
        return ROWSET;
    }

    static class RowSetPropertyAccessor implements PropertyAccessor {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

        public boolean canHandle(Object object, String xpath, Class target) {
            if (object instanceof RowSet) {
                RowSet rst = (RowSet) object;
                try {
                    return (rst.findColumn(xpath) > 0);
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        }

        public Object get(Object object, String xpath, Class target) throws java.lang.IllegalArgumentException {
            RowSet rst = (RowSet) object;
            try {
                int index = rst.findColumn(xpath);
                Literal l = ff.literal(rst.getObject(index));
                return l.evaluate("");
            } catch (Exception ex) {
                return null;
            }
        }

        public void set(Object object, String xpath, Object value, Class target) throws IllegalArgumentException {
            RowSet rst = (RowSet) object;
            try {
                int index = rst.findColumn(xpath);
                rst.setObject(index, value);
            } catch (Exception ex) {
            }
        }
    }
}
