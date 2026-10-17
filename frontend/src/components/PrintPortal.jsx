import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';

/**
 * Full-screen print preview. What you see on screen is exactly what prints:
 * when printing, CSS (index.css) hides everything on the page except this portal, hides the toolbar,
 * and lays the "paper" out for A4. The dashboard, sidebar, filters and buttons never reach the printout.
 */
export default function PrintPortal({ title, onClose, children, actions, ready = true }) {
  const [el] = useState(() => {
    const d = document.createElement('div');
    d.className = 'print-portal';
    return d;
  });

  useEffect(() => {
    document.body.appendChild(el);
    const previousTitle = document.title;
    if (title) document.title = title; // becomes the default file name when saving as PDF
    return () => {
      document.title = previousTitle;
      if (el.parentNode) el.parentNode.removeChild(el);
    };
  }, [el, title]);

  return createPortal(
    <div className="print-overlay">
      <div className="print-toolbar no-print">
        <div className="text-sm font-semibold">{title}</div>
        <div className="flex gap-2">
          {actions}
          <button
            onClick={() => window.print()}
            disabled={!ready}
            className="bg-[#2563eb] hover:bg-blue-700 disabled:opacity-50 text-white px-5 py-2 rounded-lg text-[13px] font-bold"
          >
            Print
          </button>
          <button onClick={onClose} className="bg-white/90 hover:bg-white text-slate-700 px-5 py-2 rounded-lg text-[13px] font-bold">
            Close
          </button>
        </div>
      </div>
      <div className="print-paper">{children}</div>
    </div>,
    el
  );
}
