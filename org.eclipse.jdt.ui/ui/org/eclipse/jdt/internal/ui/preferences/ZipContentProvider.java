/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.zip.ZipFile;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * An adapter for presenting a zip file in a tree viewer.
 */

public class ZipContentProvider implements ITreeContentProvider {

	private String fInitialSelection;
	private ZipTreeNode fTree;
	
	public ZipContentProvider(String initialSelection, ZipFile zipFile){
		fInitialSelection= initialSelection;
		fTree= ZipTreeNode.newZipTree((ZipFile) zipFile);
	}
	
	public ZipTreeNode getSelectedNode(){
		return fTree.findNode(fInitialSelection);
	}
	
	/**
	 * @see ITreeContentProvider#inputChanged
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	  * @see ITreeContentProvider#getParent
	  */
	public Object getParent(Object element) {
		return ((ZipTreeNode) element).getParent();
	}

	/**
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element) {
		return ((ZipTreeNode) element).hasChildren();
	}

	/**
	 * @see ITreeContentProvider#getChildren
	 */
	public Object[] getChildren(Object element) {
		return ((ZipTreeNode) element).getChildren();
	}

	/**
	 * @see ITreeContentProvider#getElements
	 */
	public Object[] getElements(Object zipFile) {
		 return fTree.getChildren();
	}

	/**
	 * @see ITreeContentProvider#isDeleted
	 */
	public boolean isDeleted(Object p0) {
		return false;
	}
	/**
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
	}

}


