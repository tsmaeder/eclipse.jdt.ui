/*******************************************************************************
 * Copyright (c) 2019 GK Software SE, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModuleKind;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.LimitModules;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/*
 * TODO: ("+" must have, "-" later)
 * LHS:
 * - module kind Upgrade of a System Library (incl. icon decoration)
 * - indicator for module having tweaks (to be shown in the LHS)
 * - better help on how to remove non-JRE modules (module-info, modulepath)
 * RHS:
 * + consider one more toplevel synth node: "Patched with/from" (whatever word we select, here and for the button)
 *   + its children would be IPackageFragmentRoot, or (Till's suggestions): IJavaProject
 *   + validate that no root tries to patch different modules at the same time
 *   + handle patch project w/o module-info
 *     + treat the patched module as the current context module (pinned)
 * + qualified exports / opens
 * + show Declared > reads (for requires)
 * + fix decoration in ModulesSelectionDialog (when used for add reads)
 * General:
 * - distinguish test/main dependencies
 * + special elements: ALL-UNNAMED, ALL-SYSTEM ...
 *   + Select All button in Add system module dialog
 * - Help page and reference to it
 */
public class ModuleDependenciesPage extends BuildPathBasePage {

	/** Composed image descriptor consisting of a base image and optionally a decoration overlay. */
	static class DecoratedImageDescriptor extends CompositeImageDescriptor {
		private ImageDescriptor fBaseImage;
		private ImageDescriptor fOverlay;
		private boolean fDrawAtOffset;
		public DecoratedImageDescriptor(ImageDescriptor baseImage, ImageDescriptor overlay, boolean drawAtOffset) {
			fBaseImage= baseImage;
			fOverlay= overlay;
			fDrawAtOffset= drawAtOffset;
		}
		@Override
		protected void drawCompositeImage(int width, int height) {
			drawImage(createCachedImageDataProvider(fBaseImage), 0, 0);
			if (fOverlay != null) {
				CachedImageDataProvider provider= createCachedImageDataProvider(fOverlay);
				if (fDrawAtOffset) {
					drawImage(provider, getSize().x - provider.getWidth(), 0);
				} else {
					drawImage(provider, 0, 0);
				}
			}
		}
		@Override
		protected Point getSize() {
			return JavaElementImageProvider.BIG_SIZE;
		}
		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + ((fBaseImage == null) ? 0 : fBaseImage.hashCode());
			result= prime * result + ((fOverlay == null) ? 0 : fOverlay.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DecoratedImageDescriptor other= (DecoratedImageDescriptor) obj;
			if (fBaseImage == null) {
				if (other.fBaseImage != null)
					return false;
			} else if (!fBaseImage.equals(other.fBaseImage))
				return false;
			if (fOverlay == null) {
				if (other.fOverlay != null)
					return false;
			} else if (!fOverlay.equals(other.fOverlay))
				return false;
			return true;
		}
	}

	private final ListDialogField<CPListElement> fClassPathList; // shared with other pages
	private IJavaProject fCurrJProject;

	// LHS list:
	private ModuleDependenciesList fModuleList;

	// RHS tree:
	private final TreeListDialogField<Object> fDetailsList;

	// bi-directional dependency graph:
	private Map<String,List<String>> fModule2RequiredModules;
	private Map<String,List<String>> fModuleRequiredByModules;

	public final Map<String,String> fPatchMap= new HashMap<>();

	private Control fSWTControl;
	private final IWorkbenchPreferenceContainer fPageContainer; // for switching page (not yet used)

