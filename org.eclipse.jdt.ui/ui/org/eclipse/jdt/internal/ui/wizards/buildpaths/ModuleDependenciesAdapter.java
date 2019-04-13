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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesList.ModuleKind;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDependenciesPage.DecoratedImageDescriptor;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddExpose;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddOpens;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModuleAddReads;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/**
 * Implementation of the right-hand pane of the {@link ModuleDependenciesPage}.
 */
class ModuleDependenciesAdapter implements IDialogFieldListener, ITreeListAdapter<Object> {

	private static final int IDX_REMOVE= 0;

	private static final int IDX_READ_MODULE= 2;
	private static final int IDX_EXPOSE_PACKAGE= 3;

	private static final int IDX_PATCH= 5;
	
	private static final int IDX_EDIT= 7;
	
	abstract static class Details {

		/** the module selected in the LHS pane, for which details are being shown / edited in this RHS pane. */
		protected final IModuleDescription fFocusModule;
		/** the classpath element by which the current project refers to the focus module. */
		protected final CPListElement fElem;

		public Details(IModuleDescription focusModule, CPListElement elem) {
			fFocusModule= focusModule;
			fElem= elem;
		}

		/**
		 * Answer the module of the current IJavaProject for which the build path is being configured. 
		 * @return the module of the current project, or {@code null}.
		 */
		protected IModuleDescription getContextModule() {
			try {
				IModuleDescription moduleDescription= fElem.getJavaProject().getModuleDescription();
				if (moduleDescription != null) {
					return moduleDescription;
				}
			} catch (JavaModelException jme) {
				JavaPlugin.log(jme.getStatus());
			}
			return null;
		}
		protected String getContextModuleName() {
			try {
				IModuleDescription moduleDescription= fElem.getJavaProject().getModuleDescription();
				if (moduleDescription != null) {
					return moduleDescription.getElementName();
				}
			} catch (JavaModelException jme) {
				JavaPlugin.log(jme.getStatus());
			}
			return ""; //$NON-NLS-1$
		}

		protected IJavaProject getContextProject() {
			return fElem.getJavaProject();
		}
	}

	/** Synthetic tree node as a parent for details that are declared by the focus module (in its module-info) */
	static class DeclaredDetails extends Details {

		public DeclaredDetails(IModuleDescription mod, CPListElement elem) {
			super(mod, elem);
		}

