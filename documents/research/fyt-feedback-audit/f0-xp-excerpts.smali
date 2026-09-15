.method public register(Lx/b;II)V
    .registers 8

    if-nez p1, :cond_3

    return-void

    :cond_3
    if-ltz p2, :cond_129

    const/16 v0, 0x4b0

    if-ge p2, v0, :cond_129

    .line 1
    sget-object v0, Lf0/tp;->e:[Li1/v;

    aget-object v1, v0, p2

    if-nez v1, :cond_1d

    .line 2
    monitor-enter p0

    .line 3
    :try_start_10
    aget-object v1, v0, p2

    if-nez v1, :cond_18

    .line 4
    sget-object v1, Lf0/tp;->g:Li1/v;

    aput-object v1, v0, p2

    .line 5
    :cond_18
    monitor-exit p0

    goto :goto_1d

    :catchall_1a
    move-exception p1

    monitor-exit p0
    :try_end_1c
    .catchall {:try_start_10 .. :try_end_1c} :catchall_1a

    throw p1

    .line 6
    :cond_1d
    :goto_1d
    sget-object v0, Lf0/tp;->g:Li1/v;

    invoke-static {v0, p1, p2}, Li1/v;->b(Li1/v;Lx/b;I)V

    if-nez p3, :cond_25

    return-void

    :cond_25
    const/16 v0, 0x3e8

    if-lt p2, v0, :cond_122

    const/16 v0, 0x3fd

    if-ne p2, v0, :cond_2f

    goto/16 :goto_122

    :cond_2f
    const/4 p3, 0x1

    const/4 v0, 0x0

    packed-switch p2, :pswitch_data_12a

    :pswitch_34
    goto/16 :goto_129

    :pswitch_36
    new-array p3, p3, [I

    .line 7
    sget v1, Lf0/tp;->j:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_41
    new-array p3, p3, [I

    .line 8
    sget v1, Lf0/tp;->k:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    .line 9
    :pswitch_4c
    sget-object v1, Lf0/tp;->Y:Ljava/lang/String;

    invoke-static {v1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_5b

    .line 10
    sget-object v0, Lf0/tp;->Y:Ljava/lang/String;

    invoke-static {p1, p2, p3, v0}, Li1/v;->i(Lx/b;IILjava/lang/String;)V

    goto/16 :goto_129

    .line 11
    :cond_5b
    sget-object p3, Lf0/tp;->Y:Ljava/lang/String;

    invoke-static {p1, p2, v0, p3}, Li1/v;->i(Lx/b;IILjava/lang/String;)V

    goto/16 :goto_129

    :pswitch_62
    new-array p3, p3, [I

    .line 12
    sget v1, Lf0/tp;->P:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_6d
    new-array p3, p3, [I

    aput v0, p3, v0

    .line 13
    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_76
    new-array p3, p3, [I

    aput v0, p3, v0

    .line 14
    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_7f
    new-array p3, p3, [I

    .line 15
    sget v1, Lf0/tp;->F:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_8a
    new-array p3, p3, [I

    aput v0, p3, v0

    .line 16
    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_93
    new-array p3, p3, [I

    .line 17
    sget v1, Lf0/tp;->J:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_9e
    new-array p3, p3, [I

    aput v0, p3, v0

    .line 18
    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_a7
    new-array p3, p3, [I

    .line 19
    sget v1, Lf0/tp;->O:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_b2
    new-array p3, p3, [I

    .line 20
    sget v1, Lf0/tp;->D:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto/16 :goto_129

    :pswitch_bd
    new-array p3, p3, [I

    .line 21
    sget v1, Lf0/tp;->W:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_c7
    new-array p3, p3, [I

    .line 22
    sget v1, Lf0/tp;->E:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_d1
    new-array p3, p3, [I

    .line 23
    sget v1, Lf0/tp;->C:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    .line 24
    :pswitch_db
    sget-object p3, Lf0/tp;->X:Ljava/lang/String;

    invoke-static {p1, p2, p3}, Li1/v;->j(Lx/b;ILjava/lang/String;)V

    goto :goto_129

    :pswitch_e1
    new-array p3, p3, [I

    .line 25
    sget v1, Lf0/tp;->B:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_eb
    new-array p3, p3, [I

    .line 26
    sget v1, Lf0/tp;->U:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_f5
    new-array p3, p3, [I

    .line 27
    sget v1, Lf0/tp;->Q:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_ff
    new-array p3, p3, [I

    .line 28
    sget v1, Lf0/tp;->S:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    :pswitch_109
    const/16 v1, 0x40d

    new-array v2, p3, [I

    .line 29
    sget v3, Lf0/tp;->a:I

    invoke-static {v3}, Lf0/wp;->f(I)I

    move-result v3

    aput v3, v2, v0

    invoke-static {p1, v1, v2}, Li1/v;->k(Lx/b;I[I)V

    new-array p3, p3, [I

    .line 30
    sget v1, Lf0/tp;->a:I

    aput v1, p3, v0

    invoke-static {p1, p2, p3}, Li1/v;->k(Lx/b;I[I)V

    goto :goto_129

    .line 31
    :cond_122
    :goto_122
    iget-object v0, p0, Lf0/xp;->c:Lf0/rp;

    if-eqz v0, :cond_129

    .line 32
    invoke-virtual {v0, p1, p2, p3}, Lf0/rp;->register(Lx/b;II)V

    :cond_129
    :goto_129
    return-void

    :pswitch_data_12a
    .packed-switch 0x3e8
        :pswitch_109
        :pswitch_ff
        :pswitch_f5
        :pswitch_eb
        :pswitch_e1
        :pswitch_db
        :pswitch_34
        :pswitch_d1
        :pswitch_34
        :pswitch_c7
        :pswitch_bd
        :pswitch_34
        :pswitch_b2
        :pswitch_a7
        :pswitch_34
        :pswitch_9e
        :pswitch_93
        :pswitch_8a
        :pswitch_7f
        :pswitch_76
        :pswitch_6d
        :pswitch_34
        :pswitch_62
        :pswitch_4c
        :pswitch_34
        :pswitch_41
        :pswitch_36
    .end packed-switch
.end method

.method public get(I[I[F[Ljava/lang/String;)Lx/f;
    .registers 6

    const/16 v0, 0x3e8

    if-ge p1, v0, :cond_d

    .line 1
    iget-object v0, p0, Lf0/xp;->c:Lf0/rp;

    if-eqz v0, :cond_2c

    .line 2
    invoke-virtual {v0, p1, p2, p3, p4}, Lf0/rp;->get(I[I[F[Ljava/lang/String;)Lx/f;

    move-result-object p1

    return-object p1

    :cond_d
    if-eq p1, v0, :cond_10

    goto :goto_2c

    :cond_10
    const/4 p1, 0x1

    .line 3
    invoke-virtual {p0, p2, p1}, Lf0/rp;->intsOk([II)Z

    move-result p1

    if-eqz p1, :cond_2c

    const/4 p1, 0x0

    aget p3, p2, p1

    if-ltz p3, :cond_2c

    aget p3, p2, p1

    if-ge p3, v0, :cond_2c

    .line 4
    new-instance p3, Lx/f;

    sget-object p4, Lf0/tp;->Z:[I

    aget p1, p2, p1

    aget p1, p4, p1

    invoke-direct {p3, p1}, Lx/f;-><init>(I)V

    return-object p3

    :cond_2c
    :goto_2c
    const/4 p1, 0x0

    return-object p1
.end method
