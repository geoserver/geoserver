# Workflow

GeoServer documentation aims to mirror the development process of the software itself. The process for writing/editing documentation is as follows:

- **Step 1**: Check out source
- **Step 2**: Make changes
- **Step 3**: Build and test locally
- **Step 4**: Commit changes

## Check out source

### Software

You must use the version control software ``git`` to retrieve files.

- <https://windows.github.com>
- <https://mac.github.com>
- <http://git-scm.com/downloads/guis>
- Or use git on the command line

### Repository

This documentation source code exists in the same repository as the GeoServer source code:

    https://github.com/geoserver/geoserver

Follow these instructions to fork the GeoServer repository:

- <https://help.github.com/articles/fork-a-repo>

  Substituting `geoserver/geoserver` in place of `octocat/Spoon-Knife`, when you are finished `git remote -v` should show:

      $  git remote -v
      origin    https://github.com/YOUR_USERNAME/geoserver.git (fetch)
      origin    https://github.com/YOUR_USERNAME/geoserver.git (push)
      upstream  https://github.com/geoserver/geoserver.git (fetch)
      upstream  https://github.com/geoserver/geoserver.git (push)

Within this repository are the various branches and tags associated with releases, and the documentation is always inside a **`doc`** path. Inside this path, the repository contains directories corresponding to different translations. The languages are referred to by a two letter code, with `en` (English) being the default.

For example, the path review the English docs is:

    https://github.com/geoserver/geoserver/tree/main/doc/en

Inside this directory, there are four directories:

    user/
    developer/
    docguide/
    theme/

|  |  |
|----|----|
| **Directory** | **Description** |
| **`user`** | User Manual source files |
| **`developer`** | Developer Manual source files |
| **`docguide`** | Documentation Guide source files (this is what you are reading now) |
| **`theme`** | GeoServer Sphinx theme (common to all three projects) |

## Make changes

Documentation in Sphinx is written in [reStructuredText](https://docutils.sourceforge.io/rst.html), a lightweight markup syntax. For suggestions on writing reStructuredText for use with Sphinx, please see the section on [Sphinx Syntax](sphinx.md). For suggestions about writing style, please see the [Style Guidelines](style.md).

## Build and test locally

You should install Sphinx on your local system (see the next page on [Installing Sphinx](install.md)) to build the documentation locally and view any changes made. Sphinx builds the reStructuredText files into HTML pages and PDF files.

1.  Confirm availability of Python 3:

    ``` bash
    python --version
    ```

2.  Install sphinx and sphinx-autobuild:

    ``` bash
    cd doc\en
    pip3 install -r requirements.txt
    ```

### HTML

1.  On a terminal, navigate to your GeoServer source checkout and change to the **`doc/en`** directory (or whichever project you wish to build).

2.  Run the following command to build the docs and open the browser with a live preview:

    ``` bash
    ant user-site
    ```

    The documentation will refresh as you edit individual files.

3.  Run the following command to only build the docs:

    ``` bash
    ant user
    ```

    The resulting HTML pages will be contained in **`doc/en/target/user/html`**.

4.  Watch the output of the above commands for any errors and warnings. These could be indicative of problems with your markup. Please fix any errors and warnings before continuing.

### PDF

1.  On a terminal, navigate to your GeoServer source checkout and change to the **`doc/en`** directory (or whichever project you wish to build).

2.  Run the following command:

    ``` bash
    ant user-pdf
    ```

    This will create a PDF file called **`{GeoServerProject}.pdf`** in the same directory

    !!! note

        The exact name of **`{GeoServerProject}`** depends on which project is being built. However, there will only be one file with the extension `.tex` in the **`doc/en/user/build/latex`** directory, so there should hopefully be little confusion.

    !!! warning

        This command requires [LaTeX](http://www.latex-project.org/) to be installed, and ***pdflatex*** to be added to your Path.

3.  Watch the output of the above command for any errors and warnings. These could be indicative of problems with your markup. Please fix any errors and warnings before continuing.

## Commit changes

!!! warning

    If you have any errors or warnings in your project, please fix them before committing!

The final step is to commit the changes to a branch in *your* repository, using these commands:

    git checkout -b doc-fix
    git add [path/file(s)]
    git commit -m "message describing your fix"
    git push origin doc-fix

You can use any name you like for the branch, often I use the issue number so I can tell my branches apart if I need to find them later. **`{path/file(s)}`** is the path and file(s) you wish to commit to the repository. If you are unclear about which files you have changed you can use `git status -sb` to list the files that you have changed, this will give you a list of changed files, and indicate the ones that still need to be added to this commit:

    $ git status -sb 
    ## update
     M docguide/source/background.rst
     M docguide/source/contributing.rst
     M docguide/source/install.rst
     M docguide/source/installlatex.rst
     M docguide/source/workflow.rst

Here the `M` indicate these files are modified but not added. Once `git add *.rst` is run the indicator will change to `A`, files that are not under git's control will show a `?` these are new files that you may need to add if you have created them for the documentation.

When ready return to the GitHub website and submit a pull request:

- <https://help.github.com/articles/using-pull-requests>

The GitHub website provides a link to [CONTRIBUTING.md](https://github.com/geoserver/geoserver/blob/main/CONTRIBUTING.md) outlining how we can accept your patch. Small fixes may be contributed on your behalf, changes larger than a file (such as a tutorial) may require some paperwork.
