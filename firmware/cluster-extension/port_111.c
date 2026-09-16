/* SHA-pinned CRI 111 port. Experimental bench image, NOT an installable update. */
#include "hcx.h"
#define STATE ((HcxState *)0x20004E80u)
#define TICKS (*(volatile uint32_t *)0x20004BECu)
_Static_assert(sizeof(HcxState)<=128, "Bench RAM reservation exceeded");
static uint32_t lock(void) {
    uint32_t old; __asm volatile("mrs %0, primask\ncpsid i":"=r"(old)::"memory"); return old;
}
static void unlock(uint32_t old) { __asm volatile("msr primask, %0"::"r"(old):"memory"); }
void __aeabi_memclr4(void *p, unsigned n) {
    volatile uint8_t *b=p; while(n--) *b++=0;
}
void __aeabi_memcpy4(void *d, const void *s, unsigned n) {
    uint8_t *out=d; const uint8_t *in=s; while(n--) *out++=*in++;
}
void hcx_boot(void) { hcx_init(STATE); } /* Replaces a verified bx-lr no-op. */
int hcx_can_enqueue(const HcxFrame *f) {
    /* Caller holds PRIMASK. The original queue writer can otherwise silently drop. */
    uintptr_t slot=*(volatile uint32_t *)0x200020BCu;
    if(!slot || *(volatile uint8_t *)(slot+4)) return 0;
    ((void (*)(const HcxFrame *))0x08004815u)(f);
    return 1;
}
static void send_reply(const HcxReply *r) {
    uint8_t wire[HCX_BODY_SIZE+2], sum=0;
    wire[0]=0x2E;
    for(unsigned i=0;i<HCX_BODY_SIZE;i++) { wire[i+1]=r->body[i]; sum+=r->body[i]; }
    wire[HCX_BODY_SIZE+1]=(uint8_t)(sum^0xFF);
    /* Main/task context only. Does not replace decoder identification text. */
    ((void (*)(const uint8_t *,unsigned))0x08012503u)(wire,sizeof(wire));
}
int hcx_dispatch(const uint8_t *p) {
    if(p[0]!=HCX_TYPE) return ((int (*)(const uint8_t *))0x080133C1u)(p);
    HcxReply reply; uint32_t old=lock();
    int used=hcx_command(STATE,p,(unsigned)p[1]+2,TICKS,&reply);
    unlock(old);
    if(used) send_reply(&reply);
    return used;
}
void hcx_main_tick(void) {
    ((void (*)(void))0x0800E74Bu)(); /* Preserve original call. */
    HcxReply reply; uint32_t old=lock();
    int changed=hcx_poll(STATE,TICKS,&reply);
    unlock(old);
    if(changed) send_reply(&reply);
}
unsigned hcx_transmit(HcxFrame *f) {
    /* Original mailbox writer shifts/mutates IDs. Match BEFORE that call. */
    uint32_t old=lock(); int owned=hcx_before_tx(STATE,f,TICKS);
    if(owned==2) { unlock(old); return 4; }
    unsigned mailbox=((unsigned (*)(HcxFrame *))0x080110E9u)(f);
    if(owned==1) hcx_after_tx(STATE,mailbox,TICKS);
    unlock(old); return mailbox;
}
void hcx_receive(unsigned fifo, HcxFrame *f) {
    ((void (*)(unsigned,HcxFrame *))0x0801125Fu)(fifo,f);
    uint32_t old=lock(); hcx_can_received(STATE,f,TICKS); unlock(old);
}
