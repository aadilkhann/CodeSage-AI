import { useState } from 'react';
import { Check, X, FileCode, TrendingUp } from 'lucide-react';
import { Card, CardBody } from '@/components/ui/Card';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import { CodeViewer } from '@/components/code/CodeDiffViewer';
import { Suggestion } from '@/types/pullRequest';
import { useAppDispatch } from '@/store/hooks';
import { updateSuggestion } from '@/store/slices/pullRequestSlice';

interface SuggestionCardProps {
    suggestion: Suggestion;
}

export default function SuggestionCard({ suggestion }: SuggestionCardProps) {
    const dispatch = useAppDispatch();
    const [feedback, setFeedback] = useState('');
    const [showFeedback, setShowFeedback] = useState(false);

    const handleAccept = async () => {
        await dispatch(updateSuggestion({
            id: suggestion.id,
            data: { status: 'accepted', userFeedback: feedback || undefined }
        }));
        setShowFeedback(false);
        setFeedback('');
    };

    const handleReject = async () => {
        await dispatch(updateSuggestion({
            id: suggestion.id,
            data: { status: 'rejected', userFeedback: feedback || undefined }
        }));
        setShowFeedback(false);
        setFeedback('');
    };

    const getSeverityVariant = () => {
        switch (suggestion.severity) {
            case 'critical': return 'critical';
            case 'moderate': return 'moderate';
            case 'minor': return 'minor';
            default: return 'default';
        }
    };

    const getLanguageFromFileName = (filename: string): string => {
        const ext = filename.split('.').pop()?.toLowerCase();
        const languageMap: Record<string, string> = {
            'js': 'javascript',
            'jsx': 'javascript',
            'ts': 'typescript',
            'tsx': 'typescript',
            'py': 'python',
            'java': 'java',
            'cpp': 'cpp',
            'c': 'c',
            'cs': 'csharp',
            'go': 'go',
            'rs': 'rust',
            'rb': 'ruby',
            'php': 'php',
            'swift': 'swift',
            'kt': 'kotlin',
        };
        return languageMap[ext || ''] || 'plaintext';
    };

    return (
        <Card>
            <CardBody>
                <div className="space-y-4">
                    {/* Header */}
                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                                <Badge variant={getSeverityVariant()}>
                                    {suggestion.severity}
                                </Badge>
                                <Badge variant="default">
                                    {suggestion.category}
                                </Badge>
                                <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
                                    <TrendingUp className="h-4 w-4" />
                                    <span>{suggestion.confidenceScore}% confidence</span>
                                </div>
                            </div>
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                                {suggestion.message}
                            </h3>
                        </div>
                    </div>

                    {/* File Location */}
                    <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                        <FileCode className="h-4 w-4" />
                        <span className="font-mono">{suggestion.filePath}</span>
                        <span>Line {suggestion.lineNumber}{suggestion.lineEnd ? `-${suggestion.lineEnd}` : ''}</span>
                    </div>

                    {/* Explanation */}
                    <div className="bg-gray-50 dark:bg-gray-900/50 p-4 rounded-lg">
                        <p className="text-gray-700 dark:text-gray-300 text-sm">
                            {suggestion.explanation}
                        </p>
                    </div>

                    {/* Suggested Fix */}
                    {suggestion.suggestedFix && (
                        <div className="space-y-2">
                            <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                                Suggested Fix:
                            </p>
                            <CodeViewer
                                code={suggestion.suggestedFix}
                                language={getLanguageFromFileName(suggestion.filePath)}
                                fileName={suggestion.filePath}
                                highlightLines={
                                    suggestion.lineNumber && suggestion.lineEnd
                                        ? Array.from(
                                            { length: suggestion.lineEnd - suggestion.lineNumber + 1 },
                                            (_, i) => suggestion.lineNumber! + i
                                        )
                                        : suggestion.lineNumber
                                            ? [suggestion.lineNumber]
                                            : []
                                }
                                height="250px"
                            />
                        </div>
                    )}

                    {/* Actions */}
                    {suggestion.status === 'pending' && (
                        <div className="space-y-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                            {showFeedback && (
                                <textarea
                                    value={feedback}
                                    onChange={(e) => setFeedback(e.target.value)}
                                    placeholder="Optional feedback..."
                                    rows={2}
                                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                                />
                            )}

                            <div className="flex gap-2">
                                <Button
                                    variant="primary"
                                    size="sm"
                                    icon={<Check className="h-4 w-4" />}
                                    onClick={() => {
                                        if (showFeedback) {
                                            handleAccept();
                                        } else {
                                            setShowFeedback(true);
                                        }
                                    }}
                                >
                                    Accept
                                </Button>
                                <Button
                                    variant="danger"
                                    size="sm"
                                    icon={<X className="h-4 w-4" />}
                                    onClick={() => {
                                        if (showFeedback) {
                                            handleReject();
                                        } else {
                                            setShowFeedback(true);
                                        }
                                    }}
                                >
                                    Reject
                                </Button>
                                {showFeedback && (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => {
                                            setShowFeedback(false);
                                            setFeedback('');
                                        }}
                                    >
                                        Cancel
                                    </Button>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Status Badge */}
                    {suggestion.status !== 'pending' && (
                        <div className="pt-4 border-t border-gray-200 dark:border-gray-700">
                            <Badge variant={suggestion.status === 'accepted' ? 'success' : 'error'}>
                                {suggestion.status}
                            </Badge>
                            {suggestion.userFeedback && (
                                <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">
                                    Feedback: {suggestion.userFeedback}
                                </p>
                            )}
                        </div>
                    )}
                </div>
            </CardBody>
        </Card>
    );
}
