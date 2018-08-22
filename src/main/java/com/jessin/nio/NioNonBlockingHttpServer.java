package com.jessin.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author zexin.guo
 * @create 2017-12-17 下午3:58
 **/
public class NioNonBlockingHttpServer {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private Charset charset = Charset.forName("utf8");

    public static void main(String[] args) {
        new NioNonBlockingHttpServer();
    }

    public NioNonBlockingHttpServer() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            // 必须在绑定之前设置为非阻塞，才能生效
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            // 绑定的地址
            SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 11111);
            // 默认128个连接
            serverSocketChannel.bind(socketAddress, 128);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("开始监听：{}", socketAddress);
            select();
        } catch (IOException e) {
            logger.error("连接出错", e);
        } finally {
            logger.info("关闭资源");
            try {
                if (selector != null) {
                    selector.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (serverSocketChannel != null) {
                    serverSocketChannel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void select() throws IOException {
        while (true) {
            int availableCount = selector.select();
            if (availableCount == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            logger.info("selectionKey为：{}", selectionKeys);
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                // 必须移除，否则下一次select会出错
                iterator.remove();
                try {
                    if (!selectionKey.isValid()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        client.close();
                        logger.info("客户端已经失效：{}", client.getRemoteAddress());
                    } else if (selectionKey.isAcceptable()) {
                        // 将客户端注册到大管家selector中，并且刚兴趣的是已经读取了
                        SocketChannel client = serverSocketChannel.accept();
                        client.configureBlocking(false);
                        // 要求客户端写
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (selectionKey.isReadable()) {
                        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
                        // 获取到可以写的客户端
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        int len = client.read(receiveBuffer);
                        // 必须重新设置，position=0，否则下面日志为空
                        receiveBuffer.flip();
                        if(len <= 0) {
                            logger.warn("客户端已经关闭了：{}，接收到的数据长度为：{}", client.getRemoteAddress(), len);
                            // 关闭链接，否则会出异常，只取消这个key是不对的
                            // selectionKey.cancel();
                            client.close();
                        } else {
                            logger.info("接收到客户端：{}的消息：{}",
                                    client.getRemoteAddress(), charset.decode(receiveBuffer));
                            if (selectionKey.attachment() == null) {
                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                            }
                        }
                    } else if (selectionKey.isWritable()) {
                        // 获取到可以写的客户端
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        String reply = "我退出了";
                        client.write(charset.encode(reply));
                        logger.info("向客户端：{} 回复：{}", client.getRemoteAddress(), reply);
                        selectionKey.interestOps(selectionKey.interestOps() & ~ SelectionKey.OP_WRITE);
                        selectionKey.attach(false);
                    }
                } catch(Exception e) {
                    // 客户端突然关闭时，再去读，会出错，所以要catch住这个异常。
                    // 同时必须取消这个key，否则还会有读事件
                    selectionKey.cancel();
                    logger.error("处理事件出错", e);
                }
            }
        }
    }
}
