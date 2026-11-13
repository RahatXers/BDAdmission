const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, '..');
const data = JSON.parse(fs.readFileSync(path.join(root, 'data/admissions.json'), 'utf8'));
fs.writeFileSync(path.join(root, 'data/admissions.js'), '/* Source: admissions.json. Run node scripts/build-data.cjs after editing. */\nwindow.ADMISSIONS = ' + JSON.stringify(data, null, 2) + ';\n');
