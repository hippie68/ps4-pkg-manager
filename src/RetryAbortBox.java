import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class RetryAbortBox {
    private final Shell shell;
    private int result = SWT.ABORT;

    public RetryAbortBox(Shell parent, String title, String message) {
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText(title);
        shell.setLayout(new GridLayout(1, false));
        ShellHelpers.setShellMargin(shell);

        new Label(shell, SWT.NONE).setText(message);

        ShellHelpers.createDialogButtons(shell, SWT.RIGHT,
            new DialogButton("Report Bug",
                e -> Program.launch("https://github.com/hippie68/ps4-pkg-manager/issues")),
            new DialogButton("Abort", e -> { result = SWT.ABORT; shell.dispose(); }),
            new DialogButton("Retry", e -> { result = SWT.RETRY; shell.dispose(); })
        );

        shell.pack();
    }

    public int open() {
        ShellHelpers.centerShell(shell);
        shell.open();
        ShellHelpers.runEventLoop(shell);
        return result;
    }
}