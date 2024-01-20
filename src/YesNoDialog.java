
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        Label label = new Label(composite, SWT.NONE);
        label.setText(question);

        Composite buttons = new Composite(shell, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        buttons.setLayout(new GridLayout(2, true));

        Button no = new Button(buttons, SWT.PUSH);
        no.setText("No");
        no.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        no.addListener(SWT.Selection, e -> {
            result = false;
            shell.close();
        });

        Button yes = new Button(buttons, SWT.PUSH);
        yes.setText("Yes");
        yes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        yes.addListener(SWT.Selection, e -> {
            this.result = true;
            shell.close();
        });

        shell.setDefaultButton(yes);
        shell.pack();
    }

    /** Displays the dialog and returns true if the user selected "Yes", otherwise false. */
    public boolean open() {
        GUI.centerShell(shell);
        shell.open();

        Display display = shell.getParent().getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();

        return result;
    }
}
