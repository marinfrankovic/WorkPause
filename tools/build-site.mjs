import { copyFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const output = resolve(root, '.site-build');
mkdirSync(join(output, 'assets'), { recursive: true });

for (const name of ['index.html', 'styles.css']) {
  copyFileSync(join(root, 'site', name), join(output, name));
}
for (const [source, name] of [
  [['docs', 'workpause-icon.svg'], 'workpause-icon.svg'],
  [['docs', 'screenshots', 'workpause-main.png'], 'workpause-main.png'],
]) {
  copyFileSync(join(root, ...source), join(output, 'assets', name));
}
writeFileSync(join(output, '.nojekyll'), '');
console.log(`Website built at ${output}`);
