package com.jessin.nio;

/**
 * @author zexin.guo
 * @create 2017-12-17 下午2:39
 **/


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioNonBlockingHttpClient {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private static Selector selector;
    private Charset charset = Charset.forName("utf8");
    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
        NioNonBlockingHttpClient client = new NioNonBlockingHttpClient();
        List<String> hostList = new ArrayList();
        hostList.add("127.0.0.1");
        // 先注册到大管家
        for (String host : hostList) {
            client.registerSocketChannel(host, 11111);
        }
        client.select();
    }

    public void registerSocketChannel(String host, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        // 保证处于time_wait状态端口的可以绑定
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        //socketChannel.bind(new InetSocketAddress("127.0.0.1", 12345));
        socketChannel.socket().setSoTimeout(5000);
        SocketAddress remote = new InetSocketAddress(host, port);
        socketChannel.configureBlocking(false);
        // 调用connect
        socketChannel.connect(remote);
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void select() throws IOException {

            while (selector.select() > 0) {
                Set<SelectionKey> keys = selector.selectedKeys();

                Iterator<SelectionKey> it = keys.iterator();
                int i = 0;
                while (it.hasNext()) {
                    i++;
                    SelectionKey key = it.next();
                    it.remove();
                    try {
                        if(!key.isValid()) {
                            logger.info("服务端已经失效了");
                            key.cancel();
                        } else if (key.isConnectable()) {
                            connect(key);
                        } else if (key.isWritable()) {
                            write(key);
                        } else if (key.isReadable()) {
                            receive(key);
                        }
                    } catch (Exception e) {
                        key.cancel();
                    }
                }
                System.out.println(i);
            }

    }

    private void connect(SelectionKey key) throws IOException {
        logger.info("收到连接事件");
        SocketChannel channel = (SocketChannel) key.channel();
        // 连接完成
        channel.finishConnect();
        InetSocketAddress remote = (InetSocketAddress) channel.socket().getRemoteSocketAddress();
        String host = remote.getHostName();
        int port = remote.getPort();
        logger.info(String.format("访问地址: {}:{} 连接成功!", host, port));
        // 只关注写事件
        key.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * 一般都可以写？？
     * @param key
     * @throws IOException
     */
    private void write(SelectionKey key) throws IOException {
        System.out.println("接收到写事件");
        SocketChannel channel = (SocketChannel) key.channel();
        System.out.println("接收到写事件");
        // 可以先把数据暂存起来，然后poll，允许发送时再发送数据
        String request = "客服端上线了";
        System.out.println(request);

        channel.write(charset.encode(request));
        // 注册对读感兴趣，之前的被覆盖了
        key.interestOps(SelectionKey.OP_READ);
    }

    private void receive(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        // 先写到缓冲区
        int len = channel.read(buffer);
        // 切换到读取模式，读取刚写入的数据
        buffer.flip();
        String receiveData = charset.decode(buffer).toString();
        System.out.println("接收到读事件，数据长度为" + len);

        // 保证无限循环
        //key.interestOps(0);
        System.out.println(receiveData);
        if (len <= 0) {
            System.out.println("服务端数据读取完毕");
            channel.close();
            System.exit(0);
            return;
        }
    }
}