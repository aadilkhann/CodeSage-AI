import { useEffect, useState } from 'react';
import { GitPullRequest, Filter, ChevronDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import MainLayout from '@/components/layout/MainLayout';
import { Card, CardBody } from '@/components/ui/Card';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchPullRequests } from '@/store/slices/pullRequestSlice';
import { fetchRepositories } from '@/store/slices/repositorySlice';

export default function PRList() {
    const dispatch = useAppDispatch();
    const { pullRequests, loading } = useAppSelector((state) => state.pullRequests);
    const { repositories } = useAppSelector((state) => state.repositories);

    const [selectedRepo, setSelectedRepo] = useState<string>('');
    const [selectedStatus, setSelectedStatus] = useState<string>('');

    useEffect(() => {
        dispatch(fetchRepositories());
        dispatch(fetchPullRequests({}));
    }, [dispatch]);

    const handleFilter = () => {
        dispatch(fetchPullRequests({
            repositoryId: selectedRepo || undefined,
            status: selectedStatus || undefined,
        }));
    };

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Pull Requests</h1>
                    <p className="text-gray-600 dark:text-gray-300 mt-2">
                        View and analyze pull requests across your repositories
                    </p>
                </div>

                {/* Filters */}
                <Card>
                    <CardBody>
                        <div className="flex flex-wrap gap-4">
                            <div className="flex-1 min-w-[200px]">
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                    Repository
                                </label>
                                <select
                                    value={selectedRepo}
                                    onChange={(e) => setSelectedRepo(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                >
                                    <option value="">All Repositories</option>
                                    {repositories.map((repo) => (
                                        <option key={repo.id} value={repo.id}>
                                            {repo.fullName}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex-1 min-w-[200px]">
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                    Status
                                </label>
                                <select
                                    value={selectedStatus}
                                    onChange={(e) => setSelectedStatus(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                >
                                    <option value="">All Statuses</option>
                                    <option value="open">Open</option>
                                    <option value="closed">Closed</option>
                                    <option value="merged">Merged</option>
                                </select>
                            </div>

                            <div className="flex items-end">
                                <Button
                                    variant="primary"
                                    icon={<Filter className="h-4 w-4" />}
                                    onClick={handleFilter}
                                >
                                    Apply Filters
                                </Button>
                            </div>
                        </div>
                    </CardBody>
                </Card>

                {/* PR List */}
                <div className="space-y-4">
                    {loading && pullRequests.length === 0 ? (
                        <Card>
                            <CardBody>
                                <div className="text-center py-12">
                                    <p className="text-gray-600 dark:text-gray-300">Loading pull requests...</p>
                                </div>
                            </CardBody>
                        </Card>
                    ) : pullRequests.length === 0 ? (
                        <Card>
                            <CardBody>
                                <div className="text-center py-12">
                                    <GitPullRequest className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                                    <p className="text-gray-600 dark:text-gray-300">No pull requests found</p>
                                </div>
                            </CardBody>
                        </Card>
                    ) : (
                        pullRequests.map((pr) => (
                            <Link key={pr.id} to={`/pull-requests/${pr.id}`}>
                                <Card hover>
                                    <CardBody>
                                        <div className="flex items-start justify-between">
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-3 mb-2">
                                                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                                                        {pr.title}
                                                    </h3>
                                                    <Badge variant={pr.status === 'open' ? 'success' : 'default'}>
                                                        {pr.status}
                                                    </Badge>
                                                </div>

                                                <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
                                                    {pr.repositoryFullName} #{pr.prNumber} • by {pr.author}
                                                </p>

                                                <div className="flex items-center gap-4 text-sm text-gray-600 dark:text-gray-400">
                                                    <span>{pr.filesChanged} files changed</span>
                                                    <span className="text-green-600 dark:text-green-400">
                                                        +{pr.additions}
                                                    </span>
                                                    <span className="text-red-600 dark:text-red-400">
                                                        -{pr.deletions}
                                                    </span>
                                                    <span>{pr.baseBranch} ← {pr.headBranch}</span>
                                                </div>
                                            </div>

                                            <ChevronDown className="h-5 w-5 text-gray-400 transform -rotate-90" />
                                        </div>
                                    </CardBody>
                                </Card>
                            </Link>
                        ))
                    )}
                </div>
            </div>
        </MainLayout>
    );
}
