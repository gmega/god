package ddproto1.lexer;

/**
 * This class takes care of transforming certain patterns into more
 * diggestable integers.
 */
 
%%

%class Lexer
%type Token
%public
%unicode
%line
%column

%{ 
	private Token makeToken(int tokentype){
		return new Token(tokentype);
	}
	
	private Token makeToken(int tokentype, String tokentext){
		return new Token(tokentype, tokentext);
	}
%}

endl=\n|\r|\r\n
blank={endl}|[ \t\f]
machine_id="<"([:jletterdigit:]|" ")+">"

ident = [:jletter:][:jletterdigit:]*
packagename={ident}|(({ident}".")+)
classname={ident}|(({ident}"$")+)

fullname={packagename}?{classname}
maybearrayclassname = {fullname}"[]"?

paramlist=(({maybearrayclassname}",")+{maybearrayclassname})|{maybearrayclassname}
method={fullname}"."{ident}"("({paramlist}|"")")"
number=[0-9]+
dotted_id={number}"."{number}

hex_id=[0-9]+"x"[0-9a-zA-Z]+

%%

/* Our console will exibit gdb-like syntax */

<YYINITIAL> {
	"info"				{return makeToken(Token.INFO);}
	
	"launch"            {return makeToken(Token.LAUNCH);}
	"resume" 			{return makeToken(Token.RESUME);}
	"suspend"			{return makeToken(Token.SUSPEND); }
	"attach"            {return makeToken(Token.ATTACH);}
	"run"				{return makeToken(Token.RUN);}
	"all"				{return makeToken(Token.ALL);}
	"kill"				{return makeToken(Token.KILL);}
	"select"			{return makeToken(Token.SELECT);}
	"thread"			{return makeToken(Token.THREAD, yytext());}
	"break"				{return makeToken(Token.BREAKPOINT);}
	"show" 				{return makeToken(Token.SHOW, yytext());}
	"ls" 				{return makeToken(Token.LIST, yytext());}
	"exit" 				{return makeToken(Token.EXIT, yytext());}
	"step" 				{return makeToken(Token.STEP, yytext());}
	"script"			{return makeToken(Token.RUNSCRIPT, yytext());}


	{method}":"{number}		{return makeToken(Token.METHOD_BREAKSPEC, yytext());}	
	{fullname}":"{number}	{return makeToken(Token.CLASS_BREAKSPEC, yytext());}
	{machine_id}			{return makeToken(Token.MACHINE_ID, yytext());}
	{hex_id}				{return makeToken(Token.HEX_ID, yytext());}
	{dotted_id}		    	{return makeToken(Token.DOTTED_ID, yytext()); }
	{number}				{return makeToken(Token.NUMBER, yytext());}

/*
	{packagename}		{System.err.println("packagename");}	
	{fullname}			{System.err.println("fullname");}
	{classname}			{System.err.println("classname");}
	{paramlist}			{System.err.println("paramlist");}
	{method}			{System.err.println("method");}
*/	
	[a-zA-Z]+			{return makeToken(Token.WORD, yytext());}
	
	
	{blank} 			{ }
}

	. 					{return makeToken(Token.UNIDENTIFIED, yytext());} 