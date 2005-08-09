/* The following code was generated by JFlex 1.3.5 on 3/21/05 6:31 PM */

package ddproto1.lexer;

/**
 * This class takes care of transforming certain patterns into more
 * diggestable integers.
 */
 

/**
 * This class is a scanner generated by 
 * <a href="http://www.jflex.de/">JFlex</a> 1.3.5
 * on 3/21/05 6:31 PM from the specification file
 * <tt>file:/home/giuliano/workspace/Distributed Debugger Prototype 1/jflexspec/commands.jflex</tt>
 */
public class Lexer {

  /** This character denotes the end of file */
  final public static int YYEOF = -1;

  /** initial size of the lookahead buffer */
  final private static int YY_BUFFERSIZE = 16384;

  /** lexical states */
  final public static int YYINITIAL = 0;

  /** 
   * Translates characters to character classes
   */
  final private static String yycmap_packed = 
    "\11\5\1\3\1\1\1\0\1\3\1\2\16\5\4\0\1\6\3\0"+
    "\1\10\3\0\1\15\1\16\2\0\1\14\1\0\1\11\1\0\12\17"+
    "\1\45\1\0\1\4\1\0\1\7\2\0\32\21\1\12\1\0\1\13"+
    "\1\0\1\10\1\0\1\27\1\43\1\31\1\40\1\34\1\24\1\21"+
    "\1\32\1\22\1\21\1\42\1\26\1\36\1\23\1\25\1\37\1\21"+
    "\1\33\1\35\1\41\1\30\1\21\1\44\1\20\2\21\4\0\41\5"+
    "\2\0\4\10\4\0\1\10\2\0\1\5\7\0\1\10\4\0\1\10"+
    "\5\0\27\10\1\0\37\10\1\0\u013f\10\31\0\162\10\4\0\14\10"+
    "\16\0\5\10\11\0\1\10\21\0\130\5\5\0\23\5\12\0\1\10"+
    "\13\0\1\10\1\0\3\10\1\0\1\10\1\0\24\10\1\0\54\10"+
    "\1\0\46\10\1\0\5\10\4\0\202\10\1\0\4\5\3\0\105\10"+
    "\1\0\46\10\2\0\2\10\6\0\20\10\41\0\46\10\2\0\1\10"+
    "\7\0\47\10\11\0\21\5\1\0\27\5\1\0\3\5\1\0\1\5"+
    "\1\0\2\5\1\0\1\5\13\0\33\10\5\0\3\10\15\0\4\5"+
    "\14\0\6\5\13\0\32\10\5\0\13\10\16\5\7\0\12\5\4\0"+
    "\2\10\1\5\143\10\1\0\1\10\10\5\1\0\6\5\2\10\2\5"+
    "\1\0\4\5\2\10\12\5\3\10\2\0\1\10\17\0\1\5\1\10"+
    "\1\5\36\10\33\5\2\0\3\10\60\0\46\10\13\5\1\10\u014f\0"+
    "\3\5\66\10\2\0\1\5\1\10\20\5\2\0\1\10\4\5\3\0"+
    "\12\10\2\5\2\0\12\5\21\0\3\5\1\0\10\10\2\0\2\10"+
    "\2\0\26\10\1\0\7\10\1\0\1\10\3\0\4\10\2\0\1\5"+
    "\1\10\7\5\2\0\2\5\2\0\3\5\11\0\1\5\4\0\2\10"+
    "\1\0\3\10\2\5\2\0\12\5\4\10\15\0\3\5\1\0\6\10"+
    "\4\0\2\10\2\0\26\10\1\0\7\10\1\0\2\10\1\0\2\10"+
    "\1\0\2\10\2\0\1\5\1\0\5\5\4\0\2\5\2\0\3\5"+
    "\13\0\4\10\1\0\1\10\7\0\14\5\3\10\14\0\3\5\1\0"+
    "\11\10\1\0\3\10\1\0\26\10\1\0\7\10\1\0\2\10\1\0"+
    "\5\10\2\0\1\5\1\10\10\5\1\0\3\5\1\0\3\5\2\0"+
    "\1\10\17\0\2\10\2\5\2\0\12\5\1\0\1\10\17\0\3\5"+
    "\1\0\10\10\2\0\2\10\2\0\26\10\1\0\7\10\1\0\2\10"+
    "\1\0\5\10\2\0\1\5\1\10\6\5\3\0\2\5\2\0\3\5"+
    "\10\0\2\5\4\0\2\10\1\0\3\10\4\0\12\5\1\0\1\10"+
    "\20\0\1\5\1\10\1\0\6\10\3\0\3\10\1\0\4\10\3\0"+
    "\2\10\1\0\1\10\1\0\2\10\3\0\2\10\3\0\3\10\3\0"+
    "\10\10\1\0\3\10\4\0\5\5\3\0\3\5\1\0\4\5\11\0"+
    "\1\5\17\0\11\5\11\0\1\10\7\0\3\5\1\0\10\10\1\0"+
    "\3\10\1\0\27\10\1\0\12\10\1\0\5\10\4\0\7\5\1\0"+
    "\3\5\1\0\4\5\7\0\2\5\11\0\2\10\4\0\12\5\22\0"+
    "\2\5\1\0\10\10\1\0\3\10\1\0\27\10\1\0\12\10\1\0"+
    "\5\10\2\0\1\5\1\10\7\5\1\0\3\5\1\0\4\5\7\0"+
    "\2\5\7\0\1\10\1\0\2\10\4\0\12\5\22\0\2\5\1\0"+
    "\10\10\1\0\3\10\1\0\27\10\1\0\20\10\4\0\6\5\2\0"+
    "\3\5\1\0\4\5\11\0\1\5\10\0\2\10\4\0\12\5\22\0"+
    "\2\5\1\0\22\10\3\0\30\10\1\0\11\10\1\0\1\10\2\0"+
    "\7\10\3\0\1\5\4\0\6\5\1\0\1\5\1\0\10\5\22\0"+
    "\2\5\15\0\60\10\1\5\2\10\7\5\4\0\10\10\10\5\1\0"+
    "\12\5\47\0\2\10\1\0\1\10\2\0\2\10\1\0\1\10\2\0"+
    "\1\10\6\0\4\10\1\0\7\10\1\0\3\10\1\0\1\10\1\0"+
    "\1\10\2\0\2\10\1\0\4\10\1\5\2\10\6\5\1\0\2\5"+
    "\1\10\2\0\5\10\1\0\1\10\1\0\6\5\2\0\12\5\2\0"+
    "\2\10\42\0\1\10\27\0\2\5\6\0\12\5\13\0\1\5\1\0"+
    "\1\5\1\0\1\5\4\0\2\5\10\10\1\0\42\10\6\0\24\5"+
    "\1\0\2\5\4\10\4\0\10\5\1\0\44\5\11\0\1\5\71\0"+
    "\42\10\1\0\5\10\1\0\2\10\1\0\7\5\3\0\4\5\6\0"+
    "\12\5\6\0\6\10\4\5\106\0\46\10\12\0\51\10\7\0\132\10"+
    "\5\0\104\10\5\0\122\10\6\0\7\10\1\0\77\10\1\0\1\10"+
    "\1\0\4\10\2\0\7\10\1\0\1\10\1\0\4\10\2\0\47\10"+
    "\1\0\1\10\1\0\4\10\2\0\37\10\1\0\1\10\1\0\4\10"+
    "\2\0\7\10\1\0\1\10\1\0\4\10\2\0\7\10\1\0\7\10"+
    "\1\0\27\10\1\0\37\10\1\0\1\10\1\0\4\10\2\0\7\10"+
    "\1\0\47\10\1\0\23\10\16\0\11\5\56\0\125\10\14\0\u026c\10"+
    "\2\0\10\10\12\0\32\10\5\0\113\10\3\0\3\10\17\0\15\10"+
    "\1\0\4\10\3\5\13\0\22\10\3\5\13\0\22\10\2\5\14\0"+
    "\15\10\1\0\3\10\1\0\2\5\14\0\64\10\40\5\3\0\1\10"+
    "\3\0\2\10\1\5\2\0\12\5\41\0\3\5\2\0\12\5\6\0"+
    "\130\10\10\0\51\10\1\5\126\0\35\10\3\0\14\5\4\0\14\5"+
    "\12\0\12\5\36\10\2\0\5\10\u038b\0\154\10\224\0\234\10\4\0"+
    "\132\10\6\0\26\10\2\0\6\10\2\0\46\10\2\0\6\10\2\0"+
    "\10\10\1\0\1\10\1\0\1\10\1\0\1\10\1\0\37\10\2\0"+
    "\65\10\1\0\7\10\1\0\1\10\3\0\3\10\1\0\7\10\3\0"+
    "\4\10\2\0\6\10\4\0\15\10\5\0\3\10\1\0\7\10\17\0"+
    "\4\5\32\0\5\5\20\0\2\10\23\0\1\10\13\0\4\5\6\0"+
    "\6\5\1\0\1\10\15\0\1\10\40\0\22\10\36\0\15\5\4\0"+
    "\1\5\3\0\6\5\27\0\1\10\4\0\1\10\2\0\12\10\1\0"+
    "\1\10\3\0\5\10\6\0\1\10\1\0\1\10\1\0\1\10\1\0"+
    "\4\10\1\0\3\10\1\0\7\10\3\0\3\10\5\0\5\10\26\0"+
    "\44\10\u0e81\0\3\10\31\0\11\10\6\5\1\0\5\10\2\0\5\10"+
    "\4\0\126\10\2\0\2\5\2\0\3\10\1\0\137\10\5\0\50\10"+
    "\4\0\136\10\21\0\30\10\70\0\20\10\u0200\0\u19b6\10\112\0\u51a6\10"+
    "\132\0\u048d\10\u0773\0\u2ba4\10\u215c\0\u012e\10\2\0\73\10\225\0\7\10"+
    "\14\0\5\10\5\0\1\10\1\5\12\10\1\0\15\10\1\0\5\10"+
    "\1\0\1\10\1\0\2\10\1\0\2\10\1\0\154\10\41\0\u016b\10"+
    "\22\0\100\10\2\0\66\10\50\0\15\10\3\0\20\5\20\0\4\5"+
    "\17\0\2\10\30\0\3\10\31\0\1\10\6\0\5\10\1\0\207\10"+
    "\2\0\1\5\4\0\1\10\13\0\12\5\7\0\32\10\4\0\1\10"+
    "\1\0\32\10\12\0\132\10\3\0\6\10\2\0\6\10\2\0\6\10"+
    "\2\0\3\10\3\0\2\10\3\0\2\10\22\0\3\5\4\0";

