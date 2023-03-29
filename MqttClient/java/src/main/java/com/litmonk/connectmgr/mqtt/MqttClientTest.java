package com.litmonk.connectmgr.mqtt;

import com.litmonk.connectmgr.mqtt.utils.SslUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

@Slf4j
public class MqttClientTest {
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;

    /**
     * 创建连接
     *
     * @throws Exception
     */
    public void deviceConnect(String host, String deviceId) throws Exception {
        //缓存两种模式 存在内存 文件  设置成null 缓存在内存中 最多缓存65535条信息
        //ScheduledExecutorService 可以设置线程池大小 默认10；发布消息方法是异步的
        this.mqttClient = new MqttClient(host, deviceId, null);
        mqttConnectOptions = new MqttConnectOptions();
        // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
        // 这里设置为true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);
        // mqttConnectOptions.sto
        // 设置超时时间 s
        mqttConnectOptions.setConnectionTimeout(20);
        // 设置会话心跳时间
        mqttConnectOptions.setKeepAliveInterval(60);
        mqttConnectOptions.setAutomaticReconnect(true);
        //setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
        // mqttConnectOptions.setWill("sec", "close".getBytes(), 2, true);

        //客户端证书路径
        String keyStoreFile = "/" + deviceId + ".jks";
        mqttConnectOptions.setSocketFactory(SslUtil.getTrustAllSocketFactory(keyStoreFile, "JKS", "Test_001"));

        // 忽略域名认证
        mqttConnectOptions.setHttpsHostnameVerificationEnabled(false);

        //mqttConnectOptions.setWill();   //可以设置断线发送、接收消息
        mqttClient.connect(mqttConnectOptions);
        log.info("mqtt 连接成功！！！");
    }

    /**
     * 完成连接时 主要用于断开连接时 重新订阅
     * <p>
     * testTopic/#   #多层通配符    +单层通配符
     * topicFilter
     *
     * @param topicFilter
     * @param iotMqttMessageListener
     * @throws Exception
     */
    public void setReSubscribe(String topicFilter, MqttMessageListener iotMqttMessageListener) throws Exception {
        //先订阅一次
        mqttClient.subscribe(topicFilter, iotMqttMessageListener);
        //断线重连
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    try {
                        mqttClient.subscribe(topicFilter, iotMqttMessageListener);
                        log.info("mqtt重新建立连接后,topic={} 重新订阅！", topicFilter);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    /**
     * 取消订阅
     *
     * @param topics
     * @throws Exception
     */
    public void unsubscribe(String[] topics) throws Exception {
        mqttClient.unsubscribe(topics);
    }

    /**
     * 是否处于连接状态
     *
     * @return
     */
    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    /**
     * 发布数据
     *
     * @param
     * @param data
     * @throws Exception
     */
    public void publish(String publicLiveTopic, String data) throws Exception {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(data.getBytes("UTF-8"));
        //QoS：发布消息的服务质量，即：保证消息传递的次数（消费者收到的次数）
        //0：最多一次，即：<=1；每个消息只发一次，也不会缓存下来。
        //1：至少一次，即：>=1；一直发送确保消费者至少收到一次，发送失败会缓存下来。
        //2：一次，即：=1       一直发送确保消费者只能收到一次；发送失败会缓存下来 。
        mqttMessage.setQos(1);
        //消费者断开连接后是否接受离线消息
        mqttMessage.setRetained(true);
        mqttClient.publish(publicLiveTopic, mqttMessage);
        log.info("topic:{} send  dataSize {}kb ", publicLiveTopic, data.length() / 1024.0);
    }

    /**
     * 断开连接
     */
    public void destroy() {
        try {
            this.mqttClient.disconnect();
            log.info("mqtt 手动断开连接！！！");
        } catch (MqttException e) {
            log.error("手动断开连接报错error={}", e.getMessage());
        }
    }

    /**
     * 设置连接
     *
     * @param args
     */
    public static void main(String[] args) {
        MqttClientTest xbMqttDataUtil = new MqttClientTest();

        try {
            xbMqttDataUtil.deviceConnect("ssl://127.0.0.1:8883", "T001");
            xbMqttDataUtil.setReSubscribe("/mqtt/#", new MqttMessageListener());

            while (true) {
                xbMqttDataUtil.publish("test/", "hello mqtt!");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
