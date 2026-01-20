import { useState } from 'react';
import { Filter } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardBody } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { Suggestion } from '@/types/pullRequest';
import SuggestionCard from './SuggestionCard';

interface SuggestionListProps {
    suggestions: Suggestion[];
}

export default function SuggestionList({ suggestions }: SuggestionListProps) {
    const [filterCategory, setFilterCategory] = useState<string>('');
    const [filterSeverity, setFilterSeverity] = useState<string>('');

    const filteredSuggestions = suggestions.filter((suggestion) => {
        if (filterCategory && suggestion.category !== filterCategory) return false;
        if (filterSeverity && suggestion.severity !== filterSeverity) return false;
        return true;
    });

    const categories = Array.from(new Set(suggestions.map(s => s.category)));
    const severities = ['critical', 'moderate', 'minor'];

    return (
        <div className="space-y-6">
            {/* Filters */}
            <Card>
                <CardBody>
                    <div className="flex flex-wrap gap-4">
                        <div className="flex-1 min-w-[200px]">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                Category
                            </label>
                            <select
                                value={filterCategory}
                                onChange={(e) => setFilterCategory(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            >
                                <option value="">All Categories</option>
                                {categories.map((cat) => (
                                    <option key={cat} value={cat}>{cat}</option>
                                ))}
                            </select>
                        </div>

                        <div className="flex-1 min-w-[200px]">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                Severity
                            </label>
                            <select
                                value={filterSeverity}
                                onChange={(e) => setFilterSeverity(e.target.value)}
                                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                            >
                                <option value="">All Severities</option>
                                {severities.map((sev) => (
                                    <option key={sev} value={sev}>{sev}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </CardBody>
            </Card>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card>
                    <CardBody>
                        <div className="text-center">
                            <p className="text-3xl font-bold text-gray-900 dark:text-white">
                                {filteredSuggestions.length}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">Total</p>
                        </div>
                    </CardBody>
                </Card>
                <Card>
                    <CardBody>
                        <div className="text-center">
                            <p className="text-3xl font-bold text-red-600">
                                {filteredSuggestions.filter(s => s.severity === 'critical').length}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">Critical</p>
                        </div>
                    </CardBody>
                </Card>
                <Card>
                    <CardBody>
                        <div className="text-center">
                            <p className="text-3xl font-bold text-amber-600">
                                {filteredSuggestions.filter(s => s.severity === 'moderate').length}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">Moderate</p>
                        </div>
                    </CardBody>
                </Card>
                <Card>
                    <CardBody>
                        <div className="text-center">
                            <p className="text-3xl font-bold text-gray-600">
                                {filteredSuggestions.filter(s => s.severity === 'minor').length}
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">Minor</p>
                        </div>
                    </CardBody>
                </Card>
            </div>

            {/* Suggestion Cards */}
            <div className="space-y-4">
                {filteredSuggestions.length === 0 ? (
                    <Card>
                        <CardBody>
                            <div className="text-center py-12">
                                <p className="text-gray-600 dark:text-gray-300">
                                    {suggestions.length === 0
                                        ? 'No suggestions yet. Run an analysis to get started.'
                                        : 'No suggestions match the selected filters.'}
                                </p>
                            </div>
                        </CardBody>
                    </Card>
                ) : (
                    filteredSuggestions.map((suggestion) => (
                        <SuggestionCard key={suggestion.id} suggestion={suggestion} />
                    ))
                )}
            </div>
        </div>
    );
}
