import re
import json
import ssl
import time
import os

import paho.mqtt.client as mqtt

from inspect import isfunction


class MqttClient(object):
    # MQTT client
    client = None
    # MQTT message handlers
    message_handlers = None
    # MQTT config
    config = None

    def __init__(self, config):
        self.config = config
        self.client = mqtt.Client(
            client_id = self.config['client_id'] if 'client_id' in self.config else 'client_id')
        cert_path = os.path.dirname(__file__)
        ca_cert = cert_path + "/server.pem"
        cert_file = cert_path + "/T001.pem"
        key_file = cert_path + "/T001.key"
        self.client.tls_set(ca_certs=ca_cert, certfile=cert_file, keyfile=key_file, tls_version=ssl.PROTOCOL_TLSv1_2)
        self.client.tls_insecure_set(True)

        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message
        self.client.on_subscribe = self.on_subscribe

    def connect(self):
        '''connect to the mqtt server. '''
        # self.client.username_pw_set(
        #     self.config['username'] if 'username' in self.config else None, self.config['password'])
        self.client.connect(
            self.config['host'], self.config['port'],
            keepalive=self.config['keepalive'] if 'keepalive' in self.config else 60)
        self.client.loop_start()

    def disconnect(self):
        self.client.loop_stop()

    def add_message_handler(self, handler, topic):
        '''
        add message hanlder
        :param handler: handle message function
        :param topic: handle the assigned topic message
        '''
        if topic not in self.message_handlers:
            self.message_handlers[topic] = []

        self.message_handlers[topic].append(handler)

    def on_connect(self, client, userdata, flags, rc):
        '''
        callback func when client connected to the mqtt server.
        rc  0：连接成功
            1：连接被拒绝-协议版本不正确
            2：连接被拒绝-无效的客户端标识符
            3：连接被拒绝-服务器不可用
            4：连接被拒绝-用户名或密码错误
            5：连接被拒绝-未经授权
            6-255：当前未使用。
        '''
        print(client, userdata, flags, rc)

    def on_disconnect(self, client, userdata, rc):
        print(client, userdata, rc)

    def on_subscribe(self, client, userdata, mid, granted_qos):
        print(client, userdata, mid, granted_qos)

    def on_message(self, client, userdata, message):
        '''
        handle the message when a PUBLISH message is received from the server
        handlers should not set blocked
        '''
        print(client, userdata, message.topic, message.payload)
        if self.message_handlers:
            for topic in self.message_handlers.keys():
                regex = re.compile(
                    '^{}$'.format(topic.replace('+', '[^\/\s]+').replace('#', '\S+').replace('/', '\/').replace('$', '\$')))
                if regex.match(message.topic):
                    for handler in self.message_handlers.get(topic, []):
                        handler(message.payload)

    def subscribe(self, topic, **kwargs):
        '''
        subscribe topic message
        default set qos to 0
        '''
        print(topic)
        return self.client.subscribe(
            topic, **kwargs)

    def publish(self, topic, message, **kwargs):
        '''publish message through topic'''
        return self.client.publish(topic, json.dumps(message, ensure_ascii=False), **kwargs)

    def unsubscribe(self, topic, properties=None):
        '''unsubscribe topic'''
        return self.client.unsubscribe(topic, properties=properties)


mqtt_client = MqttClient(config = {'host': '127.0.0.1', 'port': 8883, 'client_id': 'T001', 'keepalive': 60})
mqtt_client.connect()
mqtt_client.subscribe("to/mqtt/response")

while 1:
    mqtt_client.publish("from/python/test", "python test!", qos=0)
    time.sleep(10)