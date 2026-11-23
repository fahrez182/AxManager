; -----------------------------------------------------------------------------
; File: PagingEngine.asm
; Project: AxManager Hyper-Engine
; Description: Direct manipulation of CR3 and Page Table hierarchies (PML4, PDPT, PD, PT).
;               This is the foundation of the Virtual Memory Sandbox.
; -----------------------------------------------------------------------------

section .text
    global ax_paging_load_pml4
    global ax_paging_flush_tlb
    global ax_paging_map_page

; -----------------------------------------------------------------------------
; ax_paging_load_pml4
; Purpose: Loads the PML4 base address into the CR3 register.
; Argument: RDI = Physical address of the PML4 table.
; -----------------------------------------------------------------------------
ax_paging_load_pml4:
    mov cr3, rdi        ; Directly switch the entire memory context!
    ret

; -----------------------------------------------------------------------------
; ax_paging_flush_tlb
; Purpose: Flushes the Translation Lookaside Buffer (TLB) to ensure no stale 
;          mappings exist.
; -----------------------------------------------------------------------------
ax_paging_flush_tlb:
    mov rax, cr3        ; Moving CR3 to RAX
    mov cr3, rax        ; Writing it back flushes the non-global TLB entries
    ret

; -----------------------------------------------------------------------------
; ax_paging_map_page (Simplified Logic)
; Purpose: To be called by C++ to link a virtual address to a physical address
;          via the 4-level hierarchy.
; -----------------------------------------------------------------------------
; (In a real implementation, this would involve complex bit-masking of 
;  the virtual address to traverse PML4 -> PDPT -> PD -> PT)
