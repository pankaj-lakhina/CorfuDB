package org.corfudb.util;

import io.netty.channel.ChannelHandlerContext;
import org.corfudb.infrastructure.IServerRouter;
import org.corfudb.protocols.wireprotocol.CorfuMsg;
import org.corfudb.runtime.clients.IClient;
import org.corfudb.runtime.clients.IClientRouter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mwei on 7/27/16.
 */
public class ClientMsgHandler {

    @FunctionalInterface
    public interface Handler<T extends CorfuMsg> {
        Object handle(T CorfuMsg, ChannelHandlerContext ctx, IClientRouter r) throws Exception;
    }

    /** The handler map. */
    private Map<CorfuMsg.CorfuMsgType, ClientMsgHandler.Handler> handlerMap;

    /** The client. */
    private IClient client;

    /** Construct a new instance of ClientMsgHandler. */
    public ClientMsgHandler(IClient client) {
        this.client = client;
        handlerMap = new ConcurrentHashMap<>();
    }

    /** Add a handler to this message handler.
     *
     * @param messageType       The type of CorfuMsg this handler will handle.
     * @param handler           The handler itself.
     * @param <T>               A CorfuMsg type.
     * @return                  This handler, to support chaining.
     */
    public <T extends CorfuMsg> ClientMsgHandler
    addHandler(CorfuMsg.CorfuMsgType messageType, ClientMsgHandler.Handler<T> handler) {
        // We do type-checking at runtime.
        // This should be okay as any incorrect handler will be registered
        // at startup, and be caught during almost any unit test.

        // Type-checking during compile time would be nice, but Java is just
        // not friendly...

        // TODO: Turn off this check when we aren't running tests.
        try {
            Class<?> c = handler.getClass().getMethod("handle",
                    CorfuMsg.class, ChannelHandlerContext.class, IClientRouter.class)
                    .getParameterTypes()[0];

            if (!c.isAssignableFrom(messageType.messageType.getRawType())) {
                throw new UnsupportedOperationException(
                        "Handler for incorrect type registered, expected "
                                + messageType.messageType.toString() + " but got " +
                                c.toGenericString());
            }

            handlerMap.put(messageType, handler);
            return this;
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    /** Handle an incoming CorfuMsg.
     *
     * @param message   The message to handle.
     * @param ctx       The channel handler context.
     * @return          True, if the message was handled.
     *                  False otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean handle(CorfuMsg message, ChannelHandlerContext ctx) {
        if (handlerMap.containsKey(message.getMsgType())) {
            try {
                Object ret = handlerMap.get(message.getMsgType()).handle(message, ctx, client.getRouter());
                if (ret != null) {
                    client.getRouter().completeRequest(message.getRequestID(), ret);
                }
            } catch (Exception ex) {
                client.getRouter().completeExceptionally(message.getRequestID(), ex);
            }
            return true;
        }
        return false;
    }

    /** Get the types this handler will handle.
     *
     * @return  The types this handler will handle.
     */
    public Set<CorfuMsg.CorfuMsgType> getHandledTypes() {
        return handlerMap.keySet();
    }
}
