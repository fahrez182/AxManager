	.text
	.file	"axelden_live.cpp"
	.section	.text.Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot,"ax",@progbits
	.globl	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot # -- Begin function Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot
	.p2align	4, 0x90
	.type	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot,@function
Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot: # @Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot
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
	subq	$1192, %rsp                     # imm = 0x4A8
	.cfi_def_cfa_offset 1248
	.cfi_offset %rbx, -56
	.cfi_offset %r12, -48
	.cfi_offset %r13, -40
	.cfi_offset %r14, -32
	.cfi_offset %r15, -24
	.cfi_offset %rbp, -16
	movq	%rdi, %rbx
	xorps	%xmm0, %xmm0
	movaps	%xmm0, 784(%rsp)
	movaps	%xmm0, 768(%rsp)
	movaps	%xmm0, 752(%rsp)
	movaps	%xmm0, 736(%rsp)
	movaps	%xmm0, 720(%rsp)
	movaps	%xmm0, 704(%rsp)
	movaps	%xmm0, 688(%rsp)
	leaq	688(%rsp), %rdi
	callq	sysinfo@PLT
	leaq	56(%rsp), %rax
	movq	%rax, 40(%rsp)
	movabsq	$7308549874410221615, %rax      # imm = 0x656D2F636F72702F
	movq	%rax, 56(%rsp)
	movabsq	$8027224784920472879, %rax      # imm = 0x6F666E696D656D2F
	movq	%rax, 61(%rsp)
	movq	$13, 48(%rsp)
	movb	$0, 69(%rsp)
.Ltmp0:
	leaq	312(%rsp), %rdi
	leaq	40(%rsp), %rsi
	callq	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Ltmp1:
# %bb.1:
.Ltmp3:
	leaq	808(%rsp), %rdi
	leaq	312(%rsp), %rsi
	movl	$8, %edx
	callq	_ZNSt7__cxx1119basic_istringstreamIcSt11char_traitsIcESaIcEEC1ERKNS_12basic_stringIcS2_S3_EESt13_Ios_Openmode@PLT
