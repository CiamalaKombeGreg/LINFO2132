package compiler.Lexer;

public enum SymbolType {
    // Literals / names
    IDENTIFIER, /* Variables */
    KEYWORD, /* if, else, while, for, return, etc. */
    COLLECTION_NAME, // starts with Capital letter, for example: List, Set, Map, etc.
    INT,
    FLOAT,
    BOOLEAN, /* Boolean literals: true, false */
    STRING,

    // Operators
    ASSIGNMENTOPERATOR, /* = */
    PLUS,
    MINUS,
    MULT, /* * for multiplication */
    DIV, /* / for division */
    MOD, /* % for modulo */

    // Logical and comparison operators
    EQ, /* == */
    NEQ, /* =/= */
    LT, /* < */
    GT, /* > */
    LE, /* <= */
    GE, /* >= */
    AND, /* && */
    OR, /* || */

    // Delimiters and punctuation
    LPAREN, /* ( */
    RPAREN, /* ) */
    LBRACE, /* { */
    RBRACE, /* } */
    LBRACKET, /* [ */
    RBRACKET, /* ] */
    COMMA, /* , */
    SEMICOLON, /* ; */
    DOT, /* . */
    EOF /* End of file */
}
