.method public static s([Li1/v;[III)V
    .registers 5

    if-eqz p1, :cond_22

    if-ltz p2, :cond_22

    .line 1
    array-length v0, p1

    if-ge p2, v0, :cond_22

    .line 2
    aget v0, p1, p2

    if-eq v0, p3, :cond_22

    .line 3
    aput p3, p1, p2

    if-eqz p0, :cond_22

    .line 4
    array-length p1, p0

    if-ge p2, p1, :cond_22

    aget-object p1, p0, p2

    if-eqz p1, :cond_22

    .line 5
    aget-object p0, p0, p2

    const/4 p1, 0x1

    new-array p1, p1, [I

    const/4 v0, 0x0

    aput p3, p1, v0

    const/4 p3, 0x0

    invoke-virtual {p0, p2, p1, p3, p3}, Li1/v;->h(I[I[F[Ljava/lang/String;)V

    :cond_22
    return-void
.end method
