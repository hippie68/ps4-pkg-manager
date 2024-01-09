import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
        shell.setLayout(new GridLayout(2, true));
        GridData data;

        Label label = new Label(shell, SWT.NONE);
        label.setText("Enter new tab name:");
        data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Text text = new Text(shell, SWT.BORDER);
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

        Button buttonCancel = new Button(shell, SWT.PUSH);
        buttonCancel.setText("Cancel");
        data = new GridData(GridData.FILL_HORIZONTAL);
        buttonCancel.setLayoutData(data);
        buttonCancel.addListener(SWT.Selection, (e) -> shell.dispose());

        Button buttonOK = new Button(shell, SWT.PUSH);
        buttonOK.setText("OK");
        buttonOK.setLayoutData(data);
        buttonOK.addListener(SWT.Selection, (e) -> {
            this.tabName = text.getText();
            shell.dispose();
        });

        shell.setDefaultButton(buttonOK);
        shell.pack();
        shell.open();

        Display display = getParent().getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();

        return this.tabName;
    }
}
