#include "hcx.h"

static int live(const HcxState *s) { return s->magic == HCX_MAGIC; }
static int expired(const HcxState *s, uint32_t now) {
    return (uint32_t)(now - s->started) >= HCX_TIMEOUT_TICKS;
}
static void response(HcxReply *r, uint8_t status, uint32_t id,
                     uint8_t feature, uint8_t value) {
    uint8_t *p=r->body;
    p[0]=HCX_TYPE; p[1]=11; p[2]='H'; p[3]='C'; p[4]='X'; p[5]=1;
    p[6]=status; p[7]=(uint8_t)(id>>24); p[8]=(uint8_t)(id>>16);
    p[9]=(uint8_t)(id>>8); p[10]=(uint8_t)id; p[11]=feature; p[12]=value;
}
static int value_map(uint8_t feature, uint8_t api, uint8_t *menu, uint8_t *value) {
    /* OEM XML 03/5B, 03/38, 03/29, 03/2A. Values are explicit requests. */
    switch(feature) {
    case 1: if(api<1 || api>3) return 0; *menu=0x15; *value=api-1; return 1;
    case 2: if(api<1 || api>2) return 0; *menu=0x20; *value=api-1; return 1;
    case 3: if(api<1 || api>7) return 0; *menu=0x32; *value=api; return 1;
    case 4: if(api<1 || api>8) return 0; *menu=0x33; *value=api==1 ? 7 : api-1; return 1;
    default: return 0;
    }
}
static int same_frame(const HcxFrame *a, const HcxFrame *b) {
    return a->ide==4 && a->rtr==0 && a->dlc==3 &&
           a->extended_id==b->extended_id && a->data[0]==b->data[0] &&
           a->data[1]==b->data[1] && a->data[2]==b->data[2];
}
void hcx_init(HcxState *s) {
    uint8_t *p=(uint8_t *)s;
    for(unsigned i=0;i<sizeof(*s);i++) p[i]=0;
    s->magic=HCX_MAGIC; s->status=HCX_READY; s->published=HCX_READY;
}
int hcx_command(HcxState *s, const uint8_t *p, unsigned n, uint32_t now, HcxReply *r) {
    if(!n || p[0]!=HCX_TYPE) return 0; /* Preserve the original dispatcher. */
    response(r,HCX_INVALID,0,0,0);
    if(!live(s) || n!=HCX_BODY_SIZE || p[1]!=11 || p[2]!='H' || p[3]!='C' ||
       p[4]!='X' || p[5]!=1) return 1;
    uint32_t id=((uint32_t)p[7]<<24)|((uint32_t)p[8]<<16)|((uint32_t)p[9]<<8)|p[10];
    uint8_t feature=p[11], api=p[12], menu, value;
    if(p[6]==HCX_HELLO && !feature && !api) {
        /* Last fields are protocol revision and feature mask, NOT vehicle state. */
        response(r,HCX_READY,id,1,0x0F); return 1;
    }
    if(p[6]==HCX_STATUS && !feature && !api && id && id==s->transaction) {
        if((s->status==HCX_QUEUED || s->status==HCX_TX_STARTED) && expired(s,now))
            s->status=HCX_TIMEOUT;
        response(r,s->status,id,s->feature,s->api_value); return 1;
    }
    if(p[6]!=HCX_SET || !id || !value_map(feature,api,&menu,&value)) return 1;
    if(s->queued_owned || s->status==HCX_TX_STARTED) {
        response(r,HCX_BUSY,id,feature,api); return 1;
    }
    if(id==s->transaction) { response(r,HCX_DUPLICATE,id,feature,api); return 1; }
    HcxFrame frame={0};
    frame.extended_id=0x16305054; frame.ide=4; frame.dlc=3;
    frame.data[0]=0x40; frame.data[1]=menu; frame.data[2]=value;
    s->frame=frame; s->transaction=id; s->feature=feature; s->api_value=api;
    s->started=now; s->queued_owned=1; s->status=HCX_QUEUED;
    if(!hcx_can_enqueue(&s->frame)) { s->queued_owned=0; s->status=HCX_QUEUE_FULL; }
    s->published=s->status;
    response(r,s->status,id,feature,api); return 1;
}
int hcx_before_tx(HcxState *s, const HcxFrame *f, uint32_t now) {
    if(!live(s) || !s->queued_owned || !same_frame(f,&s->frame)) return 0;
    if(s->status==HCX_TIMEOUT || expired(s,now)) {
        s->queued_owned=0; s->status=HCX_TIMEOUT; return 2;
    }
    return 1;
}
void hcx_after_tx(HcxState *s, unsigned mailbox, uint32_t now) {
    if(!live(s) || !s->queued_owned) return;
    s->queued_owned=0;
    s->status=mailbox<3 ? HCX_TX_STARTED : HCX_TX_FAILED;
    s->started=now;
}
void hcx_can_received(HcxState *s, const HcxFrame *f, uint32_t now) {
    if(!live(s) || s->status!=HCX_TX_STARTED) return;
    if(expired(s,now)) { s->status=HCX_TIMEOUT; return; }
    if(f->extended_id==0x16305450 && f->ide==4 && !f->rtr && f->dlc==3 &&
       f->data[0]==0xC0 && f->data[1]==s->frame.data[1] && f->data[2]==s->frame.data[2])
        s->status=HCX_MATCHING_REPLY;
}
int hcx_poll(HcxState *s, uint32_t now, HcxReply *r) {
    if(!live(s)) return 0;
    if((s->status==HCX_QUEUED || s->status==HCX_TX_STARTED) && expired(s,now))
        s->status=HCX_TIMEOUT;
    if(s->published==s->status) return 0;
    s->published=s->status;
    response(r,s->status,s->transaction,s->feature,s->api_value);
    return 1;
}
