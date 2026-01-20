import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { handleOAuthCallback } from '@/store/slices/authSlice';

export default function Callback() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const { loading, error } = useAppSelector((state) => state.auth);

    useEffect(() => {
        const processCallback = async () => {
            try {
                await dispatch(handleOAuthCallback()).unwrap();
                // Success - redirect to dashboard
                navigate('/dashboard');
            } catch (err) {
                // Error - redirect to login
                console.error('OAuth callback failed:', err);
                setTimeout(() => navigate('/login'), 2000);
            }
        };

        processCallback();
    }, [dispatch, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
            <div className="text-center">
                {loading && (
                    <>
                        <Loader2 className="h-12 w-12 animate-spin text-indigo-600 mx-auto mb-4" />
                        <p className="text-gray-600 dark:text-gray-300">
                            Completing authentication...
                        </p>
                    </>
                )}

                {error && (
                    <div className="text-red-600 dark:text-red-400">
                        <p className="font-semibold mb-2">Authentication Failed</p>
                        <p className="text-sm">{error}</p>
                        <p className="text-sm mt-2">Redirecting to login...</p>
                    </div>
                )}
            </div>
        </div>
    );
}
