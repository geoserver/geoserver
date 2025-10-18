.. _install_sphinx:

Installing Sphinx
=================

In order to work with Sphinx and generate the HTML/PDF documentation you will need the following:

* `Python 3 <http://www.python.org/download/>`_
* `pip3 <https://pypi.org/project/pip/>`_ (Package Installer for Python, included with Python)

To optionally make PDF documentation you will need the following:

* `LaTeX <http://www.latex-project.org/>`_ installation with full extensions (in order to build PDF documentation). For more details, see :ref:`install_latex`.
   
Windows
-------

#. Download and install Python 3. Though there are various distributions and versions, the `official versions <https://www.python.org/downloads/>`_ have been tested and work as expected.

   Put :command:`python` in your Path.  To do so, go to :menuselection:`Control Panel --> System --> Advanced --> Environment Variables`.  Look for ``PATH`` among the system variables, and add the installation locations to the end of the string.  For example, if :command:`python` is installed in :file:`C:\\Python` add the following to the end of the string:
   
   .. code-block:: bat
   
      ...;C:\Python
      
   Confirm availability of Python 3:

   .. code-block:: bash

      python --version
   
#. Open a command line window and run:
   
   .. code-block:: bat
      
      cd doc\en
      pip3 install -r requirements.txt

#. Confirm availability with:

   .. code-block:: bat
   
      sphinx-build --version
      sphinx-autobuild --version

#. To test for a successful installation, in a command line window, navigate to your GeoServer source checkout and run:
   
   .. code-block:: bat
   
      mvn clean -f doc/en install
      
   This is the same as running:
   
   .. code-block:: bat
      
      cd doc\en
      ant user
      ant docguide
      ant developer
  
   This should generate HTML pages in the :file:`doc\\en\\target\\user\\html` directory.

Ubuntu
------

.. note:: These instructions may work on other Linux distributions as well, but have not been tested.

#. Open a terminal and type the following command:
   
   .. code-block:: bash
   
      sudo apt-get install python-dev build-essential pip
  
   Depending on your system this may trigger the installation of other packages.
   
   Confirm availability of Python 3:

   .. code-block:: bash

      python --version

#. Install Sphinx using :command:`pip`:
   
   .. code-block:: bash
   
      cd doc/en
      pip3 install -r requirements.txt
      
#. Confirm availability with:

   .. code-block:: bash
   
      sphinx-build --version
      sphinx-autobuild --version
  
#. To test for a successful installation, navigate to your GeoServer source checkout and run:
   
   .. code-block:: bash

      mvn clean -f doc/en install
  
   This should generate HTML pages in the :file:`doc/en/target/user/html` directory.
   
Mac OS X
--------

Installing Sphinx on macOS is nearly identical to installing Sphinx on a Linux system. 

#. Example using `homebrew <https://brew.sh>`__ package manager:

   .. code-block:: bash

      brew install python

   Confirm availability of Python 3:

   .. code-block:: bash

      python --version

#. Use ``pip`` or ``pip3`` to install :command:`sphinx` and related tools:

   .. code-block:: bash

      cd doc/en
      pip3 install -r requirements.txt

#. Confirm availability with:

   .. code-block:: bash
   
      sphinx-build --version
      sphinx-autobuild --version

#. To test for a successful installation, navigate to your GeoServer source checkout and run:

   .. code-block:: bash

      mvn clean -f doc/en install

