import { cn } from '@/utils/cn';
import { HTMLAttributes, forwardRef } from 'react';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
    variant?: 'default' | 'success' | 'warning' | 'error' | 'info' | 'critical' | 'moderate' | 'minor';
}

const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
    ({ className, variant = 'default', children, ...props }, ref) => {
        const variants = {
            default: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
            success: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
            warning: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400',
            error: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
            info: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
            critical: 'bg-red-100 text-red-800 ring-1 ring-red-600/20 dark:bg-red-900/30 dark:text-red-400',
            moderate: 'bg-amber-100 text-amber-800 ring-1 ring-amber-600/20 dark:bg-amber-900/30 dark:text-amber-400',
            minor: 'bg-gray-100 text-gray-800 ring-1 ring-gray-600/20 dark:bg-gray-700 dark:text-gray-300',
        };

        return (
            <span
                ref={ref}
                className={cn(
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    variants[variant],
                    className
                )}
                {...props}
            >
                {children}
            </span>
        );
    }
);

Badge.displayName = 'Badge';

export default Badge;
