	.text
	.file	"axelden_monitor.cpp"
	.section	.rodata.cst8,"aM",@progbits,8
	.p2align	3, 0x0                          # -- Begin function Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample
.LCPI0_0:
	.quad	0xbff0000000000000              # double -1
.LCPI0_3:
	.quad	0x4059000000000000              # double 100
	.section	.rodata.cst16,"aM",@progbits,16
	.p2align	4, 0x0
.LCPI0_1:
	.long	1127219200                      # 0x43300000
	.long	1160773632                      # 0x45300000
	.long	0                               # 0x0
	.long	0                               # 0x0
.LCPI0_2:
	.quad	0x4330000000000000              # double 4503599627370496
	.quad	0x4530000000000000              # double 1.9342813113834067E+25
	.section	.text.Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample,"ax",@progbits
	.globl	Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample
	.p2align	4, 0x90
	.type	Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample,@function
Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample: # @Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample
.Lfunc_begin0:
	.cfi_startproc
	.cfi_personality 155, DW.ref.__gxx_personality_v0
	.cfi_lsda 27, .Lexception0
# %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	pushq	%r15
	.cfi_def_cfa_offset 24
	pushq	%r14
	.cfi_def_cfa_offset 32
	pushq	%r13
	.cfi_def_cfa_offset 40
	pushq	%r12
	.cfi_def_cfa_offset 48
	pushq	%rbx
	.cfi_def_cfa_offset 56
	subq	$728, %rsp                      # imm = 0x2D8
	.cfi_def_cfa_offset 784
	.cfi_offset %rbx, -56
	.cfi_offset %r12, -48
	.cfi_offset %r13, -40
	.cfi_offset %r14, -32
	.cfi_offset %r15, -24
	.cfi_offset %rbp, -16
	movq	%rdi, 88(%rsp)                  # 8-byte Spill
	xorps	%xmm0, %xmm0
	movaps	%xmm0, 192(%rsp)
	movaps	%xmm0, 176(%rsp)
	movaps	%xmm0, 160(%rsp)
	movaps	%xmm0, 144(%rsp)
	movaps	%xmm0, 128(%rsp)
	movaps	%xmm0, 112(%rsp)
	movaps	%xmm0, 96(%rsp)
	.cfi_escape 0x2e, 0x00
	leaq	96(%rsp), %rdi
	callq	sysinfo@PLT
	movl	%eax, 12(%rsp)                  # 4-byte Spill
	.cfi_escape 0x2e, 0x00
	leaq	.L.str(%rip), %rdi
	callq	_ZN12_GLOBAL__N_111read_mem_kbEPKc
	movq	%rax, 80(%rsp)                  # 8-byte Spill
	.cfi_escape 0x2e, 0x00
	leaq	.L.str.1(%rip), %rdi
	callq	_ZN12_GLOBAL__N_111read_mem_kbEPKc
	movq	%rax, 72(%rsp)                  # 8-byte Spill
	movq	$-1, %r15
	leaq	208(%rsp), %r14
	leaq	.L.str.6(%rip), %r12
	xorl	%ebx, %ebx
	jmp	.LBB0_1
	.p2align	4, 0x90
.LBB0_3:                                #   in Loop: Header=BB0_1 Depth=1
	incl	%ebx
	cmpl	$64, %ebx
	je	.LBB0_4
.LBB0_1:                                # =>This Inner Loop Header: Depth=1
	.cfi_escape 0x2e, 0x00
	movl	$96, %esi
	movq	%r14, %rdi
	leaq	.L.str.8(%rip), %rdx
	movl	%ebx, %ecx
	xorl	%eax, %eax
	callq	snprintf@PLT
	.cfi_escape 0x2e, 0x00
	movq	%r14, %rdi
	movq	%r12, %rsi
	callq	fopen@PLT
	testq	%rax, %rax
	je	.LBB0_3
# %bb.2:                                #   in Loop: Header=BB0_1 Depth=1
	movq	%rax, %rbp
	movq	$-1, (%rsp)
	.cfi_escape 0x2e, 0x00
	movq	%rax, %rdi
	leaq	.L.str.9(%rip), %rsi
	movq	%rsp, %rdx
	xorl	%eax, %eax
	callq	__isoc23_fscanf@PLT
	movl	%eax, %r13d
	.cfi_escape 0x2e, 0x00
	movq	%rbp, %rdi
	callq	fclose@PLT
	movq	(%rsp), %rax
	leaq	-1000(%rax), %rcx
	cmpq	%rax, %r15
	cmovgq	%r15, %rax
	cmpq	$199001, %rcx                   # imm = 0x30959
	cmovaeq	%r15, %rax
	cmpl	$1, %r13d
	cmoveq	%rax, %r15
	jmp	.LBB0_3
