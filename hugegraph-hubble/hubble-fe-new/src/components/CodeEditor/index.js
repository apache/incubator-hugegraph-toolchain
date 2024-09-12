import {autocompletion} from '@codemirror/autocomplete';
import {syntaxHighlighting, HighlightStyle} from '@codemirror/language';
import {basicSetup, EditorView} from 'codemirror';
import React, {useRef, useEffect} from 'react';
import {placeholder as cmplaceholder} from '@codemirror/view';
import syntaxConfig from './syntax';
import {tags} from '@lezer/highlight';

const CodeEditor = ({value, placeholder, onChange, lang = 'gremlin'}) => {

    const editor = useRef();
    const cm = useRef();

    useEffect(() => {
        const syntax = syntaxConfig[lang] ?? syntaxConfig.default;

        const myCompletions = context => {
            let before = context.matchBefore(/\w+/);
            if (!context.explicit && !before) {
                return null;
            }

            return {
                from: before ? before.from : context.pos,
                options: syntax.hint,
                validFor: /^\w*$/,
            };
        };

        const myHighlightStyle = HighlightStyle.define([
            {tag: tags.keyword, color: '#fc6eee'},
            {tag: tags.function, color: '#ff0'},
        ]);

        cm.current = new EditorView({
            extensions: [
                basicSetup,
                autocompletion({override: [myCompletions]}),
                syntaxHighlighting(myHighlightStyle),
                EditorView.updateListener.of(e => {
                    onChange && onChange(e.state.doc.toString());
                }),
                EditorView.theme(
                    {
                        '&': {
                            color: '#000',
                        },
                        '&.cm-focused': {
                            outline: '0',
                        },
                        '.cm-activeLine': {
                            'background-color': 'transparent',
                        },
                    }
                ),
                cmplaceholder(placeholder ?? '请输入查询语句'),
            ],
            parent: editor.current,
        });

        // onChange && EditorView.updateListener.of(e => onChange(e.state.doc.toString()));

        return () => {
            cm.current.destroy();
        };
    }, [lang, onChange, placeholder]);

    useEffect(() => {
        if (value !== null && cm.current.state.doc && value !== cm.current.state.doc.toString()) {
            cm.current.dispatch({
                changes: {from: 0, to: cm.current.state.doc.length, insert: value},
            });
        }
    }, [value]);

    return (
        <div ref={editor} />
    );
};

export default CodeEditor;
