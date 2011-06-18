var PlsqlParser = Editor.Parser = (function() {

  function wordRegexp(words) {
    return new RegExp("^(?:" + words.join("|") + ")$", "i");
  }

  var functions = wordRegexp([
"abs","acos","add_months","ascii","asin","atan","atan2","average",
"bfilename",
"ceil","chartorowid","chr","concat","convert","cos","cosh","count",
"decode","deref","dual","dump","dup_val_on_index",
"empty","error","exp",
"false","floor","found",
"glb","greatest",
"hextoraw",
"initcap","instr","instrb","isopen",
"last_day","least","lenght","lenghtb","ln","lower","lpad","ltrim","lub",
"make_ref","max","min","mod","months_between",
"new_time","next_day","nextval","nls_charset_decl_len","nls_charset_id","nls_charset_name","nls_initcap","nls_lower",
"nls_sort","nls_upper","nlssort","no_data_found","notfound","null","nvl",
"others",
"power",
"rawtohex","reftohex","round","rowcount","rowidtochar","rpad","rtrim",
"sign","sin","sinh","soundex","sqlcode","sqlerrm","sqrt","stddev","substr","substrb","sum","sysdate",
"tan","tanh","to_char","to_date","to_label","to_multi_byte","to_number","to_single_byte","translate","true","trunc",
"uid","upper","user","userenv",
"variance","vsize"

  ]);

  var keywords = wordRegexp([
"abort","accept","access","add","all","alter","and","any","array","arraylen","as","asc","assert","assign","at","attributes","audit",
"authorization","avg",
"base_table","begin","between","binary_integer","body","boolean","by",
"case","cast","char","char_base","check","close","cluster","clusters","colauth","column","comment","commit","compress","connect",
"connected","constant","constraint","crash","create","current","currval","cursor",
"data_base","database","date","dba","deallocate","debugoff","debugon","decimal","declare","default","definition","delay","delete",
"desc","digits","dispose","distinct","do","drop",
"else","elsif","enable","end","entry","escape","exception","exception_init","exchange","exclusive","exists","exit","external",
"fast","fetch","file","for","force","form","from","function",
"generic","goto","grant","group",
"having",
"identified","if","immediate","in","increment","index","indexes","indicator","initial","initrans","insert","interface","intersect",
"into","is",
"key",
"level","library","like","limited","local","lock","log","logging","long","loop",
"master","maxextents","maxtrans","member","minextents","minus","mislabel","mode","modify","multiset",
"new","next","no","noaudit","nocompress","nologging","noparallel","not","nowait","number_base",
"object","of","off","offline","on","online","only","open","option","or","order","out",
"package","parallel","partition","pctfree","pctincrease","pctused","pls_integer","positive","positiven","pragma","primary","prior",
"private","privileges","procedure","public",
"raise","range","raw","read","rebuild","record","ref","references","refresh","release","rename","replace","resource","restrict","return",
"returning","reverse","revoke","rollback","row","rowid","rowlabel","rownum","rows","run",
"savepoint","schema","segment","select","separate","session","set","share","snapshot","some","space","split","sql","start","statement",
"storage","subtype","successful","synonym",
"tabauth","table","tables","tablespace","task","terminate","then","to","trigger","truncate","type",
"union","unique","unlimited","unrecoverable","unusable","update","use","using",
"validate","value","values","variable","view","views",
"when","whenever","where","while","with","work"
  ]);

  var types = wordRegexp([
"bfile","blob",
"character","clob",
"dec",
"float",
"int","integer",
"mlslabel",
"natural","naturaln","nchar","nclob","number","numeric","nvarchar2",
"real","rowtype",
"signtype","smallint","string",
"varchar","varchar2"
  ]);

  var operators = wordRegexp([
    ":=", "<", "<=", "==", "!=", "<>", ">", ">=", "like", "rlike", "in", "xor", "between"
  ]);

  var operatorChars = /[*+\-<>=&|:\/]/;

  var tokenizeSql = (function() {
    function normal(source, setState) {
      var ch = source.next();
      if (ch == "@" || ch == "$") {
        source.nextWhileMatches(/[\w\d]/);
        return "plsql-var";
      }
      else if (ch == "\"" || ch == "'" || ch == "`") {
        setState(inLiteral(ch));
        return null;
      }
      else if (ch == "," || ch == ";") {
        return "plsql-separator"
      }
      else if (ch == '-') {
        if (source.peek() == "-") {
          while (!source.endOfLine()) source.next();
          return "plsql-comment";
        }
        else if (/\d/.test(source.peek())) {
          source.nextWhileMatches(/\d/);
          if (source.peek() == '.') {
            source.next();
            source.nextWhileMatches(/\d/);
          }
          return "plsql-number";
        }
        else
          return "plsql-operator";
      }
      else if (operatorChars.test(ch)) {
        source.nextWhileMatches(operatorChars);
        return "plsql-operator";
      }
      else if (/\d/.test(ch)) {
        source.nextWhileMatches(/\d/);
        if (source.peek() == '.') {
          source.next();
          source.nextWhileMatches(/\d/);
        }
        return "plsql-number";
      }
      else if (/[()]/.test(ch)) {
        return "plsql-punctuation";
      }
      else {
        source.nextWhileMatches(/[_\w\d]/);
        var word = source.get(), type;
        if (operators.test(word))
          type = "plsql-operator";
        else if (keywords.test(word))
          type = "plsql-keyword";
        else if (functions.test(word))
          type = "plsql-function";
        else if (types.test(word))
          type = "plsql-type";
        else
          type = "plsql-word";
        return {style: type, content: word};
      }
    }

    function inLiteral(quote) {
      return function(source, setState) {
        var escaped = false;
        while (!source.endOfLine()) {
          var ch = source.next();
          if (ch == quote && !escaped) {
            setState(normal);
            break;
          }
          escaped = !escaped && ch == "\\";
        }
        return quote == "`" ? "plsql-word" : "plsql-literal";
      };
    }

    return function(source, startState) {
      return tokenizer(source, startState || normal);
    };
  })();

  function indentSql(context) {
    return function(nextChars) {
      var firstChar = nextChars && nextChars.charAt(0);
      var closing = context && firstChar == context.type;
      if (!context)
        return 0;
      else if (context.align)
        return context.col - (closing ? context.width : 0);
      else
        return context.indent + (closing ? 0 : indentUnit);
    }
  }

  function parseSql(source) {
    var tokens = tokenizeSql(source);
    var context = null, indent = 0, col = 0;
    function pushContext(type, width, align) {
      context = {prev: context, indent: indent, col: col, type: type, width: width, align: align};
    }
    function popContext() {
      context = context.prev;
    }

    var iter = {
      next: function() {
        var token = tokens.next();
        var type = token.style, content = token.content, width = token.value.length;

        if (content == "\n") {
          token.indentation = indentSql(context);
          indent = col = 0;
          if (context && context.align == null) context.align = false;
        }
        else if (type == "whitespace" && col == 0) {
          indent = width;
        }
        else if (!context && type != "plsql-comment") {
          pushContext(";", 0, false);
        }

        if (content != "\n") col += width;

        if (type == "plsql-punctuation") {
          if (content == "(")
            pushContext(")", width);
          else if (content == ")")
            popContext();
        }
        else if (type == "plsql-separator" && content == ";" && context && !context.prev) {
          popContext();
        }

        return token;
      },

      copy: function() {
        var _context = context, _indent = indent, _col = col, _tokenState = tokens.state;
        return function(source) {
          tokens = tokenizeSql(source, _tokenState);
          context = _context;
          indent = _indent;
          col = _col;
          return iter;
        };
      }
    };
    return iter;
  }

  return {make: parseSql, electricChars: ")"};
})();
