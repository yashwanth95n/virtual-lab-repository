/**
 * pdf-guard.js
 *
 * Casual-user deterrents for the PDF/NOTE viewer blocks in course-view.html.
 *
 * IMPORTANT LIMITATIONS (read before relying on this):
 * - This cannot stop Print Screen, OS-level screenshot tools, or screen recording.
 * - This cannot stop a user with devtools open from grabbing the file URL or bytes.
 * - This cannot stop "print to PDF" if the OS/browser exposes it outside the page's control.
 * - Real protection against redistribution requires server-side access control
 *   (auth-gated streaming endpoint, short-lived signed URLs, no direct static file
 *   access) plus optional watermarking of the PDF with the viewing user's identity
 *   so leaks are traceable. This script only removes the *easy, one-click* paths.
 */
(function () {
  'use strict';

  function isInsideGuard(el) {
    return !!(el && el.closest && el.closest('.pdf-guard'));
  }

  // Block Ctrl/Cmd+P (print) and Ctrl/Cmd+S (save) while focus is inside a guarded block.
  document.addEventListener('keydown', function (e) {
    const key = (e.key || '').toLowerCase();
    const isPrintOrSave = (e.ctrlKey || e.metaKey) && (key === 'p' || key === 's');
    if (!isPrintOrSave) return;

    const active = document.activeElement;
    const guardedRegionFocused = isInsideGuard(active);
    const anyGuardOnPage = document.querySelector('.pdf-guard');

    // If a guarded viewer is present and either it's focused or it's the only
    // meaningful content on screen, block the shortcut.
    if (anyGuardOnPage && (guardedRegionFocused || active === document.body)) {
      e.preventDefault();
      e.stopPropagation();
    }
  }, true);

  // Belt-and-suspenders: block right-click / context menu anywhere inside a guarded block.
  document.addEventListener('contextmenu', function (e) {
    if (isInsideGuard(e.target)) {
      e.preventDefault();
    }
  }, true);

  // Discourage drag-out-to-save on any <iframe>/<embed> inside a guarded block.
  document.addEventListener('dragstart', function (e) {
    if (isInsideGuard(e.target)) {
      e.preventDefault();
    }
  }, true);
})();
