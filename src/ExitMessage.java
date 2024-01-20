import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Displays a message while hiding the rest of the UI. Supposed to be called when the program is about to exit. */
public class ExitMessage {
    Shell shell;

    public ExitMessage(Shell parent, String message) {
        this.shell = new Shell(parent, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
        shell.setText(GUI.PROGRAM_NAME);
        shell.setLayout(new GridLayout(1, true));

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new RowLayout());
        Label label = new Label(composite, SWT.NONE);
        label.setText(message);

        shell.pack();
        GUI.centerShell(shell);
        parent.setVisible(false);
        shell.open();

        while (Display.getCurrent().readAndDispatch())
            ;
    }

    public void close() {
        shell.close();
    }
}
