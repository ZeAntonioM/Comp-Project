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
LTHAN : '<';
GTHAN : '>';
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

COMMENT : LCOM .*? RCOM -> skip;
LINECOMMENT : COM .*? ('\r')?'\n' -> skip;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF #Program
    ;

importDecl
    : ‘import’ ID ( ‘.’ ID )* SEMI #ImportDecl
    ;

classDecl
    : CLASS name=ID ('extends' ID)? LCURLY varDecl* methodDecl* RCURLY #ClassDecl
    ;

varDecl
    : type name=ID SEMI #VarDecl
    ;

type
    : name=INT LBRAC RBRAC #ArrayType
    | name=INT '...' #VarargType
    | name=BOOL #BoolType
    | name=INT #IntType
    | name=ID #ObjectType
    ;


methodDecl 
    : (PUBLIC)? type name=ID LPAREN ( type args+=ID ( ',' type args+=ID )* )? RPAREN LCURLY ( varDecl )* ( stmt )* RETURN expr ';' RCURLY #ClassMethod
    | (PUBLIC)? 'static' 'void' 'main' LPAREN 'String' LBRAC RBRAC args=ID RPAREN LCURLY ( varDecl )* ( stmt )* RCURLY #MainFunction
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt 
    | 'if' LPAREN expr* RPAREN stmt 'else' stmt #IfElseStmt
    | 'while' LPAREN expr* RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : LPAREN expr RPAREN #PrecendentExpr
    | '!' expr #NegExpr
    | expr op=( MUL | DIV ) expr #BinaryExpr 
    | expr op=( ADD | SUB ) expr #BinaryExpr 
    | expr op=( LTHAN | GTHAN | AND ) expr #BinaryExpr 
    | LBRAC ( expr ( ',' expr )* )? RBRAC #ArrayInitExpr
    | 'new' 'int' LBRAC expr RBRAC #NewArrayExpr
    | expr '.length' #LengthExpr
    | 'this' #SelfExpr
    | 'new' name=ID LPAREN RPAREN #NewObjExpr
    | name=ID LBRAC expr RBRAC #ArrayRefExpr
    | name=ID #VarRefExpr 
    | value=INTEGER #IntegerLiteral 
    | bool=( 'true' | 'false' ) #BoolExpr
    ;



