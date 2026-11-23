; -----------------------------------------------------------------------------
; File: HyperContextSwitcher.asm
; Project: AxManager Hyper-Engine
; Description: Ultra-low latency context switching using manual register dumping.
;              This is the "Soul" of the Hyper-Kernel.
; -----------------------------------------------------------------------------

section .text
    global ax_hyper_context_save
    global ax_hyper_context_restore

; -----------------------------------------------------------------------------
; ax_hyper_context_save
; Purpose: Saves the current CPU state into an AxTaskStruct (Memory-mapped).
; Arguments:
;    RDI = Pointer to the current AxTaskStruct (destination for registers)
; -----------------------------------------------------------------------------
ax_hyper_context_save:
    ; We assume RDI points to the start of the AxTaskStruct memory block
    ; Structure: [R15, R14, ..., R8, RBP, RDI, RSI, RDX, RCX, RBX, RAX, RSP, RBP, RDI, RSI, RDX, RCX, RBX, RAX, RIP]
    
    ; Save General Purpose Registers
    mov [rdi + 0],  r15
    mov [rdi + 8],  r14
    mov [rdi + 16], r13
    mov [rdi + 24], r12
    mov [rdi + 32], r11
    mov [rdi + 40], r10
    mov [rdi + 48], r9
    mov [rdi + 56], r8
    mov [rdi + 64], rbp
    mov [rdi + 72], rdi
    mov [rdi + 80], rsi
    mov [rdi + 88], rdx
    mov [rdi + 96], rcx
    mov [rdi + 104], rbx
    mov [rdi + 112], rax

    ; Save Stack Pointer and Instruction Pointer
    mov [rdi + 120], rsp
    lea rax, [rel .return_label] ; Capture the return address as current RIP
    mov [rdi + 128], rax

    ret

.return_label:
    ret

; -----------------------------------------------------------------------------
; ax_hyper_context_restore
; Purpose: Restores the CPU state from a target AxTaskStruct.
; Arguments:
;    RDI = Pointer to the target AxTaskStruct (source of registers)
; -----------------------------------------------------------------------------
ax_hyper_context_restore:
    ; Note: We must be careful not to corrupt RDI before we use it.
    
    ; Load General Purpose Registers
    mov r15, [rdi + 0]
    mov r14, [rdi + 8]
    mov r13, [rdi + 16]
    mov r12, [rdi + 24]
    mov r11, [rdi + 32]
    mov r10, [rdi + 40]
    mov r9,  [rdi + 48]
    mov r8,  [rdi + 56]
    mov rbp, [rdi + 64]
    mov rdi, [rdi + 72]
    mov rsi, [rdi + 80]
    mov rdx, [rdi + 88]
    mov rcx, [rdi + 96]
    mov rbx, [rdi + 104]
    mov rax, [rdi + 112]

    ; Restore Stack Pointer
    mov rsp, [rdi + 120]

    ; THE CRITICAL MOMENT: Jump to the saved Instruction Pointer (RIP)
    ; This effectively transfers control to the next task.
    jmp [rdi + 128]
