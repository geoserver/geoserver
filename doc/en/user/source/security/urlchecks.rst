.. _security_urlchecks:

URL Checks
==========

The :guilabel:`URL External Access Checks` page controls the checks that are performed on user provided URLs that
GeoServer will use to access remote resources.

Currently, the checks are performed on the following functionality:

- WMS GetMap requests with remote SLD stylesheets (``sld`` parameter)
- Remote icons referenced by styles (access to icons in the data directory is always allowed)
- WMS GetMap in feature portrayal mode (``REMOTE_OWS`` and ``REMOTE_OWS_TYPE`` parameters)
- WPS remote inputs, either as GET or POST requests

Check this page for any additional remote service access checks added in the future.

Configuration of URL checks
---------------------------

Navigate to :menuselection:`Data > URL Checks` page to manage and configure URL Checks.

.. figure:: images/urlchecks.png

   URL Checks table

Use the :guilabel:`Enable/Disable URL Checks` enable this safety feature:

* When the :guilabel:`URL checks are enabled` checkbox is enabled, URL checks are performed to limit GeoServer access to remote resources as outlined above.
  
  Enabling URL checks is recommended to limit normal Open Web Service protocols interaction being used for Cross Site Scripting attacks.

* When checkbox disabled, :guilabel:`URL checks are NOT enabled`, GeoServer is provided unrestricted access to remote resources.
   
  Disabling URL Checks is not a secure or recommended setting.

Adding a regular expression based check
---------------------------------------

The buttons for adding and removing URL checks can be found at the top of the :guilabel:`URL Check list` table.

To add a URL Check, press the :guilabel:`Add new URL check` button. You will be prompted to enter URL check details (as described in :ref:`security_urlchecks_edit` below).

Removing a regular expression based check
-----------------------------------------

To remove a URL Check, select the checkbox next to one or more rows in the :guilabel:`URL Check list` table.
Press the :guilabel:`Remove selected URL checks` button to remove. You will be asked to confirm or cancel the removal. Pressing :guilabel:`OK` removes the selected URL Checks.

.. _security_urlchecks_edit:

Editing a URL Check
-------------------

Regular Expression URL checks can be configured, with the following parameters for each check:

.. list-table::
   :widths: 30 70 
   :header-rows: 1

   * - Field
     - Description
   * - Name
     - Name for the check, used to identify it in the list.
   * - Description
     - Description of the check, for later reference.
   * - Regular Expression
     - A regular expression used to match allowed URLs
   * - Enabled
     - Check box to enable or disable the check

The most common URL pattern type, allowing matches prefixed by a given host name, follows the following pattern:

``^https?://(www\.)?example\.com/.*$``

Example allowing WMS ``REMOTE_OWS`` data access to an external WFS service:

``^https://safeWFS/geoserver/ows/.*SERVICE=WFS.*$``

.. figure:: images/urlchecks-edit.png
   
   Configure Regular Expression URL check
    
.. note::

   Web sites are available to help define a valid Java regular expression pattern. These tools can be used to interpret, explain and test regular expressions. For example:

   * https://regex101.com/ (enable the Java 8 flavor)
   * https://www.freeformatter.com/java-regex-tester.html 

Testing URL checks
------------------

The :guilabel:`Test URL Checks with external URL` form allows a URL to be checked, reporting if access is allowed or disallowed.

Test URL Checks form:

.. list-table::
   :widths: 30 70 
   :header-rows: 1

   * - Field
     - Description
   * - URL to check 
     - Supply URL of external resource to check if access is allowed

Press the :guilabel:`Test URL` button to perform the checks. If at least one URL Check matches the URL, it will be allowed and the test will indicate the URL Check permitting access. Otherwise it will be rejected and the test will indicate that no URL Check matched.

.. figure:: images/urlchecks-test.png
   
   Test URL Checks with external URL
   
