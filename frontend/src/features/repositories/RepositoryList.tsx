import { useEffect, useState } from 'react';
import { Plus, Search, RefreshCw } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { Card, CardHeader, CardTitle, CardBody } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchRepositories } from '@/store/slices/repositorySlice';
import RepositoryCard from './RepositoryCard';
import AddRepositoryModal from './AddRepositoryModal';

export default function RepositoryList() {
    const dispatch = useAppDispatch();
    const { repositories, loading } = useAppSelector((state) => state.repositories);
    const [searchQuery, setSearchQuery] = useState('');
    const [showAddModal, setShowAddModal] = useState(false);

    useEffect(() => {
        dispatch(fetchRepositories());
    }, [dispatch]);

    const filteredRepositories = repositories.filter((repo) =>
        repo.fullName.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const handleRefresh = () => {
        dispatch(fetchRepositories());
    };

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Repositories</h1>
                        <p className="text-gray-600 dark:text-gray-300 mt-2">
                            Manage your connected GitHub repositories
                        </p>
                    </div>
                    <div className="flex gap-3">
                        <Button
                            variant="outline"
                            icon={<RefreshCw className="h-4 w-4" />}
                            onClick={handleRefresh}
                            isLoading={loading}
                        >
                            Refresh
                        </Button>
                        <Button
                            variant="primary"
                            icon={<Plus className="h-4 w-4" />}
                            onClick={() => setShowAddModal(true)}
                        >
                            Add Repository
                        </Button>
                    </div>
                </div>

                {/* Search */}
                <Card>
                    <CardBody>
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                            <input
                                type="text"
                                placeholder="Search repositories..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            />
                        </div>
                    </CardBody>
                </Card>

                {/* Repository Grid */}
                {loading && repositories.length === 0 ? (
                    <div className="text-center py-12">
                        <RefreshCw className="h-12 w-12 animate-spin text-indigo-600 mx-auto mb-4" />
                        <p className="text-gray-600 dark:text-gray-300">Loading repositories...</p>
                    </div>
                ) : filteredRepositories.length === 0 ? (
                    <Card>
                        <CardBody>
                            <div className="text-center py-12">
                                <p className="text-gray-600 dark:text-gray-300 mb-4">
                                    {searchQuery ? 'No repositories match your search' : 'No repositories connected yet'}
                                </p>
                                {!searchQuery && (
                                    <Button
                                        variant="primary"
                                        icon={<Plus className="h-4 w-4" />}
                                        onClick={() => setShowAddModal(true)}
                                    >
                                        Add Your First Repository
                                    </Button>
                                )}
                            </div>
                        </CardBody>
                    </Card>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {filteredRepositories.map((repo) => (
                            <RepositoryCard key={repo.id} repository={repo} />
                        ))}
                    </div>
                )}
            </div>

            {/* Add Repository Modal */}
            {showAddModal && (
                <AddRepositoryModal onClose={() => setShowAddModal(false)} />
            )}
        </MainLayout>
    );
}
