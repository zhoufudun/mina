package org.apache.mina.example.echoserver;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.example.echoserver.ssl.BogusSslContextFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filter.logging.MdcInjectionFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class client {
    private static IoSession session;

    public static void main(String[] args) {
        NioSocketConnector connector = new NioSocketConnector();
        connect(connector, new InetSocketAddress(8080), false);
    }


    public static boolean connect(NioSocketConnector connector, SocketAddress address,
                                  boolean useSsl) {
        if (session != null && session.isConnected()) {
            throw new IllegalStateException(
                    "Already connected. Disconnect first.");
        }

        try {
            IoFilter LOGGING_FILTER = new LoggingFilter();

            IoFilter CODEC_FILTER = new ProtocolCodecFilter(
                    new TextLineCodecFactory());

            connector.getFilterChain().addLast("mdc", new MdcInjectionFilter());
            connector.getFilterChain().addLast("codec", CODEC_FILTER);
            connector.getFilterChain().addLast("logger", LOGGING_FILTER);


            connector.setHandler(new EchoProtocolHandler() {
                @Override
                public void sessionCreated(final IoSession session) {
//
//                        try {
//                            session.getFilterChain().addFirst(
//                                    "SSL",
//                                    new SslFilter(BogusSslContextFactory
//                                            .getInstance(true)));
//                        } catch (GeneralSecurityException e) {
//                            throw new RuntimeException(e);
//                        }
                    System.out.println("sessionCreated, session=" + session);

                    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            session.write("schedule job");
                        }
                    },1,1, TimeUnit.SECONDS);

                }

                // This is for TLS re-entrance test
                @Override
                public void messageReceived(IoSession session, Object message)
                        throws Exception {
                    if (!(message instanceof IoBuffer)) {
                        return;
                    }

                    IoBuffer buf = (IoBuffer) message;

                    buf.mark();
//
//                    if (session.getFilterChain().contains("SSL")
//                            && buf.remaining() == 1 && buf.get() == (byte) '.') {
//                        ((SslFilter) session.getFilterChain().get("SSL"))
//                                .startSsl(session);
//
//                        // Send a response
//                        buf.capacity(1);
//                        buf.flip();
//                        session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE);
//                        session.write(buf);
//                    } else {
//                        buf.reset();
                    System.out.println("messageReceived, message=" + message);
                    super.messageReceived(session, buf);
//                    }
                }
            });
            ConnectFuture future1 = connector.connect(address);
            future1.awaitUninterruptibly();
            if (!future1.isConnected()) {
                return false;
            }
            session = future1.getSession();

            login();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void login() {
        session.write("LOGIN ");
    }

}
