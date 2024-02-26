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
    : classDecl EOF
    ;


classDecl
    : CLASS name=ID
        LCURLY
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INT '['']'
    | name=INT '...'
    | name=BOOL
    | name=INT
    | name=ID
    ;


methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : expr op=( MUL | DIV ) expr #BinaryExpr //
    | expr op=( ADD | SUB ) expr #BinaryExpr //
    | expr op=( MINOR | AND ) expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;



