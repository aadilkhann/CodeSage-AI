import api from './api';
import { Repository, AddRepositoryRequest } from '@/types/repository';

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

export const getRepositories = async (): Promise<Repository[]> => {
    const response = await api.get<ApiResponse<Repository[]>>('/api/v1/repositories');
    return response.data.data;
};

export const getRepository = async (id: string): Promise<Repository> => {
    const response = await api.get<ApiResponse<Repository>>(`/api/v1/repositories/${id}`);
    return response.data.data;
};

export const addRepository = async (data: AddRepositoryRequest): Promise<Repository> => {
    const response = await api.post<ApiResponse<Repository>>('/api/v1/repositories', data);
    return response.data.data;
};

export const updateRepository = async (
    id: string,
    data: Partial<Repository>
): Promise<Repository> => {
    const response = await api.put<ApiResponse<Repository>>(`/api/v1/repositories/${id}`, data);
    return response.data.data;
};

export const deleteRepository = async (id: string): Promise<void> => {
    await api.delete(`/api/v1/repositories/${id}`);
};

export const syncRepository = async (id: string): Promise<void> => {
    await api.post(`/api/v1/repositories/${id}/sync`);
};

export const repositoryService = {
    getRepositories,
    getRepository,
    addRepository,
    updateRepository,
    deleteRepository,
    syncRepository,
};

export default repositoryService;
