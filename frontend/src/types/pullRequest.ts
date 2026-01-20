export interface PullRequest {
    id: string;
    repositoryId: string;
    repositoryFullName: string;
    prNumber: number;
    title: string;
    description: string;
    author: string;
    baseBranch: string;
    headBranch: string;
    status: string;
    githubUrl: string;
    filesChanged: number;
    additions: number;
    deletions: number;
    createdAt: string;
    updatedAt: string;
}

export interface Analysis {
    id: string;
    pullRequestId: string;
    status: 'pending' | 'processing' | 'completed' | 'failed';
    filesAnalyzed: number;
    durationMs: number;
    errorMessage?: string;
    createdAt: string;
    completedAt?: string;
}

export interface Suggestion {
    id: string;
    analysisId: string;
    filePath: string;
    lineNumber: number;
    lineEnd?: number;
    category: string;
    severity: 'critical' | 'moderate' | 'minor';
    message: string;
    explanation: string;
    suggestedFix?: string;
    confidenceScore: number;
    status: 'pending' | 'accepted' | 'rejected' | 'ignored';
    userFeedback?: string;
    createdAt: string;
}
