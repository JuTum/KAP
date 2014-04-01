package org.deidentifier.arx.gui.view.impl.wizard.importdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deidentifier.arx.DataType;
import org.deidentifier.arx.io.CSVDataInput;
import org.deidentifier.arx.io.importdata.CSVConfiguration;
import org.deidentifier.arx.io.importdata.CSVImportAdapter;
import org.deidentifier.arx.io.importdata.Column;
import org.deidentifier.arx.io.importdata.DataSourceImportAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;


public class CsvPage extends WizardPage {

    private ImportDataWizard wizardImport;

    private ArrayList<WizardColumn> wizardColumns;

    private Label lblLocation;
    private Combo comboLocation;
    private Button btnChoose;
    private Button btnContainsHeader;
    private Combo comboSeparator;
    private Label lblSeparator;
    private Table tablePreview;
    private TableViewer tableViewerPreview;

    private int selection;
    private final char[] separators = {';', ',', '|', '\t'};
    private final String[] labels = {";", ",", "|", "Tab"};
    private boolean customSeparator;


    public CsvPage(ImportDataWizard wizardImport)
    {

        super("WizardImportCsvPage");

        setTitle("CSV");
        setDescription("Please provide the information requested below");

        this.wizardImport = wizardImport;

    }

    public void createControl(Composite parent)
    {

        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(3, false));

        lblLocation = new Label(container, SWT.NONE);
        lblLocation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLocation.setText("Location");

