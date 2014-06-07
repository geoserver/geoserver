.. _background:

Background
==========

Welcome!  From all of the GeoServer users and developers, we are happy to see that you have an interest in contributing to the GeoServer documentation.  Whether it's the fix of a typo or a whole new collection of pages, all contributions are appreciated.  With your help we can make GeoServer documentation helpful and robust, and thus make GeoServer a better product.

History
-------

GeoServer documentation has for a long time been hosted at `geoserver.org <http://geoserver.org>`_.  This site, a `wiki <http://www.atlassian.com/software/confluence>`_, served two functions, as it is both the homepage for GeoServer and the site of the documentation.

There were advantages to having documentation as a wiki: a low barrier to entry (anyone could add/edit pages) and the homepage was already using the wiki software.  However, there were many disadvantages to this as well.  Confluence didn't allow for easy PDF output, pages didn't adhere to any obvious hierarchy, there was no version control, and the quality of documentation was inconsistent.  Thus, it was decided to break off the documentation from the wiki, and `implement the documentation in a new framework <https://github.com/geoserver/geoserver.github.io/wiki/GSIP%2025%20-%20New%20Documentation%20Framework>`_.  

Today
-----

Documentation is now written in `reStructuredText <http://docutils.sourceforge.net/rst.html>`_, a lightweight markup syntax, and built into HTML and PDF content using `Sphinx <http://sphinx.pocoo.org>`_, a documentation framework written by the developers of Python.  In this way, the documentation could be merged with the source code of GeoServer itself, and exist in the `same repository <https://svn.codehaus.org/geoserver/>`_.  With this merge, version control follows naturally, so documentation can now be version specific.  Since the repository requires authentication to make changes, authors must be granted access before being able to directly contribute to the documentation.  While this does slightly raise the barrier to entry, the ability to control the quality of the documentation was seen as a greater benefit.

Read on to find out :ref:`contributing`.



