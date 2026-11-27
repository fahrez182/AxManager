; -----------------------------------------------------------------------------
; File: VmxEngine.asm
; Project: AxManager Hyper-Engine
; Description: Intel VT-x hardware virtualization root enabler.
;              Enables VMX, checks capabilities via MSRs, and enters VMX root operation.
; -----------------------------------------------------------------------------

section .text
    global ax_vmx_check_support
    global ax_vmx_enable
    global ax_vmx_on

; -----------------------------------------------------------------------------
; ax_vmx_check_support
; Purpose: Checks if the CPU supports Intel VMX via CPUID leaf 1.
; Returns: RAX = 1 if supported, 0 otherwise.
; -----------------------------------------------------------------------------
ax_vmx_check_support:
    push rbx
    mov eax, 1          ; CPUID function 1
    cpuid
    ; ECX bit 5 indicates VMX support
    bt ecx, 5
    setc al             ; AL = carry flag (1 if bit 5 set, 0 otherwise)
    movzx rax, al
    pop rbx
    ret

; -----------------------------------------------------------------------------
; ax_vmx_enable
; Purpose: Enables VMX operation in CR4 control register (CR4.VMXE = bit 13).
; -----------------------------------------------------------------------------
ax_vmx_enable:
    mov rax, cr4
    or rax, (1 << 13)   ; Set VMXE (Virtual Machine Extensions Enable)
    mov cr4, rax
    ret

; -----------------------------------------------------------------------------
; ax_vmx_on
; Purpose: Executes the VMXON instruction to enter VMX root operation.
; Argument: RDI = Pointer to a 4KB aligned physical region (VMXON region).
; Returns: RAX = 0 on success, 1 on failure.
; -----------------------------------------------------------------------------
ax_vmx_on:
    vmxon [rdi]
    jc .error           ; Jump if carry flag set (failure)
    jz .error           ; Jump if zero flag set (failure)
    xor rax, rax        ; Success: return 0
    ret
.error:
    mov rax, 1          ; Failure: return 1
    ret
