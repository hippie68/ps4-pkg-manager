import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class GUI {

    public static final String PROGRAM_NAME = "PS4 PKG Manager";
    public static final String PROGRESS_TEXT = "Processing...";
    public static final int PROGRESS_CHECK_INTERVAL = 1000;

    private Display display;
    private Shell shell;
    private TabFolder tabFolder;
    private static String DEFAULT_TAB_NAME = "Default Tab";
    private static String NEW_TAB_NAME = "New Tab";
    private Label currentSelectionStatus;
    private Label progressIndicator;
    public Table previousTable;

    public String dataDirectory = System.getProperty("user.home") + switch (SWT.getPlatform()) {
        case "win32" -> "/AppData/PKG Manager";
        case "cocoa" -> "/Library/Application Support/PKG Manager";
        default -> "/.pkg-manager";
    };
    private final String settingsPath = dataDirectory + "/ui.properties";
    private final String actionsPath = dataDirectory + "/actions.txt";
    private final String databasePath = dataDirectory + "/pkgs.db";;

    public static void main(String[] args) {
        try {
            GUI gui = new GUI();
            gui.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void open() {
        this.display = Display.getDefault();

        shell = new Shell();
        shell.setText(PROGRAM_NAME);
        shell.setLayout(new BorderLayout());
        shell.addListener(SWT.Close, e -> {
            TabContent[] tabContents = getTabContents();

            // Stop watcher threads from creating new tasks for the table threads.
            for (TabContent t : tabContents)
                t.watcherThread.interrupt();

            // Flush pending queue insertions.
            while (display.readAndDispatch())
                ;

            // Tell all table threads to automatically return as soon as their queue is done.
            for (TabContent t : tabContents)
                t.queue.push(null);

            if (isProcessingData(true)
                && new YesNoDialog("Please Confirm", "Data is still being processed.\nWait for it to finish?")
                    .open() == true) {
                new ExitMessage(shell, "Processing remaining data and saving application state...");
            } else {
                // Force table threads to return now.
                for (TabContent t : tabContents)
                    t.tableThread.interrupt();

                new ExitMessage(shell, "Saving application state...");
            }

            // Wait for all table threads to return.
            for (TabContent t : tabContents)
                try {
                    t.tableThread.join();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

            // Flush pending table item insertions.
            while (display.readAndDispatch())
                ;

            saveGUIState();
            CustomActions.saveActionsToFile(actionsPath);
            System.exit(0);
        });

        createMenu();
        createStatusBar();

        // Try to load GUI state from file; on failure, create a single, detached default table.
        if (this.loadGUIState() == -1) {
            createNewTab(null);

            // Set 16:9 aspect ratio.
            shell.pack();
            Point p = shell.getSize();
            shell.setSize(p.x, p.x * 9 / 16); // TODO: use screen's actual aspect ratio.
        }

        // Load Custom Actions.
        CustomActions.loadActionsFromFile(actionsPath);

        // Check for updates before opening shell.
        Thread updateThread = new Thread(() -> {
            if (Settings.checkUpdates == true) {
                int ret = Version.checkForUpdates();
                // TODO: make this unobtrusive once the beta has ended.
                if (ret == 0)
                    display.asyncExec(() -> new VersionCheckDialog(shell, true));
            }
        });
        updateThread.start();

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /** Returns the GUI's shell. */
    public Shell getShell() {
        return this.shell;
    }

    /** Creates the GUI's main menu (File, Edit, ...). */
    // TODO: private functions for menu entries that both appear in the main menu and the context menu.
    private void createMenu() {
        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
        mntmFile.setText("&File");

        Menu menuFile = new Menu(mntmFile);
        mntmFile.setMenu(menuFile);

        MenuItem mntmNewTab = new MenuItem(menuFile, SWT.NONE);
        mntmNewTab.setText("New &Tab");
        mntmNewTab.addListener(SWT.Selection, e -> createNewTab(null));

        new MenuItem(menuFile, SWT.SEPARATOR);

        MenuItem mntmAddFiles = new MenuItem(menuFile, SWT.NONE);
        mntmAddFiles.setText("Add &Files...");
        mntmAddFiles.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            dialog.setText("Select PS4 PKG files (complete or partially downloaded)");
            dialog.open();
            String[] fileNames = dialog.getFileNames();
            String directory = dialog.getFilterPath();
            String paths[] = new String[fileNames.length];
            for (int i = 0; i < fileNames.length; i++)
                paths[i] = directory + System.getProperty("file.separator") + fileNames[i];

            getCurrentTabContent().addFiles(paths);
        });

        MenuItem mntmAddDirectory = new MenuItem(menuFile, SWT.NONE);
        mntmAddDirectory.setText("Add " + Platform.DIRECTORY_TERM.replaceFirst("(d|D)", "&$1") + "...");
        mntmAddDirectory.addListener(SWT.Selection, e -> {
            DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
            dialog.setText("Select a " + Platform.DIRECTORY_TERM + " that contains PS4 PKG files");
            String directory = dialog.open();
            if (directory != null)
                getCurrentTabContent().addDirectory(directory, false);
        });

        MenuItem mntmAddDirectoryRecursively = new MenuItem(menuFile, SWT.NONE);
        mntmAddDirectoryRecursively.setText("Add " + Platform.DIRECTORY_TERM + " &Recursively...");
        mntmAddDirectoryRecursively.addListener(SWT.Selection, e -> {
            DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
            dialog.setText("Select a " + Platform.DIRECTORY_TERM);
            String directory = dialog.open();
            if (directory != null)
                getCurrentTabContent().addDirectory(directory, true);
        });

        new MenuItem(menuFile, SWT.SEPARATOR);

        MenuItem mntmQuit = new MenuItem(menuFile, SWT.NONE);
        mntmQuit.setText("&Quit");
        mntmQuit.addListener(SWT.Selection, e -> shell.close());

        MenuItem mntmEdit = new MenuItem(menu, SWT.CASCADE);
        mntmEdit.setText("&Edit");

        Menu menuEdit = new Menu(mntmEdit);
        mntmEdit.setMenu(menuEdit);

        MenuItem mntmSelectAll = new MenuItem(menuEdit, SWT.NONE);
        mntmSelectAll.setText("Select &All");
        mntmSelectAll.addListener(SWT.Selection, e -> {
            Table table = getCurrentTable();
            if (table == null)
                return;
            table.selectAll();
            updateCurrentSelectionStatus(table.getSelection());
        });

        MenuItem mntmDeselectAll = new MenuItem(menuEdit, SWT.NONE);
        mntmDeselectAll.setText("&Deselect All");
        mntmDeselectAll.addListener(SWT.Selection, e -> {
            Table table = getCurrentTable();
            if (table == null)
                return;
            table.deselectAll();
            updateCurrentSelectionStatus(null);
        });

        MenuItem mntmInvertSelection = new MenuItem(menuEdit, SWT.NONE);
        mntmInvertSelection.setText("&Invert Selection");
        mntmInvertSelection.addListener(SWT.Selection, e -> {
            Table table = getCurrentTable();
            for (TableItem item : table.getItems()) {
                int index = table.indexOf(item);
                if (table.isSelected(index))
                    table.deselect(index);
                else
                    table.select(index);
            }
            table.setTopIndex(table.getSelectionIndex());
            updateCurrentSelectionStatus(table.getSelection());
        });

        new MenuItem(menuEdit, SWT.SEPARATOR);

        MenuItem mntmSelectExisting = new MenuItem(menuEdit, SWT.NONE);
        mntmSelectExisting.setText("Select &Existing");
        mntmSelectExisting.addListener(SWT.Selection, e -> {
            Table table = getCurrentTable();
            table.deselectAll();
            for (TableItem item : table.getItems())
                if (Files.exists(Paths.get(((PS4PKG) item.getData()).path)))
                    table.select(table.indexOf(item));
            table.setTopIndex(table.getSelectionIndex());
            updateCurrentSelectionStatus(table.getSelection());
        });

        MenuItem mntmSelectNonExisting = new MenuItem(menuEdit, SWT.NONE);
        mntmSelectNonExisting.setText("Select Non-E&xisting");
        mntmSelectNonExisting.addListener(SWT.Selection, e -> {
            Table table = getCurrentTable();
            table.deselectAll();
            for (TableItem item : table.getItems())
                if (!Files.exists(Paths.get(((PS4PKG) item.getData()).path)))
                    table.select(table.indexOf(item));
            table.setTopIndex(table.getSelectionIndex());
            updateCurrentSelectionStatus(table.getSelection());
        });

        MenuItem mntmSelectSynchronized = new MenuItem(menuEdit, SWT.NONE);
        mntmSelectSynchronized.setText("Select &Synchronized");
        mntmSelectSynchronized.addListener(SWT.Selection, e -> {
            TabContent tabContent = getCurrentTabContent();
            Table table = tabContent.getTable();
            TableItem[] items = table.getItems();
            String[] synchedDirs = tabContent.watcherThread.getSynchedDirs();
            table.deselectAll();
            if (synchedDirs.length != 0) {
                int[] synchedDirsRecursionState = tabContent.watcherThread.getSynchedDirsRecursionState();
                for (TableItem item : items) {
                    PS4PKG pkg = (PS4PKG) item.getData();
                    for (int i = 0; i < synchedDirs.length; i++) {
                        if (synchedDirsRecursionState[i] == 0) {
                            if (pkg.directory.equals(synchedDirs[i]))
                                table.select(table.indexOf(item));
                        } else if (pkg.directory.startsWith(synchedDirs[i])) {
                            table.select(table.indexOf(item));
                        }
                    }
                }
                table.setTopIndex(table.getSelectionIndex());
            }
            updateCurrentSelectionStatus(table.getSelection());
        });

        MenuItem mntmSelectNonSynchronized = new MenuItem(menuEdit, SWT.NONE);
        mntmSelectNonSynchronized.setText("Select Non-S&ynchronized");
        mntmSelectNonSynchronized.addListener(SWT.Selection, e -> {
            TabContent tabContent = getCurrentTabContent();
            Table table = tabContent.getTable();
            TableItem[] items = table.getItems();
            String[] synchedDirs = tabContent.watcherThread.getSynchedDirs();
            if (synchedDirs.length == 0) {
                table.selectAll();
            } else {
                table.deselectAll();
                int[] synchedDirsRecursionState = tabContent.watcherThread.getSynchedDirsRecursionState();
                for (TableItem item : items) {
                    PS4PKG pkg = (PS4PKG) item.getData();
                    for (int i = 0; i < synchedDirs.length; i++) {
                        if (synchedDirsRecursionState[i] == 0) {
                            if (!pkg.directory.equals(synchedDirs[i]))
                                table.select(table.indexOf(item));
                        } else if (!pkg.directory.startsWith(synchedDirs[i])) {
                            table.select(table.indexOf(item));
                        }
                    }
                }
                table.setTopIndex(table.getSelectionIndex());
            }
            updateCurrentSelectionStatus(table.getSelection());
        });

        new MenuItem(menuEdit, SWT.SEPARATOR);

        MenuItem mntmActions = new MenuItem(menuEdit, SWT.NONE);
        mntmActions.setText("&Custom Actions...");
        mntmActions.addListener(SWT.Selection, e -> new CustomActions(shell));

        MenuItem mntmOptions = new MenuItem(menuEdit, SWT.NONE);
        mntmOptions.setText("Se&ttings...");
        mntmOptions.addListener(SWT.Selection, e -> new Settings(shell, SWT.NONE, this));

        MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
        mntmHelp.setText("&Help");

        Menu menuHelp = new Menu(mntmHelp);
        mntmHelp.setMenu(menuHelp);

        MenuItem mntmAbout = new MenuItem(menuHelp, SWT.NONE);
        mntmAbout.setText("&About");
        mntmAbout.addListener(SWT.Selection, e -> new About(shell, SWT.NONE, this));
    }

    /** Returns an array containing all TabContents. */
    public TabContent[] getTabContents() {
        if (this.tabFolder == null) {
            Control[] controls = shell.getChildren();
            for (Control control : controls)
                if (control instanceof TabContent tabContent)
                    return new TabContent[] { tabContent };
            return new TabContent[] {};
        } else {
            TabContent[] result = new TabContent[tabFolder.getItemCount()];
            TabItem[] items = tabFolder.getItems();
            for (int i = 0; i < result.length; i++)
                result[i] = (TabContent) items[i].getControl();
            return result;
        }
    }

    /** Returns the currently displayed TabContent. */
    public TabContent getCurrentTabContent() {
        if (this.tabFolder == null) {
            Control[] controls = shell.getChildren();
            for (Control control : controls)
                if (control instanceof TabContent tabContent)
                    return tabContent;
            throw new IllegalStateException("GUI does not have a TabContent.");
        } else {
            return (TabContent) this.tabFolder.getSelection()[0].getControl();
        }
    }

    /** Returns an array containing all TabContents' Tables. */
    public Table[] getTables() {
        TabContent[] tabContents = getTabContents();
        assert tabContents.length != 0;
        Table[] tables = new Table[tabContents.length];
        for (int i = 0; i < tabContents.length; i++)
            tables[i] = tabContents[i].getTable();
        return tables;
    }

    /** Returns the currently displayed Table. */
    public Table getCurrentTable() {
        return getCurrentTabContent().getTable();
    }

    /**
     * Checks if one of the table threads is still processing data.
     *
     * @param ignoreNullObjects if true, currently queued null objects are not taken into account
     * @return true if data is still being processed
     */
    private boolean isProcessingData(boolean ignoreNullObjects) {
        for (TabContent tabContent : getTabContents())
            if (tabContent.tableThread.isProcessingData(ignoreNullObjects))
                return true;
        return false;
    }

    /** Creates a Properties window containing the provided Table's current PKG selection. */
    public void openPkgProperties(Table table) {
        TableItem[] items = table.getSelection();

        int index;
        if (items.length == 1) {
            // If only 1 PKG is selected, open all PKGs, for convenience.
            index = table.indexOf(items[0]);
            items = table.getItems();
        } else {
            // If specific PKGs are selected, open only those.
            index = 0;
        }

        PS4PKG[] pkgs = new PS4PKG[items.length];
        for (int i = 0; i < items.length; i++)
            pkgs[i] = (PS4PKG) items[i].getData();

        new PkgProperties(shell, pkgs, index);
    }

    private void createStatusBar() {
        Composite statusBar = new Composite(shell, SWT.NONE);
        statusBar.setLayoutData(new BorderData(SWT.BOTTOM));
        statusBar.setLayout(new GridLayout(2, false));

        this.currentSelectionStatus = new Label(statusBar, SWT.NONE);
        currentSelectionStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        this.progressIndicator = new Label(statusBar, SWT.RIGHT | SWT.NONE);
        progressIndicator.setText(PROGRESS_TEXT);
        GridData progressIndicatorLayoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        progressIndicatorLayoutData.widthHint = progressIndicator.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        progressIndicator.setLayoutData(progressIndicatorLayoutData);
        progressIndicator.setText("");

        Thread progressIndicatorThread = new Thread(() -> {
            while (true) {
                display.syncExec(() -> {
                    if (isProcessingData(false) == true) {
                        if (progressIndicator.getText().isEmpty())
                            setProgressStatus("Processing...");
                    } else {
                        if (!progressIndicator.getText().isEmpty())
                            setProgressStatus("");
                    }
                });

                try {
                    Thread.sleep(PROGRESS_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        progressIndicatorThread.setDaemon(true);
        progressIndicatorThread.start();
    }

    /**
     * Prints the currently selected PKG file name(s) on the status bar. Normally this method is triggered
     * automatically. But if the PKG selection was done programmatically, for the status to appear this function must be
     * explicitly called."
     *
     * @param items the table items that the status bar should tell about. If null, the status bar will be empty.
     */
    public void updateCurrentSelectionStatus(TableItem[] items) {
        if (items == null || items.length == 0)
            this.currentSelectionStatus.setText("");
        else if (items.length == 1)
            this.currentSelectionStatus.setText(((PS4PKG) items[0].getData()).path);
        else
            this.currentSelectionStatus.setText(String.format("%d PKG files selected.", items.length));
        // TODO: add "[Incomplete]", perhaps in a different color, when the file's length is smaller than the one in
        // the header.
    }

    /** Puts text in the bottom right corner of the status bar. */
    private void setProgressStatus(String text) {
        progressIndicator.setText(text);
    }

    private boolean createDataDirectory() {
        File saveDir = new File(dataDirectory);
        if (saveDir.exists())
            return true;
        return saveDir.mkdirs();
    }

    private void saveGUIState() {
        if (createDataDirectory() == false) {
            RetryAbortBox box;
            boolean success = false;
            do {
                box = new RetryAbortBox(shell, "Error",
                    String.format("Could not create the application directory\n%s", dataDirectory));
            } while (box.open() == SWT.RETRY && (success = createDataDirectory()) == false);
            if (success == false)
                return;
        }

        Properties props = new Properties();

        // Save settings -----------------------------------------------------------------------------------------------

        props.setProperty("check_updates", String.valueOf(Settings.checkUpdates));
        props.setProperty("share_columns", String.valueOf(Settings.shareColumns));

        // Save other --------------------------------------------------------------------------------------------------

        // Save column count.
        props.setProperty("column_count", String.valueOf(Column.length));

        TabContent[] tabContents = getTabContents();
        Table[] tables = getTables();

        // Save table count.
        props.setProperty("table_count", String.valueOf(tables.length));

        // Save state of all tables.
        for (int i = 0; i < tables.length; i++) {
            // Save table name.
            props.setProperty(i + "_tab_name", ((TabContent) tables[i].getParent()).getName());

            // Save column order.
            props.setProperty(i + "_column_order", Arrays.toString(tables[i].getColumnOrder()));

            TableColumn[] columns = tables[i].getColumns();

            // Save column width.
            int[] columnWidth = new int[columns.length];
            for (int j = 1; j < columns.length; j++) { // Ignoring the "Index" column, 0.
                int width = columns[j].getWidth();
                if (width == 0)
                    columnWidth[j] = (int) columns[j].getData();
                else
                    columnWidth[j] = width;
            }
            props.setProperty(i + "_column_width", Arrays.toString(columnWidth));

            // Save column visibility.
            int[] columnVisibility = new int[columns.length];
            for (int j = 0; j < columns.length; j++)
                columnVisibility[j] = columns[j].getWidth() == 0 ? 0 : 1;
            props.setProperty(i + "_column_visibility", Arrays.toString(columnVisibility));

            // Save sort column.
            props.setProperty(i + "_sort_column", String.valueOf(tables[i].indexOf(tables[i].getSortColumn())));

            // Save sort direction.
            props.setProperty(i + "_sort_direction", String.valueOf(tables[i].getSortDirection()));

            // Save PKG count.
            int pkgCount = tabContents[i].getTableItemBuffer().size();
            props.setProperty(i + "_pkg_count", String.valueOf(pkgCount));
        }

        // Save currently selected tab.
        if (tabFolder != null)
            props.setProperty("selected_tab", String.valueOf(tabFolder.getSelectionIndex()));

        // Save shell size.
        Point size = shell.getSize();
        props.setProperty("shell_size", String.format("%dx%d", size.x, size.y));

        // Save custom release tags.
        props.setProperty("custom_release_groups", String.join(", ", Settings.releaseGroups));
        props.setProperty("custom_releases", String.join(", ", Settings.releases));

        // Save synchronized directories, delimited by null characters.
        for (int i = 0; i < tabContents.length; i++) {
            String[] synchedDirs = tabContents[i].watcherThread.getSynchedDirs();
            int[] synchedDirsRecursions = tabContents[i].watcherThread.getSynchedDirsRecursionState();
            props.setProperty(i + "_synced_dirs", String.join("\0", synchedDirs));
            props.setProperty(i + "_synced_dirs_recursions", Arrays.toString(synchedDirsRecursions));
        }

        try {
            props.store(new FileWriter(settingsPath), "PS4 PKG Manager user interface settings");
        } catch (IOException e) {
        }

        // Save PKG database -------------------------------------------------------------------------------------------

        // Write PKG objects to database.
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(databasePath))) {
            for (int i = 0; i < tables.length; i++) {
                for (TableItemData data : tabContents[i].getTableItemBuffer())
                    oos.writeObject(data.pkg());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper function for loadGUIState().
    private int[] propertyToIntArray(String property) {
        String[] temp = property.replace("[", "").replace("]", "").split(", ");
        int[] array = new int[temp.length];
        for (int i = 0; i < temp.length; i++)
            array[i] = Integer.valueOf(temp[i]);
        return array;
    }

    /**
     * Tries to load all settings from the program's properties file and all PKG metadata from the PKG database.
     * <p>
     * This includes:<br>
     * - The program's window size<br>
     * - Each tab's column layout (widths, visible columns)<br>
     * - Each tab's "Table Settings"<br>
     * - Each tab's PKGs
     *
     * @return 0 on success, otherwise -1.
     */
    private int loadGUIState() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(settingsPath));
        } catch (IOException e) {
            return -1;
        }

        // Load settings -----------------------------------------------------------------------------------------------

        try {
            Settings.checkUpdates = Boolean.valueOf(props.getProperty("check_updates"));
            Settings.shareColumns = Boolean.valueOf(props.getProperty("share_columns"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load other --------------------------------------------------------------------------------------------------

        // Load state of all tables.
        {
            // Load required iterator counts.
            int tableCount;
            int columnCount;
            try {
                tableCount = Integer.valueOf(props.getProperty("table_count"));
                columnCount = Integer.valueOf(props.getProperty("column_count"));
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            for (int i = 0; i < tableCount; i++) {
                try {
                    // Load table name.
                    String tabName = props.getProperty(i + "_tab_name");

                    // Load column order.
                    int[] columnOrder = propertyToIntArray(props.getProperty(i + "_column_order"));

                    // Load column width.
                    int[] columnWidth = propertyToIntArray(props.getProperty(i + "_column_width"));
                    if (Settings.shareColumns == true)
                        Settings.setSharedColumnWidths(Arrays.copyOf(columnWidth, columnWidth.length));

                    // Load column visibility.
                    int[] columnVisibility = propertyToIntArray(props.getProperty(i + "_column_visibility"));

                    // Load sort column.
                    int sortColumnIndex = Integer.valueOf(props.getProperty(i + "_sort_column"));

                    // Load sort direction.
                    int sortDirection = Integer.valueOf(props.getProperty(i + "_sort_direction"));

                    // Use the acquired data to create a new TabContent.
                    TabContent tabContent = createNewTab(null);
                    tabContent.setName(tabName);
                    Table table = tabContent.getTable();
                    table.setColumnOrder(columnOrder);
                    TableColumn[] columns = table.getColumns();
                    for (int j = 1; j < columnCount; j++) { // Ignoring the "Index" column, 0.
                        if (columnVisibility[j] == 0) {
                            columns[j].setWidth(0);
                            columns[j].setData(columnWidth[j]);
                        } else {
                            columns[j].setWidth(columnWidth[j]);
                        }
                    }
                    table.setSortColumn(table.getColumn(sortColumnIndex));
                    table.setSortDirection(sortDirection);

                    // If the current program version has additional columns, append them.
                    if (Column.length > columnCount) {
                        for (int j = columnCount; i < Column.length; i++) {
                            columns[j].setWidth(100);
                            columns[j].setData(100);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Load currently selected tab.
            if (tableCount > 1)
                try {
                    int selectionIndex = Integer.valueOf(props.getProperty("selected_tab"));
                    tabFolder.setSelection(selectionIndex);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            previousTable = getCurrentTable();
        }

        // Load shell size.
        try {
            int x, y;
            String windowSize = props.getProperty("shell_size");
            String[] size = windowSize.split("x");
            x = Integer.valueOf(size[0]);
            y = Integer.valueOf(size[1]);
            shell.setSize(x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load custom release tags.
        try {
            Settings.releaseGroups = props.getProperty("custom_release_groups").split(", ");
            Settings.releases = props.getProperty("custom_releases").split(", ");
            ReleaseTags.addReleaseTags(Settings.releaseGroups, Settings.releases);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load synchronized directories.
        try {
            TabContent[] tabContents = getTabContents();
            for (int i = 0; i < tabContents.length; i++) {
                // Load synchronized directories.
                String[] syncedDirs = props.getProperty(i + "_synced_dirs").split("\0");
                if (syncedDirs[0].isEmpty())
                    continue;
                int[] syncedDirsRecursions = propertyToIntArray(props.getProperty(i + "_synced_dirs_recursions"));
                for (int j = 0; j < syncedDirs.length; j++) {
                    String dir = syncedDirs[j];
                    Boolean recursion = syncedDirsRecursions[j] == 1;
                    tabContents[i].watcherThread.addDirectory(dir, recursion);
                }

                tabContents[i].watcherThread.generateWatchKeys();
                tabContents[i].watcherThread.start();
                // tabContents[i].watcherThread.printSynchronizedDirectories(); // DEBUG
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load PKGs from database.
        // TODO: if serial UID mismatches, load all PKG files from their original locations.
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(databasePath))) {
            TabContent[] tabContents = getTabContents();
            loop: for (int i = 0; i < tabContents.length; i++) {
                int pkgCount = Integer.parseInt(props.getProperty(i + "_pkg_count"));
                // Read this TabContent's PKGs from file and send them to its TableThread for processing.
                for (int j = 0; j < pkgCount; j++) {
                    // This will send the PKGs to the table thread, adding them while the user interface can be used.
                    // However, while the resulting initial sort order is a correct one, it is not necessarily the exact
                    // same order of the previous session.
                    // tabContents[i].queue.push(ois.readObject());

                    // This alternative approach retains the original sort order, at the cost of bypassing various
                    // methods the thread would normally use. Any changes here must be double-checked very carefully.
                    // Depending on the hardware, it may also introduce a short period of user interface lag.
                    // Fragile as it is: should this break one day, forget about it and use the previous method.
                    // TODO: let the user choose the method.
                    TableItem item = new TableItem(tabContents[i].getTable(), SWT.NONE);
                    PS4PKG pkg = (PS4PKG) ois.readObject();
                    if (pkg == null)
                        break loop;
                    String[] texts = tabContents[i].tableThread.Ps4PkgToTableItemText(pkg);
                    item.setText(texts);
                    item.setData(pkg);
                    tabContents[i].getTableItemBuffer().add(new TableItemData(pkg, texts));
                    // tabContents[i].markIncompletePkg(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Creates and returns a new tab folder.
    private TabFolder createTabFolder() {
        TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
        tabFolder.addListener(SWT.Selection, e -> updateCurrentSelectionStatus(getCurrentTable().getSelection()));

        Menu tabMenu = new Menu(tabFolder);

        // Make tab folder's right-click menu detect the correct tab.
        tabFolder.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                Point pt = display.getCursorLocation();
                pt = tabFolder.toControl(pt);
                TabItem item = tabFolder.getItem(pt); // TODO: won't select the tab item if near its borders.
                                                      // Try the map method?
                if (item == null) {
                    // e.doit = false;
                    for (int i = 1; i < tabMenu.getItemCount(); i++)
                        tabMenu.getItem(i).setEnabled(false);
                    return;
                } else {
                    tabFolder.setData("menu_origin", item); // Used to retrieve the TabItem the right-click originated
                                                            // in.
                    for (int i = 1; i < tabMenu.getItemCount(); i++)
                        tabMenu.getItem(i).setEnabled(true);
                }
            }
        });

        // Create tab folder's right-click menu.

        tabFolder.setMenu(tabMenu);

        // "New tab"
        // TODO: insert new tab before the right-clicked tab?
        MenuItem mntmNewTab = new MenuItem(tabMenu, SWT.NONE);
        mntmNewTab.setText("New Tab");
        mntmNewTab.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewTab(null);
            }
        });

        new MenuItem(tabMenu, SWT.SEPARATOR);

        // "Rename"
        MenuItem mntmRename = new MenuItem(tabMenu, SWT.NONE);
        mntmRename.setText("Rename...");
        mntmRename.addListener(SWT.Selection, e -> {
            TabItem item = (TabItem) tabFolder.getData("menu_origin");
            RenameTabDialog dialog = new RenameTabDialog(shell, item.getText());
            String name = dialog.open();
            item.setText(name);
            ((TabContent) item.getControl()).setName(name);
        });

        new MenuItem(tabMenu, SWT.SEPARATOR);

        // "Move left"
        MenuItem mntmMoveLeft = new MenuItem(tabMenu, SWT.NONE);
        mntmMoveLeft.setText("Move Left");
        mntmMoveLeft.addListener(SWT.Selection, e -> {
            int index = tabFolder.indexOf((TabItem) tabFolder.getData("menu_origin"));
            if (index == 0) // Abort if the item is the leftmost one.
                return;

            // Save current selection.
            int selectionIndex = tabFolder.getSelectionIndex();

            // Swap selection, if necessary.
            if (selectionIndex == index)
                selectionIndex = index - 1;
            else if (selectionIndex == index - 1)
                selectionIndex = index;

            // Swap items.
            TabContent[] tabContents = getTabContents();
            TabContent temp = tabContents[index];
            tabContents[index] = tabContents[index - 1];
            tabContents[index - 1] = temp;

            // Re-create tabs.
            TabItem[] items = tabFolder.getItems();
            for (int i = index - 1; i < items.length; i++)
                items[i].dispose();
            for (int i = index - 1; i < items.length; i++)
                createNewTab(tabContents[i]);

            // Restore previous selection.
            tabFolder.setSelection(selectionIndex);
        });

        // "Move right"
        MenuItem mntmMoveRight = new MenuItem(tabMenu, SWT.NONE);
        mntmMoveRight.setText("Move Right");
        mntmMoveRight.addListener(SWT.Selection, e -> {
            int index = tabFolder.indexOf((TabItem) tabFolder.getData("menu_origin"));
            if (index == tabFolder.getItemCount() - 1) // Abort if the item is the rightmost one.
                return;

            // Save current selection.
            int selectionIndex = tabFolder.getSelectionIndex();

            // Swap selection, if necessary.
            if (selectionIndex == index)
                selectionIndex = index + 1;
            else if (selectionIndex == index + 1)
                selectionIndex = index;

            // Swap items.
            TabContent[] tabContents = getTabContents();
            TabContent temp = tabContents[index];
            tabContents[index] = tabContents[index + 1];
            tabContents[index + 1] = temp;

            // Re-create tabs.
            TabItem[] items = tabFolder.getItems();
            for (int i = index; i < items.length; i++)
                items[i].dispose();
            for (int i = index; i < items.length; i++)
                createNewTab(tabContents[i]);

            // Restore previous selection.
            tabFolder.setSelection(selectionIndex);
        });

        new MenuItem(tabMenu, SWT.SEPARATOR);

        // "Remove"
        MenuItem mntmRemoveTab = new MenuItem(tabMenu, SWT.NONE);
        mntmRemoveTab.setText("Close Tab");
        mntmRemoveTab.addListener(SWT.Selection, e -> removeTab((TabItem) tabFolder.getData("menu_origin")));

        // Handle shared column layout upon switching tabs.
        tabFolder.addListener(SWT.Selection, e -> {
            if (Settings.shareColumns) {
                Table table = getCurrentTable();
                if (table == previousTable)
                    return;

                if (previousTable != null && !previousTable.isDisposed())
                    Settings.copySharedColumnLayoutFrom(previousTable);
                Settings.pasteSharedColumnLayoutTo(table);
                previousTable = table;
            }
        });

        return tabFolder;
    }

    // Creates a new tab and puts the specified TabContent inside. If the TabContent is null, a new one is created
    // using default data. The resulting TabContent is returned.
    private TabContent createNewTab(TabContent tabContent) {
        int tabContentCount = getTabContents().length;
        switch (tabContentCount) {
            case 0:
                if (tabContent == null) {
                    // Create the very first TabContent without a tab folder.
                    tabContent = new TabContent(shell, this, GUI.DEFAULT_TAB_NAME);
                    return tabContent;
                } // Else fall-through.
            case 1:
                if (tabFolder == null) {
                    // Create the program's tab folder, a new tab and attach the already-existing first TabContent.
                    TabFolder newTabFolder = createTabFolder();
                    TabItem tabItem0 = new TabItem(newTabFolder, SWT.NONE);
                    TabContent tab0 = getCurrentTabContent();
                    tabItem0.setText(tab0.getName());
                    tab0.setParent(newTabFolder);
                    tabItem0.setControl(tab0);
                    this.tabFolder = newTabFolder;
                }
                // Fall-through.
            default:
                // Create a new tab.
                TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
                if (tabContent == null) {
                    // Create a new TabContent.
                    String newTabName = "";
                    loop: for (int i = tabContentCount; i < Integer.MAX_VALUE; i++) { // Find a unique tab name.
                        newTabName = GUI.NEW_TAB_NAME + " #" + i;
                        for (TabItem item : tabFolder.getItems()) {
                            if (newTabName.equals(item.getText()))
                                continue loop;
                        }
                        break;
                    }
                    tabItem.setText(newTabName);
                    tabContent = new TabContent(tabFolder, this, newTabName);
                } else {
                    // Attach the provided TabContent.
                    tabItem.setText(tabContent.getName());
                    tabContent.setParent(tabFolder);
                }
                tabItem.setControl(tabContent);
                tabFolder.setSelection(tabItem);

                // Somehow this is necessary here to make the table show up, at least with GTK.
                // The alternative, for whatever reason, would be not to call .setSelection()
                // above and instead having to click on the new tab manually.
                tabFolder.setLayoutData(new BorderData(SWT.LEFT));
                shell.layout();
                tabFolder.setLayoutData(new BorderData(SWT.CENTER));
                shell.layout();

                return tabContent;
        }
    }

    private void removeTab(TabItem item) {
        // Ask for confirmation if the table is not empty.
        if (((TabContent) item.getControl()).getTable().getItemCount() != 0) {
            YesNoDialog dialog = new YesNoDialog("Close Tab",
                "Do you really want to close the tab \"" + item.getText() + "\"?\nThis can't be undone.");
            boolean reply = dialog.open();
            if (reply == false)
                return;
        }

        if (Settings.shareColumns)
            Settings.copySharedColumnLayoutFrom(getCurrentTable());

        item.getControl().dispose();
        item.dispose();
        if (tabFolder.getItemCount() == 1) {
            getCurrentTabContent().setParent(shell);
            tabFolder.dispose();
            tabFolder = null;
            shell.layout();
        }
    }

    /** Centers a shell on its parent. Must be called after shell.pack() and before shell.open(). */
    public static void centerShell(Shell shell) {
        Rectangle parentSize;
        Composite parent = shell.getParent();
        if (parent == null) { // For shells which have no parent shell (i.e. Properties).
            return;
            // TODO
            // parentSize = shell.getDisplay().getBounds();
        } else
            parentSize = shell.getParent().getBounds();
        Rectangle shellSize = shell.getBounds();
        int x = (parentSize.width - shellSize.width) / 2 + parentSize.x;
        int y = (parentSize.height - shellSize.height) / 2 + parentSize.y;
        shell.setLocation(new Point(x, y));
    }
}
