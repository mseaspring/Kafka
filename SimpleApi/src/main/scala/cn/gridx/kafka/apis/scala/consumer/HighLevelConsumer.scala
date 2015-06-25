package cn.gridx.kafka.apis.scala.consumer

import java.util.Properties

import kafka.consumer.{KafkaStream, ConsumerConfig, Consumer}
import kafka.message.MessageAndMetadata
import org.apache.kafka.clients.producer.Producer

import scala.collection.Map
import scala.collection.mutable.HashMap

/**
 * Created by tao on 6/24/15.
 *
 * 展示一个Consumer的最基本用法
 *
 * 运行命令： java -cp SimpleAPI-1.0-SNAPSHOT.jar:jars\*  cn.gridx.kafka.apis.scala.consumer.HighLevelConsumer
 *    其中，jars目录中的依赖包如下：
 *
 *          kafka_2.10-0.8.2.0.jar
            kafka-clients-0.8.2.0.jar
            log4j-1.2.16.jar
            metrics-core-2.2.0.jar
            scala-library.jar
            slf4j-api-1.7.6.jar
            zkclient-0.3.jar
            zookeeper-3.3.1.jar


    HighLevelConsumer启动后，会等待名为`my-2nd-topic`的topic中的消息。
    此时，运行cn.gridx.kafka.apis.scala.producer.ProduceKeyedMsg 来产生新消息
 *
 */
object HighLevelConsumer {
    def ZK_CONN     = "ecs1:2181,ecs2:2181,ecs3:2181"
    def GROUP_ID    = "xt-group-1"
    def TOPIC       = "my-2nd-topic"

    def main(args: Array[String]): Unit = {
        println(" 开始了 !!")

        val connector = Consumer.create(createConfig)

        /**
         * `topicCountMap` tells Kafka how many threads we are providing for which topics
         *
         * 参考 [5.1 API Design](http://kafka.apache.org/08/implementation.html)
         *
         *  Each `KafkaStream` represents the stream of messages from one or more partitions
         *  on one or more servers. Each stream is used for single threaded processing, so
         *  the client can provide the number of desired streams in the create call.
         *
         *  Thus a stream may represent the merging of multiple server partitions (to correspond
         *  to the number of processing threads), but each partition only goes to one stream.
         *
         */
        val topicCountMap = new HashMap[String, Int]()
        topicCountMap.put(TOPIC, 1)

        /**
         * `createMessageStreams`
         *
         * This method is used to get a list of `KafkaStream`s, which are iterators over
         * `MessageAndMetadata` objects from which you can obtain messages and their
         * associated metadata (currently only topic).
         *  Input: a map of <topic, #streams>
         *  Output: a map of <topic, list of message streams>
         *
         *     `createMessageStreams` 的实现在哪里？
         *
         *     第一个 Array[Byte]是key，第二个Array[Byte]是message
         *
         *
         *  The `createMessageStreams` call registers the consumer for the topic, which
         *  results in rebalancing the broker/consumer assignment.
         */
        val msgStreams: Map[String, List[KafkaStream[Array[Byte], Array[Byte]]]]
            = connector.createMessageStreams(topicCountMap)

        println("# of message streams is " + msgStreams.get(TOPIC).size)

        val stream: KafkaStream[Array[Byte], Array[Byte]] = msgStreams.get(TOPIC).get(0)
        val it = stream.iterator()

        // 会消费topic中目前还未被消费的message
        while (it.hasNext) {
            val data: MessageAndMetadata[Array[Byte], Array[Byte]] = it.next
            // 如果produce的是正常的ProducerRecord[String, String],那么输出的key和value是正常的字符串，
            println("msg -> " + new String(data.message) + "  ||   key -> " + new String(data.key))
        }

        println(" 结束了 !!")
    }


    /**
     *  结果配置项的意义
     *
     * `zookeeper.connect` : Kafka uses ZK to store offset of messages consumed for
     *         a specific topic and partition by this Consumer Group
     *
     * `group.id` : this string defines the Consumer Group this process is consuming on behalf of
     *
     * `zookeeper.session.timeout.ms` :  how many milliseconds Kafka will wait for ZK to respond to
     *          a read or write request before giving up and continuing to consume messages
     *
     *
     *  `zookeeper.sync.time.ms` : the number of milliseconds a ZK follower can be behind the master
     *          before an error occurs
     *
     *  `auto.commit.interval.ms` : how often updates to the consumed offsets are written to ZK.
     *          Since the commit frequency if time based instead of # of messages consumed, if an
     *          error occurs between updates to ZK on restart you will get replayed messages
     *
     * @return
     */
    def createConfig(): ConsumerConfig = {
        val props = new Properties();
        props.put("zookeeper.connect", ZK_CONN)
        props.put("group.id", GROUP_ID)
        props.put("zookeeper.session.timeout.ms", "400")
        props.put("zookeeper.sync.time.ms", "200")
        props.put("auto.commit.interval.ms", "1000")

        new ConsumerConfig(props)
    }
}