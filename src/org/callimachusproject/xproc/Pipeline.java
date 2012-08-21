package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.xml.XdmNodeURIResolver;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;

public class Pipeline {
	private final XProcConfiguration config;
	private final XdmNodeURIResolver resolver;
	private final FluidBuilder fb = FluidFactory.getInstance().builder();
	private final String systemId;
	private final XdmNode pipeline;

	Pipeline(String systemId, XdmNodeURIResolver resolver, XProcConfiguration config) {
		assert systemId != null;
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = null;
	}

	Pipeline(InputStream in, String systemId, XdmNodeURIResolver resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.parse(systemId, in);
	}

	Pipeline(Reader in, String systemId, XdmNodeURIResolver resolver, XProcConfiguration config) throws SAXException, IOException {
		this.systemId = systemId;
		this.config = config;
		this.resolver = resolver;
		this.pipeline = resolver.parse(systemId, in);
	}

	public String getSystemId() {
		return systemId;
	}

	public PipelineBuilder pipe() throws SAXException, IOException {
		return pipeSource(null);
	}

	public PipelineBuilder pipe(Object source, Type type, String... media)
			throws SAXException, IOException, XProcException {
		return pipeReader(asReader(source, type, media));
	}

	public PipelineBuilder pipeStream(InputStream source)
			throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(null, source));
	}

	public PipelineBuilder pipeReader(Reader reader) throws SAXException, IOException, XProcException {
		return pipeSource(resolver.parse(null, reader));
	}

	private PipelineBuilder pipeSource(XdmNode source) throws SAXException, XProcException, IOException {
		XProcRuntime runtime = new XProcRuntime(config);
		runtime.setEntityResolver(resolver.getEntityResolver());
		runtime.setURIResolver(resolver);
		try {
			XPipeline xpipeline = runtime.use(resolvePipeline());
			if (source != null) {
				xpipeline.writeTo("source", source);
			}
			return new PipelineBuilder(runtime, resolver, xpipeline, systemId);
		} catch (SaxonApiException e) {
			throw new SAXException(e);
		}
	}

	private XdmNode resolvePipeline() throws IOException, SAXException {
		if (pipeline != null)
			return pipeline;
		return resolver.resolve(systemId);
	}

	private Reader asReader(Object source, Type type, String... media)
			throws IOException {
		try {
			return fb.consume(source, null, type, media).asReader();
		} catch (FluidException e) {
			throw new XProcException(e);
		}
	}

}
