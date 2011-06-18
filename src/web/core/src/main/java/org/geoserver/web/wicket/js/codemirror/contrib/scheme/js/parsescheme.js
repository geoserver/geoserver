var SchemeParser = Editor.Parser = (function() {


    // isLparen: char -> boolean
    var isLparen = function(ch) {
	return ch === '(' || ch === '[' || ch === '{';
    };

    // isRparen: char -> boolean
    var isRparen = function(ch) {
	return ch === ')' || ch === ']' || ch === '}';
    };

    // isMatchingParens: char char -> boolean
    var isMatchingParens = function(lparen, rparen) {
	return ((lparen === '(' && rparen === ')') ||
		(lparen === '[' && rparen === ']') ||
		(lparen === '{' && rparen === '}'));
    };


    // Compute the indentation context enclosing the end of the token
    // sequence tokens.
    // The context is the token sequence of the enclosing s-expression,
    // augmented with column information.
    var getIndentationContext = function(tokenStack) {
	var EMPTY_CONTEXT = [];

	var pendingParens = [], i = 0, j, line, column, context;
	var tokens = [];

	// Scan for the start of the indentation context, accumulating tokens.
	while (! isEmptyPair(tokenStack)) {
	    i++;
	    tokens.push(pairFirst(tokenStack));
	    if (isLparen(pairFirst(tokenStack).type)) {
		if (pendingParens.length === 0) {
		    break;
		} else {
		    if (isMatchingParens(pairFirst(tokenStack).value,
					 pendingParens[pendingParens.length - 1])) {
			pendingParens.pop();
		    } else {
			// Error condition: we see mismatching parens,
			// so we exit with no known indentation context.
			return EMPTY_CONTEXT;
		    }
		}
	    } else if (isRparen(pairFirst(tokenStack).type))  {
		pendingParens.push(pairFirst(tokenStack).type);
	    }
	    tokenStack = pairRest(tokenStack);
	}

	// If we scanned backward too far, we couldn't find a context.  Just
	// return the empty context.
	if (isEmptyPair(tokenStack)) { 
	    return EMPTY_CONTEXT; 
	}

	// Position tokenStack to the next token beyond.
	tokenStack = pairRest(tokenStack);

	// We now scan backwards to closest newline to figure out the column
	// number:
	while (! isEmptyPair(tokenStack)) {
	    if(pairFirst(tokenStack).type === 'whitespace' && 
	       pairFirst(tokenStack).value === '\n') {
		break;
	    }
	    tokens.push(pairFirst(tokenStack));
	    tokenStack = pairRest(tokenStack);
	}

	line = 0;
	column = 0;
	context = [];
	// Start generating the context, walking forward.
	for (j = tokens.length-1; j >= 0; j--) {
	    if (j < i) {
		context.push({ type: tokens[j].type,
			       value: tokens[j].value,
			       line: line,
			       column: column });
	    }

	    if (tokens[j].type === 'whitespace' && 
		tokens[j].value === '\n') {
		column = 0;
		line++;
	    } else {
		column += tokens[j].value.length;
	    }
	}
	return context;


    };





    // calculateIndentationFromContext: indentation-context number -> number
    var calculateIndentationFromContext = function(context, currentIndentation) {
	if (context.length === 0) {
	    return 0;
	}
 	if (isBeginLikeContext(context)) {
	    return beginLikeIndentation(context);
 	}
	if (isDefineLikeContext(context)) {
	    return defineLikeIndentation(context);
	}
  	if (isLambdaLikeContext(context)) {
	    return lambdaLikeIndentation(context);
  	}
	return beginLikeIndentation(context, 0);
    };



    // findContextElement: indentation-context number -> index or -1
    var findContextElement = function(context, index) {
	var depth = 0;
	for(var i = 0; i < context.length; i++) {
	    if (context[i].type !== 'whitespace' && depth === 1) {
		if (index === 0)
		    return i;
		else
		    index--;
	    }

	    if (isLparen(context[i].type)) {
		depth++;
	    }
	    if (isRparen(context[i].type)) {
		depth = Math.max(depth - 1, 0);
	    }
	}
	return -1;
    };

    // contextElement: context -> (arrayof index)
    var contextElements = function(context) {
	var i = 0, index, results = [];
	
	while ((index = findContextElement(context, i++)) != -1) {
	    results.push(index);
	}
	return results;
    };



    //////////////////////////////////////////////////////////////////////

    var BEGIN_LIKE_KEYWORDS = ["case-lambda", 
			       "compound-unit",
			       "compound-unit/sig",
			       "cond",
			       "delay",
			       "inherit",
			       "match-lambda",
			       "match-lambda*",
			       "override",
			       "private",
			       "public",
			       "sequence",
			       "unit"];

    var isBeginLikeContext = function(context) {
	var j = findContextElement(context, 0);
	if (j === -1) { return false; }
	return (/^begin/.test(context[j].value) ||
		isMember(context[j].value, BEGIN_LIKE_KEYWORDS));
    };


    // Begin: if there's no elements within the begin context,
    // the indentation is that of the begin keyword's column + offset.
    // Otherwise, find the leading element on the last line.
    // Also used for default indentation.
    var beginLikeIndentation = function(context, offset) {
	if (typeof(offset) === 'undefined') { offset = 1; }

	var indices = contextElements(context), j;
	if (indices.length === 0) {
	    return context[0].column + 1;
	} else if (indices.length === 1) {
	    // if we only see the begin keyword, indentation is based
	    // off the keyword.
	    return context[indices[0]].column + offset;
	} else {
	    // Otherwise, we scan for the contextElement of the last line
	    for (j = indices.length -1; j > 1; j--) {
		if (context[indices[j]].line !==
		    context[indices[j-1]].line) {
		    return context[indices[j]].column;
		}
	    }
	    return context[indices[j]].column;
	}
    };



    //////////////////////////////////////////////////////////////////////


    var DEFINE_LIKE_KEYWORDS = ["local"];

    var isDefineLikeContext = function(context) {
	var j = findContextElement(context, 0);
	if (j === -1) { return false; }
	return (/^def/.test(context[j].value) ||
		isMember(context[j].value, DEFINE_LIKE_KEYWORDS));
    };


    var defineLikeIndentation = function(context) {
	var i = findContextElement(context, 0);
	if (i === -1) { return 0; }
	return context[i].column +1; 
    };

    //////////////////////////////////////////////////////////////////////

    var LAMBDA_LIKE_KEYWORDS = ["cases",
				"instantiate",
				"super-instantiate",
				"syntax/loc",
				"quasisyntax/loc",
				"lambda",
				"let",
				"let*",
				"letrec",
				"recur",
				"lambda/kw",
				"letrec-values",
				"with-syntax",
				"with-continuation-mark",
				"module",
				"match",
				"match-let",
				"match-let*",
				"match-letrec",
				"let/cc",
				"let/ec",
				"letcc",
				"catch",
				"let-syntax",
				"letrec-syntax",
				"fluid-let-syntax",
				"letrec-syntaxes+values",
				"for",
				"for/list",
				"for/hash",
				"for/hasheq",
				"for/and",
				"for/or",
				"for/lists",
				"for/first",
				"for/last",
				"for/fold",
				"for*",
				"for*/list",
				"for*/hash",
				"for*/hasheq",
				"for*/and",
				"for*/or",
				"for*/lists",
				"for*/first",
				"for*/last",
				"for*/fold",
				"kernel-syntax-case",
				"syntax-case",
				"syntax-case*",
				"syntax-rules",
				"syntax-id-rules",
				"let-signature",
				"fluid-let",
				"let-struct",
				"let-macro",
				"let-values",
				"let*-values",
				"case",
				"when",
				"unless",
				"let-enumerate",
				"class",
				"class*",
				"class-asi",
				"class-asi*",
				"class*/names",
				"class100",
				"class100*",
				"class100-asi",
				"class100-asi*",
				"class100*/names",
				"rec",
				"make-object",
				"mixin",
				"define-some",
				"do",
				"opt-lambda",
				"send*",
				"with-method",
				"define-record",
				"catch",
				"shared",
				"unit/sig",
				"unit/lang",
				"with-handlers",
				"interface",
				"parameterize",
				"call-with-input-file",
				"call-with-input-file*",
				"with-input-from-file",
				"with-input-from-port",
				"call-with-output-file",
				"with-output-to-file",
				"with-output-to-port",
				"for-all"];


    var isLambdaLikeContext = function(context) {
	var j = findContextElement(context, 0);
	if (j === -1) { return false; }
	return (isMember(context[j].value, LAMBDA_LIKE_KEYWORDS));
    };


    var lambdaLikeIndentation = function(context) {
	var i = findContextElement(context, 0);
	if (i === -1) { return 0; }
	var j = findContextElement(context, 1);
	if (j === -1) { 
	    return context[i].column + 4; 
	} else {
	    return context[i].column + 1;
	}
    };




    //////////////////////////////////////////////////////////////////////
    // Helpers
    var isMember = function(x, l) {
	for (var i = 0; i < l.length; i++) {
	    if (x === l[i]) { return true; }
	}
	return false;
    };



    //////////////////////////////////////////////////////////////////////

    var pair = function(x, y) {
	return [x,y];
    };
    var EMPTY_PAIR = [];
    var pairFirst = function(p) { return p[0]; }
    var pairRest = function(p) { return p[1]; }
    var isEmptyPair = function(p) { return p === EMPTY_PAIR; }
    var pairLength = function(p) {
	var l = 0;
	while (! isEmptyPair(p)) {
	    p = pairRest(p);
	}
	return l;
    };

    //////////////////////////////////////////////////////////////////////




    var indentTo = function(tokenStack) {
	return function(tokenText, currentIndentation, direction) {

	    // If we're in the middle of an unclosed token,
	    // do not change indentation.
	    if ((! isEmptyPair(tokenStack)) &&
		(! isEmptyPair(pairRest(tokenStack))) &&
		(pairFirst(pairRest(tokenStack)).isUnclosed)) {
		return currentIndentation;
	    }

	    var indentationContext = 
		getIndentationContext(tokenStack);
	    return calculateIndentationFromContext(indentationContext,
						   currentIndentation);		
	};
    };


    var startParse = function(source) {
	source = tokenizeScheme(source);	
	var tokenStack = EMPTY_PAIR;
	var iter = {
	    next: function() {
		var tok = source.next();
		tokenStack = pair(tok, tokenStack);
		if (tok.type === "whitespace") {
		    if (tok.value === "\n") {
			tok.indentation = indentTo(tokenStack);
		    }
		}
		return tok;
	    },

	    copy: function() {
		var _tokenStack = tokenStack;
		var _tokenState = source.state;
		return function(_source) {
		    tokenStack = _tokenStack;
		    source = tokenizeScheme(_source, _tokenState);
		    return iter;
		};
	    }
	};
	return iter;
    };
    return { make: startParse };
})();
