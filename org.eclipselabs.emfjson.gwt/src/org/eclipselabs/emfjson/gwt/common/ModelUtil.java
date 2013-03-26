/*******************************************************************************
 * Copyright (c) 2011 Guillaume Hillairet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Hillairet - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.emfjson.gwt.common;

import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_ELEMENT_ANNOTATION;
import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_JSON_ANNOTATION;
import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_ROOT_ANNOTATION;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import com.google.gwt.json.client.JSONValue;

/**
 * 
 * @author ghillairet
 *
 */
public class ModelUtil {
	
	public static String getElementName(EStructuralFeature feature) {
		final EAnnotation annotation = feature.getEAnnotation(EJS_JSON_ANNOTATION);
		if (annotation != null && annotation.getDetails().containsKey(EJS_ELEMENT_ANNOTATION)) {
			return annotation.getDetails().get(EJS_ELEMENT_ANNOTATION);
		}
		return feature.getName();
	}

	public static EAttribute getEAttribute(EClass eClass, String key) {
		if (eClass == null || key == null) return null;

		EStructuralFeature eStructuralFeature = eClass.getEStructuralFeature(key);
		if (eStructuralFeature == null) {
			int i = 0;
			while(i < eClass.getEAllAttributes().size() && eStructuralFeature == null) {
				EAttribute eAttribute = eClass.getEAllAttributes().get(i);
				if (key.equals(getElementName(eAttribute))) {
					eStructuralFeature = eAttribute;
				}
				i++;
			}
		}
		return eStructuralFeature instanceof EAttribute ? (EAttribute) eStructuralFeature : null;
	}
	
	public static EReference getEReference(EClass eClass, String key) {
		if (eClass == null || key == null) return null;

		EStructuralFeature eStructuralFeature = eClass.getEStructuralFeature(key);
		if (eStructuralFeature == null) {
			int i = 0;
			while(i < eClass.getEAllReferences().size() && eStructuralFeature == null) {
				EReference eReference = eClass.getEAllReferences().get(i);
				if (key.equals(getElementName(eReference))) {
					eStructuralFeature = eReference;
				}
				i++;
			}
		}
		return eStructuralFeature instanceof EReference ? (EReference) eStructuralFeature : null;
	}
	
	public static EStructuralFeature getDynamicMapEntryFeature(EClass eClass) {
		if (eClass == null) return null;

		EStructuralFeature eMapEntry = null;
		int i = 0;

		while(i < eClass.getEAllStructuralFeatures().size() && eMapEntry == null) {
			EStructuralFeature eFeature = eClass.getEAllStructuralFeatures().get(i);
			if (isDynamicMapEntryFeature(eFeature)) {
				eMapEntry = eFeature;
			}
			i++;
		}

		return eMapEntry;
	}
	
	public static boolean isDynamicMapEntryFeature(EStructuralFeature eFeature) {
		if (eFeature != null && isMapEntry(eFeature.getEType())) {
			EAnnotation annotation = eFeature.getEAnnotation(EJS_JSON_ANNOTATION);
			if (annotation != null && annotation.getDetails().containsKey("dynamicMap")) {
				return Boolean.parseBoolean(annotation.getDetails().get("dynamicMap"));
			}
		}
		return false;
	}
	
	public static boolean isMapEntry(EClassifier eType) {
		return eType != null && eType.equals(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY);
	}
	
	public static String getRootNode(EObject object) {
		if (object instanceof EClass) {
			EClass eClass = (EClass) object;

			if (eClass.getEAnnotation(EJS_JSON_ANNOTATION) != null) {
				EAnnotation annotation = eClass.getEAnnotation(EJS_JSON_ANNOTATION);

				if (annotation.getDetails().containsKey(EJS_ROOT_ANNOTATION)) {
					if (annotation.getDetails().containsKey(EJS_ELEMENT_ANNOTATION)) {
						return annotation.getDetails().get(EJS_ELEMENT_ANNOTATION);
					}
				}
			}
			return null;
		}
		return null;
	}

	public static URI getEObjectURI(JSONValue jsonNode, URI uri, Map<String, String> nsMap) {
		final JSONString string = jsonNode.isString();
		final String value = string == null ? "" : string.stringValue();

		if (value.startsWith("#//")) { // is fragment
			return URI.createURI(uri+value);
		} else if (value.contains("#//") && nsMap.keySet().contains(value.split("#//")[0])) {
			String[] split = value.split("#//");
			String nsURI = nsMap.get(split[0]);
			return URI.createURI(nsURI+"#//"+split[1]);
		} else if (value.contains(":")) {
			return URI.createURI(value);
		} else { // is ID
			return uri.appendFragment(value);
		}
	}
}
