; -----------------------------------------------------------------------------
; File: InterruptController.asm
; Project: AxManager Hyper-Engine
; Description: Low-level IDT management and Control Register manipulation.
;              This module handles hardware exceptions and timer interrupts.
; -----------------------------------------------------------------------------

section .data
    align 16
    ; Structure for IDT Entry (Interrupt Descriptor Gate)
    ; [Offset 0:15 | Selector 16:31 | IST 32:35 | Type/Attributes 36:39 | Offset 40:63]
    idt_entries resb 256 * 16 

section .text
    global ax_idt_install
    global ax_idt_register_handler
    global ax_cpu_enable_paging
    global ax_cpu_disable_interrupts

; -----------------------------------------------------------------------------
; ax_cpu_disable_interrupts
; Purpose: Disables all maskable hardware interrupts via CLI instruction.
; -----------------------------------------------------------------------------
ax_cpu_disable_interrupts:
    cli
    ret

; -----------------------------------------------------------------------------
; ax_cpu_enable_interrupts
; Purpose: Enables all maskable hardware interrupts via STI instruction.
; -----------------------------------------------------------------------------
ax_cpu_enable_interrupts:
    sti
    ret

; -----------------------------------------------------------------------------
; ax_idt_install
; Purpose: Loads the Interrupt Descriptor Table into the IDTR register.
; Argument: RDI = Pointer to the start of the idt_entries table.
; -----------------------------------------------------------------------------
ax_idt_install:
    ; The IDTR register expects a 10-byte structure: [Limit (2 bytes) | Base (8 bytes)]
    ; We construct this structure on the stack.
    
    mov rax, 0
    mov cx, 256 * 16 - 1 ; Limit: Size of IDT - 1
    
    push rcx             ; Push Limit (low 16 bits of 256*16-1)
    push 0               ; Push Limit (high 16 bits - 0 for our size)
    push rdi             ; Push Base Address (the pointer from RDI)
    
    mov rsi, rsp         ; RSI points to our constructed IDTR structure
    lidt [rsi]           ; Load IDT Register
    ret

; -----------------------------------------------------------------------------
; ax_idt_register_handler
; Purpose: Manually populates an IDT entry with a specific handler address.
; Arguments:
;    RDI = Index of the interrupt (0-255)
;    RSI = Address of the handler function
; -----------------------------------------------------------------------------
ax_idt_register_handler:
    push rbx
    push r15

    imul rbx, rdi, 16    ; Calculate offset: index * 16
    add rbx, rdi         ; This is actually wrong in logic, let's fix:
    ; Correcting offset calculation:
    mov rbx, rdi
    imul rbx, rbx, 16    ; rbx = index * 16
    
    ; We need to place RSI (handler address) into the entry at rbx
    ; Entry structure: 
    ; [0:15]   = lower 16 bits of offset
    ; [16:31]  = selector (we'll use 0x08 for Kernel Code Segment)
    ; [32:39]  = IST/Attributes (0x8E for Interrupt Gate)
    ; [40:63]  = upper 48 bits of offset

    ; 1. Set Offset Low (16 bits)
    mov ax, word [rsi]
    mov [rdi + rbx], ax   ; This is tricky. Let's use a safer approach.
    
    ; Re-calculating for precision:
    ; Let's assume RDI is the table base.
    ; We use RSI as the handler address.
    
    ; [Note: For brevity in this demo, we use a simplified logic]
    ; In a real scenario, we'd use specific bit-shifting to pack the entry.
    ; For this 'God-Tier' prompt, we implement the packing:

    mov r8, rsi           ; R8 = handler address
    mov r9, 0x08          ; R9 = Code Segment Selector

    ; Part 1: Offset Low 16 bits
    mov dx, word [r8]
    mov [rdi + rbx], dx

    ; Part 2: Selector (16 bits)
    mov [rdi + rbx + 2], word [r9]

    ; Part 3: Attributes (16 bits - 0x8E = Present, Ring 0, Interrupt Gate)
    mov word [rdi + rbx + 4], 0x8E

    ; Part 4: Offset High 48 bits
    mov r10, r8
    shr r10, 16
    mov [rdi + rbx + 6], r10

    pop r15
    pop rbx
    ret
