import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store/store';
import Login from './features/auth/Login';
import Callback from './features/auth/Callback';
import Dashboard from './features/dashboard/Dashboard';
import RepositoryList from './features/repositories/RepositoryList';
import PRList from './features/pullrequests/PRList';
import PRDetail from './features/pullrequests/PRDetail';
import ProtectedRoute from './components/common/ProtectedRoute';
import MainLayout from './components/layout/MainLayout';
import ErrorBoundary from './components/common/ErrorBoundary';

function App() {
    return (
        <ErrorBoundary>
            <Provider store={store}>
                <Router>
                    <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route path="/callback" element={<Callback />} />
                        <Route
                            path="/"
                            element={
                                <ProtectedRoute>
                                    <MainLayout>
                                        <Dashboard />
                                    </MainLayout>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/repositories"
                            element={
                                <ProtectedRoute>
                                    <MainLayout>
                                        <RepositoryList />
                                    </MainLayout>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/repositories/:repoId/pulls"
                            element={
                                <ProtectedRoute>
                                    <MainLayout>
                                        <PRList />
                                    </MainLayout>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/repositories/:repoId/pulls/:prId"
                            element={
                                <ProtectedRoute>
                                    <MainLayout>
                                        <PRDetail />
                                    </MainLayout>
                                </ProtectedRoute>
                            }
                        />
                    </Routes>
                </Router>
            </Provider>
        </ErrorBoundary>
    );
}

export default App;
