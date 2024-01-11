import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** A dialog that makes sure the user really wants to remove items from a list. */
public class RemoveFromListDialog {
    private static long timeStamp = 0;
    private boolean result;
    public Shell shell;

    public RemoveFromListDialog() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < timeStamp)
            return;

        this.shell = new Shell(Display.getCurrent().getActiveShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Remove From List");
        shell.setLayout(new GridLayout(1, false));

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        Label label = new Label(composite, SWT.NONE);
        label.setText("Are you sure you want to remove these items?\nThis cannot be undone.\n");

        Button checkbox = new Button(composite, SWT.CHECK);
        checkbox.setText("Do not ask again for the next 15 minutes.");

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
            if (checkbox.getSelection() == true)
                timeStamp = System.currentTimeMillis() + 15 * 60 * 1000;
            result = true;
            shell.close();
        });

        shell.setDefaultButton(yes);
        shell.pack();
    }

    public boolean open() {
        if (shell == null)
            return true;

        Platform.centerShell(shell);
        shell.open();

        Display display = shell.getParent().getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();

        return result;
    }
}
