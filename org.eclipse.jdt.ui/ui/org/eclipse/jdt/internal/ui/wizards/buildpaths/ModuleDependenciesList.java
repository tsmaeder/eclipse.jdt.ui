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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.jdt.core.IModuleDescription;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleDialog.ListContentProvider;

/**
 * List widget for the left-hand pane showing all modules in the module graph
 */
class ModuleDependenciesList {

	static class FocusAwareStringComparator implements Comparator<String> {
		private String fFocusString;

		public FocusAwareStringComparator(String focusString) {
			fFocusString= focusString;
		}
		@Override
		public int compare(String o1, String o2) {
			if (o1.equals(fFocusString)) {
				return -1;
			}
			if (o2.equals(fFocusString)) {
				return 1;
			}
			return o1.compareTo(o2);
		}
	}

	static ImageDescriptor DESC_OBJ_MODULE = new JavaElementImageDescriptor(JavaPluginImages.DESC_OBJS_MODULE, 0,
			JavaElementImageProvider.BIG_SIZE);
	
	static class ModulesLabelProvider extends LabelProvider implements ITableLabelProvider {
		Function<String,ModuleKind> fGetModuleKind;

		public ModulesLabelProvider(Function<String, ModuleKind> getModuleKind) {
			fGetModuleKind= getModuleKind;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			ModuleKind kind= fGetModuleKind.apply((String) element);
			ImageDescriptor imgDesc= new ModuleDependenciesPage.DecoratedImageDescriptor(
					DESC_OBJ_MODULE, kind.getDecoration(), kind != ModuleKind.Focus
			);
			return JavaPlugin.getImageDescriptorRegistry().get(imgDesc);
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			return element.toString();
		}
	}

	enum ModuleKind { 
		Normal, Focus, System, UpgradedSystem, Automatic;
		
		public ImageDescriptor getDecoration() {
			switch (this) {
				case Focus:
					return JavaPluginImages.DESC_OVR_FOCUS;
				case Automatic:
					return JavaPluginImages.DESC_OVR_AUTO_MOD;
				case System:
					return JavaPluginImages.DESC_OVR_SYSTEM_MOD;
					//$CASES-OMITTED$
				default:
					return null;
			}
		}
	}
	public final List<String> fNames= new ArrayList<>();
	private FocusAwareStringComparator fNamesComparator;
	public final Map<String,CPListElement> fModule2Element= new HashMap<>();
	private List<String> fInitialNames= new ArrayList<>();
	private TableViewer fViewer;
	private Map<CPListElement,String> fElem2ModName= new HashMap<>();
	private Map<CPListElement,IModuleDescription> fModules= new HashMap<>();
	private Map<CPListElement,ModuleKind> fKinds= new HashMap<>();

	public void createViewer(Composite left, PixelConverter converter) {
		TableViewer tableViewer= new TableViewer(left, SWT.MULTI | SWT.BORDER);
		tableViewer.setContentProvider(new ListContentProvider());
		tableViewer.setLabelProvider(new ModulesLabelProvider(this::getModuleKind));
		tableViewer.setInput(fNames);

		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint= converter.convertWidthInCharsToPixels(30);
		gd.heightHint= converter.convertHeightInCharsToPixels(6);
		tableViewer.getControl().setLayoutData(gd);
		fViewer= tableViewer;
	}

	public void setSelectionChangedListener(BiConsumer<List<CPListElement>,IModuleDescription> listener) {
		fViewer.addSelectionChangedListener(e -> listener.accept(getSelectedElements(), getSelectedModule()));
	}

	public void addModule(IModuleDescription module, CPListElement cpe, ModuleKind kind) {
		String moduleName = module.getElementName();
		fNames.add(moduleName);
		fModule2Element.put(moduleName, cpe);
		fElem2ModName.put(cpe, moduleName);
		fKinds.put(cpe, kind);
		switch (kind) {
			case System:
				break; // system modules are already stored inside the CPListElement
			case Focus:
				fNamesComparator= new FocusAwareStringComparator(moduleName);
				//$FALL-THROUGH$
				//$CASES-OMITTED$
			default:
				fModules.put(cpe, module);
				break;
		}
	}

	public void captureInitial() {
		fInitialNames.clear();
		fInitialNames.addAll(fNames);
	}
	
	public boolean isModified() {
		return !fInitialNames.equals(fNames);
	}

	public void refresh() {
		fNames.sort(fNamesComparator);
		fViewer.refresh();
	}

	public ModuleKind getModuleKind(String name) {
		CPListElement element= fModule2Element.get(name);
		if (element != null) {
			return fKinds.get(element);
		}
		return ModuleKind.Normal;
	}
	
	public ModuleKind getModuleKind(CPListElement element) {
		return getModuleKind(fElem2ModName.get(element));
	}

	public List<CPListElement> getSelectedElements() {
		List<CPListElement> selectedElements= new ArrayList<>();
		for (Object selected : fViewer.getStructuredSelection().toList()) {
			selectedElements.add(fModule2Element.get(selected));
		}
		return selectedElements;
	}

	private IModuleDescription getSelectedModule() {
		List<CPListElement> selectedElems= getSelectedElements();
		if (selectedElems.size() == 1) {
			CPListElement selectedElem= selectedElems.get(0);
			if (selectedElem.getModule() != null) {
				return selectedElem.getModule(); // system module
			}
			return fModules.get(selectedElem);
		}
		return null;
	}

	public void setSelectionToModule(String moduleName) {
		fViewer.setSelection(new StructuredSelection(moduleName), true);
	}
}