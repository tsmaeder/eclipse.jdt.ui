/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.ContextType;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.GlobalVariables;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;

/**
  */
public class CodeTemplateContextType extends ContextType {
	
	public static final String CATCHBLOCK_CONTEXTTYPE= "catchblock_context"; //$NON-NLS-1$
	public static final String METHODBODY_CONTEXTTYPE= "methodbody_context"; //$NON-NLS-1$
	public static final String CONSTRUCTORBODY_CONTEXTTYPE= "constructorbody_context"; //$NON-NLS-1$
	public static final String GETTERBODY_CONTEXTTYPE= "getterbody_context"; //$NON-NLS-1$
	public static final String SETTERBODY_CONTEXTTYPE= "setterbody_context"; //$NON-NLS-1$
	public static final String NEWTYPE_CONTEXTTYPE= "newtype_context"; //$NON-NLS-1$
	public static final String TYPECOMMENT_CONTEXTTYPE= "typecomment_context"; //$NON-NLS-1$
	public static final String FIELDCOMMENT_CONTEXTTYPE= "fieldcomment_context"; //$NON-NLS-1$
	public static final String METHODCOMMENT_CONTEXTTYPE= "methodcomment_context"; //$NON-NLS-1$
	public static final String CONSTRUCTORCOMMENT_CONTEXTTYPE= "constructorcomment_context"; //$NON-NLS-1$
	public static final String OVERRIDECOMMENT_CONTEXTTYPE= "overridecomment_context"; //$NON-NLS-1$
	public static final String GETTERCOMMENT_CONTEXTTYPE= "gettercomment_context"; //$NON-NLS-1$
	public static final String SETTERCOMMENT_CONTEXTTYPE= "settercomment_context"; //$NON-NLS-1$

	public static final String EXCEPTION_TYPE= "exception_type"; //$NON-NLS-1$
	public static final String EXCEPTION_VAR= "exception_var"; //$NON-NLS-1$
	public static final String ENCLOSING_METHOD= "enclosing_method"; //$NON-NLS-1$
	public static final String ENCLOSING_TYPE= "enclosing_type"; //$NON-NLS-1$
	public static final String BODY_STATEMENT= "body_statement"; //$NON-NLS-1$
	public static final String FIELD= "field"; //$NON-NLS-1$
	public static final String FIELD_TYPE= "field_type"; //$NON-NLS-1$
	public static final String BARE_FIELD_NAME= "bare_field_name"; //$NON-NLS-1$
	
	
	public static final String PARAM= "param"; //$NON-NLS-1$
	public static final String RETURN_TYPE= "return_type"; //$NON-NLS-1$
	public static final String SEE_TAG= "see_to_overridden"; //$NON-NLS-1$
	
	public static final String TAGS= "tags"; //$NON-NLS-1$
	
	public static final String TYPENAME= "type_name"; //$NON-NLS-1$
	public static final String FILENAME= "file_name"; //$NON-NLS-1$
	public static final String PACKAGENAME= "package_name"; //$NON-NLS-1$
	public static final String PROJECTNAME= "project_name"; //$NON-NLS-1$

	public static final String PACKAGE_DECLARATION= "package_declaration"; //$NON-NLS-1$
	public static final String TYPE_DECLARATION= "type_declaration"; //$NON-NLS-1$
	public static final String TYPE_COMMENT= "typecomment"; //$NON-NLS-1$
	
	public static class CodeTemplateVariableResolver extends TemplateVariableResolver {
		public CodeTemplateVariableResolver(String type, String description) {
			super(type, description);
		}
		
		protected String resolve(TemplateContext context) {
			return context.getVariable(getType());
		}
	}
	
	public static class TagsVariableResolver extends TemplateVariableResolver {
		public TagsVariableResolver() {
			super(TAGS,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.tags")); //$NON-NLS-1$
		}
		
		protected String resolve(TemplateContext context) {
			return "@"; //$NON-NLS-1$
		}
	}	
		
	protected static class Todo extends TemplateVariableResolver {

		public Todo() {
			super("todo", JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.todo")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		protected String resolve(TemplateContext context) {
			String todoTaskTag= StubUtility.getTodoTaskTag(((CodeTemplateContext) context).getJavaProject());
			if (todoTaskTag == null)
				return "XXX"; //$NON-NLS-1$
	
			return todoTaskTag;
		}
	}
	
	private boolean fIsComment;
	
