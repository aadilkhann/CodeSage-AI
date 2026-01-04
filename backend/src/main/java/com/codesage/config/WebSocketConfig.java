package com.codesage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 * 
 * Sets up STOMP over WebSocket for real-time communication
 * 
 * Architecture:
 * - Clients connect to /ws endpoint
 * - Subscribe to /topic/* for public broadcasts
 * - Subscribe to /queue/* for private messages
 * - Send messages to /app/* endpoints
 * 
 * Example client usage (JavaScript):
 * const client = new StompJs.Client({
 * brokerURL: 'ws://localhost:8080/ws',
 * onConnect: () => {
 * client.subscribe('/topic/analysis/{id}', (message) => {
 * console.log(JSON.parse(message.body));
 * });
 * }
 * });
 * client.activate();
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker
     * 
     * /topic - For broadcasting to multiple subscribers (e.g., analysis updates)
     * /queue - For point-to-point messaging (private user messages)
     * /app - Application destination prefix for messages from clients
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints
     * 
     * /ws - WebSocket endpoint with SockJS fallback
     * SockJS provides fallback options for browsers that don't support WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .withSockJS(); // Enable SockJS fallback
    }
}
