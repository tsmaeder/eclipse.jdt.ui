package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class PullUpRefactoring extends Refactoring {

	private static final String PREF_TAB_SIZE= "org.eclipse.jdt.core.formatter.tabulation.size";

	private final CodeGenerationSettings fPreferenceSettings;
	private IMember[] fElementsToPullUp;
	private IMethod[] fMethodsToDelete;

	//caches
	private IType fCachedSuperType;
	private ITypeHierarchy fCachedSuperTypeHierarchy;
	private IType fCachedDeclaringType;

	public PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(elements.length > 0);
		Assert.isNotNull(preferenceSettings);
		fElementsToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMethodsToDelete= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Pull Up";
	}
	
	/**
	 * Sets the methodsToDelete.
	 * @param methodsToDelete The methodsToDelete to set
	 * no validation is done on these methods - they will simply be removed. 
	 * it is the caller's resposibility to ensure that the selection makes sense from the behavior-preservation point of view.
	 */
	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete= getOriginals(methodsToDelete);
	}
	
	private IMember[] getMembersToDelete(IProgressMonitor pm){
		try{
			pm.beginTask("", 1);
			IMember[] matchingFields= getMembersOfType(getMatchingElements(new SubProgressMonitor(pm, 1)), IJavaElement.FIELD);
			return merge(getOriginals(matchingFields), fMethodsToDelete);
		} catch (JavaModelException e){
			//fallback
			return fMethodsToDelete;
		}	finally{
			pm.done();
		}
	}
	
	public IMember[] getElementsToPullUp() throws JavaModelException {
		return fElementsToPullUp;
	}
	
	public IType getDeclaringType(){
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		//all members declared in same type - checked in precondition
		fCachedDeclaringType= fElementsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		return fCachedDeclaringType;
	}
	
	//XXX added for performance reasons
	public ITypeHierarchy getSuperTypeHierarchy(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			if (fCachedSuperTypeHierarchy != null)
				return fCachedSuperTypeHierarchy;
			fCachedSuperTypeHierarchy= getSuperType(new SubProgressMonitor(pm, 1)).newTypeHierarchy(new SubProgressMonitor(pm, 1));
			return fCachedSuperTypeHierarchy;
		} finally{
			pm.done();
		}	
	}
	
	public IType getSuperType(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1);
			if (fCachedSuperType != null)
				return fCachedSuperType;
			IType declaringType= getDeclaringType();
			ITypeHierarchy st= declaringType.newSupertypeHierarchy(new SubProgressMonitor(pm, 1));
			fCachedSuperType= st.getSuperclass(declaringType);
			return fCachedSuperType;
		} finally{
			pm.done();
		}			
	}
	
	public IMember[] getMatchingElements(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			Set result= new HashSet();
			IType superType= getSuperType(new SubProgressMonitor(pm, 1));
			ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] subTypes= hierarchy.getAllSubtypes(superType);
			for (int i = 0; i < subTypes.length; i++) {
				result.addAll(getMatchingMembers(hierarchy, subTypes[i]));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally{
			pm.done();
		}				
	}
	
	//Set of IMembers
	private Set getMatchingMembers(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Set result= new HashSet();
		Map mapping= getMatchingMembersMapping(hierarchy, type);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			result.addAll((Set)mapping.get(iter.next()));
		}
		return result;
	}

	/*
	 * mapping: Map: IMember -> Set of IMembers (of the same type as key)
	 */	
	private static void addToMapping(Map mapping, IMember key, IMember matchingMethod){
		Set matchingSet;
		if (mapping.containsKey(key)){
			matchingSet= (Set)mapping.get(key);
		}else{
			matchingSet= new HashSet();
			mapping.put(key, matchingSet);
		}
		matchingSet.add(matchingMethod);
	}
	
	/*
	 * mapping: Map: IMember -> Set of IMembers (of the same type as key)
	 */
	private static void addToMapping(Map mapping, IMember key, Set matchingMembers){
		for (Iterator iter= matchingMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			Assert.isTrue(key.getElementType() == member.getElementType());
			addToMapping(mapping, key, member);
		}
	}
	
	private Map getMatchingMembersMapping(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Map result= new HashMap();//IMember -> Set of IMembers (of the same type as key)
		
		//current type
		for (int i = 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() == IJavaElement.METHOD){
				IMethod method= (IMethod)fElementsToPullUp[i];
				IMethod found= MemberCheckUtil.findMethod(method, type.getMethods());
				if (found != null)
					addToMapping(result, method, found);
			} else if (fElementsToPullUp[i].getElementType() == IJavaElement.FIELD){
				IField field= (IField)fElementsToPullUp[i];
				IField found= type.getField(field.getElementName());
				if (found.exists())
					addToMapping(result, field, found);
			}
		}
		
		//subtypes
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		for (int i = 0; i < subTypes.length; i++) {
			IType subType = subTypes[i];
			Map subTypeMapping= getMatchingMembersMapping(hierarchy, subType);
			for (Iterator iter= subTypeMapping.keySet().iterator(); iter.hasNext();) {
				IMember m= (IMember) iter.next();
				addToMapping(result, m, (Set)subTypeMapping.get(m));
			}
		}
		return result;		
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
			RefactoringStatus result= new RefactoringStatus();
						
			result.merge(checkAllElements());
			if (result.hasFatalError())
				return result;
			
			if (! haveCommonDeclaringType())
				return RefactoringStatus.createFatalErrorStatus("All selected elements must be declared in the same type.");			

			return new RefactoringStatus();
	}
		
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		try {
			pm.beginTask("", 3);
			RefactoringStatus result= new RefactoringStatus();
						
			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			if (getSuperType(new SubProgressMonitor(pm, 1)) == null)
				return RefactoringStatus.createFatalErrorStatus("Pull up not allowed.");
			if (getSuperType(new SubProgressMonitor(pm, 1)).isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in subtypes of binary types.");

			fElementsToPullUp= getOriginals(fElementsToPullUp);
			for (int i= 0; i < fElementsToPullUp.length; i++) {
				IMember orig= fElementsToPullUp[i];
				if (orig == null || ! orig.exists())
					result.addFatalError("Element " + orig.getElementName() + " does not exist in the saved version of the file.");
			}
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}
	
	private static IMethod[] getOriginals(IMethod[] methods){
		IMethod[] result= new IMethod[methods.length];
		for (int i= 0; i < methods.length; i++) {
			result[i]= (IMethod)WorkingCopyUtil.getOriginal(methods[i]);
		}
		return result;
	}
	
	private static IMember[] getOriginals(IMember[] members){
		IMember[] result= new IMember[members.length];
		for (int i= 0; i < members.length; i++) {
			result[i]= (IMember)WorkingCopyUtil.getOriginal(members[i]);
		}
		return result;
	}
	
	private static IMember[] merge(IMember[] a1, IMember[] a2){
		Set result= new HashSet(a1.length + a2.length);
		result.addAll(Arrays.asList(a1));
		result.addAll(Arrays.asList(a2));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
	
	private static IMember[] getMembersOfType(IMember[] members, int type){
		List list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 4);
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(MemberCheckUtil.checkMembersInDestinationType(fElementsToPullUp, getSuperType(new SubProgressMonitor(pm, 1))));
			pm.worked(1);
			result.merge(checkMembersInSubclasses(new SubProgressMonitor(pm, 1)));
			return result;
		} finally {
			pm.done();
		}	
	}

	//--- private helpers
	
	private RefactoringStatus checkAllElements() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fElementsToPullUp.length; i++) {
			IMember member = fElementsToPullUp[i];

			if (member.getElementType() != IJavaElement.METHOD && 
				member.getElementType() != IJavaElement.FIELD)
					return RefactoringStatus.createFatalErrorStatus("Pull up is allowed only on fields and methods.");			
			if (! member.exists())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements that do not exist.");			
	
			if (member.isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on binary elements.");	

			if (member.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on read-only elements.");					

			if (! member.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements with unknown structure.");					

			if (JdtFlags.isStatic(member)) //for now
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on static elements.");
			
			if (member.getElementType() == IJavaElement.METHOD)
				return checkMethod((IMethod)member);
		}
		return null;
	}
	
	private static RefactoringStatus checkMethod(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on constructors.");			
			
		if (JdtFlags.isAbstract(method))
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on abstract methods.");
			
 		if (JdtFlags.isNative(method)) //for now - move to input preconditions
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on native methods.");				

		return null;	
	}
	
	private RefactoringStatus checkDeclaringType(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 3);
		IType declaringType= getDeclaringType();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on interface members.");
		
		if (JavaModelUtil.getFullyQualifiedName(declaringType).equals("java.lang.Object"))
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in java.lang.Object.");	

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in binary types.");	

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in read-only types.");	
	
		if (getSuperType(new SubProgressMonitor(pm, 1)) == null)	
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in this type.");	
			
		if (getSuperType(new SubProgressMonitor(pm, 1)).isBinary())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in subclasses of binary types.");	

		if (getSuperType(new SubProgressMonitor(pm, 1)).isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in subclasses of read-only types.");	
		
		return null;
	}
	
	private boolean haveCommonDeclaringType(){
		IType declaringType= fElementsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (! declaringType.equals(fElementsToPullUp[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	private RefactoringStatus checkFinalFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", fElementsToPullUp.length);
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() == IJavaElement.FIELD && JdtFlags.isFinal(fElementsToPullUp[i])){
				Context context= JavaSourceContext.create(fElementsToPullUp[i]);
				result.addWarning("Pulling up final fields will result in compilation errors if they are not initialized on creation or in constructors", context);
			}
			pm.worked(1);
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("Checking referenced elements", 3);
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1)));
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= ReferenceFinderUtil.getTypesReferencedIn(fElementsToPullUp, pm);
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! iType.exists())
				continue;
			
			if (! canBeAccessedFrom(iType, superType)){
				String msg= "Type '" + JavaModelUtil.getFullyQualifiedName(iType) + "' referenced in one of the pulled elements is not accessible from type '" 
								+ JavaModelUtil.getFullyQualifiedName(superType) + "'.";
				result.addError(msg, JavaSourceContext.create(iType));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedFields.length; i++) {
			IField iField= accessedFields[i];
			if (! iField.exists())
				continue;
			
			if (! canBeAccessedFrom(iField, superType) && ! pulledUpList.contains(iField) && !deletedList.contains(iField)){
				String msg= "Field '" + iField.getElementName() + "' referenced in one of the pulled elements is not accessible from type '" 
								+ JavaModelUtil.getFullyQualifiedName(superType) + "'.";
				result.addError(msg, JavaSourceContext.create(iField));
			}	
		}
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod iMethod= accessedMethods[i];
			if (! iMethod.exists())
				continue;
			if (! canBeAccessedFrom(iMethod, superType) && ! pulledUpList.contains(iMethod) && !deletedList.contains(iMethod)){
				String msg= "Method '" + JavaElementUtil.createMethodSignature(iMethod) + "' referenced in one of the pulled elements is not accessible from type '" 
								+ JavaModelUtil.getFullyQualifiedName(superType) + "'.";
				result.addError(msg, JavaSourceContext.create(iMethod));
			}	
		}
		return result;
	}
	
	private boolean canBeAccessedFrom(IMember member, IType newHome) throws JavaModelException{
		Assert.isTrue(!(member instanceof IInitializer));
		if (newHome.equals(member.getDeclaringType()))
			return true;
			
		if (newHome.equals(member))
			return true;	
		
		if (! member.exists())
			return false;
			
		if (JdtFlags.isPrivate(member))
			return false;
			
		if (member.getDeclaringType() == null){ //top level
			if (! (member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;
				
			//FIX ME: protected and default treated the same
			return ((IType)member).getPackageFragment().equals(newHome.getPackageFragment());		
		}	

		 if (! canBeAccessedFrom(member.getDeclaringType(), newHome))
		 	return false;

		if (member.getDeclaringType().equals(getDeclaringType())) //XXX
			return false;
			
		if (JdtFlags.isPublic(member))
			return true;
		 
		//FIX ME: protected and default treated the same
		return (member.getDeclaringType().getPackageFragment().equals(newHome.getPackageFragment()));		
	}
	
	private RefactoringStatus checkMembersInSubclasses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		Set notDeletedMembers= getNotDeletedMembers();
		checkAccessModifiers(result, notDeletedMembers);
		checkMethodReturnTypes(pm, result, notDeletedMembers);
		checkFieldTypes(pm, result);
		return result;
	}

	private void checkMethodReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
		ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
		Map mapping= getMatchingMembersMapping(hierarchy, hierarchy.getType());//IMember -> Set of IMembers
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.METHOD)
				continue;
			IMethod method= (IMethod)fElementsToPullUp[i];
			String returnType= getReturnTypeName(method);
			for (Iterator iter= ((Set)mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (!notDeletedMembers.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String msg= "Method '" + JavaElementUtil.createMethodSignature(matchingMethod) + "' declared in type'"
									 + JavaModelUtil.getFullyQualifiedName(matchingMethod.getDeclaringType())
									 + "' has a different return type than its pulled up counterpart, which will result in compile errors if you proceed." ;
				Context context= JavaSourceContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(msg, context);	
			}
		}
	}

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
		Map mapping= getMatchingMembersMapping(hierarchy, hierarchy.getType());//IMember -> Set of IMembers
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)fElementsToPullUp[i];
			String type= getTypeName(field);
			for (Iterator iter= ((Set)mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (type.equals(getTypeName(matchingField)))
					continue;
				String msg= "Field '" + matchingField.getElementName() + "' declared in type'"
									 + JavaModelUtil.getFullyQualifiedName(matchingField.getDeclaringType())
									 + "' has a different type than its pulled up counterpart." ;
				Context context= JavaSourceContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());					 
				result.addError(msg, context);	
			}
		}
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
		for (Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			
			if (member.getElementType() == IJavaElement.METHOD)
				checkMethodAccessModifiers(result, ((IMethod) member));
		}
	}
	
	private void checkMethodAccessModifiers(RefactoringStatus result, IMethod method) throws JavaModelException {
		Context errorContext= JavaSourceContext.create(method);
		
		if (JdtFlags.isStatic(method)){
				String msg= "Method '" + JavaElementUtil.createMethodSignature(method) + "' declared in type '" 
									 + JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())
									 + "' is 'static', which will result in compile errors if you proceed." ;
				result.addError(msg, errorContext);
		 } 
		 if (isVisibilityLowerThanProtected(method)){
			String msg= "Method '" + JavaElementUtil.createMethodSignature(method)+ "' declared in type '" 
								 + JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())
								 + "' has visibility lower than 'protected', which will result in compile errors if you proceed." ;
			result.addError(msg, errorContext);	
		} 
	}

	private static String getReturnTypeName(IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}
	
	private static String getTypeName(IField field) throws JavaModelException {
		return Signature.toString(field.getTypeSignature());
	}

	private Set getNotDeletedMembers() throws JavaModelException {
		Set matchingSet= new HashSet();
		matchingSet.addAll(Arrays.asList(getMatchingElements(new NullProgressMonitor())));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new NullProgressMonitor())));
		return matchingSet;
	}
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! (JdtFlags.isPublic(member) || JdtFlags.isProtected(member));
	}
	
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("preparing preview", 4);
			
			TextChangeManager manager= new TextChangeManager();
			
			addCopyMembersChange(manager);
			pm.worked(1);
			
			if (needsAddingImports())
				addAddImportsChange(manager, new SubProgressMonitor(pm, 1));
			pm.worked(1);
			
			addDeleteMembersChange(manager);
			pm.worked(1);
			
			pm.worked(1);
			return new CompositeChange("Pull Up", manager.getAllChanges());
		} catch(CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}

	private boolean needsAddingImports() throws JavaModelException {
		return ! getSuperType(new NullProgressMonitor()).getCompilationUnit().equals(getDeclaringType().getCompilationUnit());
	}
	
	private void addCopyMembersChange(TextChangeManager manager) throws CoreException {
		for (int i = fElementsToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			addCopyChange(manager, fElementsToPullUp[i]);
		}
	}
	
	private void addCopyChange(TextChangeManager manager, IMember member) throws CoreException {
		String source= computeNewSource(member);
		String changeName= getCopyChangeName(member);
									
		if (needsToChangeVisibility(member))
			changeName += " (changing its visibility to '"+ "protected" + "')";
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(source));
	}

	private String getCopyChangeName(IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.METHOD)
			return "copy method '" + JavaElementUtil.createMethodSignature((IMethod)member) 
										+ "' to type '" + getDeclaringType().getElementName() + "'";
		
		else return "copy field '" + member.getElementName() 
										+ "' to type '" + getDeclaringType().getElementName() + "'";
	}
		
	private TextEdit createAddMemberEdit(String methodSource) throws JavaModelException {
		int tabWidth=getTabWidth();
		IMethod sibling= getLastMethod(getSuperType(new NullProgressMonitor()));
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, new String[]{ methodSource}, tabWidth);
		return
			new MemberEdit(getSuperType(new NullProgressMonitor()), MemberEdit.ADD_AT_END, new String[]{ methodSource}, tabWidth);
	}

	private void addDeleteMembersChange(TextChangeManager manager) throws CoreException {
		IMember[] membersToDelete= getMembersToDelete(new NullProgressMonitor());
		for (int i = 0; i < membersToDelete.length; i++) {
			String changeName= getDeleteChangeName(membersToDelete[i]);
			DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(membersToDelete[i], membersToDelete[i].getCompilationUnit());
			manager.get(WorkingCopyUtil.getWorkingCopyIfExists(membersToDelete[i].getCompilationUnit())).addTextEdit(changeName, edit);
		}
	}
	
	private static String getDeleteChangeName(IMember member) throws JavaModelException{
		if (member.getElementType() == IJavaElement.METHOD){
			IMethod method = (IMethod)member;
			return "Delete method '"
						+ JavaElementUtil.createMethodSignature(method)	 
						+ "' declared in type '" + JavaModelUtil.getFullyQualifiedName(method.getDeclaringType()) + "'";
		} else {
			return "Delete field '"
						+ member.getElementName()	 
						+ "' declared in type '" + JavaModelUtil.getFullyQualifiedName(member.getDeclaringType()) + "'";	
		}
	}
	
	private void addAddImportsChange(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());		
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(fElementsToPullUp, pm);
		ImportEdit importEdit= new ImportEdit(cu, fPreferenceSettings);
		for (int i= 0; i < referencedTypes.length; i++) {
			IType iType= referencedTypes[i];
			importEdit.addImport(JavaModelUtil.getFullyQualifiedName(iType));
		}
		if (! importEdit.isEmpty())
			manager.get(cu).addTextEdit("Update imports", importEdit);
	}

	private static int getTabWidth() {
		return Integer.parseInt((String)JavaCore.getOptions().get(PREF_TAB_SIZE));
	}
	
	private static boolean needsToChangeVisibility(IMember method) throws JavaModelException {
		return ! (JdtFlags.isPublic(method) || JdtFlags.isProtected(method));
	}
	
	private static String computeNewSource(IMember member) throws JavaModelException {
		String source;
		
		if (member.getElementType() == IJavaElement.METHOD)
			source= replaceSuperCalls((IMethod)member);
		else
			source= SourceReferenceSourceRangeComputer.computeSource(member);

		if (! needsToChangeVisibility(member))
			return source;
			
		if (JdtFlags.isPrivate(member))
			return substitutePrivateWithProtected(source);
		return "protected " + removeLeadingWhiteSpaces(source);
	}

	private static String replaceSuperCalls(IMethod method) throws JavaModelException {
		ISourceRange[] superRefOffsert= SourceRange.reverseSortByOffset(SuperReferenceFinder.findSuperReferenceRanges(method));
		
		StringBuffer source= new StringBuffer(SourceReferenceSourceRangeComputer.computeSource(method));
		ISourceRange originalMethodRange= SourceReferenceSourceRangeComputer.computeSourceRange(method, method.getCompilationUnit());
		
		for (int i= 0; i < superRefOffsert.length; i++) {
			int start= superRefOffsert[i].getOffset() - originalMethodRange.getOffset();
			int end= start + superRefOffsert[i].getLength();
			source.replace(start, end, "this");
		}
		return source.toString();
	}
	
	private static String removeLeadingWhiteSpaces(String s){
		if ("".equals(s))
			return "";

		if ("".equals(s.trim()))	
			return "";
			
		int i= 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))){
			i++;
		}
		if (i == s.length())
			return "";
		return s.substring(i);
	}
	
	private static String substitutePrivateWithProtected(String methodSource) throws JavaModelException {
		IScanner scanner= ToolFactory.createScanner(false, false, false);
		scanner.setSource(methodSource.toCharArray());
		int offset= 0;
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (token == ITerminalSymbols.TokenNameprivate) {
					offset= scanner.getCurrentTokenStartPosition();
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		//must do it this way - unicode
		int length= scanner.getCurrentTokenSource().length;
		return  new StringBuffer(methodSource).delete(offset, offset + length).insert(offset, "protected").toString();
	}
	
	private static IMethod getLastMethod(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IMethod[] methods= type.getMethods();
		if (methods.length == 0)
			return null;
		return methods[methods.length - 1];	
	}
}

