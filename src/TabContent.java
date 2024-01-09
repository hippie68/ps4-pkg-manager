import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class TabContent extends Composite {
    private GUI gui;
    private String name;
    private Composite searchBar;
    private String filter = null;
    private Table table;
    public PKGQueue<Object> queue;
    public TableThread tableThread;
    public WatcherThread watcherThread;
    private ArrayList<TableItemData> tableItemBuffer; // Full, always-sorted list of table's PKGs and TableItem texts.
    public long stamp = Long.MIN_VALUE; // Keeps track of the order in which PKGs are added in the current session.

    // TODO stop thread when disposing a TabContent widget.

    public TabContent(Composite parent, GUI gui, String name) {
        super(parent, SWT.NONE);
        this.setLayout(new BorderLayout());

        this.gui = gui;

        enableSearchBar();

        this.name = name;
        this.table = createTable(this);

        gui.updateCurrentSelectionStatus(null);
        this.tableItemBuffer = new ArrayList<TableItemData>();
        this.queue = new PKGQueue<Object>();

        this.tableThread = new TableThread(this, queue);
        tableThread.setDaemon(true);
        tableThread.start();

        // TODO: enable watcherThread on demand only.
        this.watcherThread = new WatcherThread(this);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    // TODO: check if current file size is this size; put something like "WRONG
    // SIZE" if not. Better: fill the item background red if incomplete. Maybe less red over time until it's complete?
//    public void markIncompletePkg(TableItem item) {
//        Color incompleteColor = new Color(128, 0, 0);
//
//        PS4PKG pkg = (PS4PKG) item.getData();
//        try {
//            if (Files.size(Paths.get(pkg.path)) != pkg.header.pkg_size)
//                item.setBackground(incompleteColor);
//            else if (item.getBackground() == incompleteColor)
//                item.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public boolean tableItemTextsMatchFilter(String[] texts) {
        if (this.filter == null)
            return true;
        if (texts[Column.FILENAME.ordinal()].toLowerCase().contains(filter)
            || texts[Column.TITLE.ordinal()].toLowerCase().contains(filter)
            || texts[Column.TITLE_ID.ordinal()].toLowerCase().contains(filter))
            return true;
        return false;
    }

    private void insertTableItem(int index, TableItemData data) {
        if (tableItemTextsMatchFilter(data.texts())) {
            TableItem item = new TableItem(table, SWT.NONE, index);
            item.setData(data.pkg());
            item.setText(data.texts());
            // markIncompletePkg(item);
        }
    }

    private void addTableItem(TableItemData data) {
        TableColumn sortColumn = table.getSortColumn();
        int sortColumnIndex = table.indexOf(sortColumn);
        Comparator<String> comparator = Column.get(sortColumnIndex).comparator;
        TableItem[] items = table.getItems();
        int itemCount = table.getItemCount();

        int bufferIndex = 0;
        int itemIndex = 0;
        if (table.getSortDirection() == Platform.SORT_DIRECTION_DESCENDING) {
            while (bufferIndex < itemCount && comparator.compare(data.texts()[sortColumnIndex],
                tableItemBuffer.get(bufferIndex).texts()[sortColumnIndex]) < 0)
                bufferIndex++;
            while (itemIndex < itemCount
                && comparator.compare(data.texts()[sortColumnIndex], items[itemIndex].getText(sortColumnIndex)) < 0)
                itemIndex++;
        } else { // Platform.SORT_DIRECTION_ASCENDING and SWT.NONE
            while (bufferIndex < itemCount && comparator.compare(data.texts()[sortColumnIndex],
                tableItemBuffer.get(bufferIndex).texts()[sortColumnIndex]) >= 0)
                bufferIndex++;
            while (itemIndex < itemCount
                && comparator.compare(data.texts()[sortColumnIndex], items[itemIndex].getText(sortColumnIndex)) >= 0)
                itemIndex++;
        }

        tableItemBuffer.add(bufferIndex, data);
        insertTableItem(itemIndex, data);
    }

    public void processNewTableItemData(TableItemData data) {
        // If the PKG is already known, replace it...
        for (int i = 0; i < tableItemBuffer.size(); i++) {
            if (tableItemBuffer.get(i).pkg().path.equals(data.pkg().path)) {
                tableItemBuffer.set(i, data); // Update both the buffer...
                for (TableItem item : table.getItems()) // ...and, if it exists, the filtered item, too.
                    if (item.getText(Column.PATH.ordinal()).equals(data.pkg().path)) {
                        item.setData(data.pkg());
                        item.setText(data.texts());
                        // markIncompletePkg(item);
                        break;
                    }
                return;
            }
        }

        // ...otherwise, add a new TableItem.
        addTableItem(data);
    }

    public void enableSearchBar() {
        this.searchBar = new Composite(this, SWT.NONE);
        searchBar.setLayoutData(new BorderData(SWT.TOP));
        searchBar.setLayout(new GridLayout(5, false));

        Label searchLabel = new Label(searchBar, SWT.NONE);
        searchLabel.setText("Filter: ");

        Text searchPrompt = new Text(searchBar, SWT.BORDER);
        searchPrompt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
        searchPrompt.addModifyListener((e) -> {
            table.setRedraw(false);
            this.filter = searchPrompt.getText().toLowerCase();
            table.removeAll();
            for (TableItemData data : tableItemBuffer) {
                if (tableItemTextsMatchFilter(data.texts())) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setData(data.pkg());
                    item.setText(data.texts());
                }
            }
            table.setRedraw(true);
        });

        Button clearButton = new Button(searchBar, SWT.PUSH);
        clearButton.setText("Reset Filter");
        clearButton.addListener(SWT.Selection, (e) -> {
            searchPrompt.setText("");
            this.filter = null;
        });

        Label separatorLabel = new Label(searchBar, SWT.SEPARATOR);
        GridData layoutData = new GridData();
        layoutData.heightHint = clearButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        separatorLabel.setLayoutData(layoutData);

        Button tableSettingsButton = new Button(searchBar, SWT.PUSH);
        tableSettingsButton.setText("Table Settings...");
        tableSettingsButton.addListener(SWT.Selection, e -> new TableSettings(this.getShell(), SWT.NONE, this));
    }

    public void disableSearchBar() {
        this.searchBar.dispose();
    }

    public void addFiles(String[] paths) {
        queue.push(paths);
    }

    public void addFile(String path) {
        queue.push(path);
    }

    /**
     * Removes a single PKG file from the tab.
     *
     * @param path The file's absolute path.
     */
    public void removeFile(String path) {
        // Remove PKG from buffer.
        tableItemBuffer.removeIf(data -> data.pkg().path.equals(path));

        // Remove PKG from table.
        for (TableItem item : table.getItems()) {
            PS4PKG pkg = (PS4PKG) item.getData();
            if (pkg.path.equals(path))
                item.dispose();
        }

        // TODO: queued files? TableThread?
    }

    /**
     * Removes all PKG files from the tab that have the provided directory as one of their parent directories.
     *
     * @param parentDirectory The parent directory's absolute path.
     */
    public void removeFiles(String parentDirectory) {
        // Remove PKGs from buffer.
        tableItemBuffer.removeIf(data -> data.pkg().path.startsWith(parentDirectory));

        // Remove PKGs from table.
        for (TableItem item : table.getItems()) {
            PS4PKG pkg = (PS4PKG) item.getData();
            if (pkg.path.startsWith(parentDirectory))
                item.dispose();
        }
    }

    /**
     * (Re-)creates the table's right-click context menu, which includes adding available Custom Actions. Helper
     * function for createTable().
     */
    private void createContextMenu(Table table) {
        Menu oldContextMenu = table.getMenu();
        if (oldContextMenu != null)
            oldContextMenu.dispose();

        Menu contextMenu = new Menu(table);
        table.setMenu(contextMenu);

        MenuItem mntmProperties = new MenuItem(contextMenu, SWT.NONE);
        mntmProperties.setText("Properties");
        mntmProperties.addListener(SWT.Selection, e -> gui.OpenPKGProperties(table));

        // TODO: enable this when implementing PKG renaming.
//        new MenuItem(contextMenu, SWT.SEPARATOR);
//
//        MenuItem mntmRename = new MenuItem(contextMenu, SWT.NONE);
//        mntmRename.setText("Rename...");
//        mntmRename.addListener(SWT.Selection, e -> {
//            TableItem[] items = table.getSelection();
//            PS4PKG[] pkgs = new PS4PKG[items.length];
//            for (int i = 0; i < pkgs.length; i++)
//                pkgs[i] = (PS4PKG) items[i].getData();
//            new PkgRename(gui.getShell(), pkgs);
//        });

        new MenuItem(contextMenu, SWT.SEPARATOR);

        MenuItem mntmRemove = new MenuItem(contextMenu, SWT.NONE);
        mntmRemove.setText("Remove from list");
        mntmRemove.addListener(SWT.Selection, e -> {
            // TODO: kinda inefficient.
            for (TableItem item : table.getSelection()) {
                PS4PKG pkg = (PS4PKG) item.getData();
                for (int i = 0; i < tableItemBuffer.size(); i++) {
                    TableItemData data = tableItemBuffer.get(i);
                    if (pkg == data.pkg())
                        tableItemBuffer.remove(i--);
                }
                item.dispose();
            }
            gui.updateCurrentSelectionStatus(null);
        });

        // Add currently known Custom Actions.
        // TODO: only add a Custom Action if the number and type of selected PKGs matches the command pattern.
        CustomAction[] actions = CustomActions.actions;
        if (actions != null) {
            new MenuItem(contextMenu, SWT.SEPARATOR);

            for (int i = 0; i < actions.length; i++) {
                MenuItem menuItem = new MenuItem(contextMenu, SWT.NONE);
                menuItem.setText(actions[i].name);
                int final_i = i;
                menuItem.addListener(SWT.Selection, e -> {
                    TableItem[] items = table.getSelection();
                    PS4PKG[] pkgs = new PS4PKG[items.length];
                    Arrays.setAll(pkgs, index -> (PS4PKG) items[index].getData());

                    actions[final_i].run(pkgs);
                });
            }
        }
    }

    private Table createTable(Composite parent) {
        Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.MULTI);
        table.setLayoutData(new BorderData(SWT.CENTER));
        table.setHeaderVisible(true);
        if (!System.getProperty("os.name").startsWith("Windows"))
            table.setLinesVisible(true);

        createColumns(table);
        restoreDefaultColumnLayout(table);
        gui.previousTable = table;

        // Handle clicks and double-clicks.
        table.addSelectionListener(new SelectionListener() {
            // Update the status bar upon list item selection.
            @Override
            public void widgetSelected(SelectionEvent e) {
                gui.updateCurrentSelectionStatus(table.getSelection());
            }

            // Open a new properties window on double-click.
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                gui.OpenPKGProperties(table);
            }
        });

        // Add drag and drop functionality.
        DropTarget dropTarget = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void drop(DropTargetEvent event) {
                final String[] paths = (String[]) event.data;
                addFiles(paths);
            }
        });

        // Add right-click context menu.
        createContextMenu(table);

        // Intercept the right-click menu to ...
        table.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                Point pt = Display.getCurrent().map(null, table, new Point(e.x, e.y));
                Rectangle clientArea = table.getClientArea();
                boolean headerClicked = (clientArea.y <= pt.y && pt.y <= (clientArea.y + table.getHeaderHeight()));

                if (headerClicked) { // ... instead display a column selection menu ...
                    // Add table header right-click column selection menu.
                    e.doit = false;
                    Menu tableHeaderMenu = new Menu(table);
                    int[] order = table.getColumnOrder();
                    for (int i : order) {
                        if (i == 0)
                            continue;

                        MenuItem item = new MenuItem(tableHeaderMenu, SWT.CHECK);
                        TableColumn column = table.getColumn(i);
                        item.setText(column.getText());

                        if (column.getWidth() == 0)
                            item.setSelection(false);
                        else
                            item.setSelection(true);

                        item.addListener(SWT.Selection, event -> {
                            if (item.getSelection() == false)
                                hideColumn(column);
                            else
                                showColumn(column);
                        });
                    }
                    tableHeaderMenu.setVisible(true);
                } else if (table.getSelectionCount() == 0) { // ... cancel it if no TableItems are selected ...
                    e.doit = false;
                } else { // ... re-create it based on current Custom Actions.
                    createContextMenu(table);
                }
            }
        });

        return table;
    }

    private void createColumns(Table table) {
        // Create columns.
        for (Column column : Column.values()) {
            TableColumn clmn = new TableColumn(table, column.style);
            clmn.setText(column.name);
            clmn.setToolTipText(column.tooltip);
            clmn.setData(column.comparator);
            clmn.setMoveable(true);
        }
        table.getColumns()[0].setWidth(0);

        // Make columns sortable.
        SelectionAdapter sortListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableColumn column = (TableColumn) e.widget;
                TableColumn sortColumn = table.getSortColumn();

                int sortDirection = table.getSortDirection();
                if (column == sortColumn) {
                    if (sortDirection == Platform.SORT_DIRECTION_ASCENDING)
                        sortDirection = Platform.SORT_DIRECTION_DESCENDING;
                    else if (sortDirection == Platform.SORT_DIRECTION_DESCENDING)
                        sortDirection = SWT.NONE;
                    else
                        sortDirection = Platform.SORT_DIRECTION_ASCENDING;
                } else {
                    table.setSortColumn(column);
                    sortDirection = Platform.SORT_DIRECTION_ASCENDING;
                }

                Comparator<String> comparator = Column.get(table.indexOf(column)).comparator;
                int columnIndex = table.indexOf(column);
                table.setRedraw(false);
                table.removeAll();
                if (sortDirection == Platform.SORT_DIRECTION_ASCENDING)
                    tableItemBuffer.sort((a, b) -> comparator.compare(a.texts()[columnIndex], b.texts()[columnIndex]));
                else if (sortDirection == Platform.SORT_DIRECTION_DESCENDING)
                    tableItemBuffer
                        .sort((a, b) -> -1 * comparator.compare(a.texts()[columnIndex], b.texts()[columnIndex]));
                else
                    // 0: Column.INDEX (sort by the order in which PKGs were added)
                    tableItemBuffer.sort((a, b) -> Column.get(0).comparator.compare(a.texts()[0], b.texts()[0]));
                table.setSortDirection(sortDirection);
                for (TableItemData data : tableItemBuffer) {
                    if (tableItemTextsMatchFilter(data.texts())) { // Respect current filter.
                        TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(data.texts());
                        item.setData(data.pkg());
                    }
                }
                table.setRedraw(true);
            }
        };
        for (TableColumn column : table.getColumns()) {
            column.addSelectionListener(sortListener);
            assert (column.getData() != null); // Each column must have a comparator.
        }
    }

    public void setName(String name) {
        this.name = name;
        Object parent = this.getParent();
        if (parent instanceof TabFolder) {
            TabItem[] tabItems = ((TabFolder) parent).getItems();
            for (TabItem item : tabItems)
                if (item.getControl() == this)
                    item.setText(name);
        }
    }

    public String getName() {
        return this.name;
    }

    public Table getTable() {
        return this.table;
    }

    public ArrayList<TableItemData> getTableItemBuffer() {
        return tableItemBuffer;
    }

    public void showColumn(TableColumn column) {
        assert column.getWidth() == 0;
        int width = (int) column.getData();
        column.setWidth(width);
    }

    public void hideColumn(TableColumn column) {
        int width = column.getWidth();
        assert width != 0;
        column.setData(width);
        column.setWidth(0);
    }

    public void toggleColumn(TableColumn column) {
        int width = column.getWidth();
        if (width == 0)
            showColumn(column);
        else
            hideColumn(column);
    }

    // Restores the default column layout for a table.
    private void restoreDefaultColumnLayout(Table table) {
        TableColumn[] columns = table.getColumns();

        // Reset column order.
        int[] defaultOrder = new int[Column.length];
        for (int i = 0; i < Column.length; i++)
            defaultOrder[i] = i;

        // Set current program version's column order.
        table.setColumnOrder(Column.order);

        // Set aesthetically pleasing column widths
        TableItem t = new TableItem(table, SWT.NONE);
        // Set longest reasonable dummy texts.
        t.setText(Column.TITLE_ID.ordinal(), "CUSA88888");
        t.setText(Column.TITLE_ID.ordinal(), "CUSA88888");
        t.setText(Column.REGION.ordinal(), "World");
        t.setText(Column.TYPE.ordinal(), "Patch");
        t.setText(Column.VERSION.ordinal(), "88.88");
        t.setText(Column.SDK.ordinal(), "88.88");
        t.setText(Column.FIRMWARE.ordinal(), "88.88");
        t.setText(Column.SIZE.ordinal(), "888.88 GB");
        t.setText(Column.RELEASE_TAGS.ordinal(), "OPOISSO893");
        table.setSortDirection(SWT.UP);
        for (int i = 0; i < columns.length; i++) {
            TableColumn column = columns[i];
            table.setSortColumn(column);
            column.pack();
            column.setWidth(column.getWidth() + 2);
            column.setData(column.getWidth());
        }
        t.dispose();
        table.setSortDirection(SWT.NONE);
        table.setSortColumn(table.getColumn(Column.INDEX.ordinal()));
        table.getColumn(Column.TITLE.ordinal())
            .setWidth(table.getColumn(Column.COMPATIBILITY_CHECKSUM.ordinal()).getWidth());

        // Now that each column is set to its optimal width, reset default visibility state.
        for (int i = 0; i < Column.length; i++) {
            Column column = Column.get(i);
            if (!column.enabledByDefault)
                hideColumn(columns[i]);
        }
    }
}
