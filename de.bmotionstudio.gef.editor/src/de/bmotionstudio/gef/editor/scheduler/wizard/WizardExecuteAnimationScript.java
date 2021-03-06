/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.gef.editor.scheduler.wizard;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import de.bmotionstudio.gef.editor.BMotionStudioImage;
import de.bmotionstudio.gef.editor.EditorImageRegistry;
import de.bmotionstudio.gef.editor.edit.PredicateEditingSupport;
import de.bmotionstudio.gef.editor.model.BControl;
import de.bmotionstudio.gef.editor.scheduler.AnimationScriptObject;
import de.bmotionstudio.gef.editor.scheduler.ExecuteAnimationScript;
import de.bmotionstudio.gef.editor.scheduler.SchedulerEvent;
import de.bmotionstudio.gef.editor.scheduler.SchedulerWizard;

public class WizardExecuteAnimationScript extends SchedulerWizard {

	private class SchedulerPage extends WizardPage {

		private TableViewer tableViewer;

		protected SchedulerPage(String pageName) {
			super(pageName);
		}

		public void createControl(final Composite parent) {

			DataBindingContext dbc = new DataBindingContext();

			Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(new GridLayout(1, true));

			setControl(container);

			tableViewer = new TableViewer(container, SWT.BORDER
					| SWT.FULL_SELECTION);
			tableViewer.getTable().setLinesVisible(true);
			tableViewer.getTable().setHeaderVisible(true);
			tableViewer.getTable().setLayoutData(
					new GridData(GridData.FILL_BOTH));
			tableViewer.getTable().setFont(
					new Font(Display.getDefault(), new FontData("Arial", 10,
							SWT.NONE)));

			TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.NONE);
			column.getColumn().setText("Predicate");
			column.getColumn().setWidth(225);
			column.setEditingSupport(new PredicateEditingSupport(tableViewer,
					dbc, "predicate", getBControl().getVisualization(),
					getShell()));

			column = new TableViewerColumn(tableViewer, SWT.NONE);
			column.getColumn().setText("Edit");
			column.getColumn().setWidth(225);
			column.setEditingSupport(new AnimationScriptEditingSupport(
					tableViewer));

			ObservableListContentProvider contentProvider = new ObservableListContentProvider();
			tableViewer.setContentProvider(contentProvider);

			tableViewer.setLabelProvider(new ObservableMapLabelProvider(
					BeansObservables.observeMaps(
							contentProvider.getKnownElements(),
							new String[] { "predicate" })) {

				@Override
				public String getColumnText(Object element, int columnIndex) {
					if (columnIndex == 1) {
						return "Edit Scheduler";
					}
					return super.getColumnText(element, columnIndex);
				}

				@Override
				public Image getColumnImage(Object element, int columnIndex) {
					return null;
				}

			});

			final WritableList input = new WritableList(
					((ExecuteAnimationScript) getScheduler()).getList(),
					AnimationScriptObject.class);
			tableViewer.setInput(input);

			Composite comp = new Composite(container, SWT.NONE);
			comp.setLayout(new RowLayout());
			comp.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			Button btRemove = new Button(comp, SWT.PUSH);
			btRemove.setText("Remove");
			btRemove.setImage(BMotionStudioImage
					.getImage(EditorImageRegistry.IMG_ICON_DELETE));
			btRemove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (tableViewer.getSelection().isEmpty()) {
						return;
					}
					AnimationScriptObject obj = (AnimationScriptObject) ((IStructuredSelection) tableViewer
							.getSelection()).getFirstElement();
					input.remove(obj);
				}
			});

			Button btAdd = new Button(comp, SWT.PUSH);
			btAdd.setText("Add");
			btAdd.setImage(BMotionStudioImage
					.getImage(EditorImageRegistry.IMG_ICON_ADD));
			btAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AnimationScriptObject obj = new AnimationScriptObject("");
					input.add(obj);
				}
			});

		}
	}

	public WizardExecuteAnimationScript(BControl bcontrol,
			SchedulerEvent scheduler) {
		super(bcontrol, scheduler);
		addPage(new SchedulerPage("SchedulerPage"));
	}

	@Override
	protected Boolean prepareToFinish() {
		return true;
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public Point getSize() {
		return new Point(600, 500);
	}

	private class AnimationScriptEditingSupport extends EditingSupport {

		public AnimationScriptEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new AnimationScriptDialogCellEditor((Composite) getViewer()
					.getControl(), (AnimationScriptObject) element);
		}

		@Override
		protected Object getValue(Object element) {
			return "Edit Scheduler";
		}

		@Override
		protected void setValue(Object element, Object value) {
		}

	}

	private class AnimationScriptDialogCellEditor extends DialogCellEditor {

		private AnimationScriptObject animObj;

		public AnimationScriptDialogCellEditor(Composite parent,
				AnimationScriptObject animObj) {
			super(parent);
			this.animObj = animObj;
		}

		@Override
		protected Object openDialogBox(Control cellEditorWindow) {
			AnimationScriptObjectDialog dialog = new AnimationScriptObjectDialog(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), getBControl(), animObj);
			if (dialog.open() == Dialog.OK) {
				return getValue();
			}
			return null;
		}

	}

}
