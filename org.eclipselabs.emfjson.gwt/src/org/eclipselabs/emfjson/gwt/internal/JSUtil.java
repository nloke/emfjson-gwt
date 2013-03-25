package org.eclipselabs.emfjson.gwt.internal;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipselabs.emfjson.gwt.common.Constants;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class JSUtil {

	public static JSONValue parse(InputStream inputStream) {
		return JSONParser.parseStrict(toString(inputStream));
	}
	
	private static String toString(InputStream inStream) {
		final StringBuilder out = new StringBuilder();
		byte[] b = new byte[4096];
		try {
			for (int n; (n = inStream.read(b)) != -1;) {
				out.append(new String(b, 0, n));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toString();
	}
	
	public static JSONObject getNode(Resource resource, URI objectURI, EClass eClass) {
		URI fileURI = objectURI.trimFragment();
		ResourceSet resourceSet = resource.getResourceSet();
		URIConverter uriConverter = resourceSet.getURIConverter();

		String slash = "/";
		String current = slash;
		try {
			InputStream inStream = uriConverter.createInputStream(fileURI);
			JSONValue root = parse(inStream);

			return findNode(root, resourceSet, current, objectURI);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static JSONObject findNode(JSONValue node, ResourceSet resourceSet, String fragment, URI objectURI) {
//		JSONArray array = node.isArray();
//		if (array != null) {
//			int pos = 0;
//			String idx = fragment;
//			
//			for (int i=0; i<array.size(); i++) {
//				idx = fragment + pos;
//				JSONValue current = array.get(i);
//				
//				final EClass currentEClass = getEClass(current, resourceSet);
//				if (currentEClass != null) {
//					EAttribute id = currentEClass.getEIDAttribute();
//					if (id != null) {
//						if (objectURI.trimFragment().appendFragment(current.get(id.getName()).getValueAsText()).equals(objectURI)) {
//							return current;
//						}
//					} else {
//						if (objectURI.trimFragment().appendFragment(idx).equals(objectURI)) {
//							return current;
//						}
//					}
//
//					for (EReference reference: currentEClass.getEAllContainments()) {
//						if (current.has(reference.getName())) {
//							JsonNode found = findNode(
//									current.get(reference.getName()), 
//									resourceSet, 
//									idx += "/@"+reference.getName() + (reference.isMany() ? "." : ""), 
//									objectURI);
//							if (found != null) {
//								return found;
//							}
//						}
//					}
//				}
//
//				pos++;
//			}
//		} else {
//			final EClass currentEClass = getEClass(node, resourceSet);
//			if (currentEClass != null) {
//				EAttribute id = currentEClass.getEIDAttribute();
//				if (id != null) {
//					if (objectURI.trimFragment().appendFragment(node.get(id.getName()).getValueAsText()).equals(objectURI)) {
//						return node;
//					}
//				} else {
//					if (objectURI.trimFragment().appendFragment(fragment).equals(objectURI)) {
//						return node;
//					}
//				}
//
//				for (EReference reference: currentEClass.getEAllContainments()) {
//					if (node.has(reference.getName())) {
//						JsonNode found = findNode(
//								node.get(reference.getName()), 
//								resourceSet, 
//								fragment += "/@"+reference.getName() + (reference.isMany() ? "." : ""), 
//								objectURI);
//						if (found != null) {
//							return found;
//						}
//					}
//				}
//			}
//		}

		return null;
	}
	
	public static EClass getEClass(JSONValue node, ResourceSet resourceSet) {
		JSONObject jsobject = node.isObject();
		if (jsobject != null && jsobject.containsKey(Constants.EJS_TYPE_KEYWORD)) {
			return (EClass) resourceSet.getEObject(URI.createURI(jsobject.get(Constants.EJS_TYPE_KEYWORD).toString()), false);
		} else {
			return null;
		}
	}

}
