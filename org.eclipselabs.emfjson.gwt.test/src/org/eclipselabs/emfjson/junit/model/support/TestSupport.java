/*******************************************************************************
 * Copyright (c) 2012 Guillaume Hillairet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Hillairet - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.emfjson.junit.model.support;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIService;
import org.eclipse.emf.ecore.resource.URIServiceAsync;
import org.eclipse.emf.ecore.resource.URIServiceCallback;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipselabs.emfjson.gwt.EMFJs;
import org.eclipselabs.emfjson.gwt.resource.JsResourceFactoryImpl;
import org.eclipselabs.emfjson.junit.model.ModelPackage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

public abstract class TestSupport extends GWTTestCase {
	
	protected String baseTestFilesPath = "platform:/plugin/org.eclipselabs.emfjson.junit/tests/";
	
	protected final Map<String ,Object> options = new HashMap<String, Object>();
	protected ResourceSet resourceSet;
	
	@Override
	protected void gwtSetUp() throws Exception {
		super.gwtSetUp();
		EPackage.Registry.INSTANCE.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new JsResourceFactoryImpl());
		options.put(EMFJs.OPTION_INDENT_OUTPUT, false);
		resourceSet = new ResourceSetImpl();
		
		final URIServiceAsync modelService = GWT.create(URIService.class);
		final URIServiceCallback modelCallback = new URIServiceCallback(modelService);
		resourceSet.getURIConverter().getURIHandlers().add(modelCallback);
	}	
	
	protected URI uri(String fileName) {
		return URI.createURI(baseTestFilesPath+fileName, true);
	}
	
	@Override
	public String getModuleName() {
		return "org.eclipselabs.emfjson.junit.model.Model";
	}
}
