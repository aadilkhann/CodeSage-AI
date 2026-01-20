import { Activity, FolderGit, GitPullRequest, CheckCircle } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { Card, CardHeader, CardTitle, CardBody } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { Link } from 'react-router-dom';

export default function Dashboard() {
    return (
        <MainLayout>
            <div className="space-y-8">
                {/* Page Header */}
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
                    <p className="text-gray-600 dark:text-gray-300 mt-2">
                        Welcome to CodeSage AI - Your intelligent code review assistant
                    </p>
                </div>

                {/* Stats Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatCard
                        icon={<FolderGit className="h-6 w-6" />}
                        title="Repositories"
                        value="0"
                        description="Connected repositories"
                        color="text-blue-600"
                    />
                    <StatCard
                        icon={<GitPullRequest className="h-6 w-6" />}
                        title="Active PRs"
                        value="0"
                        description="Pull requests analyzed"
                        color="text-purple-600"
                    />
                    <StatCard
                        icon={<Activity className="h-6 w-6" />}
                        title="Suggestions"
                        value="0"
                        description="Pending review"
                        color="text-amber-600"
                    />
                    <StatCard
                        icon={<CheckCircle className="h-6 w-6" />}
                        title="Acceptance Rate"
                        value="0%"
                        description="Suggestions accepted"
                        color="text-green-600"
                    />
                </div>

                {/* Quick Actions */}
                <Card>
                    <CardHeader>
                        <CardTitle>Quick Actions</CardTitle>
                    </CardHeader>
                    <CardBody>
                        <div className="flex flex-wrap gap-4">
                            <Link to="/repositories">
                                <Button variant="primary">
                                    Add Repository
                                </Button>
                            </Link>
                            <Link to="/pull-requests">
                                <Button variant="outline">
                                    View Pull Requests
                                </Button>
                            </Link>
                        </div>
                    </CardBody>
                </Card>

                {/* Getting Started */}
                <Card>
                    <CardHeader>
                        <CardTitle>Getting Started</CardTitle>
                    </CardHeader>
                    <CardBody>
                        <div className="space-y-4">
                            <Step
                                number={1}
                                title="Connect a repository"
                                description="Add a GitHub repository to start analyzing pull requests"
                                completed={false}
                            />
                            <Step
                                number={2}
                                title="Create or open a pull request"
                                description="CodeSage AI will automatically analyze new PRs"
                                completed={false}
                            />
                            <Step
                                number={3}
                                title="Review suggestions"
                                description="Accept, reject, or provide feedback on AI suggestions"
                                completed={false}
                            />
                        </div>
                    </CardBody>
                </Card>
            </div>
        </MainLayout>
    );
}

interface StatCardProps {
    icon: React.ReactNode;
    title: string;
    value: string;
    description: string;
    color: string;
}

function StatCard({ icon, title, value, description, color }: StatCardProps) {
    return (
        <Card>
            <CardBody>
                <div className="flex items-start justify-between">
                    <div>
                        <p className="text-sm text-gray-600 dark:text-gray-400">{title}</p>
                        <p className="text-3xl font-bold text-gray-900 dark:text-white mt-2">{value}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{description}</p>
                    </div>
                    <div className={`${color}`}>
                        {icon}
                    </div>
                </div>
            </CardBody>
        </Card>
    );
}

interface StepProps {
    number: number;
    title: string;
    description: string;
    completed: boolean;
}

function Step({ number, title, description, completed }: StepProps) {
    return (
        <div className="flex items-start gap-4">
            <div
                className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold ${completed
                        ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                        : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
                    }`}
            >
                {completed ? '✓' : number}
            </div>
            <div>
                <h4 className="font-medium text-gray-900 dark:text-white">{title}</h4>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{description}</p>
            </div>
        </div>
    );
}
