import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class TableSettings extends Dialog {

    // TODO: fill the empty table with the watcher thread's current user-provided directories and booleans.
    public TableSettings(Shell parent, int style, TabContent tabContent) {
        super(parent, style);
        setText("Table Settings for Tab \"" + tabContent.getName() + "\"");

        Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText(getText());
        shell.setLayout(new RowLayout(SWT.VERTICAL));

        Group syncGroup = new Group(shell, SWT.NONE);
        syncGroup.setText(Platform.DIRECTORY_TERM + " Synchronization");
        syncGroup.setLayout(new GridLayout(3, true));

        Table table = new Table(syncGroup, SWT.CHECK | SWT.BORDER);
        GridData tableLayoutData = new GridData(SWT.FILL, SWT.CENTER, false, true, 2, 3);
        tableLayoutData.heightHint = table.getHeaderHeight() + 3 * table.getItemHeight();
        table.setLayoutData(tableLayoutData);

        // Add user-provided directories to table.
        String[] dirs = tabContent.watcherThread.getSynchedDirs();
        int[] checked = tabContent.watcherThread.getSynchedDirsRecursionState();
        for (int i = 0; i < dirs.length; i++) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(dirs[i]);
            item.setChecked(checked[i] == 1);
        }

        Button addDirectoryButton = new Button(syncGroup, SWT.PUSH);
        addDirectoryButton.setText("Add " + Platform.DIRECTORY_TERM + "...");
        GridData buttonLayoutData = new GridData();
        buttonLayoutData.horizontalAlignment = SWT.FILL;
        addDirectoryButton.setLayoutData(buttonLayoutData);
        addDirectoryButton.addListener(SWT.Selection, e -> {
            DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
            dialog.setText("Select " + Platform.DIRECTORY_TERM);
            dialog.open();
            String directory = dialog.getFilterPath();
            if (!directory.isEmpty()) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(directory);
            }
        });

        Button removeDirectoryButton = new Button(syncGroup, SWT.PUSH);
        removeDirectoryButton.setText("Remove " + Platform.DIRECTORY_TERM);
        removeDirectoryButton.setLayoutData(buttonLayoutData);
        removeDirectoryButton.addListener(SWT.Selection, e -> {
            TableItem[] items = table.getSelection();
            if (items.length != 0) {
                tabContent.watcherThread.removeDirectory(items[0].getText());
                items[0].dispose();
            }
        });

        Button closeButton = new Button(shell, SWT.PUSH);
        closeButton.addListener(SWT.Selection, e -> shell.close());
        closeButton.setText("Apply and Close");

        new Label(syncGroup, SWT.NONE);
        Label commentLabel = new Label(syncGroup, SWT.NONE);
        commentLabel
            .setText("Checked paths will have their sub" + Platform.DIRECTORIES_TERM.toLowerCase() + " synced, too.");
        GridData commentLabelLayoutData = new GridData();
        commentLabelLayoutData.horizontalSpan = 3;
        commentLabel.setLayoutData(commentLabelLayoutData);

        // Apply settings when closing the window.
        shell.addListener(SWT.Close, e -> {
            for (TableItem item : table.getItems())
                tabContent.watcherThread.addDirectory(item.getText(0), item.getChecked());

            tabContent.watcherThread.generateWatchKeys();
            tabContent.watcherThread.addPkgFilesFromWatchedDirectories();
        });

        shell.setDefaultButton(closeButton);
        shell.pack();
        shell.open();
    }
}
