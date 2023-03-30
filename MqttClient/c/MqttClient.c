#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

#include <openssl/ssl.h>
#include <openssl/err.h>

#include "MQTTClient.h"
#include "MQTTClientPersistence.h"
#include "MQTTProperties.h"

#define ADDRESS     "ssl://localhost:8883"
#define CLIENTID    "ExampleClientSub"
#define TIMEOUT     10000L

#define QOS         1
#define TOPIC       "example/topic"
#define PAYLOAD     "Hello, world!"
#define PAYLOAD_LEN strlen(PAYLOAD)

#define CAFILE      "/path/to/ca.crt"
#define CERTFILE    "/path/to/client.crt"
#define KEYFILE     "/path/to/client.key"

volatile int toStop = 0;

void intHandler(int dummy) {
    toStop = 1;
}

int main(int argc, char* argv[]) {
    signal(SIGINT, intHandler);

    MQTTClient_SSLOptions ssl_opts = MQTTClient_SSLOptions_initializer;
    ssl_opts.trustStore = CAFILE;
    ssl_opts.keyStore = CERTFILE;
    ssl_opts.privateKey = KEYFILE;
    ssl_opts.enableServerCertAuth = 1;

    MQTTClient_createOptions create_opts = MQTTClient_createOptions_initializer;
    create_opts.SSLOpts = &ssl_opts;

    MQTTClient client;
    MQTTClient_connectOptions conn_opts = MQTTClient_connectOptions_initializer;
    int rc = 0;

    MQTTClient_message* msg = NULL;
    MQTTProperties props = MQTTProperties_initializer;

    MQTTClient_createWithOptions(&client, ADDRESS, CLIENTID, MQTTCLIENT_PERSISTENCE_NONE, NULL, &create_opts);

    conn_opts.keepAliveInterval = 20;
    conn_opts.cleansession = 1;

    rc = MQTTClient_connect(client, &conn_opts);
    if (rc != MQTTCLIENT_SUCCESS) {
        printf("Failed to connect, return code %d\n", rc);
        exit(-1);
    }

    printf("Connected to MQTT broker\n");

    MQTTClient_subscribe(client, TOPIC, QOS);

    while (!toStop) {
        MQTTClient_messageArrived* cb = MQTTClient_waitForMessage(client, 1000);
        if (cb) {
            msg = cb->message;

            printf("Received message: %.*s\n", (int)msg->payloadlen, (char*)msg->payload);
            MQTTClient_freeMessage(&msg);
            MQTTClient_free(msg);
        }
        MQTTClient_yield();
    }

    MQTTClient_disconnect(client, 10000);
    MQTTClient_destroy(&client);

    return rc;
}