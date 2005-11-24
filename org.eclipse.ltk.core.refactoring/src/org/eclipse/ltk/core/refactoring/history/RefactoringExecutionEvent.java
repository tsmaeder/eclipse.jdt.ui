/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring.history;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.ltk.internal.core.refactoring.Assert;

/**
 * Event object to communicate refactoring execution notifications. These
 * include before-the-fact notification of perform, undo and redo refactoring
 * operations as well as after-the-fact notification of the above refactoring
 * operations.
 * <p>
 * Refactoring execution listeners must be prepared to receive notifications
 * from a background thread. Any UI access occurring inside the implementation
 * must be properly synchronized using the techniques specified by the client's
 * widget library.
 * </p>
 * <p>
 * Note: This API is considered experimental and may change in the near future.
 * </p>
 * 
 * @since 3.2
 */
public final class RefactoringExecutionEvent {

	/** Event type indicating that a refactoring is about to be performed (value 4) */
	public static final int ABOUT_TO_PERFORM= 4;

	/** Event type indicating that a refactoring is about to be redone (value 6) */
	public static final int ABOUT_TO_REDO= 6;

	/** Event type indicating that a refactoring is about to be undone (value 5) */
	public static final int ABOUT_TO_UNDO= 5;

	/** Event type indicating that a refactoring has been performed (value 1) */
	public static final int PERFORMED= 1;

	/** Event type indicating that a refactoring has been performed (value 3) */
	public static final int REDONE= 3;

	/** Event type indicating that a refactoring has been undone (value 2) */
	public static final int UNDONE= 2;

	/** The refactoring descriptor */
	private final RefactoringDescriptor fDescriptor;

	/** The refactoring history service */
	private final IRefactoringHistoryService fService;

	/** The event type */
	private final int fType;

	/**
	 * Creates a new refactoring execution event.
	 * 
	 * @param service
	 *            the refactoring history service
	 * @param type
	 *            the event type
	 * @param descriptor
	 *            the refactoring descriptor
	 */
	public RefactoringExecutionEvent(final IRefactoringHistoryService service, final int type, final RefactoringDescriptor descriptor) {
		Assert.isNotNull(service);
		Assert.isNotNull(descriptor);
		fService= service;
		fType= type;
		fDescriptor= descriptor;
	}

	/**
	 * Returns the refactoring descriptor.
	 * 
	 * @return the refactoring descriptor
	 */
	public RefactoringDescriptor getDescriptor() {
		return fDescriptor;
	}

	/**
	 * Returns the event type.
	 * 
	 * @return the event type
	 */
	public int getEventType() {
		return fType;
	}

	/**
	 * Returns the refactoring history service
	 * 
	 * @return the refactoring history service
	 */
	public IRefactoringHistoryService getHistoryService() {
		return fService;
	}
}
