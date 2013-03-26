GWT version of emfjson meant to be use on the client side of a GWT application.

## Maven repository

```xml
<repository>
  <id>emfgwt-repository</id>
	<url>http://repository-ghillairet.forge.cloudbees.com/snapshot</url>
	<snapshots>
		<enabled>true</enabled>
		<updatePolicy>always</updatePolicy>
	</snapshots>
</repository>

<dependency>
	<groupId>org.eclipselabs</groupId>
	<artifactId>org.eclipselabs.emfjson.gwt</artifactId>
	<version>0.5.2-SNAPSHOT</version>
</dependency>
```

## Getting started

### Initialize a ResourceSet

```java
import org.eclipselabs.emfjson.gwt.resource.JsResourceImpl;
import org.eclipse.emf.ecore.resource.URIService;
import org.eclipse.emf.ecore.resource.URIServiceAsync;
import org.eclipse.emf.ecore.resource.URIServiceCallback;

...

final URIServiceAsync modelService = GWT.create(URIService.class);
final URIServiceCallback modelCallback = new URIServiceCallback(modelService);
    
ResourceSet resourceSet = new ResourceSetImpl();
resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new ResourceFactoryImpl(){
  @Override
  public Resource createResource(URI uri) {
    return new JsResourceImpl(uri);
  }
});

resourceSet.getURIConverter().getURIHandlers().add(modelCallback);
```

### Create a Resource

```java

Resource resource = resourceSet.createResource(URI.createURI("uriService/model.json"));

```
