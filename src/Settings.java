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
	public static String[] ps4Ips = { "127.0.0.1" };
	public static String[] ps4FtpPorts = { "1337", "2121" };
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
			} else
				sharedColumnVisibilities[i] = true;
			sharedColumnWidths[i] = width;
		}

		// DEBUG
		// System.out.println("copy widths:");
		// for (int i = 0; i < sharedColumnWidths.length; i++) {
		// System.out.print(sharedColumnWidths[i] + ",");
		// }
		// System.out.println();
		// System.out.println("copy visibilities:");
		// for (int i = 0; i < sharedColumnVisibilities.length; i++) {
		// System.out.print(sharedColumnVisibilities[i] + ",");
		// }
		// System.out.println();
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
		// System.out.println("paste:");
		// for (TableColumn column : columns)
		// System.out.print(column.getWidth() + ",");
		// System.out.println();
	}

	public static void setSharedColumnWidths(int[] widths) {
		sharedColumnWidths = widths;
	}

	public Settings(Shell parent, int style, GUI gui) {
		super(parent, style);
		setText("Settings");

		TabContent[] tabContents = gui.getTabContents(); // Used below by various settings.

		Shell shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(getText());
		shell.setLayout(new GridLayout(1, false));

		Composite checkboxes = new Composite(shell, SWT.NONE);
		checkboxes.setLayout(new GridLayout(2, false));

		// Checkbox "version check"
		Button versionCheckButton = new Button(checkboxes, SWT.CHECK);
		versionCheckButton.setText("Check for new program version at startup.");
		versionCheckButton.setSelection(Settings.checkUpdates);
		Button checkNow = new Button(checkboxes, SWT.PUSH);
		checkNow.setText("Check Now");
		checkNow.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		checkNow.addListener(SWT.Selection, e -> new VersionCheckDialog(shell, false));

		// Checkbox "shared column layout"
		Composite sharedColumnsComposite = new Composite(checkboxes, SWT.NONE);
		sharedColumnsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
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

		// // Checkbox "extended PKG types"
		// Button extendedTypesButton = new Button(checkboxComposite, SWT.CHECK);
		// extendedTypesButton.setText("[NOT IMPLEMENTED] Display extended PKG types in the \"Type\" column.");

		// Network
		Composite network = new Composite(shell, SWT.NONE);
		network.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		network.setLayout(new GridLayout(1, false));
		Group networkGroup = new Group(network, SWT.NONE);
		networkGroup.setText("Network");
		networkGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		networkGroup.setLayout(new GridLayout(2, true));
		Label ps4IpLabel = new Label(networkGroup, SWT.NONE);
		ps4IpLabel.setText("PS4 IP Address or Hostname");
		Label ps4PortLabel = new Label(networkGroup, SWT.NONE);
		ps4PortLabel.setText("PS4 FTP Ports");
		Text ps4Ips = new Text(networkGroup, SWT.BORDER);
		ps4Ips.setText(String.join(", ", Settings.ps4Ips));
		ps4Ips.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		Text ps4FtpPorts = new Text(networkGroup, SWT.BORDER);
		ps4FtpPorts.setText(String.join(", ", Settings.ps4FtpPorts));
		ps4FtpPorts.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		Label ftpInfo = new Label(networkGroup, SWT.NONE);
		ftpInfo.setText("Multiple comma-separated IPs/hostnames and ports can be specified.");
		ftpInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Release Tags
		Composite tags = new Composite(shell, SWT.NONE);
		tags.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		tags.setLayout(new GridLayout(1, false));
		Group tagsGroup = new Group(tags, SWT.NONE);
		tagsGroup.setText("Custom Release Tags");
		tagsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayout(new GridLayout(2, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Button cancel = new Button(buttons, SWT.NONE);
		cancel.setText("Cancel");
		cancel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		cancel.addListener(SWT.Selection, e -> shell.close());

		Button close = new Button(buttons, SWT.NONE);
		close.setText("Apply and Close");
		close.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		close.addListener(SWT.Selection, e -> {
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

			Settings.ps4Ips = ps4Ips.getText().split(", *");
			Settings.ps4FtpPorts = ps4FtpPorts.getText().split(", *");

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

			shell.close();
		});

		shell.setDefaultButton(close);
		shell.pack();

		// Make sure to populate text boxes only after shell packing so the text won't increase the shell width.
		releaseGroupsText.setText(String.join(", ", Settings.releaseGroups));
		releasesText.setText(String.join(", ", Settings.releases));

		GUI.centerShell(shell);
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
