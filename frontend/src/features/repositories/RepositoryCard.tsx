import { useState } from 'react';
import { Settings, Trash2, GitBranch } from 'lucide-react';
import { Card, CardBody } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Badge from '@/components/ui/Badge';
import { Repository } from '@/types/repository';
import { useAppDispatch } from '@/store/hooks';
import { deleteRepository, updateRepository } from '@/store/slices/repositorySlice';

interface RepositoryCardProps {
    repository: Repository;
}

export default function RepositoryCard({ repository }: RepositoryCardProps) {
    const dispatch = useAppDispatch();
    const [isDeleting, setIsDeleting] = useState(false);

    const handleToggleActive = async () => {
        await dispatch(updateRepository({
            id: repository.id,
            data: { isActive: !repository.isActive }
        }));
    };

    const handleDelete = async () => {
        if (window.confirm(`Are you sure you want to delete ${repository.fullName}?`)) {
            setIsDeleting(true);
            await dispatch(deleteRepository(repository.id));
        }
    };

    return (
        <Card hover>
            <CardBody>
                <div className="space-y-4">
                    {/* Header */}
                    <div className="flex items-start justify-between">
                        <div className="flex-1 min-w-0">
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white truncate">
                                {repository.fullName}
                            </h3>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1 line-clamp-2">
                                {repository.description || 'No description'}
                            </p>
                        </div>
                        <Badge variant={repository.isActive ? 'success' : 'default'}>
                            {repository.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                    </div>

                    {/* Metadata */}
                    <div className="flex items-center gap-4 text-sm text-gray-600 dark:text-gray-400">
                        <div className="flex items-center gap-1">
                            <div className="w-3 h-3 rounded-full bg-indigo-500" />
                            <span>{repository.language}</span>
                        </div>
                        <div className="flex items-center gap-1">
                            <GitBranch className="h-4 w-4" />
                            <span>{repository.webhookId ? 'Webhook Active' : 'No Webhook'}</span>
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-2 pt-2 border-t border-gray-200 dark:border-gray-700">
                        <Button
                            variant={repository.isActive ? 'outline' : 'primary'}
                            size="sm"
                            onClick={handleToggleActive}
                            className="flex-1"
                        >
                            {repository.isActive ? 'Disable' : 'Enable'}
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            icon={<Settings className="h-4 w-4" />}
                            title="Settings"
                        />
                        <Button
                            variant="ghost"
                            size="sm"
                            icon={<Trash2 className="h-4 w-4" />}
                            onClick={handleDelete}
                            isLoading={isDeleting}
                            title="Delete"
                        />
                    </div>
                </div>
            </CardBody>
        </Card>
    );
}
