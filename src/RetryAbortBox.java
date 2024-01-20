import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class RetryAbortBox {
    private Shell shell;
    private int result = SWT.ABORT;

    public RetryAbortBox(Shell parent, String title, String message) {
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText(title);
        shell.setLayout(new GridLayout(3, true));
        Label messageLabel = new Label(shell, SWT.NONE);
        messageLabel.setText(message);
        messageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

        Button bugButton = new Button(shell, SWT.PUSH);
        bugButton.setText("Report Bug");
        bugButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        bugButton.addListener(SWT.Selection, e -> Program.launch("https://github.com/hippie68/ps4-pkg-manager/issues"));

        Button abortButton = new Button(shell, SWT.PUSH);
        abortButton.setText("Abort");
        abortButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        abortButton.addListener(SWT.Selection, e -> {
            result = SWT.ABORT;
            shell.dispose();
        });

        Button retryButton = new Button(shell, SWT.PUSH);
        retryButton.setText("Retry");
        retryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        retryButton.addListener(SWT.Selection, e -> {
            result = SWT.RETRY;
            shell.dispose();
        });

        shell.setDefaultButton(retryButton);
        shell.pack();
    }

    public int open() {
        GUI.centerShell(shell);
        shell.open();

        Display display = shell.getParent().getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();

        return result;
    }
}