.Ltmp4:
# %bb.2:
	movq	312(%rsp), %rdi
	leaq	328(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_4
# %bb.3:
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_4:
	movq	%rbx, 304(%rsp)                 # 8-byte Spill
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_6
# %bb.5:
	movq	56(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_6:
	leaq	120(%rsp), %rax
	movq	%rax, 104(%rsp)
	movq	$0, 112(%rsp)
	movb	$0, 120(%rsp)
	leaq	200(%rsp), %rax
	movq	%rax, 184(%rsp)
	movq	$0, 192(%rsp)
	movb	$0, 200(%rsp)
	movq	$0, 248(%rsp)                   # 8-byte Folded Spill
	leaq	808(%rsp), %rbx
	leaq	104(%rsp), %r14
	leaq	296(%rsp), %r15
	leaq	184(%rsp), %r12
	movabsq	$4207888596644686177, %rbp      # imm = 0x3A656C62616C6961
	movabsq	$7809651250343798093, %r13      # imm = 0x6C61746F546D654D
	movq	$0, 256(%rsp)                   # 8-byte Folded Spill
	.p2align	4, 0x90
.LBB0_7:                                # =>This Inner Loop Header: Depth=1
.Ltmp6:
	movq	%rbx, %rdi
	movq	%r14, %rsi
	callq	_ZStrsIcSt11char_traitsIcESaIcEERSt13basic_istreamIT_T0_ES7_RNSt7__cxx1112basic_stringIS4_S5_T1_EE@PLT
.Ltmp7:
# %bb.8:                                #   in Loop: Header=BB0_7 Depth=1
.Ltmp8:
	movq	%rax, %rdi
	movq	%r15, %rsi
	callq	_ZNSi10_M_extractIxEERSiRT_@PLT
.Ltmp9:
# %bb.9:                                #   in Loop: Header=BB0_7 Depth=1
.Ltmp10:
	movq	%rax, %rdi
	movq	%r12, %rsi
	callq	_ZStrsIcSt11char_traitsIcESaIcEERSt13basic_istreamIT_T0_ES7_RNSt7__cxx1112basic_stringIS4_S5_T1_EE@PLT
.Ltmp11:
# %bb.10:                               #   in Loop: Header=BB0_7 Depth=1
	movq	(%rax), %rcx
	movq	-24(%rcx), %rcx
	testb	$5, 32(%rax,%rcx)
	jne	.LBB0_17
# %bb.11:                               #   in Loop: Header=BB0_7 Depth=1
	movq	112(%rsp), %rax
	cmpq	$13, %rax
	je	.LBB0_15
# %bb.12:                               #   in Loop: Header=BB0_7 Depth=1
	cmpq	$9, %rax
	jne	.LBB0_7
# %bb.13:                               #   in Loop: Header=BB0_7 Depth=1
	movq	104(%rsp), %rax
	movq	(%rax), %rcx
	xorq	%r13, %rcx
	movzbl	8(%rax), %eax
	xorq	$58, %rax
	orq	%rcx, %rax
	jne	.LBB0_7
# %bb.14:                               #   in Loop: Header=BB0_7 Depth=1
	movq	296(%rsp), %rax
	movq	%rax, 256(%rsp)                 # 8-byte Spill
	jmp	.LBB0_7
.LBB0_15:                               #   in Loop: Header=BB0_7 Depth=1
	movq	104(%rsp), %rax
	movq	(%rax), %rcx
	movabsq	$7811882189182559565, %rdx      # imm = 0x6C696176416D654D
	xorq	%rdx, %rcx
	movq	5(%rax), %rax
	xorq	%rbp, %rax
	orq	%rcx, %rax
	jne	.LBB0_7
# %bb.16:                               #   in Loop: Header=BB0_7 Depth=1
	movq	296(%rsp), %rax
	movq	%rax, 248(%rsp)                 # 8-byte Spill
	jmp	.LBB0_7
.LBB0_17:
	leaq	328(%rsp), %rbp
	movq	%rbp, 312(%rsp)
	movabsq	$8028844339812790319, %rax      # imm = 0x6F6C2F636F72702F
	movq	%rax, 328(%rsp)
	movabsq	$7455253316917750831, %rax      # imm = 0x67766164616F6C2F
	movq	%rax, 333(%rsp)
	movq	$13, 320(%rsp)
	movb	$0, 341(%rsp)
.Ltmp13:
	leaq	264(%rsp), %rdi
	leaq	312(%rsp), %rsi
	callq	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Ltmp14:
# %bb.18:
	movq	312(%rsp), %rdi
	cmpq	%rbp, %rdi
	je	.LBB0_20
# %bb.19:
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_20:
	cmpq	$0, 272(%rsp)
	je	.LBB0_23
# %bb.21:
	leaq	264(%rsp), %rdi
	movl	$32, %esi
	xorl	%edx, %edx
	callq	_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE4findEcm@PLT
	cmpq	$-1, %rax
	je	.LBB0_23
# %bb.22:
.Ltmp16:
	leaq	264(%rsp), %rdi
	movq	%rax, %rsi
	xorl	%edx, %edx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE6resizeEmc@PLT
.Ltmp17:
.LBB0_23:
	leaq	.L.str.12(%rip), %rdi
	callq	opendir@PLT
	movq	$-1, %r14
	movq	$-1, 144(%rsp)                  # 8-byte Folded Spill
	testq	%rax, %rax
	je	.LBB0_80
# %bb.24:
	movq	%rax, %rbx
	leaq	88(%rsp), %r14
	movq	$-1, 144(%rsp)                  # 8-byte Folded Spill
	leaq	8(%rsp), %r12
	jmp	.LBB0_26
	.p2align	4, 0x90
.LBB0_38:                               #   in Loop: Header=BB0_26 Depth=1
	xorl	%r13d, %r13d
	leaq	328(%rsp), %rbp
	cmpq	%rbp, %r15
	je	.LBB0_25
.LBB0_78:                               #   in Loop: Header=BB0_26 Depth=1
	movq	328(%rsp), %rsi
	incq	%rsi
	movq	%r15, %rdi
	callq	_ZdlPvm@PLT
.LBB0_25:                               #   in Loop: Header=BB0_26 Depth=1
	testb	%r13b, %r13b
	jne	.LBB0_79
.LBB0_26:                               # =>This Inner Loop Header: Depth=1
.Ltmp19:
	movq	%rbx, %rdi
	callq	readdir@PLT
.Ltmp20:
# %bb.27:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%rax, %r15
	testq	%rax, %rax
	je	.LBB0_79
# %bb.28:                               #   in Loop: Header=BB0_26 Depth=1
	addq	$19, %r15
	movq	%rbp, 312(%rsp)
	movq	%r15, %rdi
	callq	strlen@PLT
	movq	%rax, %r13
	movq	%rax, 40(%rsp)
	movq	%rbp, %rax
	cmpq	$16, %r13
	jb	.LBB0_31
# %bb.29:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp21:
	leaq	312(%rsp), %rdi
	leaq	40(%rsp), %rsi
	xorl	%edx, %edx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_createERmm@PLT
.Ltmp22:
# %bb.30:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%rax, 312(%rsp)
	movq	40(%rsp), %rcx
	movq	%rcx, 328(%rsp)
.LBB0_31:                               #   in Loop: Header=BB0_26 Depth=1
	testq	%r13, %r13
	je	.LBB0_35
# %bb.32:                               #   in Loop: Header=BB0_26 Depth=1
	cmpq	$1, %r13
	jne	.LBB0_34
# %bb.33:                               #   in Loop: Header=BB0_26 Depth=1
	movzbl	(%r15), %ecx
	movb	%cl, (%rax)
	jmp	.LBB0_35
	.p2align	4, 0x90
.LBB0_34:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%rax, %rdi
	movq	%r15, %rsi
	movq	%r13, %rdx
	callq	memcpy@PLT
.LBB0_35:                               #   in Loop: Header=BB0_26 Depth=1
	movq	40(%rsp), %rax
	movq	%rax, 320(%rsp)
	movq	312(%rsp), %rcx
	movb	$0, (%rcx,%rax)
	movq	312(%rsp), %r15
	movq	320(%rsp), %rbp
	cmpq	$2, %rbp
	je	.LBB0_39
# %bb.36:                               #   in Loop: Header=BB0_26 Depth=1
	cmpq	$1, %rbp
	jne	.LBB0_40
# %bb.37:                               #   in Loop: Header=BB0_26 Depth=1
	cmpb	$46, (%r15)
	jne	.LBB0_40
	jmp	.LBB0_38
	.p2align	4, 0x90
.LBB0_39:                               #   in Loop: Header=BB0_26 Depth=1
	cmpw	$11822, (%r15)                  # imm = 0x2E2E
	je	.LBB0_38
.LBB0_40:                               #   in Loop: Header=BB0_26 Depth=1
	leaq	24(%rsp), %rax
	movq	%rax, 8(%rsp)
	movq	$0, 16(%rsp)
	movb	$0, 24(%rsp)
	leaq	24(%rbp), %rsi
.Ltmp24:
	movq	%r12, %rdi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE7reserveEm@PLT
.Ltmp25:
# %bb.41:                               #   in Loop: Header=BB0_26 Depth=1
	movq	16(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	addq	$19, %rax
	cmpq	$24, %rax
	jb	.LBB0_149
# %bb.42:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp26:
	movl	$24, %edx
	movq	%r12, %rdi
	leaq	.L.str.15(%rip), %rsi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp27:
# %bb.43:                               #   in Loop: Header=BB0_26 Depth=1
	movabsq	$4611686018427387903, %rax      # imm = 0x3FFFFFFFFFFFFFFF
	subq	16(%rsp), %rax
	cmpq	%rbp, %rax
	jb	.LBB0_149
# %bb.44:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp28:
	movq	%r12, %rdi
	movq	%r15, %rsi
	movq	%rbp, %rdx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp29:
# %bb.45:                               #   in Loop: Header=BB0_26 Depth=1
	movq	16(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	cmpq	$4, %rax
	jbe	.LBB0_155
# %bb.46:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp31:
	movl	$5, %edx
	movq	%r12, %rdi
	leaq	.L.str.16(%rip), %rsi
	leaq	328(%rsp), %rbp
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp32:
# %bb.47:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%r14, 72(%rsp)
	movq	(%rax), %rcx
	movq	%rax, %r15
	addq	$16, %r15
	cmpq	%r15, %rcx
	je	.LBB0_49
# %bb.48:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%rcx, 72(%rsp)
	movq	16(%rax), %rcx
	movq	%rcx, 88(%rsp)
	movq	8(%rax), %r13
	jmp	.LBB0_50
	.p2align	4, 0x90
.LBB0_49:                               #   in Loop: Header=BB0_26 Depth=1
	movq	8(%rax), %r13
	leaq	1(%r13), %rdx
	movq	%r14, %rdi
	movq	%r15, %rsi
	movq	%rax, %rbp
	callq	memcpy@PLT
	movq	%rbp, %rax
	leaq	328(%rsp), %rbp
.LBB0_50:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%r13, 80(%rsp)
	movq	%r15, (%rax)
	movq	$0, 8(%rax)
	movb	$0, 16(%rax)
.Ltmp34:
	leaq	40(%rsp), %rdi
	leaq	72(%rsp), %rsi
	callq	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Ltmp35:
# %bb.51:                               #   in Loop: Header=BB0_26 Depth=1
	movq	72(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_53
# %bb.52:                               #   in Loop: Header=BB0_26 Depth=1
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_53:                               #   in Loop: Header=BB0_26 Depth=1
	movq	8(%rsp), %rdi
	leaq	24(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_55
# %bb.54:                               #   in Loop: Header=BB0_26 Depth=1
	movq	24(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_55:                               #   in Loop: Header=BB0_26 Depth=1
	xorl	%r13d, %r13d
	movl	$7, %ecx
	leaq	40(%rsp), %rdi
	leaq	.L.str.17(%rip), %rsi
	xorl	%edx, %edx
	callq	_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE4findEPKcmm@PLT
	cmpq	$-1, %rax
	je	.LBB0_75
# %bb.56:                               #   in Loop: Header=BB0_26 Depth=1
	movq	312(%rsp), %r15
	movq	320(%rsp), %r13
	leaq	168(%rsp), %rax
	movq	%rax, 152(%rsp)
	movq	$0, 160(%rsp)
	movb	$0, 168(%rsp)
	leaq	24(%r13), %rsi
.Ltmp37:
	leaq	152(%rsp), %rdi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE7reserveEm@PLT
.Ltmp38:
# %bb.57:                               #   in Loop: Header=BB0_26 Depth=1
	movq	160(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	addq	$19, %rax
	cmpq	$24, %rax
	jb	.LBB0_153
# %bb.58:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp39:
	movl	$24, %edx
	leaq	152(%rsp), %rdi
	leaq	.L.str.15(%rip), %rsi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp40:
# %bb.59:                               #   in Loop: Header=BB0_26 Depth=1
	movabsq	$4611686018427387903, %rax      # imm = 0x3FFFFFFFFFFFFFFF
	subq	160(%rsp), %rax
	cmpq	%r13, %rax
	jb	.LBB0_153
# %bb.60:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp41:
	leaq	152(%rsp), %rdi
	movq	%r15, %rsi
	movq	%r13, %rdx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp42:
# %bb.61:                               #   in Loop: Header=BB0_26 Depth=1
	movq	160(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	addq	$4, %rax
	cmpq	$8, %rax
	jbe	.LBB0_159
# %bb.62:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp44:
	movl	$9, %edx
	leaq	152(%rsp), %rdi
	leaq	.L.str.18(%rip), %rsi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp45:
# %bb.63:                               #   in Loop: Header=BB0_26 Depth=1
	leaq	24(%rsp), %rdi
	movq	%rdi, 8(%rsp)
	movq	(%rax), %rcx
	movq	%rax, %r15
	addq	$16, %r15
	cmpq	%r15, %rcx
	je	.LBB0_65
# %bb.64:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%rcx, 8(%rsp)
	movq	16(%rax), %rcx
	movq	%rcx, 24(%rsp)
	movq	8(%rax), %r13
	jmp	.LBB0_66
.LBB0_65:                               #   in Loop: Header=BB0_26 Depth=1
	movq	8(%rax), %r13
	leaq	1(%r13), %rdx
	movq	%r15, %rsi
	movq	%rax, %rbp
	callq	memcpy@PLT
	movq	%rbp, %rax
	leaq	328(%rsp), %rbp
.LBB0_66:                               #   in Loop: Header=BB0_26 Depth=1
	movq	%r13, 16(%rsp)
	movq	%r15, (%rax)
	movq	$0, 8(%rax)
	movb	$0, 16(%rax)
.Ltmp47:
	leaq	72(%rsp), %r15
	movq	%r15, %rdi
	movq	%r12, %rsi
	callq	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Ltmp48:
# %bb.67:                               #   in Loop: Header=BB0_26 Depth=1
.Ltmp50:
	movq	%r15, %rdi
	callq	_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx
	movq	%rax, 144(%rsp)                 # 8-byte Spill
.Ltmp51:
# %bb.68:                               #   in Loop: Header=BB0_26 Depth=1
	movq	72(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_70
# %bb.69:                               #   in Loop: Header=BB0_26 Depth=1
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_70:                               #   in Loop: Header=BB0_26 Depth=1
	movq	8(%rsp), %rdi
	leaq	24(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_72
# %bb.71:                               #   in Loop: Header=BB0_26 Depth=1
	movq	24(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_72:                               #   in Loop: Header=BB0_26 Depth=1
	movq	152(%rsp), %rdi
	leaq	168(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_74
# %bb.73:                               #   in Loop: Header=BB0_26 Depth=1
	movq	168(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_74:                               #   in Loop: Header=BB0_26 Depth=1
	cmpq	$0, 144(%rsp)                   # 8-byte Folded Reload
	setns	%r13b
.LBB0_75:                               #   in Loop: Header=BB0_26 Depth=1
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_77
# %bb.76:                               #   in Loop: Header=BB0_26 Depth=1
	movq	56(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_77:                               #   in Loop: Header=BB0_26 Depth=1
	movq	312(%rsp), %r15
	cmpq	%rbp, %r15
	jne	.LBB0_78
	jmp	.LBB0_25
.LBB0_79:
	movq	%rbx, %rdi
	callq	closedir@PLT
	movq	$-1, %r14
.LBB0_80:
	leaq	.L.str.21(%rip), %rdi
	callq	opendir@PLT
	testq	%rax, %rax
	je	.LBB0_118
# %bb.81:
.Ltmp65:
	movq	%rax, %rbx
	movq	%rax, %rdi
	callq	readdir@PLT
.Ltmp66:
# %bb.82:
	movq	%rax, %rbp
	testq	%rax, %rax
	je	.LBB0_116
# %bb.83:
	leaq	328(%rsp), %r12
	leaq	24(%rsp), %r15
	movq	$-1, 136(%rsp)                  # 8-byte Folded Spill
	leaq	8(%rsp), %r14
	.p2align	4, 0x90
.LBB0_84:                               # =>This Inner Loop Header: Depth=1
	addq	$19, %rbp
	movq	%r12, 312(%rsp)
	movq	%rbp, %rdi
	callq	strlen@PLT
	movq	%rax, %r13
	movq	%rax, 40(%rsp)
	movq	%r12, %rax
	cmpq	$16, %r13
	jb	.LBB0_87
# %bb.85:                               #   in Loop: Header=BB0_84 Depth=1
.Ltmp68:
	leaq	312(%rsp), %rdi
	leaq	40(%rsp), %rsi
	xorl	%edx, %edx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_createERmm@PLT
.Ltmp69:
# %bb.86:                               #   in Loop: Header=BB0_84 Depth=1
	movq	%rax, 312(%rsp)
	movq	40(%rsp), %rcx
	movq	%rcx, 328(%rsp)
.LBB0_87:                               #   in Loop: Header=BB0_84 Depth=1
	testq	%r13, %r13
	je	.LBB0_91
# %bb.88:                               #   in Loop: Header=BB0_84 Depth=1
	cmpq	$1, %r13
	jne	.LBB0_90
# %bb.89:                               #   in Loop: Header=BB0_84 Depth=1
	movzbl	(%rbp), %ecx
	movb	%cl, (%rax)
	jmp	.LBB0_91
	.p2align	4, 0x90
.LBB0_90:                               #   in Loop: Header=BB0_84 Depth=1
	movq	%rax, %rdi
	movq	%rbp, %rsi
	movq	%r13, %rdx
	callq	memcpy@PLT
.LBB0_91:                               #   in Loop: Header=BB0_84 Depth=1
	movq	40(%rsp), %rax
	movq	%rax, 320(%rsp)
	movq	312(%rsp), %rcx
	movb	$0, (%rcx,%rax)
	movl	$12, %ecx
	leaq	312(%rsp), %rdi
	leaq	.L.str.22(%rip), %rsi
	xorl	%edx, %edx
	callq	_ZNKSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE5rfindEPKcmm@PLT
	testq	%rax, %rax
	jne	.LBB0_112
# %bb.92:                               #   in Loop: Header=BB0_84 Depth=1
	movq	312(%rsp), %rbp
	movq	320(%rsp), %r13
	movq	%r15, 8(%rsp)
	movq	$0, 16(%rsp)
	movb	$0, 24(%rsp)
	leaq	19(%r13), %rsi
.Ltmp70:
	movq	%r14, %rdi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE7reserveEm@PLT
.Ltmp71:
# %bb.93:                               #   in Loop: Header=BB0_84 Depth=1
	movq	16(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	addq	$14, %rax
	cmpq	$19, %rax
	jb	.LBB0_151
# %bb.94:                               #   in Loop: Header=BB0_84 Depth=1
.Ltmp72:
	movl	$19, %edx
	movq	%r14, %rdi
	leaq	.L.str.23(%rip), %rsi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp73:
# %bb.95:                               #   in Loop: Header=BB0_84 Depth=1
	movabsq	$4611686018427387903, %rax      # imm = 0x3FFFFFFFFFFFFFFF
	subq	16(%rsp), %rax
	cmpq	%r13, %rax
	jb	.LBB0_151
# %bb.96:                               #   in Loop: Header=BB0_84 Depth=1
.Ltmp74:
	movq	%r14, %rdi
	movq	%rbp, %rsi
	movq	%r13, %rdx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp75:
# %bb.97:                               #   in Loop: Header=BB0_84 Depth=1
	movq	16(%rsp), %rax
	movabsq	$-4611686018427387899, %rcx     # imm = 0xC000000000000005
	addq	%rcx, %rax
	cmpq	$4, %rax
	jbe	.LBB0_157
# %bb.98:                               #   in Loop: Header=BB0_84 Depth=1
.Ltmp77:
	movl	$5, %edx
	movq	%r14, %rdi
	leaq	.L.str.24(%rip), %rsi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_appendEPKcm@PLT
.Ltmp78:
# %bb.99:                               #   in Loop: Header=BB0_84 Depth=1
	leaq	88(%rsp), %rdi
	movq	%rdi, 72(%rsp)
	movq	(%rax), %rcx
	movq	%rax, %r13
	addq	$16, %r13
	cmpq	%r13, %rcx
	je	.LBB0_101
# %bb.100:                              #   in Loop: Header=BB0_84 Depth=1
	movq	%rcx, 72(%rsp)
	movq	16(%rax), %rcx
	movq	%rcx, 88(%rsp)
	movq	8(%rax), %rbp
	jmp	.LBB0_102
.LBB0_101:                              #   in Loop: Header=BB0_84 Depth=1
	movq	8(%rax), %rbp
	leaq	1(%rbp), %rdx
	movq	%r13, %rsi
	movq	%rax, %r15
	callq	memcpy@PLT
	movq	%r15, %rax
	leaq	24(%rsp), %r15
.LBB0_102:                              #   in Loop: Header=BB0_84 Depth=1
	movq	%rbp, 80(%rsp)
	movq	%r13, (%rax)
	movq	$0, 8(%rax)
	movb	$0, 16(%rax)
.Ltmp80:
	leaq	40(%rsp), %r13
	movq	%r13, %rdi
	leaq	72(%rsp), %rsi
	callq	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Ltmp81:
# %bb.103:                              #   in Loop: Header=BB0_84 Depth=1
.Ltmp83:
	movq	%r13, %rdi
	callq	_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx
.Ltmp84:
# %bb.104:                              #   in Loop: Header=BB0_84 Depth=1
	movq	%rax, %rbp
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_106
# %bb.105:                              #   in Loop: Header=BB0_84 Depth=1
	movq	56(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_106:                              #   in Loop: Header=BB0_84 Depth=1
	movq	72(%rsp), %rdi
	leaq	88(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_108
# %bb.107:                              #   in Loop: Header=BB0_84 Depth=1
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_108:                              #   in Loop: Header=BB0_84 Depth=1
	movq	8(%rsp), %rdi
	cmpq	%r15, %rdi
	je	.LBB0_110
# %bb.109:                              #   in Loop: Header=BB0_84 Depth=1
	movq	24(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_110:                              #   in Loop: Header=BB0_84 Depth=1
	leaq	-1000(%rbp), %rax
	cmpq	$199000, %rax                   # imm = 0x30958
	ja	.LBB0_112
# %bb.111:                              #   in Loop: Header=BB0_84 Depth=1
	movq	136(%rsp), %rax                 # 8-byte Reload
	cmpq	%rbp, %rax
	cmovleq	%rbp, %rax
	movq	%rax, 136(%rsp)                 # 8-byte Spill
	.p2align	4, 0x90
.LBB0_112:                              #   in Loop: Header=BB0_84 Depth=1
	movq	312(%rsp), %rdi
	cmpq	%r12, %rdi
	je	.LBB0_114
# %bb.113:                              #   in Loop: Header=BB0_84 Depth=1
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_114:                              #   in Loop: Header=BB0_84 Depth=1
.Ltmp86:
	movq	%rbx, %rdi
	callq	readdir@PLT
.Ltmp87:
# %bb.115:                              #   in Loop: Header=BB0_84 Depth=1
	movq	%rax, %rbp
	testq	%rax, %rax
	jne	.LBB0_84
	jmp	.LBB0_117
.LBB0_116:
	movq	$-1, 136(%rsp)                  # 8-byte Folded Spill
.LBB0_117:
	movq	%rbx, %rdi
	callq	closedir@PLT
	movq	136(%rsp), %r14                 # 8-byte Reload
.LBB0_118:
.Ltmp95:
	leaq	312(%rsp), %rdi
	callq	_ZNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEEC1Ev@PLT
.Ltmp96:
# %bb.119:
.Ltmp98:
	leaq	.L.str.4(%rip), %rsi
	leaq	312(%rsp), %rdi
	movl	$14, %edx
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp99:
# %bb.120:
	movq	688(%rsp), %rsi
.Ltmp100:
	leaq	312(%rsp), %rdi
	callq	_ZNSo9_M_insertIlEERSoT_@PLT
.Ltmp101:
# %bb.121:
.Ltmp102:
	movq	%rax, %rbx
	leaq	.L.str.5(%rip), %rsi
	movl	$16, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp103:
# %bb.122:
.Ltmp104:
	movq	%rbx, %rdi
	movq	256(%rsp), %rsi                 # 8-byte Reload
	callq	_ZNSo9_M_insertIxEERSoT_@PLT
.Ltmp105:
# %bb.123:
.Ltmp106:
	movq	%rax, %rbx
	leaq	.L.str.6(%rip), %rsi
	movl	$20, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp107:
# %bb.124:
.Ltmp108:
	movq	%rbx, %rdi
	movq	248(%rsp), %rsi                 # 8-byte Reload
	callq	_ZNSo9_M_insertIxEERSoT_@PLT
.Ltmp109:
# %bb.125:
.Ltmp110:
	movq	%rax, %rbx
	leaq	.L.str.7(%rip), %rsi
	movl	$19, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp111:
# %bb.126:
.Ltmp112:
	movq	%rbx, %rdi
	movq	144(%rsp), %rsi                 # 8-byte Reload
	callq	_ZNSo9_M_insertIxEERSoT_@PLT
.Ltmp113:
# %bb.127:
.Ltmp114:
	movq	%rax, %rbx
	leaq	.L.str.8(%rip), %rsi
	movl	$18, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp115:
# %bb.128:
.Ltmp116:
	movq	%rbx, %rdi
	movq	%r14, %rsi
	callq	_ZNSo9_M_insertIxEERSoT_@PLT
.Ltmp117:
# %bb.129:
.Ltmp118:
	movq	%rax, %rbx
	leaq	.L.str.9(%rip), %rsi
	movl	$10, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp119:
# %bb.130:
	movq	264(%rsp), %rsi
	movq	272(%rsp), %rdx
.Ltmp120:
	movq	%rbx, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp121:
# %bb.131:
.Ltmp122:
	leaq	.L.str.10(%rip), %rsi
	movl	$2, %edx
	movq	%rax, %rdi
	callq	_ZSt16__ostream_insertIcSt11char_traitsIcEERSt13basic_ostreamIT_T0_ES6_PKS3_l@PLT
.Ltmp123:
# %bb.132:
	leaq	232(%rsp), %r14
	movq	%r14, 216(%rsp)
	movq	$0, 224(%rsp)
	movq	344(%rsp), %r8
	movq	360(%rsp), %rax
	testq	%rax, %rax
	sete	%cl
	cmpq	%r8, %rax
	cmovaq	%rax, %r8
	movb	$0, 232(%rsp)
	testq	%r8, %r8
	sete	%al
	orb	%cl, %al
	je	.LBB0_134
# %bb.133:
	leaq	392(%rsp), %rsi
.Ltmp127:
	leaq	216(%rsp), %rdi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_assignERKS4_@PLT
.Ltmp128:
	jmp	.LBB0_135
.LBB0_134:
	movq	352(%rsp), %rcx
	subq	%rcx, %r8
.Ltmp125:
	leaq	216(%rsp), %rdi
	xorl	%esi, %esi
	xorl	%edx, %edx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE10_M_replaceEmmPKcm@PLT
.Ltmp126:
.LBB0_135:
	movq	_ZTTNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEEE@GOTPCREL(%rip), %rax
	movq	(%rax), %rcx
	movq	24(%rax), %rax
	movq	%rcx, 312(%rsp)
	movq	-24(%rcx), %rcx
	movq	%rax, 312(%rsp,%rcx)
	movq	_ZTVNSt7__cxx1115basic_stringbufIcSt11char_traitsIcESaIcEEE@GOTPCREL(%rip), %rbx
	addq	$16, %rbx
	movq	%rbx, 320(%rsp)
	movq	392(%rsp), %rdi
	leaq	408(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_137
# %bb.136:
	movq	408(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_137:
	movq	_ZTVSt15basic_streambufIcSt11char_traitsIcEE@GOTPCREL(%rip), %r15
	addq	$16, %r15
	movq	%r15, 320(%rsp)
	leaq	376(%rsp), %rdi
	callq	_ZNSt6localeD1Ev@PLT
	leaq	424(%rsp), %rdi
	callq	_ZNSt8ios_baseD2Ev@PLT
	movq	264(%rsp), %rdi
	leaq	280(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_139
# %bb.138:
	movq	280(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_139:
	movq	184(%rsp), %rdi
	leaq	200(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_141
# %bb.140:
	movq	200(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_141:
	movq	104(%rsp), %rdi
	leaq	120(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_143
# %bb.142:
	movq	120(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_143:
	movq	_ZTTNSt7__cxx1119basic_istringstreamIcSt11char_traitsIcESaIcEEE@GOTPCREL(%rip), %r12
	movq	(%r12), %rax
	movq	24(%r12), %rcx
	movq	%rax, 808(%rsp)
	movq	-24(%rax), %rax
	movq	%rcx, 808(%rsp,%rax)
	movq	%rbx, 824(%rsp)
	movq	896(%rsp), %rdi
	leaq	912(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_145
# %bb.144:
	movq	912(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_145:
	movq	%r15, 824(%rsp)
	leaq	880(%rsp), %rdi
	callq	_ZNSt6localeD1Ev@PLT
	movq	8(%r12), %rax
	movq	16(%r12), %rcx
	movq	%rax, 808(%rsp)
	movq	-24(%rax), %rax
	movq	%rcx, 808(%rsp,%rax)
	movq	$0, 816(%rsp)
	leaq	928(%rsp), %rdi
	callq	_ZNSt8ios_baseD2Ev@PLT
	movq	216(%rsp), %rsi
	movq	304(%rsp), %rdi                 # 8-byte Reload
	movq	(%rdi), %rax
.Ltmp130:
	callq	*1336(%rax)
.Ltmp131:
# %bb.146:
	movq	216(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_148
# %bb.147:
	movq	232(%rsp), %rsi
	incq	%rsi
	movq	%rax, %rbx
	callq	_ZdlPvm@PLT
	movq	%rbx, %rax
.LBB0_148:
	addq	$1192, %rsp                     # imm = 0x4A8
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
.LBB0_149:
	.cfi_def_cfa_offset 1248
.Ltmp62:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp63:
# %bb.150:
.LBB0_151:
.Ltmp92:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp93:
# %bb.152:
.LBB0_153:
.Ltmp56:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp57:
# %bb.154:
.LBB0_155:
.Ltmp59:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp60:
# %bb.156:
.LBB0_157:
.Ltmp89:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp90:
# %bb.158:
.LBB0_159:
.Ltmp53:
	leaq	.L.str.19(%rip), %rdi
	callq	_ZSt20__throw_length_errorPKc@PLT
.Ltmp54:
# %bb.160:
.LBB0_161:
.Ltmp18:
	movq	%rax, %rbx
	jmp	.LBB0_219
.LBB0_162:
.Ltmp67:
	movq	%rax, %rbx
	jmp	.LBB0_219
.LBB0_163:
.Ltmp132:
	movq	%rax, %rbx
	movq	216(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_228
# %bb.164:
	movq	232(%rsp), %rsi
	jmp	.LBB0_175
.LBB0_165:
.Ltmp129:
	movq	%rax, %rbx
	movq	216(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_197
# %bb.166:
	movq	232(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_197
.LBB0_167:
.Ltmp97:
	movq	%rax, %rbx
	jmp	.LBB0_219
.LBB0_168:
.Ltmp15:
	movq	%rax, %rbx
	movq	312(%rsp), %rdi
	cmpq	%rbp, %rdi
	je	.LBB0_222
# %bb.169:
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_222
.LBB0_170:
.Ltmp5:
	movq	%rax, %rbx
	movq	312(%rsp), %rdi
	leaq	328(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_173
# %bb.171:
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_173
.LBB0_172:
.Ltmp2:
	movq	%rax, %rbx
.LBB0_173:
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_228
# %bb.174:
	movq	56(%rsp), %rsi
.LBB0_175:
	incq	%rsi
	callq	_ZdlPvm@PLT
	movq	%rbx, %rdi
	callq	_Unwind_Resume@PLT
.LBB0_176:
.Ltmp55:
	jmp	.LBB0_199
.LBB0_177:
.Ltmp52:
	movq	%rax, %rbx
	movq	72(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_180
# %bb.178:
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_180
.LBB0_179:
.Ltmp49:
	movq	%rax, %rbx
.LBB0_180:
	movq	8(%rsp), %rdi
	leaq	24(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_200
# %bb.181:
	movq	24(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_200
.LBB0_182:
.Ltmp46:
	jmp	.LBB0_199
.LBB0_183:
.Ltmp79:
	jmp	.LBB0_206
.LBB0_184:
.Ltmp91:
	jmp	.LBB0_206
.LBB0_185:
.Ltmp85:
	movq	%rax, %rbx
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_188
# %bb.186:
	movq	56(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_188
.LBB0_187:
.Ltmp82:
	movq	%rax, %rbx
.LBB0_188:
	movq	72(%rsp), %rdi
	leaq	88(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_207
# %bb.189:
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_207
.LBB0_190:
.Ltmp33:
	jmp	.LBB0_213
.LBB0_191:
.Ltmp61:
	jmp	.LBB0_213
.LBB0_192:
.Ltmp36:
	movq	%rax, %rbx
	movq	72(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_214
# %bb.193:
	movq	88(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_214
.LBB0_194:
.Ltmp58:
	jmp	.LBB0_199
.LBB0_195:
.Ltmp94:
	jmp	.LBB0_206
.LBB0_196:
.Ltmp124:
	movq	%rax, %rbx
.LBB0_197:
	leaq	312(%rsp), %rdi
	callq	_ZNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEED1Ev@PLT
	jmp	.LBB0_219
.LBB0_198:
.Ltmp43:
.LBB0_199:
	movq	%rax, %rbx
.LBB0_200:
	movq	152(%rsp), %rdi
	leaq	168(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_202
# %bb.201:
	movq	168(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_202:
	movq	40(%rsp), %rdi
	leaq	56(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_217
# %bb.203:
	movq	56(%rsp), %rsi
	jmp	.LBB0_216
.LBB0_204:
.Ltmp64:
	jmp	.LBB0_213
.LBB0_205:
.Ltmp76:
.LBB0_206:
	movq	%rax, %rbx
.LBB0_207:
	movq	8(%rsp), %rdi
	leaq	24(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_209
# %bb.208:
	movq	24(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_209:
	movq	312(%rsp), %rdi
	cmpq	%r12, %rdi
	jne	.LBB0_218
	jmp	.LBB0_219
.LBB0_210:
.Ltmp23:
	movq	%rax, %rbx
	jmp	.LBB0_219
.LBB0_211:
.Ltmp88:
	movq	%rax, %rbx
	jmp	.LBB0_219
.LBB0_212:
.Ltmp30:
.LBB0_213:
	movq	%rax, %rbx
.LBB0_214:
	movq	8(%rsp), %rdi
	leaq	24(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_217
# %bb.215:
	movq	24(%rsp), %rsi
.LBB0_216:
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_217:
	movq	312(%rsp), %rdi
	leaq	328(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_219
.LBB0_218:
	movq	328(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_219:
	leaq	120(%rsp), %r14
	leaq	200(%rsp), %r15
	movq	264(%rsp), %rdi
	leaq	280(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB0_223
# %bb.220:
	movq	280(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB0_223
.LBB0_221:
.Ltmp12:
	movq	%rax, %rbx
.LBB0_222:
	leaq	120(%rsp), %r14
	leaq	200(%rsp), %r15
.LBB0_223:
	movq	184(%rsp), %rdi
	cmpq	%r15, %rdi
	je	.LBB0_225
# %bb.224:
	movq	200(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_225:
	movq	104(%rsp), %rdi
	cmpq	%r14, %rdi
	je	.LBB0_227
# %bb.226:
	movq	120(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB0_227:
	leaq	808(%rsp), %rdi
	callq	_ZNSt7__cxx1119basic_istringstreamIcSt11char_traitsIcESaIcEED1Ev@PLT
.LBB0_228:
	movq	%rbx, %rdi
	callq	_Unwind_Resume@PLT
.Lfunc_end0:
	.size	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot, .Lfunc_end0-Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot
	.cfi_endproc
	.section	.gcc_except_table.Java_frb_axeron_manager_core_AxEldenLiveNative_nativeSnapshot,"a",@progbits
	.p2align	2, 0x0
GCC_except_table0:
.Lexception0:
	.byte	255                             # @LPStart Encoding = omit
	.byte	255                             # @TType Encoding = omit
	.byte	1                               # Call site Encoding = uleb128
	.uleb128 .Lcst_end0-.Lcst_begin0
.Lcst_begin0:
	.uleb128 .Ltmp0-.Lfunc_begin0           # >> Call Site 1 <<
	.uleb128 .Ltmp1-.Ltmp0                  #   Call between .Ltmp0 and .Ltmp1
	.uleb128 .Ltmp2-.Lfunc_begin0           #     jumps to .Ltmp2
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp3-.Lfunc_begin0           # >> Call Site 2 <<
	.uleb128 .Ltmp4-.Ltmp3                  #   Call between .Ltmp3 and .Ltmp4
	.uleb128 .Ltmp5-.Lfunc_begin0           #     jumps to .Ltmp5
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp6-.Lfunc_begin0           # >> Call Site 3 <<
	.uleb128 .Ltmp11-.Ltmp6                 #   Call between .Ltmp6 and .Ltmp11
	.uleb128 .Ltmp12-.Lfunc_begin0          #     jumps to .Ltmp12
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp13-.Lfunc_begin0          # >> Call Site 4 <<
	.uleb128 .Ltmp14-.Ltmp13                #   Call between .Ltmp13 and .Ltmp14
	.uleb128 .Ltmp15-.Lfunc_begin0          #     jumps to .Ltmp15
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp16-.Lfunc_begin0          # >> Call Site 5 <<
	.uleb128 .Ltmp17-.Ltmp16                #   Call between .Ltmp16 and .Ltmp17
	.uleb128 .Ltmp18-.Lfunc_begin0          #     jumps to .Ltmp18
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp19-.Lfunc_begin0          # >> Call Site 6 <<
	.uleb128 .Ltmp22-.Ltmp19                #   Call between .Ltmp19 and .Ltmp22
	.uleb128 .Ltmp23-.Lfunc_begin0          #     jumps to .Ltmp23
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp22-.Lfunc_begin0          # >> Call Site 7 <<
	.uleb128 .Ltmp24-.Ltmp22                #   Call between .Ltmp22 and .Ltmp24
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp24-.Lfunc_begin0          # >> Call Site 8 <<
	.uleb128 .Ltmp29-.Ltmp24                #   Call between .Ltmp24 and .Ltmp29
	.uleb128 .Ltmp30-.Lfunc_begin0          #     jumps to .Ltmp30
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp31-.Lfunc_begin0          # >> Call Site 9 <<
	.uleb128 .Ltmp32-.Ltmp31                #   Call between .Ltmp31 and .Ltmp32
	.uleb128 .Ltmp33-.Lfunc_begin0          #     jumps to .Ltmp33
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp32-.Lfunc_begin0          # >> Call Site 10 <<
	.uleb128 .Ltmp34-.Ltmp32                #   Call between .Ltmp32 and .Ltmp34
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp34-.Lfunc_begin0          # >> Call Site 11 <<
	.uleb128 .Ltmp35-.Ltmp34                #   Call between .Ltmp34 and .Ltmp35
	.uleb128 .Ltmp36-.Lfunc_begin0          #     jumps to .Ltmp36
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp37-.Lfunc_begin0          # >> Call Site 12 <<
	.uleb128 .Ltmp42-.Ltmp37                #   Call between .Ltmp37 and .Ltmp42
	.uleb128 .Ltmp43-.Lfunc_begin0          #     jumps to .Ltmp43
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp44-.Lfunc_begin0          # >> Call Site 13 <<
	.uleb128 .Ltmp45-.Ltmp44                #   Call between .Ltmp44 and .Ltmp45
	.uleb128 .Ltmp46-.Lfunc_begin0          #     jumps to .Ltmp46
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp45-.Lfunc_begin0          # >> Call Site 14 <<
	.uleb128 .Ltmp47-.Ltmp45                #   Call between .Ltmp45 and .Ltmp47
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp47-.Lfunc_begin0          # >> Call Site 15 <<
	.uleb128 .Ltmp48-.Ltmp47                #   Call between .Ltmp47 and .Ltmp48
	.uleb128 .Ltmp49-.Lfunc_begin0          #     jumps to .Ltmp49
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp50-.Lfunc_begin0          # >> Call Site 16 <<
	.uleb128 .Ltmp51-.Ltmp50                #   Call between .Ltmp50 and .Ltmp51
	.uleb128 .Ltmp52-.Lfunc_begin0          #     jumps to .Ltmp52
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp65-.Lfunc_begin0          # >> Call Site 17 <<
	.uleb128 .Ltmp66-.Ltmp65                #   Call between .Ltmp65 and .Ltmp66
	.uleb128 .Ltmp67-.Lfunc_begin0          #     jumps to .Ltmp67
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp68-.Lfunc_begin0          # >> Call Site 18 <<
	.uleb128 .Ltmp69-.Ltmp68                #   Call between .Ltmp68 and .Ltmp69
	.uleb128 .Ltmp88-.Lfunc_begin0          #     jumps to .Ltmp88
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp69-.Lfunc_begin0          # >> Call Site 19 <<
	.uleb128 .Ltmp70-.Ltmp69                #   Call between .Ltmp69 and .Ltmp70
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp70-.Lfunc_begin0          # >> Call Site 20 <<
	.uleb128 .Ltmp75-.Ltmp70                #   Call between .Ltmp70 and .Ltmp75
	.uleb128 .Ltmp76-.Lfunc_begin0          #     jumps to .Ltmp76
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp77-.Lfunc_begin0          # >> Call Site 21 <<
	.uleb128 .Ltmp78-.Ltmp77                #   Call between .Ltmp77 and .Ltmp78
	.uleb128 .Ltmp79-.Lfunc_begin0          #     jumps to .Ltmp79
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp78-.Lfunc_begin0          # >> Call Site 22 <<
	.uleb128 .Ltmp80-.Ltmp78                #   Call between .Ltmp78 and .Ltmp80
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp80-.Lfunc_begin0          # >> Call Site 23 <<
	.uleb128 .Ltmp81-.Ltmp80                #   Call between .Ltmp80 and .Ltmp81
	.uleb128 .Ltmp82-.Lfunc_begin0          #     jumps to .Ltmp82
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp83-.Lfunc_begin0          # >> Call Site 24 <<
	.uleb128 .Ltmp84-.Ltmp83                #   Call between .Ltmp83 and .Ltmp84
	.uleb128 .Ltmp85-.Lfunc_begin0          #     jumps to .Ltmp85
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp86-.Lfunc_begin0          # >> Call Site 25 <<
	.uleb128 .Ltmp87-.Ltmp86                #   Call between .Ltmp86 and .Ltmp87
	.uleb128 .Ltmp88-.Lfunc_begin0          #     jumps to .Ltmp88
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp95-.Lfunc_begin0          # >> Call Site 26 <<
	.uleb128 .Ltmp96-.Ltmp95                #   Call between .Ltmp95 and .Ltmp96
	.uleb128 .Ltmp97-.Lfunc_begin0          #     jumps to .Ltmp97
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp98-.Lfunc_begin0          # >> Call Site 27 <<
	.uleb128 .Ltmp123-.Ltmp98               #   Call between .Ltmp98 and .Ltmp123
	.uleb128 .Ltmp124-.Lfunc_begin0         #     jumps to .Ltmp124
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp127-.Lfunc_begin0         # >> Call Site 28 <<
	.uleb128 .Ltmp126-.Ltmp127              #   Call between .Ltmp127 and .Ltmp126
	.uleb128 .Ltmp129-.Lfunc_begin0         #     jumps to .Ltmp129
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp130-.Lfunc_begin0         # >> Call Site 29 <<
	.uleb128 .Ltmp131-.Ltmp130              #   Call between .Ltmp130 and .Ltmp131
	.uleb128 .Ltmp132-.Lfunc_begin0         #     jumps to .Ltmp132
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp62-.Lfunc_begin0          # >> Call Site 30 <<
	.uleb128 .Ltmp63-.Ltmp62                #   Call between .Ltmp62 and .Ltmp63
	.uleb128 .Ltmp64-.Lfunc_begin0          #     jumps to .Ltmp64
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp92-.Lfunc_begin0          # >> Call Site 31 <<
	.uleb128 .Ltmp93-.Ltmp92                #   Call between .Ltmp92 and .Ltmp93
	.uleb128 .Ltmp94-.Lfunc_begin0          #     jumps to .Ltmp94
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp56-.Lfunc_begin0          # >> Call Site 32 <<
	.uleb128 .Ltmp57-.Ltmp56                #   Call between .Ltmp56 and .Ltmp57
	.uleb128 .Ltmp58-.Lfunc_begin0          #     jumps to .Ltmp58
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp59-.Lfunc_begin0          # >> Call Site 33 <<
	.uleb128 .Ltmp60-.Ltmp59                #   Call between .Ltmp59 and .Ltmp60
	.uleb128 .Ltmp61-.Lfunc_begin0          #     jumps to .Ltmp61
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp89-.Lfunc_begin0          # >> Call Site 34 <<
	.uleb128 .Ltmp90-.Ltmp89                #   Call between .Ltmp89 and .Ltmp90
	.uleb128 .Ltmp91-.Lfunc_begin0          #     jumps to .Ltmp91
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp53-.Lfunc_begin0          # >> Call Site 35 <<
	.uleb128 .Ltmp54-.Ltmp53                #   Call between .Ltmp53 and .Ltmp54
	.uleb128 .Ltmp55-.Lfunc_begin0          #     jumps to .Ltmp55
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp54-.Lfunc_begin0          # >> Call Site 36 <<
	.uleb128 .Lfunc_end0-.Ltmp54            #   Call between .Ltmp54 and .Lfunc_end0
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
.Lcst_end0:
	.p2align	2, 0x0
                                        # -- End function
	.section	.text.Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis,"ax",@progbits
	.globl	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis # -- Begin function Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis
	.p2align	4, 0x90
	.type	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis,@function
Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis: # @Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis
	.cfi_startproc
# %bb.0:
	subq	$24, %rsp
	.cfi_def_cfa_offset 32
	xorps	%xmm0, %xmm0
	movaps	%xmm0, (%rsp)
	movq	%rsp, %rsi
	movl	$1, %edi
	callq	clock_gettime@PLT
	imulq	$1000, (%rsp), %rcx             # imm = 0x3E8
	movabsq	$4835703278458516699, %rax      # imm = 0x431BDE82D7B634DB
	imulq	8(%rsp)
	movq	%rdx, %rax
	shrq	$63, %rax
	sarq	$18, %rdx
	addq	%rdx, %rax
	addq	%rcx, %rax
	addq	$24, %rsp
	.cfi_def_cfa_offset 8
	retq
.Lfunc_end1:
	.size	Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis, .Lfunc_end1-Java_frb_axeron_manager_core_AxEldenLiveNative_nativeMonotonicMillis
	.cfi_endproc
                                        # -- End function
	.section	.text._ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE,"ax",@progbits
	.p2align	4, 0x90                         # -- Begin function _ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
	.type	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE,@function
_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE: # @_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
.Lfunc_begin1:
	.cfi_startproc
	.cfi_personality 155, DW.ref.__gxx_personality_v0
	.cfi_lsda 27, .Lexception1
# %bb.0:
	pushq	%r15
	.cfi_def_cfa_offset 16
	pushq	%r14
	.cfi_def_cfa_offset 24
	pushq	%rbx
	.cfi_def_cfa_offset 32
	subq	$896, %rsp                      # imm = 0x380
	.cfi_def_cfa_offset 928
	.cfi_offset %rbx, -32
	.cfi_offset %r14, -24
	.cfi_offset %r15, -16
	movq	%rdi, %rbx
	leaq	376(%rsp), %rdi
	movl	$8, %edx
	callq	_ZNSt14basic_ifstreamIcSt11char_traitsIcEEC1ERKNSt7__cxx1112basic_stringIcS1_SaIcEEESt13_Ios_Openmode@PLT
	movq	376(%rsp), %rax
	movq	-24(%rax), %rax
	testb	$5, 408(%rsp,%rax)
	je	.LBB2_3
# %bb.1:
	leaq	16(%rbx), %rax
	movq	%rax, (%rbx)
	movq	$0, 8(%rbx)
	movb	$0, 16(%rbx)
	jmp	.LBB2_2
.LBB2_3:
.Ltmp133:
	movq	%rsp, %rdi
	callq	_ZNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEEC1Ev@PLT
.Ltmp134:
# %bb.4:
	leaq	392(%rsp), %rsi
.Ltmp136:
	movq	%rsp, %rdi
	callq	_ZNSolsEPSt15basic_streambufIcSt11char_traitsIcEE@PLT
.Ltmp137:
# %bb.5:
	leaq	16(%rbx), %r15
	movq	%r15, (%rbx)
	movq	$0, 8(%rbx)
	movq	32(%rsp), %r8
	movq	48(%rsp), %rax
	testq	%rax, %rax
	sete	%cl
	cmpq	%r8, %rax
	cmovaq	%rax, %r8
	movb	$0, 16(%rbx)
	testq	%r8, %r8
	sete	%al
	orb	%cl, %al
	je	.LBB2_6
# %bb.9:
	leaq	80(%rsp), %rsi
.Ltmp141:
	movq	%rbx, %rdi
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_assignERKS4_@PLT
.Ltmp142:
	jmp	.LBB2_10
.LBB2_6:
	movq	40(%rsp), %rcx
	subq	%rcx, %r8
.Ltmp139:
	movq	%rbx, %rdi
	xorl	%esi, %esi
	xorl	%edx, %edx
	callq	_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE10_M_replaceEmmPKcm@PLT
.Ltmp140:
.LBB2_10:
	movq	_ZTTNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEEE@GOTPCREL(%rip), %rax
	movq	(%rax), %rcx
	movq	24(%rax), %rax
	movq	%rcx, (%rsp)
	movq	-24(%rcx), %rcx
	movq	%rax, (%rsp,%rcx)
	movq	_ZTVNSt7__cxx1115basic_stringbufIcSt11char_traitsIcESaIcEEE@GOTPCREL(%rip), %rax
	addq	$16, %rax
	movq	%rax, 8(%rsp)
	movq	80(%rsp), %rdi
	leaq	96(%rsp), %rax
	cmpq	%rax, %rdi
	je	.LBB2_12
# %bb.11:
	movq	96(%rsp), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
.LBB2_12:
	movq	_ZTVSt15basic_streambufIcSt11char_traitsIcEE@GOTPCREL(%rip), %rax
	addq	$16, %rax
	movq	%rax, 8(%rsp)
	leaq	64(%rsp), %rdi
	callq	_ZNSt6localeD1Ev@PLT
	leaq	112(%rsp), %rdi
	callq	_ZNSt8ios_baseD2Ev@PLT
.LBB2_2:
	movq	_ZTTSt14basic_ifstreamIcSt11char_traitsIcEE@GOTPCREL(%rip), %rsi
	leaq	376(%rsp), %rdi
	callq	_ZNSt14basic_ifstreamIcSt11char_traitsIcEED2Ev@PLT
	leaq	632(%rsp), %rdi
	callq	_ZNSt8ios_baseD2Ev@PLT
	movq	%rbx, %rax
	addq	$896, %rsp                      # imm = 0x380
	.cfi_def_cfa_offset 32
	popq	%rbx
	.cfi_def_cfa_offset 24
	popq	%r14
	.cfi_def_cfa_offset 16
	popq	%r15
	.cfi_def_cfa_offset 8
	retq
.LBB2_7:
	.cfi_def_cfa_offset 928
.Ltmp143:
	movq	%rax, %r14
	movq	(%rbx), %rdi
	cmpq	%r15, %rdi
	je	.LBB2_15
# %bb.8:
	movq	(%r15), %rsi
	incq	%rsi
	callq	_ZdlPvm@PLT
	jmp	.LBB2_15
.LBB2_14:
.Ltmp138:
	movq	%rax, %r14
.LBB2_15:
	movq	%rsp, %rdi
	callq	_ZNSt7__cxx1119basic_ostringstreamIcSt11char_traitsIcESaIcEED1Ev@PLT
	jmp	.LBB2_16
.LBB2_13:
.Ltmp135:
	movq	%rax, %r14
.LBB2_16:
	movq	_ZTTSt14basic_ifstreamIcSt11char_traitsIcEE@GOTPCREL(%rip), %rsi
	leaq	376(%rsp), %rdi
	callq	_ZNSt14basic_ifstreamIcSt11char_traitsIcEED2Ev@PLT
	leaq	632(%rsp), %rdi
	callq	_ZNSt8ios_baseD2Ev@PLT
	movq	%r14, %rdi
	callq	_Unwind_Resume@PLT
.Lfunc_end2:
	.size	_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE, .Lfunc_end2-_ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
	.cfi_endproc
	.section	.gcc_except_table._ZL9read_fileRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE,"a",@progbits
	.p2align	2, 0x0
GCC_except_table2:
.Lexception1:
	.byte	255                             # @LPStart Encoding = omit
	.byte	255                             # @TType Encoding = omit
	.byte	1                               # Call site Encoding = uleb128
	.uleb128 .Lcst_end1-.Lcst_begin1
.Lcst_begin1:
	.uleb128 .Lfunc_begin1-.Lfunc_begin1    # >> Call Site 1 <<
	.uleb128 .Ltmp133-.Lfunc_begin1         #   Call between .Lfunc_begin1 and .Ltmp133
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp133-.Lfunc_begin1         # >> Call Site 2 <<
	.uleb128 .Ltmp134-.Ltmp133              #   Call between .Ltmp133 and .Ltmp134
	.uleb128 .Ltmp135-.Lfunc_begin1         #     jumps to .Ltmp135
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp136-.Lfunc_begin1         # >> Call Site 3 <<
	.uleb128 .Ltmp137-.Ltmp136              #   Call between .Ltmp136 and .Ltmp137
	.uleb128 .Ltmp138-.Lfunc_begin1         #     jumps to .Ltmp138
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp141-.Lfunc_begin1         # >> Call Site 4 <<
	.uleb128 .Ltmp140-.Ltmp141              #   Call between .Ltmp141 and .Ltmp140
	.uleb128 .Ltmp143-.Lfunc_begin1         #     jumps to .Ltmp143
	.byte	0                               #   On action: cleanup
	.uleb128 .Ltmp140-.Lfunc_begin1         # >> Call Site 5 <<
	.uleb128 .Lfunc_end2-.Ltmp140           #   Call between .Ltmp140 and .Lfunc_end2
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
.Lcst_end1:
	.p2align	2, 0x0
                                        # -- End function
	.section	.text._ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx,"ax",@progbits
	.p2align	4, 0x90                         # -- Begin function _ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx
	.type	_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx,@function
_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx: # @_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx
.Lfunc_begin2:
	.cfi_startproc
	.cfi_personality 155, DW.ref.__gxx_personality_v0
	.cfi_lsda 27, .Lexception2
# %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	pushq	%r14
	.cfi_def_cfa_offset 24
	pushq	%rbx
	.cfi_def_cfa_offset 32
	subq	$16, %rsp
	.cfi_def_cfa_offset 48
	.cfi_offset %rbx, -32
	.cfi_offset %r14, -24
	.cfi_offset %rbp, -16
	movq	(%rdi), %r14
	callq	__errno_location@PLT
	movq	%rax, %rbx
	movl	(%rax), %ebp
	movl	$0, (%rax)
	leaq	8(%rsp), %rsi
	movq	%r14, %rdi
	movl	$10, %edx
	callq	__isoc23_strtoll@PLT
	cmpq	%r14, 8(%rsp)
	je	.LBB3_1
# %bb.6:
	movl	(%rbx), %ecx
	testl	%ecx, %ecx
	je	.LBB3_10
# %bb.7:
	cmpl	$34, %ecx
	jne	.LBB3_11
# %bb.8:
.Ltmp144:
	leaq	.L.str.20(%rip), %rdi
	callq	_ZSt20__throw_out_of_rangePKc@PLT
.Ltmp145:
# %bb.9:
.LBB3_10:
	movl	%ebp, (%rbx)
.LBB3_11:
	addq	$16, %rsp
	.cfi_def_cfa_offset 32
	popq	%rbx
	.cfi_def_cfa_offset 24
	popq	%r14
	.cfi_def_cfa_offset 16
	popq	%rbp
	.cfi_def_cfa_offset 8
	retq
.LBB3_1:
	.cfi_def_cfa_offset 48
.Ltmp146:
	leaq	.L.str.20(%rip), %rdi
	callq	_ZSt24__throw_invalid_argumentPKc@PLT
.Ltmp147:
# %bb.2:
.LBB3_3:
.Ltmp148:
	cmpl	$0, (%rbx)
	jne	.LBB3_5
# %bb.4:
	movl	%ebp, (%rbx)
.LBB3_5:
	movq	%rax, %rdi
	callq	__cxa_begin_catch@PLT
	callq	__cxa_end_catch@PLT
	movq	$-1, %rax
	jmp	.LBB3_11
.Lfunc_end3:
	.size	_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx, .Lfunc_end3-_ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx
	.cfi_endproc
	.section	.gcc_except_table._ZL6numberRKNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEEx,"a",@progbits
	.p2align	2, 0x0
GCC_except_table3:
.Lexception2:
	.byte	255                             # @LPStart Encoding = omit
	.byte	155                             # @TType Encoding = indirect pcrel sdata4
	.uleb128 .Lttbase0-.Lttbaseref0
.Lttbaseref0:
	.byte	1                               # Call site Encoding = uleb128
	.uleb128 .Lcst_end2-.Lcst_begin2
.Lcst_begin2:
	.uleb128 .Ltmp144-.Lfunc_begin2         # >> Call Site 1 <<
	.uleb128 .Ltmp147-.Ltmp144              #   Call between .Ltmp144 and .Ltmp147
	.uleb128 .Ltmp148-.Lfunc_begin2         #     jumps to .Ltmp148
	.byte	1                               #   On action: 1
	.uleb128 .Ltmp147-.Lfunc_begin2         # >> Call Site 2 <<
	.uleb128 .Lfunc_end3-.Ltmp147           #   Call between .Ltmp147 and .Lfunc_end3
	.byte	0                               #     has no landing pad
	.byte	0                               #   On action: cleanup
.Lcst_end2:
	.byte	1                               # >> Action Record 1 <<
                                        #   Catch TypeInfo 1
	.byte	0                               #   No further actions
	.p2align	2, 0x0
                                        # >> Catch TypeInfos <<
	.long	0                               # TypeInfo 1
.Lttbase0:
	.p2align	2, 0x0
                                        # -- End function
	.type	.L.str,@object                  # @.str
	.section	.rodata.str1.1,"aMS",@progbits,1
.L.str:
	.asciz	"/proc/meminfo"
	.size	.L.str, 14

	.type	.L.str.1,@object                # @.str.1
.L.str.1:
	.asciz	"MemTotal:"
	.size	.L.str.1, 10

	.type	.L.str.2,@object                # @.str.2
.L.str.2:
	.asciz	"MemAvailable:"
	.size	.L.str.2, 14

	.type	.L.str.3,@object                # @.str.3
.L.str.3:
	.asciz	"/proc/loadavg"
	.size	.L.str.3, 14

	.type	.L.str.4,@object                # @.str.4
.L.str.4:
	.asciz	"{\"uptime_sec\":"
	.size	.L.str.4, 15

	.type	.L.str.5,@object                # @.str.5
.L.str.5:
	.asciz	",\"ram_total_kb\":"
	.size	.L.str.5, 17

	.type	.L.str.6,@object                # @.str.6
.L.str.6:
	.asciz	",\"ram_available_kb\":"
	.size	.L.str.6, 21

	.type	.L.str.7,@object                # @.str.7
.L.str.7:
	.asciz	",\"battery_percent\":"
	.size	.L.str.7, 20

	.type	.L.str.8,@object                # @.str.8
.L.str.8:
	.asciz	",\"thermal_max_mC\":"
	.size	.L.str.8, 19

	.type	.L.str.9,@object                # @.str.9
.L.str.9:
	.asciz	",\"load1\":\""
	.size	.L.str.9, 11

	.type	.L.str.10,@object               # @.str.10
.L.str.10:
	.asciz	"\"}"
	.size	.L.str.10, 3

	.type	.L.str.12,@object               # @.str.12
.L.str.12:
	.asciz	"/sys/class/power_supply"
	.size	.L.str.12, 24

	.type	.L.str.14,@object               # @.str.14
.L.str.14:
	.asciz	".."
	.size	.L.str.14, 3

	.type	.L.str.15,@object               # @.str.15
.L.str.15:
	.asciz	"/sys/class/power_supply/"
	.size	.L.str.15, 25

	.type	.L.str.16,@object               # @.str.16
.L.str.16:
	.asciz	"/type"
	.size	.L.str.16, 6

	.type	.L.str.17,@object               # @.str.17
.L.str.17:
	.asciz	"Battery"
	.size	.L.str.17, 8

	.type	.L.str.18,@object               # @.str.18
.L.str.18:
	.asciz	"/capacity"
	.size	.L.str.18, 10

	.type	.L.str.19,@object               # @.str.19
.L.str.19:
	.asciz	"basic_string::append"
	.size	.L.str.19, 21

	.type	.L.str.20,@object               # @.str.20
.L.str.20:
	.asciz	"stoll"
	.size	.L.str.20, 6

	.type	.L.str.21,@object               # @.str.21
.L.str.21:
	.asciz	"/sys/class/thermal"
	.size	.L.str.21, 19

	.type	.L.str.22,@object               # @.str.22
.L.str.22:
	.asciz	"thermal_zone"
	.size	.L.str.22, 13

	.type	.L.str.23,@object               # @.str.23
.L.str.23:
	.asciz	"/sys/class/thermal/"
	.size	.L.str.23, 20

	.type	.L.str.24,@object               # @.str.24
.L.str.24:
	.asciz	"/temp"
	.size	.L.str.24, 6

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
