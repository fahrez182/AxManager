#!/usr/bin/env python3

import glob
import os
import re

KERNEL_VERSION_ORDER = ["5.10", "5.15", "6.1", "6.6", "6.12"]


def read_or_default(path, default="*No changelog available*"):
	if os.path.isfile(path):
		return open(path).read().rstrip("\n")
	return default


def slugify(text):
	text = text.lower()
	text = re.sub(r"[^\w\s-]", "", text)
	text = re.sub(r"\s+", "-", text.strip())
	return text


def parse_env_file(path):
	env = {}
	with open(path) as f:
		for line in f:
			line = line.strip()
			if "=" in line:
				key, value = line.split("=", 1)
				if key:
					env[key] = value
	return env


def load_build_envs():
	env_files = sorted(glob.glob("release-artifacts/build-env-*.txt"))
	if not env_files:
		raise SystemExit("ERROR: no build-env-*.txt files found in release-artifacts/")

	all_builds = [parse_env_file(f) for f in env_files]

	shared_env = {}
	for key in ("RELEASE_REPO", "RELEASE", "RELEASE_NAME", "KERNEL_NAME"):
		values = {b[key] for b in all_builds if b.get(key)}
		if not values:
			raise SystemExit(f"ERROR: missing required build-env value: {key}")
		shared_env[key] = sorted(values)[0]

	return all_builds, shared_env


def kernel_versions_present(all_builds):
	present = {b.get("KERNEL_VERSION") for b in all_builds if b.get("KERNEL_VERSION")}
	ordered = [v for v in KERNEL_VERSION_ORDER if v in present]
	ordered += sorted(present - set(ordered))
	return ordered


def variant_zip_name(build):
	"""Reconstruct the exact zip filename build.sh produced for this job."""
	base_name = build.get("BASE_NAME")
	release = build.get("RELEASE")
	linux_version = build.get("LINUX_VERSION")
	if not (base_name and release and linux_version):
		return None
	return f"{base_name}-{release}-{linux_version}.zip"


def variant_link(display_name, filename, repo, tag):
	return f"- [{display_name}](https://github.com/{repo}/releases/download/{tag}/{filename})"


def display_variant_name(build):
	name = build.get("BUILD_VARIANT", "")
	kver = build.get("KERNEL_VERSION", "")
	prefix = f"{kver}-"
	return name[len(prefix):] if name.startswith(prefix) else name


def build_kernel_section(kernel_version, builds, repo, tag, existing_zips, inputs):
	android_release = builds[0].get("ANDROID_RELEASE", "unknown")
	label = f"Android{android_release}-{kernel_version}-LTS"
	anchor_title = f"{label} Files"

	representative = next((b for b in builds if b.get("KSU_SUSFS") == "true"), builds[0])

	file_lines = []
	for b in sorted(builds, key=lambda b: display_variant_name(b)):
		zip_name = variant_zip_name(b)
		if zip_name and zip_name in existing_zips:
			file_lines.append(variant_link(display_variant_name(b), zip_name, repo, tag))
	files_block = "\n".join(file_lines) if file_lines else "- *No build artifacts found*"

	wireless_prefix = f"WirelessKSU-{kernel_version}-"
	wireless_zips = sorted(z for z in existing_zips if z.startswith(wireless_prefix))
	if inputs["nh"] != "true":
		wireless_block = "None (NetHunter disabled for this run)"
	elif wireless_zips:
		wireless_block = "\n".join(
			f"- [{z[:-4]}](https://github.com/{repo}/releases/download/{tag}/{z})"
			for z in wireless_zips
		)
	else:
		wireless_block = "*Built-in driver, no separate module needed*"

	susfs_changelog_file = f"release-artifacts/susfs_changelog-{kernel_version}.txt"
	susfs_version = representative.get("SUSFS_VERSION", "Not included")

	anchor_id = slugify(label)
	section = f"""<a name="{anchor_id}"></a>
## {anchor_title}

**Downloads:**
{files_block}

**Kali NetHunter KernelSU modules:**
{wireless_block}

**Build details:**
- Linux version: {representative.get('LINUX_VERSION', 'unknown')}
- Compiler: {representative.get('COMPILER_STRING', 'unknown')}
- SuSFS: {susfs_version}

**SuSFS changelog for this line (last 5 commits):**

{read_or_default(susfs_changelog_file)}
"""
	return label, section