		/**
		 * Answer all packages accessible to the context module (exports/opens) 
		 * @return accessible packages represented by {@link AccessiblePackage}.
		 */
		public Object[] getPackages() {
			try {
				if (fFocusModule != null && !fFocusModule.isAutoModule()) {
					IModuleDescription contextModule= getContextModule();
					String[] exported= fFocusModule.getExportedPackageNames(contextModule);
					List<AccessiblePackage> result= new ArrayList<>();
					for (String export : exported) {
						result.add(new AccessiblePackage(export, AccessiblePackage.Kind.Exports, this));
					}
					String[] opened= fFocusModule.getOpenedPackageNames(contextModule);
					for (String open : opened) {
						result.add(new AccessiblePackage(open, AccessiblePackage.Kind.Opens, this));
					}
					return result.toArray();
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return new Object[0];
		}
	}

	/** Synthetic tree node. */
	static class ConfiguredDetails extends Details {
		private ModuleKind fKind;
	
		public ConfiguredDetails(IModuleDescription focusModule, CPListElement elem, ModuleKind moduleKind) {
			super(focusModule, elem);
			fKind= moduleKind;
		}
		
		public Object[] getChildren() {
			if (fKind == ModuleKind.System) {
				// aggregate attribute is in the parent (corresponding to the JRE)
				Object parent= fElem.getParentContainer();
				if (parent instanceof CPListElement) {
					Object attribute= ((CPListElement) parent).getAttribute(CPListElement.MODULE);
					if (attribute instanceof ModuleEncapsulationDetail[]) {
						return convertEncapsulationDetails((ModuleEncapsulationDetail[]) attribute, fFocusModule.getElementName());
					}					
				}
			}
			Object attribute= fElem.getAttribute(CPListElement.MODULE);
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				return convertEncapsulationDetails((ModuleEncapsulationDetail[]) attribute, null);
			}
			return fElem.getChildren(true);
		}

		private DetailNode<?>[] convertEncapsulationDetails(ModuleEncapsulationDetail[] attribute, String filterModule) {
			List<DetailNode<?>> filteredDetails= new ArrayList<>();
			for (ModuleEncapsulationDetail detail : attribute) {
				if (detail instanceof ModuleAddExpose) {
					ModuleAddExpose moduleAddExpose= (ModuleAddExpose) detail;
					if (filterModule == null || filterModule.equals(moduleAddExpose.fSourceModule)) {
						AccessiblePackage.Kind kind= moduleAddExpose instanceof ModuleAddExport ? AccessiblePackage.Kind.Exports : AccessiblePackage.Kind.Opens;
						filteredDetails.add(new AccessiblePackage(moduleAddExpose.fPackage, kind, this));
					}
				} else if (detail instanceof ModuleAddReads) {
					ModuleAddReads moduleAddReads= (ModuleAddReads) detail;
					if (filterModule == null || filterModule.equals(moduleAddReads.fSourceModule)) {
						filteredDetails.add(new ReadModule(moduleAddReads.fTargetModule, this));
					}								
				} else if (detail instanceof ModulePatch) {
					ModulePatch modulePatch= (ModulePatch) detail;
					if (filterModule == null || filterModule.equals(modulePatch.fModule)) {
						try {
							if (modulePatch.fPaths != null) {
								IPath path= new Path(modulePatch.fPaths);
								IFolder folder= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
								IJavaElement elem= JavaCore.create(folder.getProject()).getPackageFragmentRoot(folder);
								if (elem instanceof IPackageFragmentRoot) {
									filteredDetails.add(new PatchModule((IPackageFragmentRoot) elem, this));
								}
							} else {
								for (IClasspathEntry entry : fElem.getJavaProject().getRawClasspath()) {
									if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
										for (IPackageFragmentRoot root : fElem.getJavaProject().findPackageFragmentRoots(entry)) {
											if (root.getKind() == IPackageFragmentRoot.K_SOURCE)
												filteredDetails.add(new PatchModule(root, this));
										}
									}
								}
							}
						} catch (JavaModelException e) {
							JavaPlugin.log(e);
						}
					}
				}
			}
			return filteredDetails.toArray(new DetailNode<?>[filteredDetails.size()]);
		}
		public void removeAll() {
			if (fKind == ModuleKind.System) {
				// aggregate attribute is in the parent (corresponding to the JRE)
				Object parent= fElem.getParentContainer();
				if (parent instanceof CPListElement) {
					CPListElement jreElement= (CPListElement) parent;
					Object attribute= jreElement.getAttribute(CPListElement.MODULE);
					if (attribute instanceof ModuleEncapsulationDetail[]) {
						// need to filter so we remove only affected details:
						ModuleEncapsulationDetail[] filtered= Arrays.stream((ModuleEncapsulationDetail[]) attribute)
									.filter(d -> !d.affects(fFocusModule.getElementName()))
									.toArray(ModuleEncapsulationDetail[]::new);
						jreElement.setAttribute(CPListElement.MODULE, filtered);
						return;
					}
				}
			}
			Object attribute= fElem.getAttribute(CPListElement.MODULE);
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				fElem.setAttribute(CPListElement.MODULE, new ModuleEncapsulationDetail[0]);
			}
		}
		public boolean addOrEditAccessiblePackage(AccessiblePackage selectedPackage, Shell shell) {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= element.findAttributeElement(CPListElement.MODULE);

			IPackageFragmentRoot packRoot= (IPackageFragmentRoot) fFocusModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			String packageName= selectedPackage != null ? selectedPackage.getName() : ""; //$NON-NLS-1$

			ModuleAddExpose initial= (selectedPackage != null)
					? selectedPackage.convertToCP(moduleAttribute)
					: new ModuleAddExport(fFocusModule.getElementName(), packageName, getContextModuleName(), moduleAttribute);

			ModuleAddExportsDialog dialog= new ModuleAddExportsDialog(shell, new IJavaElement[] { packRoot }, initial);
			if (dialog.open() == Window.OK) {
				ModuleAddExpose expose= dialog.getExport(moduleAttribute);
				if (expose != null) {
					Object attribute= moduleAttribute.getValue();
					ModuleEncapsulationDetail[] arrayValue= null;
					if (attribute instanceof ModuleEncapsulationDetail[]) {
						arrayValue= (ModuleEncapsulationDetail[]) attribute;
						if (selectedPackage != null) {
							// editing: replace existing entry
							for (int i= 0; i < arrayValue.length; i++) {
								ModuleEncapsulationDetail detail= arrayValue[i];
								if (detail.equals(initial)) {
									arrayValue[i]= expose;
									break;
								}
							}
						} else {
							arrayValue= Arrays.copyOf(arrayValue, arrayValue.length+1);
							arrayValue[arrayValue.length-1]= expose;
						}
					} else {
						arrayValue= new ModuleEncapsulationDetail[] { expose };
					}
					element.setAttribute(CPListElement.MODULE, arrayValue);
					return true;
				}
			}
			return false;
		}

