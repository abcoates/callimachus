package org.callimachusproject.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLog extends Filter {
	private static final String USERNAME = "username=";
	private static final String NIL = "-";
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private final Logger logger = LoggerFactory.getLogger(AccessLog.class);
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);

	public AccessLog(Filter delegate) {
		super(delegate);
	}

	@Override
	public HttpResponse intercept(Request req) throws IOException {
		trace(req);
		return super.intercept(req);
	}

	@Override
	public HttpResponse filter(Request req, HttpResponse resp)
			throws IOException {
		resp = super.filter(req, resp);
		trace(req, resp);
		if (req.isInternal())
			return resp;
		final int code = resp.getStatusLine().getStatusCode();
		if (logger.isInfoEnabled() || logger.isWarnEnabled() && code >= 400
				|| logger.isErrorEnabled() && code >= 500) {
			final String addr = req.getRemoteAddr().getHostAddress();
			final String username = getUsername(req).replaceAll("\\s+",
					"_");
			final String line = req.getRequestLine().toString();
			final String referer = req.getHeader("Referer");
			final String agent = req.getHeader("User-Agent");
			HttpEntity entity = resp.getEntity();
			if (entity == null) {
				log(addr, username, line, code, 0, referer, agent);
			} else {
				final long length = entity.getContentLength();
				resp.setEntity(new HttpEntityWrapper(entity) {
					@Override
					protected InputStream getDelegateContent()
							throws IOException {
						InputStream in = super.getDelegateContent();
						return logOnClose(addr, username, line, code, length, referer, agent, in);
					}
				});
			}
		}
		return resp;
	}

	InputStream logOnClose(final String addr, final String username,
			final String line, final int code, final long length, final String referer, final String agent, InputStream in) {
		final ReadableByteChannel delegate = ChannelUtil.newChannel(in);
		return ChannelUtil.newInputStream(new ReadableByteChannel() {
			private long size = 0;
			private boolean complete;
			private boolean error;

			public boolean isOpen() {
				return delegate.isOpen();
			}

			public synchronized void close() throws IOException {
				delegate.close();
				if (!complete) {
					complete = true;
					if (error) {
						log(addr, username, line, 599, size, referer, agent);
					} else if (size < length || length < 0) {
						log(addr, username, line, 499, size, referer, agent);
					} else {
						log(addr, username, line, code, size, referer, agent);
					}
				}
			}

			public synchronized int read(ByteBuffer dst) throws IOException {
				error = true;
				int read = delegate.read(dst);
				if (read < 0) {
					complete = true;
					log(addr, username, line, code, size, referer, agent);
				} else {
					size += read;
				}
				error = false;
				return read;
			}
		});
	}

	void log(String addr, String username, String line, int code, long length,
			String referer, String agent) {
		StringBuilder sb = new StringBuilder();
		sb.append(addr).append('\t').append(username);
		sb.append('\t').append('"').append(line).append('"');
		sb.append('\t').append(code).append('\t').append(length);
		if (referer == null) {
			sb.append('\t').append('-');
		} else {
			sb.append('\t').append('"').append(referer).append('"');
		}
		if (agent == null) {
			sb.append('\t').append('-');
		} else {
			sb.append('\t').append('"').append(agent).append('"');
		}
		if (code < 400 || code == 401) {
			logger.info(sb.toString());
		} else if (code < 500) {
			logger.warn(sb.toString());
		} else {
			logger.error(sb.toString());
		}
	}

	private String getUsername(HttpRequest req) {
		for (Header hd : req.getHeaders("Authorization")) {
			Matcher m = TOKENS_REGEX.matcher(hd.getValue());
			while (m.find()) {
				String key = m.group(1);
				if ("username".equals(key))
					return m.group(2);
			}
		}
		for (Header hd : req.getHeaders("Cookie")) {
			for (String cookie : hd.getValue().split("\\s*,\\s*")) {
				if (!cookie.contains(USERNAME))
					continue;
				String[] pair = cookie.split("\\s*;\\s*");
				for (String p : pair) {
					if (p.startsWith(USERNAME)) {
						return p.substring(USERNAME.length());
					}
				}
			}
		}
		return NIL;
	}

	private void trace(Request req) {
		if (logger.isDebugEnabled() && !req.isInternal() || logger.isTraceEnabled()) {
			String id = uid + seq.getAndIncrement();
			req.setForensicId(id);
			StringBuilder sb = new StringBuilder();
			sb.append("+").append(req.getForensicId());
			sb.append("|").append(req.getRequestLine().toString().replace('|', '_'));
			for (Header hd : req.getAllHeaders()) {
				sb.append("|").append(hd.getName().replace('|', '_'));
				sb.append(":").append(hd.getValue().replace('|', '_'));
			}
			if (req.isInternal()) {
				logger.trace(sb.toString());
			} else {
				logger.debug(sb.toString());
			}
		}
	}

	private void trace(Request req, HttpResponse resp) {
		if (logger.isDebugEnabled() && !req.isInternal() || logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("-").append(req.getForensicId());
			sb.append("|").append(resp.getStatusLine().toString().replace('|', '_'));
			if (req.isInternal()) {
				logger.trace(sb.toString());
			} else {
				logger.debug(sb.toString());
			}
		}
	}

}