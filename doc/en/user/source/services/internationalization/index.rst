.. _internationalization:

Internationalization (i18n)
===========================

GeoServer provides the possibility to return the content of a GetCapabilities response according to user defined languages. The functionality is available for the following services:

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

The response content language can be defined through the ``AcceptLanguages`` request param. The way in which GeoServer will reply to the parameter value follows the below rules:

* the content being returned according to the specified language will comprise titles, abstracts and keywords.

* if a single language code is specified, eg. ``AcceptLanguages=en`` GeoServer will try to return the content in that language. If no content is found in that language an error message will be returned.

* if multiple language codes are specified, eg. ``AcceptLanguages=en fr`` for each internationalizable content GeoServer will try to return it in one of the specified language. If no content is found for each language an error message will be returned.

* languages can be configured and request also according to local language variants eg. ``AcceptLanguages=en fr-CA``. If any i18n content has been specified with a local variant eg. ``fr-CA`` and the request parameters specifies only the language code eg. ``AcceptLanguages=fr``, the ``fr-CA`` content will be encoded in the response, while in the inverse case the content will not be included.

* if a ``*`` is present among the parameter values, eg ``AcceptLanguages=en fr *``, GeoServer will try to return the content in one of the specified language code. If no content is found content will be returned in a language among the ones availables.

* if not all the configurable elements have i18n title and abstract available for the requested language, GeoServer will encode those attributes only for services, layers, layergroups and styles that have them defined and will put, in place of the missing internazionaled content an error message like the following: ``DID NOT FIND i18n CONTENT FOR THIS ELEMENT`` as in the example below.



.. figure:: img/ErrorMessage.png
