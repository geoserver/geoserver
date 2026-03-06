# Installing Sphinx

In order to work with Sphinx and generate the HTML/PDF documentation you will need the following:

- [Python 3](http://www.python.org/download/)
- [pip3](https://pypi.org/project/pip/) (Package Installer for Python, included with Python)

To optionally make PDF documentation you will need the following:

- [LaTeX](http://www.latex-project.org/) installation with full extensions (in order to build PDF documentation). For more details, see [Installing LaTeX](installlatex.md).

## Windows

1.  Download and install Python 3. Though there are various distributions and versions, the [official versions](https://www.python.org/downloads/) have been tested and work as expected.

    Put ***python*** in your Path. To do so, go to **Control Panel --> System --> Advanced --> Environment Variables**. Look for `PATH` among the system variables, and add the installation locations to the end of the string. For example, if ***python*** is installed in **`C:\Python`** add the following to the end of the string:

    ``` bat
    ...;C:\Python
    ```

    Confirm availability of Python 3:

    ``` bash
    python --version
    ```

2.  Open a command line window and run:

    ``` bat
    cd doc\en
    pip3 install -r requirements.txt
    ```

3.  Confirm availability with:

    ``` bat
    sphinx-build --version
    sphinx-autobuild --version
    ```

4.  To test for a successful installation, in a command line window, navigate to your GeoServer source checkout and run:

    ``` bat
    mvn clean -f doc/en install
    ```

    This is the same as running:

    ``` bat
    cd doc\en
    ant user
    ant docguide
    ant developer
    ```

    This should generate HTML pages in the **`doc\en\target\user\html`** directory.

## Ubuntu

!!! note

    These instructions may work on other Linux distributions as well, but have not been tested.

1.  Open a terminal and type the following command:

    ``` bash
    sudo apt-get install python-dev build-essential pip
    ```

    Depending on your system this may trigger the installation of other packages.

    Confirm availability of Python 3:

    ``` bash
    python --version
    ```

2.  Install Sphinx using ***pip***:

    ``` bash
    cd doc/en
    pip3 install -r requirements.txt
    ```

3.  Confirm availability with:

    ``` bash
    sphinx-build --version
    sphinx-autobuild --version
    ```

4.  To test for a successful installation, navigate to your GeoServer source checkout and run:

    ``` bash
    mvn clean -f doc/en install
    ```

    This should generate HTML pages in the **`doc/en/target/user/html`** directory.

## Mac OS X

Installing Sphinx on macOS is nearly identical to installing Sphinx on a Linux system.

1.  Example using [homebrew](https://brew.sh) package manager:

    ``` bash
    brew install python
    ```

    Confirm availability of Python 3:

    ``` bash
    python --version
    ```

2.  Use `pip` or `pip3` to install ***sphinx*** and related tools:

    ``` bash
    cd doc/en
    pip3 install -r requirements.txt
    ```

3.  Confirm availability with:

    ``` bash
    sphinx-build --version
    sphinx-autobuild --version
    ```

4.  To test for a successful installation, navigate to your GeoServer source checkout and run:

    ``` bash
    mvn clean -f doc/en install
    ```
