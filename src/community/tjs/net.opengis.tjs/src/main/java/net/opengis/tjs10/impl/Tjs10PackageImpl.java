/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.ows11.Ows11Package;
import net.opengis.tjs10.*;
import net.opengis.tjs10.util.Tjs10Validator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;
import org.w3.xlink.XlinkPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class Tjs10PackageImpl extends EPackageImpl implements Tjs10Package {
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass abstractTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass acceptVersionsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass attributeDataTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass attributesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass boundingCoordinatesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass classesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass classesType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass columnsetTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass columnTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass columnType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass columnType2EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass countTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass dataDescriptionsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass dataInputsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass datasetDescriptionsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass datasetTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass datasetType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass datasetType2EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass datasetType3EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeDataRequestTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeDatasetsRequestTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeDatasetsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeDataTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeFrameworkKeyTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeFrameworksTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass describeKeyTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass documentRootEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass exceptionReportTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass failedTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkDatasetDescribeDataTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkDescriptionsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkKeyDescriptionTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkKeyTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkKeyType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkType2EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkType3EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass frameworkType4EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass gdasTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass getCapabilitiesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass getDataRequestTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass getDataTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass getDataXMLTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass joinAbilitiesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass joinDataResponseTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass joinDataTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass joinedOutputsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass kTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass languagesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass mapStylingTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass measureCountExceptionsEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass measureTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass mechanismTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass nominalOrdinalExceptionsEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass nominalTypeEClass = null;


    private EClass shortFormEClass = null;
    private EClass longFormEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass nullTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass nullType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass ordinalTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass outputMechanismsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass outputStylingsTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass outputStylingsType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass outputTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass parameterTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass referenceDateTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass requestBaseTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass resourceTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass rowsetTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass rowsetType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass rowTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass rowType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass spatialFrameworksTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass statusTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass stylingTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass tjsCapabilitiesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass uncertaintyTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass uomTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass valuesTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass valueTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass valueType1EClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass vTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EClass wsdlTypeEClass = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum dataClassTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum describeDatasetsValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum describeDataValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum describeFrameworksValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum describeJoinAbilitiesValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum describeKeyValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum gaussianTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum getCapabilitiesValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum getDataValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum joinDataValueTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum purposeTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum requestServiceTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum typeTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum updateTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum versionTypeEEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum versionType1EEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EEnum versionType2EEnum = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType acceptLanguagesTypeEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType dataClassTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType describeDatasetsValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType describeDataValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType describeFrameworksValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType describeJoinAbilitiesValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType describeKeyValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType gaussianTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType getCapabilitiesValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType getDataValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType joinDataValueTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType purposeTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType requestServiceTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType sectionsTypeEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType typeTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType updateTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType versionTypeObjectEDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType versionTypeObject1EDataType = null;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private EDataType versionTypeObject2EDataType = null;

    /**
     * Creates an instance of the model <b>Package</b>, registered with
     * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
     * package URI value.
     * <p>Note: the correct way to create the package is via the static
     * factory method {@link #init init()}, which also performs
     * initialization of the package, or returns the registered package,
     * if one already exists.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see org.eclipse.emf.ecore.EPackage.Registry
     * @see net.opengis.tjs10.Tjs10Package#eNS_URI
     * @see #init()
     */
    private Tjs10PackageImpl() {
        super(eNS_URI, Tjs10Factory.eINSTANCE);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static boolean isInited = false;

    /**
     * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
     * <p/>
     * <p>This method is used to initialize {@link Tjs10Package#eINSTANCE} when that field is accessed.
     * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #eNS_URI
     * @see #createPackageContents()
     * @see #initializePackageContents()
     */
    public static Tjs10Package init() {
        if (isInited) return (Tjs10Package) EPackage.Registry.INSTANCE.getEPackage(Tjs10Package.eNS_URI);

        // Obtain or create and register package
        Tjs10PackageImpl theTjs10Package = (Tjs10PackageImpl) (EPackage.Registry.INSTANCE.get(eNS_URI) instanceof Tjs10PackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new Tjs10PackageImpl());

        isInited = true;

        // Initialize simple dependencies
        Ows11Package.eINSTANCE.eClass();
        XlinkPackage.eINSTANCE.eClass();
        XMLTypePackage.eINSTANCE.eClass();

        // Create package meta-data objects
        theTjs10Package.createPackageContents();

        // Initialize created meta-data
        theTjs10Package.initializePackageContents();

        // Register package validator
        EValidator.Registry.INSTANCE.put
                                             (theTjs10Package,
                                                     new EValidator.Descriptor() {
                                                         public EValidator getEValidator() {
                                                             return Tjs10Validator.INSTANCE;
                                                         }
                                                     });

        // Mark meta-data to indicate it can't be changed
        theTjs10Package.freeze();


        // Update the registry and return the package
        EPackage.Registry.INSTANCE.put(Tjs10Package.eNS_URI, theTjs10Package);
        return theTjs10Package;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getAbstractType() {
        return abstractTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getAcceptVersionsType() {
        return acceptVersionsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getAcceptVersionsType_Version() {
        return (EAttribute) acceptVersionsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getAttributeDataType() {
        return attributeDataTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getAttributeDataType_GetDataURL() {
        return (EAttribute) attributeDataTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getAttributeDataType_GetDataXML() {
        return (EReference) attributeDataTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getAttributesType() {
        return attributesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getAttributesType_Column() {
        return (EReference) attributesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getBoundingCoordinatesType() {
        return boundingCoordinatesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getBoundingCoordinatesType_North() {
        return (EAttribute) boundingCoordinatesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getBoundingCoordinatesType_South() {
        return (EAttribute) boundingCoordinatesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getBoundingCoordinatesType_East() {
        return (EAttribute) boundingCoordinatesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getBoundingCoordinatesType_West() {
        return (EAttribute) boundingCoordinatesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getClassesType() {
        return classesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getClassesType_Title() {
        return (EAttribute) classesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getClassesType_Abstract() {
        return (EReference) classesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getClassesType_Documentation() {
        return (EAttribute) classesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getClassesType_Value() {
        return (EReference) classesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getClassesType1() {
        return classesType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getClassesType1_Title() {
        return (EAttribute) classesType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getClassesType1_Abstract() {
        return (EReference) classesType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getClassesType1_Documentation() {
        return (EAttribute) classesType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getClassesType1_Value() {
        return (EReference) classesType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getColumnsetType() {
        return columnsetTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getColumnsetType_FrameworkKey() {
        return (EReference) columnsetTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getColumnsetType_Attributes() {
        return (EReference) columnsetTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getColumnType() {
        return columnTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType_Decimals() {
        return (EAttribute) columnTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType_Length() {
        return (EAttribute) columnTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType_Name() {
        return (EAttribute) columnTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType_Type() {
        return (EAttribute) columnTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getColumnType1() {
        return columnType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Title() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getColumnType1_Abstract() {
        return (EReference) columnType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Documentation() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getColumnType1_Values() {
        return (EReference) columnType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getColumnType1_GetDataRequest() {
        return (EReference) columnType1EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Decimals() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Length() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Name() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Purpose() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType1_Type() {
        return (EAttribute) columnType1EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getColumnType2() {
        return columnType2EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType2_Decimals() {
        return (EAttribute) columnType2EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType2_Length() {
        return (EAttribute) columnType2EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType2_Name() {
        return (EAttribute) columnType2EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getColumnType2_Type() {
        return (EAttribute) columnType2EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getCountType() {
        return countTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getCountType_UOM() {
        return (EReference) countTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getCountType_Uncertainty() {
        return (EReference) countTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getCountType_Exceptions() {
        return (EReference) countTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDataDescriptionsType() {
        return dataDescriptionsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDataDescriptionsType_Framework() {
        return (EReference) dataDescriptionsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDataDescriptionsType_Capabilities() {
        return (EAttribute) dataDescriptionsTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDataDescriptionsType_Lang() {
        return (EAttribute) dataDescriptionsTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDataDescriptionsType_Service() {
        return (EAttribute) dataDescriptionsTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDataDescriptionsType_Version() {
        return (EAttribute) dataDescriptionsTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDataInputsType() {
        return dataInputsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDataInputsType_Framework() {
        return (EReference) dataInputsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDatasetDescriptionsType() {
        return datasetDescriptionsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetDescriptionsType_Framework() {
        return (EReference) datasetDescriptionsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetDescriptionsType_Capabilities() {
        return (EAttribute) datasetDescriptionsTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetDescriptionsType_Lang() {
        return (EAttribute) datasetDescriptionsTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetDescriptionsType_Service() {
        return (EAttribute) datasetDescriptionsTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetDescriptionsType_Version() {
        return (EAttribute) datasetDescriptionsTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDatasetType() {
        return datasetTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType_DatasetURI() {
        return (EAttribute) datasetTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType_Organization() {
        return (EAttribute) datasetTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType_Title() {
        return (EAttribute) datasetTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType_Abstract() {
        return (EReference) datasetTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType_ReferenceDate() {
        return (EReference) datasetTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType_Version() {
        return (EAttribute) datasetTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType_Documentation() {
        return (EAttribute) datasetTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType_DescribeDataRequest() {
        return (EReference) datasetTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDatasetType1() {
        return datasetType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType1_DatasetURI() {
        return (EAttribute) datasetType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType1_Organization() {
        return (EAttribute) datasetType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType1_Title() {
        return (EAttribute) datasetType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType1_Abstract() {
        return (EReference) datasetType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType1_ReferenceDate() {
        return (EReference) datasetType1EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType1_Version() {
        return (EAttribute) datasetType1EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType1_Documentation() {
        return (EAttribute) datasetType1EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType1_Columnset() {
        return (EReference) datasetType1EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType1_Rowset() {
        return (EReference) datasetType1EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDatasetType2() {
        return datasetType2EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType2_DatasetURI() {
        return (EAttribute) datasetType2EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType2_Organization() {
        return (EAttribute) datasetType2EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType2_Title() {
        return (EAttribute) datasetType2EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType2_Abstract() {
        return (EReference) datasetType2EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType2_ReferenceDate() {
        return (EReference) datasetType2EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType2_Version() {
        return (EAttribute) datasetType2EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType2_Documentation() {
        return (EAttribute) datasetType2EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType2_DescribeDataRequest() {
        return (EReference) datasetType2EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType2_Columnset() {
        return (EReference) datasetType2EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType2_Rowset() {
        return (EReference) datasetType2EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDatasetType3() {
        return datasetType3EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType3_DatasetURI() {
        return (EAttribute) datasetType3EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType3_Organization() {
        return (EAttribute) datasetType3EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType3_Title() {
        return (EAttribute) datasetType3EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType3_Abstract() {
        return (EReference) datasetType3EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType3_ReferenceDate() {
        return (EReference) datasetType3EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType3_Version() {
        return (EAttribute) datasetType3EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDatasetType3_Documentation() {
        return (EAttribute) datasetType3EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType3_DescribeDataRequest() {
        return (EReference) datasetType3EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDatasetType3_Columnset() {
        return (EReference) datasetType3EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeDataRequestType() {
        return describeDataRequestTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDataRequestType_Href() {
        return (EAttribute) describeDataRequestTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeDatasetsRequestType() {
        return describeDatasetsRequestTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDatasetsRequestType_Href() {
        return (EAttribute) describeDatasetsRequestTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeDatasetsType() {
        return describeDatasetsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDatasetsType_FrameworkURI() {
        return (EAttribute) describeDatasetsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDatasetsType_DatasetURI() {
        return (EAttribute) describeDatasetsTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeDataType() {
        return describeDataTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDataType_FrameworkURI() {
        return (EAttribute) describeDataTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDataType_DatasetURI() {
        return (EAttribute) describeDataTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeDataType_Attributes() {
        return (EAttribute) describeDataTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeFrameworkKeyType() {
        return describeFrameworkKeyTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworkKeyType_FrameworkURI() {
        return (EAttribute) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworkKeyType_Organization() {
        return (EAttribute) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworkKeyType_Title() {
        return (EAttribute) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDescribeFrameworkKeyType_Abstract() {
        return (EReference) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDescribeFrameworkKeyType_ReferenceDate() {
        return (EReference) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworkKeyType_Version() {
        return (EAttribute) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworkKeyType_Documentation() {
        return (EAttribute) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDescribeFrameworkKeyType_FrameworkKey() {
        return (EReference) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDescribeFrameworkKeyType_BoundingCoordinates() {
        return (EReference) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDescribeFrameworkKeyType_Rowset() {
        return (EReference) describeFrameworkKeyTypeEClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeFrameworksType() {
        return describeFrameworksTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeFrameworksType_FrameworkURI() {
        return (EAttribute) describeFrameworksTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDescribeKeyType() {
        return describeKeyTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDescribeKeyType_FrameworkURI() {
        return (EAttribute) describeKeyTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getDocumentRoot() {
        return documentRootEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Mixed() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_XMLNSPrefixMap() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_XSISchemaLocation() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Abstract() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_AttributeLimit() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Attributes() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_BoundingCoordinates() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Capabilities() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Columnset() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Count() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_DataClass() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(10);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DataDescriptions() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(11);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Dataset() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(12);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DatasetDescriptions() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(13);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_DatasetURI() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(14);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeData() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(15);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeDataRequest() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(16);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeDatasets() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(17);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeDatasetsRequest() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(18);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeFrameworks() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(19);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeJoinAbilities() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(20);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_DescribeKey() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(21);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Documentation() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(22);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Framework() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(23);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_FrameworkDescriptions() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(24);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_FrameworkKey() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(25);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_FrameworkKeyDescription() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(26);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_FrameworkURI() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(27);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_GDAS() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(28);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_GetCapabilities() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(29);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_GetData() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(30);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_GetDataRequest() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(31);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Identifier() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(32);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_JoinAbilities() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(33);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_JoinData() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(34);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_JoinDataResponse() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(35);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_K() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(36);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_LinkageKeys() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(37);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Measure() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(38);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Mechanism() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(39);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Nominal() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(40);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Ordinal() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(41);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Organization() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(42);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_ReferenceDate() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(43);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Rowset() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(44);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_SpatialFrameworks() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(45);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Styling() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(46);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Title() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(47);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Uncertainty() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(48);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_UOM() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(49);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getDocumentRoot_Values() {
        return (EReference) documentRootEClass.getEStructuralFeatures().get(50);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getDocumentRoot_Version() {
        return (EAttribute) documentRootEClass.getEStructuralFeatures().get(51);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getExceptionReportType() {
        return exceptionReportTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getExceptionReportType_Exception() {
        return (EReference) exceptionReportTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFailedType() {
        return failedTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkDatasetDescribeDataType() {
        return frameworkDatasetDescribeDataTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDatasetDescribeDataType_FrameworkURI() {
        return (EAttribute) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDatasetDescribeDataType_Organization() {
        return (EAttribute) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDatasetDescribeDataType_Title() {
        return (EAttribute) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_Abstract() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_ReferenceDate() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDatasetDescribeDataType_Version() {
        return (EAttribute) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDatasetDescribeDataType_Documentation() {
        return (EAttribute) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_FrameworkKey() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_BoundingCoordinates() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_DescribeDatasetsRequest() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDatasetDescribeDataType_Dataset() {
        return (EReference) frameworkDatasetDescribeDataTypeEClass.getEStructuralFeatures().get(10);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkDescriptionsType() {
        return frameworkDescriptionsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkDescriptionsType_Framework() {
        return (EReference) frameworkDescriptionsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDescriptionsType_Capabilities() {
        return (EAttribute) frameworkDescriptionsTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDescriptionsType_Lang() {
        return (EAttribute) frameworkDescriptionsTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDescriptionsType_Service() {
        return (EAttribute) frameworkDescriptionsTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkDescriptionsType_Version() {
        return (EAttribute) frameworkDescriptionsTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkKeyDescriptionType() {
        return frameworkKeyDescriptionTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkKeyDescriptionType_Framework() {
        return (EReference) frameworkKeyDescriptionTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyDescriptionType_Capabilities() {
        return (EAttribute) frameworkKeyDescriptionTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyDescriptionType_Lang() {
        return (EAttribute) frameworkKeyDescriptionTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyDescriptionType_Service() {
        return (EAttribute) frameworkKeyDescriptionTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyDescriptionType_Version() {
        return (EAttribute) frameworkKeyDescriptionTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkKeyType() {
        return frameworkKeyTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkKeyType_Column() {
        return (EReference) frameworkKeyTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkKeyType1() {
        return frameworkKeyType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkKeyType1_Column() {
        return (EReference) frameworkKeyType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyType1_Complete() {
        return (EAttribute) frameworkKeyType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkKeyType1_Relationship() {
        return (EAttribute) frameworkKeyType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkType() {
        return frameworkTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType_FrameworkURI() {
        return (EAttribute) frameworkTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType_Organization() {
        return (EAttribute) frameworkTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType_Title() {
        return (EAttribute) frameworkTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType_Abstract() {
        return (EReference) frameworkTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType_ReferenceDate() {
        return (EReference) frameworkTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType_Version() {
        return (EAttribute) frameworkTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType_Documentation() {
        return (EAttribute) frameworkTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType_FrameworkKey() {
        return (EReference) frameworkTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType_BoundingCoordinates() {
        return (EReference) frameworkTypeEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkType1() {
        return frameworkType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType1_FrameworkURI() {
        return (EAttribute) frameworkType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType1_Organization() {
        return (EAttribute) frameworkType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType1_Title() {
        return (EAttribute) frameworkType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType1_Abstract() {
        return (EReference) frameworkType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType1_ReferenceDate() {
        return (EReference) frameworkType1EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType1_Version() {
        return (EAttribute) frameworkType1EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType1_Documentation() {
        return (EAttribute) frameworkType1EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType1_FrameworkKey() {
        return (EReference) frameworkType1EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType1_BoundingCoordinates() {
        return (EReference) frameworkType1EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType1_Dataset() {
        return (EReference) frameworkType1EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkType2() {
        return frameworkType2EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType2_FrameworkURI() {
        return (EAttribute) frameworkType2EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType2_Organization() {
        return (EAttribute) frameworkType2EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType2_Title() {
        return (EAttribute) frameworkType2EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType2_Abstract() {
        return (EReference) frameworkType2EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType2_ReferenceDate() {
        return (EReference) frameworkType2EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType2_Version() {
        return (EAttribute) frameworkType2EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType2_Documentation() {
        return (EAttribute) frameworkType2EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType2_FrameworkKey() {
        return (EReference) frameworkType2EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType2_BoundingCoordinates() {
        return (EReference) frameworkType2EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType2_DescribeDatasetsRequest() {
        return (EReference) frameworkType2EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkType3() {
        return frameworkType3EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType3_FrameworkURI() {
        return (EAttribute) frameworkType3EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType3_Organization() {
        return (EAttribute) frameworkType3EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType3_Title() {
        return (EAttribute) frameworkType3EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_Abstract() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_ReferenceDate() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType3_Version() {
        return (EAttribute) frameworkType3EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType3_Documentation() {
        return (EAttribute) frameworkType3EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_FrameworkKey() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_BoundingCoordinates() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_DescribeDatasetsRequest() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType3_Dataset() {
        return (EReference) frameworkType3EClass.getEStructuralFeatures().get(10);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getFrameworkType4() {
        return frameworkType4EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType4_FrameworkURI() {
        return (EAttribute) frameworkType4EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType4_Organization() {
        return (EAttribute) frameworkType4EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType4_Title() {
        return (EAttribute) frameworkType4EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_Abstract() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_ReferenceDate() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType4_Version() {
        return (EAttribute) frameworkType4EClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getFrameworkType4_Documentation() {
        return (EAttribute) frameworkType4EClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_FrameworkKey() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_BoundingCoordinates() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_DescribeDatasetsRequest() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getFrameworkType4_Dataset() {
        return (EReference) frameworkType4EClass.getEStructuralFeatures().get(10);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getGDASType() {
        return gdasTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGDASType_Framework() {
        return (EReference) gdasTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGDASType_Capabilities() {
        return (EAttribute) gdasTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGDASType_Lang() {
        return (EAttribute) gdasTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGDASType_Service() {
        return (EAttribute) gdasTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGDASType_Version() {
        return (EAttribute) gdasTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getGetCapabilitiesType() {
        return getCapabilitiesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetCapabilitiesType_AcceptVersions() {
        return (EReference) getCapabilitiesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetCapabilitiesType_Sections() {
        return (EReference) getCapabilitiesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetCapabilitiesType_AcceptFormats() {
        return (EReference) getCapabilitiesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetCapabilitiesType_Language() {
        return (EAttribute) getCapabilitiesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetCapabilitiesType_Service() {
        return (EAttribute) getCapabilitiesTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetCapabilitiesType_UpdateSequence() {
        return (EAttribute) getCapabilitiesTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getGetDataRequestType() {
        return getDataRequestTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataRequestType_Href() {
        return (EAttribute) getDataRequestTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getGetDataType() {
        return getDataTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataType_FrameworkURI() {
        return (EAttribute) getDataTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataType_DatasetURI() {
        return (EAttribute) getDataTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataType_Attributes() {
        return (EAttribute) getDataTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataType_LinkageKeys() {
        return (EAttribute) getDataTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetDataType_FilterColumn() {
        return (EReference) getDataTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetDataType_FilterValue() {
        return (EReference) getDataTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getGetDataType_XSL() {
        return (EReference) getDataTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataType_Aid() {
        return (EAttribute) getDataTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getGetDataXMLType() {
        return getDataXMLTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_FrameworkURI() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_DatasetURI() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_Attributes() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_LinkageKeys() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_GetDataHost() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getGetDataXMLType_Language() {
        return (EAttribute) getDataXMLTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getJoinAbilitiesType() {
        return joinAbilitiesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinAbilitiesType_SpatialFrameworks() {
        return (EReference) joinAbilitiesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_AttributeLimit() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinAbilitiesType_OutputMechanisms() {
        return (EReference) joinAbilitiesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinAbilitiesType_OutputStylings() {
        return (EReference) joinAbilitiesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinAbilitiesType_ClassificationSchemaURL() {
        return (EReference) joinAbilitiesTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_Capabilities() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_Lang() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_Service() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_UpdateSupported() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinAbilitiesType_Version() {
        return (EAttribute) joinAbilitiesTypeEClass.getEStructuralFeatures().get(9);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getJoinDataResponseType() {
        return joinDataResponseTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataResponseType_Status() {
        return (EReference) joinDataResponseTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataResponseType_DataInputs() {
        return (EReference) joinDataResponseTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataResponseType_JoinedOutputs() {
        return (EReference) joinDataResponseTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinDataResponseType_Capabilities() {
        return (EAttribute) joinDataResponseTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinDataResponseType_Lang() {
        return (EAttribute) joinDataResponseTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinDataResponseType_Service() {
        return (EAttribute) joinDataResponseTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinDataResponseType_Version() {
        return (EAttribute) joinDataResponseTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getJoinDataType() {
        return joinDataTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataType_AttributeData() {
        return (EReference) joinDataTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataType_MapStyling() {
        return (EReference) joinDataTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinDataType_ClassificationURL() {
        return (EReference) joinDataTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getJoinDataType_Update() {
        return (EAttribute) joinDataTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getJoinedOutputsType() {
        return joinedOutputsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getJoinedOutputsType_Output() {
        return (EReference) joinedOutputsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getKType() {
        return kTypeEClass;
    }

    public EAttribute getShortForm_Value() {
        return (EAttribute) shortFormEClass.getEStructuralFeatures().get(0);
    }

    public EAttribute getLongForm_Value() {
        return (EAttribute) longFormEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getKType_Value() {
        return (EAttribute) kTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getKType_Aid() {
        return (EAttribute) kTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getLanguagesType() {
        return languagesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getLanguagesType_Language() {
        return (EAttribute) languagesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getMapStylingType() {
        return mapStylingTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getMapStylingType_StylingIdentifier() {
        return (EReference) mapStylingTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getMapStylingType_StylingURL() {
        return (EAttribute) mapStylingTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getMeasureCountExceptions() {
        return measureCountExceptionsEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getMeasureCountExceptions_Null() {
        return (EReference) measureCountExceptionsEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getMeasureType() {
        return measureTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getMeasureType_UOM() {
        return (EReference) measureTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getMeasureType_Uncertainty() {
        return (EReference) measureTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getMeasureType_Exceptions() {
        return (EReference) measureTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getMechanismType() {
        return mechanismTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getMechanismType_Identifier() {
        return (EAttribute) mechanismTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getMechanismType_Title() {
        return (EAttribute) mechanismTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getMechanismType_Abstract() {
        return (EAttribute) mechanismTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getMechanismType_Reference() {
        return (EAttribute) mechanismTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getNominalOrdinalExceptions() {
        return nominalOrdinalExceptionsEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getNominalOrdinalExceptions_Null() {
        return (EReference) nominalOrdinalExceptionsEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getNominalType() {
        return nominalTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getLongForm() {
        return longFormEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getShortForm() {
        return shortFormEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getNominalType_Classes() {
        return (EReference) nominalTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getNominalType_Exceptions() {
        return (EReference) nominalTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getNullType() {
        return nullTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType_Identifier() {
        return (EAttribute) nullTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType_Title() {
        return (EAttribute) nullTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getNullType_Abstract() {
        return (EReference) nullTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType_Documentation() {
        return (EAttribute) nullTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getNullType1() {
        return nullType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType1_Identifier() {
        return (EAttribute) nullType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType1_Title() {
        return (EAttribute) nullType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getNullType1_Abstract() {
        return (EReference) nullType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType1_Documentation() {
        return (EAttribute) nullType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getNullType1_Color() {
        return (EAttribute) nullType1EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getOrdinalType() {
        return ordinalTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOrdinalType_Classes() {
        return (EReference) ordinalTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOrdinalType_Exceptions() {
        return (EReference) ordinalTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getOutputMechanismsType() {
        return outputMechanismsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOutputMechanismsType_Mechanism() {
        return (EReference) outputMechanismsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getOutputStylingsType() {
        return outputStylingsTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOutputStylingsType_Styling() {
        return (EReference) outputStylingsTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getOutputStylingsType1() {
        return outputStylingsType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getOutputType() {
        return outputTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOutputType_Mechanism() {
        return (EReference) outputTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOutputType_Resource() {
        return (EReference) outputTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getOutputType_ExceptionReport() {
        return (EReference) outputTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getParameterType() {
        return parameterTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getParameterType_Value() {
        return (EAttribute) parameterTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getParameterType_Name() {
        return (EAttribute) parameterTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getReferenceDateType() {
        return referenceDateTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getReferenceDateType_Value() {
        return (EAttribute) referenceDateTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getReferenceDateType_StartDate() {
        return (EAttribute) referenceDateTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getRequestBaseType() {
        return requestBaseTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getRequestBaseType_Language() {
        return (EAttribute) requestBaseTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getRequestBaseType_Service() {
        return (EAttribute) requestBaseTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getRequestBaseType_Version() {
        return (EAttribute) requestBaseTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getResourceType() {
        return resourceTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getResourceType_URL() {
        return (EReference) resourceTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getResourceType_Parameter() {
        return (EReference) resourceTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getRowsetType() {
        return rowsetTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getRowsetType_Row() {
        return (EReference) rowsetTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getRowsetType1() {
        return rowsetType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getRowsetType1_Row() {
        return (EReference) rowsetType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getRowType() {
        return rowTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getRowType_K() {
        return (EReference) rowTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getRowType_Title() {
        return (EAttribute) rowTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getRowType1() {
        return rowType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getRowType1_K() {
        return (EReference) rowType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getRowType1_V() {
        return (EReference) rowType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getSpatialFrameworksType() {
        return spatialFrameworksTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getSpatialFrameworksType_Framework() {
        return (EReference) spatialFrameworksTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getStatusType() {
        return statusTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getStatusType_Accepted() {
        return (EReference) statusTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getStatusType_Completed() {
        return (EReference) statusTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getStatusType_Failed() {
        return (EReference) statusTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStatusType_CreationTime() {
        return (EAttribute) statusTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStatusType_Href() {
        return (EAttribute) statusTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getStylingType() {
        return stylingTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStylingType_Identifier() {
        return (EAttribute) stylingTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStylingType_Title() {
        return (EAttribute) stylingTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStylingType_Abstract() {
        return (EAttribute) stylingTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStylingType_Reference() {
        return (EAttribute) stylingTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getStylingType_Schema() {
        return (EAttribute) stylingTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getTjsCapabilitiesType() {
        return tjsCapabilitiesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getTjsCapabilitiesType_ServiceIdentification() {
        return (EReference) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getTjsCapabilitiesType_ServiceProvider() {
        return (EReference) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getTjsCapabilitiesType_OperationsMetadata() {
        return (EReference) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getTjsCapabilitiesType_Languages() {
        return (EReference) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getTjsCapabilitiesType_WSDL() {
        return (EReference) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getTjsCapabilitiesType_Lang() {
        return (EAttribute) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getTjsCapabilitiesType_Service() {
        return (EAttribute) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(6);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getTjsCapabilitiesType_UpdateSequence() {
        return (EAttribute) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(7);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getTjsCapabilitiesType_Version() {
        return (EAttribute) tjsCapabilitiesTypeEClass.getEStructuralFeatures().get(8);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getUncertaintyType() {
        return uncertaintyTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getUncertaintyType_Value() {
        return (EAttribute) uncertaintyTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getUncertaintyType_Gaussian() {
        return (EAttribute) uncertaintyTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getUOMType() {
        return uomTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getUOMType_ShortForm() {
        return (EReference) uomTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getUOMType_LongForm() {
        return (EReference) uomTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getUOMType_Reference() {
        return (EAttribute) uomTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getValuesType() {
        return valuesTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValuesType_Nominal() {
        return (EReference) valuesTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValuesType_Ordinal() {
        return (EReference) valuesTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValuesType_Count() {
        return (EReference) valuesTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValuesType_Measure() {
        return (EReference) valuesTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getValueType() {
        return valueTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType_Identifier() {
        return (EAttribute) valueTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType_Title() {
        return (EAttribute) valueTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValueType_Abstract() {
        return (EReference) valueTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType_Documentation() {
        return (EAttribute) valueTypeEClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType_Color() {
        return (EAttribute) valueTypeEClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType_Rank() {
        return (EAttribute) valueTypeEClass.getEStructuralFeatures().get(5);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getValueType1() {
        return valueType1EClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType1_Identifier() {
        return (EAttribute) valueType1EClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType1_Title() {
        return (EAttribute) valueType1EClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EReference getValueType1_Abstract() {
        return (EReference) valueType1EClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType1_Documentation() {
        return (EAttribute) valueType1EClass.getEStructuralFeatures().get(3);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getValueType1_Color() {
        return (EAttribute) valueType1EClass.getEStructuralFeatures().get(4);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getVType() {
        return vTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getVType_Value() {
        return (EAttribute) vTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getVType_Aid() {
        return (EAttribute) vTypeEClass.getEStructuralFeatures().get(1);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getVType_Null() {
        return (EAttribute) vTypeEClass.getEStructuralFeatures().get(2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EClass getWSDLType() {
        return wsdlTypeEClass;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EAttribute getWSDLType_Href() {
        return (EAttribute) wsdlTypeEClass.getEStructuralFeatures().get(0);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDataClassType() {
        return dataClassTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDescribeDatasetsValueType() {
        return describeDatasetsValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDescribeDataValueType() {
        return describeDataValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDescribeFrameworksValueType() {
        return describeFrameworksValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDescribeJoinAbilitiesValueType() {
        return describeJoinAbilitiesValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getDescribeKeyValueType() {
        return describeKeyValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getGaussianType() {
        return gaussianTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getGetCapabilitiesValueType() {
        return getCapabilitiesValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getGetDataValueType() {
        return getDataValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getJoinDataValueType() {
        return joinDataValueTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getPurposeType() {
        return purposeTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getRequestServiceType() {
        return requestServiceTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getTypeType() {
        return typeTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getUpdateType() {
        return updateTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getVersionType() {
        return versionTypeEEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getVersionType1() {
        return versionType1EEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EEnum getVersionType2() {
        return versionType2EEnum;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getAcceptLanguagesType() {
        return acceptLanguagesTypeEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDataClassTypeObject() {
        return dataClassTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDescribeDatasetsValueTypeObject() {
        return describeDatasetsValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDescribeDataValueTypeObject() {
        return describeDataValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDescribeFrameworksValueTypeObject() {
        return describeFrameworksValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDescribeJoinAbilitiesValueTypeObject() {
        return describeJoinAbilitiesValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getDescribeKeyValueTypeObject() {
        return describeKeyValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getGaussianTypeObject() {
        return gaussianTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getGetCapabilitiesValueTypeObject() {
        return getCapabilitiesValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getGetDataValueTypeObject() {
        return getDataValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getJoinDataValueTypeObject() {
        return joinDataValueTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getPurposeTypeObject() {
        return purposeTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getRequestServiceTypeObject() {
        return requestServiceTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getSectionsType() {
        return sectionsTypeEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getTypeTypeObject() {
        return typeTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getUpdateTypeObject() {
        return updateTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getVersionTypeObject() {
        return versionTypeObjectEDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getVersionTypeObject1() {
        return versionTypeObject1EDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EDataType getVersionTypeObject2() {
        return versionTypeObject2EDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10Factory getTjs10Factory() {
        return (Tjs10Factory) getEFactoryInstance();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private boolean isCreated = false;

    /**
     * Creates the meta-model objects for the package.  This method is
     * guarded to have no affect on any invocation but its first.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void createPackageContents() {
        if (isCreated) return;
        isCreated = true;

        // Create classes and their features
        abstractTypeEClass = createEClass(ABSTRACT_TYPE);

        acceptVersionsTypeEClass = createEClass(ACCEPT_VERSIONS_TYPE);
        createEAttribute(acceptVersionsTypeEClass, ACCEPT_VERSIONS_TYPE__VERSION);

        attributeDataTypeEClass = createEClass(ATTRIBUTE_DATA_TYPE);
        createEAttribute(attributeDataTypeEClass, ATTRIBUTE_DATA_TYPE__GET_DATA_URL);
        createEReference(attributeDataTypeEClass, ATTRIBUTE_DATA_TYPE__GET_DATA_XML);

        attributesTypeEClass = createEClass(ATTRIBUTES_TYPE);
        createEReference(attributesTypeEClass, ATTRIBUTES_TYPE__COLUMN);

        boundingCoordinatesTypeEClass = createEClass(BOUNDING_COORDINATES_TYPE);
        createEAttribute(boundingCoordinatesTypeEClass, BOUNDING_COORDINATES_TYPE__NORTH);
        createEAttribute(boundingCoordinatesTypeEClass, BOUNDING_COORDINATES_TYPE__SOUTH);
        createEAttribute(boundingCoordinatesTypeEClass, BOUNDING_COORDINATES_TYPE__EAST);
        createEAttribute(boundingCoordinatesTypeEClass, BOUNDING_COORDINATES_TYPE__WEST);

        classesTypeEClass = createEClass(CLASSES_TYPE);
        createEAttribute(classesTypeEClass, CLASSES_TYPE__TITLE);
        createEReference(classesTypeEClass, CLASSES_TYPE__ABSTRACT);
        createEAttribute(classesTypeEClass, CLASSES_TYPE__DOCUMENTATION);
        createEReference(classesTypeEClass, CLASSES_TYPE__VALUE);

        classesType1EClass = createEClass(CLASSES_TYPE1);
        createEAttribute(classesType1EClass, CLASSES_TYPE1__TITLE);
        createEReference(classesType1EClass, CLASSES_TYPE1__ABSTRACT);
        createEAttribute(classesType1EClass, CLASSES_TYPE1__DOCUMENTATION);
        createEReference(classesType1EClass, CLASSES_TYPE1__VALUE);

        columnsetTypeEClass = createEClass(COLUMNSET_TYPE);
        createEReference(columnsetTypeEClass, COLUMNSET_TYPE__FRAMEWORK_KEY);
        createEReference(columnsetTypeEClass, COLUMNSET_TYPE__ATTRIBUTES);

        columnTypeEClass = createEClass(COLUMN_TYPE);
        createEAttribute(columnTypeEClass, COLUMN_TYPE__DECIMALS);
        createEAttribute(columnTypeEClass, COLUMN_TYPE__LENGTH);
        createEAttribute(columnTypeEClass, COLUMN_TYPE__NAME);
        createEAttribute(columnTypeEClass, COLUMN_TYPE__TYPE);

        columnType1EClass = createEClass(COLUMN_TYPE1);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__TITLE);
        createEReference(columnType1EClass, COLUMN_TYPE1__ABSTRACT);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__DOCUMENTATION);
        createEReference(columnType1EClass, COLUMN_TYPE1__VALUES);
        createEReference(columnType1EClass, COLUMN_TYPE1__GET_DATA_REQUEST);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__DECIMALS);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__LENGTH);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__NAME);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__PURPOSE);
        createEAttribute(columnType1EClass, COLUMN_TYPE1__TYPE);

        columnType2EClass = createEClass(COLUMN_TYPE2);
        createEAttribute(columnType2EClass, COLUMN_TYPE2__DECIMALS);
        createEAttribute(columnType2EClass, COLUMN_TYPE2__LENGTH);
        createEAttribute(columnType2EClass, COLUMN_TYPE2__NAME);
        createEAttribute(columnType2EClass, COLUMN_TYPE2__TYPE);

        countTypeEClass = createEClass(COUNT_TYPE);
        createEReference(countTypeEClass, COUNT_TYPE__UOM);
        createEReference(countTypeEClass, COUNT_TYPE__UNCERTAINTY);
        createEReference(countTypeEClass, COUNT_TYPE__EXCEPTIONS);

        dataDescriptionsTypeEClass = createEClass(DATA_DESCRIPTIONS_TYPE);
        createEReference(dataDescriptionsTypeEClass, DATA_DESCRIPTIONS_TYPE__FRAMEWORK);
        createEAttribute(dataDescriptionsTypeEClass, DATA_DESCRIPTIONS_TYPE__CAPABILITIES);
        createEAttribute(dataDescriptionsTypeEClass, DATA_DESCRIPTIONS_TYPE__LANG);
        createEAttribute(dataDescriptionsTypeEClass, DATA_DESCRIPTIONS_TYPE__SERVICE);
        createEAttribute(dataDescriptionsTypeEClass, DATA_DESCRIPTIONS_TYPE__VERSION);

        dataInputsTypeEClass = createEClass(DATA_INPUTS_TYPE);
        createEReference(dataInputsTypeEClass, DATA_INPUTS_TYPE__FRAMEWORK);

        datasetDescriptionsTypeEClass = createEClass(DATASET_DESCRIPTIONS_TYPE);
        createEReference(datasetDescriptionsTypeEClass, DATASET_DESCRIPTIONS_TYPE__FRAMEWORK);
        createEAttribute(datasetDescriptionsTypeEClass, DATASET_DESCRIPTIONS_TYPE__CAPABILITIES);
        createEAttribute(datasetDescriptionsTypeEClass, DATASET_DESCRIPTIONS_TYPE__LANG);
        createEAttribute(datasetDescriptionsTypeEClass, DATASET_DESCRIPTIONS_TYPE__SERVICE);
        createEAttribute(datasetDescriptionsTypeEClass, DATASET_DESCRIPTIONS_TYPE__VERSION);

        datasetTypeEClass = createEClass(DATASET_TYPE);
        createEAttribute(datasetTypeEClass, DATASET_TYPE__DATASET_URI);
        createEAttribute(datasetTypeEClass, DATASET_TYPE__ORGANIZATION);
        createEAttribute(datasetTypeEClass, DATASET_TYPE__TITLE);
        createEReference(datasetTypeEClass, DATASET_TYPE__ABSTRACT);
        createEReference(datasetTypeEClass, DATASET_TYPE__REFERENCE_DATE);
        createEAttribute(datasetTypeEClass, DATASET_TYPE__VERSION);
        createEAttribute(datasetTypeEClass, DATASET_TYPE__DOCUMENTATION);
        createEReference(datasetTypeEClass, DATASET_TYPE__DESCRIBE_DATA_REQUEST);

        datasetType1EClass = createEClass(DATASET_TYPE1);
        createEAttribute(datasetType1EClass, DATASET_TYPE1__DATASET_URI);
        createEAttribute(datasetType1EClass, DATASET_TYPE1__ORGANIZATION);
        createEAttribute(datasetType1EClass, DATASET_TYPE1__TITLE);
        createEReference(datasetType1EClass, DATASET_TYPE1__ABSTRACT);
        createEReference(datasetType1EClass, DATASET_TYPE1__REFERENCE_DATE);
        createEAttribute(datasetType1EClass, DATASET_TYPE1__VERSION);
        createEAttribute(datasetType1EClass, DATASET_TYPE1__DOCUMENTATION);
        createEReference(datasetType1EClass, DATASET_TYPE1__COLUMNSET);
        createEReference(datasetType1EClass, DATASET_TYPE1__ROWSET);

        datasetType2EClass = createEClass(DATASET_TYPE2);
        createEAttribute(datasetType2EClass, DATASET_TYPE2__DATASET_URI);
        createEAttribute(datasetType2EClass, DATASET_TYPE2__ORGANIZATION);
        createEAttribute(datasetType2EClass, DATASET_TYPE2__TITLE);
        createEReference(datasetType2EClass, DATASET_TYPE2__ABSTRACT);
        createEReference(datasetType2EClass, DATASET_TYPE2__REFERENCE_DATE);
        createEAttribute(datasetType2EClass, DATASET_TYPE2__VERSION);
        createEAttribute(datasetType2EClass, DATASET_TYPE2__DOCUMENTATION);
        createEReference(datasetType2EClass, DATASET_TYPE2__DESCRIBE_DATA_REQUEST);
        createEReference(datasetType2EClass, DATASET_TYPE2__COLUMNSET);
        createEReference(datasetType2EClass, DATASET_TYPE2__ROWSET);

        datasetType3EClass = createEClass(DATASET_TYPE3);
        createEAttribute(datasetType3EClass, DATASET_TYPE3__DATASET_URI);
        createEAttribute(datasetType3EClass, DATASET_TYPE3__ORGANIZATION);
        createEAttribute(datasetType3EClass, DATASET_TYPE3__TITLE);
        createEReference(datasetType3EClass, DATASET_TYPE3__ABSTRACT);
        createEReference(datasetType3EClass, DATASET_TYPE3__REFERENCE_DATE);
        createEAttribute(datasetType3EClass, DATASET_TYPE3__VERSION);
        createEAttribute(datasetType3EClass, DATASET_TYPE3__DOCUMENTATION);
        createEReference(datasetType3EClass, DATASET_TYPE3__DESCRIBE_DATA_REQUEST);
        createEReference(datasetType3EClass, DATASET_TYPE3__COLUMNSET);

        describeDataRequestTypeEClass = createEClass(DESCRIBE_DATA_REQUEST_TYPE);
        createEAttribute(describeDataRequestTypeEClass, DESCRIBE_DATA_REQUEST_TYPE__HREF);

        describeDatasetsRequestTypeEClass = createEClass(DESCRIBE_DATASETS_REQUEST_TYPE);
        createEAttribute(describeDatasetsRequestTypeEClass, DESCRIBE_DATASETS_REQUEST_TYPE__HREF);

        describeDatasetsTypeEClass = createEClass(DESCRIBE_DATASETS_TYPE);
        createEAttribute(describeDatasetsTypeEClass, DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI);
        createEAttribute(describeDatasetsTypeEClass, DESCRIBE_DATASETS_TYPE__DATASET_URI);

        describeDataTypeEClass = createEClass(DESCRIBE_DATA_TYPE);
        createEAttribute(describeDataTypeEClass, DESCRIBE_DATA_TYPE__FRAMEWORK_URI);
        createEAttribute(describeDataTypeEClass, DESCRIBE_DATA_TYPE__DATASET_URI);
        createEAttribute(describeDataTypeEClass, DESCRIBE_DATA_TYPE__ATTRIBUTES);

        describeFrameworkKeyTypeEClass = createEClass(DESCRIBE_FRAMEWORK_KEY_TYPE);
        createEAttribute(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI);
        createEAttribute(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION);
        createEAttribute(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE);
        createEReference(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT);
        createEReference(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE);
        createEAttribute(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION);
        createEAttribute(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION);
        createEReference(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY);
        createEReference(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES);
        createEReference(describeFrameworkKeyTypeEClass, DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET);

        describeFrameworksTypeEClass = createEClass(DESCRIBE_FRAMEWORKS_TYPE);
        createEAttribute(describeFrameworksTypeEClass, DESCRIBE_FRAMEWORKS_TYPE__FRAMEWORK_URI);

        describeKeyTypeEClass = createEClass(DESCRIBE_KEY_TYPE);
        createEAttribute(describeKeyTypeEClass, DESCRIBE_KEY_TYPE__FRAMEWORK_URI);

        documentRootEClass = createEClass(DOCUMENT_ROOT);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__MIXED);
        createEReference(documentRootEClass, DOCUMENT_ROOT__XMLNS_PREFIX_MAP);
        createEReference(documentRootEClass, DOCUMENT_ROOT__XSI_SCHEMA_LOCATION);
        createEReference(documentRootEClass, DOCUMENT_ROOT__ABSTRACT);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__ATTRIBUTE_LIMIT);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__ATTRIBUTES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__BOUNDING_COORDINATES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__CAPABILITIES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__COLUMNSET);
        createEReference(documentRootEClass, DOCUMENT_ROOT__COUNT);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__DATA_CLASS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DATA_DESCRIPTIONS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DATASET);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DATASET_DESCRIPTIONS);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__DATASET_URI);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_DATA);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_DATASETS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__DESCRIBE_KEY);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__DOCUMENTATION);
        createEReference(documentRootEClass, DOCUMENT_ROOT__FRAMEWORK);
        createEReference(documentRootEClass, DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__FRAMEWORK_KEY);
        createEReference(documentRootEClass, DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__FRAMEWORK_URI);
        createEReference(documentRootEClass, DOCUMENT_ROOT__GDAS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__GET_CAPABILITIES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__GET_DATA);
        createEReference(documentRootEClass, DOCUMENT_ROOT__GET_DATA_REQUEST);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__IDENTIFIER);
        createEReference(documentRootEClass, DOCUMENT_ROOT__JOIN_ABILITIES);
        createEReference(documentRootEClass, DOCUMENT_ROOT__JOIN_DATA);
        createEReference(documentRootEClass, DOCUMENT_ROOT__JOIN_DATA_RESPONSE);
        createEReference(documentRootEClass, DOCUMENT_ROOT__K);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__LINKAGE_KEYS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__MEASURE);
        createEReference(documentRootEClass, DOCUMENT_ROOT__MECHANISM);
        createEReference(documentRootEClass, DOCUMENT_ROOT__NOMINAL);
        createEReference(documentRootEClass, DOCUMENT_ROOT__ORDINAL);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__ORGANIZATION);
        createEReference(documentRootEClass, DOCUMENT_ROOT__REFERENCE_DATE);
        createEReference(documentRootEClass, DOCUMENT_ROOT__ROWSET);
        createEReference(documentRootEClass, DOCUMENT_ROOT__SPATIAL_FRAMEWORKS);
        createEReference(documentRootEClass, DOCUMENT_ROOT__STYLING);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__TITLE);
        createEReference(documentRootEClass, DOCUMENT_ROOT__UNCERTAINTY);
        createEReference(documentRootEClass, DOCUMENT_ROOT__UOM);
        createEReference(documentRootEClass, DOCUMENT_ROOT__VALUES);
        createEAttribute(documentRootEClass, DOCUMENT_ROOT__VERSION);

        exceptionReportTypeEClass = createEClass(EXCEPTION_REPORT_TYPE);
        createEReference(exceptionReportTypeEClass, EXCEPTION_REPORT_TYPE__EXCEPTION);

        failedTypeEClass = createEClass(FAILED_TYPE);

        frameworkDatasetDescribeDataTypeEClass = createEClass(FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE);
        createEAttribute(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__FRAMEWORK_URI);
        createEAttribute(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__ORGANIZATION);
        createEAttribute(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__TITLE);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__ABSTRACT);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__REFERENCE_DATE);
        createEAttribute(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__VERSION);
        createEAttribute(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DOCUMENTATION);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__FRAMEWORK_KEY);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__BOUNDING_COORDINATES);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DESCRIBE_DATASETS_REQUEST);
        createEReference(frameworkDatasetDescribeDataTypeEClass, FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DATASET);

        frameworkDescriptionsTypeEClass = createEClass(FRAMEWORK_DESCRIPTIONS_TYPE);
        createEReference(frameworkDescriptionsTypeEClass, FRAMEWORK_DESCRIPTIONS_TYPE__FRAMEWORK);
        createEAttribute(frameworkDescriptionsTypeEClass, FRAMEWORK_DESCRIPTIONS_TYPE__CAPABILITIES);
        createEAttribute(frameworkDescriptionsTypeEClass, FRAMEWORK_DESCRIPTIONS_TYPE__LANG);
        createEAttribute(frameworkDescriptionsTypeEClass, FRAMEWORK_DESCRIPTIONS_TYPE__SERVICE);
        createEAttribute(frameworkDescriptionsTypeEClass, FRAMEWORK_DESCRIPTIONS_TYPE__VERSION);

        frameworkKeyDescriptionTypeEClass = createEClass(FRAMEWORK_KEY_DESCRIPTION_TYPE);
        createEReference(frameworkKeyDescriptionTypeEClass, FRAMEWORK_KEY_DESCRIPTION_TYPE__FRAMEWORK);
        createEAttribute(frameworkKeyDescriptionTypeEClass, FRAMEWORK_KEY_DESCRIPTION_TYPE__CAPABILITIES);
        createEAttribute(frameworkKeyDescriptionTypeEClass, FRAMEWORK_KEY_DESCRIPTION_TYPE__LANG);
        createEAttribute(frameworkKeyDescriptionTypeEClass, FRAMEWORK_KEY_DESCRIPTION_TYPE__SERVICE);
        createEAttribute(frameworkKeyDescriptionTypeEClass, FRAMEWORK_KEY_DESCRIPTION_TYPE__VERSION);

        frameworkKeyTypeEClass = createEClass(FRAMEWORK_KEY_TYPE);
        createEReference(frameworkKeyTypeEClass, FRAMEWORK_KEY_TYPE__COLUMN);

        frameworkKeyType1EClass = createEClass(FRAMEWORK_KEY_TYPE1);
        createEReference(frameworkKeyType1EClass, FRAMEWORK_KEY_TYPE1__COLUMN);
        createEAttribute(frameworkKeyType1EClass, FRAMEWORK_KEY_TYPE1__COMPLETE);
        createEAttribute(frameworkKeyType1EClass, FRAMEWORK_KEY_TYPE1__RELATIONSHIP);

        frameworkTypeEClass = createEClass(FRAMEWORK_TYPE);
        createEAttribute(frameworkTypeEClass, FRAMEWORK_TYPE__FRAMEWORK_URI);
        createEAttribute(frameworkTypeEClass, FRAMEWORK_TYPE__ORGANIZATION);
        createEAttribute(frameworkTypeEClass, FRAMEWORK_TYPE__TITLE);
        createEReference(frameworkTypeEClass, FRAMEWORK_TYPE__ABSTRACT);
        createEReference(frameworkTypeEClass, FRAMEWORK_TYPE__REFERENCE_DATE);
        createEAttribute(frameworkTypeEClass, FRAMEWORK_TYPE__VERSION);
        createEAttribute(frameworkTypeEClass, FRAMEWORK_TYPE__DOCUMENTATION);
        createEReference(frameworkTypeEClass, FRAMEWORK_TYPE__FRAMEWORK_KEY);
        createEReference(frameworkTypeEClass, FRAMEWORK_TYPE__BOUNDING_COORDINATES);

        frameworkType1EClass = createEClass(FRAMEWORK_TYPE1);
        createEAttribute(frameworkType1EClass, FRAMEWORK_TYPE1__FRAMEWORK_URI);
        createEAttribute(frameworkType1EClass, FRAMEWORK_TYPE1__ORGANIZATION);
        createEAttribute(frameworkType1EClass, FRAMEWORK_TYPE1__TITLE);
        createEReference(frameworkType1EClass, FRAMEWORK_TYPE1__ABSTRACT);
        createEReference(frameworkType1EClass, FRAMEWORK_TYPE1__REFERENCE_DATE);
        createEAttribute(frameworkType1EClass, FRAMEWORK_TYPE1__VERSION);
        createEAttribute(frameworkType1EClass, FRAMEWORK_TYPE1__DOCUMENTATION);
        createEReference(frameworkType1EClass, FRAMEWORK_TYPE1__FRAMEWORK_KEY);
        createEReference(frameworkType1EClass, FRAMEWORK_TYPE1__BOUNDING_COORDINATES);
        createEReference(frameworkType1EClass, FRAMEWORK_TYPE1__DATASET);

        frameworkType2EClass = createEClass(FRAMEWORK_TYPE2);
        createEAttribute(frameworkType2EClass, FRAMEWORK_TYPE2__FRAMEWORK_URI);
        createEAttribute(frameworkType2EClass, FRAMEWORK_TYPE2__ORGANIZATION);
        createEAttribute(frameworkType2EClass, FRAMEWORK_TYPE2__TITLE);
        createEReference(frameworkType2EClass, FRAMEWORK_TYPE2__ABSTRACT);
        createEReference(frameworkType2EClass, FRAMEWORK_TYPE2__REFERENCE_DATE);
        createEAttribute(frameworkType2EClass, FRAMEWORK_TYPE2__VERSION);
        createEAttribute(frameworkType2EClass, FRAMEWORK_TYPE2__DOCUMENTATION);
        createEReference(frameworkType2EClass, FRAMEWORK_TYPE2__FRAMEWORK_KEY);
        createEReference(frameworkType2EClass, FRAMEWORK_TYPE2__BOUNDING_COORDINATES);
        createEReference(frameworkType2EClass, FRAMEWORK_TYPE2__DESCRIBE_DATASETS_REQUEST);

        frameworkType3EClass = createEClass(FRAMEWORK_TYPE3);
        createEAttribute(frameworkType3EClass, FRAMEWORK_TYPE3__FRAMEWORK_URI);
        createEAttribute(frameworkType3EClass, FRAMEWORK_TYPE3__ORGANIZATION);
        createEAttribute(frameworkType3EClass, FRAMEWORK_TYPE3__TITLE);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__ABSTRACT);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__REFERENCE_DATE);
        createEAttribute(frameworkType3EClass, FRAMEWORK_TYPE3__VERSION);
        createEAttribute(frameworkType3EClass, FRAMEWORK_TYPE3__DOCUMENTATION);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__FRAMEWORK_KEY);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__BOUNDING_COORDINATES);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST);
        createEReference(frameworkType3EClass, FRAMEWORK_TYPE3__DATASET);

        frameworkType4EClass = createEClass(FRAMEWORK_TYPE4);
        createEAttribute(frameworkType4EClass, FRAMEWORK_TYPE4__FRAMEWORK_URI);
        createEAttribute(frameworkType4EClass, FRAMEWORK_TYPE4__ORGANIZATION);
        createEAttribute(frameworkType4EClass, FRAMEWORK_TYPE4__TITLE);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__ABSTRACT);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__REFERENCE_DATE);
        createEAttribute(frameworkType4EClass, FRAMEWORK_TYPE4__VERSION);
        createEAttribute(frameworkType4EClass, FRAMEWORK_TYPE4__DOCUMENTATION);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__FRAMEWORK_KEY);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__BOUNDING_COORDINATES);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__DESCRIBE_DATASETS_REQUEST);
        createEReference(frameworkType4EClass, FRAMEWORK_TYPE4__DATASET);

        gdasTypeEClass = createEClass(GDAS_TYPE);
        createEReference(gdasTypeEClass, GDAS_TYPE__FRAMEWORK);
        createEAttribute(gdasTypeEClass, GDAS_TYPE__CAPABILITIES);
        createEAttribute(gdasTypeEClass, GDAS_TYPE__LANG);
        createEAttribute(gdasTypeEClass, GDAS_TYPE__SERVICE);
        createEAttribute(gdasTypeEClass, GDAS_TYPE__VERSION);

        getCapabilitiesTypeEClass = createEClass(GET_CAPABILITIES_TYPE);
        createEReference(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS);
        createEReference(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__SECTIONS);
        createEReference(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__ACCEPT_FORMATS);
        createEAttribute(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__LANGUAGE);
        createEAttribute(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__SERVICE);
        createEAttribute(getCapabilitiesTypeEClass, GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE);

        getDataRequestTypeEClass = createEClass(GET_DATA_REQUEST_TYPE);
        createEAttribute(getDataRequestTypeEClass, GET_DATA_REQUEST_TYPE__HREF);

        getDataTypeEClass = createEClass(GET_DATA_TYPE);
        createEAttribute(getDataTypeEClass, GET_DATA_TYPE__FRAMEWORK_URI);
        createEAttribute(getDataTypeEClass, GET_DATA_TYPE__DATASET_URI);
        createEAttribute(getDataTypeEClass, GET_DATA_TYPE__ATTRIBUTES);
        createEAttribute(getDataTypeEClass, GET_DATA_TYPE__LINKAGE_KEYS);
        createEReference(getDataTypeEClass, GET_DATA_TYPE__FILTER_COLUMN);
        createEReference(getDataTypeEClass, GET_DATA_TYPE__FILTER_VALUE);
        createEReference(getDataTypeEClass, GET_DATA_TYPE__XSL);
        createEAttribute(getDataTypeEClass, GET_DATA_TYPE__AID);

        getDataXMLTypeEClass = createEClass(GET_DATA_XML_TYPE);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__FRAMEWORK_URI);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__DATASET_URI);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__ATTRIBUTES);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__LINKAGE_KEYS);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__GET_DATA_HOST);
        createEAttribute(getDataXMLTypeEClass, GET_DATA_XML_TYPE__LANGUAGE);

        joinAbilitiesTypeEClass = createEClass(JOIN_ABILITIES_TYPE);
        createEReference(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT);
        createEReference(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS);
        createEReference(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS);
        createEReference(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__CAPABILITIES);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__LANG);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__SERVICE);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED);
        createEAttribute(joinAbilitiesTypeEClass, JOIN_ABILITIES_TYPE__VERSION);

        joinDataResponseTypeEClass = createEClass(JOIN_DATA_RESPONSE_TYPE);
        createEReference(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__STATUS);
        createEReference(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS);
        createEReference(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS);
        createEAttribute(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__CAPABILITIES);
        createEAttribute(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__LANG);
        createEAttribute(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__SERVICE);
        createEAttribute(joinDataResponseTypeEClass, JOIN_DATA_RESPONSE_TYPE__VERSION);

        joinDataTypeEClass = createEClass(JOIN_DATA_TYPE);
        createEReference(joinDataTypeEClass, JOIN_DATA_TYPE__ATTRIBUTE_DATA);
        createEReference(joinDataTypeEClass, JOIN_DATA_TYPE__MAP_STYLING);
        createEReference(joinDataTypeEClass, JOIN_DATA_TYPE__CLASSIFICATION_URL);
        createEAttribute(joinDataTypeEClass, JOIN_DATA_TYPE__UPDATE);

        joinedOutputsTypeEClass = createEClass(JOINED_OUTPUTS_TYPE);
        createEReference(joinedOutputsTypeEClass, JOINED_OUTPUTS_TYPE__OUTPUT);

        kTypeEClass = createEClass(KTYPE);
        createEAttribute(kTypeEClass, KTYPE__VALUE);
        createEAttribute(kTypeEClass, KTYPE__AID);

        shortFormEClass = createEClass(SHORTFORM_TYPE);
        createEAttribute(shortFormEClass, SHORTFORM_TYPE__VALUE);

        longFormEClass = createEClass(LONGFORM_TYPE);
        createEAttribute(longFormEClass, LONGFORM_TYPE__VALUE);

        languagesTypeEClass = createEClass(LANGUAGES_TYPE);
        createEAttribute(languagesTypeEClass, LANGUAGES_TYPE__LANGUAGE);

        mapStylingTypeEClass = createEClass(MAP_STYLING_TYPE);
        createEReference(mapStylingTypeEClass, MAP_STYLING_TYPE__STYLING_IDENTIFIER);
        createEAttribute(mapStylingTypeEClass, MAP_STYLING_TYPE__STYLING_URL);

        measureCountExceptionsEClass = createEClass(MEASURE_COUNT_EXCEPTIONS);
        createEReference(measureCountExceptionsEClass, MEASURE_COUNT_EXCEPTIONS__NULL);

        measureTypeEClass = createEClass(MEASURE_TYPE);
        createEReference(measureTypeEClass, MEASURE_TYPE__UOM);
        createEReference(measureTypeEClass, MEASURE_TYPE__UNCERTAINTY);
        createEReference(measureTypeEClass, MEASURE_TYPE__EXCEPTIONS);

        mechanismTypeEClass = createEClass(MECHANISM_TYPE);
        createEAttribute(mechanismTypeEClass, MECHANISM_TYPE__IDENTIFIER);
        createEAttribute(mechanismTypeEClass, MECHANISM_TYPE__TITLE);
        createEAttribute(mechanismTypeEClass, MECHANISM_TYPE__ABSTRACT);
        createEAttribute(mechanismTypeEClass, MECHANISM_TYPE__REFERENCE);

        nominalOrdinalExceptionsEClass = createEClass(NOMINAL_ORDINAL_EXCEPTIONS);
        createEReference(nominalOrdinalExceptionsEClass, NOMINAL_ORDINAL_EXCEPTIONS__NULL);

        nominalTypeEClass = createEClass(NOMINAL_TYPE);
        createEReference(nominalTypeEClass, NOMINAL_TYPE__CLASSES);
        createEReference(nominalTypeEClass, NOMINAL_TYPE__EXCEPTIONS);

        nullTypeEClass = createEClass(NULL_TYPE);
        createEAttribute(nullTypeEClass, NULL_TYPE__IDENTIFIER);
        createEAttribute(nullTypeEClass, NULL_TYPE__TITLE);
        createEReference(nullTypeEClass, NULL_TYPE__ABSTRACT);
        createEAttribute(nullTypeEClass, NULL_TYPE__DOCUMENTATION);

        nullType1EClass = createEClass(NULL_TYPE1);
        createEAttribute(nullType1EClass, NULL_TYPE1__IDENTIFIER);
        createEAttribute(nullType1EClass, NULL_TYPE1__TITLE);
        createEReference(nullType1EClass, NULL_TYPE1__ABSTRACT);
        createEAttribute(nullType1EClass, NULL_TYPE1__DOCUMENTATION);
        createEAttribute(nullType1EClass, NULL_TYPE1__COLOR);

        ordinalTypeEClass = createEClass(ORDINAL_TYPE);
        createEReference(ordinalTypeEClass, ORDINAL_TYPE__CLASSES);
        createEReference(ordinalTypeEClass, ORDINAL_TYPE__EXCEPTIONS);

        outputMechanismsTypeEClass = createEClass(OUTPUT_MECHANISMS_TYPE);
        createEReference(outputMechanismsTypeEClass, OUTPUT_MECHANISMS_TYPE__MECHANISM);

        outputStylingsTypeEClass = createEClass(OUTPUT_STYLINGS_TYPE);
        createEReference(outputStylingsTypeEClass, OUTPUT_STYLINGS_TYPE__STYLING);

        outputStylingsType1EClass = createEClass(OUTPUT_STYLINGS_TYPE1);

        outputTypeEClass = createEClass(OUTPUT_TYPE);
        createEReference(outputTypeEClass, OUTPUT_TYPE__MECHANISM);
        createEReference(outputTypeEClass, OUTPUT_TYPE__RESOURCE);
        createEReference(outputTypeEClass, OUTPUT_TYPE__EXCEPTION_REPORT);

        parameterTypeEClass = createEClass(PARAMETER_TYPE);
        createEAttribute(parameterTypeEClass, PARAMETER_TYPE__VALUE);
        createEAttribute(parameterTypeEClass, PARAMETER_TYPE__NAME);

        referenceDateTypeEClass = createEClass(REFERENCE_DATE_TYPE);
        createEAttribute(referenceDateTypeEClass, REFERENCE_DATE_TYPE__VALUE);
        createEAttribute(referenceDateTypeEClass, REFERENCE_DATE_TYPE__START_DATE);

        requestBaseTypeEClass = createEClass(REQUEST_BASE_TYPE);
        createEAttribute(requestBaseTypeEClass, REQUEST_BASE_TYPE__LANGUAGE);
        createEAttribute(requestBaseTypeEClass, REQUEST_BASE_TYPE__SERVICE);
        createEAttribute(requestBaseTypeEClass, REQUEST_BASE_TYPE__VERSION);

        resourceTypeEClass = createEClass(RESOURCE_TYPE);
        createEReference(resourceTypeEClass, RESOURCE_TYPE__URL);
        createEReference(resourceTypeEClass, RESOURCE_TYPE__PARAMETER);

        rowsetTypeEClass = createEClass(ROWSET_TYPE);
        createEReference(rowsetTypeEClass, ROWSET_TYPE__ROW);

        rowsetType1EClass = createEClass(ROWSET_TYPE1);
        createEReference(rowsetType1EClass, ROWSET_TYPE1__ROW);

        rowTypeEClass = createEClass(ROW_TYPE);
        createEReference(rowTypeEClass, ROW_TYPE__K);
        createEAttribute(rowTypeEClass, ROW_TYPE__TITLE);

        rowType1EClass = createEClass(ROW_TYPE1);
        createEReference(rowType1EClass, ROW_TYPE1__K);
        createEReference(rowType1EClass, ROW_TYPE1__V);

        spatialFrameworksTypeEClass = createEClass(SPATIAL_FRAMEWORKS_TYPE);
        createEReference(spatialFrameworksTypeEClass, SPATIAL_FRAMEWORKS_TYPE__FRAMEWORK);

        statusTypeEClass = createEClass(STATUS_TYPE);
        createEReference(statusTypeEClass, STATUS_TYPE__ACCEPTED);
        createEReference(statusTypeEClass, STATUS_TYPE__COMPLETED);
        createEReference(statusTypeEClass, STATUS_TYPE__FAILED);
        createEAttribute(statusTypeEClass, STATUS_TYPE__CREATION_TIME);
        createEAttribute(statusTypeEClass, STATUS_TYPE__HREF);

        stylingTypeEClass = createEClass(STYLING_TYPE);
        createEAttribute(stylingTypeEClass, STYLING_TYPE__IDENTIFIER);
        createEAttribute(stylingTypeEClass, STYLING_TYPE__TITLE);
        createEAttribute(stylingTypeEClass, STYLING_TYPE__ABSTRACT);
        createEAttribute(stylingTypeEClass, STYLING_TYPE__REFERENCE);
        createEAttribute(stylingTypeEClass, STYLING_TYPE__SCHEMA);

        tjsCapabilitiesTypeEClass = createEClass(TJS_CAPABILITIES_TYPE);
        createEReference(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION);
        createEReference(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER);
        createEReference(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA);
        createEReference(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__LANGUAGES);
        createEReference(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__WSDL);
        createEAttribute(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__LANG);
        createEAttribute(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__SERVICE);
        createEAttribute(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE);
        createEAttribute(tjsCapabilitiesTypeEClass, TJS_CAPABILITIES_TYPE__VERSION);

        uncertaintyTypeEClass = createEClass(UNCERTAINTY_TYPE);
        createEAttribute(uncertaintyTypeEClass, UNCERTAINTY_TYPE__VALUE);
        createEAttribute(uncertaintyTypeEClass, UNCERTAINTY_TYPE__GAUSSIAN);

        uomTypeEClass = createEClass(UOM_TYPE);
        createEReference(uomTypeEClass, UOM_TYPE__SHORT_FORM);
        createEReference(uomTypeEClass, UOM_TYPE__LONG_FORM);
        createEAttribute(uomTypeEClass, UOM_TYPE__REFERENCE);

        valuesTypeEClass = createEClass(VALUES_TYPE);
        createEReference(valuesTypeEClass, VALUES_TYPE__NOMINAL);
        createEReference(valuesTypeEClass, VALUES_TYPE__ORDINAL);
        createEReference(valuesTypeEClass, VALUES_TYPE__COUNT);
        createEReference(valuesTypeEClass, VALUES_TYPE__MEASURE);

        valueTypeEClass = createEClass(VALUE_TYPE);
        createEAttribute(valueTypeEClass, VALUE_TYPE__IDENTIFIER);
        createEAttribute(valueTypeEClass, VALUE_TYPE__TITLE);
        createEReference(valueTypeEClass, VALUE_TYPE__ABSTRACT);
        createEAttribute(valueTypeEClass, VALUE_TYPE__DOCUMENTATION);
        createEAttribute(valueTypeEClass, VALUE_TYPE__COLOR);
        createEAttribute(valueTypeEClass, VALUE_TYPE__RANK);

        valueType1EClass = createEClass(VALUE_TYPE1);
        createEAttribute(valueType1EClass, VALUE_TYPE1__IDENTIFIER);
        createEAttribute(valueType1EClass, VALUE_TYPE1__TITLE);
        createEReference(valueType1EClass, VALUE_TYPE1__ABSTRACT);
        createEAttribute(valueType1EClass, VALUE_TYPE1__DOCUMENTATION);
        createEAttribute(valueType1EClass, VALUE_TYPE1__COLOR);

        vTypeEClass = createEClass(VTYPE);
        createEAttribute(vTypeEClass, VTYPE__VALUE);
        createEAttribute(vTypeEClass, VTYPE__AID);
        createEAttribute(vTypeEClass, VTYPE__NULL);

        wsdlTypeEClass = createEClass(WSDL_TYPE);
        createEAttribute(wsdlTypeEClass, WSDL_TYPE__HREF);

        // Create enums
        dataClassTypeEEnum = createEEnum(DATA_CLASS_TYPE);
        describeDatasetsValueTypeEEnum = createEEnum(DESCRIBE_DATASETS_VALUE_TYPE);
        describeDataValueTypeEEnum = createEEnum(DESCRIBE_DATA_VALUE_TYPE);
        describeFrameworksValueTypeEEnum = createEEnum(DESCRIBE_FRAMEWORKS_VALUE_TYPE);
        describeJoinAbilitiesValueTypeEEnum = createEEnum(DESCRIBE_JOIN_ABILITIES_VALUE_TYPE);
        describeKeyValueTypeEEnum = createEEnum(DESCRIBE_KEY_VALUE_TYPE);
        gaussianTypeEEnum = createEEnum(GAUSSIAN_TYPE);
        getCapabilitiesValueTypeEEnum = createEEnum(GET_CAPABILITIES_VALUE_TYPE);
        getDataValueTypeEEnum = createEEnum(GET_DATA_VALUE_TYPE);
        joinDataValueTypeEEnum = createEEnum(JOIN_DATA_VALUE_TYPE);
        purposeTypeEEnum = createEEnum(PURPOSE_TYPE);
        requestServiceTypeEEnum = createEEnum(REQUEST_SERVICE_TYPE);
        typeTypeEEnum = createEEnum(TYPE_TYPE);
        updateTypeEEnum = createEEnum(UPDATE_TYPE);
        versionTypeEEnum = createEEnum(VERSION_TYPE);
        versionType1EEnum = createEEnum(VERSION_TYPE1);
        versionType2EEnum = createEEnum(VERSION_TYPE2);

        // Create data types
        acceptLanguagesTypeEDataType = createEDataType(ACCEPT_LANGUAGES_TYPE);
        dataClassTypeObjectEDataType = createEDataType(DATA_CLASS_TYPE_OBJECT);
        describeDatasetsValueTypeObjectEDataType = createEDataType(DESCRIBE_DATASETS_VALUE_TYPE_OBJECT);
        describeDataValueTypeObjectEDataType = createEDataType(DESCRIBE_DATA_VALUE_TYPE_OBJECT);
        describeFrameworksValueTypeObjectEDataType = createEDataType(DESCRIBE_FRAMEWORKS_VALUE_TYPE_OBJECT);
        describeJoinAbilitiesValueTypeObjectEDataType = createEDataType(DESCRIBE_JOIN_ABILITIES_VALUE_TYPE_OBJECT);
        describeKeyValueTypeObjectEDataType = createEDataType(DESCRIBE_KEY_VALUE_TYPE_OBJECT);
        gaussianTypeObjectEDataType = createEDataType(GAUSSIAN_TYPE_OBJECT);
        getCapabilitiesValueTypeObjectEDataType = createEDataType(GET_CAPABILITIES_VALUE_TYPE_OBJECT);
        getDataValueTypeObjectEDataType = createEDataType(GET_DATA_VALUE_TYPE_OBJECT);
        joinDataValueTypeObjectEDataType = createEDataType(JOIN_DATA_VALUE_TYPE_OBJECT);
        purposeTypeObjectEDataType = createEDataType(PURPOSE_TYPE_OBJECT);
        requestServiceTypeObjectEDataType = createEDataType(REQUEST_SERVICE_TYPE_OBJECT);
        sectionsTypeEDataType = createEDataType(SECTIONS_TYPE);
        typeTypeObjectEDataType = createEDataType(TYPE_TYPE_OBJECT);
        updateTypeObjectEDataType = createEDataType(UPDATE_TYPE_OBJECT);
        versionTypeObjectEDataType = createEDataType(VERSION_TYPE_OBJECT);
        versionTypeObject1EDataType = createEDataType(VERSION_TYPE_OBJECT1);
        versionTypeObject2EDataType = createEDataType(VERSION_TYPE_OBJECT2);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private boolean isInitialized = false;

    /**
     * Complete the initialization of the package and its meta-model.  This
     * method is guarded to have no affect on any invocation but its first.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void initializePackageContents() {
        if (isInitialized) return;
        isInitialized = true;

        // Initialize package
        setName(eNAME);
        setNsPrefix(eNS_PREFIX);
        setNsURI(eNS_URI);

        // Obtain other dependent packages
        XMLTypePackage theXMLTypePackage = (XMLTypePackage) EPackage.Registry.INSTANCE.getEPackage(XMLTypePackage.eNS_URI);
        Ows11Package theOws11Package = (Ows11Package) EPackage.Registry.INSTANCE.getEPackage(Ows11Package.eNS_URI);

        // Add supertypes to classes
        abstractTypeEClass.getESuperTypes().add(theXMLTypePackage.getAnyType());
        describeDatasetsTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        describeDataTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        describeFrameworksTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        describeKeyTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        getDataTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        joinDataTypeEClass.getESuperTypes().add(this.getRequestBaseType());
        outputStylingsType1EClass.getESuperTypes().add(this.getOutputStylingsType());

        // Initialize classes and features; add operations and parameters
        initEClass(abstractTypeEClass, AbstractType.class, "AbstractType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);


        initEClass(shortFormEClass, ShortForm.class, "ShortForm", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getShortForm_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, KType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEClass(longFormEClass, LongForm.class, "LongForm", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getLongForm_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, KType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(acceptVersionsTypeEClass, AcceptVersionsType.class, "AcceptVersionsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getAcceptVersionsType_Version(), this.getVersionType1(), "version", null, 1, -1, AcceptVersionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(attributeDataTypeEClass, AttributeDataType.class, "AttributeDataType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getAttributeDataType_GetDataURL(), theXMLTypePackage.getAnyURI(), "getDataURL", null, 0, 1, AttributeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getAttributeDataType_GetDataXML(), this.getGetDataXMLType(), null, "getDataXML", null, 0, 1, AttributeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(attributesTypeEClass, AttributesType.class, "AttributesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getAttributesType_Column(), this.getColumnType1(), null, "column", null, 1, -1, AttributesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(boundingCoordinatesTypeEClass, BoundingCoordinatesType.class, "BoundingCoordinatesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getBoundingCoordinatesType_North(), theXMLTypePackage.getDecimal(), "north", null, 1, 1, BoundingCoordinatesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getBoundingCoordinatesType_South(), theXMLTypePackage.getDecimal(), "south", null, 1, 1, BoundingCoordinatesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getBoundingCoordinatesType_East(), theXMLTypePackage.getDecimal(), "east", null, 1, 1, BoundingCoordinatesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getBoundingCoordinatesType_West(), theXMLTypePackage.getDecimal(), "west", null, 1, 1, BoundingCoordinatesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(classesTypeEClass, ClassesType.class, "ClassesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getClassesType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, ClassesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getClassesType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, ClassesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getClassesType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, ClassesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getClassesType_Value(), this.getValueType(), null, "value", null, 1, -1, ClassesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(classesType1EClass, ClassesType1.class, "ClassesType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getClassesType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, ClassesType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getClassesType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, ClassesType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getClassesType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, ClassesType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getClassesType1_Value(), this.getValueType1(), null, "value", null, 1, -1, ClassesType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(columnsetTypeEClass, ColumnsetType.class, "ColumnsetType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getColumnsetType_FrameworkKey(), this.getFrameworkKeyType1(), null, "frameworkKey", null, 1, 1, ColumnsetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getColumnsetType_Attributes(), this.getAttributesType(), null, "attributes", null, 1, 1, ColumnsetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(columnTypeEClass, ColumnType.class, "ColumnType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getColumnType_Decimals(), theXMLTypePackage.getNonNegativeInteger(), "decimals", null, 0, 1, ColumnType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType_Length(), theXMLTypePackage.getNonNegativeInteger(), "length", null, 1, 1, ColumnType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ColumnType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType_Type(), this.getTypeType(), "type", null, 1, 1, ColumnType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(columnType1EClass, ColumnType1.class, "ColumnType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getColumnType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getColumnType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getColumnType1_Values(), this.getValuesType(), null, "values", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getColumnType1_GetDataRequest(), this.getGetDataRequestType(), null, "getDataRequest", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Decimals(), theXMLTypePackage.getNonNegativeInteger(), "decimals", null, 0, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Length(), theXMLTypePackage.getNonNegativeInteger(), "length", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Purpose(), this.getPurposeType(), "purpose", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType1_Type(), this.getTypeType(), "type", null, 1, 1, ColumnType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(columnType2EClass, ColumnType2.class, "ColumnType2", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getColumnType2_Decimals(), theXMLTypePackage.getNonNegativeInteger(), "decimals", null, 0, 1, ColumnType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType2_Length(), theXMLTypePackage.getNonNegativeInteger(), "length", null, 1, 1, ColumnType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType2_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ColumnType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getColumnType2_Type(), this.getTypeType(), "type", null, 1, 1, ColumnType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(countTypeEClass, CountType.class, "CountType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getCountType_UOM(), this.getUOMType(), null, "uOM", null, 1, 1, CountType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getCountType_Uncertainty(), this.getUncertaintyType(), null, "uncertainty", null, 0, 1, CountType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getCountType_Exceptions(), this.getMeasureCountExceptions(), null, "exceptions", null, 0, 1, CountType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(dataDescriptionsTypeEClass, DataDescriptionsType.class, "DataDescriptionsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getDataDescriptionsType_Framework(), this.getFrameworkDatasetDescribeDataType(), null, "framework", null, 1, -1, DataDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDataDescriptionsType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, DataDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDataDescriptionsType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, DataDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDataDescriptionsType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, DataDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDataDescriptionsType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, DataDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(dataInputsTypeEClass, DataInputsType.class, "DataInputsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getDataInputsType_Framework(), this.getFrameworkDatasetDescribeDataType(), null, "framework", null, 1, 1, DataInputsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(datasetDescriptionsTypeEClass, DatasetDescriptionsType.class, "DatasetDescriptionsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getDatasetDescriptionsType_Framework(), this.getFrameworkType4(), null, "framework", null, 1, -1, DatasetDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetDescriptionsType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, DatasetDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetDescriptionsType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, DatasetDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetDescriptionsType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, DatasetDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetDescriptionsType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, DatasetDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(datasetTypeEClass, DatasetType.class, "DatasetType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDatasetType_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType_DescribeDataRequest(), this.getDescribeDataRequestType(), null, "describeDataRequest", null, 1, 1, DatasetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(datasetType1EClass, DatasetType1.class, "DatasetType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDatasetType1_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType1_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType1_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType1_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType1_Columnset(), this.getColumnsetType(), null, "columnset", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType1_Rowset(), this.getRowsetType1(), null, "rowset", null, 1, 1, DatasetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(datasetType2EClass, DatasetType2.class, "DatasetType2", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDatasetType2_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType2_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType2_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType2_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType2_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType2_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType2_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType2_DescribeDataRequest(), this.getDescribeDataRequestType(), null, "describeDataRequest", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType2_Columnset(), this.getColumnsetType(), null, "columnset", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType2_Rowset(), this.getRowsetType1(), null, "rowset", null, 1, 1, DatasetType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(datasetType3EClass, DatasetType3.class, "DatasetType3", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDatasetType3_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType3_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType3_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType3_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType3_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType3_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDatasetType3_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType3_DescribeDataRequest(), this.getDescribeDataRequestType(), null, "describeDataRequest", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDatasetType3_Columnset(), this.getColumnsetType(), null, "columnset", null, 1, 1, DatasetType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeDataRequestTypeEClass, DescribeDataRequestType.class, "DescribeDataRequestType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeDataRequestType_Href(), theXMLTypePackage.getAnyURI(), "href", null, 1, 1, DescribeDataRequestType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeDatasetsRequestTypeEClass, DescribeDatasetsRequestType.class, "DescribeDatasetsRequestType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeDatasetsRequestType_Href(), theXMLTypePackage.getAnyURI(), "href", null, 1, 1, DescribeDatasetsRequestType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeDatasetsTypeEClass, DescribeDatasetsType.class, "DescribeDatasetsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeDatasetsType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 0, 1, DescribeDatasetsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeDatasetsType_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 0, 1, DescribeDatasetsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeDataTypeEClass, DescribeDataType.class, "DescribeDataType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeDataType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 0, 1, DescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeDataType_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 0, 1, DescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeDataType_Attributes(), theXMLTypePackage.getString(), "attributes", null, 0, 1, DescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeFrameworkKeyTypeEClass, DescribeFrameworkKeyType.class, "DescribeFrameworkKeyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeFrameworkKeyType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeFrameworkKeyType_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeFrameworkKeyType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDescribeFrameworkKeyType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDescribeFrameworkKeyType_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeFrameworkKeyType_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getDescribeFrameworkKeyType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDescribeFrameworkKeyType_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDescribeFrameworkKeyType_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDescribeFrameworkKeyType_Rowset(), this.getRowsetType(), null, "rowset", null, 1, 1, DescribeFrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeFrameworksTypeEClass, DescribeFrameworksType.class, "DescribeFrameworksType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeFrameworksType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 0, 1, DescribeFrameworksType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(describeKeyTypeEClass, DescribeKeyType.class, "DescribeKeyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDescribeKeyType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, DescribeKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(documentRootEClass, DocumentRoot.class, "DocumentRoot", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getDocumentRoot_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, null, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_XMLNSPrefixMap(), ecorePackage.getEStringToStringMapEntry(), null, "xMLNSPrefixMap", null, 0, -1, null, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_XSISchemaLocation(), ecorePackage.getEStringToStringMapEntry(), null, "xSISchemaLocation", null, 0, -1, null, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Abstract(), this.getAbstractType(), null, "abstract", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_AttributeLimit(), theXMLTypePackage.getPositiveInteger(), "attributeLimit", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Attributes(), theXMLTypePackage.getString(), "attributes", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Capabilities(), this.getTjsCapabilitiesType(), null, "capabilities", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Columnset(), this.getColumnsetType(), null, "columnset", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Count(), this.getCountType(), null, "count", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_DataClass(), this.getDataClassType(), "dataClass", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DataDescriptions(), this.getDataDescriptionsType(), null, "dataDescriptions", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Dataset(), this.getDatasetType1(), null, "dataset", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DatasetDescriptions(), this.getDatasetDescriptionsType(), null, "datasetDescriptions", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeData(), this.getDescribeDataType(), null, "describeData", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeDataRequest(), this.getDescribeDataRequestType(), null, "describeDataRequest", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeDatasets(), this.getDescribeDatasetsType(), null, "describeDatasets", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeDatasetsRequest(), this.getDescribeDatasetsRequestType(), null, "describeDatasetsRequest", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeFrameworks(), this.getDescribeFrameworksType(), null, "describeFrameworks", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeJoinAbilities(), this.getRequestBaseType(), null, "describeJoinAbilities", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_DescribeKey(), this.getDescribeKeyType(), null, "describeKey", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Framework(), this.getFrameworkType1(), null, "framework", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_FrameworkDescriptions(), this.getFrameworkDescriptionsType(), null, "frameworkDescriptions", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_FrameworkKeyDescription(), this.getFrameworkKeyDescriptionType(), null, "frameworkKeyDescription", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_GDAS(), this.getGDASType(), null, "gDAS", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_GetCapabilities(), this.getGetCapabilitiesType(), null, "getCapabilities", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_GetData(), this.getGetDataType(), null, "getData", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_GetDataRequest(), this.getGetDataRequestType(), null, "getDataRequest", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Identifier(), theXMLTypePackage.getString(), "identifier", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_JoinAbilities(), this.getJoinAbilitiesType(), null, "joinAbilities", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_JoinData(), this.getJoinDataType(), null, "joinData", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_JoinDataResponse(), this.getJoinDataResponseType(), null, "joinDataResponse", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_K(), this.getKType(), null, "k", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_LinkageKeys(), theXMLTypePackage.getString(), "linkageKeys", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Measure(), this.getMeasureType(), null, "measure", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Mechanism(), this.getMechanismType(), null, "mechanism", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Nominal(), this.getNominalType(), null, "nominal", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Ordinal(), this.getOrdinalType(), null, "ordinal", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Organization(), theXMLTypePackage.getString(), "organization", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Rowset(), this.getRowsetType1(), null, "rowset", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_SpatialFrameworks(), this.getSpatialFrameworksType(), null, "spatialFrameworks", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Styling(), this.getStylingType(), null, "styling", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Title(), theXMLTypePackage.getString(), "title", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Uncertainty(), this.getUncertaintyType(), null, "uncertainty", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_UOM(), this.getUOMType(), null, "uOM", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEReference(getDocumentRoot_Values(), this.getValuesType(), null, "values", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
        initEAttribute(getDocumentRoot_Version(), theXMLTypePackage.getString(), "version", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);

        initEClass(exceptionReportTypeEClass, ExceptionReportType.class, "ExceptionReportType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getExceptionReportType_Exception(), theOws11Package.getExceptionType(), null, "exception", null, 1, 1, ExceptionReportType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(failedTypeEClass, FailedType.class, "FailedType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

        initEClass(frameworkDatasetDescribeDataTypeEClass, FrameworkDatasetDescribeDataType.class, "FrameworkDatasetDescribeDataType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkDatasetDescribeDataType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDatasetDescribeDataType_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDatasetDescribeDataType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDatasetDescribeDataType_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDatasetDescribeDataType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_DescribeDatasetsRequest(), this.getDescribeDatasetsRequestType(), null, "describeDatasetsRequest", null, 1, 1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkDatasetDescribeDataType_Dataset(), this.getDatasetType3(), null, "dataset", null, 1, -1, FrameworkDatasetDescribeDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkDescriptionsTypeEClass, FrameworkDescriptionsType.class, "FrameworkDescriptionsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getFrameworkDescriptionsType_Framework(), this.getFrameworkType2(), null, "framework", null, 1, -1, FrameworkDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDescriptionsType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, FrameworkDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDescriptionsType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, FrameworkDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDescriptionsType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, FrameworkDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkDescriptionsType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, FrameworkDescriptionsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkKeyDescriptionTypeEClass, FrameworkKeyDescriptionType.class, "FrameworkKeyDescriptionType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getFrameworkKeyDescriptionType_Framework(), this.getDescribeFrameworkKeyType(), null, "framework", null, 1, 1, FrameworkKeyDescriptionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyDescriptionType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, FrameworkKeyDescriptionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyDescriptionType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, FrameworkKeyDescriptionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyDescriptionType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, FrameworkKeyDescriptionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyDescriptionType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, FrameworkKeyDescriptionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkKeyTypeEClass, FrameworkKeyType.class, "FrameworkKeyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getFrameworkKeyType_Column(), this.getColumnType(), null, "column", null, 1, -1, FrameworkKeyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkKeyType1EClass, FrameworkKeyType1.class, "FrameworkKeyType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getFrameworkKeyType1_Column(), this.getColumnType2(), null, "column", null, 1, -1, FrameworkKeyType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyType1_Complete(), theXMLTypePackage.getAnySimpleType(), "complete", null, 1, 1, FrameworkKeyType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkKeyType1_Relationship(), theXMLTypePackage.getAnySimpleType(), "relationship", null, 1, 1, FrameworkKeyType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkTypeEClass, FrameworkType.class, "FrameworkType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkType1EClass, FrameworkType1.class, "FrameworkType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkType1_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType1_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType1_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType1_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType1_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType1_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType1_Dataset(), this.getDatasetType1(), null, "dataset", null, 1, 1, FrameworkType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkType2EClass, FrameworkType2.class, "FrameworkType2", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkType2_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType2_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType2_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType2_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType2_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType2_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType2_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType2_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType2_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType2_DescribeDatasetsRequest(), this.getDescribeDatasetsRequestType(), null, "describeDatasetsRequest", null, 1, 1, FrameworkType2.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkType3EClass, FrameworkType3.class, "FrameworkType3", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkType3_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType3_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType3_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType3_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType3_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_DescribeDatasetsRequest(), this.getDescribeDatasetsRequestType(), null, "describeDatasetsRequest", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType3_Dataset(), this.getDatasetType2(), null, "dataset", null, 1, 1, FrameworkType3.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(frameworkType4EClass, FrameworkType4.class, "FrameworkType4", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getFrameworkType4_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType4_Organization(), theXMLTypePackage.getString(), "organization", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType4_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_ReferenceDate(), this.getReferenceDateType(), null, "referenceDate", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType4_Version(), theXMLTypePackage.getString(), "version", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getFrameworkType4_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_FrameworkKey(), this.getFrameworkKeyType(), null, "frameworkKey", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_BoundingCoordinates(), this.getBoundingCoordinatesType(), null, "boundingCoordinates", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_DescribeDatasetsRequest(), this.getDescribeDatasetsRequestType(), null, "describeDatasetsRequest", null, 1, 1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getFrameworkType4_Dataset(), this.getDatasetType(), null, "dataset", null, 1, -1, FrameworkType4.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(gdasTypeEClass, GDASType.class, "GDASType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getGDASType_Framework(), this.getFrameworkType3(), null, "framework", null, 1, 1, GDASType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGDASType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, GDASType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGDASType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, GDASType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGDASType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, GDASType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGDASType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, GDASType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(getCapabilitiesTypeEClass, GetCapabilitiesType.class, "GetCapabilitiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getGetCapabilitiesType_AcceptVersions(), this.getAcceptVersionsType(), null, "acceptVersions", null, 0, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getGetCapabilitiesType_Sections(), theOws11Package.getSectionsType(), null, "sections", null, 0, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getGetCapabilitiesType_AcceptFormats(), theOws11Package.getAcceptFormatsType(), null, "acceptFormats", null, 0, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetCapabilitiesType_Language(), theXMLTypePackage.getAnySimpleType(), "language", null, 0, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetCapabilitiesType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetCapabilitiesType_UpdateSequence(), theOws11Package.getUpdateSequenceType(), "updateSequence", null, 0, 1, GetCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(getDataRequestTypeEClass, GetDataRequestType.class, "GetDataRequestType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getGetDataRequestType_Href(), theXMLTypePackage.getAnyURI(), "href", null, 1, 1, GetDataRequestType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(getDataTypeEClass, GetDataType.class, "GetDataType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getGetDataType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataType_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataType_Attributes(), theXMLTypePackage.getString(), "attributes", null, 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataType_LinkageKeys(), theXMLTypePackage.getString(), "linkageKeys", null, 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getGetDataType_FilterColumn(), ecorePackage.getEObject(), null, "filterColumn", null, 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getGetDataType_FilterValue(), ecorePackage.getEObject(), null, "filterValue", null, 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getGetDataType_XSL(), ecorePackage.getEObject(), null, "xSL", null, 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataType_Aid(), theXMLTypePackage.getBoolean(), "aid", "false", 0, 1, GetDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(getDataXMLTypeEClass, GetDataXMLType.class, "GetDataXMLType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getGetDataXMLType_FrameworkURI(), theXMLTypePackage.getString(), "frameworkURI", null, 1, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataXMLType_DatasetURI(), theXMLTypePackage.getString(), "datasetURI", null, 1, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataXMLType_Attributes(), theXMLTypePackage.getString(), "attributes", null, 0, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataXMLType_LinkageKeys(), theXMLTypePackage.getString(), "linkageKeys", null, 0, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataXMLType_GetDataHost(), theXMLTypePackage.getAnyURI(), "getDataHost", null, 0, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getGetDataXMLType_Language(), theXMLTypePackage.getString(), "language", null, 0, 1, GetDataXMLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(joinAbilitiesTypeEClass, JoinAbilitiesType.class, "JoinAbilitiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getJoinAbilitiesType_SpatialFrameworks(), this.getSpatialFrameworksType(), null, "spatialFrameworks", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_AttributeLimit(), theXMLTypePackage.getPositiveInteger(), "attributeLimit", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinAbilitiesType_OutputMechanisms(), this.getOutputMechanismsType(), null, "outputMechanisms", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinAbilitiesType_OutputStylings(), this.getOutputStylingsType1(), null, "outputStylings", null, 0, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinAbilitiesType_ClassificationSchemaURL(), ecorePackage.getEObject(), null, "classificationSchemaURL", null, 0, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_UpdateSupported(), theXMLTypePackage.getBoolean(), "updateSupported", null, 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinAbilitiesType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, JoinAbilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(joinDataResponseTypeEClass, JoinDataResponseType.class, "JoinDataResponseType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getJoinDataResponseType_Status(), this.getStatusType(), null, "status", null, 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinDataResponseType_DataInputs(), this.getDataInputsType(), null, "dataInputs", null, 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinDataResponseType_JoinedOutputs(), this.getJoinedOutputsType(), null, "joinedOutputs", null, 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinDataResponseType_Capabilities(), theXMLTypePackage.getString(), "capabilities", null, 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinDataResponseType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinDataResponseType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinDataResponseType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, JoinDataResponseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(joinDataTypeEClass, JoinDataType.class, "JoinDataType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getJoinDataType_AttributeData(), this.getAttributeDataType(), null, "attributeData", null, 1, 1, JoinDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinDataType_MapStyling(), this.getMapStylingType(), null, "mapStyling", null, 0, 1, JoinDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getJoinDataType_ClassificationURL(), ecorePackage.getEObject(), null, "classificationURL", null, 0, 1, JoinDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getJoinDataType_Update(), this.getUpdateType(), "update", null, 0, 1, JoinDataType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(joinedOutputsTypeEClass, JoinedOutputsType.class, "JoinedOutputsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getJoinedOutputsType_Output(), this.getOutputType(), null, "output", null, 1, -1, JoinedOutputsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(kTypeEClass, KType.class, "KType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getKType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, KType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getKType_Aid(), theXMLTypePackage.getAnySimpleType(), "aid", null, 0, 1, KType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(languagesTypeEClass, LanguagesType.class, "LanguagesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getLanguagesType_Language(), theXMLTypePackage.getLanguage(), "language", null, 1, -1, LanguagesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(mapStylingTypeEClass, MapStylingType.class, "MapStylingType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getMapStylingType_StylingIdentifier(), ecorePackage.getEObject(), null, "stylingIdentifier", null, 1, 1, MapStylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getMapStylingType_StylingURL(), theXMLTypePackage.getAnyURI(), "stylingURL", null, 1, 1, MapStylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(measureCountExceptionsEClass, MeasureCountExceptions.class, "MeasureCountExceptions", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getMeasureCountExceptions_Null(), this.getNullType(), null, "null", null, 1, -1, MeasureCountExceptions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(measureTypeEClass, MeasureType.class, "MeasureType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getMeasureType_UOM(), this.getUOMType(), null, "uOM", null, 1, 1, MeasureType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getMeasureType_Uncertainty(), this.getUncertaintyType(), null, "uncertainty", null, 0, 1, MeasureType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getMeasureType_Exceptions(), this.getMeasureCountExceptions(), null, "exceptions", null, 0, 1, MeasureType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(mechanismTypeEClass, MechanismType.class, "MechanismType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getMechanismType_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, MechanismType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getMechanismType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, MechanismType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getMechanismType_Abstract(), theXMLTypePackage.getString(), "abstract", null, 1, 1, MechanismType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getMechanismType_Reference(), theXMLTypePackage.getAnyURI(), "reference", null, 1, 1, MechanismType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(nominalOrdinalExceptionsEClass, NominalOrdinalExceptions.class, "NominalOrdinalExceptions", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getNominalOrdinalExceptions_Null(), this.getNullType1(), null, "null", null, 1, -1, NominalOrdinalExceptions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(nominalTypeEClass, NominalType.class, "NominalType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getNominalType_Classes(), this.getClassesType1(), null, "classes", null, 0, 1, NominalType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNominalType_Exceptions(), this.getNominalOrdinalExceptions(), null, "exceptions", null, 0, 1, NominalType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(nullTypeEClass, NullType.class, "NullType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getNullType_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, NullType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getNullType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, NullType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNullType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, NullType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getNullType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, NullType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(nullType1EClass, NullType1.class, "NullType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getNullType1_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, NullType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getNullType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, NullType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNullType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, NullType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getNullType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, NullType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getNullType1_Color(), theXMLTypePackage.getAnySimpleType(), "color", null, 0, 1, NullType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(ordinalTypeEClass, OrdinalType.class, "OrdinalType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getOrdinalType_Classes(), this.getClassesType(), null, "classes", null, 0, 1, OrdinalType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getOrdinalType_Exceptions(), this.getNominalOrdinalExceptions(), null, "exceptions", null, 0, 1, OrdinalType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(outputMechanismsTypeEClass, OutputMechanismsType.class, "OutputMechanismsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getOutputMechanismsType_Mechanism(), this.getMechanismType(), null, "mechanism", null, 1, -1, OutputMechanismsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(outputStylingsTypeEClass, OutputStylingsType.class, "OutputStylingsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getOutputStylingsType_Styling(), this.getStylingType(), null, "styling", null, 1, -1, OutputStylingsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(outputStylingsType1EClass, OutputStylingsType1.class, "OutputStylingsType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

        initEClass(outputTypeEClass, OutputType.class, "OutputType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getOutputType_Mechanism(), this.getMechanismType(), null, "mechanism", null, 1, 1, OutputType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getOutputType_Resource(), this.getResourceType(), null, "resource", null, 0, 1, OutputType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getOutputType_ExceptionReport(), this.getExceptionReportType(), null, "exceptionReport", null, 0, 1, OutputType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(parameterTypeEClass, ParameterType.class, "ParameterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getParameterType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, ParameterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getParameterType_Name(), theXMLTypePackage.getAnySimpleType(), "name", null, 1, 1, ParameterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(referenceDateTypeEClass, ReferenceDateType.class, "ReferenceDateType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getReferenceDateType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, ReferenceDateType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getReferenceDateType_StartDate(), theXMLTypePackage.getString(), "startDate", null, 0, 1, ReferenceDateType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(requestBaseTypeEClass, RequestBaseType.class, "RequestBaseType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getRequestBaseType_Language(), theXMLTypePackage.getString(), "language", null, 0, 1, RequestBaseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getRequestBaseType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, RequestBaseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getRequestBaseType_Version(), this.getVersionType2(), "version", null, 0, 1, RequestBaseType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(resourceTypeEClass, ResourceType.class, "ResourceType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getResourceType_URL(), ecorePackage.getEObject(), null, "uRL", null, 1, 1, ResourceType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getResourceType_Parameter(), this.getParameterType(), null, "parameter", null, 0, -1, ResourceType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(rowsetTypeEClass, RowsetType.class, "RowsetType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getRowsetType_Row(), this.getRowType(), null, "row", null, 1, -1, RowsetType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(rowsetType1EClass, RowsetType1.class, "RowsetType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getRowsetType1_Row(), this.getRowType1(), null, "row", null, 1, -1, RowsetType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(rowTypeEClass, RowType.class, "RowType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getRowType_K(), this.getKType(), null, "k", null, 1, -1, RowType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getRowType_Title(), theXMLTypePackage.getString(), "title", null, 0, 1, RowType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(rowType1EClass, RowType1.class, "RowType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getRowType1_K(), this.getKType(), null, "k", null, 1, -1, RowType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getRowType1_V(), this.getVType(), null, "v", null, 1, -1, RowType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(spatialFrameworksTypeEClass, SpatialFrameworksType.class, "SpatialFrameworksType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getSpatialFrameworksType_Framework(), this.getFrameworkType(), null, "framework", null, 1, -1, SpatialFrameworksType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(statusTypeEClass, StatusType.class, "StatusType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getStatusType_Accepted(), ecorePackage.getEObject(), null, "accepted", null, 0, 1, StatusType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getStatusType_Completed(), ecorePackage.getEObject(), null, "completed", null, 0, 1, StatusType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getStatusType_Failed(), this.getFailedType(), null, "failed", null, 0, 1, StatusType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStatusType_CreationTime(), theXMLTypePackage.getAnySimpleType(), "creationTime", null, 1, 1, StatusType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStatusType_Href(), theXMLTypePackage.getAnyURI(), "href", null, 1, 1, StatusType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(stylingTypeEClass, StylingType.class, "StylingType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getStylingType_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, StylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStylingType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, StylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStylingType_Abstract(), theXMLTypePackage.getString(), "abstract", null, 1, 1, StylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStylingType_Reference(), theXMLTypePackage.getAnyURI(), "reference", null, 1, 1, StylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getStylingType_Schema(), theXMLTypePackage.getAnyURI(), "schema", null, 0, 1, StylingType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(tjsCapabilitiesTypeEClass, TjsCapabilitiesType.class, "TjsCapabilitiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getTjsCapabilitiesType_ServiceIdentification(), theOws11Package.getServiceIdentificationType(), null, "serviceIdentification", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getTjsCapabilitiesType_ServiceProvider(), theOws11Package.getServiceProviderType(), null, "serviceProvider", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getTjsCapabilitiesType_OperationsMetadata(), theOws11Package.getOperationsMetadataType(), null, "operationsMetadata", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getTjsCapabilitiesType_Languages(), this.getLanguagesType(), null, "languages", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getTjsCapabilitiesType_WSDL(), this.getWSDLType(), null, "wSDL", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getTjsCapabilitiesType_Lang(), theXMLTypePackage.getAnySimpleType(), "lang", null, 1, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getTjsCapabilitiesType_Service(), theXMLTypePackage.getAnySimpleType(), "service", "TJS", 1, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getTjsCapabilitiesType_UpdateSequence(), theOws11Package.getUpdateSequenceType(), "updateSequence", null, 0, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getTjsCapabilitiesType_Version(), theXMLTypePackage.getAnySimpleType(), "version", "1.0", 1, 1, TjsCapabilitiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(uncertaintyTypeEClass, UncertaintyType.class, "UncertaintyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getUncertaintyType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, UncertaintyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getUncertaintyType_Gaussian(), this.getGaussianType(), "gaussian", null, 1, 1, UncertaintyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(uomTypeEClass, UOMType.class, "UOMType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getUOMType_ShortForm(), this.getShortForm(), null, "shortForm", null, 1, 1, UOMType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getUOMType_LongForm(), this.getLongForm(), null, "longForm", null, 1, 1, UOMType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
//        initEReference(getUOMType_ShortForm(), ecorePackage.getEObject(), null, "shortForm", null, 1, 1, UOMType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
//        initEReference(getUOMType_LongForm(), ecorePackage.getEObject(), null, "longForm", null, 1, 1, UOMType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getUOMType_Reference(), theXMLTypePackage.getAnyURI(), "reference", null, 0, 1, UOMType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(valuesTypeEClass, ValuesType.class, "ValuesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getValuesType_Nominal(), this.getNominalType(), null, "nominal", null, 0, 1, ValuesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getValuesType_Ordinal(), this.getOrdinalType(), null, "ordinal", null, 0, 1, ValuesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getValuesType_Count(), this.getCountType(), null, "count", null, 0, 1, ValuesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getValuesType_Measure(), this.getMeasureType(), null, "measure", null, 0, 1, ValuesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(valueTypeEClass, ValueType.class, "ValueType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getValueType_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getValueType_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType_Color(), theXMLTypePackage.getAnySimpleType(), "color", null, 0, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType_Rank(), theXMLTypePackage.getNonNegativeInteger(), "rank", null, 1, 1, ValueType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(valueType1EClass, ValueType1.class, "ValueType1", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getValueType1_Identifier(), theXMLTypePackage.getString(), "identifier", null, 1, 1, ValueType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType1_Title(), theXMLTypePackage.getString(), "title", null, 1, 1, ValueType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getValueType1_Abstract(), this.getAbstractType(), null, "abstract", null, 1, 1, ValueType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType1_Documentation(), theXMLTypePackage.getAnyURI(), "documentation", null, 0, 1, ValueType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getValueType1_Color(), theXMLTypePackage.getAnySimpleType(), "color", null, 0, 1, ValueType1.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(vTypeEClass, VType.class, "VType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getVType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, VType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getVType_Aid(), theXMLTypePackage.getString(), "aid", null, 0, 1, VType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getVType_Null(), theXMLTypePackage.getBoolean(), "null", "false", 0, 1, VType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        initEClass(wsdlTypeEClass, WSDLType.class, "WSDLType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getWSDLType_Href(), theXMLTypePackage.getAnySimpleType(), "href", null, 1, 1, WSDLType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

        // Initialize enums and add enum literals
        initEEnum(dataClassTypeEEnum, DataClassType.class, "DataClassType");
        addEEnumLiteral(dataClassTypeEEnum, DataClassType.NOMINAL_LITERAL);
        addEEnumLiteral(dataClassTypeEEnum, DataClassType.ORDINAL_LITERAL);
        addEEnumLiteral(dataClassTypeEEnum, DataClassType.MEASURE_LITERAL);
        addEEnumLiteral(dataClassTypeEEnum, DataClassType.COUNT_LITERAL);

        initEEnum(describeDatasetsValueTypeEEnum, DescribeDatasetsValueType.class, "DescribeDatasetsValueType");
        addEEnumLiteral(describeDatasetsValueTypeEEnum, DescribeDatasetsValueType.DESCRIBE_DATASETS_LITERAL);

        initEEnum(describeDataValueTypeEEnum, DescribeDataValueType.class, "DescribeDataValueType");
        addEEnumLiteral(describeDataValueTypeEEnum, DescribeDataValueType.DESCRIBE_DATA_LITERAL);

        initEEnum(describeFrameworksValueTypeEEnum, DescribeFrameworksValueType.class, "DescribeFrameworksValueType");
        addEEnumLiteral(describeFrameworksValueTypeEEnum, DescribeFrameworksValueType.DESCRIBE_FRAMEWORKS_LITERAL);

        initEEnum(describeJoinAbilitiesValueTypeEEnum, DescribeJoinAbilitiesValueType.class, "DescribeJoinAbilitiesValueType");
        addEEnumLiteral(describeJoinAbilitiesValueTypeEEnum, DescribeJoinAbilitiesValueType.DESCRIBE_JOIN_ABILITIES_LITERAL);

        initEEnum(describeKeyValueTypeEEnum, DescribeKeyValueType.class, "DescribeKeyValueType");
        addEEnumLiteral(describeKeyValueTypeEEnum, DescribeKeyValueType.DESCRIBE_KEY_LITERAL);

        initEEnum(gaussianTypeEEnum, GaussianType.class, "GaussianType");
        addEEnumLiteral(gaussianTypeEEnum, GaussianType.TRUE_LITERAL);
        addEEnumLiteral(gaussianTypeEEnum, GaussianType.FALSE_LITERAL);
        addEEnumLiteral(gaussianTypeEEnum, GaussianType.UNKNOWN_LITERAL);

        initEEnum(getCapabilitiesValueTypeEEnum, GetCapabilitiesValueType.class, "GetCapabilitiesValueType");
        addEEnumLiteral(getCapabilitiesValueTypeEEnum, GetCapabilitiesValueType.GET_CAPABILITIES_LITERAL);

        initEEnum(getDataValueTypeEEnum, GetDataValueType.class, "GetDataValueType");
        addEEnumLiteral(getDataValueTypeEEnum, GetDataValueType.GET_DATA_LITERAL);

        initEEnum(joinDataValueTypeEEnum, JoinDataValueType.class, "JoinDataValueType");
        addEEnumLiteral(joinDataValueTypeEEnum, JoinDataValueType.JOIN_DATA_LITERAL);

        initEEnum(purposeTypeEEnum, PurposeType.class, "PurposeType");
        addEEnumLiteral(purposeTypeEEnum, PurposeType.SPATIAL_COMPONENT_IDENTIFIER_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.SPATIAL_COMPONENT_PROPORTION_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.SPATIAL_COMPONENT_PERCENTAGE_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.TEMPORAL_IDENTIFIER_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.TEMPORAL_VALUE_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.VERTICAL_IDENTIFIER_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.VERTICAL_VALUE_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.OTHER_SPATIAL_IDENTIFIER_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.NON_SPATIAL_IDENTIFIER_LITERAL);
        addEEnumLiteral(purposeTypeEEnum, PurposeType.ATTRIBUTE_LITERAL);

        initEEnum(requestServiceTypeEEnum, RequestServiceType.class, "RequestServiceType");
        addEEnumLiteral(requestServiceTypeEEnum, RequestServiceType.TJS_LITERAL);

        initEEnum(typeTypeEEnum, TypeType.class, "TypeType");
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_BOOLEAN_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_INTEGER_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_DECIMAL_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_FLOAT_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_DOUBLE_LITERAL);
        addEEnumLiteral(typeTypeEEnum, TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_DATETIME_LITERAL);

        initEEnum(updateTypeEEnum, UpdateType.class, "UpdateType");
        addEEnumLiteral(updateTypeEEnum, UpdateType.TRUE_LITERAL);
        addEEnumLiteral(updateTypeEEnum, UpdateType.FALSE_LITERAL);

        initEEnum(versionTypeEEnum, VersionType.class, "VersionType");
        addEEnumLiteral(versionTypeEEnum, VersionType._10_LITERAL);

        initEEnum(versionType1EEnum, VersionType1.class, "VersionType1");
        addEEnumLiteral(versionType1EEnum, VersionType1._10_LITERAL);

        initEEnum(versionType2EEnum, VersionType2.class, "VersionType2");
        addEEnumLiteral(versionType2EEnum, VersionType2._1_LITERAL);
        addEEnumLiteral(versionType2EEnum, VersionType2._10_LITERAL);
        addEEnumLiteral(versionType2EEnum, VersionType2._100_LITERAL);

        // Initialize data types
        initEDataType(acceptLanguagesTypeEDataType, String.class, "AcceptLanguagesType", IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);
        initEDataType(dataClassTypeObjectEDataType, DataClassType.class, "DataClassTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(describeDatasetsValueTypeObjectEDataType, DescribeDatasetsValueType.class, "DescribeDatasetsValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(describeDataValueTypeObjectEDataType, DescribeDataValueType.class, "DescribeDataValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(describeFrameworksValueTypeObjectEDataType, DescribeFrameworksValueType.class, "DescribeFrameworksValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(describeJoinAbilitiesValueTypeObjectEDataType, DescribeJoinAbilitiesValueType.class, "DescribeJoinAbilitiesValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(describeKeyValueTypeObjectEDataType, DescribeKeyValueType.class, "DescribeKeyValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(gaussianTypeObjectEDataType, GaussianType.class, "GaussianTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(getCapabilitiesValueTypeObjectEDataType, GetCapabilitiesValueType.class, "GetCapabilitiesValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(getDataValueTypeObjectEDataType, GetDataValueType.class, "GetDataValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(joinDataValueTypeObjectEDataType, JoinDataValueType.class, "JoinDataValueTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(purposeTypeObjectEDataType, PurposeType.class, "PurposeTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(requestServiceTypeObjectEDataType, RequestServiceType.class, "RequestServiceTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(sectionsTypeEDataType, String.class, "SectionsType", IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);
        initEDataType(typeTypeObjectEDataType, TypeType.class, "TypeTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(updateTypeObjectEDataType, UpdateType.class, "UpdateTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(versionTypeObjectEDataType, VersionType.class, "VersionTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(versionTypeObject1EDataType, VersionType1.class, "VersionTypeObject1", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
        initEDataType(versionTypeObject2EDataType, VersionType2.class, "VersionTypeObject2", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);

        // Create resource
        createResource(eNS_URI);

        // Create annotations
        // null
        createNullAnnotations();
        // urn:opengis:specification:gml:schema-xlinks:v3.0c2
        createUrnopengisspecificationgmlschemaxlinksv3Annotations();
        // http://www.w3.org/XML/1998/namespace
        createNamespaceAnnotations();
        // http:///org/eclipse/emf/ecore/util/ExtendedMetaData
        createExtendedMetaDataAnnotations();
    }

    /**
     * Initializes the annotations for <b>null</b>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected void createNullAnnotations() {
        String source = null;
        addAnnotation
                (this,
                        source,
                        new String[]{
                                            "appinfo", "$Id: tjsGetData_response.xsd 2010-12-23 $\r\nowsAll.xsd\r\nowsGetResourceByID.xsd\r\nowsExceptionReport.xsd\r\nowsDomainType.xsd\r\nowsContents.xsd\r\nowsInputOutputData.xsd\r\nowsManifest.xsd\r\nowsDataIdentification.xsd\r\nowsCommon.xsd\r\nowsGetCapabilities.xsd\r\nowsServiceIdentification.xsd\r\nowsServiceProvider.xsd\r\nowsOperationsMetadata.xsd\r\nows19115subset.xsd"
                        });
    }

    /**
     * Initializes the annotations for <b>urn:opengis:specification:gml:schema-xlinks:v3.0c2</b>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected void createUrnopengisspecificationgmlschemaxlinksv3Annotations() {
        String source = "urn:opengis:specification:gml:schema-xlinks:v3.0c2";
        addAnnotation
                (this,
                        source,
                        new String[]{
                                            "appinfo", "xlinks.xsd v3.0b2 2001-07"
                        });
    }

    /**
     * Initializes the annotations for <b>http://www.w3.org/XML/1998/namespace</b>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected void createNamespaceAnnotations() {
        String source = "http://www.w3.org/XML/1998/namespace";
        addAnnotation
                (this,
                        source,
                        new String[]{
                                            "lang", "en"
                        });
    }

    /**
     * Initializes the annotations for <b>http:///org/eclipse/emf/ecore/util/ExtendedMetaData</b>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected void createExtendedMetaDataAnnotations() {
        String source = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";
        addAnnotation
                (abstractTypeEClass,
                        source,
                        new String[]{
                                            "name", "AbstractType",
                                            "kind", "mixed"
                        });
        addAnnotation
                (acceptLanguagesTypeEDataType,
                        source,
                        new String[]{
                                            "name", "AcceptLanguagesType",
                                            "baseType", "http://www.eclipse.org/emf/2003/XMLType#string"
                        });
        addAnnotation
                (acceptVersionsTypeEClass,
                        source,
                        new String[]{
                                            "name", "AcceptVersions_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getAcceptVersionsType_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (attributeDataTypeEClass,
                        source,
                        new String[]{
                                            "name", "AttributeData_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getAttributeDataType_GetDataURL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetDataURL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getAttributeDataType_GetDataXML(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetDataXML",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (attributesTypeEClass,
                        source,
                        new String[]{
                                            "name", "Attributes_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getAttributesType_Column(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Column",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (boundingCoordinatesTypeEClass,
                        source,
                        new String[]{
                                            "name", "BoundingCoordinates_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getBoundingCoordinatesType_North(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "North",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getBoundingCoordinatesType_South(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "South",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getBoundingCoordinatesType_East(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "East",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getBoundingCoordinatesType_West(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "West",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (classesTypeEClass,
                        source,
                        new String[]{
                                            "name", "Classes_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getClassesType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType_Value(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Value",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (classesType1EClass,
                        source,
                        new String[]{
                                            "name", "Classes_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getClassesType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getClassesType1_Value(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Value",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (columnsetTypeEClass,
                        source,
                        new String[]{
                                            "name", "Columnset_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getColumnsetType_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnsetType_Attributes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Attributes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (columnTypeEClass,
                        source,
                        new String[]{
                                            "name", "Column_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getColumnType_Decimals(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "decimals"
                        });
        addAnnotation
                (getColumnType_Length(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "length"
                        });
        addAnnotation
                (getColumnType_Name(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "name"
                        });
        addAnnotation
                (getColumnType_Type(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "type"
                        });
        addAnnotation
                (columnType1EClass,
                        source,
                        new String[]{
                                            "name", "Column_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getColumnType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnType1_Values(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Values",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnType1_GetDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getColumnType1_Decimals(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "decimals"
                        });
        addAnnotation
                (getColumnType1_Length(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "length"
                        });
        addAnnotation
                (getColumnType1_Name(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "name"
                        });
        addAnnotation
                (getColumnType1_Purpose(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "purpose"
                        });
        addAnnotation
                (getColumnType1_Type(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "type"
                        });
        addAnnotation
                (columnType2EClass,
                        source,
                        new String[]{
                                            "name", "Column_._2_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getColumnType2_Decimals(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "decimals"
                        });
        addAnnotation
                (getColumnType2_Length(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "length"
                        });
        addAnnotation
                (getColumnType2_Name(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "name"
                        });
        addAnnotation
                (getColumnType2_Type(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "type"
                        });
        addAnnotation
                (countTypeEClass,
                        source,
                        new String[]{
                                            "name", "Count_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getCountType_UOM(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "UOM",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getCountType_Uncertainty(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Uncertainty",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getCountType_Exceptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Exceptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (dataClassTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DataClass_._type"
                        });
        addAnnotation
                (dataClassTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DataClass_._type:Object",
                                            "baseType", "DataClass_._type"
                        });
        addAnnotation
                (dataDescriptionsTypeEClass,
                        source,
                        new String[]{
                                            "name", "DataDescriptions_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDataDescriptionsType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDataDescriptionsType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getDataDescriptionsType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getDataDescriptionsType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getDataDescriptionsType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (dataInputsTypeEClass,
                        source,
                        new String[]{
                                            "name", "DataInputs_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDataInputsType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (datasetDescriptionsTypeEClass,
                        source,
                        new String[]{
                                            "name", "DatasetDescriptions_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDatasetDescriptionsType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetDescriptionsType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getDatasetDescriptionsType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getDatasetDescriptionsType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getDatasetDescriptionsType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (datasetTypeEClass,
                        source,
                        new String[]{
                                            "name", "Dataset_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDatasetType_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType_DescribeDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (datasetType1EClass,
                        source,
                        new String[]{
                                            "name", "Dataset_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDatasetType1_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Columnset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Columnset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType1_Rowset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Rowset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (datasetType2EClass,
                        source,
                        new String[]{
                                            "name", "Dataset_._2_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDatasetType2_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_DescribeDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Columnset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Columnset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType2_Rowset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Rowset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (datasetType3EClass,
                        source,
                        new String[]{
                                            "name", "Dataset_._3_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDatasetType3_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_DescribeDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDatasetType3_Columnset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Columnset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeDataRequestTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeDataRequest_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getDescribeDataRequestType_Href(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "href",
                                            "namespace", "http://www.w3.org/1999/xlink"
                        });
        addAnnotation
                (describeDatasetsRequestTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeDatasetsRequest_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getDescribeDatasetsRequestType_Href(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "href",
                                            "namespace", "http://www.w3.org/1999/xlink"
                        });
        addAnnotation
                (describeDatasetsTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeDatasets_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDescribeDatasetsType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeDatasetsType_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeDatasetsValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DescribeDatasetsValueType"
                        });
        addAnnotation
                (describeDatasetsValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DescribeDatasetsValueType:Object",
                                            "baseType", "DescribeDatasetsValueType"
                        });
        addAnnotation
                (describeDataTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeData_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDescribeDataType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeDataType_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeDataType_Attributes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Attributes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeDataValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DescribeDataValueType"
                        });
        addAnnotation
                (describeDataValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DescribeDataValueType:Object",
                                            "baseType", "DescribeDataValueType"
                        });
        addAnnotation
                (describeFrameworkKeyTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeFrameworkKeyType",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDescribeFrameworkKeyType_Rowset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Rowset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeFrameworksTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeFrameworks_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDescribeFrameworksType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeFrameworksValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DescribeFrameworksValueType"
                        });
        addAnnotation
                (describeFrameworksValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DescribeFrameworksValueType:Object",
                                            "baseType", "DescribeFrameworksValueType"
                        });
        addAnnotation
                (describeJoinAbilitiesValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DescribeJoinAbilitiesValueType"
                        });
        addAnnotation
                (describeJoinAbilitiesValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DescribeJoinAbilitiesValueType:Object",
                                            "baseType", "DescribeJoinAbilitiesValueType"
                        });
        addAnnotation
                (describeKeyTypeEClass,
                        source,
                        new String[]{
                                            "name", "DescribeKey_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getDescribeKeyType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (describeKeyValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "DescribeKeyValueType"
                        });
        addAnnotation
                (describeKeyValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "DescribeKeyValueType:Object",
                                            "baseType", "DescribeKeyValueType"
                        });
        addAnnotation
                (documentRootEClass,
                        source,
                        new String[]{
                                            "name", "",
                                            "kind", "mixed"
                        });
        addAnnotation
                (getDocumentRoot_Mixed(),
                        source,
                        new String[]{
                                            "kind", "elementWildcard",
                                            "name", ":mixed"
                        });
        addAnnotation
                (getDocumentRoot_XMLNSPrefixMap(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "xmlns:prefix"
                        });
        addAnnotation
                (getDocumentRoot_XSISchemaLocation(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "xsi:schemaLocation"
                        });
        addAnnotation
                (getDocumentRoot_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_AttributeLimit(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "AttributeLimit",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Attributes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Attributes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Capabilities",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Columnset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Columnset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Count(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Count",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DataClass(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DataClass",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DataDescriptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DataDescriptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Dataset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Dataset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DatasetDescriptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetDescriptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeData(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeData",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeDatasets(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasets",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeDatasetsRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasetsRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeFrameworks(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeFrameworks",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeJoinAbilities(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeJoinAbilities",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_DescribeKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_FrameworkDescriptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkDescriptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_FrameworkKeyDescription(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKeyDescription",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_GDAS(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GDAS",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_GetCapabilities(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetCapabilities",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_GetData(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetData",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_GetDataRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "GetDataRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_JoinAbilities(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "JoinAbilities",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_JoinData(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "JoinData",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_JoinDataResponse(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "JoinDataResponse",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_K(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "K",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_LinkageKeys(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "LinkageKeys",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Measure(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Measure",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Mechanism(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Mechanism",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Nominal(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Nominal",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Ordinal(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Ordinal",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Rowset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Rowset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_SpatialFrameworks(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "SpatialFrameworks",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Styling(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Styling",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Uncertainty(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Uncertainty",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_UOM(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "UOM",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Values(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Values",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getDocumentRoot_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (exceptionReportTypeEClass,
                        source,
                        new String[]{
                                            "name", "ExceptionReport_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getExceptionReportType_Exception(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Exception",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (failedTypeEClass,
                        source,
                        new String[]{
                                            "name", "Failed_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (frameworkDatasetDescribeDataTypeEClass,
                        source,
                        new String[]{
                                            "name", "FrameworkDatasetDescribeDataType",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_DescribeDatasetsRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasetsRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDatasetDescribeDataType_Dataset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Dataset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkDescriptionsTypeEClass,
                        source,
                        new String[]{
                                            "name", "FrameworkDescriptions_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkDescriptionsType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkDescriptionsType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getFrameworkDescriptionsType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getFrameworkDescriptionsType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getFrameworkDescriptionsType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (frameworkKeyDescriptionTypeEClass,
                        source,
                        new String[]{
                                            "name", "FrameworkKeyDescription_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkKeyDescriptionType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkKeyDescriptionType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getFrameworkKeyDescriptionType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getFrameworkKeyDescriptionType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getFrameworkKeyDescriptionType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (frameworkKeyTypeEClass,
                        source,
                        new String[]{
                                            "name", "FrameworkKey_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkKeyType_Column(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Column",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkKeyType1EClass,
                        source,
                        new String[]{
                                            "name", "FrameworkKey_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkKeyType1_Column(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Column",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkKeyType1_Complete(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "complete"
                        });
        addAnnotation
                (getFrameworkKeyType1_Relationship(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "relationship"
                        });
        addAnnotation
                (frameworkTypeEClass,
                        source,
                        new String[]{
                                            "name", "Framework_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkType1EClass,
                        source,
                        new String[]{
                                            "name", "Framework_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkType1_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType1_Dataset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Dataset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkType2EClass,
                        source,
                        new String[]{
                                            "name", "Framework_._2_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkType2_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType2_DescribeDatasetsRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasetsRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkType3EClass,
                        source,
                        new String[]{
                                            "name", "Framework_._3_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkType3_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_DescribeDatasetsRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasetsRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType3_Dataset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Dataset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (frameworkType4EClass,
                        source,
                        new String[]{
                                            "name", "Framework_._4_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getFrameworkType4_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Organization(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Organization",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_ReferenceDate(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ReferenceDate",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Version(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Version",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_FrameworkKey(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkKey",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_BoundingCoordinates(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "BoundingCoordinates",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_DescribeDatasetsRequest(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DescribeDatasetsRequest",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getFrameworkType4_Dataset(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Dataset",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (gaussianTypeEEnum,
                        source,
                        new String[]{
                                            "name", "gaussian_._type"
                        });
        addAnnotation
                (gaussianTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "gaussian_._type:Object",
                                            "baseType", "gaussian_._type"
                        });
        addAnnotation
                (gdasTypeEClass,
                        source,
                        new String[]{
                                            "name", "GDAS_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getGDASType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGDASType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getGDASType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getGDASType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getGDASType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (getCapabilitiesTypeEClass,
                        source,
                        new String[]{
                                            "name", "GetCapabilities_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getGetCapabilitiesType_AcceptVersions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "AcceptVersions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetCapabilitiesType_Sections(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Sections",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetCapabilitiesType_AcceptFormats(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "AcceptFormats",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetCapabilitiesType_Language(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "language"
                        });
        addAnnotation
                (getGetCapabilitiesType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getGetCapabilitiesType_UpdateSequence(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "updateSequence"
                        });
        addAnnotation
                (getCapabilitiesValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "GetCapabilitiesValueType"
                        });
        addAnnotation
                (getCapabilitiesValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "GetCapabilitiesValueType:Object",
                                            "baseType", "GetCapabilitiesValueType"
                        });
        addAnnotation
                (getDataRequestTypeEClass,
                        source,
                        new String[]{
                                            "name", "GetDataRequest_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getGetDataRequestType_Href(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "href",
                                            "namespace", "http://www.w3.org/1999/xlink"
                        });
        addAnnotation
                (getDataTypeEClass,
                        source,
                        new String[]{
                                            "name", "GetData_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getGetDataType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_Attributes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Attributes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_LinkageKeys(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "LinkageKeys",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_FilterColumn(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FilterColumn",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_FilterValue(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FilterValue",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_XSL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "XSL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataType_Aid(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "aid"
                        });
        addAnnotation
                (getDataValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "GetDataValueType"
                        });
        addAnnotation
                (getDataValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "GetDataValueType:Object",
                                            "baseType", "GetDataValueType"
                        });
        addAnnotation
                (getDataXMLTypeEClass,
                        source,
                        new String[]{
                                            "name", "GetDataXML_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getGetDataXMLType_FrameworkURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "FrameworkURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataXMLType_DatasetURI(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DatasetURI",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataXMLType_Attributes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Attributes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataXMLType_LinkageKeys(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "LinkageKeys",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getGetDataXMLType_GetDataHost(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "getDataHost"
                        });
        addAnnotation
                (getGetDataXMLType_Language(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "language"
                        });
        addAnnotation
                (joinAbilitiesTypeEClass,
                        source,
                        new String[]{
                                            "name", "JoinAbilities_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getJoinAbilitiesType_SpatialFrameworks(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "SpatialFrameworks",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_AttributeLimit(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "AttributeLimit",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_OutputMechanisms(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "OutputMechanisms",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_OutputStylings(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "OutputStylings",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_ClassificationSchemaURL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ClassificationSchemaURL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getJoinAbilitiesType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getJoinAbilitiesType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getJoinAbilitiesType_UpdateSupported(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "updateSupported"
                        });
        addAnnotation
                (getJoinAbilitiesType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (joinDataResponseTypeEClass,
                        source,
                        new String[]{
                                            "name", "JoinDataResponse_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getJoinDataResponseType_Status(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Status",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataResponseType_DataInputs(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "DataInputs",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataResponseType_JoinedOutputs(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "JoinedOutputs",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataResponseType_Capabilities(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "capabilities"
                        });
        addAnnotation
                (getJoinDataResponseType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getJoinDataResponseType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getJoinDataResponseType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (joinDataTypeEClass,
                        source,
                        new String[]{
                                            "name", "JoinData_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getJoinDataType_AttributeData(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "AttributeData",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataType_MapStyling(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "MapStyling",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataType_ClassificationURL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ClassificationURL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getJoinDataType_Update(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "update"
                        });
        addAnnotation
                (joinDataValueTypeEEnum,
                        source,
                        new String[]{
                                            "name", "JoinDataValueType"
                        });
        addAnnotation
                (joinDataValueTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "JoinDataValueType:Object",
                                            "baseType", "JoinDataValueType"
                        });
        addAnnotation
                (joinedOutputsTypeEClass,
                        source,
                        new String[]{
                                            "name", "JoinedOutputs_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getJoinedOutputsType_Output(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Output",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (kTypeEClass,
                        source,
                        new String[]{
                                            "name", "K_._type",
                                            "kind", "simple"
                        });
        addAnnotation
                (getKType_Value(),
                        source,
                        new String[]{
                                            "name", ":0",
                                            "kind", "simple"
                        });
        addAnnotation
                (getKType_Aid(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "aid"
                        });
        addAnnotation
                (languagesTypeEClass,
                        source,
                        new String[]{
                                            "name", "Languages_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getLanguagesType_Language(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Language",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (mapStylingTypeEClass,
                        source,
                        new String[]{
                                            "name", "MapStyling_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getMapStylingType_StylingIdentifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "StylingIdentifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMapStylingType_StylingURL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "StylingURL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (measureCountExceptionsEClass,
                        source,
                        new String[]{
                                            "name", "MeasureCountExceptions",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getMeasureCountExceptions_Null(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Null",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (measureTypeEClass,
                        source,
                        new String[]{
                                            "name", "Measure_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getMeasureType_UOM(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "UOM",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMeasureType_Uncertainty(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Uncertainty",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMeasureType_Exceptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Exceptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (mechanismTypeEClass,
                        source,
                        new String[]{
                                            "name", "Mechanism_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getMechanismType_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMechanismType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMechanismType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getMechanismType_Reference(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Reference",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (nominalOrdinalExceptionsEClass,
                        source,
                        new String[]{
                                            "name", "NominalOrdinalExceptions",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getNominalOrdinalExceptions_Null(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Null",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (nominalTypeEClass,
                        source,
                        new String[]{
                                            "name", "Nominal_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getNominalType_Classes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Classes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNominalType_Exceptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Exceptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (nullTypeEClass,
                        source,
                        new String[]{
                                            "name", "Null_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getNullType_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (nullType1EClass,
                        source,
                        new String[]{
                                            "name", "Null_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getNullType1_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getNullType1_Color(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "color"
                        });
        addAnnotation
                (ordinalTypeEClass,
                        source,
                        new String[]{
                                            "name", "Ordinal_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getOrdinalType_Classes(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Classes",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getOrdinalType_Exceptions(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Exceptions",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (outputMechanismsTypeEClass,
                        source,
                        new String[]{
                                            "name", "OutputMechanismsType",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getOutputMechanismsType_Mechanism(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Mechanism",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (outputStylingsTypeEClass,
                        source,
                        new String[]{
                                            "name", "OutputStylingsType",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getOutputStylingsType_Styling(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Styling",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (outputStylingsType1EClass,
                        source,
                        new String[]{
                                            "name", "OutputStylings_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (outputTypeEClass,
                        source,
                        new String[]{
                                            "name", "Output_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getOutputType_Mechanism(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Mechanism",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getOutputType_Resource(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Resource",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getOutputType_ExceptionReport(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ExceptionReport",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (parameterTypeEClass,
                        source,
                        new String[]{
                                            "name", "Parameter_._type",
                                            "kind", "simple"
                        });
        addAnnotation
                (getParameterType_Value(),
                        source,
                        new String[]{
                                            "name", ":0",
                                            "kind", "simple"
                        });
        addAnnotation
                (getParameterType_Name(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "name"
                        });
        addAnnotation
                (purposeTypeEEnum,
                        source,
                        new String[]{
                                            "name", "purpose_._type"
                        });
        addAnnotation
                (purposeTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "purpose_._type:Object",
                                            "baseType", "purpose_._type"
                        });
        addAnnotation
                (referenceDateTypeEClass,
                        source,
                        new String[]{
                                            "name", "ReferenceDate_._type",
                                            "kind", "simple"
                        });
        addAnnotation
                (getReferenceDateType_Value(),
                        source,
                        new String[]{
                                            "name", ":0",
                                            "kind", "simple"
                        });
        addAnnotation
                (getReferenceDateType_StartDate(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "startDate"
                        });
        addAnnotation
                (requestBaseTypeEClass,
                        source,
                        new String[]{
                                            "name", "RequestBaseType",
                                            "kind", "empty"
                        });
        addAnnotation
                (getRequestBaseType_Language(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "language"
                        });
        addAnnotation
                (getRequestBaseType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getRequestBaseType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (requestServiceTypeEEnum,
                        source,
                        new String[]{
                                            "name", "RequestServiceType"
                        });
        addAnnotation
                (requestServiceTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "RequestServiceType:Object",
                                            "baseType", "RequestServiceType"
                        });
        addAnnotation
                (resourceTypeEClass,
                        source,
                        new String[]{
                                            "name", "Resource_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getResourceType_URL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "URL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getResourceType_Parameter(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Parameter",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (rowsetTypeEClass,
                        source,
                        new String[]{
                                            "name", "Rowset_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getRowsetType_Row(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Row",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (rowsetType1EClass,
                        source,
                        new String[]{
                                            "name", "Rowset_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getRowsetType1_Row(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Row",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (rowTypeEClass,
                        source,
                        new String[]{
                                            "name", "Row_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getRowType_K(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "K",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getRowType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (rowType1EClass,
                        source,
                        new String[]{
                                            "name", "Row_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getRowType1_K(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "K",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getRowType1_V(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "V",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (sectionsTypeEDataType,
                        source,
                        new String[]{
                                            "name", "SectionsType",
                                            "baseType", "http://www.eclipse.org/emf/2003/XMLType#string",
                                            "pattern", "(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes)(,(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes))*"
                        });
        addAnnotation
                (spatialFrameworksTypeEClass,
                        source,
                        new String[]{
                                            "name", "SpatialFrameworks_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getSpatialFrameworksType_Framework(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Framework",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (statusTypeEClass,
                        source,
                        new String[]{
                                            "name", "Status_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getStatusType_Accepted(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Accepted",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStatusType_Completed(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Completed",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStatusType_Failed(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Failed",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStatusType_CreationTime(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "creationTime"
                        });
        addAnnotation
                (getStatusType_Href(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "href",
                                            "namespace", "http://www.w3.org/1999/xlink"
                        });
        addAnnotation
                (stylingTypeEClass,
                        source,
                        new String[]{
                                            "name", "Styling_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getStylingType_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStylingType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStylingType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStylingType_Reference(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Reference",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getStylingType_Schema(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Schema",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (tjsCapabilitiesTypeEClass,
                        source,
                        new String[]{
                                            "name", "tjsCapabilitiesType",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getTjsCapabilitiesType_ServiceIdentification(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ServiceIdentification",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (getTjsCapabilitiesType_ServiceProvider(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ServiceProvider",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (getTjsCapabilitiesType_OperationsMetadata(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "OperationsMetadata",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (getTjsCapabilitiesType_Languages(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Languages",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getTjsCapabilitiesType_WSDL(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "WSDL",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getTjsCapabilitiesType_Lang(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "lang",
                                            "namespace", "http://www.w3.org/XML/1998/namespace"
                        });
        addAnnotation
                (getTjsCapabilitiesType_Service(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "service"
                        });
        addAnnotation
                (getTjsCapabilitiesType_UpdateSequence(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "updateSequence"
                        });
        addAnnotation
                (getTjsCapabilitiesType_Version(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "version"
                        });
        addAnnotation
                (typeTypeEEnum,
                        source,
                        new String[]{
                                            "name", "type_._type"
                        });
        addAnnotation
                (typeTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "type_._type:Object",
                                            "baseType", "type_._type"
                        });
        addAnnotation
                (uncertaintyTypeEClass,
                        source,
                        new String[]{
                                            "name", "Uncertainty_._type",
                                            "kind", "simple"
                        });
        addAnnotation
                (getUncertaintyType_Value(),
                        source,
                        new String[]{
                                            "name", ":0",
                                            "kind", "simple"
                        });
        addAnnotation
                (getUncertaintyType_Gaussian(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "gaussian"
                        });
        addAnnotation
                (uomTypeEClass,
                        source,
                        new String[]{
                                            "name", "UOM_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getUOMType_ShortForm(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "ShortForm",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getUOMType_LongForm(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "LongForm",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getUOMType_Reference(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "reference",
                                            "namespace", "http://www.opengis.net/ows/1.1"
                        });
        addAnnotation
                (updateTypeEEnum,
                        source,
                        new String[]{
                                            "name", "update_._type"
                        });
        addAnnotation
                (updateTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "update_._type:Object",
                                            "baseType", "update_._type"
                        });
        addAnnotation
                (valuesTypeEClass,
                        source,
                        new String[]{
                                            "name", "Values_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getValuesType_Nominal(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Nominal",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValuesType_Ordinal(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Ordinal",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValuesType_Count(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Count",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValuesType_Measure(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Measure",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (valueTypeEClass,
                        source,
                        new String[]{
                                            "name", "Value_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getValueType_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType_Color(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "color"
                        });
        addAnnotation
                (getValueType_Rank(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "rank"
                        });
        addAnnotation
                (valueType1EClass,
                        source,
                        new String[]{
                                            "name", "Value_._1_._type",
                                            "kind", "elementOnly"
                        });
        addAnnotation
                (getValueType1_Identifier(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Identifier",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType1_Title(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Title",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType1_Abstract(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Abstract",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType1_Documentation(),
                        source,
                        new String[]{
                                            "kind", "element",
                                            "name", "Documentation",
                                            "namespace", "##targetNamespace"
                        });
        addAnnotation
                (getValueType1_Color(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "color"
                        });
        addAnnotation
                (versionTypeEEnum,
                        source,
                        new String[]{
                                            "name", "VersionType"
                        });
        addAnnotation
                (versionType1EEnum,
                        source,
                        new String[]{
                                            "name", "Version_._type"
                        });
        addAnnotation
                (versionType2EEnum,
                        source,
                        new String[]{
                                            "name", "version_._type"
                        });
        addAnnotation
                (versionTypeObjectEDataType,
                        source,
                        new String[]{
                                            "name", "VersionType:Object",
                                            "baseType", "VersionType"
                        });
        addAnnotation
                (versionTypeObject1EDataType,
                        source,
                        new String[]{
                                            "name", "Version_._type:Object",
                                            "baseType", "Version_._type"
                        });
        addAnnotation
                (versionTypeObject2EDataType,
                        source,
                        new String[]{
                                            "name", "version_._type:Object",
                                            "baseType", "version_._type"
                        });
        addAnnotation
                (vTypeEClass,
                        source,
                        new String[]{
                                            "name", "V_._type",
                                            "kind", "simple"
                        });
        addAnnotation
                (getVType_Value(),
                        source,
                        new String[]{
                                            "name", ":0",
                                            "kind", "simple"
                        });
        addAnnotation
                (getVType_Aid(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "aid"
                        });
        addAnnotation
                (getVType_Null(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "null"
                        });
        addAnnotation
                (wsdlTypeEClass,
                        source,
                        new String[]{
                                            "name", "WSDL_._type",
                                            "kind", "empty"
                        });
        addAnnotation
                (getWSDLType_Href(),
                        source,
                        new String[]{
                                            "kind", "attribute",
                                            "name", "href",
                                            "namespace", "http://www.w3.org/1999/xlink"
                        });
    }

} //Tjs10PackageImpl
