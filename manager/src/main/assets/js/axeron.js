let callbackCounter = 0;

// Resolves to { errno, stdout, stderr }. Runs through the callback variant of Axeron.exec,
// so the page keeps rendering while the command runs.
export function exec(command, options) {
  if (typeof options === "undefined") {
    options = {};
  }
  return new Promise((resolve, reject) => {
    const name = `axeron_exec_callback_${Date.now()}_${callbackCounter++}`;
    window[name] = (errno, stdout, stderr) => {
      delete window[name];
      resolve({ errno, stdout, stderr });
    };
    try {
      Axeron.exec(command, JSON.stringify(options), name);
    } catch (error) {
      delete window[name];
      reject(error);
    }
  });
}
