/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

public abstract class BuildPathBasePage {

	private final ClasspathAttributeConfigurationDescriptors fAttributeDescriptors;

	public BuildPathBasePage() {
		fAttributeDescriptors= JavaPlugin.getDefault().getClasspathAttributeConfigurationDescriptors();
	}

	protected boolean editCustomAttribute(Shell shell, CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			IClasspathAttribute result= config.performEdit(shell, elem.getClasspathAttributeAccess());
			if (result != null) {
				elem.setValue(result.getValue());
				return true;
			}
		}
		return false;
	}

	protected boolean showAddExportDialog(Shell shell, CPListElementAttribute elem) {
		CPListElement selElement= elem.getParent();
		// the element targeted by the CP entry will be the source module from which packages are exported:
		IJavaElement[] selectedJavaElements= ModuleAddExport.getTargetJavaElements(selElement.getJavaProject(), selElement.getPath());
		if (selectedJavaElements == null) {
			MessageDialog dialog= new MessageDialog(shell, NewWizardMessages.BuildPathBasePage_notAddedQuestion_title, null,
					Messages.format(NewWizardMessages.BuildPathBasePage_notAddedQuestion_description, selElement.getPath().toString()),
					MessageDialog.QUESTION, 0,
					NewWizardMessages.BuildPathBasePage_addNow_button,
					NewWizardMessages.BuildPathBasePage_proceedWithoutAdding_button,
					NewWizardMessages.BuildPathBasePage_cancel_button);
			switch (dialog.open()) {
				case 0: // Add now ...
					try {
						selectedJavaElements= persistEntry(selElement);
					} catch (InvocationTargetException e) {
						ExceptionHandler.handle(e, shell, PreferencesMessages.BuildPathsPropertyPage_error_title, PreferencesMessages.BuildPathsPropertyPage_error_message);
						return false;
					} catch (InterruptedException e) {
						return false;
					}
					break;
				case 1: // Process without adding ...
					break;
				case 2: // Cancel
					return false;
				default:
					throw new IllegalStateException(NewWizardMessages.BuildPathBasePage_unexpectedAnswer_error);
			}			
		}
		ModuleDialog dialog= new ModuleDialog(shell, selElement, selectedJavaElements);
		int res= dialog.open();
		if (res == Window.OK) {
			ModuleAddExport[] newExports= dialog.getAddExports();
			elem.setValue(newExports);
			return true;
		}
		return false;
	}

	private IJavaElement[] persistEntry(CPListElement element) throws InterruptedException, InvocationTargetException {
		// NB: we assume that element is a *new* entry
		IJavaElement[] selectedJavaElements;
		IJavaProject javaProject= element.getJavaProject();
		IClasspathEntry newEntry= element.getClasspathEntry();
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				IClasspathEntry[] oldClasspath= javaProject.getRawClasspath();
				int nEntries= oldClasspath.length;
				IClasspathEntry[] newEntries= Arrays.copyOf(oldClasspath, nEntries+1);
				newEntries[nEntries]= newEntry;
				javaProject.setRawClasspath(newEntries, monitor);
			}
		};
		PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));
		selectedJavaElements= ModuleAddExport.getTargetJavaElements(element.getJavaProject(), element.getPath());
		return selectedJavaElements;
	}

	protected boolean removeCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			IClasspathAttribute result= config.performRemove(elem.getClasspathAttributeAccess());
			if (result != null) {
				elem.setValue(result.getValue());
				return true;
			}
		}
		return false;
	}

	protected void removeAddExport(ModuleAddExport export) {
		CPListElementAttribute parent= export.getParent();
		if (parent != null) {
			Object value= parent.getValue();
			if (value instanceof ModuleAddExport[]) {
				ModuleAddExport[] existingExports= (ModuleAddExport[]) value;
				int count= 0;
				for (int j= 0; j < existingExports.length; j++) {
					ModuleAddExport anExport= existingExports[j];
					if (anExport != export)
						existingExports[count++]= anExport;
				}
				if (count < existingExports.length) {
					ModuleAddExport[] newExports= new ModuleAddExport[count];
					System.arraycopy(existingExports, 0, newExports, 0, count);
					parent.setValue(newExports);
					parent.getParent().attributeChanged(CPListElement.MODULE);
				}
			}
		}
	}

	protected boolean canEditCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			return config.canEdit(elem.getClasspathAttributeAccess());
		}
		return false;
	}

	protected boolean canRemoveCustomAttribute(CPListElementAttribute elem) {
		ClasspathAttributeConfiguration config= fAttributeDescriptors.get(elem.getKey());
		if (config != null) {
			return config.canRemove(elem.getClasspathAttributeAccess());
		}
		return false;
	}


	public abstract List<?> getSelection();
	public abstract void setSelection(List<?> selection, boolean expand);


	/**
	 * Adds an element to the page
	 *
	 * @param element the element to add
	 */
	public void addElement(CPListElement element) {
		// default implementation does nothing
	}

	public abstract boolean isEntryKind(int kind);

	protected void filterAndSetSelection(List<?> list) {
		ArrayList<Object> res= new ArrayList<>(list.size());
		for (int i= list.size()-1; i >= 0; i--) {
			Object curr= list.get(i);
			if (curr instanceof CPListElement) {
				CPListElement elem= (CPListElement) curr;
				if (elem.getParentContainer() == null && isEntryKind(elem.getEntryKind())) {
					res.add(curr);
				}
			}
		}
		setSelection(res, false);
	}

	public static void fixNestingConflicts(CPListElement[] newEntries, CPListElement[] existing, Set<CPListElement> modifiedSourceEntries) {
		for (int i= 0; i < newEntries.length; i++) {
			addExclusionPatterns(newEntries[i], existing, modifiedSourceEntries);
		}
	}

	private static void addExclusionPatterns(CPListElement newEntry, CPListElement[] existing, Set<CPListElement> modifiedEntries) {
		IPath entryPath= newEntry.getPath();
		for (int i= 0; i < existing.length; i++) {
			CPListElement curr= existing[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath currPath= curr.getPath();
				if (!currPath.equals(entryPath)) {
					if (currPath.isPrefixOf(entryPath)) {
						if (addToExclusions(entryPath, curr)) {
							modifiedEntries.add(curr);
						}
					} else if (entryPath.isPrefixOf(currPath) && newEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if (addToExclusions(currPath, newEntry)) {
							modifiedEntries.add(curr);
						}
					}
				}
			}
		}
	}

	private static boolean addToExclusions(IPath entryPath, CPListElement curr) {
		IPath[] exclusionFilters= (IPath[]) curr.getAttribute(CPListElement.EXCLUSION);
		if (!JavaModelUtil.isExcludedPath(entryPath, exclusionFilters)) {
			IPath pathToExclude= entryPath.removeFirstSegments(curr.getPath().segmentCount()).addTrailingSeparator();
			IPath[] newExclusionFilters= new IPath[exclusionFilters.length + 1];
			System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0, exclusionFilters.length);
			newExclusionFilters[exclusionFilters.length]= pathToExclude;
			curr.setAttribute(CPListElement.EXCLUSION, newExclusionFilters);
			return true;
		}
		return false;
	}

	protected boolean containsOnlyTopLevelEntries(List<?> selElements) {
		if (selElements.size() == 0) {
			return true;
		}
		for (int i= 0; i < selElements.size(); i++) {
			Object elem= selElements.get(i);
			if (elem instanceof CPListElement) {
				if (((CPListElement) elem).getParentContainer() != null) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public abstract void init(IJavaProject javaProject);

	public abstract Control getControl(Composite parent);

	public abstract void setFocus();
	


	protected abstract class CPListAdapter implements IDialogFieldListener, ITreeListAdapter<CPListElement> {
		private final Object[] EMPTY_ARR= new Object[0];

		@Override
		public Object[] getChildren(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElement) {
				return ((CPListElement) element).getChildren(false);
			} else if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute) element;
				if (CPListElement.MODULE.equals(attribute.getKey())) {
					return (ModuleAddExport[]) attribute.getValue();
				}
			}
			return EMPTY_ARR;
		}

		@Override
		public Object getParent(TreeListDialogField<CPListElement> field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(TreeListDialogField<CPListElement> field, Object element) {
			Object[] children= getChildren(field, element);
			return children != null && children.length > 0;
		}
	}

}
