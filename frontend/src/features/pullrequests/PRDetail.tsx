import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Play, GitBranch, User } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { Card, CardHeader, CardTitle, CardBody } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Badge from '@/components/ui/Badge';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchPullRequest, triggerAnalysis, fetchSuggestions } from '@/store/slices/pullRequestSlice';
import SuggestionList from './SuggestionList';

export default function PRDetail() {
    const { id } = useParams<{ id: string }>();
    const dispatch = useAppDispatch();
    const { selectedPR, currentAnalysis: _currentAnalysis, suggestions } = useAppSelector((state) => state.pullRequests);
    const [activeTab, setActiveTab] = useState<'overview' | 'suggestions'>('overview');
    const [analyzing, setAnalyzing] = useState(false);

    useEffect(() => {
        if (id) {
            dispatch(fetchPullRequest(id));
        }
    }, [dispatch, id]);

    const handleTriggerAnalysis = async () => {
        if (!id) return;
        setAnalyzing(true);
        try {
            const result = await dispatch(triggerAnalysis(id)).unwrap();
            // Fetch suggestions after a delay to allow analysis to complete
            setTimeout(() => {
                dispatch(fetchSuggestions(result.analysisId));
                setAnalyzing(false);
            }, 3000);
        } catch (error) {
            setAnalyzing(false);
        }
    };

    if (!selectedPR) {
        return (
            <MainLayout>
                <div className="text-center py-12">
                    <p className="text-gray-600 dark:text-gray-300">Loading...</p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Back Button */}
                <Link to="/pull-requests">
                    <Button variant="ghost" icon={<ArrowLeft className="h-4 w-4" />}>
                        Back to Pull Requests
                    </Button>
                </Link>

                {/* PR Header */}
                <Card>
                    <CardBody>
                        <div className="space-y-4">
                            <div className="flex items-start justify-between">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-2">
                                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                                            {selectedPR.title}
                                        </h1>
                                        <Badge variant={selectedPR.status === 'open' ? 'success' : 'default'}>
                                            {selectedPR.status}
                                        </Badge>
                                    </div>
                                    <p className="text-gray-600 dark:text-gray-400">
                                        #{selectedPR.prNumber} • {selectedPR.repositoryFullName}
                                    </p>
                                </div>

                                <Button
                                    variant="primary"
                                    icon={<Play className="h-4 w-4" />}
                                    onClick={handleTriggerAnalysis}
                                    isLoading={analyzing}
                                >
                                    {analyzing ? 'Analyzing...' : 'Run Analysis'}
                                </Button>
                            </div>

                            {/* Metadata */}
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                                <div className="flex items-center gap-2 text-sm">
                                    <User className="h-4 w-4 text-gray-400" />
                                    <span className="text-gray-600 dark:text-gray-400">
                                        {selectedPR.author}
                                    </span>
                                </div>
                                <div className="flex items-center gap-2 text-sm">
                                    <GitBranch className="h-4 w-4 text-gray-400" />
                                    <span className="text-gray-600 dark:text-gray-400">
                                        {selectedPR.baseBranch} ← {selectedPR.headBranch}
                                    </span>
                                </div>
                                <div className="text-sm">
                                    <span className="text-gray-600 dark:text-gray-400">
                                        {selectedPR.filesChanged} files changed
                                    </span>
                                </div>
                                <div className="text-sm">
                                    <span className="text-green-600 dark:text-green-400">
                                        +{selectedPR.additions}
                                    </span>
                                    {' / '}
                                    <span className="text-red-600 dark:text-red-400">
                                        -{selectedPR.deletions}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </CardBody>
                </Card>

                {/* Tabs */}
                <div className="border-b border-gray-200 dark:border-gray-700">
                    <nav className="flex gap-6">
                        <button
                            onClick={() => setActiveTab('overview')}
                            className={`pb-3 px-1 border-b-2 font-medium text-sm transition-colors ${activeTab === 'overview'
                                ? 'border-indigo-600 text-indigo-600 dark:border-indigo-400 dark:text-indigo-400'
                                : 'border-transparent text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
                                }`}
                        >
                            Overview
                        </button>
                        <button
                            onClick={() => setActiveTab('suggestions')}
                            className={`pb-3 px-1 border-b-2 font-medium text-sm transition-colors ${activeTab === 'suggestions'
                                ? 'border-indigo-600 text-indigo-600 dark:border-indigo-400 dark:text-indigo-400'
                                : 'border-transparent text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
                                }`}
                        >
                            Suggestions ({suggestions.length})
                        </button>
                    </nav>
                </div>

                {/* Tab Content */}
                {activeTab === 'overview' && (
                    <Card>
                        <CardHeader>
                            <CardTitle>Description</CardTitle>
                        </CardHeader>
                        <CardBody>
                            <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
                                {selectedPR.description || 'No description provided'}
                            </p>
                        </CardBody>
                    </Card>
                )}

                {activeTab === 'suggestions' && (
                    <SuggestionList suggestions={suggestions} />
                )}
            </div>
        </MainLayout>
    );
}
