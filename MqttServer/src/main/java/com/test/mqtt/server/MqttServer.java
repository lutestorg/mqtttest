package com.test.mqtt.server;

import io.moquette.broker.Server;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.broker.config.ClasspathResourceLoader;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.IResourceLoader;
import io.moquette.broker.config.ResourceLoaderConfig;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MqttServer {
    private static Server mqttBroker;

    static class PublisherListener extends AbstractInterceptHandler {
        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = msg.getPayload().toString(StandardCharsets.UTF_8);
            log.info("Received on topic: {}, content: {}", msg.getTopicName(), decodedPayload);

            MqttPublishMessage message = MqttMessageBuilders.publish()
                    .topicName("to/mqtt/response")
                    .retained(true)
                    .qos(MqttQoS.AT_MOST_ONCE)
                    .payload(Unpooled.copiedBuffer("Hello World!!".getBytes(StandardCharsets.UTF_8)))
                    .build();

            mqttBroker.internalPublish(message, "INTRLPUB");
        }
    }

    public static void main(String[] args) throws IOException {
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);

        mqttBroker = new Server();
        List<? extends InterceptHandler> userHandlers = Collections.singletonList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);

        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping broker");
            mqttBroker.stopServer();
            System.out.println("Broker stopped");
        }));

        log.info("Broker started success");
    }
}
