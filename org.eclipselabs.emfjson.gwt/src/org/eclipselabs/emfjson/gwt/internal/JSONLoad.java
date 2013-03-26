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
package org.eclipselabs.emfjson.gwt.internal;

import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_REF_KEYWORD;
import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_TYPE_KEYWORD;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.Callback;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipselabs.emfjson.gwt.EMFJs;
import org.eclipselabs.emfjson.gwt.common.ModelUtil;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONLoad {
	private static Logger logger = Logger.getLogger(JSONLoad.class.getName());

	private final Map<String, String> nsMap = new HashMap<String, String>();
	private EClass rootClass;
	private ResourceSet resourceSet;
	private boolean useProxyAttributes = false;
	private Map<EObject, JSONValue> processed = new HashMap<EObject, JSONValue>();

	public JSONLoad() {
	}

	private void init(Resource resource, Map<?,?> options) {
		processed.clear();

		if (options == null) {
			options = Collections.emptyMap();
		}

		resourceSet = resource.getResourceSet() != null ? resource.getResourceSet() : new ResourceSetImpl();

		useProxyAttributes = Boolean.TRUE.equals(options.get(EMFJs.OPTION_PROXY_ATTRIBUTES));

		if (options.containsKey(EMFJs.OPTION_ROOT_ELEMENT)) {
			rootClass = (EClass) options.get(EMFJs.OPTION_ROOT_ELEMENT);
		}
	}

	public void fillResource(final Resource resource, InputStream inStream, Map<?,?> options) {
		init(resource, options);

		JSONValue root = JSUtil.parse(inStream);

		if (root == null) {
			throw new IllegalArgumentException("root node should not be null.");
		}

		final JSONObject rootObject = root.isObject();
		if (rootObject != null) {
			fillEObject(resource, rootObject);
		} else {
			final JSONArray jsonArray = root.isArray();
			if (jsonArray != null) {
				for (int i=0; i<jsonArray.size(); i++) {
					final JSONValue jsonValue = jsonArray.get(i);
					final JSONObject jsonObject = jsonValue.isObject();

					fillEObject(resource, jsonObject);
				}
			}
		}
	}

	private void fillEObject(final Resource resource, final JSONObject jsonObject) {
		if (jsonObject != null) {
			if (rootClass != null) {
				EObject eObject = createEObject(resource, rootClass, jsonObject);
				if (eObject != null) {
					resource.getContents().add(eObject);

				}
			} else {
				if (jsonObject.containsKey(EJS_TYPE_KEYWORD)) {
					JSONString jsonString = jsonObject.get(EJS_TYPE_KEYWORD).isString();

					URI eClassURI = URI.createURI(jsonString.stringValue());
					getEClass(eClassURI, resourceSet, new Callback<EObject>() {
						@Override
						public void onFailure(Throwable caught) {
							logger.log(Level.SEVERE, "", caught);
						}

						@Override
						public void onSuccess(EObject result) {
							if (result instanceof EClass) {
								EObject eObject = createEObject(resource, (EClass) result, jsonObject);
								if (eObject != null) {
									resource.getContents().add(eObject);
									processReferences(resource);
								}
							}
						}
					});
				}
			}
		}
	}

	private void processReferences(final Resource resource) {
		for (EObject eObject: resource.getContents()) {
			if (processed.containsKey(eObject)) {
				fillEReference(eObject, processed.get(eObject).isObject(), resource);
			}
		}
	}

	private EObject createEObject(Resource resource, EClass eClass, JSONObject node) {
		if (node == null) return null;

		final EObject object = EcoreUtil.create(eClass);
		processed.put(object, node);

		fillEAttribute(object, node);
		fillEContainment(object, node, resource);

		return object;
	}

	private void fillEAttribute(EObject container, JSONObject node) {
		final EClass eClass = container.eClass();
		if (node == null) return;

		// Iterates over all key values of the JSON Object,
		// if the value is not an object then
		// if the key corresponds to an EAttribute, fill it
		// if not and the EClass contains a MapEntry, fill it with the key, value.

		for (String key: node.keySet()) {
			JSONValue value = node.get(key);

			if (value.isObject() != null) // not an attribute
				continue;

			EAttribute attribute = ModelUtil.getEAttribute(eClass, key);

			if (attribute != null && !attribute.isTransient() && !attribute.isDerived()) {
				if (value.isArray() != null) {
					JSONArray array = value.isArray();
					for (int i=0; i<array.size(); i++) {
						setEAttributeValue(container, attribute, array.get(i));
					}
				} else {
					setEAttributeValue(container, attribute, value);
				}
			} else {
				EStructuralFeature eFeature = ModelUtil.getDynamicMapEntryFeature(eClass);
				if (eFeature != null) {
					@SuppressWarnings("unchecked")
					EList<EObject> values = (EList<EObject>) container.eGet(eFeature);
					values.add(createEntry(key, value));
				}
			}
		}
	}

	private void fillEContainment(EObject eObject, JSONObject node, Resource resource) {
		final EClass eClass = eObject.eClass();
		if (node == null) return;

		for (String key: node.keySet()) {
			JSONValue value = node.get(key);

			EReference reference = ModelUtil.getEReference(eClass, key);
			if (reference != null && reference.isContainment() && !reference.isTransient()) {
				if (ModelUtil.isMapEntry(reference.getEType()) && value.isObject() != null) {
					createMapEntry(eObject, reference, value.isObject());
				} else {
					createContainment(eObject, reference, node, value, resource);
				}
			}
		}
	}

	private void fillEReference(EObject eObject, JSONObject node, Resource resource) {
		if (node == null) return;
		final EClass eClass = eObject.eClass();

		for (String key: node.keySet()) {
			JSONValue value = node.get(key);
			EReference reference = ModelUtil.getEReference(eClass, key);

			if (reference != null && !reference.isContainment() &&
					!reference.isDerived() && !reference.isTransient()) {

				JSONArray array = value.isArray();
				if (array != null) {
					for (int i=0; i<array.size(); i++) {
						createProxyReference(eObject, node, array.get(i).isObject(), reference, resource);
					}
				} else {
					createProxyReference(eObject, node, value.isObject(), reference, resource);
				}

			}
		}

		for (EObject content: eObject.eContents()) {
			if (processed.containsKey(content))
				fillEReference(content, processed.get(content).isObject(), resource);
		}
	}

	private void createContainment(EObject eObject, EReference reference, JSONObject root, JSONValue node, Resource resource) {
		if (node.isArray() != null) {
			JSONArray array = node.isArray();

			if (reference.isMany()) {
				@SuppressWarnings("unchecked")
				EList<EObject> values = (EList<EObject>) eObject.eGet(reference);

				for (int i=0; i<array.size(); i++) {
					JSONValue current = array.get(i);
					EObject contained = createContainedObject(reference, root, current.isObject(), resource);
					if (contained != null) values.add(contained);
				}
			} else if (array.size() > 0) {
				JSONValue current = array.get(0);
				EObject contained = createContainedObject(reference, root, current.isObject(), resource);
				if (contained != null) eObject.eSet(reference, contained);
			}

		} else {
			EObject contained = createContainedObject(reference, root, node.isObject(), resource);

			if (reference.isMany()) {
				@SuppressWarnings("unchecked")
				EList<EObject> values = (EList<EObject>) eObject.eGet(reference);
				if (contained != null) values.add(contained);
			} else {
				if (contained != null) eObject.eSet(reference, contained);
			}
		}
	}

	private EObject createContainedObject(EReference reference, JSONObject root, JSONObject node, Resource resource) {
		EClass refClass = findEClass(reference.getEReferenceType(), node, root, resource);
		EObject obj;

		if (isRefNode(node)) {
			obj = createProxy(resource, refClass, node);
		} else {
			obj = createEObject(resource, refClass, node);
		}

		return obj;
	}

	private EObject getOrCreateProxyReference(EReference reference, JSONObject root, JSONObject node, Resource resource) {
		EObject obj = findEObject(resource, node);
		if (obj == null) {
			EClass refClass = findEClass(reference.getEReferenceType(), node, root, resource);
			obj = createProxy(resource, refClass, node);
		}
		return obj;
	}

	private void createProxyReference(EObject eObject, JSONObject root, JSONObject node, EReference reference, Resource resource) {
		EObject proxy = getOrCreateProxyReference(reference, root, node, resource);
		if (proxy != null && reference.isMany()) {
			@SuppressWarnings("unchecked")
			InternalEList<EObject> values = (InternalEList<EObject>) eObject.eGet(reference);
			values.addUnique(proxy);
		} else if (proxy != null) {
			eObject.eSet(reference, proxy);
		}
	}

	private void setEAttributeValue(EObject obj, EAttribute attribute, JSONValue node) {
		JSONString string = node.isString();
		if (string != null) {
			setEAttributeStringValue(obj, attribute, string);
		} else {
			JSONNumber number = node.isNumber();
			if (number != null) {
				setEAttributeIntegerValue(obj, attribute, number);
			} else {
				JSONBoolean bool = node.isBoolean();
				if (bool != null) {
					setEAttributeBooleanValue(obj, attribute, bool);
				}
			}
		}
	}

	private void setEAttributeStringValue(EObject obj, EAttribute attribute, JSONString value) {
		final String stringValue = value.stringValue();

		if (stringValue != null && !stringValue.trim().isEmpty()) {
			Object newValue;

			if (attribute.getEAttributeType().getInstanceClass().isEnum()) {
				newValue = EcoreUtil.createFromString(attribute.getEAttributeType(), stringValue.toUpperCase());
			} else {
				newValue = EcoreUtil.createFromString(attribute.getEAttributeType(), stringValue);
			}

			if (!attribute.isMany()) {
				obj.eSet(attribute, newValue);
			} else {
				@SuppressWarnings("unchecked")
				Collection<Object> values = (Collection<Object>) obj.eGet(attribute);
				values.add(newValue);
			}
		}
	}

	private void setEAttributeIntegerValue(EObject obj, EAttribute attribute, JSONNumber value) {
		final int intValue = (int) value.doubleValue();

		if (!attribute.isMany()) {
			obj.eSet(attribute, intValue);
		} else {
			@SuppressWarnings("unchecked")
			Collection<Object> values = (Collection<Object>) obj.eGet(attribute);
			values.add(intValue);
		}
	}

	private void setEAttributeBooleanValue(EObject obj, EAttribute attribute, JSONBoolean value) {
		final boolean boolValue = value.booleanValue();

		if (!attribute.isMany()) {
			obj.eSet(attribute, boolValue);
		} else {
			@SuppressWarnings("unchecked")
			Collection<Object> values = (Collection<Object>) obj.eGet(attribute);
			values.add(boolValue);
		}
	}

	private void createMapEntry(EObject container, EReference reference, JSONObject jsonObject) {
		if (jsonObject == null) return;

		if (reference.isMany()) {
			@SuppressWarnings("unchecked")
			EList<EObject> values = (EList<EObject>) container.eGet(reference);
			for (String key: jsonObject.keySet()) {
				values.add(createEntry(key, jsonObject.get(key)));
			}
		} else {
			if (jsonObject.keySet().size() > 0) {
				String key = jsonObject.keySet().iterator().next();
				container.eSet(reference, createEntry(key, jsonObject.get(key)));
			}
		}
	}

	private EObject createEntry(String key, JSONValue value) {
		EObject eObject = EcoreUtil.create(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY);
		eObject.eSet(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY__KEY, key);
		String entryValue;
		if (value.isString() != null)
			entryValue = value.isString().stringValue();
		else entryValue = value.toString();
		eObject.eSet(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY__VALUE, entryValue);

		return eObject;
	}

	private EObject createProxy(Resource resource, EClass eClass, JSONObject node) {
		EObject proxy = null;

		if (isRefNode(node)) {
			final URI objectURI = ModelUtil.getEObjectURI(node.get(EJS_REF_KEYWORD), resource.getURI(), nsMap);
			proxy = EcoreUtil.create(eClass);
			((InternalEObject) proxy).eSetProxyURI(objectURI);

			if (useProxyAttributes) {
				JSONObject refNode = JSUtil.getNode(resource, objectURI, eClass);
				if (refNode != null) fillEAttribute(proxy, refNode);
			}
		}

		return proxy;
	}

	private EObject findEObject(Resource resource, JSONValue node) {
		EObject eObject = null;
		if (node.isObject() != null) {
			final URI objectURI = ModelUtil.getEObjectURI(node.isObject().get(EJS_REF_KEYWORD), resource.getURI(), nsMap);
			eObject = resourceSet.getEObject(objectURI, false);
		}
		return eObject;
	}

	private EClass findEClass(EClass eReferenceType, JSONObject node, JSONObject root, Resource resource) {
		if (eReferenceType.isAbstract() || node.get(EJS_TYPE_KEYWORD) != null) {

			if (node.containsKey(EJS_REF_KEYWORD)) {
				URI refURI = getEObjectURI(node.get(EJS_REF_KEYWORD), resource);
				EObject found = resourceSet.getEObject(refURI, false);
				if (found != null) {
					return found.eClass();
				}
				if (node.containsKey(EJS_TYPE_KEYWORD)) {
					JSONString typeValue = node.get(EJS_TYPE_KEYWORD).isString();
					return (EClass) resourceSet.getEObject(URI.createURI(typeValue.stringValue()), false);
				}
				// JsonNode refNode = findNode(refURI, eReferenceType, rootNode);
				// if (refNode != null) {
				// return findEClass(eReferenceType, refNode, root, resource);
				// }
			}
			else if (node.containsKey(EJS_TYPE_KEYWORD)) {
				final URI typeURI = getEObjectURI(node.get(EJS_TYPE_KEYWORD), eReferenceType.eResource());
				if (typeURI.fragment().equals(eReferenceType.getName())) {
					return eReferenceType;
				}
				else {
					final EObject o = this.resourceSet.getEObject(typeURI, true);
					final EClass found = o != null && o instanceof EClass ? (EClass)o : null;
					if (found != null) {
						return found;
					} else {
						throw new IllegalArgumentException("Cannot find EClass from type "+typeURI);
					}
				}
			}
		}
		return eReferenceType;
	}

	private boolean isRefNode(JSONValue node) {
		return node.isObject() != null && node.isObject().containsKey(EJS_REF_KEYWORD);
	}

	private void getEClass(URI uri, ResourceSet resourceSet, Callback<EObject> callback) {
		resourceSet.getEObject(uri, callback);
	}

	private URI getEObjectURI(JSONValue jsonValue, Resource resource) {
		final JSONString string = jsonValue.isString();
		final String value = string == null ? "" : string.stringValue();

		if (value.startsWith("#//")) { // is fragment
			return URI.createURI(resource.getURI()+value);
		} else if (value.contains("#//") && nsMap.keySet().contains(value.split("#//")[0])) {
			String[] split = value.split("#//");
			String nsURI = nsMap.get(split[0]);
			return URI.createURI(nsURI+"#//"+split[1]);
		} else if (value.contains(":")) {
			return URI.createURI(value);
		} else { // is ID
			return resource.getURI().appendFragment(value);
		}
	}

}