.LBB0_4:
	.cfi_escape 0x2e, 0x00
	leaq	_ZN12_GLOBAL__N_16g_lockE(%rip), %rdi
	callq	pthread_mutex_lock@PLT
	testl	%eax, %eax
	jne	.LBB0_14
# %bb.5:
	.cfi_escape 0x2e, 0x00
	leaq	.L.str.11(%rip), %rdi
	leaq	.L.str.6(%rip), %rsi
	callq	fopen@PLT
	movsd	.LCPI0_0(%rip), %xmm0           # xmm0 = [-1.0E+0,0.0E+0]
	movsd	%xmm0, 16(%rsp)                 # 8-byte Spill
	testq	%rax, %rax
	je	.LBB0_13
# %bb.6:
	movq	%rax, %rbx
	movq	$0, 208(%rsp)
	movq	$0, (%rsp)
	movq	$0, 64(%rsp)
	movq	$0, 56(%rsp)
	movq	$0, 48(%rsp)
	movq	$0, 40(%rsp)
	movq	$0, 32(%rsp)
	movq	$0, 24(%rsp)
.Ltmp0:
	.cfi_escape 0x2e, 0x20
	leaq	24(%rsp), %r10
	leaq	32(%rsp), %r11
	leaq	40(%rsp), %r14
	leaq	48(%rsp), %r12
	leaq	.L.str.12(%rip), %rsi
	leaq	208(%rsp), %rdx
	movq	%rsp, %rcx
	leaq	64(%rsp), %r8
	leaq	56(%rsp), %r9
	movq	%rax, %rdi
	xorl	%eax, %eax
	pushq	%r10
	.cfi_adjust_cfa_offset 8
	pushq	%r11
	.cfi_adjust_cfa_offset 8
	pushq	%r14
	.cfi_adjust_cfa_offset 8
	pushq	%r12
	.cfi_adjust_cfa_offset 8
	callq	__isoc23_fscanf@PLT
	addq	$32, %rsp
	.cfi_adjust_cfa_offset -32
.Ltmp1:
# %bb.7:
	movl	%eax, %ebp
	.cfi_escape 0x2e, 0x00
	movq	%rbx, %rdi
	callq	fclose@PLT
	cmpl	$3, %ebp
	jle	.LBB0_13
# %bb.8:
	movq	48(%rsp), %rax
	addq	56(%rsp), %rax
	movq	208(%rsp), %rcx
	addq	%rax, %rcx
	addq	(%rsp), %rcx
	addq	64(%rsp), %rcx
	addq	40(%rsp), %rcx
	addq	32(%rsp), %rcx
	addq	24(%rsp), %rcx
	movq	_ZN12_GLOBAL__N_112g_prev_totalE(%rip), %rsi
	movsd	.LCPI0_0(%rip), %xmm5           # xmm5 = [-1.0E+0,0.0E+0]
	testq	%rsi, %rsi
	je	.LBB0_12
# %bb.9:
	movq	%rcx, %rdx
	subq	%rsi, %rdx
	jbe	.LBB0_12
# %bb.10:
	movq	_ZN12_GLOBAL__N_111g_prev_idleE(%rip), %rsi
	cmpq	%rsi, %rax
	jb	.LBB0_12
# %bb.11:
	movq	%rdx, %xmm0
	subq	%rax, %rdx
	addq	%rsi, %rdx
	movq	%rdx, %xmm1
	movq	.LCPI0_1(%rip), %xmm2           # xmm2 = [1127219200,1160773632,0,0]
	punpckldq	%xmm2, %xmm1            # xmm1 = xmm1[0],xmm2[0],xmm1[1],xmm2[1]
	movapd	.LCPI0_2(%rip), %xmm3           # xmm3 = [4.503599627370496E+15,1.9342813113834067E+25]
	subpd	%xmm3, %xmm1
	movapd	%xmm1, %xmm4
	unpckhpd	%xmm1, %xmm4                    # xmm4 = xmm4[1],xmm1[1]
	addsd	%xmm1, %xmm4
	movsd	.LCPI0_3(%rip), %xmm5           # xmm5 = [1.0E+2,0.0E+0]
	mulsd	%xmm5, %xmm4
	punpckldq	%xmm2, %xmm0            # xmm0 = xmm0[0],xmm2[0],xmm0[1],xmm2[1]
	subpd	%xmm3, %xmm0
	movapd	%xmm0, %xmm1
	unpckhpd	%xmm0, %xmm1                    # xmm1 = xmm1[1],xmm0[1]
	addsd	%xmm0, %xmm1
	divsd	%xmm1, %xmm4
	minsd	%xmm4, %xmm5
