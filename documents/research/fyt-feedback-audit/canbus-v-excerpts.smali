.method public register(Lx/b;II)V
    .registers 6

    if-ltz p2, :cond_13

    const/16 p3, 0xe9

    if-ge p2, p3, :cond_13

    const/4 p3, 0x1

    new-array p3, p3, [I

    const/4 v0, 0x0

    .line 1
    sget-object v1, Lf0/tp;->f:[I

    aget v1, v1, p2

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    :cond_13
    return-void
.end method

.method public get(I[I[F[Ljava/lang/String;)Lx/f;
    .registers 5

    const/4 p1, 0x0

    return-object p1
.end method
