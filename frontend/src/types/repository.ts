export interface Repository {
    id: string;
    fullName: string;
    description: string;
    language: string;
    githubRepoId: number;
    isActive: boolean;
    settings: {
        autoAnalyze?: boolean;
        minConfidenceScore?: number;
    };
    webhookId?: string;
    createdAt: string;
    updatedAt: string;
}

export interface AddRepositoryRequest {
    githubRepoId: number;
    fullName: string;
    description: string;
    language: string;
}
