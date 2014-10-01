package org.geotools.tjs.bindings;


import net.opengis.tjs10.ShortForm;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/tjs/1.0:Identifier.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:element name="Identifier" type="xsd:string"/&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class ShortFormBinding extends AbstractComplexEMFBinding {
    //Tjs10Factory factory;

    public ShortFormBinding(Tjs10Factory factory) {
        super(factory);
        //this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.ShortForm;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return ShortForm.class;
    }

    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        return super.parse(instance, node, value);    //To change body of overridden methods use File | Settings | File Templates.
    }

}