.LBB0_12:
	movsd	%xmm5, 16(%rsp)                 # 8-byte Spill
	movq	%rcx, _ZN12_GLOBAL__N_112g_prev_totalE(%rip)
	movq	%rax, _ZN12_GLOBAL__N_111g_prev_idleE(%rip)
.LBB0_13:
	.cfi_escape 0x2e, 0x00
	leaq	_ZN12_GLOBAL__N_16g_lockE(%rip), %rdi
	callq	pthread_mutex_unlock@PLT
	xorl	%r10d, %r10d
	movl	12(%rsp), %ecx                  # 4-byte Reload
	movl	%ecx, %eax
	negl	%eax
	sbbq	%r10, %r10
	orq	96(%rsp), %r10
	testl	%ecx, %ecx
	leaq	.L.str.3(%rip), %rax
	leaq	.L.str.4(%rip), %r11
	cmoveq	%rax, %r11
	.cfi_escape 0x2e, 0x10
	leaq	.L.str.2(%rip), %rdx
	leaq	208(%rsp), %rbx
	movl	$512, %esi                      # imm = 0x200
	movq	%rbx, %rdi
	movq	80(%rsp), %rcx                  # 8-byte Reload
	movq	72(%rsp), %r8                   # 8-byte Reload
	movsd	16(%rsp), %xmm0                 # 8-byte Reload
                                        # xmm0 = mem[0],zero
	movq	%r15, %r9
	movb	$1, %al
	pushq	%r11
	.cfi_adjust_cfa_offset 8
	pushq	%r10
	.cfi_adjust_cfa_offset 8
	callq	snprintf@PLT
	addq	$16, %rsp
	.cfi_adjust_cfa_offset -16
	movq	88(%rsp), %rdi                  # 8-byte Reload
	movq	(%rdi), %rax
	.cfi_escape 0x2e, 0x00
	movq	%rbx, %rsi
	callq	*1336(%rax)
	addq	$728, %rsp                      # imm = 0x2D8
	.cfi_def_cfa_offset 56
	popq	%rbx
	.cfi_def_cfa_offset 48
	popq	%r12
	.cfi_def_cfa_offset 40
	popq	%r13
	.cfi_def_cfa_offset 32
	popq	%r14
	.cfi_def_cfa_offset 24
	popq	%r15
	.cfi_def_cfa_offset 16
	popq	%rbp
	.cfi_def_cfa_offset 8
	retq
.LBB0_14:
	.cfi_def_cfa_offset 784
	.cfi_escape 0x2e, 0x00
	movl	%eax, %edi
	callq	_ZSt20__throw_system_errori@PLT
.LBB0_15:
.Ltmp2:
	movq	%rax, %rbx
	.cfi_escape 0x2e, 0x00
	leaq	_ZN12_GLOBAL__N_16g_lockE(%rip), %rdi
	callq	pthread_mutex_unlock@PLT
	.cfi_escape 0x2e, 0x00
	movq	%rbx, %rdi
	callq	_Unwind_Resume@PLT
.Lfunc_end0:
	.size	Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample, .Lfunc_end0-Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample
	.cfi_endproc
	.section	.gcc_except_table.Java_frb_axeron_manager_core_AxEldenMonitor_nativeSample,"a",@progbits
	.p2align	2, 0x0
GCC_except_table0:
.Lexception0:
	.byte	255                             # @LPStart Encoding = omit
	.byte	255                             # @TType Encoding = omit
	.byte	1                               # Call site Encoding = uleb128
	.uleb128 .Lcst_end0-.Lcst_begin0
