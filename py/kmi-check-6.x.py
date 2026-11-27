#!/usr/bin/env python3
# encoding: utf-8

import os
import sys
import re
import itertools
from typing import Final, Dict, Set, List, Tuple
from rich.console import Console
from rich.table import Table


__author__: Final = "Pzqqt"

LOCAL_DIR: Final = os.path.dirname(os.path.abspath(__file__))


def crc_to_int(crc: str) -> int:
    if re.match(r'0x[0-9a-fA-F]+', crc):
        return int(crc[2:], 16)
    raise ValueError(crc)

def int_to_crc(num: int) -> str:
    return '0x' + hex(num)[2:].zfill(8)


def parse_stg_file(filepath: str) -> Dict[str, int]:
    """Parse the .stg file format to extract symbol names and CRC values."""
    symbols = {}
    current_symbol = None
    current_name = None

    with open(filepath, 'r', encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            # Start of a new symbol block
            if line == "elf_symbol {":
                current_symbol = {}
                continue

            # End of a symbol block
            if line == "}":
                if current_symbol and "name" in current_symbol and "crc" in current_symbol:
                    symbols[current_symbol["name"]] = crc_to_int(current_symbol["crc"])
                current_symbol = None
                continue

            # Parse key-value pairs within the symbol block
            if current_symbol is not None and ":" in line:
                key, value = line.split(":", 1)
                key = key.strip()
                value = value.strip()

                # Remove quotes if present
                if value.startswith('"') and value.endswith('"'):
                    value = value[1:-1]

                current_symbol[key] = value
    return symbols


def main(abi_gki_aarch64_stg_file: str, vmlinux_symvers_file: str) -> int:
    assert os.path.isfile(abi_gki_aarch64_stg_file)
    assert os.path.isfile(vmlinux_symvers_file)

    # Parse the .stg file
    abi_gki_aarch64_elf_symbols = parse_stg_file(abi_gki_aarch64_stg_file)

    # Parse vmlinux.symvers file (unchanged)
    with open(vmlinux_symvers_file, 'r', encoding="utf-8") as f:
        vmlinux_symvers = {
            line.split()[1]: crc_to_int(line.split()[0]) for line in f.readlines()
        }

    if missing_symbols := abi_gki_aarch64_elf_symbols.keys() - vmlinux_symvers.keys():
        print("Warning: The kernel image is missing the following symbols:")
        for symbol in missing_symbols:
            print('-', symbol)

    diff_crc_items = [
        (key, abi_gki_aarch64_elf_symbols[key], vmlinux_symvers[key])
        for key in set(abi_gki_aarch64_elf_symbols.keys()) & set(vmlinux_symvers.keys())
        if abi_gki_aarch64_elf_symbols[key] != vmlinux_symvers[key]
    ]

    if not diff_crc_items:
        print("No mismatching crc values found. Good job!")
        return 0

    rich_console = Console()

    rich_table = Table(show_header=True, header_style="bold magenta")
    rich_table.add_column("Function/variable symbol", style="dim")
    rich_table.add_column("Crc from abi_gki_aarch64.stg")
    rich_table.add_column("Crc from vmlinux.symvers")
    for item in sorted(diff_crc_items, key=lambda x: x[0]):
        rich_table.add_row(item[0], int_to_crc(item[1]), int_to_crc(item[2]))

    rich_console.print(rich_table)

    print(f"Found {len(diff_crc_items)} function/variable symbol(s) with mismatched crc values!")
    return 1

if __name__ == "__main__":
    if len(sys.argv) == 1:
        sys.exit(main(
            os.path.join(LOCAL_DIR, 'android', 'abi_gki_aarch64.stg'),
            os.path.join(LOCAL_DIR, 'out', 'vmlinux.symvers')
        ))
    if len(sys.argv) == 3:
        sys.exit(main(sys.argv[1], sys.argv[2]))
    print(f'Usage: {sys.argv[0]} <abi_gki_aarch64.stg file> <vmlinux.symvers file>')
    sys.exit(2)
