/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.engine.helpers;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

/**
 * Base class for piped RDF readers.
 * 
 * @author James Leigh
 *
 */
public abstract class AbstractRDFEventReader implements RDFEventReader {
	private RDFEvent next;

	public final boolean hasNext() throws RDFParseException {
		if (next == null)
			return (next = take()) != null;
		return true;
	}

	public final RDFEvent peek() throws RDFParseException {
		if (next == null)
			return next = take();
		return next;
	}

	public final RDFEvent next() throws RDFParseException {
		if (next == null)
			return take();
		try {
			return next;
		} finally {
			next = null;
		}
	}

	public abstract void close() throws RDFParseException;

	protected abstract RDFEvent take() throws RDFParseException;

}