def build_release_body():
	all_builds, shared_env = load_build_envs()
	repo = shared_env["RELEASE_REPO"]
	tag = shared_env["RELEASE"]
	release_name = shared_env["RELEASE_NAME"]

	existing_zips = {os.path.basename(p) for p in glob.glob("release-artifacts/*.zip")}
	versions = kernel_versions_present(all_builds)

	inputs = {
		"nh": os.environ.get("NH_INPUT", ""),
		"nm": os.environ.get("NM_INPUT", ""),
		"droidspaces": os.environ.get("DROIDSPACES_INPUT", ""),
		"lto": os.environ.get("LTO_INPUT", ""),
		"test": os.environ.get("TEST_INPUT", ""),
	}
	status_map = {"true": "Enabled", "false": "Disabled"}
	cap_first = lambda s: s[0].upper() + s[1:] if s else s

	warning = (
		"> [!Warning]\n> This is a test release for pipeline debugging - please do not download or install.\n\n"
		if inputs["test"] == "yes"
		else ""
	)

	toc_entries = []
	sections = []
	for kv in versions:
		builds_for_version = [b for b in all_builds if b.get("KERNEL_VERSION") == kv]
		label, section = build_kernel_section(kv, builds_for_version, repo, tag, existing_zips, inputs)
		toc_entries.append(f"- [{label}](#{slugify(label)})")
		sections.append(section)

	toc_block = "\n".join(toc_entries)
	sections_block = "\n\n---\n\n".join(sections)

	body = f"""{warning}### {release_name}

## ❤️ Support This Project

**[Donations](https://github.com/ahmed-alnassif#-support-my-work)**

Your donations keep this project alive! I spend countless hours maintaining kernel builds for 5 different versions, fixing bugs, adding features, and supporting users. **Every donation matters!** 🙏

- **[ReSuSFS](https://github.com/ahmed-alnassif/ReSuSFS)** – Root hiding made simple, powerful when you need it. A [KernelSU](https://kernelsu.org) module and WebUI that turns SuSFS into clean config files and toggle switches for everyday use, with **strong hiding applied out of the box** via built-in spoofing and hiding scripts for one-tap protection, plus a script manager for power users who want more, all without leaving the WebUI.

- **Community:** join the discussion and get support on [Telegram](https://t.me/ahmed_alnassif_tg).

**Run settings:**
- LTO optimizations: {cap_first(inputs['lto']) or 'Unknown'}
- Kali NetHunter: {status_map.get(inputs['nh'], 'Disabled')}
- DroidSpaces: {status_map.get(inputs['droidspaces'], 'Disabled')}
- NoMount: {status_map.get(inputs['nm'], 'Disabled')}

> [!Important]
> These are **GKI** kernels, not custom kernels. Each line below supports **all** devices that shipped with the matching Linux version and Android release (stock or AOSP).

## 🧭 Which variant should I flash?

| Variant | Root | SuSFS | LTO | Compat |
|---------|------|-------|-----|--------|
| Vanilla | ❌ | ❌ | Full | ❌ |
| Vanilla+NoLTO | ❌ | ❌ | ❌ | ❌ |
| KernelSU | ✅ | ❌ | Full | ❌ |
| KernelSU+SuSFS | ✅ | ✅ | Full | ❌ |
| KSU+SuSFS+MM | ✅ | ✅ | Full | ❌ |
| BakaSU+SuSFS | ✅ | ✅ | Full | ❌ |
| Compat+KSU+SuSFS | ✅ | ✅ | ❌ | ✅ |
| Compat+BakaSU+SuSFS | ✅ | ✅ | ❌ | ✅ |

**Not sure? Use a `Compat` variant first**: it fixes most boot issues. Full feature breakdown, LTO explanation, and troubleshooting live in the [README](https://github.com/ahmed-alnassif/GKID-Kernels#-build-variants).

## Contents
{toc_block}

---

{sections_block}

---

>[!Note]
>- **Bootloop?** Flash a **Compat** variant first.
>- **Issues?** Check [Discussions](https://github.com/ahmed-alnassif/GKID-Kernels/discussions) before opening an issue.

---

### Community & Support
- **Have questions?** Start a [Discussion](https://github.com/ahmed-alnassif/GKID-Kernels/discussions)
- **Found a bug?** Open an [Issue](https://github.com/ahmed-alnassif/GKID-Kernels/issues) with logs
- **Enjoying the kernel?** Star the [repo](https://github.com/ahmed-alnassif/GKID-Kernels)

---

**Performance & battery optimizations**
Engineered for smoother UI, better multitasking, and gaming on Poco X6 Pro:

**Performance**
- 300Hz timer -> lower input lag, snappier feel
- MGLRU -> better multitasking & battery life
- Faster memory ops -> up to 50% faster string/memory handling
- mq-deadline I/O -> low-latency on UFS 4.0 storage
- CPU governors: schedutil + ondemand -> efficient & responsive
- NTSync driver -> faster Windows games/apps on Winlator/GameHub

**Network**
- TCP BBRv3 + Westwood+ -> better WiFi/mobile data speeds
- IPv6 NAT + IP Set -> better tethering & VPN

**Battery life**
- Wakelock cap: 500ms -> prevents battery drain
- Freeze timeout: 20s -> 1s -> faster deadlock detection
- ext4 commit age: 30s -> fewer disk writes
- Minimized alarm wakeups -> less standby drain

**Storage & filesystem**
- F2FS tuning: reduced GC sleep (50ms) -> smoother I/O
- ext4 optimization -> extended commit age

**Security**
- Baseband Guard (BBG) -> blocks unauthorized writes to critical partitions

---

### Recommended companion modules
Enhance your Poco X6 Pro with these modules designed for GKID kernels:

| Module | Description | ROM |
|--------|-------------|-----|
| [**GPU Unlocker**](https://github.com/ahmed-alnassif/GPU-Unlocker) | Unlock Mali-G615 MC6 from 701MHz to 1.4GHz (100% boost) | HyperOS |
| [**Thermal Manager**](https://github.com/ahmed-alnassif/Thermal-Manager) | Fix thermal mode reset. Force-persist Balanced, Battery Saver, Performance, or Gaming. Includes WebUI. | AOSP |
| [**DSP AudioFix**](https://github.com/ahmed-alnassif/DSP-AudioFix) | Fix distorted audio on devices with Awinic smart amps | AOSP |

> [!Tip]
> HyperOS users: GPU Unlocker gives a large gaming performance boost.
> AOSP users: Thermal Manager fixes a stock bug that resets your thermal mode.

---

>[!Tip]
>This kernel includes **TCP BBRv3** (default) and **Westwood+** congestion control algorithms.
>You can switch between them - changes are temporary and reset after reboot.

**Switch to Westwood+ (better for some networks):**
```bash
su -c "sysctl -w net.ipv4.tcp_congestion_control=westwood"
```

**Restore BBRv3 (default):**
```bash
su -c "sysctl -w net.ipv4.tcp_congestion_control=bbr"
```

Test both and use whichever performs better on your network.
> **Note:** to make the change permanent, create a script in `/data/adb/service.d/` with the sysctl command.

---
**NoMount changelog (last 5 commits):**

{read_or_default("release-artifacts/nomount_changelog.txt")}

**Full commit history:** [Browse all commits](https://github.com/maxsteeel/nomount/commits/master)

---
**KernelSU changelog (last 5 commits):**

{read_or_default("release-artifacts/ksu_changelog.txt")}

**Full commit history:** [Browse all commits](https://github.com/tiann/KernelSU/commits/main)

---
**BakaSU changelog (last 5 commits):**

{read_or_default("release-artifacts/BakaSU_changelog.txt")}

**Full commit history:** [Browse all commits](https://github.com/Baka-SU/BakaSU/commits/main)

---
> [!Tip]
> **Checksums:**
> SHA256 checksums for all files in this release are available in [`checksums.txt`](https://github.com/{repo}/releases/download/{tag}/checksums.txt), attached below.
"""
	return body, versions, shared_env, all_builds
	return body, versions, shared_env, all_builds


