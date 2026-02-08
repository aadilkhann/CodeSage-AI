import React, { useEffect, useRef, useState } from 'react';
import * as monaco from 'monaco-editor';
import Loader from '../common/Loader';

interface CodeDiffViewerProps {
    originalCode: string;
    modifiedCode: string;
    language: string;
    fileName?: string;
    readOnly?: boolean;
    height?: string;
}

/**
 * Monaco Diff Editor Component
 * 
 * Displays side-by-side code comparison with syntax highlighting
 * Features:
 * - Syntax highlighting for multiple languages
 * - Line-by-line diff visualization
 * - Inline change markers
 * - Responsive layout
 */
export const CodeDiffViewer: React.FC<CodeDiffViewerProps> = ({
    originalCode,
    modifiedCode,
    language = 'plaintext',
    fileName,
    readOnly = true,
    height = '500px',
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const editorRef = useRef<monaco.editor.IStandaloneDiffEditor | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (!containerRef.current) return;

        // Create diff editor
        const diffEditor = monaco.editor.createDiffEditor(containerRef.current, {
            readOnly,
            automaticLayout: true,
            renderSideBySide: true,
            enableSplitViewResizing: true,
            renderIndicators: true,
            originalEditable: false,
            scrollBeyondLastLine: false,
            minimap: { enabled: false },
            fontSize: 14,
            lineNumbers: 'on',
            glyphMargin: false,
            folding: true,
            lineDecorationsWidth: 10,
            lineNumbersMinChars: 3,
            renderWhitespace: 'boundary',
            scrollbar: {
                verticalScrollbarSize: 10,
                horizontalScrollbarSize: 10,
            },
        });

        // Set models
        const originalModel = monaco.editor.createModel(originalCode, language);
        const modifiedModel = monaco.editor.createModel(modifiedCode, language);

        diffEditor.setModel({
            original: originalModel,
            modified: modifiedModel,
        });

        editorRef.current = diffEditor;
        setIsLoading(false);

        // Cleanup
        return () => {
            originalModel.dispose();
            modifiedModel.dispose();
            diffEditor.dispose();
        };
    }, [originalCode, modifiedCode, language, readOnly]);

    // Update content when props change
    useEffect(() => {
        if (!editorRef.current) return;

        const model = editorRef.current.getModel();
        if (model) {
            model.original.setValue(originalCode);
            model.modified.setValue(modifiedCode);
        }
    }, [originalCode, modifiedCode]);

    return (
        <div className="code-diff-viewer">
            {fileName && (
                <div className="bg-gray-100 dark:bg-gray-800 px-4 py-2 border-b border-gray-300 dark:border-gray-700">
                    <span className="font-mono text-sm text-gray-700 dark:text-gray-300">
                        {fileName}
                    </span>
                </div>
            )}

            {isLoading && (
                <div className="flex items-center justify-center" style={{ height }}>
                    <Loader size="md" />
                </div>
            )}

            <div
                ref={containerRef}
                style={{ height }}
                className="border border-gray-300 dark:border-gray-700"
            />
        </div>
    );
};

interface CodeViewerProps {
    code: string;
    language: string;
    fileName?: string;
    highlightLines?: number[];
    readOnly?: boolean;
    height?: string;
}

/**
 * Monaco Code Viewer Component (single file)
 * 
 * Displays a single code file with syntax highlighting
 * Used for viewing suggestions and code snippets
 */
export const CodeViewer: React.FC<CodeViewerProps> = ({
    code,
    language = 'plaintext',
    fileName,
    highlightLines = [],
    readOnly = true,
    height = '400px',
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (!containerRef.current) return;

        // Create code editor
        const codeEditor = monaco.editor.create(containerRef.current, {
            value: code,
            language,
            readOnly,
            automaticLayout: true,
            scrollBeyondLastLine: false,
            minimap: { enabled: false },
            fontSize: 14,
            lineNumbers: 'on',
            glyphMargin: false,
            folding: true,
            renderWhitespace: 'boundary',
            scrollbar: {
                verticalScrollbarSize: 10,
                horizontalScrollbarSize: 10,
            },
        });

        editorRef.current = codeEditor;
        setIsLoading(false);

        // Cleanup
        return () => {
            codeEditor.dispose();
        };
    }, [code, language, readOnly]);

    // Highlight specific lines
    useEffect(() => {
        if (!editorRef.current || highlightLines.length === 0) return;

        const decorations = highlightLines.map(line => ({
            range: new monaco.Range(line, 1, line, 1),
            options: {
                isWholeLine: true,
                className: 'highlighted-line',
                glyphMarginClassName: 'highlighted-line-glyph',
            },
        }));

        editorRef.current.deltaDecorations([], decorations);
    }, [highlightLines]);

    // Update content when code changes
    useEffect(() => {
        if (!editorRef.current) return;
        editorRef.current.setValue(code);
    }, [code]);

    return (
        <div className="code-viewer">
            {fileName && (
                <div className="bg-gray-100 dark:bg-gray-800 px-4 py-2 border-b border-gray-300 dark:border-gray-700">
                    <span className="font-mono text-sm text-gray-700 dark:text-gray-300">
                        {fileName}
                    </span>
                </div>
            )}

            {isLoading && (
                <div className="flex items-center justify-center" style={{ height }}>
                    <Loader size="md" />
                </div>
            )}

            <div
                ref={containerRef}
                style={{ height }}
                className="border border-gray-300 dark:border-gray-700"
            />

            {/* Add custom CSS for highlighting */}
            <style>{`
        .highlighted-line {
          background-color: rgba(255, 217, 0, 0.2);
        }
        .highlighted-line-glyph {
          background-color: rgba(255, 217, 0, 0.6);
          width: 3px !important;
          margin-left: 3px;
        }
      `}</style>
        </div>
    );
};

export default CodeDiffViewer;