	public ModuleDependenciesPage(CheckedListDialogField<CPListElement> classPathList, IWorkbenchPreferenceContainer pageContainer) {
		fClassPathList= classPathList;
		fPageContainer= pageContainer;
		fSWTControl= null;
		
		String[] buttonLabels= new String[] {
				NewWizardMessages.ModuleDependenciesPage_modules_remove_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_read_button,
				NewWizardMessages.ModuleDependenciesPage_modules_expose_package_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_patch_button,
				/* */ null,
				NewWizardMessages.ModuleDependenciesPage_modules_edit_button
			};

		fModuleList= new ModuleDependenciesList();

		ModuleDependenciesAdapter adapter= new ModuleDependenciesAdapter(this);
		fDetailsList= new TreeListDialogField<>(adapter, buttonLabels, new ModuleDependenciesAdapter.ModularityDetailsLabelProvider());
		fDetailsList.setDialogFieldListener(adapter);
		fDetailsList.setLabelText(NewWizardMessages.ModuleDependenciesPage_details_label);

		adapter.setList(fDetailsList);

		fDetailsList.setViewerComparator(new ModuleDependenciesAdapter.ElementSorter());
	}

	@Override
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		GridLayout layout= new GridLayout(2, false);
		layout.marginBottom= 0;
		composite.setLayout(layout);
		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		composite.setLayoutData(gd);

		// === left: ===
		Composite left= new Composite(composite, SWT.NONE);
		layout= new GridLayout(1, false);
		layout.marginBottom= 0;
		left.setLayout(layout);
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		left.setLayoutData(gd);

		Label title= new Label(left, SWT.NONE);
		title.setText(NewWizardMessages.ModuleDependenciesPage_modules_label);

		fModuleList.createViewer(left, converter);
		fModuleList.setSelectionChangedListener((elems, mod) -> selectModule(elems, mod));
		
