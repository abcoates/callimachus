/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.server.readers;

import java.net.MalformedURLException;

import org.callimachusproject.server.readers.base.URIListReader;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Reads RDFObjects from a URI list.
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectURIReader extends URIListReader<Object> {
	private RDFObjectReader neighbour = new RDFObjectReader();

	public RDFObjectURIReader() {
		super(null);
	}

	public boolean isReadable(MessageType mtype) {
		if (neighbour.isReadable(mtype))
			return false;
		if (!super.isReadable(mtype))
			return false;
		Class<?> c = mtype.clas();
		if (mtype.isSetOrArray()) {
			c = mtype.getComponentClass();
		}
		if (Object.class.equals(c))
			return true;
		if (RDFObject.class.isAssignableFrom(c))
			return true;
		return mtype.isConcept(c);
	}

	@Override
	protected Object create(ObjectConnection con, String uri)
			throws MalformedURLException, RepositoryException {
		if (uri == null)
			return null;
		if (uri.startsWith("_:"))
			return con.getObject(con.getValueFactory().createBNode(
					uri.substring(2)));
		return con.getObject(uri);
	}

}