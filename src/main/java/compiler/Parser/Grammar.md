// Grammar for a simple programming language
Program         -> TopLevel* // We read the whole program as a sequence of top-level constructs.
TopLevel        -> FunctionDef | Statement // Can be a function definition or a statement at the top level.

// Function 
FunctionDef     -> "def" Identifier "(" ParamList? ")" Block
ParamList       -> Param ("," Param)*
Param           -> Type Identifier

// Blocks and statements
Block           -> "{" Statement* "}"

Statement       -> VarDecl ";"
                | Assignment ";"
                | ReturnStmt ";"
                | IfStmt
                | WhileStmt
                | ForStmt
                | Expr ";"
                | Block

// Variable declarations
VarDecl         -> FinalOpt Type Identifier InitOpt
FinalOpt        -> "final" | ε
InitOpt         -> "=" Expr | ε

// Assignments
Assignment      -> Identifier "=" Expr

// Control flow
IfStmt          -> "if" "(" Expr ")" Statement ElseOpt
ElseOpt         -> "else" Statement | ε
WhileStmt       -> "while" "(" Expr ")" Statement
ForStmt         -> "for" "(" ForInitOpt ";" ForCondOpt ";" ForUpdateOpt ")" Statement
ForInitOpt      -> VarDecl | Assignment | Expr | ε
ForCondOpt      -> Expr | ε
ForUpdateOpt    -> Assignment | Expr | ε

// Return statement
ReturnStmt      -> "return" ReturnExprOpt
ReturnExprOpt   -> Expr | ε

// Types
Type            -> BaseType
                | CollectionType
                | ArrayType
BaseType        -> "INT"
                | "FLOAT"
                | "STRING"
                | "BOOL"
CollectionType  -> CollectionName
ArrayType       -> "ARRAY" "[" Type "]"

// Identifiers
Identifier      -> IDENTIFIER
CollectionName  -> COLLECTION_NAME

// Expressions
Expr            -> OrExpr
OrExpr          -> AndExpr ("||" AndExpr)*
AndExpr         -> EqualityExpr ("&&" EqualityExpr)*
EqualityExpr     -> RelExpr (("==" | "=/=") RelExpr)*
relExpr         -> AddExpr (("<" | ">" | "<=" | ">=") AddExpr)*
AddExpr         -> MulExpr (("+" | "-") MulExpr)*
MulExpr         -> UnaryExpr (("*" | "/" | "%") UnaryExpr)*
UnaryExpr       -> ("-" | "not") UnaryExpr
                | PostfixExpr
PostfixExpr     -> PrimaryExpr PostfixSuffix*
PostfixSuffix   -> "(" ArgList? ")"
                | "[" Expr "]"
                | "." Identifier
ArgList         -> Expr ( "," Expr )*
PrimaryExpr     -> INT
                | FLOAT
                | STRING
                | BOOLEAN
                | Identifier
                | "(" Expr ")"
