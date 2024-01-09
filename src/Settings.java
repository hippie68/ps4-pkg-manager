import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class Settings extends Dialog {
    // Customizable user variables
    // Note: when adding new variables that directly or indirectly interfere with running threads, make sure they are
    // handled in a thread-safe manner.
    public static boolean checkUpdates = true;

    public static boolean shareColumns = false;
    private static int[] sharedColumnOrder = Column.order;
    private static int[] sharedColumnWidths = new int[Column.length];
    private static boolean[] sharedColumnVisibilities = new boolean[Column.length];

    private static final int RELEASE_TAGS_ROW_COUNT = 3; // Number of rows displayed for release tag text boxes.

    // public static boolean useExtendedPkgTypes = false;
    public static String[] releaseGroups = {}; // Release group tags entered by the user.
    public static String[] releases = {}; // Release tags entered by the user.

    /** Copies a table's current column layout into the static variables .columnOrder and .columnWidths. */
    public static void copySharedColumnLayoutFrom(Table table) {
        sharedColumnOrder = table.getColumnOrder();

        for (int i = 0; i < sharedColumnWidths.length; i++) {
            int width = table.getColumn(i).getWidth();
            if (width == 0) {
                width = (int) table.getColumn(i).getData();
                sharedColumnVisibilities[i] = false;
            } else {
                sharedColumnVisibilities[i] = true;
            }
            sharedColumnWidths[i] = width;
        }

        // DEBUG
//        System.out.println("copy widths:");
//        for (int i = 0; i < sharedColumnWidths.length; i++) {
//            System.out.print(sharedColumnWidths[i] + ",");
//        }
//        System.out.println();
//        System.out.println("copy visibilities:");
//        for (int i = 0; i < sharedColumnVisibilities.length; i++) {
//            System.out.print(sharedColumnVisibilities[i] + ",");
//        }
//        System.out.println();
    }

    /** Sets stored column order and column widths for a specific table. */
    public static void pasteSharedColumnLayoutTo(Table table) {
        table.setColumnOrder(sharedColumnOrder);
        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < sharedColumnWidths.length; i++) {
            int width = sharedColumnWidths[i];
            if (sharedColumnVisibilities[i] == true)
                columns[i].setWidth(width);
            else
                columns[i].setWidth(0);
            columns[i].setData(width);
        }

        // DEBUG
//        System.out.println("paste:");
//        for (TableColumn column : columns)
//            System.out.print(column.getWidth() + ",");
//        System.out.println();
    }

    public static void setSharedColumnWidths(int[] widths) {
        sharedColumnWidths = widths;
    }

    public Settings(Shell parent, int style, GUI gui) {
        super(parent, style);
        setText("Settings");

        Shell shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText(getText());
        GridLayout shellLayout = new GridLayout(1, true);
        shell.setLayout(shellLayout);

        TabContent[] tabContents = gui.getTabContents();

        // Checkboxes

        Composite checkboxComposite = new Composite(shell, SWT.NONE);
        checkboxComposite.setLayout(new GridLayout(2, false));

        // Checkbox "version check"
        // TODO: check the GitHub release page for new version numbers; provide a link.
        Button versionCheckButton = new Button(checkboxComposite, SWT.CHECK);
        versionCheckButton.setText("Check for new program version at startup.");
        versionCheckButton.setSelection(Settings.checkUpdates);
        Button checkNowButton = new Button(checkboxComposite, SWT.PUSH);
        checkNowButton.setText("Check Now");
        checkNowButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        checkNowButton.addListener(SWT.Selection, e -> new VersionCheckDialog(shell, false));

        // Checkbox "shared column layout"
        Composite sharedColumnsComposite = new Composite(checkboxComposite, SWT.NONE);
        sharedColumnsComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        RowLayout sharedColumnsCompositeLayout = new RowLayout();
        sharedColumnsCompositeLayout.center = true;
        sharedColumnsCompositeLayout.marginLeft = 0;
        sharedColumnsCompositeLayout.marginRight = 0;
        sharedColumnsComposite.setLayout(sharedColumnsCompositeLayout);
        Button sharedColumnsButton = new Button(sharedColumnsComposite, SWT.CHECK);
        sharedColumnsButton.setText("Use the same column layout for all tabs, based on tab ");
        sharedColumnsButton.setSelection(Settings.shareColumns);
        Combo tabCombo = new Combo(sharedColumnsComposite, SWT.READ_ONLY);
        tabCombo.add("[Current Tab]");
        tabCombo.select(0);
        for (TabContent tabContent : tabContents)
            tabCombo.add(tabContent.getName());

//        // Checkbox "extended PKG types"
//        Button extendedTypesButton = new Button(checkboxComposite, SWT.CHECK);
//        extendedTypesButton.setText("[NOT IMPLEMENTED] Display extended PKG types in the \"Type\" column.");

        // Release Tags
        Group tagsGroup = new Group(shell, SWT.NONE);
        tagsGroup.setText("Custom Release Tags");
        tagsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        tagsGroup.setLayout(new GridLayout(2, true));
        Label releaseGroupsLabel = new Label(tagsGroup, SWT.NONE);
        releaseGroupsLabel.setText("Release Groups");
        Label releasesLabel = new Label(tagsGroup, SWT.NONE);
        releasesLabel.setText("Releases");

        Text releaseGroupsText = new Text(tagsGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData releaseGroupsTextLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        int textHeight = RELEASE_TAGS_ROW_COUNT
            * new GC(releaseGroupsLabel).stringExtent(releaseGroupsLabel.getText()).y;
        releaseGroupsTextLayoutData.heightHint = textHeight;
        releaseGroupsText.setLayoutData(releaseGroupsTextLayoutData);
        Text releasesText = new Text(tagsGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData releasesTextLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        releasesTextLayoutData.heightHint = textHeight;
        releasesText.setLayoutData(releasesTextLayoutData);

        Label tagsInfo = new Label(tagsGroup, SWT.WRAP);
        tagsInfo.setText("Tags are to be entered as comma-separated lists.");
        tagsInfo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        // Hard-coded tags
        Button internalTagsButton = new Button(tagsGroup, SWT.PUSH);
        internalTagsButton.setText("Show Hard-Coded Release Tags");
        internalTagsButton.addListener(SWT.Selection, e -> new ShowInternalReleaseTags(shell));

        Button closeButton = new Button(shell, SWT.NONE);
        closeButton.addListener(SWT.Selection, e -> shell.close());
        closeButton.setText("Apply and Close");
        shell.setDefaultButton(closeButton);

        // Save custom user variables when closing the window.
        shell.addListener(SWT.Close, e -> {
            Settings.checkUpdates = versionCheckButton.getSelection();

            boolean previousShareColumns = Settings.shareColumns;
            Settings.shareColumns = sharedColumnsButton.getSelection();

            if (previousShareColumns == true && shareColumns == false) {
                // Handle the corner case where the current table has been changed before shareColumns was deselected,
                // updating all tables one last time.
                copySharedColumnLayoutFrom(gui.getCurrentTable());
                for (TabContent tabContent : tabContents)
                    pasteSharedColumnLayoutTo(tabContent.getTable());
            } else if (Settings.shareColumns == true) {
                int index = tabCombo.getSelectionIndex();
                Table table;
                if (index == 0) // "[Current Tab]"
                    table = gui.getCurrentTable();
                else
                    table = tabContents[index - 1].getTable();
                copySharedColumnLayoutFrom(table);
                gui.previousTable = table;
                for (TabContent tabContent : tabContents)
                    pasteSharedColumnLayoutTo(tabContent.getTable());
            }

            Settings.releaseGroups = splitUserInput(releaseGroupsText.getText());
            Settings.releases = splitUserInput(releasesText.getText());
            ReleaseTags.addReleaseTags(Settings.releaseGroups, Settings.releases);
            // Make all table threads re-evaluate all PKGs to update the tags.
            // TODO: only do this if the tags have actually been changed.
            for (TabContent tabContent : gui.getTabContents()) {
                ArrayList<TableItemData> tableItemBuffer = tabContent.getTableItemBuffer();
                PS4PKG[] pkgs = new PS4PKG[tableItemBuffer.size()];
                for (int i = 0; i < tableItemBuffer.size(); i++)
                    pkgs[i] = tableItemBuffer.get(i).pkg();
                tabContent.queue.push(pkgs);
            }
        });

        shell.pack();

        // Make sure to populate text boxes only after shell packing so the text won't increase the shell width.
        releaseGroupsText.setText(String.join(", ", Settings.releaseGroups));
        releasesText.setText(String.join(", ", Settings.releases));

        shell.open();
    }

    private String[] splitUserInput(String input) {
        // TODO: somehow it's still possible to enter/cause empty strings.
        String[] result = input.replaceAll("\\R", ",").split(",");
        for (int i = 0; i < result.length; i++)
            result[i] = result[i].strip();
        return result;
    }
}
