#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>

#include "MQTTClient.h"
#include "MQTTClientPersistence.h"
#include "MQTTProperties.h"

#define ADDRESS     "ssl://127.0.0.1:8883"
#define CLIENTID    "C_Client"
#define TIMEOUT     10000L

#define QOS         0
#define TOPIC       "to/mqtt/response"

#define CAFILE      "server.pem"
#define CERTFILE    "T001.pem"
#define KEYFILE     "T001.key"


// 失去连接回调函数
void connect_lost(void *context, char *cause) {
    printf("Connection lost,The reason: %s \n",cause);
}

// 收到主题信息回调函数
int message_arrived(void *context, char *topicName, int topicLen, MQTTClient_message *message) {
    printf("Receive topic: %s, message data: \n", topicName);
    printf("%.*s\n", message->payloadlen, (char*)message->payload);
    MQTTClient_freeMessage(&message);
    MQTTClient_free(topicName);
    return 1;
}

// 主题发布成功回调函数
void delivery_complete(void *context, MQTTClient_deliveryToken dt) {
    printf("publish topic success,token = %d \n", dt);
}

int main() {
    printf("Start to connect MQTT broker.\n");

    MQTTClient client;
    int rc = 0;

    MQTTClient_message* msg = NULL;
    MQTTProperties props = MQTTProperties_initializer;

    // 创建一个MQTT客户端
    if (rc = MQTTClient_create(&client, ADDRESS, CLIENTID, MQTTCLIENT_PERSISTENCE_NONE, NULL) != MQTTCLIENT_SUCCESS) {
        printf("Failed to create client, return code %d\n", rc);
        rc = EXIT_FAILURE;
        exit(-1);
    }

    // 创建一个MQTT连接配置结构体，并配置其参数
    MQTTClient_SSLOptions ssl_opts = MQTTClient_SSLOptions_initializer;
    ssl_opts.trustStore = CAFILE;
    ssl_opts.keyStore = CERTFILE;
    ssl_opts.privateKey = KEYFILE;
    ssl_opts.enableServerCertAuth = 1;
    // 不校验host name
    ssl_opts.verify = 0;

    MQTTClient_connectOptions conn_opts = MQTTClient_connectOptions_initializer;
    conn_opts.ssl = &ssl_opts;
    conn_opts.MQTTVersion = MQTTVERSION_3_1_1;
    conn_opts.keepAliveInterval = 60;
    conn_opts.cleansession = 1;

    // 设置MQTT连接时的回调函数
    if ((rc = MQTTClient_setCallbacks(client, NULL, connect_lost, message_arrived, delivery_complete)) != MQTTCLIENT_SUCCESS)
    {
        printf("Failed to set callbacks, return code %d\n", rc);
        rc = EXIT_FAILURE;
        exit(-1);
    }

    rc = MQTTClient_connect(client, &conn_opts);
    if (rc != MQTTCLIENT_SUCCESS) {
        printf("Failed to connect, return code %d\n", rc);
        exit(-1);
    }

    printf("Connected to MQTT broker\n");

    MQTTClient_subscribe(client, TOPIC, QOS);

    // 定义一个主题消息存储结构体
    MQTTClient_message pubmsg = MQTTClient_message_initializer;
    char mag_data[] = "C client test!";
    pubmsg.payload = mag_data;
    pubmsg.payloadlen = (int)strlen(mag_data);
    pubmsg.qos = 0;                 // qos等级为1 
    pubmsg.retained = 0;            // 服务器不保留消息
    MQTTClient_deliveryToken token; // 标记MQTT消息的值，用来检查消息是否发送成功

    // 发布主题信息
    if ((rc = MQTTClient_publishMessage(client, "pubtest", &pubmsg, &token)) != MQTTCLIENT_SUCCESS)
    {
        printf("Failed to publish message, return code %d\n", rc);
        exit(EXIT_FAILURE);
    }

    // 等待输入'Q'或'q'退出
    printf("Press Q or q + <Enter> to quit\n\n");
    int ch;
    do {
        ch = getchar();
    } while (ch!='Q' && ch != 'q');

    MQTTClient_disconnect(client, 10000);
    MQTTClient_destroy(&client);

    return rc;
}