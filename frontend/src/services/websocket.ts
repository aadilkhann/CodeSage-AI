import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type MessageHandler = (message: any) => void;

class WebSocketClient {
    private client: Client | null = null;
    private subscriptions: Map<string, any> = new Map();
    private isConnecting = false;
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;

    constructor(private url: string) { }

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.client?.connected) {
                resolve();
                return;
            }

            if (this.isConnecting) {
                reject(new Error('Connection already in progress'));
                return;
            }

            this.isConnecting = true;

            try {
                this.client = new Client({
                    webSocketFactory: () => new SockJS(this.url),
                    reconnectDelay: 5000,
                    heartbeatIncoming: 4000,
                    heartbeatOutgoing: 4000,
                    onConnect: () => {
                        console.log('WebSocket connected');
                        this.isConnecting = false;
                        this.reconnectAttempts = 0;
                        resolve();
                    },
                    onStompError: (frame) => {
                        console.error('STOMP error:', frame);
                        this.isConnecting = false;
                        reject(new Error(frame.headers['message'] || 'WebSocket error'));
                    },
                    onWebSocketClose: () => {
                        console.log('WebSocket closed');
                        this.isConnecting = false;

                        // Auto-reconnect
                        if (this.reconnectAttempts < this.maxReconnectAttempts) {
                            this.reconnectAttempts++;
                            console.log(`Reconnecting... (attempt ${this.reconnectAttempts})`);
                            setTimeout(() => this.connect(), 2000 * this.reconnectAttempts);
                        }
                    },
                });

                this.client.activate();
            } catch (error) {
                this.isConnecting = false;
                reject(error);
            }
        });
    }

    disconnect(): void {
        if (this.client) {
            this.subscriptions.clear();
            this.client.deactivate();
            this.client = null;
        }
    }

    subscribe(destination: string, handler: MessageHandler): void {
        if (!this.client?.connected) {
            throw new Error('WebSocket not connected');
        }

        const subscription = this.client.subscribe(destination, (message) => {
            try {
                const data = JSON.parse(message.body);
                handler(data);
            } catch (error) {
                console.error('Error parsing message:', error);
            }
        });

        this.subscriptions.set(destination, subscription);
    }

    unsubscribe(destination: string): void {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    send(destination: string, body: any): void {
        if (!this.client?.connected) {
            throw new Error('WebSocket not connected');
        }

        this.client.publish({
            destination,
            body: JSON.stringify(body),
        });
    }

    isConnected(): boolean {
        return this.client?.connected || false;
    }
}

// Singleton instance
const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
export const websocketClient = new WebSocketClient(wsUrl);

export default websocketClient;
