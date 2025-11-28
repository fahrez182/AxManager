export async function exec(command, options) {
  if (typeof options === "undefined") {
    options = {};
  }
  return await JSON.parse(Axeron.exec(command, JSON.stringify(options)));
}