  /** 
   * Translates characters to character classes
   */
  final private static char [] yycmap = yy_unpack_cmap(yycmap_packed);

  /** 
   * Translates a state to a row index in the transition table
   */
  final private static int yy_rowMap [] = { 
        0,    38,    38,    76,   114,   152,   190,   228,   266,   304, 
      342,   380,   418,   456,   494,   532,   570,   608,   152,   646, 
      684,   722,   760,   798,   836,   228,   874,   912,   950,   988, 
     1026,  1064,  1102,  1140,  1178,  1216,  1254,  1292,  1330,    38, 
     1368,   684,   722,   760,  1406,  1444,   228,  1482,   228,  1520, 
     1558,  1596,  1634,  1672,  1710,  1748,  1786,  1824,  1862,  1900, 
      228,  1938,  1976,  2014,   228,  2052,  2090,   228,  2128,   228, 
     2166,   228,  2204,  2242,  2280,  2318,  2356,  2394,  2432,  2470, 
     2508,  2546,   228,  2584,  2622,  2660,   228,   228,   228,  2698, 
      228,   228,   228,  2736,  2660,   228
  };

  /** 
   * The packed transition table of the DFA (part 0)
   */
  final private static String yy_packed0 = 
    "\1\2\1\3\1\4\1\3\1\5\1\2\1\3\1\2"+
    "\1\6\6\2\1\7\2\10\1\11\3\10\1\12\1\13"+
    "\3\10\1\14\1\15\1\16\3\10\1\17\1\20\1\21"+
    "\1\10\1\2\47\0\1\3\51\0\2\22\1\0\1\22"+
    "\6\0\26\22\6\0\1\23\2\0\1\23\1\24\5\0"+
    "\26\23\1\25\11\0\1\26\5\0\1\7\1\27\32\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\25\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\3\10"+
    "\1\30\21\10\1\25\5\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\7\10\1\31\5\10\1\32\7\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\6\10"+
    "\1\33\12\10\1\34\3\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\10\10\1\35\3\10\1\36"+
    "\10\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\1\37\24\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\10\10\1\40\1\41\1\42\1\10"+
    "\1\43\4\10\1\44\3\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\12\10\1\45\12\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\2\10"+
    "\1\46\22\10\1\25\5\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\13\10\1\47\11\10\1\25\5\0\2\22"+
    "\1\50\1\22\6\0\26\22\11\0\1\51\7\0\25\51"+
    "\20\0\1\52\45\0\1\53\45\0\26\54\6\0\1\23"+
    "\2\0\1\23\1\24\5\0\1\23\4\10\1\55\20\10"+
    "\1\25\5\0\1\23\2\0\1\23\1\24\5\0\1\23"+
    "\10\10\1\56\14\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\6\10\1\57\16\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\21\10\1\60"+
    "\3\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\3\10\1\61\21\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\15\10\1\62\7\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\2\10"+
    "\1\63\22\10\1\25\5\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\15\10\1\64\7\10\1\25\5\0\1\23"+
    "\2\0\1\23\1\24\5\0\1\23\13\10\1\65\11\10"+
    "\1\25\5\0\1\23\2\0\1\23\1\24\5\0\1\23"+
    "\5\10\1\66\17\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\6\10\1\67\16\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\14\10\1\70"+
    "\10\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\13\10\1\71\11\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\6\10\1\72\16\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\14\10"+
    "\1\73\10\10\1\25\5\0\1\51\2\0\1\51\1\24"+
    "\3\0\1\74\1\0\26\51\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\5\10\1\75\17\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\3\10"+
    "\1\76\21\10\1\25\5\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\7\10\1\77\15\10\1\25\5\0\1\23"+
    "\2\0\1\23\1\24\5\0\1\23\10\10\1\100\14\10"+
    "\1\25\5\0\1\23\2\0\1\23\1\24\5\0\1\23"+
    "\21\10\1\101\3\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\17\10\1\102\5\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\2\10\1\103"+
    "\22\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\24\10\1\104\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\14\10\1\105\10\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\17\10\1\106"+
    "\5\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\14\10\1\107\10\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\6\10\1\110\16\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\7\10"+
    "\1\111\15\10\1\25\10\0\1\112\5\0\1\113\1\0"+
    "\25\112\6\0\1\23\2\0\1\23\1\24\5\0\1\23"+
    "\11\10\1\114\13\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\11\10\1\115\13\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\16\10\1\116"+
    "\6\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\14\10\1\117\10\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\17\10\1\120\5\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\11\10"+
    "\1\121\13\10\1\25\5\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\7\10\1\122\15\10\1\25\5\0\1\23"+
    "\2\0\1\23\1\24\5\0\1\23\22\10\1\123\2\10"+
    "\1\25\5\0\1\112\2\0\1\112\1\124\1\125\1\0"+
    "\1\124\1\0\1\113\26\112\46\0\1\126\5\0\1\23"+
    "\2\0\1\23\1\24\5\0\1\23\12\10\1\127\12\10"+
    "\1\25\5\0\1\23\2\0\1\23\1\24\5\0\1\23"+
    "\12\10\1\130\12\10\1\25\5\0\1\23\2\0\1\23"+
    "\1\24\5\0\1\23\14\10\1\131\10\10\1\25\5\0"+
    "\1\23\2\0\1\23\1\24\5\0\1\23\3\10\1\132"+
    "\21\10\1\25\5\0\1\23\2\0\1\23\1\24\5\0"+
    "\1\23\21\10\1\133\3\10\1\25\5\0\1\23\2\0"+
    "\1\23\1\24\5\0\1\23\21\10\1\134\3\10\1\25"+
    "\5\0\1\23\2\0\1\23\1\24\5\0\1\23\20\10"+
    "\1\135\4\10\1\25\10\0\1\112\7\0\25\112\14\0"+
    "\1\136\51\0\1\137\33\0\1\23\2\0\1\23\1\24"+
    "\5\0\1\23\20\10\1\140\4\10\1\25\14\0\1\124"+
    "\1\0\1\113\27\0";

