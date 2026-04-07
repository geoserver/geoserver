# Installing mkdocs

In order to work with mkdocs to generate and preview documentation you will need the following:

- [Python 3](https://www.python.org/download/)
- [pip3](https://pypi.org/project/pip/) (Package Installer for Python, included with Python)

The best practice is to use a python virtual environment, so the tools required for GeoServer documentation are provided their own environment.

1. From the root of your GeoServer checkout:

    ```bash
    python3 -m venv venv
    ```

2. Activate virtual environment and install (or update) requirements:

    ```bash
    source venv/bin/activate
    pip install -r requirements.txt
    ```
 
3. Confirm ***mkdocs*** is available:

    ```bash
    mkdocs --version
    ```
