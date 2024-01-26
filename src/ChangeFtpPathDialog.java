import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

public class ChangeFtpPathDialog {
	public ChangeFtpPathDialog(Shell parent, GUI gui, TabContent tabContent, TableItem[] items) {
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("Select New FTP IP/Port");
		shell.setLayout(new GridLayout(1, false));

		Composite ftp = new Composite(shell, SWT.NONE);
		ftp.setLayout(new GridLayout(2, true));
		Label ipLabel = new Label(ftp, SWT.NONE);
		ipLabel.setText("IP Address");
		Label portLabel = new Label(ftp, SWT.NONE);
		portLabel.setText("Port");
		Combo ipCombo = new Combo(ftp, SWT.DROP_DOWN);
		ipCombo.setItems(Settings.ps4Ips);
		ipCombo.setText(Settings.ps4Ips[0]);
		Combo portCombo = new Combo(ftp, SWT.DROP_DOWN);
		portCombo.setItems(Settings.ps4FtpPorts);
		portCombo.setText(Settings.ps4FtpPorts[0]);

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		buttons.setLayout(new GridLayout(2, true));
		Button cancel = new Button(buttons, SWT.PUSH);
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, e -> shell.close());
		Button ok = new Button(buttons, SWT.PUSH);
		ok.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ok.setText("OK");
		ok.addListener(SWT.Selection, e -> {
			String ip = ipCombo.getText();
			String port = portCombo.getText();

			// Abort if input is invalid.
			String[] inputs = { ip, port };
			for (String input : inputs)
				if (input.isEmpty() || input.contains("/"))
					return;

			List<TableItem> ftpItems = new ArrayList<>(Arrays.asList(items));
			ftpItems.removeIf(item -> !((PS4PKG) item.getData()).path.startsWith("ftp://"));

			for (int i = 0; i < items.length; i++) {
				PS4PKG pkg = (PS4PKG) items[i].getData();
				System.out.println(pkg.path);
				if (pkg.path.startsWith("ftp://")) {
					int rootIndex = pkg.path.indexOf('/', 6);
					if (rootIndex == -1)
						continue;

					String path = "ftp://" + ip + ':' + port + pkg.path.substring(rootIndex);
					pkg.path = path;
					int fileSeparatorIndex = path.lastIndexOf('/');
					pkg.directory = path.substring(0, fileSeparatorIndex);
					pkg.filename = path.substring(fileSeparatorIndex + 1);

					final int index = i;
					Display.getDefault().asyncExec(() -> {
						items[index].setText(Column.PATH.ordinal(), pkg.path);
						items[index].setText(Column.DIRECTORY.ordinal(), pkg.directory);
						items[index].setText(Column.FILENAME.ordinal(), pkg.filename);
					});
				}
			}
			Display.getDefault().asyncExec(() -> gui.updateCurrentSelectionStatus(items));

			shell.close();
		});

		shell.pack();
		GUI.centerShell(shell);
		shell.open();
	}

}
