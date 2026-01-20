import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { pullRequestService } from '@/services/pullRequests';
import { PullRequest, Analysis, Suggestion } from '@/types/pullRequest';

export interface PullRequestState {
    pullRequests: PullRequest[];
    selectedPR: PullRequest | null;
    currentAnalysis: Analysis | null;
    suggestions: Suggestion[];
    loading: boolean;
    error: string | null;
    totalPages: number;
    currentPage: number;
}

const initialState: PullRequestState = {
    pullRequests: [],
    selectedPR: null,
    currentAnalysis: null,
    suggestions: [],
    loading: false,
    error: null,
    totalPages: 0,
    currentPage: 0,
};

// Async thunks
export const fetchPullRequests = createAsyncThunk(
    'pullRequests/fetchAll',
    async (params: { repositoryId?: string; status?: string; page?: number }, { rejectWithValue }) => {
        try {
            return await pullRequestService.getPullRequests(params);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch pull requests');
        }
    }
);

export const fetchPullRequest = createAsyncThunk(
    'pullRequests/fetchOne',
    async (id: string, { rejectWithValue }) => {
        try {
            return await pullRequestService.getPullRequest(id);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch pull request');
        }
    }
);

export const triggerAnalysis = createAsyncThunk(
    'pullRequests/triggerAnalysis',
    async (id: string, { rejectWithValue }) => {
        try {
            return await pullRequestService.triggerAnalysis(id);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to trigger analysis');
        }
    }
);

export const fetchSuggestions = createAsyncThunk(
    'pullRequests/fetchSuggestions',
    async (analysisId: string, { rejectWithValue }) => {
        try {
            return await pullRequestService.getSuggestions(analysisId);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch suggestions');
        }
    }
);

export const updateSuggestion = createAsyncThunk(
    'pullRequests/updateSuggestion',
    async ({ id, data }: { id: string; data: { status: string; userFeedback?: string } }, { rejectWithValue }) => {
        try {
            return await pullRequestService.updateSuggestion(id, data);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to update suggestion');
        }
    }
);

// Slice
const pullRequestSlice = createSlice({
    name: 'pullRequests',
    initialState,
    reducers: {
        setSelectedPR: (state, action: PayloadAction<PullRequest | null>) => {
            state.selectedPR = action.payload;
        },
        setCurrentAnalysis: (state, action: PayloadAction<Analysis | null>) => {
            state.currentAnalysis = action.payload;
        },
        addSuggestion: (state, action: PayloadAction<Suggestion>) => {
            state.suggestions.push(action.payload);
        },
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        // Fetch all PRs
        builder
            .addCase(fetchPullRequests.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchPullRequests.fulfilled, (state, action) => {
                state.loading = false;
                state.pullRequests = action.payload.content;
                state.totalPages = action.payload.totalPages;
                state.currentPage = action.payload.number;
            })
            .addCase(fetchPullRequests.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload as string;
            });

        // Fetch single PR
        builder
            .addCase(fetchPullRequest.fulfilled, (state, action) => {
                state.selectedPR = action.payload;
            });

        // Fetch suggestions
        builder
            .addCase(fetchSuggestions.fulfilled, (state, action) => {
                state.suggestions = action.payload;
            });

        // Update suggestion
        builder
            .addCase(updateSuggestion.fulfilled, (state, action) => {
                const index = state.suggestions.findIndex(s => s.id === action.payload.id);
                if (index !== -1) {
                    state.suggestions[index] = action.payload;
                }
            });
    },
});

export const { setSelectedPR, setCurrentAnalysis, addSuggestion, clearError } = pullRequestSlice.actions;
export default pullRequestSlice.reducer;
