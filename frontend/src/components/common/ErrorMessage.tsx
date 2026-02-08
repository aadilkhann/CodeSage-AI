import { AlertCircle, X } from 'lucide-react';
import Button from '../ui/Button';

interface ErrorMessageProps {
    title?: string;
    message: string;
    onRetry?: () => void;
    onDismiss?: () => void;
}

export default function ErrorMessage({
    title = 'Error',
    message,
    onRetry,
    onDismiss,
}: ErrorMessageProps) {
    return (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <div className="flex items-start gap-3">
                <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
                <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-semibold text-red-800 dark:text-red-300">
                        {title}
                    </h3>
                    <p className="text-sm text-red-700 dark:text-red-400 mt-1">
                        {message}
                    </p>
                    {(onRetry || onDismiss) && (
                        <div className="flex gap-2 mt-3">
                            {onRetry && (
                                <Button variant="outline" size="sm" onClick={onRetry}>
                                    Try Again
                                </Button>
                            )}
                            {onDismiss && (
                                <Button variant="ghost" size="sm" onClick={onDismiss}>
                                    Dismiss
                                </Button>
                            )}
                        </div>
                    )}
                </div>
                {onDismiss && (
                    <button
                        onClick={onDismiss}
                        className="text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300"
                    >
                        <X className="h-5 w-5" />
                    </button>
                )}
            </div>
        </div>
    );
}
