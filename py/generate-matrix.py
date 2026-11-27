#!/usr/bin/env python3

import json
import os
import sys
from typing import Dict, List, Any
from copy import deepcopy

# Default template
DEFAULT = {
	"name": "Dummy",
	"KSU": "Dummy",
	"KSU_COMPAT": "false",
	"KSU_SUSFS": "false",
	"C_LTO": "false",
	"No_DS": "false"
}

def config(**overrides) -> Dict[str, Any]:
	"""Create config from default with overrides."""
	cfg = deepcopy(DEFAULT)
	cfg.update(overrides)
	return cfg

# Build configurations
BUILD_CONFIGS: Dict[str, List[Dict[str, Any]]] = {
	"BUILD_VANILLA": [
		config(name="Vanilla", KSU="no"),
		config(name="Vanilla+NoLTO", KSU="vnlto")
	],
	"BUILD_KSU": [
		config(name="KSU", KSU="KSU")
	],
	"BUILD_KSU_SUSFS": [
		config(name="KSU+SUSFS", KSU="KSU", KSU_SUSFS="true"),
		config(name="Compat+KSU+SUSFS", KSU="KSU", KSU_COMPAT="true", KSU_SUSFS="true"),
		config(name="BSU+SUSFS", KSU="BSU", KSU_SUSFS="true"),
		config(name="Compat+BSU+SUSFS", KSU="BSU", KSU_COMPAT="true", KSU_SUSFS="true"),
		config(name="Compat+NoDS+KSU+SUSFS", KSU="KSU", KSU_COMPAT="true", KSU_SUSFS="true", No_DS="true"),
		config(name="Compat+NoDS+BSU+SUSFS", KSU="BSU", KSU_COMPAT="true", KSU_SUSFS="true", No_DS="true")
	]
}

ALL_KERNEL_VERSIONS: List[str] = ["5.10", "5.15", "6.1", "6.6", "6.12"]

def get_env_bool(var_name: str, default: bool = False) -> bool:
	"""Read environment variable as boolean."""
	value = os.environ.get(var_name, "").strip().lower()
	if not value:
		return default
	return value in ("true", "1", "yes", "on")

def resolve_kernel_versions() -> List[str]:
	selected = os.environ.get("KERNEL_VERSION", "All").strip()

	if not selected or selected.lower() == "all":
		return ALL_KERNEL_VERSIONS

	if selected not in ALL_KERNEL_VERSIONS:
		raise ValueError(
			f"Unknown KERNEL_VERSION='{selected}'. "
			f"Expected 'All' or one of: {', '.join(ALL_KERNEL_VERSIONS)}"
		)

	return [selected]

def generate_matrix() -> Dict[str, List[Dict[str, Any]]]:
	variant_entries = []

	for env_var, configs in BUILD_CONFIGS.items():
		if get_env_bool(env_var):
			variant_entries.extend(configs)

	if not variant_entries:
		raise ValueError(
			"No build configurations selected! "
			"Set at least one BUILD_* environment variable to 'true'."
		)

	kernel_versions = resolve_kernel_versions()

	entries = []
	for kernel_version in kernel_versions:
		for variant in variant_entries:
			entry = deepcopy(variant)
			entry["kernel_version"] = kernel_version
			entry["name"] = f"{kernel_version}-{entry['name']}"
			entries.append(entry)

	return {"include": entries}

def main() -> None:
	try:
		matrix = generate_matrix()
		print(f"matrix={json.dumps(matrix)}")

		print(f"::notice::Generated {len(matrix['include'])} build configurations", file=sys.stderr)

	except Exception as e:
		print(f"::error::Failed to generate matrix: {str(e)}", file=sys.stderr)
		sys.exit(1)

if __name__ == "__main__":
	main()
