/* Host-test adapter only. Never linked into the ARM image or Android APK. */
#include "hcx.h"
static HcxFrame captured;
static unsigned count;
static int available=1;
void test_queue_reset(int ready) { available=ready; count=0; }
unsigned test_queue_count(void) { return count; }
const HcxFrame *test_queue_frame(void) { return &captured; }
int hcx_can_enqueue(const HcxFrame *f) {
    if(!available) return 0;
    captured=*f; count++; return 1;
}
