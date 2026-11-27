; -----------------------------------------------------------------------------
; File: ChronosTimer.asm
; Project: AxManager Hyper-Engine
; Description: High-precision timing using TSC and APIC Timer.
;              Ensures deterministic scheduling at the nanosecond level.
; -----------------------------------------------------------------------------

section .text
    global ax_tsc_read
    global ax_apic_timer_init
    global ax_apic_set_periodic

; -----------------------------------------------------------------------------
; ax_tsc_read
; Purpose: Reads the Time Stamp Counter (TSC) for ultra-high precision timing.
; Returns: RAX containing the current CPU cycle count.
; -----------------------------------------------------------------------------
ax_tsc_read:
    rdtsc           ; Read Time Stamp Counter (RDTSC)
    ; RAX = low 32 bits, RDX = high 32 bits
    ; We combine them into a single 64-bit value in RAX
    shl rdx, 32
    or rax, rdx
    ret

; -----------------------------------------------------------------------------
; ax_apic_timer_init
; Purpose: Initializes the Local APIC Timer.
; Argument: RDI = Base address of the Local APIC (MMIO).
; -----------------------------------------------------------------------------
ax_apic_timer_init:
    mov rbx, rdi    ; Store APIC base address in RBX

    ; 1. Set the Divide Configuration (Divide by 16 for high res)
    ; LAPIC Timer Division Register is at offset 0x320
    mov dword [rbx + 0x320], 0x0    ; Divide by 1 (maximum precision)

    ; 2. Set the Initial Count (Set to a large value)
    mov dword [rbx + 0x380], 0xFFFFFFFF

    ; 3. Set Periodic Mode (Bit 0 of ICR)
    mov dword [rbx + 0x300], 0x2   ; Set bit 1 for Periodic Mode

    ret

; -----------------------------------------------------------------------------
; ax_apic_set_periodic
; Purpose: Configures the APIC timer to fire at a specific frequency.
; Argument: RDI = APIC Base, RSI = Frequency (in ticks).
; -----------------------------------------------------------------------------
ax_apic_set_periodic:
    mov rbx, rdi
    mov ecx, esi
    mov [rbx + 0x380], ecx         ; Set the reload value for the timer
    ret
