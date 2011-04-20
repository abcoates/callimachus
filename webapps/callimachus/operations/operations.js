// operations.js

importClass(Packages.calli.Creator);
importClass(Packages.calli.Copyable);
importClass(Packages.calli.copy);
importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.apache.http.ProtocolVersion);
importClass(Packages.org.apache.http.message.BasicHttpResponse);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);
importClass(Packages.org.openrdf.http.object.exceptions.BadRequest);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getCopyPage() {
	return findTemplate(this, copy).calliConstruct(this, 'create');
}

function postCopy(msg) {
	var template = findTemplate(this, copy);
	var newCopy = template.calliCreateResource(this, msg.input, this.FindCopyUriSpaces());
	if (newCopy instanceof Copyable || newCopy instanceof Creator) {
		newCopy.calliEditors.addAll(this.calliEditors);
	}
	newCopy.calliAdministrators.addAll(this.calliEditors);
	newCopy.calliAdministrators.addAll(this.FindCopyContributor(newCopy));
	return newCopy;
}

function getCreatePage(msg) {
java.lang.System.err.println('create: ' + msg);
	var factory = msg.create ? msg.create : this;
	if (this instanceof Creator && factory != this && !this.IsCreatable(factory))
		throw new BadRequest("Cannot create this class here: " + factory);
	if (!factory.calliCreate)
		throw new InternalServerError("No create template");
	return factory.calliCreate.calliConstruct(factory, 'create');
}

function postCreate(msg) {
	var factory = msg.create ? msg.create : this;
	if (this instanceof Creator && factory != this && !this.IsCreatable(factory))
		throw new BadRequest("Cannot create this class here: " + factory);
	var template = factory.calliCreate;
	if (!template)
		throw new InternalServerError("No create template");
	var newCopy = template.calliCreateResource(factory, msg.input, factory.calliUriSpace);
	newCopy = newCopy.objectConnection.addDesignation(newCopy, factory.toString());
	if (newCopy instanceof Copyable || newCopy instanceof Creator) {
		newCopy.calliEditors.addAll(factory.calliEditors);
		newCopy.calliEditors.addAll(this.calliEditors);
	}
	newCopy.calliAdministrators.addAll(factory.calliEditors);
	newCopy.calliAdministrators.addAll(this.calliEditors);
	newCopy.calliAdministrators.addAll(this.FindCreateContributor(newCopy));
	factory.touchRevision();
	return newCopy;
}

function getEditPage() {
	return findTemplate(this, edit).calliConstruct(this, 'edit');
}

function postEdit(msg) {
	var template = findTemplate(this, edit);
	template.calliEditResource(this, msg.input);
	var ver = new ProtocolVersion("HTTP", 1, 1);
	var resp = new BasicHttpResponse(ver, 201, "Modified");
	resp.addHeader("Location", this + "?view");
	return resp;
}

function findTemplate(obj, ann) {
	var annotated = findAnnotatedClass(obj.getClass(), ann);
	if (annotated) {
		var uri = annotated.getAnnotation(ann).value();
		if (uri.length != 1)
			throw new InternalServerError("Multiple templates for " + annotated.simpleName);
		var template = obj.objectConnection.getObject(uri[0]);
		if (template.calliConstruct)
			return template;
		throw new InternalServerError("Missing template");
	}
	throw new InternalServerError("No template");
}

function findAnnotatedClass(klass, ann) {
	if (klass.isAnnotationPresent(ann)) {
		return klass;
	}
	var result;
	if (klass.getSuperclass()) {
		result = findAnnotatedClass(klass.getSuperclass(), ann);
	}
	var interfaces = klass.getInterfaces();
	for (var i = interfaces.length; i--;) {
		var face = findAnnotatedClass(interfaces[i], ann);
		if (face) {
			if (!result || result.isAssignableFrom(face)) {
				result = face;
			} else if (!face.isAssignableFrom(result)) {
				throw new InternalServerError("Conflicting templates for "
					+ result.simpleName +  " and " + face.simpleName);
			}
		}
	}
	return result;
}


