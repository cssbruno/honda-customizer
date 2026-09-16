#ifndef HCX_H
#define HCX_H
#include <stdint.h>
#include <stddef.h>

/* NEW HCX protocol, not a stock XP command. All time units are vendor ticks. */
#define HCX_TYPE 0xD7
#define HCX_BODY_SIZE 13
#define HCX_TIMEOUT_TICKS 800u
#define HCX_MAGIC 0x48435831u
enum { HCX_HELLO=0, HCX_SET=1, HCX_STATUS=2 };
enum { HCX_READY=0x10, HCX_QUEUED, HCX_TX_STARTED, HCX_MATCHING_REPLY,
       HCX_TIMEOUT, HCX_BUSY, HCX_INVALID, HCX_QUEUE_FULL, HCX_TX_FAILED,
       HCX_DUPLICATE };
typedef struct {
    uint32_t standard_id, extended_id;
    uint8_t ide, rtr, dlc, data[8], padding;
} HcxFrame;
_Static_assert(sizeof(HcxFrame)==20 && offsetof(HcxFrame,data)==11, "Vendor CAN ABI");
typedef struct {
    uint32_t magic, transaction, started;
    HcxFrame frame;
    uint8_t status, published, feature, api_value, queued_owned;
} HcxState;
typedef struct { uint8_t body[HCX_BODY_SIZE]; } HcxReply;
void hcx_init(HcxState *s);
int hcx_command(HcxState *s, const uint8_t *body, unsigned size,
                uint32_t now, HcxReply *reply);
int hcx_poll(HcxState *s, uint32_t now, HcxReply *reply);
/* 0: unrelated, 1: owned and permitted, 2: expired; suppress transmission. */
int hcx_before_tx(HcxState *s, const HcxFrame *frame, uint32_t now);
void hcx_after_tx(HcxState *s, unsigned mailbox, uint32_t now);
void hcx_can_received(HcxState *s, const HcxFrame *frame, uint32_t now);
/* Port must serialize state access. Enqueue is nonblocking, never retries. */
int hcx_can_enqueue(const HcxFrame *frame);
#endif