		public void remove(List<Object> selectedElements) {
			CPListElementAttribute moduleAttribute;
			if (fKind == ModuleKind.System) {
				moduleAttribute= ((CPListElement) fElem.getParentContainer()).findAttributeElement(CPListElement.MODULE);
			} else {
				moduleAttribute= fElem.findAttributeElement(CPListElement.MODULE);
			}
			if (moduleAttribute == null) {
				// TODO report
				return;
			}
			if (!(moduleAttribute.getValue() instanceof ModuleEncapsulationDetail[])) {
				// TODO report
				return;
			}
			List<ModuleEncapsulationDetail> details= new ArrayList<>(Arrays.asList((ModuleEncapsulationDetail[]) moduleAttribute.getValue()));
			for (Object node : selectedElements) {
				if (node instanceof DetailNode<?>) {
					ModuleEncapsulationDetail med= ((DetailNode<?>) node).convertToCP(moduleAttribute);
					if (med != null) {
						details.remove(med);
					}
				} else if (node instanceof ConfiguredDetails) {
					((ConfiguredDetails) node).removeAll();
					return; // covers all details, changes in 'details' are irrelevant
				}
			}
			moduleAttribute.setValue(details.toArray(new ModuleEncapsulationDetail[details.size()]));
		}

		public boolean addReads(Shell shell) {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= element.findAttributeElement(CPListElement.MODULE);
			if (moduleAttribute == null) {
				return false; // TODO report
			}

			List<String> irrelevantModules;
			try {
				irrelevantModules= Arrays.asList(fFocusModule.getRequiredModuleNames());
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				irrelevantModules= Collections.emptyList();
			}
			irrelevantModules= new ArrayList<>(irrelevantModules);
			irrelevantModules.add(fFocusModule.getElementName());

			ModuleSelectionDialog dialog= new ModuleSelectionDialog(shell, fElem.getJavaProject(), null, irrelevantModules, HashSet::new);
			if (dialog.open() != 0) {				
				return false;
			}

			List<IModuleDescription> result= dialog.getResult();
			editModularityDetails(element, moduleAttribute, result,
					null,
					mod -> new ModuleAddReads(fFocusModule.getElementName(), mod.getElementName(), moduleAttribute));
			return true;
		}

		public boolean addPatch(Shell shell, Map<String, String> patchMap) {
			Object container= fElem.getParentContainer();
			CPListElement element= (container instanceof CPListElement) ? (CPListElement) container : fElem;
			CPListElementAttribute moduleAttribute= element.findAttributeElement(CPListElement.MODULE);
			if (moduleAttribute == null) {
				return false; // TODO report
			}
			IJavaProject contextProject= getContextProject();
			ModulePatchSourceSelectionDialog dialog= new ModulePatchSourceSelectionDialog(shell, fFocusModule, contextProject);
			if (dialog.open() != 0) {
				return false;
			}
			List<IPackageFragmentRoot> result= dialog.getResult();
			for (IPackageFragmentRoot root : result) {
				String rootPath= root.getPath().toString();
				if (patchMap.containsKey(rootPath)) {
					if (!MessageDialog.openQuestion(shell, "Patch module conflict", 
							MessageFormat.format("The source folder {0} was declared to patch module {1}, change to replacing module {2} instead?",
									root.getPath(), patchMap.get(rootPath), fFocusModule.getElementName()))) {
						return false;
					}
				}
			}
			editModularityDetails(element, moduleAttribute, result,
					(r,med) -> med instanceof ModulePatch && r.getPath().toString().equals(((ModulePatch)med).fPaths),
					root -> new ModulePatch(fFocusModule.getElementName(), root.getPath().toString(), moduleAttribute));
			return true;
		}			

