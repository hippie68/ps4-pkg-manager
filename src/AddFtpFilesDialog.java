import java.nio.ByteBuffer;
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
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class AddFtpFilesDialog {
	static final String title = "Add FTP Files";
	Thread ftpThread;
	Label connectionStatus;
	ProgressBar progressBar;

	// TODO: extract long code blocks to separate methods.
	public AddFtpFilesDialog(Shell parent, GUI gui) {
		TabContent tabContent = gui.getCurrentTabContent();

		Shell shell = new Shell(parent, SWT.MIN | SWT.CLOSE);
		shell.setText(title);
		shell.setLayout(new GridLayout(1, false));
		shell.addListener(SWT.Close, e -> {
			if (ftpThread != null)
				ftpThread.interrupt();
		});

		Composite ftp = new Composite(shell, SWT.NONE);
		ftp.setLayout(new GridLayout(2, true));
		Label ipLabel = new Label(ftp, SWT.NONE);
		ipLabel.setText("PS4 IP Address");
		Label portLabel = new Label(ftp, SWT.NONE);
		portLabel.setText("PS4 Port");
		Combo ipCombo = new Combo(ftp, SWT.DROP_DOWN);
		ipCombo.setItems(Settings.ps4Ips);
		ipCombo.setText(Settings.ps4Ips[0]);
		ipCombo.addListener(SWT.Selection, e -> {
			// Make the selected IP the new default IP.
			String defaultIp = ipCombo.getText();
			String[] items = ipCombo.getItems();
			String[] ips = Arrays.copyOf(items, items.length);
			for (int i = 0; i < ips.length; i++)
				if (ips[i].equals(defaultIp)) {
					ips[i] = ips[0];
					ips[0] = defaultIp;
					Settings.ps4Ips = ips;
					return;
				}
		});
		Combo portCombo = new Combo(ftp, SWT.DROP_DOWN);
		portCombo.setItems(Settings.ps4FtpPorts);
		portCombo.setText(Settings.ps4FtpPorts[0]);
		portCombo.addListener(SWT.Selection, e -> {
			// Make the selected port the new default port.
			String defaultPort = portCombo.getText();
			String[] items = portCombo.getItems();
			String[] ports = Arrays.copyOf(items, items.length);
			for (int i = 0; i < ports.length; i++)
				if (ports[i].equals(defaultPort)) {
					ports[i] = ports[0];
					ports[0] = defaultPort;
					Settings.ps4FtpPorts = ports;
					return;
				}
		});

		Composite status = new Composite(shell, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		status.setLayout(new GridLayout(1, false));
		new Label(status, SWT.NONE).setText(String.format("Target tab: \"%s\"", tabContent.getName()));

		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		buttons.setLayout(new GridLayout(2, true));
		Button cancel = new Button(buttons, SWT.PUSH);
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, e -> shell.close());
		Button connect = new Button(buttons, SWT.PUSH);
		connect.setText("Connect");
		connect.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		connect.addListener(SWT.Selection, e -> {
			ipCombo.setEnabled(false);
			portCombo.setEnabled(false);
			connect.setEnabled(false);
			if (connectionStatus == null) {
				this.connectionStatus = new Label(status, SWT.NONE);
				connectionStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				this.progressBar = new ProgressBar(status, SWT.HORIZONTAL);
				progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				progressBar.setMinimum(0);
			}
			connectionStatus.setText("Connecting...");
			shell.pack();

			String ip = ipCombo.getText();
			int port = Integer.parseInt(portCombo.getText());
			this.ftpThread = new Thread(() -> {
				FtpClient client = new FtpClient();
				try {
					// Find all PKGs on the PS4 and put them in the current tab.
					client.connect(ip, port);
					client.login();
					client.type(true);
					Display.getDefault().asyncExec(() -> connectionStatus.setText("Searching for installed PKGs..."));

					String regex = "[A-Z]{4}[0-9]{5}";
					List<String> appDirs = client.getDirectories("/user/app", regex);
					appDirs.addAll(client.getDirectories("/mnt/ext0/user/app", regex));
					List<String> patchDirs = client.getDirectories("/user/patch", regex);
					patchDirs.addAll(client.getDirectories("/mnt/ext0/user/patch", regex));
					List<String> addcontDirs = client.getDirectories("/user/addcont", regex);
					addcontDirs.addAll(client.getDirectories("/mnt/ext0/user/addcont", regex));
					List<String> dlcDirs = new ArrayList<>();
					for (String addcontDir : addcontDirs)
						dlcDirs.addAll(client.getDirectories(addcontDir, null));

					// DEBUG
					// appDirs.forEach(dir -> System.out.println(dir));
					// patchDirs.forEach(dir -> System.out.println(dir));
					// dlcDirs.forEach(dir -> System.out.println(dir));

					List<String> list = new ArrayList<String>();
					list.addAll(appDirs);
					list.addAll(patchDirs);
					list.addAll(dlcDirs);

					List<String> pkgFiles = new ArrayList<String>();
					for (String dir : list)
						pkgFiles.addAll(client.getFiles(dir, ".pkg"));
					// System.out.println("List size: " + list.size()); // DEBUG
					Display.getDefault().asyncExec(() -> {
						if (!shell.isDisposed())
							progressBar.setMaximum(list.size());
					});

					// DEBUG
					// for (String pkgFile : pkgFiles)
					// System.out.println(pkgFile);

					// Get all PKG files' metadata.
					Display.getDefault().asyncExec(() -> {
						if (!shell.isDisposed())
							connectionStatus.setText(String.format("Adding %d PKG files...", pkgFiles.size()));
					});
					int pkgCount = 0;
					for (String pkgFile : pkgFiles) {
						PS4PKG pkg;

						// Update progress status.
						final int i = ++pkgCount;
						final int percentage = (int) Math.ceil(100 * i / pkgFiles.size());
						Display.getDefault().asyncExec(() -> {
							if (!shell.isDisposed()) {
								shell.setText(String.format("%d%% - %s", percentage, title));
								progressBar.setSelection(i);
							}
						});

						// First determine the required data's size...
						if (tabContent.isDisposed() || ftpThread.isInterrupted())
							throw new InterruptedException();
						byte[] pkgHeader = client.downloadByteArray(pkgFile, 0x1000);
						ByteBuffer bb = ByteBuffer.wrap(pkgHeader);
						long body_offset = bb.getLong(0x20);
						long body_size = bb.getLong(0x28);
						int len = (int) (body_offset + body_size);

						// ...then download the data.
						if (tabContent.isDisposed() || ftpThread.isInterrupted())
							throw new InterruptedException();
						byte[] pkgBody = client.downloadByteArray(pkgFile, len);
						try {
							pkg = new PS4PKG(pkgBody);

							String prefix = "ftp://" + ip + ':' + port;
							pkg.path = prefix + pkgFile;
							int separator = pkgFile.lastIndexOf('/');
							pkg.directory = prefix + pkgFile.substring(0, separator);
							pkg.filename = pkgFile.substring(separator + 1);
							Display.getDefault().asyncExec(() -> {
								if (!tabContent.isDisposed())
									tabContent.addPkg(pkg);
							}); // TODO: sometimes not sorted by time of adding.
						} catch (Exception ignore) {
							ignore.printStackTrace();
						}

						// Disconnect if the user pressed "Cancel".
						if (ftpThread.isInterrupted())
							break;
					}

					// Done.
					if (pkgCount == pkgFiles.size()) {
						Display.getDefault().asyncExec(() -> {
							if (!shell.isDisposed()) {
								connectionStatus.setText("Done.");
								cancel.dispose();
								int widthHint = connect.getSize().x;
								connect.dispose();
								Button ok = new Button(buttons, SWT.PUSH);
								ok.setText("OK");
								GridData okLayoutData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1);
								okLayoutData.widthHint = widthHint;
								ok.setLayoutData(okLayoutData);
								ok.addListener(SWT.Selection, event -> shell.close());
								shell.setDefaultButton(ok);
								ok.requestLayout();
							}
						});
						return;
					}
				} catch (InterruptedException ie) {
					Display.getDefault().asyncExec(() -> {
						if (!shell.isDisposed())
							shell.close();
					});
				} catch (Exception r) {
					Display.getDefault().asyncExec(() -> {
						if (!shell.isDisposed()) {
							connectionStatus.setText("Connection error.");
							ipCombo.setEnabled(true);
							portCombo.setEnabled(true);
							connect.setText("Retry");
							connect.setEnabled(true);
							shell.layout();
						}
					});

					r.printStackTrace();
				} finally {
					client.disconnect();
				}
			});
			ftpThread.setDaemon(true);
			ftpThread.start();
		});

		shell.setDefaultButton(connect);
		shell.pack();
		ShellHelpers.centerShell(shell);
		shell.open();
	}
}