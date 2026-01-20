import { Github, Code2, Zap, Shield } from 'lucide-react';
import Button from '@/components/ui/Button';
import { authService } from '@/services/auth';

export default function Login() {
    const handleLogin = () => {
        authService.initiateGitHubLogin();
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-indigo-900">
            <div className="container mx-auto px-4 py-16">
                {/* Header */}
                <div className="text-center mb-16">
                    <div className="flex items-center justify-center mb-4">
                        <Code2 className="h-12 w-12 text-indigo-600 dark:text-indigo-400" />
                    </div>
                    <h1 className="text-5xl font-bold text-gray-900 dark:text-white mb-4">
                        CodeSage AI
                    </h1>
                    <p className="text-xl text-gray-600 dark:text-gray-300">
                        AI-Powered Code Review Assistant
                    </p>
                </div>

                {/* Login Card */}
                <div className="max-w-md mx-auto">
                    <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8 border border-gray-200 dark:border-gray-700">
                        <h2 className="text-2xl font-semibold text-center mb-6 text-gray-900 dark:text-white">
                            Get Started
                        </h2>

                        <Button
                            variant="primary"
                            size="lg"
                            className="w-full"
                            icon={<Github className="h-5 w-5" />}
                            onClick={handleLogin}
                        >
                            Continue with GitHub
                        </Button>

                        <p className="text-sm text-gray-500 dark:text-gray-400 text-center mt-4">
                            Sign in with your GitHub account to start analyzing pull requests
                        </p>
                    </div>
                </div>

                {/* Features */}
                <div className="max-w-4xl mx-auto mt-16 grid md:grid-cols-3 gap-8">
                    <FeatureCard
                        icon={<Zap className="h-8 w-8" />}
                        title="Real-time Analysis"
                        description="Get instant AI-powered suggestions as you create pull requests"
                    />
                    <FeatureCard
                        icon={<Code2 className="h-8 w-8" />}
                        title="Context-Aware"
                        description="Learns from your repository's patterns and coding style"
                    />
                    <FeatureCard
                        icon={<Shield className="h-8 w-8" />}
                        title="Secure & Private"
                        description="Self-hosted solution with zero-cost deployment"
                    />
                </div>
            </div>
        </div>
    );
}

interface FeatureCardProps {
    icon: React.ReactNode;
    title: string;
    description: string;
}

function FeatureCard({ icon, title, description }: FeatureCardProps) {
    return (
        <div className="text-center">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 mb-4">
                {icon}
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                {title}
            </h3>
            <p className="text-gray-600 dark:text-gray-300">
                {description}
            </p>
        </div>
    );
}
