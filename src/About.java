import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

public class About extends Dialog {
	public About(Shell parent) {
		super(parent, SWT.NONE);
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText("About");
		shell.setLayout(new GridLayout(1, true));
		ShellHelpers.setShellMargin(shell);

		Label titleLabel = createLabel(shell, SWT.CENTER, "PS4 PKG Manager");
		setFontScalingFactor(titleLabel, 2);

		createLabel(shell, SWT.CENTER, "Copyright \u00a9 2025 hippie68");
		createLabel(shell, SWT.CENTER, "Release #" + Version.currentVersion);

		new Label(shell, SWT.NONE);
		Runtime.getRuntime().gc();
		long memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
		createLabel(shell, SWT.LEFT, "RAM usage: " + memoryUsage + " MiB");
		createLink(shell, "Settings and database location: <a>" + GUI.dataDirectory + "</a>",
			e -> Program.launch(GUI.dataDirectory));
		createLink(shell, "Project homepage: <a>https://github.com/hippie68/ps4-pkg-manager</a>",
			e -> Program.launch("https://github.com/hippie68/ps4-pkg-manager"));

		new Label(shell, SWT.NONE);
		ShellHelpers.createDialogButtons(shell, SWT.CENTER, new DialogButton("Close", e -> shell.close()));

		shell.pack();
		ShellHelpers.centerShell(shell);
		shell.open();
	}

	private Label createLabel(Composite parent, int style, String text) {
		Label label = new Label(parent, style);
		label.setText(text);
		label.setLayoutData(new GridData(style, SWT.CENTER, true, false));
		return label;
	}

	private void createLink(Shell shell, String text, Listener listener) {
		Link link = new Link(shell, SWT.NONE);
		link.setText(text);
		link.addListener(SWT.Selection, listener);
	}

	private void setFontScalingFactor(Control control, double factor) {
		FontData[] fd = control.getFont().getFontData();
		fd[0].setHeight((int) Math.round(fd[0].getHeight() * factor));
		fd[0].setStyle(SWT.BOLD);
		control.setFont(new Font(Display.getCurrent(), fd[0]));
		control.getFont().dispose();
	}
}