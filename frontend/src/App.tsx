import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store/store';
import Login from './features/auth/Login';
import Callback from './features/auth/Callback';
import Dashboard from './features/dashboard/Dashboard';
import RepositoryList from './features/repositories/RepositoryList';
import PRList from './features/pullrequests/PRList';
import PRDetail from './features/pullrequests/PRDetail';
import ProtectedRoute from './components/common/ProtectedRoute';

function App() {
    return (
        <Provider store={store}>
            <BrowserRouter>
                <Routes>
                    {/* Public routes */}
                    <Route path="/login" element={<Login />} />
                    <Route path="/auth/callback" element={<Callback />} />

                    {/* Protected routes */}
                    <Route
                        path="/dashboard"
                        element={
                            <ProtectedRoute>
                                <Dashboard />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/repositories"
                        element={
                            <ProtectedRoute>
                                <RepositoryList />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/pull-requests"
                        element={
                            <ProtectedRoute>
                                <PRList />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/pull-requests/:id"
                        element={
                            <ProtectedRoute>
                                <PRDetail />
                            </ProtectedRoute>
                        }
                    />

                    {/* Default redirect */}
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </Provider>
    );
}

export default App;