        comboLocation = new Combo(container, SWT.READ_ONLY);
        comboLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboLocation.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {

                customSeparator = false;
                evaluatePage();

            }

        });

        btnChoose = new Button(container, SWT.NONE);
        btnChoose.setText("Browse...");
        btnChoose.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {

                setPageComplete(false);
                setErrorMessage(null);

                final String path = wizardImport.getController().actionShowOpenFileDialog("*.csv");

                if (path == null) {

                    return;

                }

                if (comboLocation.indexOf(path) == -1) {

                    comboLocation.add(path, 0);

                }

                comboLocation.select(comboLocation.indexOf(path));
                customSeparator = false;
                evaluatePage();

            }

        });

        lblSeparator = new Label(container, SWT.NONE);
        lblSeparator.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblSeparator.setText("Separator");

        comboSeparator = new Combo(container, SWT.READ_ONLY);

        for (final String s : labels) {

            comboSeparator.add(s);

        }

        comboSeparator.select(selection);
        comboSeparator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboSeparator.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {

                selection = comboSeparator.getSelectionIndex();
                customSeparator = true;
                evaluatePage();

            }

        });

        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        btnContainsHeader = new Button(container, SWT.CHECK);
        btnContainsHeader.setText("First row contains column names");
        btnContainsHeader.setSelection(true);
        btnContainsHeader.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {

                evaluatePage();

            }

        });

        new Label(container, SWT.NONE);

        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        tableViewerPreview = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        tableViewerPreview.setContentProvider(new ArrayContentProvider());

        tablePreview = tableViewerPreview.getTable();
        GridData gd_tablePreview = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
        gd_tablePreview.heightHint = 150;
        tablePreview.setLayoutData(gd_tablePreview);
        tablePreview.setLinesVisible(true);
        tablePreview.setVisible(false);

        setPageComplete(false);

    }

    private void detectSeparator(final String file) throws IOException {

        final BufferedReader r = new BufferedReader(new FileReader(new File(file)));

        int count = 0;
        String line = r.readLine();
        final Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        while ((count < ImportData.previewDataMaxLines) && (line != null)) {

            final char[] a = line.toCharArray();

            for (final char c : a) {

                for (int i = 0; i < separators.length; i++) {

                    if (c == separators[i]) {

                        if (!map.containsKey(i)) {

                            map.put(i, 0);

                        }

                        map.put(i, map.get(i) + 1);

                    }

                }

            }

            line = r.readLine();

            count++;

        }

        r.close();

        if (map.isEmpty()) {

            return;

        }

        int max = Integer.MIN_VALUE;

        for (final int key : map.keySet()) {

            if (map.get(key) > max) {

                max = map.get(key);
                selection = key;

            }

        }

    }

    private void readPreview(String string) throws Exception {

        /*
         * Parameters from the user interface
         */
        final String location = comboLocation.getText();
        final char separator = separators[selection];
        final boolean containsHeader = btnContainsHeader.getSelection();

        /*
         * Variables needed for processing
         */
        final CSVDataInput in = new CSVDataInput(location, separator);
        final Iterator<String[]> it = in.iterator();
        final ArrayList<String[]> previewData = new ArrayList<String[]>();
        final String[] firstLine;

        /*
         * Check whether there is at least one line in file and retrieve it
         */
        if (it.hasNext()) {

            firstLine = it.next();

        } else {

            throw new Exception("No data in file");

        }

        /*
         * Initialize {@link #allColumns}
         */
        wizardColumns = new ArrayList<WizardColumn>();
        List<Column> columns = new ArrayList<Column>();

        /*
         * Iterate over columns and add it to {@link #allColumns}
         */
        for (int i = 0; i < firstLine.length; i++) {

            Column column = new Column(i, DataType.STRING);
            WizardColumn wizardColumn = new WizardColumn(column);

            wizardColumns.add(wizardColumn);
            columns.add(column);

        }

        /*
         * Create import adapter and pass detected columns to it
         */
        CSVConfiguration config = new CSVConfiguration(location, separator, containsHeader);
        for (Column c : columns) {
            config.addColumn(c);
        }
        
        DataSourceImportAdapter importAdapter = DataSourceImportAdapter.create(config);
        
        /*
         * Get up to {ImportData#previewDataMaxLines} lines for previewing
         */
        int count = 0;
        while (importAdapter.hasNext() && (count <= ImportData.previewDataMaxLines)) {

            previewData.add(importAdapter.next());
            count++;

        }

        in.close();

        /*
         * Remove first entry as it contains name of columns
         */
        previewData.remove(0);

        if (previewData.size() == 0) {

            throw new Exception("No preview data in file");

        }

        tablePreview.setRedraw(false);

        while (tablePreview.getColumnCount() > 0) {

            tablePreview.getColumns()[0].dispose();

        }

        class CSVColumnLabelProvider extends ColumnLabelProvider {

            private int column;

            public CSVColumnLabelProvider(int column) {

                this.column = column;

            }

            @Override
            public String getText(Object element) {

                return ((String[]) element)[column];

            }

            @Override
            public String getToolTipText(Object element) {

                int row = previewData.indexOf(element);

                return "Row: " + (row + 1) + ", Column: " + (column + 1);

            }

        }

        for (WizardColumn column : wizardColumns) {

            TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewerPreview, SWT.NONE);
            tableViewerColumn.setLabelProvider(new CSVColumnLabelProvider(column.getColumn().getIndex()));

            TableColumn tableColumn = tableViewerColumn.getColumn();
            tableColumn.setWidth(100);

            if (btnContainsHeader.getSelection()) {

                tableColumn.setText(column.getColumn().getName());
                tableColumn.setToolTipText("Column #" + column.getColumn().getIndex());

            }

            ColumnViewerToolTipSupport.enableFor(tableViewerPreview, ToolTip.NO_RECREATE);

        }

        this.wizardImport.getData().setWizardColumns(wizardColumns);

        if (btnContainsHeader.getSelection()) {

            tablePreview.setHeaderVisible(true);

        } else {

            tablePreview.setHeaderVisible(false);

        }

        tableViewerPreview.setInput(previewData);
        wizardImport.getData().setPreviewData(previewData);

        tablePreview.setVisible(true);
        tablePreview.layout();
        tablePreview.setRedraw(true);

    }

    private void evaluatePage() {

        setPageComplete(false);
        setErrorMessage(null);

        if (comboLocation.getText().equals("")) {

            return;

        }

        try {

            if (!customSeparator) {

                detectSeparator(comboLocation.getText());
                comboSeparator.select(selection);

            }

            readPreview(comboLocation.getText());

        } catch (Exception e) {

            setErrorMessage("Error while trying to access the file");

            return;

        }

        wizardImport.getData().setWizardColumns(wizardColumns);
        wizardImport.getData().setFirstRowContainsHeader(btnContainsHeader.getSelection());
        wizardImport.getData().setFileLocation(comboLocation.getText());
        wizardImport.getData().setCsvSeparator(separators[selection]);

        setPageComplete(true);

    }

}