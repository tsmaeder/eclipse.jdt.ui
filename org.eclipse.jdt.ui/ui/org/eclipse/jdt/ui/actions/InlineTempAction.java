/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.InlineTempInputPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

/**
 * Inlines the value of a local variable at all places where a read reference
 * is used.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class InlineTempAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private static final String DIALOG_MESSAGE_TITLE= RefactoringMessages.getString("InlineTempAction.inline_temp");//$NON-NLS-1$
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public InlineTempAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	public InlineTempAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("InlineTempAction.label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.INLINE_ACTION);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void selectionChanged(ITextSelection selection) {
		//do nothing
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */		
	protected void run(ITextSelection selection) {
		try{
			Refactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), DIALOG_MESSAGE_TITLE, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, DIALOG_MESSAGE_TITLE, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new InlineTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}

	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		String helpId= IJavaHelpContextIds.INLINE_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= RefactoringMessages.getString("InlineTempAction.inline_temp"); //$NON-NLS-1$
		RefactoringWizard result= new RefactoringWizard((InlineTempRefactoring)refactoring, pageTitle, helpId) {
			protected void addUserInputPages() {
				addPage(new InlineTempInputPage());
			}
			protected int getMessageLineWidthInChars() {
				return 0;
			}
		};
		result.setExpandFirstNode(true);
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		//do nothing
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(false);
	}

}
