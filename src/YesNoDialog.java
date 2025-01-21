import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** A general-purpose dialog that asks the user for confirmation. */
public class YesNoDialog {
    private boolean result;
    public Shell shell;

    public YesNoDialog(String title, String question) {
        this.shell = new Shell(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText(title);
        shell.setLayout(new GridLayout(1, false));
        ShellHelpers.setShellMargin(shell);

        new Label(shell, SWT.NONE).setText(question);

        ShellHelpers.createDialogButtons(shell, SWT.RIGHT,
            new DialogButton("No", e -> { result = false; shell.close(); }),
            new DialogButton("Yes", e -> { result = true; shell.close(); }));

        shell.pack();
    }

    /** Displays the dialog and returns true if the user selected "Yes", otherwise false. */
    public boolean open() {
        ShellHelpers.centerShell(shell);
        shell.open();
        ShellHelpers.runEventLoop(shell);
        return result;
    }
}