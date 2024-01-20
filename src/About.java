import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class About extends Dialog {
    private Shell shell;
    private Composite composite;

    public About(Shell parent, int style, GUI gui) {
        super(parent, SWT.NONE);
        this.shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("About");
        shell.setLayout(new GridLayout(1, false));

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        this.composite = composite;

        Label titleLabel = createCenteredLabel("PS4 PKG Manager");
        setFontScalingFactor(titleLabel, 2);

        createCenteredLabel("Copyright (c) 2024 hippie68");
        createCenteredLabel("Beta Release #" + Version.currentVersion);

        new Label(composite, SWT.NONE);

        createLink("Settings and database location: <a>" + gui.dataDirectory + "</a>",
            e -> Program.launch(gui.dataDirectory));
        createLink("Project homepage: <a>https://github.com/hippie68/ps4-pkg-manager</a>",
            e -> Program.launch("https://github.com/hippie68/ps4-pkg-manager"));

        while (Display.getCurrent().readAndDispatch())
            ;

        shell.pack();
        GUI.centerShell(shell);
        shell.open();
    }

    private Label createCenteredLabel(String text) {
        Label label = new Label(composite, SWT.CENTER);
        label.setText(text);
        label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        return label;
    }

    private Link createLink(String text, Listener listener) {
        Link link = new Link(composite, SWT.NONE);
        link.setText(text);
        link.addListener(SWT.Selection, listener);
        return link;
    }

    private void setFontScalingFactor(Control control, double factor) {
        FontData[] fd = control.getFont().getFontData();
        fd[0].setHeight((int) Math.round(fd[0].getHeight() * factor));
        fd[0].setStyle(SWT.BOLD);
        control.setFont(new Font(Display.getCurrent(), fd[0]));
        control.getFont().dispose();
        // TODO: is everything properly disposed?
    }
}
