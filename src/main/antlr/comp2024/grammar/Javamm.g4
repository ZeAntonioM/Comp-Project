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
STATIC : 'static' ;
INT : 'int' ;
BOOL : 'boolean';
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0] | ([1-9][0-9]*);
BOOLEAN: 'true' | 'false';
ID : ([a-z]|[A-Z]|'_'|'$') ([a-z]|[A-Z]|'_'|'$'|[0-9])*;


COMMENT : LCOM .*? RCOM -> skip;
LINECOMMENT : COM .*? ('\r')?'\n' -> skip;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl locals [boolean isSubImport = false]
    : 'import' (name+=ID | 'main') ( '.' (name+=ID | 'main') {$isSubImport = true;})* SEMI #ImportDeclRule
    ;

classDecl locals [boolean hasSuperClass = false]
    : CLASS name=(ID | 'main')
        ('extends' superclass=(ID | 'main') {$hasSuperClass = true;})?
        LCURLY
            varDecl* methodDecl*
        RCURLY #ClassDeclRule
    ;

varDecl
    : type (name=ID | name='main' | name='length') SEMI #VarDeclRule
    ;

type locals [boolean isArray=false, boolean isVararg=false]
    : name=INT LBRAC RBRAC {$isArray=true;} #ArrayType
    | name=INT '...' {$isVararg=true;}#VarargType
    | name=INT #IntType
    | name=BOOL #BoolType
    | name=ID #ObjectType
    | name='String' {$isArray=true;} #StringType
    ;


methodDecl locals [boolean isPublic=false, boolean isStatic = false]
    : (PUBLIC {$isPublic=true;})?
        (STATIC {$isStatic=true;})?
        type name=ID
        LPAREN
            ( paramDecl ( ',' paramDecl )* )?
        RPAREN
        LCURLY
            varDecl* ( stmt )*
            RETURN expr
            SEMI
        RCURLY #ClassMethod
    | (PUBLIC {$isPublic=true;})?
        (STATIC {$isStatic=true;})?
        'void' name='main'
        LPAREN
            'String' LBRAC RBRAC
            args=ID
        RPAREN
        LCURLY
           varDecl* ( stmt )*
        RCURLY #MainFunction
    ;

paramDecl
    : type name=(ID|'main') #ParamRule
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt
    | 'if' LPAREN expr RPAREN stmt ('else' stmt) #IfElseStmt
    | 'while' LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | RETURN expr SEMI #ReturnStmt  //isto é necessário para poder haver varios returns num metodo
    | LCURLY ( stmt )* RCURLY #BlockStmt
    ;

expr
    : LPAREN expr RPAREN #PrecedentExpr  //removi o n para funcionar
    | '!' expr #NegExpr
    | expr '.' name=ID LPAREN ( expr ( ',' expr )* )? RPAREN #MemberCallExpr
    | 'this' ('.' (name=ID | name='main' | name='length'))? #SelfExpr
    | expr LBRAC expr RBRAC #ArrayRefExpr                                      //not for cp2
    | expr op=( MUL | DIV ) expr #BinaryExpr
    | expr op=( ADD | SUB ) expr #BinaryExpr
    | expr op=( LTHAN | GTHAN | AND ) expr #BinaryExpr
    | LBRAC ( expr ( ',' expr )* )? RBRAC #ArrayInitExpr                       //not for cp2
    | 'new' 'int' LBRAC expr RBRAC #NewArrayExpr                               //not for cp2
    | expr '.' 'length' #LengthExpr                                            //not for cp2
    | 'new' (name=ID | name='main' | name='length') LPAREN RPAREN #NewObjExpr
    //| (name=ID | name='main' | name='length') LBRAC expr RBRAC #ArrayRefExpr   //not for cp2
    | value=INTEGER #IntegerLiteral
    | bool=BOOLEAN #BoolExpr
    | (name=ID | name='main' | name='length') #VarRefExpr
    ;

