import api from './api';
import { PullRequest, Analysis, Suggestion } from '@/types/pullRequest';

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

interface PaginatedResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

export const getPullRequests = async (params?: {
    repositoryId?: string;
    status?: string;
    page?: number;
    size?: number;
}): Promise<PaginatedResponse<PullRequest>> => {
    const queryParams = new URLSearchParams();
    if (params?.repositoryId) queryParams.append('repositoryId', params.repositoryId);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());

    const response = await api.get<ApiResponse<PaginatedResponse<PullRequest>>>(
        `/api/v1/pull-requests?${queryParams.toString()}`
    );
    return response.data.data;
};

export const getPullRequest = async (id: string): Promise<PullRequest> => {
    const response = await api.get<ApiResponse<PullRequest>>(`/api/v1/pull-requests/${id}`);
    return response.data.data;
};

export const triggerAnalysis = async (id: string): Promise<{ analysisId: string }> => {
    const response = await api.post<ApiResponse<{ analysisId: string }>>(
        `/api/v1/pull-requests/${id}/analyze`
    );
    return response.data.data;
};

export const getAnalysis = async (prId: string): Promise<Analysis> => {
    const response = await api.get<ApiResponse<Analysis>>(`/api/v1/pull-requests/${prId}/analysis`);
    return response.data.data;
};

export const getSuggestions = async (analysisId: string): Promise<Suggestion[]> => {
    // This would be a separate endpoint in reality
    const response = await api.get<ApiResponse<Suggestion[]>>(`/api/v1/suggestions?analysisId=${analysisId}`);
    return response.data.data;
};

export const updateSuggestion = async (
    id: string,
    data: { status: string; userFeedback?: string }
): Promise<Suggestion> => {
    const response = await api.put<ApiResponse<Suggestion>>(`/api/v1/suggestions/${id}`, data);
    return response.data.data;
};

export const pullRequestService = {
    getPullRequests,
    getPullRequest,
    triggerAnalysis,
    getAnalysis,
    getSuggestions,
    updateSuggestion,
};

export default pullRequestService;
