# Apply this prepared update

GitHub target: https://github.com/Rahat0764/BDAdmission
Base commit inspected: 48f4bbc1e998cddc42bd263d58679d5832378556

The connected GitHub integration returned `403 Resource not accessible by integration` when creating a blob. No commit or branch update was made. This archive contains the prepared replacement/additional files, not the entire original repository.

## Apply using Git

1. Clone the repository or make a fresh working copy of `main`.
2. Extract this archive into the repository root, overwriting the matching files.
3. Keep the existing `assets/logo/`, `assets/background/`, and `api/apk-url.js`. They are not replaced by this update archive.
4. Run `node scripts/build-data.cjs` and `node tests/admissions.test.cjs`.
5. Review the diff, then commit and push `main` (or use a branch/PR if preferred).
6. If Vercel is linked to `main`, verify the deployment after it builds. Check `/`, `/RU#B`, `/KU#A`, and `/data/admissions.js` directly. The data script must be JavaScript, not the HTML fallback.

## GitHub web upload

Alternatively, open the repository's Add file → Upload files flow and upload the extracted files/folders at the root. Uploading the ZIP itself does not apply the code changes. Do not remove existing logos/backgrounds/API files.

## Before announcing the update

- Check 320/390/768/1440px layouts and light/dark modes in a browser.
- Check RU's first and second shifts, application-open/closed states, date-only deadlines, search, reload and browser Back.
- Read `VERIFICATION.md` for fields that still lack confirmed circulars. This update does not claim all fees/times are published.
- Build `Native app source code/AIDE-Classic` in AIDE (or the Gradle profile in a compatible IDE). Source code is included; no compiled/tested APK is included.

Direct integration writes require access allowed by the GitHub App/token, including repository Contents write permission. A ChatGPT approval-mode setting cannot itself grant missing GitHub repository scopes.
