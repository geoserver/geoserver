# Resource Browser Examples

## Uploading an icon to a styles folder

To upload a file to the global styles folder:

1.  Use **Resource Browser** to select **/ --> styles** in the resource tree.

    ![](img/upload_select_styles.png)
    *Resource Browser styles folder*

2.  Click **Upload** button to open **Upload a Resource** dialog.

3.  Use **Browse** to select a file from the local file system.

    ![](img/upload_icon.png)
    *Upload icon to styles folder*

4.  Press **OK** to upload the file.

    ![](img/upload_icon_complete.png)
    *Resource Browser icon*

## Creating a control-flow configuration file

Many extensions, such as [Control flow module](../../../extensions/controlflow/index.md), are managed using a configuration file.

To create a **`controlflow.properties`** file:

1.  Use **Resource Browser** to select the root **/** folder in the resource tree.

    This can be tricky as the label is not very long.

    ![](img/control_root.png)
    *Resource Browser root folder*

2.  Click **New resource** button to open **Edit a Resource** dialog.

    -   **Resource**: ``controlflow.properties``

    -   **Content**: file contents

        > ~~~properties
        > {% 
        >   include "../../../extensions/controlflow/controlflow.properties"
        > %}
        > ~~~

3.  Press **OK** to create the resource.

    ![](img/control_edit.png)
    *Edit a Resource controlflow.properties*
