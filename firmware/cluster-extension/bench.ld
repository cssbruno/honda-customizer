ENTRY(hcx_boot)
SECTIONS {
  . = 0x08016400;
  .text : { *(.text*) *(.rodata*) }
  .unexpected : { *(.data*) *(.bss*) *(COMMON) }
  ASSERT(SIZEOF(.unexpected) == 0, "Unexpected runtime data; no startup initialization")
  ASSERT(. <= 0x08018000, "Extension exceeds bench flash reservation")
  /DISCARD/ : { *(.ARM.exidx*) *(.ARM.extab*) *(.comment*) *(.note*) }
}
