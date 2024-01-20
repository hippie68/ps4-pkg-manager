import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** General-purpose error pop-up message. */
public class ErrorMessage {

    ErrorMessage(Shell parent, String message) {
        Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("ERROR");
        shell.setLayout(new GridLayout(1, false));
        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new FillLayout(SWT.VERTICAL));
        Label label = new Label(composite, SWT.NONE);
        label.setText(message);
        Button ok = new Button(composite, SWT.PUSH);
        ok.setText("OK");
        ok.addListener(SWT.Selection, e -> shell.close());
        shell.pack();
        GUI.centerShell(shell);
        shell.open();
    }
}
