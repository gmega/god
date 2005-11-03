package ddproto1.configurator.newimpl;
import ddproto1.configurator.newimpl.ObjectSpecFactory.SpecToken;

%%

%class StringObjectSpecLexer
%type SpecToken
%public
%unicode
%line
%column

%state ATTRIBUTE_LIST

LIST_SEPARATOR_CHAR=";"
NODE_SEPARATOR_CHAR="-"

NUMBER=[0-9]+

TYPE_TUPLE_START="("
TYPE_TUPLE_END=")"
ATTRIBUTE_LIST_BEGIN="<"
ATTRIBUTE_LIST_END=">"
ATTRIBUTE_SPLITTER=":"

ATTRIBUTE_TUPLE=~{ATTRIBUTE_SPLITTER}~{LIST_SEPARATOR_CHAR}
TYPE_TUPLE={TYPE_TUPLE_START}~{LIST_SEPARATOR_CHAR}~{TYPE_TUPLE_END}
CHILD_LIST=({NUMBER}{NODE_SEPARATOR_CHAR})+

%%

<YYINITIAL> {
	{ATTRIBUTE_LIST_BEGIN}								{yybegin(ATTRIBUTE_LIST);}
	{ATTRIBUTE_LIST_BEGIN}{ATTRIBUTE_LIST_END}          {return new SpecToken(SpecToken.UNKNOWN, yytext()); }
	{TYPE_TUPLE}										{return new SpecToken(SpecToken.TYPE_TUPLE, yytext());}
	{CHILD_LIST}										{return new SpecToken(SpecToken.CHILD_LIST, yytext());}
	{NUMBER}/{LIST_SEPARATOR_CHAR}						{return new SpecToken(SpecToken.NUMBER, yytext());}
	{LIST_SEPARATOR_CHAR}           					{ }
	.           										{return new SpecToken(SpecToken.UNKNOWN, yytext()); }
}

<ATTRIBUTE_LIST>{
	{ATTRIBUTE_TUPLE}						{return new SpecToken(SpecToken.ATTRIBUTE_TUPLE, yytext().substring(0, yytext().length()-1));}
	{ATTRIBUTE_TUPLE}/{ATTRIBUTE_LIST_END}	{yybegin(YYINITIAL); return new SpecToken(SpecToken.ATTRIBUTE_TUPLE, yytext().substring(0, yytext().length()-1)); }
	.           							{return new SpecToken(SpecToken.UNKNOWN, yytext()); }
}



