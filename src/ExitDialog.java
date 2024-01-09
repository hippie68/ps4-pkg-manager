import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ExitDialog {
    public ExitDialog(Shell parent) {
        Shell shell = new Shell(parent, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
        shell.setText("Please Wait");
        shell.setLayout(new GridLayout(1, true));

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new RowLayout());
        Label label = new Label(composite, SWT.NONE);
        label.setText("Saving application state...");

        shell.pack();
        shell.open();

        while (Display.getCurrent().readAndDispatch())
            ;
    }
}
