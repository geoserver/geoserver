
Advanced Configuration
======================

.. contents:: :local:
    :depth: 1


Import from Geonetwork
----------------------
The :guilabel:`Import from Geonetwork` option allows the user to import existing metadata from `GeoNetwork <https://geonetwork-opensource.org//>`_.
Two confurations are needed for the import to work:

    - **geonetworks:** configure a list geonetwork endpoints
    - **geonetworkmapping:** define the mapping between the geonetwork fields and the fields configured in the metadata module.

The configuration can be added to the same `yaml <https://yaml.org/>`__ file as the UI configuration or it can be put in a separate file.

Geonetwork endpoint configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The example will configure 2 endpoints. 

.. code:: YAML

    geonetworks:
        - name: Geonetwork DOV production
          url: https://www.dov.vlaanderen.be/geonetwork/srv/api/records/${UUID}/formatters/xml?attachment=true
        - name: Geonetwork test
          url: https://geonetwork-opensource.org/test/srv/api/records/${UUID}/formatters/xml?attachment=true



================  ========  ============================
Key               Required  Description
================  ========  ============================
**name**           yes       The name of the Geonetwork endpoint that will be shown in the dropdown.
**url**            yes       The url of the XML export of the metadata in the Geonetwork, where ``${UUID}`` will be replaced by the metadata's UUID.
================  ========  ============================

Geonetwork mapping configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Each field from Geonetwork can be mapped to a native field from GeoServer or a field from the metadata module. 
The configuration for simple components are added under the yaml attribute `geonetworkmapping`. 
The fields of the type ``COMPLEX`` are mapped under the attribute  `objectmapping`.

The example will map one field (UUID) from the geonetwork xml to UI.

.. code:: YAML    
    
    geonetworkmapping:
        -  geoserver: metadata-identifier
           geonetwork: //gmd:fileIdentifier/gco:CharacterString/text()

A complex object is mapped in the following example:

.. code:: YAML

    objectmapping:
        - typename: responsible-party
          mapping:
            - geoserver: organisation
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString/text()
            - geoserver: contactinfo
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:contactInfo
            - geoserver: role
              geonetwork: .//gmd:CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode/@codeListValue

Metadata from geonetwork can also be mapped to native fields. Do this by setting the `mappingType` to ``NATIVE``

.. code:: YAML

    -  geoserver: title
       geonetwork: //gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString/text()
       mappingType: NATIVE
    -  geoserver: alias
       geonetwork: //gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:alternateTitle/gco:CharacterString/text()
       mappingType: NATIVE

================  ========  ============================
Key               Required  Description
================  ========  ============================
**geoserver**      yes      the key for the attributes in geoserver
**geonetwork**     yes      The `xpath <https://developer.mozilla.org/en-US/docs/Web/XPath>`__ expression pointing to the content from the geonetwork metadata xml file.
**mappingType:**   no        | CUSTOM (default; map to fields from the metadata module configuration)
                             | NATIVE (map to geoserver native fields)
================  ========  ============================

Custom to Native Mapping
------------------------
Sometimes your custom metadata configuration may contain a more complex version of fields already present in geoserver native metadata,
or you may want to derive geoserver native fields (such as URL's, keywords, etcetera) from information in your custom metadata. Native fields
are used by ``GetCapabilities`` requests, and you want to avoid filling in the same information twice. We can automatise deriving these
native fields from custom fields using a custom-to-native mapping configuration. For example in the following configuration:

.. code:: YAML

      customNativeMappings:
        - type: KEYWORDS
          mapping:
            value: KEY_${keywords/name}
            vocabulary: ${keywords/vocabulary}
        - type: IDENTIFIERS
          mapping:
            value: ${identifiers/id}
            authority: ${identifiers/authority}
        - type: METADATALINKS
          mapping:
            value: https://my-host/geonetwork/?uuid=${uuid}
            type: text/html
            metadataType: ISO191156:2003
        - type: METADATALINKS
          mapping:
            value: https://my-host/geonetwork/srv/nl/csw?Service=CSW&Request=GetRecordById&Version=2.0.2&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full&id=${uuid}
            type: text/xml
            metadataType: ISO191156:2003

================  ========  ============================
Key               Required  Description
================  ========  ============================
**type**           yes      currently supported: KEYWORDS, IDENTIFIERS, METADATALINKS
**mapping**        yes      | List of key to value pairs. Value contains a literal with or without placeholder that contains custom attribute path (the ``/`` symbol denoting subfields inside complex fields).
                            | Possible keys for KEYWORDS: value, vocabulary
                            | Possible keys for METADATALINKS: value, type, metadataType, about
                            | Possible keys for IDENTIFIERS: value, authority
================  ========  ============================

The synchronisation of the metadata takes place each time a layer is saved. Any information that has been entered by the user in mapped native fields via the GUI will be lost.