		Button addButton= new Button(left, SWT.NONE);
		addButton.setText(NewWizardMessages.ModuleDependenciesPage_addSystemModule_button);
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addSystemModules()));

		// === right: ===
		Composite right= new Composite(composite, SWT.NONE);
		layout= new GridLayout(2, false);
		layout.marginBottom= 0;
		right.setLayout(layout);
		gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.minimumWidth= 0;
		right.setLayoutData(gd);

		LayoutUtil.doDefaultLayout(right, new DialogField[] { fDetailsList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fDetailsList.getTreeControl(null));

		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fDetailsList.setButtonsMinWidth(buttonBarWidth);

		fDetailsList.setViewerComparator(new CPListElementSorter());

		fSWTControl= composite;

		return composite;
	}

	public Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}

	@Override
	public void init(IJavaProject jproject) {
		fCurrJProject= jproject;
		if (Display.getCurrent() != null) {
			scanModules();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					scanModules();
				}
			});
		}
	}

	protected void scanModules() {
		fModule2RequiredModules= new HashMap<>();
		fModuleRequiredByModules= new HashMap<>();
		Set<String> recordedModules = new HashSet<>();

		List<CPListElement> cpelements= fClassPathList.getElements();

		for (CPListElement cpe : cpelements) {
			Object value= cpe.getAttribute(CPListElement.MODULE);
			if (value instanceof ModuleEncapsulationDetail[]) {
				for (ModuleEncapsulationDetail detail : (ModuleEncapsulationDetail[]) value) {
					if (detail instanceof ModulePatch) {
						ModulePatch patch= (ModulePatch) detail;
						fPatchMap.put(patch.fPaths, patch.fModule);
					}
				}
			}

			switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					IPackageFragmentRoot[] fragmentRoots= fCurrJProject.findPackageFragmentRoots(cpe.getClasspathEntry());
					if (fragmentRoots != null && fragmentRoots.length == 1) {
						for (IPackageFragmentRoot fragmentRoot : fragmentRoots) {
							IModuleDescription module= fragmentRoot.getModuleDescription();
							if (module != null) {
								recordModule(module, recordedModules, cpe, ModuleKind.Focus);
								break;
							}
						}
					}
					break;
				case IClasspathEntry.CPE_PROJECT:
					IProject project= fCurrJProject.getProject().getWorkspace().getRoot().getProject(cpe.getClasspathEntry().getPath().toString());
					try {
						IJavaProject jProject= JavaCore.create(project);
						IModuleDescription module= jProject.getModuleDescription();
						ModuleKind kind= ModuleKind.Normal;
						if (module == null) {
							module= JavaCore.getAutomaticModuleDescription(jProject);
							kind= ModuleKind.Automatic;
						}
						if (module != null) {
							recordModule(module, recordedModules, cpe, kind);
						}
					} catch (JavaModelException e) {
						// ignore
					}
					break;
				case IClasspathEntry.CPE_CONTAINER:
					ModuleKind kind= LibrariesWorkbookPage.isJREContainer(cpe.getPath()) ? ModuleKind.System : ModuleKind.Normal;
					for (Object object : cpe.getChildren(true)) {
						if (object instanceof CPListElement) {
							CPListElement childElement= (CPListElement) object;
							IModuleDescription childModule= childElement.getModule();
							if (childModule != null) {
								fModuleList.addModule(childModule, childElement, kind);
							}
						}
					}
					if (kind == ModuleKind.System) {
						// additionally capture dependency information about all system module disregarding --limit-modules
						for (IPackageFragmentRoot packageRoot : fCurrJProject.findUnfilteredPackageFragmentRoots(cpe.getClasspathEntry())) {
							IModuleDescription module= packageRoot.getModuleDescription();
							if (module != null) {
								recordModule(module, recordedModules, null/*don't add to fModuleList*/, kind);
							}
						}
					}
					break;
				default: // LIBRARY & VARIABLE:
					for (IPackageFragmentRoot packageRoot : fCurrJProject.findPackageFragmentRoots(cpe.getClasspathEntry())) {
						IModuleDescription module= packageRoot.getModuleDescription();
						kind= ModuleKind.Normal;
						if (module == null) {
							try {
								module= JavaCore.getAutomaticModuleDescription(packageRoot);
								kind= ModuleKind.Automatic;
							} catch (JavaModelException | IllegalArgumentException e) {
								// ignore
							}
						}
						if (module != null) {
							recordModule(module, recordedModules, cpe, kind);
							break;
						}
					}
			}
		}
		fModuleList.captureInitial();
		fModuleList.refresh();
	}

	private void recordModule(IModuleDescription module, Set<String> moduleNames, CPListElement cpe, ModuleKind kind) {
		if (cpe != null) {
			fModuleList.addModule(module, cpe, kind);
		}
		String moduleName= module.getElementName();
		if (moduleNames.add(moduleName)) {
			try {
				for (String required : module.getRequiredModuleNames()) {
					List<String> otherModules= fModule2RequiredModules.get(moduleName);
					if (otherModules == null) {
						otherModules= new ArrayList<>();
						fModule2RequiredModules.put(moduleName, otherModules);
					}
					otherModules.add(required);

					otherModules= fModuleRequiredByModules.get(required);
					if (otherModules == null) {
						otherModules= new ArrayList<>();
						fModuleRequiredByModules.put(required, otherModules);
					}
					otherModules.add(moduleName);
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}


	@Override
	public List<?> getSelection() {
		return fDetailsList.getSelectedElements();
	}

	@Override
	public void setSelection(List<?> selElements, boolean expand) {
		fDetailsList.selectElements(new StructuredSelection(selElements));
		if (expand) {
			for (int i= 0; i < selElements.size(); i++) {
				fDetailsList.expandElement(selElements.get(i), 1);
			}
		}
	}

	public void setSelectionToModule(String moduleName) {
		int idx= fModuleList.fNames.indexOf(moduleName);
		if (idx != -1) {
			fModuleList.setSelectionToModule(moduleName);
		}
	}

	private void selectModule(List<CPListElement> elements, IModuleDescription module) {
		fDetailsList.removeAllElements();
		if (elements.size() == 1) {
			CPListElement element= elements.get(0);
			fDetailsList.addElement(element);
			fDetailsList.addElement(new ModuleDependenciesAdapter.DeclaredDetails(module, element));
			ModuleKind moduleKind= fModuleList.getModuleKind(element);
			if (moduleKind != ModuleKind.Focus) {
				ModuleDependenciesAdapter.ConfiguredDetails configured= new ModuleDependenciesAdapter.ConfiguredDetails(module, element, moduleKind);
				fDetailsList.addElement(configured);
				fDetailsList.expandElement(configured, 1);
			}
			ModuleDependenciesAdapter.enableDefaultButtons(fDetailsList, true, !elements.isEmpty());
		} else {
			ModuleDependenciesAdapter.enableDefaultButtons(fDetailsList, false, !elements.isEmpty());
		}
	}

	@Override
	public boolean isEntryKind(int kind) {
		return true;
	}

	@Override
	public void setFocus() {
    	fDetailsList.setFocus();
	}
	
	void addSystemModules() {
		CPListElement cpListElement= findSystemLibraryElement();
		ModuleSelectionDialog dialog= new ModuleSelectionDialog(getShell(), fCurrJProject, cpListElement.getClasspathEntry(), fModuleList.fNames, this::computeForwardClosure);
		if (dialog.open() == IDialogConstants.OK_ID) {
			for (IModuleDescription addedModule : dialog.getResult()) {
				fModuleList.addModule(addedModule, getOrCreateModuleCPE(cpListElement, addedModule), ModuleKind.System);
			}
			updateLimitModules(cpListElement.findAttributeElement(CPListElement.MODULE));
			fModuleList.refresh();
		}
	}

	CPListElement getOrCreateModuleCPE(CPListElement parentCPE, IModuleDescription module) {
		CPListElement element= fModuleList.fModule2Element.get(module.getElementName());
		if (element != null) {
			return element;
		}
		try {
			IClasspathEntry entry= fCurrJProject.getClasspathEntryFor(module.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT).getPath());
			return CPListElement.create(parentCPE, entry, module, true, fCurrJProject);
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			return null;
		}
	}
	private CPListElement findSystemLibraryElement() {
		for (CPListElement cpListElement : fClassPathList.getElements()) {
			if (LibrariesWorkbookPage.isJREContainer(cpListElement.getPath()))
				return cpListElement;
		}
		return null;
	}

	void removeModules() {
		List<CPListElement> selectedElements= fModuleList.getSelectedElements();
		List<String> selectedModuleNames= new ArrayList<>();
		Set<String> allModulesToRemove = new HashSet<>();
		for (CPListElement selectedElement : selectedElements) {
			if (fModuleList.getModuleKind(selectedElement) == ModuleKind.Focus) {
				MessageDialog.openError(getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						NewWizardMessages.ModuleDependenciesPage_removeCurrentModule_error);
				return;
			}
			IModuleDescription mod = selectedElement.getModule();
			if (mod == null) {
				MessageDialog.openError(getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removeModule_error_with_hint,
								selectedElement.getPath().lastSegment(), NewWizardMessages.ModuleDependenciesPage_removeSystemModule_error_hint));
				// TODO: offer to switch to the corresponding tab?
				return;
			}
			String moduleName= mod.getElementName();
			if (moduleName.equals("java.base")) { //$NON-NLS-1$
				MessageDialog.openError(getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title,
						MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removeModule_error_with_hint, moduleName, "")); //$NON-NLS-1$
				return;
			}
			selectedModuleNames.add(moduleName);
			collectModulesToRemove(moduleName, allModulesToRemove);
		}
		String seedModules= String.join(", ", selectedModuleNames); //$NON-NLS-1$
		if (allModulesToRemove.size() == selectedModuleNames.size()) {
			if (confirmRemoveModule(MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removingModule_message, seedModules))) {
				fModuleList.fNames.removeAll(selectedModuleNames);
				fModuleList.refresh();
			}
		} else {
			StringBuilder message = new StringBuilder(
					MessageFormat.format(NewWizardMessages.ModuleDependenciesPage_removingModuleTransitive_message, seedModules));
			// append sorted list minus the selected module:
			message.append(allModulesToRemove.stream()
					.filter(m -> !seedModules.contains(m))
					.sorted()
					.collect(Collectors.joining("\n\t", "\t", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!confirmRemoveModule(message.toString()))
				return;
			fModuleList.fNames.removeAll(allModulesToRemove);
			fModuleList.refresh();
		}
		for (CPListElement elem: selectedElements) {
			Object container= elem.getParentContainer();
			if (container instanceof CPListElement) {
				CPListElement containerElement= (CPListElement) container;
				if (LibrariesWorkbookPage.isJREContainer(containerElement.getPath())) {
					CPListElementAttribute attribute= containerElement.findAttributeElement(CPListElement.MODULE);
					updateLimitModules(attribute);
					break;
				}
			}
		}
	}

	private Set<String> computeForwardClosure(List<String> seeds) {
		Set<String> closure= new HashSet<>();
		collectForwardClosure(seeds, closure);
		return closure;
	}
	private void collectForwardClosure(List<String> seeds, Set<String> closure) {
		for (String seed : seeds) {
			if (closure.add(seed) && !fModuleList.fNames.contains(seed)) {
				List<String> deps= fModule2RequiredModules.get(seed);
				if (deps != null) {
					collectForwardClosure(deps, closure);
				}
			}
		}
	}

	private void collectModulesToRemove(String mod, Set<String> modulesToRemove) {
		if (fModuleList.fNames.contains(mod) && modulesToRemove.add(mod)) {
			List<String> requireds= fModuleRequiredByModules.get(mod);
			if (requireds != null) {
				for (String required : requireds) {
					collectModulesToRemove(required, modulesToRemove);
				}
			}
		}
	}
	
	private boolean confirmRemoveModule(String message) {
		int answer= MessageDialog.open(MessageDialog.QUESTION, getShell(), NewWizardMessages.ModuleDependenciesPage_removeModule_dialog_title, message, SWT.NONE, NewWizardMessages.ModuleDependenciesPage_remove_button, NewWizardMessages.ModuleDependenciesPage_cancel_button);
		return answer == 0;
	}

	private void updateLimitModules(CPListElementAttribute moduleAttribute) {
		LimitModules limitModules= new ModuleEncapsulationDetail.LimitModules(reduceNames(fModuleList.fNames), moduleAttribute);
		Object value= moduleAttribute.getValue();
		if (value instanceof ModuleEncapsulationDetail[]) {
			ModuleEncapsulationDetail[] details= (ModuleEncapsulationDetail[]) value;
			for (int i= 0; i < details.length; i++) {
				if (details[i] instanceof LimitModules) {
					// replace existing --limit-modules
					details[i]= limitModules;
					moduleAttribute.setValue(details);
					return;
				}
			}
			if (details.length > 0) {
				// append to existing list of other details:
				ModuleEncapsulationDetail[] newDetails= Arrays.copyOf(details, details.length+1);
				newDetails[newDetails.length-1]= limitModules;
				moduleAttribute.setValue(newDetails);
				return;
			}
		}
		// set as singleton detail:
		moduleAttribute.setValue(new ModuleEncapsulationDetail[] { limitModules });
	}
	
	List<String> reduceNames(List<String> names) {
		List<String> reduced= new ArrayList<>();
		outer:
		for (String name : names) {
			if (fModuleList.getModuleKind(name) == ModuleKind.System) {
				List<String> dominators= fModuleRequiredByModules.get(name);
				if (dominators != null) {
					for (String dominator : dominators) {
						if (fModuleList.fNames.contains(dominator)) {
							continue outer;
						}
					}
				}
				reduced.add(name);
			}
		}
		return reduced;
	}
}