.Lcst_begin0:
	.uleb128 .Lfunc_begin0-.Lfunc_begin0    # >> Call Site 1 <<
	.uleb128 .Ltmp0-.Lfunc_begin0           #   Call between .Lfunc_begin0 and .Ltmp0
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp0-.Lfunc_begin0           # >> Call Site 2 <<
	.uleb128 .Ltmp1-.Ltmp0                  #   Call between .Ltmp0 and .Ltmp1
	.uleb128 .Ltmp2-.Lfunc_begin0           #     jumps to .Ltmp2
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp1-.Lfunc_begin0           # >> Call Site 3 <<
	.uleb128 .Lfunc_end0-.Ltmp1             #   Call between .Ltmp1 and .Lfunc_end0
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
.Lcst_end0:
	.p2align	2, 0x0
                                        # -- End function
	.section	.text._ZN12_GLOBAL__N_111read_mem_kbEPKc,"ax",@progbits
	.p2align	4, 0x90                         # -- Begin function _ZN12_GLOBAL__N_111read_mem_kbEPKc
	.type	_ZN12_GLOBAL__N_111read_mem_kbEPKc,@function
_ZN12_GLOBAL__N_111read_mem_kbEPKc:     # @_ZN12_GLOBAL__N_111read_mem_kbEPKc
	.cfi_startproc
# %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	pushq	%r15
	.cfi_def_cfa_offset 24
	pushq	%r14
	.cfi_def_cfa_offset 32
	pushq	%r13
	.cfi_def_cfa_offset 40
	pushq	%r12
	.cfi_def_cfa_offset 48
	pushq	%rbx
	.cfi_def_cfa_offset 56
	subq	$344, %rsp                      # imm = 0x158
	.cfi_def_cfa_offset 400
	.cfi_offset %rbx, -56
	.cfi_offset %r12, -48
	.cfi_offset %r13, -40
	.cfi_offset %r14, -32
	.cfi_offset %r15, -24
	.cfi_offset %rbp, -16
	movq	%rdi, %rbx
	leaq	.L.str.5(%rip), %rdi
	leaq	.L.str.6(%rip), %rsi
	callq	fopen@PLT
	testq	%rax, %rax
	je	.LBB1_8
# %bb.1:
	movq	%rax, %r14
	movq	$-1, 8(%rsp)
	leaq	80(%rsp), %rdi
	movl	$256, %esi                      # imm = 0x100
	movq	%rax, %rdx
	callq	fgets@PLT
	testq	%rax, %rax
	je	.LBB1_7
# %bb.2:
	leaq	.L.str.7(%rip), %r15
	leaq	80(%rsp), %r12
	leaq	16(%rsp), %r13
	leaq	8(%rsp), %rbp
	jmp	.LBB1_3
	.p2align	4, 0x90
.LBB1_6:                                #   in Loop: Header=BB1_3 Depth=1
	movq	%r12, %rdi
	movl	$256, %esi                      # imm = 0x100
	movq	%r14, %rdx
	callq	fgets@PLT
	testq	%rax, %rax
	je	.LBB1_7
.LBB1_3:                                # =>This Inner Loop Header: Depth=1
	movq	%r12, %rdi
	movq	%r15, %rsi
	movq	%r13, %rdx
	movq	%rbp, %rcx
	xorl	%eax, %eax
	callq	__isoc23_sscanf@PLT
	cmpl	$2, %eax
	jne	.LBB1_6
# %bb.4:                                #   in Loop: Header=BB1_3 Depth=1
	movq	%rbx, %rdi
	callq	strlen@PLT
	movq	%r13, %rdi
	movq	%rbx, %rsi
	movq	%rax, %rdx
	callq	strncmp@PLT
	testl	%eax, %eax
	jne	.LBB1_6
# %bb.5:
	movq	%r14, %rdi
	callq	fclose@PLT
	movq	8(%rsp), %rax
	jmp	.LBB1_9
.LBB1_7:
	movq	%r14, %rdi
	callq	fclose@PLT
.LBB1_8:
	movq	$-1, %rax
.LBB1_9:
	addq	$344, %rsp                      # imm = 0x158
	.cfi_def_cfa_offset 56
	popq	%rbx
	.cfi_def_cfa_offset 48
	popq	%r12
	.cfi_def_cfa_offset 40
	popq	%r13
	.cfi_def_cfa_offset 32
	popq	%r14
	.cfi_def_cfa_offset 24
	popq	%r15
	.cfi_def_cfa_offset 16
	popq	%rbp
	.cfi_def_cfa_offset 8
	retq
