import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { authService, User } from '@/services/auth';

export interface AuthState {
    user: User | null;
    token: string | null;
    refreshToken: string | null;
    isAuthenticated: boolean;
    loading: boolean;
    error: string | null;
}

const initialState: AuthState = {
    user: null,
    token: localStorage.getItem('token'),
    refreshToken: localStorage.getItem('refreshToken'),
    isAuthenticated: !!localStorage.getItem('token'),
    loading: false,
    error: null,
};

// Async thunks
export const checkAuth = createAsyncThunk(
    'auth/checkAuth',
    async (_, { rejectWithValue }) => {
        try {
            const user = await authService.getCurrentUser();
            return user;
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Authentication failed');
        }
    }
);

export const handleOAuthCallback = createAsyncThunk(
    'auth/handleCallback',
    async (_, { rejectWithValue }) => {
        try {
            const response = await authService.handleOAuthCallback();
            return response.data;
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'OAuth callback failed');
        }
    }
);

// Slice
const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        setUser: (state, action: PayloadAction<User>) => {
            state.user = action.payload;
            state.isAuthenticated = true;
        },
        setToken: (state, action: PayloadAction<{ token: string; refreshToken: string }>) => {
            state.token = action.payload.token;
            state.refreshToken = action.payload.refreshToken;
            state.isAuthenticated = true;
            localStorage.setItem('token', action.payload.token);
            localStorage.setItem('refreshToken', action.payload.refreshToken);
        },
        logout: (state) => {
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.isAuthenticated = false;
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
        },
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        // Check auth
        builder
            .addCase(checkAuth.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(checkAuth.fulfilled, (state, action) => {
                state.loading = false;
                state.user = action.payload;
                state.isAuthenticated = true;
            })
            .addCase(checkAuth.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload as string;
                state.isAuthenticated = false;
                state.user = null;
            });

        // Handle OAuth callback
        builder
            .addCase(handleOAuthCallback.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(handleOAuthCallback.fulfilled, (state, action) => {
                state.loading = false;
                state.user = action.payload.user;
                state.token = action.payload.token;
                state.refreshToken = action.payload.refreshToken;
                state.isAuthenticated = true;
            })
            .addCase(handleOAuthCallback.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload as string;
            });
    },
});

export const { setUser, setToken, logout, clearError } = authSlice.actions;
export default authSlice.reducer;
