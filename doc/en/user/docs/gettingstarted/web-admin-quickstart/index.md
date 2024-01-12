# Using the web administration interface

GeoServer has a browser-based web administration interface application used to configure all aspects of GeoServer, from adding and publishing data to changing service settings.

1.  The web admin interface is accessed via a web browser at:

        http://<host>:<port>/geoserver

2.  For a default installation on a server the link is:

        http://localhost:8080/geoserver

3.  When the application starts, it displays the Welcome page.

    ![](../../webadmin/images/web-admin.png)
    *Welcome Page*

4.  The welcome page provides links describing the web services used to access information.

    To use copy and paste these links into a Desktop GIS, mobile or web mapping application.

!!! note

    For more information, please see the [Welcome](../../webadmin/welcome.md) section.

## Logging In {#logging_in}

In order to change any server settings or configure data, a user must first be authenticated.

1.  Navigate to the upper right of the web interface to log into GeoServer. The default administration credentials are:

    -   User name: `admin`
    -   Password: `geoserver`

    !!! note

        These can be changed in the [Security](../../security/index.md) section.

    ![](login-page.png)
    *Login*

2.  Once logged in, the Welcome screen changes to show the available admin functions. These are primarily shown in the menus on the left side of the page.

    ![](logged_in.png)
    *Additional options when logged in*

## Layer Preview

The [Layer Preview](../../data/webadmin/layerpreview.md) page allows you to quickly view the output of published layers.

1.  Click the **Layer Preview** link on the menu to go to this page.

    ![](../../data/webadmin/img/preview_list.png)

2.  From here, you can find the layer you'd like to preview and click a link for an output format. Click the **OpenLayers** link for a given layer and the view will display.

3.  To sort a column alphabetically, click the column header.

    ![](../../data/webadmin/img/data_sort.png)
    *Unsorted (left) and sorted (right) columns*

4.  Searching can be used to filter the number of items displayed. This is useful for working with data types that contain a large number of items. To search data type items, enter the search string in the search box and click Enter. GeoServer will search the data type for items that match your query, and display a list view showing the search results.

    ![](../../data/webadmin/img/data_search_results.png)
    *Search results for the query "top" on the Workspace page*

    !!! note

        Sorting and searching apply to all data configuration pages.

!!! note

    For more information, please see the [Layer Preview](../../data/webadmin/layerpreview.md) section.