.Lfunc_end1:
	.size	_ZN12_GLOBAL__N_111read_mem_kbEPKc, .Lfunc_end1-_ZN12_GLOBAL__N_111read_mem_kbEPKc
	.cfi_endproc
                                        # -- End function
	.type	.L.str,@object                  # @.str
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str:
	.asciz	"MemTotal:"
	.size	.L.str, 10

	.type	.L.str.1,@object                # @.str.1
.L.str.1:
	.asciz	"MemAvailable:"
	.size	.L.str.1, 14

	.type	_ZN12_GLOBAL__N_112g_prev_totalE,@object # @_ZN12_GLOBAL__N_112g_prev_totalE
	.section	.bss._ZN12_GLOBAL__N_112g_prev_totalE,"aw",@nobits
	.p2align	3, 0x0
_ZN12_GLOBAL__N_112g_prev_totalE:
	.quad	0                               # 0x0
	.size	_ZN12_GLOBAL__N_112g_prev_totalE, 8

	.type	_ZN12_GLOBAL__N_111g_prev_idleE,@object # @_ZN12_GLOBAL__N_111g_prev_idleE
	.section	.bss._ZN12_GLOBAL__N_111g_prev_idleE,"aw",@nobits
	.p2align	3, 0x0
_ZN12_GLOBAL__N_111g_prev_idleE:
	.quad	0                               # 0x0
	.size	_ZN12_GLOBAL__N_111g_prev_idleE, 8

	.type	.L.str.2,@object                # @.str.2
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str.2:
	.asciz	"{\"mem_total_kb\":%ld,\"mem_available_kb\":%ld,\"cpu_busy_percent\":%.2f,\"thermal_milli_c\":%ld,\"uptime_seconds\":%ld,\"native_ok\":%s}"
	.size	.L.str.2, 126

	.type	.L.str.3,@object                # @.str.3
.L.str.3:
	.asciz	"true"
	.size	.L.str.3, 5

	.type	.L.str.4,@object                # @.str.4
.L.str.4:
	.asciz	"false"
	.size	.L.str.4, 6

	.type	.L.str.5,@object                # @.str.5
.L.str.5:
	.asciz	"/proc/meminfo"
	.size	.L.str.5, 14

	.type	.L.str.6,@object                # @.str.6
.L.str.6:
	.asciz	"r"
	.size	.L.str.6, 2

	.type	.L.str.7,@object                # @.str.7
.L.str.7:
	.asciz	"%63s %ld"
	.size	.L.str.7, 9

	.type	.L.str.8,@object                # @.str.8
.L.str.8:
	.asciz	"/sys/class/thermal/thermal_zone%d/temp"
	.size	.L.str.8, 39

	.type	.L.str.9,@object                # @.str.9
.L.str.9:
	.asciz	"%ld"
	.size	.L.str.9, 4

	.type	_ZN12_GLOBAL__N_16g_lockE,@object # @_ZN12_GLOBAL__N_16g_lockE
	.section	.bss._ZN12_GLOBAL__N_16g_lockE,"aw",@nobits
	.p2align	3, 0x0
_ZN12_GLOBAL__N_16g_lockE:
	.zero	40
	.size	_ZN12_GLOBAL__N_16g_lockE, 40

	.type	.L.str.11,@object               # @.str.11
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str.11:
	.asciz	"/proc/stat"
	.size	.L.str.11, 11

	.type	.L.str.12,@object               # @.str.12
.L.str.12:
	.asciz	"cpu %llu %llu %llu %llu %llu %llu %llu %llu"
	.size	.L.str.12, 44

	.hidden	DW.ref.__gxx_personality_v0
	.weak	DW.ref.__gxx_personality_v0
	.section	.data.DW.ref.__gxx_personality_v0,"awG",@progbits,DW.ref.__gxx_personality_v0,comdat
	.p2align	3, 0x0
	.type	DW.ref.__gxx_personality_v0,@object
	.size	DW.ref.__gxx_personality_v0, 8
DW.ref.__gxx_personality_v0:
	.quad	__gxx_personality_v0
	.ident	"clang version 17.0.0 (https://github.com/swiftlang/llvm-project.git 10999b6d034fe318f3d56c83bddb6572593a8bb0)"
	.section	".note.GNU-stack","",@progbits
	.addrsig
	.addrsig_sym __gxx_personality_v0
	.addrsig_sym _Unwind_Resume
	.addrsig_sym _ZN12_GLOBAL__N_16g_lockE
