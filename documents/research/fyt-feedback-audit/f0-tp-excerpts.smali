.method public static constructor <clinit>()V
    .registers 4

    .line 1
    new-instance v0, Lf0/rp;

    invoke-direct {v0}, Lf0/rp;-><init>()V

    sput-object v0, Lf0/tp;->d:Lf0/rp;

    const/16 v0, 0x4b0

    new-array v1, v0, [Li1/v;

    .line 2
    sput-object v1, Lf0/tp;->e:[Li1/v;

    new-array v0, v0, [I

    .line 3
    sput-object v0, Lf0/tp;->f:[I

    .line 4
    new-instance v1, Li1/v;

    invoke-direct {v1}, Li1/v;-><init>()V

    sput-object v1, Lf0/tp;->g:Li1/v;

    const/4 v1, 0x0

    .line 5
    sput v1, Lf0/tp;->h:I

    .line 6
    sput v1, Lf0/tp;->i:I

    const/16 v2, 0x3e9

    const/4 v3, 0x1

    aput v3, v0, v2

    const/16 v2, 0x3ea

    aput v3, v0, v2

    .line 7
    sput v1, Lf0/tp;->o:I

    .line 8
    sput v3, Lf0/tp;->P:I

    .line 9
    sput v3, Lf0/tp;->Q:I

    sput v3, Lf0/tp;->R:I

    .line 10
    sput v3, Lf0/tp;->S:I

    sput v3, Lf0/tp;->T:I

    const/16 v0, 0x3e8

    new-array v0, v0, [I

    .line 11
    sput-object v0, Lf0/tp;->Z:[I

    .line 12
    sget-object v0, Lf0/wp;->i:Li1/n;

    sput-object v0, Lf0/tp;->a0:Li1/n;

    new-array v0, v3, [Ljava/lang/String;

    const-string v2, "com.syu.canbus"

    aput-object v2, v0, v1

    .line 13
    sput-object v0, Lf0/tp;->b0:[Ljava/lang/String;

    const/4 v0, -0x1

    .line 14
    sput v0, Lf0/tp;->d0:I

    return-void
.end method
