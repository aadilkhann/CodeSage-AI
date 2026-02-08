import { useEffect, useRef, useCallback } from 'react';
import { websocketClient, MessageHandler } from '@/services/websocket';
import { useAppDispatch } from '@/store/hooks';
import { setCurrentAnalysis, addSuggestion } from '@/store/slices/pullRequestSlice';

export function useWebSocket(analysisId: string | null) {
    const dispatch = useAppDispatch();
    const isSubscribed = useRef(false);

    const handleMessage = useCallback((message: any) => {
        const { type, data } = message;

        switch (type) {
            case 'progress':
                // Update progress in UI
                console.log('Analysis progress:', data.percentage);
                break;

            case 'suggestion':
                // Add new suggestion to store
                dispatch(addSuggestion(data));
                break;

            case 'complete':
                // Update analysis status
                dispatch(setCurrentAnalysis(data.analysis));
                break;

            case 'error':
                // Handle error
                console.error('Analysis error:', data.message);
                break;

            default:
                console.log('Unknown message type:', type);
        }
    }, [dispatch]);

    useEffect(() => {
        if (!analysisId || isSubscribed.current) return;

        const connectAndSubscribe = async () => {
            try {
                // Connect if not already connected
                if (!websocketClient.isConnected()) {
                    await websocketClient.connect();
                }

                // Subscribe to analysis updates
                const destination = `/topic/analysis/${analysisId}`;
                websocketClient.subscribe(destination, handleMessage);
                isSubscribed.current = true;

                console.log(`Subscribed to ${destination}`);
            } catch (error) {
                console.error('WebSocket connection failed:', error);
            }
        };

        connectAndSubscribe();

        // Cleanup
        return () => {
            if (isSubscribed.current && analysisId) {
                websocketClient.unsubscribe(`/topic/analysis/${analysisId}`);
                isSubscribed.current = false;
            }
        };
    }, [analysisId, handleMessage]);

    return {
        isConnected: websocketClient.isConnected(),
    };
}
