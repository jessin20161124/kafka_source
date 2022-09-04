package com.jessin.kafka2;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * @author zexin.guo
 * @create 2018-02-04 下午12:48
 **/
public class ConsumerDemo {
    public static void main(String[] args) {
        Properties prop = new Properties();
        prop.put("bootstrap.servers", "localhost:9093");
        prop.put("group.id", "mygroup");
        prop.put("enable.auto.commit", "true");
        prop.put("session.timeout.ms", "30000");
        prop.put("key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer");
        prop.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer(prop);
        consumer.subscribe(Arrays.asList("test", "test2"), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.printf("rebalance，撤销partition:%s", partitions);

            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.printf("rebalance，分配partition:%s", partitions);

            }
        });
        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                // todo 这个版本，因为使用的是业务线程，如果长期不poll底层心跳会超时，自动提交也不会执行，所以业务消费逻辑不能太久
                for(ConsumerRecord<String, String> record : records) {
                    System.out.printf("offset = %d, key = %s, value = %s\n", record.offset(), record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }

    }
}
