.. _workflow:

Workflow
========

GeoServer documentation aims to mirror the development process of the software itself.  The process for writing/editing documentation is as follows:

* **Step 1**: Check out source
* **Step 2**: Make changes
* **Step 3**: Build and test locally
* **Step 4**: Commit changes
   
Check out source
----------------

Repository
``````````

This documentation source code exists in the same repository as the GeoServer source code::

   https://github.com/geoserver/geoserver

Within this repository are the various branches and tags associated with releases, and the documentation is always inside a :file:`doc` path.  Inside this path, the repository contains directories corresponding to different translations.  The languages are referred to by a two letter code, with ``en`` (English) being the default.

For example, the path review the English docs is::

   https://github.com/geoserver/geoserver/tree/master/doc/en

Inside this directory, there are four directories::

   user/
   developer/
   docguide/
   theme/

.. list-table::
   :widths: 20 80

   * - **Directory**
     - **Description**
   * - :file:`user`
     - User Manual source files
   * - :file:`developer`
     - Developer Manual source files
   * - :file:`docguide`
     - Documentation Guide source files (this is what you are reading now)
   * - :file:`theme`
     - GeoServer Sphinx theme (common to all three projects)

Software
````````

You must use a version control software to retrieve files. 

* https://windows.github.com
* https://mac.github.com
* http://git-scm.com/downloads/guis
* Or use git on the command line

Follow these instructions to fork the GeoServer repository:

* https://help.github.com/articles/fork-a-repo

Make changes
------------

Documentation in Sphinx is written in `reStructuredText <http://docutils.sourceforge.net/rst.htm>`_, a lightweight markup syntax.  For suggestions on writing reStructuredText for use with Sphinx, please see the section on :ref:`sphinx`.  For suggestions about writing style, please see the :ref:`style_guidelines`.


Build and test locally
----------------------

You should install Sphinx on your local system to build the documentation locally and view any changes made.  Sphinx builds the reStructuredText files into HTML pages and PDF files.

HTML
````

#. On a terminal, navigate to your GeoServer source checkout and change to the :file:`doc/en/user` directory (or whichever project you wish to build).

#. Run the following command::

      make html

   The resulting HTML pages will be contained in :file:`doc/en/user/build/html`.

#. Watch the output of the above command for any errors and warnings.  These could be indicative of problems with your markup.  Please fix any errors and warnings before continuing.

PDF
```

#. On a terminal, navigate to your GeoServer source checkout and change to the :file:`doc/en/user` directory (or whichever project you wish to build).

#. Run the following command::

      make latex

   The resulting LaTeX pages will be contained in :file:`doc/en/user/build/latex`.

#. Change to the :file:`doc/en/user/build/latex` directory.

#. Run the following command::

      pdflatex [GeoServerProject].tex

   This will create a PDF file called :file:`{GeoServerProject}.pdf` in the same directory

   .. note:: The exact name of :file:`{GeoServerProject}` depends on which project is being built.  However, there will only be one file with the extension ``.tex`` in the :file:`doc/en/user/build/latex` directory, so there should hopefully be little confusion.

   .. warning:: This command requires `LaTeX <http://www.latex-project.org/>`_ to be installed, and :command:`pdflatex` to be added to your Path.

#. Watch the output of the above command for any errors and warnings.  These could be indicative of problems with your markup.  Please fix any errors and warnings before continuing.


Commit changes
--------------

.. warning:: If you have any errors or warnings in your project, please fix them before committing!

The final step is to commit the changes to the repository.  If you are using Subversion, the command to use is::

   git add [path/file(s)]
   git commit -m "message describing your fix"
   git push
   
where :file:`{path/file(s)}` is the path and file(s) you wish to commit to the repository.

When ready return to the GitHub website and submit a pull request:

* https://help.github.com/articles/using-pull-requests

The GitHub website provides a link to `CONTRIBUTING.md <https://github.com/geoserver/geoserver/blob/master/CONTRIBUTING.md>`_ outlining how we can accept your patch. Small fixes may be contributed on your behalf, changes larger than a file (such as a tutorial) may require some paperwork.
