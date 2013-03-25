package org.eclipselabs.emfjson.gwt.internal;

import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_REF_KEYWORD;
import static org.eclipselabs.emfjson.gwt.common.Constants.EJS_TYPE_KEYWORD;
import static org.eclipselabs.emfjson.gwt.common.ModelUtil.getElementName;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipselabs.emfjson.gwt.EMFJs;
import org.eclipselabs.emfjson.gwt.common.ModelUtil;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONSave {

	private boolean serializeTypes = true;
	private boolean serializeRefTypes = true;
	@SuppressWarnings("unused")
	private boolean indent = true;

	public JSONSave() {}

	private void configure(Map<?, ?> options) {
		if (options == null) {
			options = Collections.emptyMap();
		}

		if (options.containsKey(EMFJs.OPTION_INDENT_OUTPUT)) {
			try {
				indent = (Boolean) options.get(EMFJs.OPTION_INDENT_OUTPUT);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(EMFJs.OPTION_SERIALIZE_TYPE)) {
			try {
				serializeTypes = (Boolean) options.get(EMFJs.OPTION_SERIALIZE_TYPE);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		if (options.containsKey(EMFJs.OPTION_SERIALIZE_REF_TYPE)) {
			try {
				serializeRefTypes = (Boolean) options.get(EMFJs.OPTION_SERIALIZE_REF_TYPE);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(Resource resource, OutputStream outputStream, Map<?, ?> options) {
		JSONValue json = genJson(resource, options);
		String stringValue = json.toString();

		try {
			outputStream.write(stringValue.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JSONValue genJson(Resource resource, Map<?,?> options) {
		configure(options);

		final JSONValue rootNode;
		final EList<EObject> contents = resource.getContents();

		if (contents.size() == 1) {
			EObject rootObject = resource.getContents().get(0);
			rootNode = writeEObject(rootObject, resource);
		} else {
			rootNode = new JSONArray();

			for (int i=0; i<contents.size(); i++) {
				EObject obj = contents.get(i);
				JSONObject node = writeEObject(obj, resource);
				if (node != null) {
					((JSONArray)rootNode).set(i, node);
				}
			}

			//			((ArrayNode)rootNode).addAll(nodes);
		}

		return rootNode;
	}

	public JSONValue genJson(Resource resource) {
		return genJson(resource, null);
	}

	public void writeValue(OutputStream outputStream, Object value) {
		if (value instanceof JSONValue) {
			String stringValue = ((JSONValue)value).toString();
			try {
				outputStream.write(stringValue.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private JSONObject writeEObject(EObject object, Resource resource) {
		JSONObject node = new JSONObject();

		writeEObjectAttributes(object, node);
		writeEObjectReferences(object, node, resource);

		return node;
	}

	private void writeEObjectAttributes(EObject object, JSONObject node) {
		final URI eClassURI = EcoreUtil.getURI(object.eClass());
		if (serializeTypes) {
			node.put(EJS_TYPE_KEYWORD, new JSONString(eClassURI.toString()));
		}

		for (EAttribute attribute: object.eClass().getEAllAttributes()) {
			if (object.eIsSet(attribute) && !attribute.isDerived() 
					&& !attribute.isTransient() && !attribute.isUnsettable()) {

				if (FeatureMapUtil.isFeatureMap(attribute)) {
					FeatureMap.Internal featureMap = (FeatureMap.Internal) object.eGet(attribute);
					Iterator<FeatureMap.Entry> iterator = featureMap.basicIterator();

					while (iterator.hasNext()) {
						FeatureMap.Entry entry = iterator.next();
						EStructuralFeature feature = entry.getEStructuralFeature();
						if (feature instanceof EAttribute) {
							setJsonValue(node, entry.getValue(), (EAttribute) feature);
						}
					}
				} else if (attribute.isMany()) {
					EList<?> rawValues = (EList<?>) object.eGet(attribute);

					if (!rawValues.isEmpty()) {
						JSONArray arrayNode = new JSONArray();
						node.put(getElementName(attribute), arrayNode);

						for (int i=0; i<rawValues.size(); i++) {
							Object val = rawValues.get(i);
							setJsonValue(arrayNode, val, attribute, i);
						}
					}
				} else {
					final Object value = object.eGet(attribute);
					setJsonValue(node, value, attribute);
				}
			}
		}
	}

	private void setJsonValue(JSONObject node, Object value, EAttribute attribute) {
		if (value == null) return;

		if (value instanceof Integer) {			
			node.put(getElementName(attribute), new JSONNumber((Integer) value));	
		} else if (value instanceof Boolean) {
			node.put(getElementName(attribute), JSONBoolean.getInstance((Boolean) value));
		} else if (value instanceof Double) {
			node.put(getElementName(attribute), new JSONNumber((Double) value));
		} else if (value instanceof Date) {
			DateTimeFormat sdf = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss");
			String dateValue = sdf.format((Date) value);
			node.put(getElementName(attribute), new JSONString(dateValue));
		} else {
			node.put(getElementName(attribute), new JSONString(value.toString()));
		}
	}

	private void setJsonValue(JSONArray node, Object value, EAttribute attribute, int position) {
		if (value == null) return;

		if (value instanceof Integer) {
			node.set(position, new JSONNumber((Integer) value));
		} else if (value instanceof Boolean) {
			node.set(position, JSONBoolean.getInstance((Boolean) value));
		} else if (value instanceof Double) {
			node.set(position, new JSONNumber((Double) value));
		} else if (value instanceof Date) {
			DateTimeFormat sdf = DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss");
			String dateValue = sdf.format((Date) value);
			node.set(position, new JSONString(dateValue));
		} else {
			node.set(position, new JSONString(value.toString()));
		}
	}

	private void writeEObjectReferences(EObject object, JSONObject node, Resource resource) {

		for (EReference reference: object.eClass().getEAllReferences()) {

			if (!reference.isTransient() && object.eIsSet(reference) && 
					!ModelUtil.isDynamicMapEntryFeature(reference)) {

				if (ModelUtil.isMapEntry(reference.getEType())) {
					writeMapEntry(object, reference, node);
				} else if (reference.isContainment()) {
					writeEObjectContainments(object, reference, node, resource);
				} else {
					writeEObjectReferences(node, object, reference);
				}
			}
		}
	}

	private void writeEObjectReferences(JSONObject target, EObject eObject, EReference reference) {
		if (reference.isMany()) {
			@SuppressWarnings("unchecked")
			EList<EObject> values = (EList<EObject>) eObject.eGet(reference);

			final JSONArray arrayNode = new JSONArray();
			target.put(getElementName(reference), arrayNode);

			for (int i=0; i < values.size(); i++) {
				arrayNode.set(i, writeReferenceValue(eObject, values.get(i), reference));
			}
		} else {
			EObject value = (EObject) eObject.eGet(reference);
			target.put(reference.getName(), writeReferenceValue(eObject, value, reference));
		}
	}

	private JSONValue writeReferenceValue(EObject eObject, EObject value, EReference reference) {
		final Resource resource = eObject.eResource();

		JSONObject node = new JSONObject();
		node.put(EJS_REF_KEYWORD, getReference(value, resource));

		if (serializeRefTypes) {
			node.put(EJS_TYPE_KEYWORD, getReference(value.eClass(), resource));
		}

		return node;
	}

	private void writeMapEntry(EObject object, EReference reference, JSONObject node) {
		final JSONObject nodeRef = new JSONObject();
		if (reference.isMany()) {
			@SuppressWarnings("unchecked")
			Collection<Map.Entry<String, String>> entries = (Collection<Entry<String, String>>) object.eGet(reference);
			for (Map.Entry<String, String> entry: entries) {
				nodeRef.put(entry.getKey(), new JSONString(entry.getValue()));
			}
		} else {
			@SuppressWarnings("unchecked")
			Map.Entry<String, String> entry = (Entry<String, String>) object.eGet(reference);
			nodeRef.put(entry.getKey(), new JSONString(entry.getValue()));
		}
		node.put(reference.getName(), nodeRef);
	}

	private void writeEObjectContainments(EObject object, EReference reference, JSONObject node, Resource resource) {
		if (reference.isMany()) {
			@SuppressWarnings("unchecked")
			EList<EObject> values = (EList<EObject>) object.eGet(reference);

			final JSONArray arrayNode = new JSONArray();
			node.put(getElementName(reference), arrayNode);

			for (int i=0; i<values.size(); i++) {
				EObject value = values.get(i);
				JSONObject subNode = new JSONObject();
				if (value.eIsProxy() || !value.eResource().equals(resource)) {
					subNode.put(EJS_REF_KEYWORD, getReference(value, resource));
				} else {
					writeEObjectAttributes(value, subNode);
					//					writeDynamicMap(value, subNode);
					writeEObjectReferences(value, subNode, resource);	
				}
				arrayNode.set(i, subNode);
			}
		} else {
			final EObject value = (EObject) object.eGet(reference);
			final JSONObject subNode = new JSONObject();

			if (value.eIsProxy() || !value.eResource().equals(resource)) {
				node.put(EJS_REF_KEYWORD, getReference(value, resource));
			} else {
				node.put(getElementName(reference), subNode);
				writeEObjectAttributes(value, subNode);
				//				writeDynamicMap(value, subNode);
				writeEObjectReferences(value, subNode, resource);	
			}
		}
	}

	private JSONValue getReference(EObject obj, Resource resource) {
		String value = null;
		if (obj.eIsProxy()) {
			value = ((InternalEObject)obj).eProxyURI().toString();
		}
		URI eObjectURI = EcoreUtil.getURI(obj);
		if (eObjectURI.trimFragment().equals(resource.getURI())) {
			value = eObjectURI.fragment();
		}
		if (value == null) {
			value = eObjectURI.toString();	
		}
		return new JSONString(value);
	}
}
