/*
Copyright 2010 Mike Brevoort http://mike.brevoort.com (twitter:@mbrevoort)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This is an indirect collective derivative of the other parses in this package

*/

// The XQuery parser uses the xquery tokenizer in tokenizexquery.js. Given the
// stream of tokens, it makes decisions to override the tokens styles by evaluating
// it's context with respect to the other tokens. 

var XqueryParser = Editor.Parser = (function() {
    function xqueryLexical(startColumn, currentToken, align, previousToken, encloseLevel) {
        this.startColumn = startColumn;
        this.currentToken = currentToken;
        if (align != null)
        this.align = align;
        this.previousToken = previousToken;
        this.encloseLevel = encloseLevel;
    }

    // Xquery indentation rules.
    function indentXquery(lexical) {
        return function(firstChars, curIndent, direction) {
            // test if this is next row after the open brace
            if (lexical.encloseLevel !== 0 && firstChars === "}") {
                return lexical.startColumn - indentUnit;
            }

            return lexical.startColumn;
        };
    }

    function parseXquery(source) {
        var tokens = tokenizeXquery(source);

        var column = 0;
        // tells the first non-whitespace symbol from the
        // start of row.
        var previousToken = null;
        var previousTokens = [];
        //mb
        var align = false;
        // tells if the text after the open brace
        var encloseLevel = 0;
        // tells curent opened braces quantity
        var cc = [statements];
        var consume,
        marked;

        var iter = {
            next: function() {
                var token = tokens.next();

                // since attribute and elements can be named the same, assume the
                // following word of each is a variable
                if (previousToken && (previousToken.content == "attribute" || previousToken.content == "element") && previousToken.type == "xqueryKeywordC") {
                    token.type = "variable";
                    token.style = "xqueryVariable";
                }

                else if (previousToken && previousToken.content == "xquery" && token.content == "version") {
                    //token.type="variable";
                    token.style = "xqueryModifier";
                }

                else if (token.type == "word" && (getPrevious(3).style == "xml-attribute" || previousToken.type == "xml-tag-open") &&
                previousToken.content.substring(previousToken.content.length - 1) != ">") {
                    token.style = "xml-attribute";
                }
                else if (previousToken && previousToken.content == "=" && previousTokens.length > 2
                && getPrevious(2).style == "xml-attribute") {
                    token.style = "xml-attribute-value";
                }
                else if(token.type == "string" && previousToken.type == "}") {
                    // looking for expressions within a string and detecting if the expression is within an attribute
                    var i=0;
                    while(i++ < previousTokens.length-1) {
                        if(getPrevious(i).style == "xml-attribute-value" ) {
                            token.style = "xml-attribute-value";
                            break;
                        } 
                        else if(getPrevious(i).type == "string") {
                            break;
                        }                            
                    }
                }    
                else if(token.type == "string") {
                    // brute force check for strings inside XML TODO... something else
                    var i=0;
                    var closeCount = 0;
                    while(i++ < previousTokens.length-1) {
                        var prev = getPrevious(i);                  
                        if(prev.type == "xml-tag-open") {
                            if(closeCount == 0) {
                                token.style = "word";
                                break;
                            } else {
                                closeCount--;
                            }
                        }    
                        else if(prev.type == "xml-tag-close") {
                            closeCount++;
                        }
                        else if(prev.content == ":=" || prev.content == "return" || prev.content == "{")
                            break;
                    }
                }
                else if(getPrevious(2).content == "module" && getPrevious(1).content == "namespace") 
                    token.style="xqueryFunction";
                else if(token.content == "=" && getPrevious(1).style == "xml-attribute")
                    token.style="xml-attribute"

                if (token.type == "whitespace") {
                    if (token.value == "\n") {
                        // test if this is end of line
                        if (previousToken !== null) {
                            if (previousToken.type === "{") {
                                // test if there is open brace at the end of line
                                align = true;
                                column += indentUnit;
                                encloseLevel++;
                            }
                            else
                            if (previousToken.type === "}") {
                                // test if there is close brace at the end of line
                                align = false;
                                if (encloseLevel > 0) {
                                    encloseLevel--;
                                }
                                else {
                                    encloseLevel = 0;
                                }
                            }
                            var lexical = new xqueryLexical(column, token, align, previousToken, encloseLevel);
                            token.indentation = indentXquery(lexical);
                        }
                    }
                    else
                    column = token.value.length;
                }

                // maintain the previous tokens array so that it doesn't continue to leak
                // keep only the last 5000
                if(previousTokens.length > 5000) previousTokens.shift();

                while (true) {
                    consume = marked = false;
                    // Take and execute the topmost action.
                    cc.pop()(token.type, token.content);
                    if (consume) {
                        // Marked is used to change the style of the current token.
                        if (marked)
                        token.style = marked;
                        // Here we differentiate between local and global variables.
                        previousToken = token;
                        previousTokens[previousTokens.length] = token;
                        return token;
                    }
                }

            },

            copy: function() {
                var _cc = cc.concat([]),
                _tokenState = tokens.state,
                _column = column;

                return function copyParser(_source) {
                    cc = _cc.concat([]);
                    column = indented = _column;
                    tokens = tokenizeXquery(_source, _tokenState);
                    return iter;
                };

            },

        };

        function statements(type) {
            return pass(statement, statements);
        }

        function statement(type) {
            cont();
        }

        function push(fs) {
            for (var i = fs.length - 1; i >= 0; i--)
            cc.push(fs[i]);
        }

        function cont() {
            push(arguments);
            consume = true;
        }

        function pass() {
            push(arguments);
            consume = false;
        }
        

        function getPrevious(numberFromCurrent) {
            var l = previousTokens.length;
            if (l - numberFromCurrent >= 0)
            return previousTokens[l - numberFromCurrent];
            else
            return {
                type: "",
                style: "",
                content: ""
            };
        }

        return iter;


    }
    return {
        make: parseXquery
    };
})();