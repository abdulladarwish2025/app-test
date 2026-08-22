import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

export class NftablesEnforcer {
  constructor({ binary = 'nft', family = 'inet', table = 'netquota' } = {}) {
    this.binary = binary;
    this.family = family;
    this.table = table;
  }

  async initialize() {
    await this.#run(['add', 'table', this.family, this.table], true);
    await this.#run(['add', 'set', this.family, this.table, 'blocked_v4', '{', 'type', 'ipv4_addr', ';', '}'], true);
    await this.#run(['add', 'chain', this.family, this.table, 'forward_guard', '{', 'type', 'filter', 'hook', 'forward', 'priority', '-5', ';', 'policy', 'accept', ';', '}'], true);
    await this.#run(['add', 'rule', this.family, this.table, 'forward_guard', 'ip', 'saddr', '@blocked_v4', 'drop'], true);
  }

  async setBlocked(ipAddress, blocked) {
    assertIpv4(ipAddress);
    const operation = blocked ? 'add' : 'delete';
    await this.#run([operation, 'element', this.family, this.table, 'blocked_v4', '{', ipAddress, '}'], !blocked || blocked);
  }

  async #run(args, tolerateExistingOrMissing = false) {
    try {
      await execFileAsync(this.binary, args, { windowsHide: true });
    } catch (error) {
      const stderr = String(error.stderr || '');
      if (tolerateExistingOrMissing && /File exists|No such file or directory/i.test(stderr)) return;
      throw new Error(`nftables command failed: ${stderr || error.message}`);
    }
  }
}

function assertIpv4(value) {
  const parts = String(value).split('.');
  if (parts.length !== 4 || parts.some((part) => !/^\d{1,3}$/.test(part) || Number(part) > 255)) {
    throw new TypeError(`Invalid IPv4 address: ${value}`);
  }
}
