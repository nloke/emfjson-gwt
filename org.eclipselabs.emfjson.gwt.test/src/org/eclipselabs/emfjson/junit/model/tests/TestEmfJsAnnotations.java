package org.eclipselabs.emfjson.junit.model.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipselabs.emfjson.gwt.EMFJs;
import org.eclipselabs.emfjson.junit.model.support.TestSupport;
import org.junit.Test;

public class TestEmfJsAnnotations extends TestSupport {
	
	@Test
	public void testSaveAnnotation() throws IOException {
		String expectedString = "{\"eClass\":\"http://www.eclipse.org/emf/2002/Ecore#//EClass\", "+
				"\"name\":\"Foo\", \"eAnnotations\":[{"+
				"\"eClass\":\"http://www.eclipse.org/emf/2002/Ecore#//EAnnotation\", "+
				"\"source\":\"source\", \"details\":{\"displayName\":\"value\"}}]}";
		
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName("Foo");

		EAnnotation eAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
		eAnnotation.setEModelElement(eClass);
		eAnnotation.setSource("source");
		eAnnotation.getDetails().put("displayName", "value");
		eClass.getEAnnotations().add(eAnnotation);
		
		Resource resource = resourceSet.createResource(URI.createURI("test.json"));
		resource.getContents().add(eClass);
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		resource.save(outStream, options);
		
		assertEquals(expectedString, outStream.toString());
	}
	
	@Test
	public void testLoadAnnotation() throws IOException {
		String inputString = "{\"eClass\":\"http://www.eclipse.org/emf/2002/Ecore#//EClass\", "+
				"\"name\":\"Foo\", \"eAnnotations\":[{ "+
					"\"eClass\":\"http://www.eclipse.org/emf/2002/Ecore#//EAnnotation\", "+
					"\"source\":\"source\", " + 
					"\"details\":{\"displayName\":\"bar\"}}]}";
		
		Resource resource = resourceSet.createResource(URI.createURI("test.json"));
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(EMFJs.OPTION_ROOT_ELEMENT, EcorePackage.Literals.ECLASS);
		resource.load(new ByteArrayInputStream(inputString.getBytes()), options);
		
		assertEquals(1, resource.getContents().size());
		
		EObject root = resource.getContents().get(0);
		
		assertEquals(EcorePackage.Literals.ECLASS, root.eClass());
		
		EList<EAnnotation> annotations = ((EClass)root).getEAnnotations();
		
		assertEquals(1, annotations.size());
		
		EAnnotation annotation = annotations.get(0);
		assertEquals(1, annotation.getDetails().size());
		assertEquals("displayName", annotation.getDetails().get(0).getKey());

		assertEquals("bar", annotation.getDetails().get(0).getValue());
	}

}
