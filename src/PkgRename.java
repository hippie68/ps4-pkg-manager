import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class PkgRename {
    private static ArrayList<PkgRenamePattern> patterns = new ArrayList<>(List.of(new PkgRenamePattern("Default",
        "%title% [%dlc%] [{v%app_ver%}{ + v%merged_ver%}] [%title_id%] [%release_group%] [%release%] [%backport%]")));

    private Table table;
    private PS4PKG[] pkgs;

    private void updatePreview(String pattern) {
        TableItem[] items = table.getItems();
        for (int i = 0; i < pkgs.length; i++)
            items[i].setText(1, rename(pattern, pkgs[i]));
    }

    public PkgRename(Shell parent, PS4PKG[] pkgs) {
        this.pkgs = pkgs;

        Shell shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Rename");
        shell.setLayout(new BorderLayout());

        // Pattern selection
        // TODO: Combo just contains names; buttons to add new patterns and to edit selected patterns.
        Composite patternComposite = new Composite(shell, SWT.NONE);
        patternComposite.setLayoutData(new BorderData(SWT.TOP));
        patternComposite.setLayout(new GridLayout(3, false));
        Label patternLabel = new Label(patternComposite, SWT.NONE);
        patternLabel.setText("Pattern:");
        Combo patternCombo = new Combo(patternComposite, SWT.READ_ONLY);
        for (PkgRenamePattern pattern : patterns)
            patternCombo.add(pattern.name());
        // TODO
        patternCombo.addListener(SWT.Selection, e -> updatePreview(patterns.get(0).pattern())); // TODO: select
                                                                                                // currently active
                                                                                                // pattern (once
                                                                                                // changing patterns is
                                                                                                // implemented).
        // TODO: make label bold.
        Label currentPatternLabel = new Label(patternComposite, SWT.NONE);
        currentPatternLabel.setText(patterns.get(0).pattern());

        // Create table
        Table table = new Table(shell, SWT.BORDER);
        table.setLayoutData(new BorderData(SWT.CENTER));
        TableColumn filenameColumn = new TableColumn(table, SWT.LEFT);
        filenameColumn.setText("Current file name");
        TableColumn newFilenameColumn = new TableColumn(table, SWT.LEFT);
        newFilenameColumn.setText("New file name");
        table.setHeaderVisible(true);
        for (PS4PKG pkg : pkgs) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, pkg.filename);
        }
        filenameColumn.pack();
        newFilenameColumn.pack();
        table.pack();
        this.table = table;
        // TODO: make preview filenames that are to be renamed bold, make those who don't need to be renamed grayed out.

        // TODO: Rename buttons
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayoutData(new BorderData(SWT.BOTTOM));
        RowLayout buttonCompositeLayout = new RowLayout();
        buttonCompositeLayout.pack = false;
        buttonComposite.setLayout(new RowLayout());
        Button yesButton = new Button(buttonComposite, SWT.PUSH);
        yesButton.setText("Rename");
        yesButton.addListener(SWT.Selection, e -> {
        });
        Button noButton = new Button(buttonComposite, SWT.PUSH);
        noButton.setText("Skip");
        noButton.addListener(SWT.Selection, e -> {
        });
        Button allButton = new Button(buttonComposite, SWT.PUSH);
        allButton.setText("Rename All");
        allButton.addListener(SWT.Selection, e -> {
        });
        Button editButton = new Button(buttonComposite, SWT.PUSH);
        editButton.setText("Edit");
        editButton.addListener(SWT.Selection, e -> {
        });

        // TODO: before renaming a PKG, check if it's incomplete (actual file length does not match the header file
        // length). If it is incomplete, ask the user if he really wants to rename that file.

        shell.pack(); // TODO: set acceptable height and width.
        shell.open();
    }

    // TODO
    private String rename(String pattern, PS4PKG pkg) {
        String result = pattern;

        // %title%
        if (pattern.contains("%title%")) {
            String title = pkg.getSFOValue("TITLE");
            result = result.replaceAll("%title%", title == null ? "" : title);
        }

        // %title_id%
        if (pattern.contains("%title_id%")) {
            String title_id = pkg.getSFOValue("TITLE_ID");
            result = result.replaceAll("%title_id%", title_id == null ? "" : title_id);
        }

        // TODO: all other pattern variables.

        return result;
    }
}
