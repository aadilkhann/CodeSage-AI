import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import repositoryReducer from './slices/repositorySlice';
import pullRequestReducer from './slices/pullRequestSlice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        repositories: repositoryReducer,
        pullRequests: pullRequestReducer,
    },
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
            serializableCheck: false,
        }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