	public CodeTemplateContextType(String contextName) {
		super(contextName);
		
		fIsComment= false;
		
		// global
		addResolver(new GlobalVariables.Dollar());
		addResolver(new GlobalVariables.Date());
		addResolver(new GlobalVariables.Year());
		addResolver(new GlobalVariables.Time());
		addResolver(new GlobalVariables.User());
		addResolver(new Todo());
		
		if (CATCHBLOCK_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(EXCEPTION_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.exceptiontype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(EXCEPTION_VAR,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.exceptionvar"))); //$NON-NLS-1$
		} else if (METHODBODY_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingmethod"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(BODY_STATEMENT,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.bodystatement"))); //$NON-NLS-1$
		} else if (CONSTRUCTORBODY_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(BODY_STATEMENT,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.bodystatement"))); //$NON-NLS-1$
		} else if (GETTERBODY_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingmethod"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldname"))); //$NON-NLS-1$
		} else if (SETTERBODY_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingmethod"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldname"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(PARAM, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.param"))); //$NON-NLS-1$
		} else if (NEWTYPE_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.typename"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(PACKAGE_DECLARATION,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.packdeclaration"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(TYPE_DECLARATION,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.typedeclaration"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(TYPE_COMMENT,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.typecomment"))); //$NON-NLS-1$
			addCompilationUnitVariables();
		} else if (TYPECOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(TYPENAME,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.typename"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (FIELDCOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.fieldtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.fieldname"))); //$NON-NLS-1$
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (METHODCOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingmethod"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(RETURN_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.returntype"))); //$NON-NLS-1$
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (OVERRIDECOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_METHOD,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingmethod"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(SEE_TAG,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.seetag"))); //$NON-NLS-1$
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (CONSTRUCTORCOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new TagsVariableResolver());
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (GETTERCOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldname"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(BARE_FIELD_NAME, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.barefieldname"))); //$NON-NLS-1$
			addCompilationUnitVariables();
			fIsComment= true;
		} else if (SETTERCOMMENT_CONTEXTTYPE.equals(contextName)) {
			addResolver(new CodeTemplateVariableResolver(ENCLOSING_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.enclosingtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldtype"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(FIELD, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.getterfieldname"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(PARAM, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.param"))); //$NON-NLS-1$
			addResolver(new CodeTemplateVariableResolver(BARE_FIELD_NAME, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.barefieldname"))); //$NON-NLS-1$
			addCompilationUnitVariables();
			fIsComment= true;
		}
	}
	
	private void addCompilationUnitVariables() {
		addResolver(new CodeTemplateVariableResolver(FILENAME, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.filename"))); //$NON-NLS-1$
		addResolver(new CodeTemplateVariableResolver(PACKAGENAME, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.packagename"))); //$NON-NLS-1$
		addResolver(new CodeTemplateVariableResolver(PROJECTNAME, JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.projectname"))); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#validateVariables(org.eclipse.jdt.internal.corext.template.TemplateVariable[])
	 */
	protected String validateVariables(TemplateVariable[] variables) {
		ArrayList required=  new ArrayList(5);
		String contextName= getName();
		if (NEWTYPE_CONTEXTTYPE.equals(contextName)) {
			required.add(PACKAGE_DECLARATION);
			required.add(TYPE_DECLARATION);
		}
		for (int i= 0; i < variables.length; i++) {
			String type= variables[i].getType();
			if (getResolver(type) == null) {
				return JavaTemplateMessages.getFormattedString("CodeTemplateContextType.validate.unknownvariable", type); //$NON-NLS-1$
			}
			required.remove(type);
		}
		if (!required.isEmpty()) {
			String missing= (String) required.get(0);
			return JavaTemplateMessages.getFormattedString("CodeTemplateContextType.validate.missingvariable", missing); //$NON-NLS-1$
		}
		return super.validateVariables(variables);
	}	
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#createContext()
	 */
	public TemplateContext createContext() {
		return null;
	}

	public static void registerContextTypes(ContextTypeRegistry registry) {
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CATCHBLOCK_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.METHODBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CONSTRUCTORBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.GETTERBODY_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.SETTERBODY_CONTEXTTYPE));		
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.NEWTYPE_CONTEXTTYPE));
		
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.TYPECOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.FIELDCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.METHODCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.CONSTRUCTORCOMMENT_CONTEXTTYPE));
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.OVERRIDECOMMENT_CONTEXTTYPE));		
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.GETTERCOMMENT_CONTEXTTYPE));		
		registry.addContextType(new CodeTemplateContextType(CodeTemplateContextType.SETTERCOMMENT_CONTEXTTYPE));		
	}
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#validate(java.lang.String)
	 */
	public String validate(String pattern) {
		String message= super.validate(pattern);
		if (message != null) {
			return message;
		}
		if (fIsComment) {
			if (!isValidComment(pattern)) {
				return JavaTemplateMessages.getString("CodeTemplateContextType.validate.invalidcomment"); //$NON-NLS-1$
			}
		}
		return null;
	}
		
	
	private boolean isValidComment(String template) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(template.toCharArray());
		try {
			int next= scanner.getNextToken();
			while (next == ITerminalSymbols.TokenNameCOMMENT_LINE || next == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || next == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
				next= scanner.getNextToken();
			}
			return next == ITerminalSymbols.TokenNameEOF;
		} catch (InvalidInputException e) {
		}
		return false;
	}	

}
