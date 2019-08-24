package com.jessin.kafka2;

import org.apache.kafka.clients.producer.*;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 先切到root下启动zookeeper：
 * cd /home/jessin/Software/kafka_2.11-0.10.0.0
 * bin/zookeeper-server-start.sh config/zookeeper.properties
 * 然后启动Kafka：
 * cd /home/jessin/Software/kafka_2.11-0.10.0.0
 * bin/kafka-server-start.sh config/server1.properties
 *
 * @author zexin.guo
 * @create 2017-12-10 下午11:00
 **/
public class ProducerDemo {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9093");
        properties.put("client.id", "DemoProducer");
        properties.put("key.serializer",
                "org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put("value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 32 * 1024 * 1024);
        // 默认等待缓冲区，1min，缓存大小为32MB，RecordBatch默认是16384B = 16KB，也就是最多有2048个RecordBatch
        KafkaProducer producer = new KafkaProducer(properties);
        boolean isASync = args.length == 0;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10,
                0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));
        for (int i = 0; i < 10; i++) {
            threadPoolExecutor.execute(() -> {
                run(producer, isASync);
            });
        }
        threadPoolExecutor.shutdown();
    }

    private static void run(KafkaProducer producer, boolean isASync) {
        String topic = "test";
        int messageNo = 1;
        while (messageNo < 100000000) {
            System.out.println("开始发送消息");
            String messageStr = Thread.currentThread()  + " Message_司机来了" + messageNo;
            long startTime = System.currentTimeMillis();
            if (isASync) {
                producer.send(new ProducerRecord(topic, messageNo, messageStr), new DemoCallBack(startTime, messageNo, messageStr));
                System.out.println("异步发送消息, messageNo="
                        + messageNo + ",messageStr=" + messageStr);
            } else {
                List<String> strList;
                try {
                    // 同步调用，会卡主，知道对应消息完成或出异常
                    producer.send(new ProducerRecord(topic, messageNo, messageStr)).get();
                    System.out.println("发送消息, messageNo="
                            + messageNo + ",messageStr=" + messageStr);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            ++messageNo;
            // 休眠一阵子，等待回调。
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        // 休眠一阵子，等待回调。
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class DemoCallBack implements Callback {
    private final long startTime;
    private int key;
    private final String message;

    public DemoCallBack(long startTime, int key, String message) {
        this.startTime = startTime;
        this.key = key;
        this.message = message;
    }

    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (recordMetadata != null) {
            System.out.println("回调函数：message(" + key + ")" + message + "sent to partition(" + recordMetadata.partition() + ")"
                    + "offset(" + recordMetadata.offset() + ") in " + elapsedTime + "ms");
        } else {
            e.printStackTrace();
        }
    }
}
