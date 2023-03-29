package com.test.mqtt.client;

import com.test.mqtt.client.utils.SslUtil;
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
        this.mqttClient = new MqttClient(host, deviceId, null);
        mqttConnectOptions = new MqttConnectOptions();
        // 设置是否清空session
        mqttConnectOptions.setCleanSession(true);
        // 设置超时时间
        mqttConnectOptions.setConnectionTimeout(20);
        // 设置会话心跳时间
        mqttConnectOptions.setKeepAliveInterval(60);
        mqttConnectOptions.setAutomaticReconnect(true);

        //客户端证书路径
        String keyStoreFile = "/" + deviceId + ".jks";
        mqttConnectOptions.setSocketFactory(SslUtil.getTrustAllSocketFactory(keyStoreFile, "JKS", "Test_001"));

        // 忽略域名认证
        mqttConnectOptions.setHttpsHostnameVerificationEnabled(false);

        mqttClient.connect(mqttConnectOptions);
        log.info("mqtt connect success!");
    }

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
                        log.info("ReSubscribe mqtt topic: {}.", topicFilter);
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

    public void publish(String publicLiveTopic, String data) throws Exception {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(data.getBytes("UTF-8"));
        mqttMessage.setQos(0);
        mqttClient.publish(publicLiveTopic, mqttMessage);
    }

    public void destroy() {
        try {
            this.mqttClient.disconnect();
            log.info("mqtt disconnect.");
        } catch (MqttException e) {
            log.error("MqttException: {}.", e.getMessage());
        }
    }

    public static void main(String[] args) {
        MqttClientTest xbMqttDataUtil = new MqttClientTest();

        try {
            xbMqttDataUtil.deviceConnect("ssl://127.0.0.1:8883", "T001");
            xbMqttDataUtil.setReSubscribe("to/mqtt/response", new MqttMessageListener());

            while (true) {
                xbMqttDataUtil.publish("from/java/test", "hello mqtt!");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
