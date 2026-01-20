import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { repositoryService } from '@/services/repositories';
import { Repository, AddRepositoryRequest } from '@/types/repository';

export interface RepositoryState {
    repositories: Repository[];
    selectedRepository: Repository | null;
    loading: boolean;
    error: string | null;
}

const initialState: RepositoryState = {
    repositories: [],
    selectedRepository: null,
    loading: false,
    error: null,
};

// Async thunks
export const fetchRepositories = createAsyncThunk(
    'repositories/fetchAll',
    async (_, { rejectWithValue }) => {
        try {
            return await repositoryService.getRepositories();
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch repositories');
        }
    }
);

export const fetchRepository = createAsyncThunk(
    'repositories/fetchOne',
    async (id: string, { rejectWithValue }) => {
        try {
            return await repositoryService.getRepository(id);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch repository');
        }
    }
);

export const addRepository = createAsyncThunk(
    'repositories/add',
    async (data: AddRepositoryRequest, { rejectWithValue }) => {
        try {
            return await repositoryService.addRepository(data);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to add repository');
        }
    }
);

export const updateRepository = createAsyncThunk(
    'repositories/update',
    async ({ id, data }: { id: string; data: Partial<Repository> }, { rejectWithValue }) => {
        try {
            return await repositoryService.updateRepository(id, data);
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to update repository');
        }
    }
);

export const deleteRepository = createAsyncThunk(
    'repositories/delete',
    async (id: string, { rejectWithValue }) => {
        try {
            await repositoryService.deleteRepository(id);
            return id;
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Failed to delete repository');
        }
    }
);

// Slice
const repositorySlice = createSlice({
    name: 'repositories',
    initialState,
    reducers: {
        setSelectedRepository: (state, action: PayloadAction<Repository | null>) => {
            state.selectedRepository = action.payload;
        },
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        // Fetch all repositories
        builder
            .addCase(fetchRepositories.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchRepositories.fulfilled, (state, action) => {
                state.loading = false;
                state.repositories = action.payload;
            })
            .addCase(fetchRepositories.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload as string;
            });

        // Fetch single repository
        builder
            .addCase(fetchRepository.fulfilled, (state, action) => {
                state.selectedRepository = action.payload;
            });

        // Add repository
        builder
            .addCase(addRepository.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(addRepository.fulfilled, (state, action) => {
                state.loading = false;
                state.repositories.push(action.payload);
            })
            .addCase(addRepository.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload as string;
            });

        // Update repository
        builder
            .addCase(updateRepository.fulfilled, (state, action) => {
                const index = state.repositories.findIndex(r => r.id === action.payload.id);
                if (index !== -1) {
                    state.repositories[index] = action.payload;
                }
                if (state.selectedRepository?.id === action.payload.id) {
                    state.selectedRepository = action.payload;
                }
            });

        // Delete repository
        builder
            .addCase(deleteRepository.fulfilled, (state, action) => {
                state.repositories = state.repositories.filter(r => r.id !== action.payload);
                if (state.selectedRepository?.id === action.payload) {
                    state.selectedRepository = null;
                }
            });
    },
});

export const { setSelectedRepository, clearError } = repositorySlice.actions;
export default repositorySlice.reducer;
