import api from './api';

export interface User {
    id: string;
    username: string;
    email: string;
    avatarUrl: string;
    githubId: number;
}

export interface AuthResponse {
    success: boolean;
    data: {
        user: User;
        token: string;
        refreshToken: string;
    };
}

/**
 * Initiate GitHub OAuth login
 * Redirects to backend OAuth endpoint
 */
export const initiateGitHubLogin = () => {
    const backendUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    window.location.href = `${backendUrl}/oauth2/authorize/github`;
};

/**
 * Handle OAuth callback
 * Called from callback page after GitHub redirects back
 */
export const handleOAuthCallback = async (): Promise<AuthResponse> => {
    // Token should be in URL params or cookie
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const refreshToken = urlParams.get('refreshToken');

    if (token && refreshToken) {
        // Store tokens
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);

        // Fetch user profile
        const response = await api.get<AuthResponse>('/api/v1/auth/me');
        return response.data;
    }

    throw new Error('No authentication tokens found');
};

/**
 * Get current user profile
 */
export const getCurrentUser = async (): Promise<User> => {
    const response = await api.get<AuthResponse>('/api/v1/auth/me');
    return response.data.data.user;
};

/**
 * Refresh JWT token
 */
export const refreshToken = async (refreshToken: string): Promise<string> => {
    const response = await api.post<AuthResponse>('/api/v1/auth/refresh', {
        refreshToken,
    });
    return response.data.data.token;
};

/**
 * Logout user
 */
export const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
};

export const authService = {
    initiateGitHubLogin,
    handleOAuthCallback,
    getCurrentUser,
    refreshToken,
    logout,
};

export default authService;
