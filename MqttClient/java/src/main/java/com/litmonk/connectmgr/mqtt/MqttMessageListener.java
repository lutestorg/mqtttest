package com.litmonk.connectmgr.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 订阅信息监听类
 */
public class MqttMessageListener implements IMqttMessageListener {

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        System.out.println(mqttMessage.getId());
        System.out.println(mqttMessage.getPayload());
        System.out.println(mqttMessage.getQos());
        System.out.println(mqttMessage.isRetained());
        System.out.println(mqttMessage.isDuplicate());
        System.out.println(topic);
    }
}
