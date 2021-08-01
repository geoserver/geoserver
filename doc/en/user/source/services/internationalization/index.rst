.. _internationalization:

Internationalization (i18n)
===========================

GeoServer supports returning a GetCapabilities document in various languages. The functionality is available for the following services:

* WMS 1.1 and 1.3
* WFS 2.0
* WCS 2.0


Configuration
=============

GeoServer provides an i18n editor for the title and abstract of:

* Layers configuration page.
* Layergroups configuration page.
* WMS, WFS, WCS service configuration pages.
* For Styles i18n configuration see :ref:`sld_language`.

The editor is disabled by default and can be enabled from the i18n checkbox:

.. figure:: img/i18nEditor.png

GetCapabilities
===============

The response content language can be selected using the ``AcceptLanguages`` request parameter. The GeoServer response will vary based on the following rules:

* The internationalized elements will be titles, abstracts and keywords.

* If a single language code is specified, e.g. ``AcceptLanguages=en`` GeoServer will try to return the content in that language. If no content is found in that language an error message will be returned.

* If multiple language codes are specified, e.g. ``AcceptLanguages=en fr`` or ``AcceptLanguages=en,fr``, for each internationalizable content GeoServer will try to return it in one of the specified language. If no content is found for each language an error message will be returned.

* Languages can be configured and request also according to local language variants e.g. ``AcceptLanguages=en fr-CA`` or ``AcceptLanguages=en,fr-CA``. If any i18n content has been specified with a local variant eg. ``fr-CA`` and the request parameters specifies only the language code e.g. ``AcceptLanguages=fr``, the ``fr-CA`` content will be encoded in the response, while in the inverse case the content will not be included.

* If a ``*`` is present among the parameter values, e.g. ``AcceptLanguages=en fr *`` or ``AcceptLanguages=en,fr,*``, GeoServer will try to return the content in one of the specified language code. If no content is found content will be returned in a language among the ones availables.

* If not all the configurable elements have i18n title and abstract available for the requested language, GeoServer will encode those attributes only for services, layers, layergroups and styles that have them defined. In case the missing value is the tile, in place of the missing internationalized content an error message like the following, will appear: ``DID NOT FIND i18n CONTENT FOR THIS ELEMENT``.

* When using ``AcceptLanguages`` parameter GeoServer will encode URL present in the response adding language parameter with the first value retrieved from the ``AcceptLanguages`` parameter.

.. figure:: img/ErrorMessages.png


Default Language
================

GeoServer allows defining a default language to be used when international content has been set in services', layers' and groups' configuration pages, but no ``AcceptLanguages`` parameter has been specified in a ``GetCapabilities`` request. The default language can be set from the services' configuration pages (WMS, WFS, WCS) or from global settings from a dropdown as shown below:

.. figure:: img/DefaultLanguage.png

GeoServer will first check for a service specific default locale. If not found, it will use the one in the global settings. If  the global default language has not been specified, GeoServer will use the server default locale.
