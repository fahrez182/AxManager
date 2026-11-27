#!/usr/bin/env python3

import re
import sys

PATTERN = re.compile(r"`[^`\n]*`|\[[^\]\n]+\]\([^)]+\)")


def escape(text):
	return re.sub(r"([\\_*\[\]()~`>#+\-=|{}.!])", r"\\\1", text)


def convert(text):
	result = []
	last_end = 0

	for m in PATTERN.finditer(text):
		result.append(escape(text[last_end:m.start()]))
		token = m.group(0)

		if token.startswith("`"):
			result.append("\\`" + escape(token[1:-1]) + "\\`")
		else:
			label, url = re.fullmatch(r"\[([^\]]+)\]\(([^)]+)\)", token).groups()
			result.append(f"[{escape(label)}]({url})")

		last_end = m.end()

	result.append(escape(text[last_end:]))
	return "".join(result)


def main():
	if len(sys.argv) == 3 and sys.argv[1] == "--escape":
		sys.stdout.write(escape(sys.argv[2]))
		return
	if len(sys.argv) != 2:
		print(f"Usage: {sys.argv[0]} <markdown-file>", file=sys.stderr)
		sys.exit(1)
	with open(sys.argv[1], encoding="utf-8") as file:
		sys.stdout.write(convert(file.read()))

if __name__ == "__main__":
	main()
