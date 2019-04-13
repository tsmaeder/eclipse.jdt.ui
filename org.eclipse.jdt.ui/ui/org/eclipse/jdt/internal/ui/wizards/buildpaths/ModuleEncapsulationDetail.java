/*******************************************************************************
 * Copyright (c) 2017, 2018 GK Software SE, and others.
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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Node in the tree of CPListElement et al, representing a module directive like add-exports ...
 */
public abstract class ModuleEncapsulationDetail {

	protected CPListElementAttribute fAttribElem;

	public CPListElementAttribute getParent() {
		return fAttribElem;
	}

	/**
	 * Retrieve the java element(s) targeted by a given classpath entry.
	 * @param currentProject the Java project holding the classpath entry 
	 * @param path the path value of the classpath entry
	 * @return either an array of {@link IPackageFragmentRoot} or a singleton array of {@link IJavaProject} 
	 * 	targeted by the given classpath entry, or {@code null} if no not found
	 */
	public static IJavaElement[] getTargetJavaElements(IJavaProject currentProject, IPath path) {
		IResource member= ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (member != null) {
			IJavaElement element= JavaCore.create(member);
			if (element != null)
				return new IJavaElement[] {element};
		} else if (path != null && path.isAbsolute()) {
			try {
				for (IClasspathEntry classpathEntry : currentProject.getRawClasspath()) {
					if (classpathEntry.getPath().equals(path)) {
						switch (classpathEntry.getEntryKind()) {
							case IClasspathEntry.CPE_LIBRARY:
								return new IJavaElement[] {currentProject.getPackageFragmentRoot(path.toString())};
							default:
								// keep looking
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		} else {
			try {
				for (IClasspathEntry classpathEntry : currentProject.getRawClasspath()) {
					if (classpathEntry.getPath().equals(path)) {
						return currentProject.findUnfilteredPackageFragmentRoots(classpathEntry);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}

	public static String encodeFiltered(ModuleEncapsulationDetail[] details, Class<?> detailClass) {
		return Arrays.stream(details)
				.filter(detailClass::isInstance)
				.map(ModuleEncapsulationDetail::toString)
				.collect(Collectors.joining(":")); //$NON-NLS-1$
	}

	/**
	 * Node in the tree of CPListElement et al, representing a patch-module directive.
	 */
	static class ModulePatch extends ModuleEncapsulationDetail {

		public static ModulePatch fromString(CPListElementAttribute attribElem, String value) {
			return new ModulePatch(value, attribElem);
		}

		public final String fModule;
		public final String fPaths;
		
		public ModulePatch(String value, CPListElementAttribute attribElem) {
			int eqIdx= value.indexOf('=');
			if (eqIdx == -1) {
				fModule= value;
				fPaths= null; // FIXME: find path to encl. project (src folder??)
			} else {
				fModule= value.substring(0, eqIdx);
				fPaths= value.substring(eqIdx + 1);
			}
			fAttribElem= attribElem;
		}

		public ModulePatch(String moduleName, String path, CPListElementAttribute attribElem) {
			fModule= moduleName;
			fPaths= path;
			fAttribElem= attribElem;
		}

		@Override
		public boolean affects(String module) {
			return module.equals(fModule);
		}

		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + ((fModule == null) ? 0 : fModule.hashCode());
			result= prime * result + ((fPaths == null) ? 0 : fPaths.hashCode());
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
			ModulePatch other= (ModulePatch) obj;
			if (fModule == null) {
				if (other.fModule != null)
					return false;
			} else if (!fModule.equals(other.fModule))
				return false;
			if (fPaths == null) {
				if (other.fPaths != null)
					return false;
			} else if (!fPaths.equals(other.fPaths))
				return false;
			return true;
		}

		@Override
		public String toString() {
			if (fPaths != null) {
				return fModule + '=' + fPaths;
			}
			return fModule;
		}
	}

	/** Shared implementation for ModuleAddExports & ModuleAddOpens (same structure). */
	abstract static class ModuleAddExpose extends ModuleEncapsulationDetail {

		public static ModuleAddExpose fromString(CPListElementAttribute attribElem, String value, boolean isExports) {
			int slash= value.indexOf('/');
			int equals= value.indexOf('=');
			if (slash != -1 && equals != -1 && equals > slash) {
				if (isExports)
					return new ModuleAddExport(value.substring(0, slash),
											value.substring(slash+1, equals),
											value.substring(equals+1),
											attribElem);
				else
					return new ModuleAddOpens(value.substring(0, slash),
							value.substring(slash+1, equals),
							value.substring(equals+1),
							attribElem);
			}
			return null;
		}

		public static Collection<ModuleAddExpose> fromMultiString(CPListElementAttribute attribElem, String values, boolean isExports) {
			List<ModuleAddExpose> exports= new ArrayList<>();
			for (String value : values.split(":")) { //$NON-NLS-1$
				ModuleAddExpose export= fromString(attribElem, value, isExports);
				if (export != null)
					exports.add(export);
			}
			return exports;
		}

		public final String fSourceModule;
		public final String fPackage;
		public final String fTargetModules;

		public ModuleAddExpose(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			fSourceModule= sourceModule;
			fPackage= aPackage;
			fTargetModules= targetModules;
			fAttribElem= attribElem;
		}

		@Override
		public boolean affects(String module) {
			return module.equals(fSourceModule);
		}

		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + ((fPackage == null) ? 0 : fPackage.hashCode());
			result= prime * result + ((fSourceModule == null) ? 0 : fSourceModule.hashCode());
			result= prime * result + ((fTargetModules == null) ? 0 : fTargetModules.hashCode());
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
			ModuleAddExpose other= (ModuleAddExpose) obj;
			if (fPackage == null) {
				if (other.fPackage != null)
					return false;
			} else if (!fPackage.equals(other.fPackage))
				return false;
			if (fSourceModule == null) {
				if (other.fSourceModule != null)
					return false;
			} else if (!fSourceModule.equals(other.fSourceModule))
				return false;
			if (fTargetModules == null) {
				if (other.fTargetModules != null)
					return false;
			} else if (!fTargetModules.equals(other.fTargetModules))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return fSourceModule+'/'+fPackage+'='+fTargetModules;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-exports module directive.
	 */
	static class ModuleAddExport extends ModuleAddExpose {
		public ModuleAddExport(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			super(sourceModule, aPackage, targetModules, attribElem);
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-opens module directive.
	 */
	static class ModuleAddOpens extends ModuleAddExpose {
		public ModuleAddOpens(String sourceModule, String aPackage, String targetModules, CPListElementAttribute attribElem) {
			super(sourceModule, aPackage, targetModules, attribElem);
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing an add-reads module directive.
	 */
	static class ModuleAddReads extends ModuleEncapsulationDetail {

		public static ModuleAddReads fromString(CPListElementAttribute attribElem, String value) {
			int equals= value.indexOf('=');
			if (equals != -1) {
				return new ModuleAddReads(value.substring(0, equals),
											value.substring(equals+1),
											attribElem);
			}
			return null;
		}

		public static Collection<ModuleAddReads> fromMultiString(CPListElementAttribute attribElem, String values) {
			List<ModuleAddReads> readss= new ArrayList<>();
			for (String value : values.split(":")) { //$NON-NLS-1$
				ModuleAddReads reads= fromString(attribElem, value);
				if (reads != null)
					readss.add(reads);
			}
			return readss;
		}

		public final String fSourceModule;
		public final String fTargetModule;

		public ModuleAddReads(String sourceModule, String targetModule, CPListElementAttribute attribElem) {
			fSourceModule= sourceModule;
			fTargetModule= targetModule;
			fAttribElem= attribElem;
		}

		@Override
		public boolean affects(String module) {
			return module.equals(fSourceModule);
		}

		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + ((fSourceModule == null) ? 0 : fSourceModule.hashCode());
			result= prime * result + ((fTargetModule == null) ? 0 : fTargetModule.hashCode());
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
			ModuleAddReads other= (ModuleAddReads) obj;
			if (fSourceModule == null) {
				if (other.fSourceModule != null)
					return false;
			} else if (!fSourceModule.equals(other.fSourceModule))
				return false;
			if (fTargetModule == null) {
				if (other.fTargetModule != null)
					return false;
			} else if (!fTargetModule.equals(other.fTargetModule))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return fSourceModule+'='+fTargetModule;
		}
	}

	/**
	 * Node in the tree of CPListElement et al, representing a limit-modules directive.
	 */
	static class LimitModules extends ModuleEncapsulationDetail {
		
		public static LimitModules fromString(CPListElementAttribute attribElem, String value) {
			String[] modules= value.split(","); //$NON-NLS-1$
			for (int i= 0; i < modules.length; i++) {
				modules[i]= modules[i].trim();
			}
			return new LimitModules(Arrays.asList(modules), attribElem);
		}
		
		public final List<String> fExplicitlyIncludedModules;

		public LimitModules(List<String> explicitlyIncludedModules, CPListElementAttribute attribElem) {
			fExplicitlyIncludedModules= explicitlyIncludedModules;
			fAttribElem= attribElem;
		}
		@Override
		public boolean affects(String module) {
			return false; // no change on the module, just on the module graph / set of root modules
		}
		@Override
		public String toString() {
			return String.join(",", fExplicitlyIncludedModules); //$NON-NLS-1$
		}
	}

	public abstract boolean affects(String module);
}
