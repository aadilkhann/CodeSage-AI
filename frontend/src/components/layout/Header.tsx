import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Code2, LogOut } from 'lucide-react';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { logout } from '@/store/slices/authSlice';
import Button from '../ui/Button';

export default function Header() {
    const location = useLocation();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { user } = useAppSelector((state) => state.auth);

    const handleLogout = () => {
        dispatch(logout());
        navigate('/login');
    };

    const isActive = (path: string) => location.pathname === path;

    return (
        <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
            <div className="container mx-auto px-4">
                <div className="flex items-center justify-between h-16">
                    {/* Logo */}
                    <Link to="/dashboard" className="flex items-center gap-2">
                        <Code2 className="h-8 w-8 text-indigo-600 dark:text-indigo-400" />
                        <span className="text-xl font-bold text-gray-900 dark:text-white">
                            CodeSage AI
                        </span>
                    </Link>

                    {/* Navigation */}
                    <nav className="hidden md:flex items-center gap-6">
                        <NavLink to="/dashboard" active={isActive('/dashboard')}>
                            Dashboard
                        </NavLink>
                        <NavLink to="/repositories" active={isActive('/repositories')}>
                            Repositories
                        </NavLink>
                        <NavLink to="/pull-requests" active={isActive('/pull-requests')}>
                            Pull Requests
                        </NavLink>
                    </nav>

                    {/* User Menu */}
                    <div className="flex items-center gap-4">
                        {user && (
                            <div className="flex items-center gap-3">
                                <div className="hidden sm:block text-right">
                                    <p className="text-sm font-medium text-gray-900 dark:text-white">
                                        {user.username}
                                    </p>
                                    <p className="text-xs text-gray-500 dark:text-gray-400">
                                        {user.email}
                                    </p>
                                </div>
                                <img
                                    src={user.avatarUrl}
                                    alt={user.username}
                                    className="h-10 w-10 rounded-full ring-2 ring-indigo-500"
                                />
                            </div>
                        )}

                        <Button
                            variant="ghost"
                            size="sm"
                            icon={<LogOut className="h-4 w-4" />}
                            onClick={handleLogout}
                            title="Logout"
                        />
                    </div>
                </div>
            </div>
        </header>
    );
}

interface NavLinkProps {
    to: string;
    active: boolean;
    children: React.ReactNode;
}

function NavLink({ to, active, children }: NavLinkProps) {
    return (
        <Link
            to={to}
            className={`text-sm font-medium transition-colors hover:text-indigo-600 dark:hover:text-indigo-400 ${active
                ? 'text-indigo-600 dark:text-indigo-400'
                : 'text-gray-600 dark:text-gray-300'
                }`}
        >
            {children}
        </Link>
    );
}