		private <T> void editModularityDetails(CPListElement element, CPListElementAttribute moduleAttribute, List<T> result,
				BiPredicate<T,ModuleEncapsulationDetail> shouldReplace, Function<T,ModuleEncapsulationDetail> factory) {
			ModuleEncapsulationDetail[] arrayValue= null;
			int idx= 0;
			Object attribute= moduleAttribute.getValue();
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				List<T> remaining= new ArrayList<>();
				arrayValue= (ModuleEncapsulationDetail[]) attribute;
				if (shouldReplace != null) {
					allResults:
					for (T newValue : result) { 
						for (int i= 0; i < arrayValue.length; i++) {
							if (shouldReplace.test(newValue, arrayValue[i])) {
								arrayValue[i]= factory.apply(result.get(0));
								continue allResults;
							}
						}
						remaining.add(newValue);
					}
					if (remaining.isEmpty())
						return;
					result= remaining;
				}
				idx= arrayValue.length;
				arrayValue= Arrays.copyOf(arrayValue, arrayValue.length+result.size());
			} else {
				arrayValue= new ModuleEncapsulationDetail[result.size()];
			}
			for (T detail : result) {
				arrayValue[idx++]= factory.apply(detail);
			}
			element.setAttribute(CPListElement.MODULE, arrayValue);
		}
	}

	abstract static class DetailNode<D extends ModuleEncapsulationDetail> {
		protected String fName;
		protected Details fParent;

		protected DetailNode(Details parent) {
			fParent= parent;
		}
		public String getName() {
			return fName;
		}
		public boolean isIsConfigured() {
			return fParent instanceof ConfiguredDetails;
		}
		public abstract D convertToCP(CPListElementAttribute attribElem);
	}

	/** Declare that the package denoted by {@link #fName} is accessible (exported/opened) to the current context module. */
	static class AccessiblePackage extends DetailNode<ModuleAddExpose> {
		enum Kind { Exports, Opens;
			ImageDescriptor getDecoration() {
				switch (this) {
					case Exports: return JavaPluginImages.DESC_OVR_EXPORTS;
					case Opens: return JavaPluginImages.DESC_OVR_OPENS;
					default: return null;
				}
			}
		}
		private Kind fKind;
		public AccessiblePackage(String name, Kind kind, Details parent) {
			super(parent);
			fName= name;
			fKind= kind;
		}
		public Kind getKind() {
			return fKind;
		}
		@Override
		public ModuleAddExpose convertToCP(CPListElementAttribute attribElem) {
			if (fParent instanceof ConfiguredDetails) {
				String targetModule= fParent.getContextModuleName();
				if (targetModule != null) {
					switch (fKind) {
						case Exports:
							return new ModuleAddExport(fParent.fFocusModule.getElementName(), fName, targetModule, attribElem);
						case Opens:
							return new ModuleAddOpens(fParent.fFocusModule.getElementName(), fName, targetModule, attribElem);
						default:
							break;
					}
				}
			}
			return null; // TODO: report
		}
	}

	/** Declare that the module given by {@link #fName} is read by the selected focus module. */
	static class ReadModule extends DetailNode<ModuleAddReads> {

		public ReadModule(String targetModule, Details parent) {
			super(parent);
			fName= targetModule;
		}

		@Override
		public ModuleAddReads convertToCP(CPListElementAttribute attribElem) {
			if (fParent instanceof ConfiguredDetails) {
				return new ModuleAddReads(fParent.fFocusModule.getElementName(), fName, attribElem);
			}
			return null; // TODO: report
		}
	}
	/** Declare that the selected focus module is patched by the content of the given package fragment root. */
	static class PatchModule extends DetailNode<ModulePatch> {
		private IPackageFragmentRoot fRoot;

		public PatchModule(IPackageFragmentRoot root, Details parent) {
			super(parent);
			fRoot= root;
			fName= root.getPath().makeRelative().toString();
		}

		@Override
		public ModulePatch convertToCP(CPListElementAttribute attribElem) {
			return new ModulePatch(fParent.fFocusModule.getElementName(), attribElem); // assumes root is "the source folder" of the current context project
		}
	}

	static class ModularityDetailsLabelProvider extends CPListLabelProvider {
		private ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
		private JavaElementImageProvider fImageLabelProvider= new JavaElementImageProvider();
		@Override
		public String getText(Object element) {
			if (element instanceof DeclaredDetails) {
				return NewWizardMessages.ModuleDependenciesAdapter_declared_node;
			}
			if (element instanceof ConfiguredDetails) {
				return NewWizardMessages.ModuleDependenciesAdapter_configured_node;
			}
			if (element instanceof DetailNode) {
				return ((DetailNode<?>) element).getName();
			}
			return super.getText(element);
		}
		@Override
		public Image getImage(Object element) {
			if (element instanceof CPListElement) {
				return fRegistry.get(JavaPluginImages.DESC_OBJS_MODULE);
			}
			if (element instanceof AccessiblePackage) {
				AccessiblePackage.Kind kind= ((AccessiblePackage) element).getKind();
				ImageDescriptor imgDesc= new DecoratedImageDescriptor(JavaPluginImages.DESC_OBJS_PACKAGE, kind.getDecoration(), true);
				return JavaPlugin.getImageDescriptorRegistry().get(imgDesc);
			}
			if (element instanceof ReadModule) {
				ImageDescriptor imgDesc= new DecoratedImageDescriptor(JavaPluginImages.DESC_OBJS_MODULE,
												JavaPluginImages.DESC_OVR_READS, true);
				return JavaPlugin.getImageDescriptorRegistry().get(imgDesc);
			}
			if (element instanceof PatchModule) {
				return fImageLabelProvider.getImageLabel(((PatchModule) element).fRoot, 0);
			}
			return super.getImage(element);
		}
	}
	
	static class ElementSorter extends CPListElementSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			// sorting for root nodes: CPListElement > DeclaredDetails > ConfiguredDetails
			if (e1 instanceof DeclaredDetails) {
				return e2 instanceof ConfiguredDetails ? -1 : 1;
			}
			if (e1 instanceof ConfiguredDetails) {
				return e2 instanceof DeclaredDetails ? 1 : -1;
			}
			if (e2 instanceof DeclaredDetails || e2 instanceof ConfiguredDetails) {
				return -1;
			}
			return super.compare(viewer, e1, e2);
		}
	}

	public static void enableDefaultButtons(TreeListDialogField<?> list, boolean enable, boolean removeEnabled) {
		list.enableButton(IDX_REMOVE, removeEnabled);
		list.enableButton(IDX_EXPOSE_PACKAGE, enable);
		list.enableButton(IDX_READ_MODULE, enable);
		list.enableButton(IDX_PATCH, enable);
	}

	private final ModuleDependenciesPage fModuleDependenciesPage; // parent structure
	private TreeListDialogField<Object> fDetailsList; // RHS widget managed by this class

	public ModuleDependenciesAdapter(ModuleDependenciesPage moduleDependenciesPage) {
		fModuleDependenciesPage= moduleDependenciesPage;
	}

	public void setList(TreeListDialogField<Object> detailsList) {
		fDetailsList= detailsList;
		fDetailsList.enableButton(IDX_REMOVE, false);
		fDetailsList.enableButton(IDX_EXPOSE_PACKAGE, false);
		fDetailsList.enableButton(IDX_READ_MODULE, false);
		fDetailsList.enableButton(IDX_PATCH, false);
		fDetailsList.enableButton(IDX_EDIT, false);
	}

	// -------- IListAdapter --------
	@Override
	public void customButtonPressed(TreeListDialogField<Object> field, int index) {
		AccessiblePackage selectedPackage= null;
		List<Object> selectedElements= field.getSelectedElements();
		switch (index) {
			case IDX_REMOVE:
				if (selectedElements.size() == 0) {
					// no detail selected, remove the module(s) (with question):
					fModuleDependenciesPage.removeModules();
				} else {
					getConfiguredDetails().remove(selectedElements);
				}
				field.refresh();
				break;
			case IDX_EDIT:
				if (selectedElements.size() == 1 && selectedElements.get(0) instanceof AccessiblePackage) {
					selectedPackage= (AccessiblePackage) selectedElements.get(0);
				}
				// FIXME: can no longer use fallthrough, when editing other details
				//$FALL-THROUGH$
			case IDX_EXPOSE_PACKAGE:
				if (getConfiguredDetails().addOrEditAccessiblePackage(selectedPackage, fModuleDependenciesPage.getShell())) {
					field.refresh();
				}
				break;
			case IDX_READ_MODULE:
				if (getConfiguredDetails().addReads(fModuleDependenciesPage.getShell())) {
					field.refresh();
				}
				break;
			case IDX_PATCH:
				if (getConfiguredDetails().addPatch(fModuleDependenciesPage.getShell(), fModuleDependenciesPage.fPatchMap)) {
					field.refresh();
				}
				break;
			default:
				throw new IllegalArgumentException("Non-existent button index "+index); //$NON-NLS-1$
		}
	}

	private ConfiguredDetails getConfiguredDetails() {
		for (Object object : fDetailsList.getElements()) {
			if (object instanceof ConfiguredDetails)
				return (ConfiguredDetails) object;
		}
		throw new IllegalStateException("detail list has no ConfiguredDetails element"); //$NON-NLS-1$
	}

	@Override
	public void selectionChanged(TreeListDialogField<Object> field) {
		List<Object> selected= fDetailsList.getSelectedElements();
		boolean enable= false;
		if (selected.size() == 1) {
			Object selectedNode= selected.get(0);
			enable= isConfigurableNode(selectedNode);
			fDetailsList.enableButton(IDX_EDIT, enable && selectedNode instanceof DetailNode<?>);
			return;
		} else {
			enable= allAreConfigurable(selected);
			fDetailsList.enableButton(IDX_EDIT, false);
		}
		fDetailsList.enableButton(IDX_EXPOSE_PACKAGE, enable);
		fDetailsList.enableButton(IDX_READ_MODULE, enable);
		fDetailsList.enableButton(IDX_PATCH, enable);
		fDetailsList.enableButton(IDX_REMOVE, enable);
	}

	private boolean allAreConfigurable(List<Object> selected) {
		for (Object node : selected) {
			if (!isConfigurableNode(node))
				return false;
		}
		return true;
	}

	private boolean isConfigurableNode(Object node) {
		if (node instanceof ConfiguredDetails) {
			return true;
		}
		if (node instanceof DeclaredDetails) {
			return false;
		}
		if (node instanceof DetailNode) {
			return ((DetailNode<?>) node).isIsConfigured();
		}
		return true;
	}

	@Override
	public void doubleClicked(TreeListDialogField<Object> field) {
		List<Object> selectedElements= fDetailsList.getSelectedElements();
		if (selectedElements.size() == 1) {
			Object selected= selectedElements.get(0);
			if (selected instanceof ReadModule) {
				String moduleName= ((ReadModule) selected).getName();
				fModuleDependenciesPage.setSelectionToModule(moduleName);
			} else {
				TreeViewer treeViewer= fDetailsList.getTreeViewer();
				boolean isExpanded= treeViewer.getExpandedState(selectedElements.get(0));
				if (isExpanded) {
					treeViewer.collapseToLevel(selectedElements.get(0), 1);				
				} else {
					treeViewer.expandToLevel(selectedElements.get(0), 1);
				}
			}
		}
	}

	@Override
	public void keyPressed(TreeListDialogField<Object> field, KeyEvent event) {
//			libaryPageKeyPressed(field, event);
	}

	@Override
	public Object[] getChildren(TreeListDialogField<Object> field, Object element) {
		if (element instanceof CPListElement) { // assumed to be root
			// no direct children
		} else if (element instanceof DeclaredDetails) {
			return ((DeclaredDetails) element).getPackages();
		} else if (element instanceof ConfiguredDetails) {
			return ((ConfiguredDetails) element).getChildren();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(TreeListDialogField<Object> field, Object element) {
		if (element instanceof CPListElementAttribute) {
			return ((CPListElementAttribute) element).getParent();
		}
		// TODO
		return null;
	}

	@Override
	public boolean hasChildren(TreeListDialogField<Object> field, Object element) {
		Object[] children= getChildren(field, element);
		return children != null && children.length > 0;
	}
	// ---------- IDialogFieldListener --------

	@Override
	public void dialogFieldChanged(DialogField field) {
//			libaryPageDialogFieldChanged(field);
	}

}