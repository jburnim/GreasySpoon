editAreaLoader.load_syntax["java"] = {
'COMMENT_SINGLE': { 1: '//', 2: '@' }
	, 'COMMENT_MULTI': { '/*': '*/' }
	, 'QUOTEMARKS': { 1: "'", 2: '"' }
	, 'KEYWORD_CASE_SENSITIVE': true
	, 'KEYWORDS': {
	    'constants': [
			'null', 'false', 'true'
		]
		, 'types': [
			'String', 'StringBuilder', 'int', 'short', 'long', 'char', 'double', 'byte',
			'float', 'static', 'void', 'private', 'boolean', 'protected',
			'public', 'const', 'class', 'final', 'abstract', 'volatile',
			'enum', 'transient', 'interface'
		]
		, 'statements': [
            'this', 'extends', 'if', 'do', 'while', 'try', 'catch', 'finally',
            'throw', 'throws', 'else', 'for', 'switch', 'continue', 'implements',
            'break', 'case', 'default', 'goto'
		]
 		, 'keywords': [
           'new', 'return', 'import', 'native', 'super', 'package', 'assert', 'synchronized',
           'instanceof', 'strictfp',
           'HttpMessage', 'httpMessage'
		]
        ,'functions' : [
			// common functions for Window object
			'toString', 'indexOf', 'setResponseBody', 'getRequestBody', 'getRequestHeaders','setRequestHeaders', 'getUsername', 'setUsername', 'getUsergroup', 'setUsergroup', 'getSharedCache', 'getResponseHeaders', 'setResponseHeaders', 'getResponseBody', 'setResponseBody'
		]
	}
	, 'OPERATORS': [
		'+', '-', '/', '*', '=', '<', '>', '%', '!', '?', ':', '&'
	]
	, 'DELIMITERS': [
		'(', ')', '[', ']', '{', '}'
	]
	, 'REGEXPS': {
	    'precompiler': {
	        'search': '()(#[^\r\n]*)()'
			, 'class': 'precompiler'
			, 'modifiers': 'g'
			, 'execute': 'before'
	    }
	}
	, 'STYLES': {
	    'COMMENTS': 'color: #AAAAAA;'
		, 'QUOTESMARKS': 'color: #6381F8;'
		, 'KEYWORDS': {
		    'constants': 'color: #EE0000;'
			, 'types': 'color: #0000EE;'
			, 'statements': 'color: #60CA00;'
			, 'keywords': 'color: #48BDDF;'
		}
		, 'OPERATORS': 'color: #FF00FF;'
		, 'DELIMITERS': 'color: #0038E1;'
		, 'REGEXPS': {
		    'precompiler': 'color: #009900;'
			, 'precompilerstring': 'color: #994400;'
		}
	}
    ,'AUTO_COMPLETION' :  {
		"default": {	// the name of this definition group. It's posisble to have different rules inside the same definition file
			"REGEXP": { "before_word": "[^a-zA-Z0-9_]|^"	// \\s|\\.|
						,"possible_words_letters": "[a-zA-Z0-9_]+"
						,"letter_after_word_must_match": "[^a-zA-Z0-9_]|$"
						,"prefix_separator": "\\."
					}
			,"CASE_SENSITIVE": true
			,"MAX_TEXT_LENGTH": 100		// the maximum length of the text being analyzed before the cursor position
			,"KEYWORDS": {
				'': [	// the prefix of thoses items
						/**
						 * 0 : the keyword the user is typing
						 * 1 : (optionnal) the string inserted in code ("{@}" being the new position of the cursor, "�" beeing the equivalent to the value the typed string indicated if the previous )
						 * 		If empty the keyword will be displayed
						 * 2 : (optionnal) the text that appear in the suggestion box (if empty, the string to insert will be displayed)
						 */
			    		['httpMessage', 'httpMessage', 'httpMessage: HTTP Request or Response Object'],
                        ['debug', 'debug({@});', 'debug(String message): store message in service log']
			    	]
		    	,'httpMessage' : [
			    		['getRequestHeaders', 'getRequestHeaders()', 'getRequestHeaders()', '(String) return raw HTTP request header']
                        ,['getRequestHeader', 'getRequestHeader({@})', 'getRequestHeader(String headerName)', '(String) return value of request header field, or null if missing']
                        ,['getResponseHeaders', 'getResponseHeaders()', 'getResponseHeaders()', '(String) return raw HTTP response header']
                        ,['getResponseHeader', 'getResponseHeader({@})', 'getResponseHeader(String headerName)', '(String) return value of response header field, or null if missing']
                        
			    		,['setHeaders', 'setHeaders({@})', 'setHeaders(String/StringBuilder rawHttpHeader)', '(void) replace HTTP header by the provided string']
                        ,['addHeader', 'addHeader({@},{@})', 'addHeader(String name,String value)', '(void) add given header to HTTP message']
                        ,['rewriteHeader', 'rewriteHeader({@},{@})', 'rewriteHeader(String name,String value)', '(void) rewrite given header with provided value']
                        ,['deleteHeader', 'deleteHeader({@})', 'deleteHeader(String headerName)', '(void) delete given header from HTTP message']
                        
                        ,['getUrl', 'getUrl()', 'getUrl()', '(String) returns requested URL']
                        ,['setUrl', 'setUrl({@})', 'setUrl(String newURL)', '(void) replace requested URL by newURL']
                        
                        ,['getType', 'getType()', 'getType()', '(String) return either Request Method in REQMOD, or Status code in RESPMOD']
                        ,['getSharedCache', 'getSharedCache()', 'getSharedCache()', '(HashMap<String><Object>) return hash table shared accross scripts']
						
                        ,['getBody', 'getBody()', 'getBody()', '(String) returns HTTP message body']
                        ,['setBody', 'setBody({@})', 'setBody(String newBody)', '(void) replace HTTP Body by the given content']
                        
                       	,['getUsername', 'getUsername()', 'getUsername()', '(String) returns User Name as identified by the proxy, or user IP address if unavailable']
                        ,['getUsergroup', 'getUsergroup()', 'getUsergroup()', '(String) returns User Group as identified by the proxy, or User-Agent if unavailable']
                        
                        ,['minify', 'minify()', 'minify()', '(void) experimental: compress CSS, JS and HTML content']
                        ,['toJson', 'toJson()', 'toJson()', '(void) experimental: convert Message Body (if XML) to JSON']
                        ,['getUnderlyingBytes', 'getUnderlyingBytes()', 'getUnderlyingBytes()', '(byte[]): returns original body as raw byte array']
					]
			}
		}
	}
};
