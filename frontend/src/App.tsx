import { BrowserRouter as Router } from 'react-router-dom'

function App() {
    return (
        <Router>
            <div className="min-h-screen bg-gray-50">
                <header className="bg-white shadow">
                    <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
                        <h1 className="text-3xl font-bold text-gray-900">
                            CodeSage AI
                        </h1>
                    </div>
                </header>
                <main>
                    <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                        <div className="px-4 py-6 sm:px-0">
                            <div className="border-4 border-dashed border-gray-200 rounded-lg h-96 flex items-center justify-center">
                                <div className="text-center">
                                    <h2 className="text-2xl font-semibold text-gray-700 mb-4">
                                        Welcome to CodeSage AI
                                    </h2>
                                    <p className="text-gray-500">
                                        AI-powered code review assistant
                                    </p>
                                    <p className="text-sm text-gray-400 mt-2">
                                        Project structure initialized successfully ✅
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </Router>
    )
}

export default App