  /** 
   * The transition table of the DFA
   */
  final private static int yytrans [] = yy_unpack();


  /* error codes */
  final private static int YY_UNKNOWN_ERROR = 0;
  final private static int YY_NO_MATCH = 2;
  final private static int YY_PUSHBACK_2BIG = 3;

  /* error messages for the codes above */
  final private static String YY_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Internal error: unknown state",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * YY_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private final static byte YY_ATTRIBUTE[] = {
     0,  9,  9,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
     1,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,  1,  1,  1,  1,  1, 
     1,  1,  1,  1,  1,  1,  1,  9,  0,  1,  1,  1,  1,  1,  1,  1, 
     1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  0,  1,  1,  1,  1, 
     1,  1,  1,  1,  1,  1,  1,  1,  1,  0,  0,  1,  1,  1,  1,  1, 
     1,  1,  1,  0,  0,  0,  1,  1,  1,  1,  1,  1,  1,  0,  1,  1
  };

  /** the input device */
  private java.io.Reader yy_reader;

  /** the current state of the DFA */
  private int yy_state;

  /** the current lexical state */
  private int yy_lexical_state = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private char yy_buffer[] = new char[YY_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int yy_markedPos;

  /** the textposition at the last state to be included in yytext */
  private int yy_pushbackPos;

  /** the current text position in the buffer */
  private int yy_currentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int yy_startRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int yy_endRead;

  /** number of newlines encountered up to the start of the matched text */
  private int yyline;

  /** the number of characters up to the start of the matched text */

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  private int yycolumn; 

  /** 
   * yy_atBOL == true <=> the scanner is currently at the beginning of a line
   */

  /** yy_atEOF == true <=> the scanner is at the EOF */
  private boolean yy_atEOF;

  /* user code: */
	private Token makeToken(int tokentype){
		return new Token(tokentype);
	}
	
	private Token makeToken(int tokentype, String tokentext){
		return new Token(tokentype, tokentext);
	}


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public Lexer(java.io.Reader in) {
    this.yy_reader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public Lexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /** 
   * Unpacks the split, compressed DFA transition table.
   *
   * @return the unpacked transition table
   */
  private static int [] yy_unpack() {
    int [] trans = new int[2774];
    int offset = 0;
    offset = yy_unpack(yy_packed0, offset, trans);
    return trans;
  }

  /** 
   * Unpacks the compressed DFA transition table.
   *
   * @param packed   the packed transition table
   * @return         the index of the last entry
   */
  private static int yy_unpack(String packed, int offset, int [] trans) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do trans[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** 
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] yy_unpack_cmap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 1738) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   * 
   * @exception   IOException  if any I/O-Error occurs
   */
  private boolean yy_refill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (yy_startRead > 0) {
      System.arraycopy(yy_buffer, yy_startRead, 
                       yy_buffer, 0, 
                       yy_endRead-yy_startRead);

      /* translate stored positions */
      yy_endRead-= yy_startRead;
      yy_currentPos-= yy_startRead;
      yy_markedPos-= yy_startRead;
      yy_pushbackPos-= yy_startRead;
      yy_startRead = 0;
    }

    /* is the buffer big enough? */
    if (yy_currentPos >= yy_buffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[yy_currentPos*2];
      System.arraycopy(yy_buffer, 0, newBuffer, 0, yy_buffer.length);
      yy_buffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = yy_reader.read(yy_buffer, yy_endRead, 
                                            yy_buffer.length-yy_endRead);

    if (numRead < 0) {
      return true;
    }
    else {
      yy_endRead+= numRead;  
      return false;
    }
  }


  /**
   * Closes the input stream.
   */
  final public void yyclose() throws java.io.IOException {
    yy_atEOF = true;            /* indicate end of file */
    yy_endRead = yy_startRead;  /* invalidate buffer    */

    if (yy_reader != null)
      yy_reader.close();
  }


  /**
   * Closes the current stream, and resets the
   * scanner to read from a new input stream.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>YY_INITIAL</tt>.
   *
   * @param reader   the new input stream 
   */
  final public void yyreset(java.io.Reader reader) throws java.io.IOException {
    yyclose();
    yy_reader = reader;
    yy_atEOF  = false;
    yy_endRead = yy_startRead = 0;
    yy_currentPos = yy_markedPos = yy_pushbackPos = 0;
    yyline = yycolumn = 0;
    yy_lexical_state = YYINITIAL;
  }


  /**
   * Returns the current lexical state.
   */
  final public int yystate() {
    return yy_lexical_state;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  final public void yybegin(int newState) {
    yy_lexical_state = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  final public String yytext() {
    return new String( yy_buffer, yy_startRead, yy_markedPos-yy_startRead );
  }


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  final public char yycharat(int pos) {
    return yy_buffer[yy_startRead+pos];
  }


  /**
   * Returns the length of the matched text region.
   */
  final public int yylength() {
    return yy_markedPos-yy_startRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void yy_ScanError(int errorCode) {
    String message;
    try {
      message = YY_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = YY_ERROR_MSG[YY_UNKNOWN_ERROR];
    }

    throw new Error(message);
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  private void yypushback(int number)  {
    if ( number > yylength() )
      yy_ScanError(YY_PUSHBACK_2BIG);

    yy_markedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   IOException  if any I/O-Error occurs
   */
  public Token yylex() throws java.io.IOException {
    int yy_input;
    int yy_action;

    // cached fields:
    int yy_currentPos_l;
    int yy_startRead_l;
    int yy_markedPos_l;
    int yy_endRead_l = yy_endRead;
    char [] yy_buffer_l = yy_buffer;
    char [] yycmap_l = yycmap;

    int [] yytrans_l = yytrans;
    int [] yy_rowMap_l = yy_rowMap;
    byte [] yy_attr_l = YY_ATTRIBUTE;

    while (true) {
      yy_markedPos_l = yy_markedPos;

      boolean yy_r = false;
      for (yy_currentPos_l = yy_startRead; yy_currentPos_l < yy_markedPos_l;
                                                             yy_currentPos_l++) {
        switch (yy_buffer_l[yy_currentPos_l]) {
        case '\u000B':
        case '\u000C':
        case '\u0085':
        case '\u2028':
        case '\u2029':
          yyline++;
          yycolumn = 0;
          yy_r = false;
          break;
        case '\r':
          yyline++;
          yycolumn = 0;
          yy_r = true;
          break;
        case '\n':
          if (yy_r)
            yy_r = false;
          else {
            yyline++;
            yycolumn = 0;
          }
          break;
        default:
          yy_r = false;
          yycolumn++;
        }
      }

      if (yy_r) {
        // peek one character ahead if it is \n (if we have counted one line too much)
        boolean yy_peek;
        if (yy_markedPos_l < yy_endRead_l)
          yy_peek = yy_buffer_l[yy_markedPos_l] == '\n';
        else if (yy_atEOF)
          yy_peek = false;
        else {
          boolean eof = yy_refill();
          yy_markedPos_l = yy_markedPos;
          yy_buffer_l = yy_buffer;
          if (eof) 
            yy_peek = false;
          else 
            yy_peek = yy_buffer_l[yy_markedPos_l] == '\n';
        }
        if (yy_peek) yyline--;
      }
      yy_action = -1;

      yy_startRead_l = yy_currentPos_l = yy_currentPos = 
                       yy_startRead = yy_markedPos_l;

      yy_state = yy_lexical_state;


      yy_forAction: {
        while (true) {

          if (yy_currentPos_l < yy_endRead_l)
            yy_input = yy_buffer_l[yy_currentPos_l++];
          else if (yy_atEOF) {
            yy_input = YYEOF;
            break yy_forAction;
          }
          else {
            // store back cached positions
            yy_currentPos  = yy_currentPos_l;
            yy_markedPos   = yy_markedPos_l;
            boolean eof = yy_refill();
            // get translated positions and possibly new buffer
            yy_currentPos_l  = yy_currentPos;
            yy_markedPos_l   = yy_markedPos;
            yy_buffer_l      = yy_buffer;
            yy_endRead_l     = yy_endRead;
            if (eof) {
              yy_input = YYEOF;
              break yy_forAction;
            }
            else {
              yy_input = yy_buffer_l[yy_currentPos_l++];
            }
          }
          int yy_next = yytrans_l[ yy_rowMap_l[yy_state] + yycmap_l[yy_input] ];
          if (yy_next == -1) break yy_forAction;
          yy_state = yy_next;

          int yy_attributes = yy_attr_l[yy_state];
          if ( (yy_attributes & 1) == 1 ) {
            yy_action = yy_state; 
            yy_markedPos_l = yy_currentPos_l; 
            if ( (yy_attributes & 8) == 8 ) break yy_forAction;
          }

        }
      }

      // store back cached position
      yy_markedPos = yy_markedPos_l;

      switch (yy_action) {

        case 41: 
          { return makeToken(Token.CLASS_BREAKSPEC, yytext()); }
        case 97: break;
        case 91: 
          { return makeToken(Token.SELECT); }
        case 98: break;
        case 88: 
          { return makeToken(Token.RESUME); }
        case 99: break;
        case 87: 
          { return makeToken(Token.ATTACH); }
        case 100: break;
        case 86: 
          { return makeToken(Token.LAUNCH); }
        case 101: break;
        case 25: 
          { return makeToken(Token.LIST, yytext()); }
        case 102: break;
        case 7: 
        case 8: 
        case 9: 
        case 10: 
        case 11: 
        case 12: 
        case 13: 
        case 14: 
        case 15: 
        case 16: 
        case 23: 
        case 24: 
        case 26: 
        case 27: 
        case 28: 
        case 29: 
        case 30: 
        case 31: 
        case 32: 
        case 33: 
        case 34: 
        case 35: 
        case 36: 
        case 37: 
        case 38: 
        case 44: 
        case 45: 
        case 47: 
        case 49: 
        case 50: 
        case 51: 
        case 52: 
        case 53: 
        case 54: 
        case 55: 
        case 56: 
        case 57: 
        case 58: 
        case 61: 
        case 62: 
        case 63: 
        case 65: 
        case 66: 
        case 68: 
        case 70: 
        case 72: 
        case 75: 
        case 76: 
        case 77: 
        case 78: 
        case 79: 
        case 80: 
        case 81: 
        case 89: 
          { return makeToken(Token.WORD, yytext()); }
        case 103: break;
        case 64: 
          { return makeToken(Token.EXIT, yytext()); }
        case 104: break;
        case 67: 
          { return makeToken(Token.SHOW, yytext()); }
        case 105: break;
        case 69: 
          { return makeToken(Token.STEP, yytext()); }
        case 106: break;
        case 92: 
          { return makeToken(Token.THREAD, yytext()); }
        case 107: break;
        case 6: 
          { return makeToken(Token.NUMBER, yytext()); }
        case 108: break;
        case 43: 
          { return makeToken(Token.HEX_ID, yytext()); }
        case 109: break;
        case 46: 
          { return makeToken(Token.ALL); }
        case 110: break;
        case 48: 
          { return makeToken(Token.RUN); }
        case 111: break;
        case 60: 
          { return makeToken(Token.INFO); }
        case 112: break;
        case 71: 
          { return makeToken(Token.KILL); }
        case 113: break;
        case 90: 
          { return makeToken(Token.RUNSCRIPT, yytext()); }
        case 114: break;
        case 42: 
          { return makeToken(Token.DOTTED_ID, yytext());  }
        case 115: break;
        case 39: 
          { return makeToken(Token.MACHINE_ID, yytext()); }
        case 116: break;
        case 2: 
        case 3: 
          {   }
        case 117: break;
        case 95: 
          { return makeToken(Token.SUSPEND);  }
        case 118: break;
        case 1: 
        case 4: 
        case 5: 
          { return makeToken(Token.UNIDENTIFIED, yytext()); }
        case 119: break;
        case 94: 
          { return makeToken(Token.METHOD_BREAKSPEC, yytext()); }
        case 120: break;
        case 82: 
          { return makeToken(Token.BREAKPOINT); }
        case 121: break;
        default: 
          if (yy_input == YYEOF && yy_startRead == yy_currentPos) {
            yy_atEOF = true;
            return null;
          } 
          else {
            yy_ScanError(YY_NO_MATCH);
          }
      }
    }
  }


}
