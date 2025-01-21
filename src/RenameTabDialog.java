import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RenameTabDialog extends Dialog {
    Shell shell;
    String tabName;

    public RenameTabDialog(Shell parent, String text) {
        super(parent);
        this.tabName = text;
    }

    public String open() {
        Shell shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Rename Tab");
        shell.setLayout(new GridLayout(1, false));

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(2, true));
        GridData data;

        Label label = new Label(composite, SWT.NONE);
        label.setText("Enter new tab name:");
        data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Text text = new Text(composite, SWT.BORDER);
        text.setText(this.tabName);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        text.setLayoutData(data);
        text.selectAll();
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR) {
                    tabName = text.getText();
                    shell.dispose();
                }
            }
        });

        Button cancel = new Button(composite, SWT.PUSH);
        cancel.setText("Cancel");
        data = new GridData(GridData.FILL_HORIZONTAL);
        cancel.setLayoutData(data);
        cancel.addListener(SWT.Selection, e -> shell.dispose());

        Button ok = new Button(composite, SWT.PUSH);
        ok.setText("OK");
        ok.setLayoutData(data);
        ok.addListener(SWT.Selection, e -> {
            this.tabName = text.getText();
            shell.dispose();
        });

        shell.setDefaultButton(ok);
        shell.pack();
        ShellHelpers.centerShell(shell);
        shell.open();

        Display display = getParent().getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();

        return this.tabName;
    }
}