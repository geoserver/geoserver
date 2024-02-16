# Bulk Load tool {: #tool_bulk }

The **Catalog Bulk Load Tool** is used to duplicate GeoServer configuration (workspaces, stores, layers) for testing. The tool can also be used to make a single duplicate for experimenting with configuration and optimization.

![](img/bulk_tool.png)
*Catalog Bulk Load Tool*

## Duplicating Configuration

1.  Navigate to **Tools --> Catalog Bulk Load Tool**
2.  Select the item to copy:
    -   **Workspace and Namespace**
    -   **Store**
    -   **Resource and Layer**
3.  Fill in the **\# of times to duplicate**.
4.  Provide a **Suffix to append**
5.  Choose to recursively copy:
    -   Recursively copying a workspace will duplicate all stores and layers contained in the workspace
    -   Recursively copying a store will copy all layers published by the store
6.  Press **Start** to begin duplicating
