package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private String S_EXPORT_UPDATE = "exportUpdate";
	private String S_DESTINATION = "destination";
	private String S_EXPORT_SOURCE="exportSource";
	private String S_ZIP_FILENAME = "zipFileName";
		
	private IStructuredSelection selection;
	protected Combo destination;
	protected Combo zipFile;

	protected ExportPart exportPart;
	protected boolean featureExport;
	protected Button zipRadio;
	protected Button updateRadio;
	protected Button browseDirectory;
	protected Button includeSource;

	protected Label directoryLabel;
	
	class ExportListProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}

	class ExportPart extends WizardCheckboxTablePart {
		public ExportPart(String label) {
			super(label);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
		}
	}

	public BaseExportWizardPage(
		IStructuredSelection selection,
		String name,
		String choiceLabel,
		boolean featureExport) {
		super(name);
		this.selection = selection;
		this.featureExport = featureExport;
		exportPart = new ExportPart(choiceLabel);
		setDescription(PDEPlugin.getResourceString("ExportWizard.Plugin.description"));
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		container.setLayout(layout);
		exportPart.createControl(container);
		GridData gd = (GridData) exportPart.getControl().getLayoutData();
		gd.heightHint = 125;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;

		createLabel(container, "", 2);
		createLabel(
			container,
			PDEPlugin.getResourceString(
				featureExport
					? "ExportWizard.Feature.label"
					: "ExportWizard.Plugin.label"),
			2);
		createZipSection(container);
		createUpdateJarsSection(container);
		
		initializeList();
		loadSettings();
		pageChanged();
		hookListeners();
		setControl(container);
		Dialog.applyDialogFont(container);
	}
	
	protected abstract void createUpdateJarsSection(Composite container);
	
	protected abstract void createZipSection(Composite container);

	protected void hookListeners() {
		destination.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		destination.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
		browseDirectory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doBrowseDirectory();
			}
		});
		
		updateRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = updateRadio.getSelection();
				enableZipSection(!enabled);
				enableUpdateJarsSection(enabled);
				pageChanged();
			}
		});

	}
	
	protected void enableUpdateJarsSection(boolean enabled) {
	}

	protected void enableZipSection(boolean enabled) {
	}
	

	protected void createLabel(Composite container, String text, int span) {
		Label label = new Label(container, SWT.NULL);
		label.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
	}

	protected Button createRadioButton(Composite container, String text) {
		Button button = new Button(container, SWT.RADIO);
		button.setText(text);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 0;
		button.setLayoutData(gd);
		return button;
	}

	protected abstract Object[] getListElements();

	protected void initializeList() {
		TableViewer viewer = exportPart.getTableViewer();
		viewer.setContentProvider(new ExportListProvider());
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		exportPart.getTableViewer().setInput(
			PDECore.getDefault().getWorkspaceModelManager());
		checkSelected();
	}

	private void doBrowseDirectory() {
		IPath result = chooseDestination();
		if (result != null) {
			destination.setText(result.toOSString());
		}
	}
	
	private IPath chooseDestination() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(destination.getText());
		dialog.setText(PDEPlugin.getResourceString("ExportWizard.dialog.title"));
		dialog.setMessage(PDEPlugin.getResourceString("ExportWizard.dialog.message"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	protected void checkSelected() {
		Object[] elems = selection.toArray();
		ArrayList checked = new ArrayList(elems.length);

		for (int i = 0; i < elems.length; i++) {
			Object elem = elems[i];
			IProject project = null;

			if (elem instanceof IFile) {
				IFile file = (IFile) elem;
				project = file.getProject();
			} else if (elem instanceof IProject) {
				project = (IProject) elem;
			} else if (elem instanceof IJavaProject) {
				project = ((IJavaProject) elem).getProject();
			}
			if (project != null) {
				IModel model = findModelFor(project);
				if (model != null) {
					checked.add(model);
				}
			}
		}
		exportPart.setSelection(checked.toArray());
	}

	protected IModel findModelFor(IProject project) {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceModel(project);
	}

	protected abstract void pageChanged();

	protected void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean exportUpdate = settings.getBoolean(S_EXPORT_UPDATE);
		zipRadio.setSelection(!exportUpdate);
		updateRadio.setSelection(exportUpdate);
		enableZipSection(!updateRadio.getSelection());
		
		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(S_DESTINATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		destination.setItems((String[]) items.toArray(new String[items.size()]));

		if (!featureExport) {
			includeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
			enableUpdateJarsSection(!zipRadio.getSelection());
			items.clear();
			for (int i = 0; i < 6; i++) {
				String curr = settings.get(S_ZIP_FILENAME + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			zipFile.setItems((String[]) items.toArray(new String[items.size()]));
		}
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_EXPORT_UPDATE, updateRadio.getSelection());
		
		if (includeSource != null)
			settings.put(S_EXPORT_SOURCE, includeSource.getSelection());
			
		if (destination.getText().length() > 0) {
			settings.put(S_DESTINATION + String.valueOf(0), destination.getText());
			String[] items = destination.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(S_DESTINATION + String.valueOf(i + 1), items[i]);
			}
		}
		if (!featureExport && zipFile.getText().length() > 0) {
			settings.put(S_ZIP_FILENAME + String.valueOf(0), zipFile.getText());
			String[] items = zipFile.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(S_ZIP_FILENAME + String.valueOf(i+1), items[i]);
			}
		}
	}

	public Object[] getSelectedItems() {
		return exportPart.getSelection();
	}

	public boolean getExportZip() {
		return zipRadio.getSelection();
	}
	
	public boolean getExportSource() {
		if (includeSource == null)
			return false;
		return includeSource.getSelection();
	}

	public String getDestination() {
		if (destination == null || destination.isDisposed())
			return "";
		return destination.getText();
	}
	
	public String getFileName() {
		return null;
	}	
}
