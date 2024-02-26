grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
RBRAC : ']' ;
LBRAC : '[' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
AND : '&&' ;
MINOR : '<';
COM : '//';
RCOM : '*/';
LCOM: '/*';

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean';
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : ('0'|[1-9]) [0-9]* ;
ID : ([a-z]|[A-Z]|'_'|'$') ([a-z]|[A-Z]|'_'|'$'|[0-9])*  ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : ‘import’ ID ( ‘.’ ID )* SEMI 
    ;

classDecl
    : CLASS name=ID ('extends' ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INT LBRAC RBRAC
    | name=INT '...'
    | name=BOOL
    | name=INT
    | name=ID
    ;


methodDecl 
    : (PUBLIC)? type ID LPAREN ( type ID ( ',' type ID )* )? RPAREN LCURLY ( varDecl )* ( stmt )* RETURN expr ';' RCURLY #classMethod
    | (PUBLIC)? 'static' 'void' 'main' LPAREN 'String' LBRAC RBRAC ID RPAREN LCURLY ( varDecl )* ( stmt )* RCURLY #mainFunction
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | 'if' LPAREN expr* RPAREN stmt 'else' stmt #IfElseStmt
    | 'while' LPAREN expr* RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : '!' expr #NegExpr
    | expr op=( MUL | DIV ) expr #BinaryExpr //
    | expr op=( ADD | SUB ) expr #BinaryExpr //
    | expr op=( MINOR | AND ) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | name=ID LBRAC expr RBRAC #ArrayRefExpr
    | expr '.length' #LengthExpr
    | 'new' 'int' LBRAC expr RBRAC #NewArrayExpr
    | 'new' name=ID LPAREN RPAREN #NewObjExpr
    | LPAREN expr RPAREN #PrecendentExpr
    | bool=( 'true' | 'false' ) #BoolExpr
    | 'this' #SelfExpr
    ;