def export_github_env(versions, shared_env, all_builds):
	github_env = os.environ.get("GITHUB_ENV")
	if not github_env:
		return

	multi_kernel = "true" if len(versions) > 1 else "false"
	with open(github_env, "a") as f:
		f.write(f"RELEASE_REPO={shared_env['RELEASE_REPO']}\n")
		f.write(f"RELEASE={shared_env['RELEASE']}\n")
		f.write(f"RELEASE_NAME={shared_env['RELEASE_NAME']}\n")
		f.write(f"MULTI_KERNEL={multi_kernel}\n")
		f.write(f"KERNEL_VERSIONS={','.join(versions)}\n")
		if len(versions) == 1:
			# Kept for the single-line Telegram caption path.
			single = versions[0]
			build = next(b for b in all_builds if b.get("KERNEL_VERSION") == single)
			f.write(f"KERNEL_VERSION={single}\n")
			f.write(f"ANDROID_RELEASE={build.get('ANDROID_RELEASE', 'unknown')}\n")


def main():
	body, versions, shared_env, all_builds = build_release_body()
	with open("release_body.md", "w") as f:
		f.write(body)
	export_github_env(versions, shared_env, all_builds)
	print(f"[+] release_body.md written for lines: {', '.join(versions)}")


if __name__ == "__main__":
	main()
