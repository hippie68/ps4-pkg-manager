import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class PkgProperties {
	private Shell shell;
	private PS4PKG[] pkgs;
	private int pkgIndex;
	private Label pathLabel;
	private Label countLabel;
	private TabFolder tabFolder;
	private Table headerTable;
	private Table fileTable;
	private Table sfoTable;
	private Text changelogText;

	private void updateTitle(PS4PKG pkg) {
		shell.setText(pkg.getSFOValue("TITLE") + " - Properties");
	}

	private void createHeaderTableItem(String name, Object value) {
		String fmt;
		if (value instanceof Short)
			fmt = "0x%04X";
		else if (value instanceof Integer)
			fmt = "0x%08X";
		else if (value instanceof Long)
			fmt = "0x%016X";
		else if (value instanceof String)
			fmt = "%s";
		else
			return;

		TableItem item = new TableItem(this.headerTable, SWT.NONE);
		item.setText(0, name);
		item.setText(1, String.format(fmt, value));
	}

	private void loadHeaderTable(PS4PKG pkg) {
		this.headerTable.removeAll();

		createHeaderTableItem("Type", pkg.header.type);
		createHeaderTableItem("File count", pkg.header.file_count);
		createHeaderTableItem("Entry count", pkg.header.entry_count);
		createHeaderTableItem("SC entry count", pkg.header.sc_entry_count);
		createHeaderTableItem("Table offset", pkg.header.table_offset);
		createHeaderTableItem("Entry data size", pkg.header.entry_data_size);
		createHeaderTableItem("Body offset", pkg.header.body_offset);
		createHeaderTableItem("Body size", pkg.header.body_size);
		createHeaderTableItem("Content offset", pkg.header.content_offset);
		createHeaderTableItem("Content size", pkg.header.content_size);
		createHeaderTableItem("Content ID", pkg.header.content_id);
		createHeaderTableItem("DRM type", pkg.header.drm_type);
		createHeaderTableItem("Content type", pkg.header.content_type);
		createHeaderTableItem("Content flags", pkg.header.content_flags);
		createHeaderTableItem("Promote size", pkg.header.promote_size);
		createHeaderTableItem("Version date", pkg.header.version_date);
		createHeaderTableItem("Version hash", pkg.header.version_hash);
		createHeaderTableItem("IRO tag", pkg.header.iro_tag);
		createHeaderTableItem("DRM type version", pkg.header.drm_type_version);
		createHeaderTableItem("Digest entries 1", pkg.header.digest_entries_1);
		createHeaderTableItem("Digest entries 2", pkg.header.digest_entries_2);
		createHeaderTableItem("Digest table digest", pkg.header.digest_table_digest);
		createHeaderTableItem("Digest body digest", pkg.header.digest_body_digest);
		createHeaderTableItem("PFS image count", pkg.header.pfs_image_count);
		createHeaderTableItem("PFS image flags", pkg.header.pfs_image_flags);
		createHeaderTableItem("PFS image offset", pkg.header.pfs_image_offset);
		createHeaderTableItem("PFS image size", pkg.header.pfs_image_size);
		createHeaderTableItem("Mount image offset", pkg.header.mount_image_offset);
		createHeaderTableItem("Mount image size", pkg.header.mount_image_size);
		createHeaderTableItem("PKG size", pkg.header.pkg_size);
		createHeaderTableItem("PFS signed size", pkg.header.pfs_signed_size);
		createHeaderTableItem("PFS cache size", pkg.header.pfs_cache_size);
		createHeaderTableItem("PFS image digest", pkg.header.pfs_image_digest);
		createHeaderTableItem("PFS signed digest", pkg.header.pfs_signed_digest);
		createHeaderTableItem("PFS split size nth 0", pkg.header.pfs_split_size_nth_0);
		createHeaderTableItem("PFS split size nth 1", pkg.header.pfs_split_size_nth_1);
		createHeaderTableItem("PKG digest", pkg.header.pkg_digest);

		for (TableColumn column : headerTable.getColumns())
			column.pack();
	}

	private void loadFileTable(PS4PKG pkg) {
		fileTable.removeAll();

		for (PS4PKGEntry entry : pkg.entries) {
			TableItem item = new TableItem(fileTable, SWT.NONE);
			item.setText(0, String.format("0x%04X", entry.id));
			item.setText(1, entry.filename != null ? entry.filename : switch (entry.id) {
				case 0x0001 -> "DIGESTS";
				case 0x0010 -> "ENTRY_KEYS";
				case 0x0020 -> "IMAGE_KEY";
				case 0x0080 -> "GENERAL_DIGESTS";
				case 0x0100 -> "METAS";
				case 0x0200 -> "ENTRY_NAMES";
				case 0x0400 -> "license.dat";
				case 0x0401 -> "license.info";
				case 0x0402 -> "nptitle.dat";
				case 0x0403 -> "npbind.dat";
				case 0x0404 -> "selfinfo.dat";
				case 0x0406 -> "imageinfo.dat";
				case 0x0407 -> "target-deltainfo.dat";
				case 0x0408 -> "origin-deltainfo.dat";
				case 0x0409 -> "psreserved.dat";
				default -> String.format("UNKNOWN_0x%04X", entry.id);
			});
			item.setText(2, entry.flags1 == 0 ? "-" : String.format("0x%08X", entry.flags1));
			item.setText(3, entry.flags2 == 0 ? "-" : String.format("0x%08X", entry.flags2));
			item.setText(4, String.valueOf(entry.offset));
			item.setText(5, String.valueOf(entry.size));
		}

		for (TableColumn column : fileTable.getColumns())
			column.pack();
	}

	private void loadSfoTable(PS4PKG pkg) {
		sfoTable.removeAll();

		if (pkg.params == null)
			return;

		for (SFOParameter param : pkg.params) {
			TableItem item = new TableItem(sfoTable, SWT.NONE);
			item.setText(0, param.name);
			item.setText(1, param.value);
		}

		for (TableColumn column : sfoTable.getColumns())
			column.pack();
	}

	private void updateCountLabel() {
		countLabel.setText(pkgIndex + 1 + " of " + pkgs.length);
		countLabel.requestLayout();
	}

	private void loadPkg(PS4PKG pkg) {
		updateTitle(pkg);
		pathLabel.setText(pkg.path);
		loadHeaderTable(pkg);
		loadFileTable(pkg);
		loadSfoTable(pkg);
		if (pkg.changelog != null) {
			changelogText.setText(pkg.changelog);
			changelogText.setEnabled(true);
		} else {
			changelogText.setText("");
			changelogText.setEnabled(false);
		}
		updateCountLabel();
	}

	public PkgProperties(Shell parent, PS4PKG[] pkgs, int index) {
		this.shell = new Shell(Display.getCurrent(), SWT.SHELL_TRIM);
		shell.setLayout(new BorderLayout());
		this.pkgs = pkgs;
		this.pkgIndex = index;

		// Create top navigation bar.
		Composite top = new Composite(shell, SWT.NONE);
		top.setLayoutData(new BorderData(SWT.TOP));
		top.setLayout(new GridLayout(4, false));
		Button prevButton = new Button(top, SWT.ARROW | SWT.LEFT);
		prevButton.setText("\u25C4");
		prevButton.addListener(SWT.Selection, e -> {
			this.pkgIndex--;
			if (pkgIndex < 0)
				pkgIndex = pkgs.length - 1;
			loadPkg(pkgs[pkgIndex]);
		});
		Button nextButton = new Button(top, SWT.ARROW | SWT.RIGHT);
		nextButton.setText("\u25BA");
		nextButton.addListener(SWT.Selection, e -> {
			this.pkgIndex++;
			if (pkgIndex >= pkgs.length)
				pkgIndex = 0;
			loadPkg(pkgs[pkgIndex]);
		});
		this.pathLabel = new Label(top, SWT.NONE);
		pathLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		FontData fontData = pathLabel.getFont().getFontData()[0];
		Font font = new Font(shell.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
		pathLabel.setFont(font);
		this.countLabel = new Label(top, SWT.NONE);
		updateCountLabel();

		// Create a tab folder.
		this.tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setLayoutData(new BorderData(SWT.CENTER));

		// Create header tab.
		// TODO: right-click/double-click and select "show this value for all pkgs".
		TabItem headerTabItem = new TabItem(tabFolder, SWT.NONE);
		headerTabItem.setText("PKG Header");
		this.headerTable = new Table(tabFolder, SWT.NONE);
		TableColumn clmnName = new TableColumn(headerTable, SWT.LEFT);
		clmnName.setText("Header Field");
		TableColumn clmnValue = new TableColumn(headerTable, SWT.LEFT);
		clmnValue.setText("Value");
		headerTable.setHeaderVisible(true);
		headerTable.setLinesVisible(true);
		headerTabItem.setControl(headerTable);

		// Create files tab.
		TabItem filesTabItem = new TabItem(tabFolder, SWT.NONE);
		filesTabItem.setText("PKG Files");
		this.fileTable = new Table(tabFolder, SWT.NONE);
		TableColumn id = new TableColumn(fileTable, SWT.LEFT);
		id.setText("ID");
		TableColumn name = new TableColumn(fileTable, SWT.LEFT);
		name.setText("Name");
		TableColumn flags1 = new TableColumn(fileTable, SWT.LEFT);
		flags1.setText("Flags 1");
		TableColumn flags2 = new TableColumn(fileTable, SWT.LEFT);
		flags2.setText("Flags 2");
		TableColumn offset = new TableColumn(fileTable, SWT.RIGHT);
		offset.setText("Offset");
		TableColumn size = new TableColumn(fileTable, SWT.RIGHT);
		size.setText("Size");
		new TableColumn(fileTable, SWT.RIGHT);
		fileTable.setHeaderVisible(true);
		fileTable.setLinesVisible(true);
		filesTabItem.setControl(fileTable);

		// Create param.sfo tab.
		TabItem sfoTabItem = new TabItem(tabFolder, SWT.NONE);
		sfoTabItem.setText("SFO Parameters");
		this.sfoTable = new Table(tabFolder, SWT.NONE);
		TableColumn key = new TableColumn(sfoTable, SWT.LEFT);
		key.setText("Key");
		TableColumn value = new TableColumn(sfoTable, SWT.LEFT);
		value.setText("Value");
		sfoTable.setHeaderVisible(true);
		sfoTable.setLinesVisible(true);
		sfoTabItem.setControl(sfoTable);

		// Create changelog tab.
		TabItem changelogTabItem = new TabItem(tabFolder, SWT.NONE);
		changelogTabItem.setText("Raw Changelog");
		this.changelogText = new Text(tabFolder, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		changelogText.setEditable(false);
		changelogTabItem.setControl(changelogText);

		// Load first PKG.
		loadPkg(pkgs[index]);
		headerTable.pack();
		Point headerTableSize = headerTable.getSize();
		shell.setSize(headerTableSize.x, headerTableSize.y);

		GUI.centerShell(shell);
		shell.open();
	}
}
