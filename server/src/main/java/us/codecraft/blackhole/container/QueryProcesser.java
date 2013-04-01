package us.codecraft.blackhole.container;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;

import us.codecraft.blackhole.cache.CacheManager;
import us.codecraft.blackhole.forward.Forwarder;

/**
 * Main logic of blackhole.<br/>
 * Process the DNS query and return the answer.
 * 
 * @author yihua.huang@dianping.com
 * @date Dec 14, 2012
 */
@Component
public class QueryProcesser {

	@Autowired
	private HandlerManager handlerManager;

	@Autowired
	private Forwarder forwarder;

	@SuppressWarnings("unused")
	private Logger logger = Logger.getLogger(getClass());

	@Autowired
	private CacheManager cacheManager;

	public byte[] process(byte[] queryData) throws IOException {
		Message query = new Message(queryData);
		byte[] cache = cacheManager.getFromCache(query);
		if (cache != null) {
			return cache;
		}
		MessageWrapper responseMessage = new MessageWrapper(new Message(query
				.getHeader().getID()));
		for (Handler handler : handlerManager.getHandlers()) {
			boolean handle = handler.handle(new MessageWrapper(query),
					responseMessage);
			if (!handle) {
				break;
			}
		}
		byte[] response = null;
		if (ServerContext.hasRecord()) {
			response = responseMessage.getMessage().toWire();
		} else {
			response = forwarder.forward(queryData, query);
			if (response == null) {
				return null;
			}
			cacheManager.setToCache(query, response);
		}

		return response;
	}

}
