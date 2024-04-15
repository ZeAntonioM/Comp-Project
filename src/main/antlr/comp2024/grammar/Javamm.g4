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

importDecl
    : 'import' name+=ID ( '.' name+=ID )* SEMI #ImportDeclRule
    ;

classDecl locals [boolean hasSuperClass = false]
    : CLASS name=ID
        ('extends' superclass=ID {$hasSuperClass = true;})?
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
    : type name=ID #ParamRule
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt
    | 'if' LPAREN expr RPAREN stmt ('else' stmt) #IfElseStmt
    | 'while' LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | RETURN expr SEMI #ReturnStmt
    | LCURLY ( stmt )* RCURLY #BlockStmt
    ;

expr
    : LPAREN expr RPAREN #PrecendentExpr
    | '!' expr #NegExpr
    | expr op=( MUL | DIV ) expr #BinaryExpr 
    | expr op=( ADD | SUB ) expr #BinaryExpr 
    | expr op=( LTHAN | GTHAN | AND ) expr #BinaryExpr
    | expr '.' ID LPAREN ( expr ( ',' expr )* )? RPAREN #MemberCallExpr
    | expr LBRAC expr RBRAC #ArrayRefExpr
    | LBRAC ( expr ( ',' expr )* )? RBRAC #ArrayInitExpr
    | 'new' 'int' LBRAC expr RBRAC #NewArrayExpr
    | expr '.' 'length' #LengthExpr
    | 'this' ('.' (name=ID | name='main' | name='length'))? #SelfExpr
    | 'new' (name=ID | name='main' | name='length') LPAREN RPAREN #NewObjExpr
    //| (name=ID | name='main' | name='length') LBRAC expr RBRAC #ArrayRefExpr
    | value=INTEGER #IntegerLiteral 
    | bool=BOOLEAN #BoolExpr
    | (name=ID | name='main' | name='length') #VarRefExpr
    ;
