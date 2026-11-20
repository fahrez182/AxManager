/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   modals.js — Device & Package add/edit/delete modals
   Built as bottom-sheets (same visual family as theme/lang sheets).
   Data goes through COPG.* (js/copg-data.js); on save calls
   window.LibRefresh() (defined in library.js) to persist + re-render.
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
(function (w) {

  /* ─── DOM scaffold: inject three sheets + reuse #sheetOverlay ─── */
  const mount = document.getElementById('modalRoot') || document.body;

  /* little info "ⓘ" used on the cpu/got toggle rows → tap for a plain explanation */
  const INFO_SVG =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`;
  const WARN_SVG =
    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;

  mount.insertAdjacentHTML('beforeend', `
    <!-- DEVICE MODAL -->
    <div class="bottom-sheet sheet--form" id="deviceModal" role="dialog" aria-modal="true">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" id="deviceModalTitle" data-i18n="dev_add_title">Add Device</span>
        <button class="sheet-header__close" data-close="deviceModal" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <form class="sheet-body form-body" id="deviceForm" autocomplete="off" novalidate>
        <label class="field"><span class="field__label" data-i18n="dev_f_name">Device Name</span>
          <input class="field__input" id="dfName" type="text" placeholder="Pixel 8 Pro" required /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_brand">Brand</span>
          <input class="field__input" id="dfBrand" type="text" placeholder="google" required /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_model">Model</span>
          <input class="field__input" id="dfModel" type="text" placeholder="Pixel 8 Pro" required /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_product">Product</span>
          <input class="field__input field__input--mono" id="dfProduct" type="text" placeholder="husky" /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_device">Device</span>
          <input class="field__input field__input--mono" id="dfDevice" type="text" placeholder="husky" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_manufacturer">Manufacturer</span>
          <input class="field__input" id="dfManufacturer" type="text" placeholder="Google" required /></label>
        <label class="field"><span class="field__label" data-i18n="dev_f_fingerprint">Fingerprint</span>
          <input class="field__input field__input--mono" id="dfFingerprint" type="text" placeholder="google/husky/husky:15/AP3A.240617.008/12345678:user/release-keys" required /></label>
        <div class="adv-group" id="dfAdvGroup">
          <button type="button" class="adv-toggle" id="dfAdvToggle" aria-expanded="false">
            <span class="field__label" data-i18n="dev_adv_title">Advanced build props</span>
            <span class="field__opt" data-i18n="opt_optional">(optional)</span>
            <span class="field__warn field__warn--inline" id="dfAdvInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span>
            <svg class="adv-caret" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div class="adv-body" id="dfAdvBody" hidden>
            <div class="field-row">
              <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_android">Android Version</span></span>
                <input class="field__input" id="dfAndroid" type="text" inputmode="decimal" placeholder="15" /></label>
              <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_sdk">SDK Int</span><span class="field__warn field__warn--inline" id="dfSdkWarn" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
                <input class="field__input" id="dfSdk" type="text" inputmode="numeric" placeholder="35" /></label>
            </div>
            <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_serial">Serial Number</span><span class="field__gen" id="dfSerialGen" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
              <input class="field__input field__input--mono" id="dfSerial" type="text" maxlength="24" placeholder="1A2B3C4D" /></label>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_board">Board</span>
                <input class="field__input field__input--mono" id="dfBoard" type="text" placeholder="sm8650" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_hardware">Hardware</span>
                <input class="field__input field__input--mono" id="dfHardware" type="text" placeholder="qcom" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_buildid">Build ID</span>
                <input class="field__input field__input--mono" id="dfId" type="text" placeholder="AP3A.240617.008" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_display">Display ID</span>
                <input class="field__input field__input--mono" id="dfDisplay" type="text" placeholder="AP3A.240617.008" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_bootloader">Bootloader</span>
                <input class="field__input field__input--mono" id="dfBootloader" type="text" placeholder="unknown" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_tags">Build Tags</span>
                <input class="field__input field__input--mono" id="dfTags" type="text" placeholder="release-keys" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_btype">Build Type</span>
                <input class="field__input field__input--mono" id="dfType" type="text" placeholder="user" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_codename">Codename</span>
                <input class="field__input field__input--mono" id="dfCodename" type="text" placeholder="REL" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <div class="field-row">
              <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_secpatch">Security Patch</span><span class="field__gen" id="dfSecPatchGen" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
                <input class="field__input field__input--mono" id="dfSecPatch" type="text" placeholder="2025-09-05" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_incremental">Incremental</span><span class="field__gen" id="dfIncrementalGen" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
                <input class="field__input field__input--mono" id="dfIncremental" type="text" placeholder="20250910.230227" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_socman">SOC Manufacturer</span>
                <input class="field__input field__input--mono" id="dfSocMan" type="text" placeholder="Qualcomm" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_socmodel">SOC Model</span>
                <input class="field__input field__input--mono" id="dfSocModel" type="text" placeholder="SM8650" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <label class="field"><span class="field__head"><span class="field__label" data-i18n="dev_f_radio">Baseband version</span><span class="field__gen" id="dfRadioGen" role="button" tabindex="0" data-i18n="serial_gen">Generate</span></span>
              <input class="field__input field__input--mono" id="dfRadio" type="text" placeholder="MPSS.HI.3.0.c4-00108-SM8650_GEN_PACK-1" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            <div class="field-row">
              <label class="field"><span class="field__label" data-i18n="dev_f_sku">SKU</span>
                <input class="field__input field__input--mono" id="dfSku" type="text" placeholder="GC3VE" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
              <label class="field"><span class="field__label" data-i18n="dev_f_odmsku">ODM SKU</span>
                <input class="field__input field__input--mono" id="dfOdmSku" type="text" placeholder="husky" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            </div>
            <label class="field"><span class="field__label" data-i18n="dev_f_baseos">Base OS</span>
              <input class="field__input field__input--mono" id="dfBaseOs" type="text" placeholder="google/husky/husky:14/AP1A.240505.005/11583682:user/release-keys" autocapitalize="off" autocorrect="off" spellcheck="false" /></label>
            <div class="field">
              <span class="field__head"><span class="field__label" data-i18n="dev_f_props">Custom system props</span><span class="field__opt" data-i18n="opt_optional">(optional)</span><span class="field__warn field__warn--inline" id="dfPropsInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
              <div id="dfPropsList" class="props-list"></div>
              <button type="button" class="props-add" id="dfPropsAdd" data-i18n="dev_props_add">+ Add property</button>
            </div>
          </div>
        </div>
        <div class="form-buttons">
          <button type="button" class="btn btn--ghost" data-close="deviceModal" data-i18n="btn_cancel">Cancel</button>
          <button type="submit" class="btn btn--primary" data-i18n="btn_save">Save</button>
        </div>
      </form>
    </div>

    <!-- DEVICE SHARE (export/import code) MODAL -->
    <div class="bottom-sheet sheet--form" id="deviceShareModal" role="dialog" aria-modal="true">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" id="dsTitle" data-i18n="dev_share_title">Share Device</span>
        <button class="sheet-header__close" data-close="deviceShareModal" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="sheet-body form-body">
        <p class="ds-hint" id="dsHint" data-i18n="dev_export_hint">Copy this code and send it to anyone — they can import this device without re-typing every field.</p>
        <label class="ds-pkgrow" id="dsPkgRow" hidden>
          <input type="checkbox" id="dsInclPkgs" />
          <span id="dsPkgLabel" data-i18n="dev_incl_pkgs">Include packages</span>
        </label>
        <label class="field"><span class="field__label" data-i18n="dev_code_label">Device Code</span>
          <textarea class="field__input" id="dsCode" rows="4" spellcheck="false" autocapitalize="off" autocomplete="off" placeholder="COPG-DEV1:…"></textarea></label>
        <div class="sheet-footer">
          <button type="button" class="btn btn--ghost" data-close="deviceShareModal" data-i18n="btn_cancel">Cancel</button>
          <button type="button" class="btn btn--primary" id="dsAction" data-i18n="act_copy">Copy</button>
        </div>
      </div>
    </div>

    <!-- PACKAGE MODAL -->
    <div class="bottom-sheet sheet--form" id="packageModal" role="dialog" aria-modal="true">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" id="packageModalTitle" data-i18n="pkg_add_title">Add Package</span>
        <button class="sheet-header__close" data-close="packageModal" aria-label="Close">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <form class="sheet-body form-body" id="packageForm" autocomplete="off">
        <div class="pf-batch-hint" id="pfBatchHint" hidden data-i18n="pkg_batch_hint">Only the options you switch are applied to every selected app.</div>
        <label class="field"><span class="field__label" data-i18n="pkg_f_package">Package Name</span>
          <div class="field-with-btn">
            <input class="field__input field__input--mono" id="pfPackage" type="text" required placeholder="com.example.app" />
            <button type="button" class="field-pick-btn" id="pfPickBtn" data-i18n-attr="aria-label:pick_from_installed" aria-label="Pick from installed apps">
              <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="2.2" fill="currentColor" stroke="none"/><rect x="14" y="3" width="7" height="7" rx="2.2"/><rect x="3" y="14" width="7" height="7" rx="2.2"/><rect x="14" y="14" width="7" height="7" rx="2.2"/></svg>
            </button>
          </div></label>
        <label class="field"><span class="field__label" data-i18n="pkg_f_name">Display Name</span>
          <input class="field__input" id="pfName" type="text" placeholder="My Game" /></label>
        <!-- Per-user scope: read-only tag, set by the picker's "Apps for" drawer (not a chooser).
             Hidden unless this entry is scoped to a specific Android user (a clone). -->
        <div class="pf-usertag" id="pfUserTag" hidden></div>


        <div class="field" id="pfDeviceField"><span class="field__label" data-i18n="pkg_f_device">Device Profile</span>
          <button type="button" class="field__input field__picker" id="pfDevice" data-i18n-attr="aria-label:pkg_f_device" aria-label="Device Profile">
            <span class="field__picker-text is-placeholder" id="pfDeviceText" data-i18n="pkg_pick_device">Choose a device…</span>
            <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button></div>

        <div id="pfToggles">
         <div class="toggles toggle-group" id="pfSpoofGroup">
          <button type="button" class="toggles__head toggles__head--btn" id="pfSpoofHead" aria-expanded="false">
            <span data-i18n="pkg_sec_spoofing">Spoofing</span>
            <svg class="adv-caret" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div class="toggle-body" id="pfSpoofBody" hidden>
          <label class="trow" id="pfRandidRow">
            <span class="trow__label"><span data-i18n="pkg_t_randid">Randomize IDs each launch</span>
              <span class="trow__pro" id="pfRandidPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="randid" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfRandid"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-hint" id="pfRandidHint" hidden></div>
          <label class="trow" id="pfWithCpuRow">
            <span class="trow__label"><span data-i18n="pkg_t_withcpu">With CPU Spoofing</span>
              <span class="trow__info" role="button" tabindex="0" data-info="withcpu" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfWithCpu"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfCpuRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_cpu_model">CPU Model</span>
            <button type="button" class="field__input field__picker" id="pfCpuModel" data-i18n-attr="aria-label:pkg_cpu_model" aria-label="CPU Model">
              <span class="field__picker-text" id="pfCpuModelText" data-i18n="pkg_cpu_default">Qualcomm Snapdragon 8 Elite</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <label class="trow" id="pfBlockRow">
            <span class="trow__label"><span data-i18n="pkg_t_block">Block CPU</span>
              <span class="trow__info" role="button" tabindex="0" data-info="block" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfBlock"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfCowRow">
            <span class="trow__label"><span data-i18n="pkg_t_cow">COW Prop Spoof</span>
              <span class="trow__pro" id="pfCowPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="cow" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfCow"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfAidRow">
            <span class="trow__label"><span data-i18n="pkg_t_aid">Spoof Android ID</span>
              <span class="trow__pro" id="pfAidPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" id="pfAidInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfAid"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfAidValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_aid_value">Android ID</span>
            <input class="field__input" id="pfAidVal" type="text" maxlength="16" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from seed)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfAidGen" data-i18n-attr="aria-label:pkg_aid_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfSerialRow">
            <span class="trow__label"><span data-i18n="pkg_t_serial">Custom serial</span>
              <span id="pfSerialHint" hidden data-i18n="pkg_serial_locked" style="font-size:11px;opacity:.6;margin-left:6px">Set by device profile</span>
              <span class="trow__info" data-info="serial" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfSerial"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfSerialValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_serial_value">Serial</span>
            <input class="field__input" id="pfSerialVal" type="text" maxlength="24" spellcheck="false" autocapitalize="characters" autocomplete="off" placeholder="auto (from package)" style="flex:1;min-width:0;text-transform:uppercase" />
            <button type="button" class="field__input field__picker" id="pfSerialGen" data-i18n-attr="aria-label:pkg_serial_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfGaidRow">
            <span class="trow__label"><span data-i18n="pkg_t_gaid">Spoof Advertising ID</span>
              <span class="trow__pro" id="pfGaidPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" id="pfGaidInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfGaid"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfGaidValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_gaid_value">Advertising ID</span>
            <input class="field__input" id="pfGaidVal" type="text" maxlength="36" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from seed)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfGaidGen" data-i18n-attr="aria-label:pkg_gaid_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfAppSetRow">
            <span class="trow__label"><span data-i18n="pkg_t_appset">Spoof App Set ID</span>
              <span class="trow__pro" id="pfAppSetPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" id="pfAppSetInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfAppSet"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfAppSetValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_appset_value">App Set ID</span>
            <input class="field__input" id="pfAppSetVal" type="text" maxlength="36" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfAppSetGen" data-i18n-attr="aria-label:pkg_appset_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfHideDevRow">
            <span class="trow__label"><span data-i18n="pkg_t_hidedev">Hide Dev Options</span>
              <span class="trow__info" data-info="hidedev" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfHideDev"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow" id="pfTzRow">
            <span class="trow__label"><span data-i18n="pkg_t_tz">Timezone</span>
              <span class="trow__info" data-info="tz" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <button type="button" class="field__input field__picker" id="pfTzPick" style="flex:none;width:auto;max-width:60%" data-i18n-attr="aria-label:pkg_t_tz" aria-label="Timezone">
              <span class="field__picker-text" id="pfTzText" data-i18n="pkg_tz_off">Real (off)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <div class="trow" id="pfLangRow">
            <span class="trow__label"><span data-i18n="pkg_t_lang">Language</span>
              <span class="trow__info" data-info="lang" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <button type="button" class="field__input field__picker" id="pfLangPick" style="flex:none;width:auto;max-width:60%" data-i18n-attr="aria-label:pkg_t_lang" aria-label="Language">
              <span class="field__picker-text" id="pfLangText" data-i18n="pkg_lang_off">Real (off)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <label class="trow" id="pfWallpaperRow">
            <span class="trow__label"><span data-i18n="pkg_t_wallpaper">Wallpaper ID</span>
              <span class="trow__pro" id="pfWallpaperPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="wallpaper" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfWallpaper"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfWallpaperIdRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_wallpaper_id">Fake ID</span>
            <input type="number" inputmode="numeric" class="field__input" id="pfWallpaperId" style="flex:none;width:auto;max-width:50%" placeholder="23" data-i18n-attr="placeholder:pkg_wallpaper_ph" aria-label="Fake wallpaper id"></div>
          <label class="trow" id="pfAccountRow">
            <span class="trow__label"><span data-i18n="pkg_t_account">Google Account</span>
              <span class="trow__pro" id="pfAccountPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="account" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfAccount"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfAccountModeRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_account_mode">Mode</span>
            <div class="seg" id="pfAccountMode" data-mode="hide">
              <button type="button" class="seg__opt active" data-mode="hide" data-i18n="pkg_account_hide">Hide</button>
              <button type="button" class="seg__opt" data-mode="fake" data-i18n="pkg_account_fake">Fake email</button>
            </div></div>
          <div class="trow-sub" id="pfAccountEmailRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_account_email">Fake email</span>
            <input type="email" inputmode="email" autocapitalize="none" autocomplete="off" class="field__input" id="pfAccountEmail" style="flex:none;width:auto;max-width:62%" placeholder="jane.doe@gmail.com" data-i18n-attr="placeholder:pkg_account_ph" aria-label="Fake google account email"></div>
          <label class="trow" id="pfGpuRow">
            <span class="trow__label"><span data-i18n="pkg_t_gpu">GPU Spoof</span>
              <span class="trow__pro" id="pfGpuPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" role="button" tabindex="0" data-info="gpu" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfGpu"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfGpuModelRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_gpu_model">GPU Model</span>
            <button type="button" class="field__input field__picker" id="pfGpuModel" data-i18n-attr="aria-label:pkg_gpu_model" aria-label="GPU Model">
              <span class="field__picker-text" id="pfGpuModelText" data-i18n="pkg_gpu_default">Adreno 830 (Snapdragon 8 Elite)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <label class="trow" id="pfMockRow">
            <span class="trow__label"><span data-i18n="pkg_t_mock">Mock Location Hide</span>
              <span class="trow__pro" id="pfMockPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="mock" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfMock"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfSensorRow">
            <span class="trow__label"><span data-i18n="pkg_t_sensor">Sensor Spoof</span>
              <span class="trow__pro" id="pfSensorPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="sensor" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfSensor"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfGetpropRow">
            <span class="trow__label"><span data-i18n="pkg_t_getprop">getprop Consistency</span>
              <span class="trow__pro" id="pfGetpropPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="getprop" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfGetprop"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfGpsRow">
            <span class="trow__label"><span data-i18n="pkg_t_gps">GPS Location Spoof</span>
              <span class="trow__pro" id="pfGpsPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="gps" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfGps"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfGpsLocRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_gps_loc">Location</span>
            <button type="button" class="field__input field__picker" id="pfGpsPick" data-i18n-attr="aria-label:pkg_gps_loc" aria-label="Location">
              <span class="field__picker-text" id="pfGpsPickText" data-i18n="pkg_gps_choose">Choose a place…</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <label class="trow" id="pfCellRow">
            <span class="trow__label"><span data-i18n="pkg_t_cell">Cell Tower Spoof</span>
              <span class="trow__pro" id="pfCellPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="cell" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfCell"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfCellLacRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_cell_lac">Area Code (LAC)</span>
            <input class="field__input" id="pfCellLac" type="text" inputmode="numeric" maxlength="12" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="1053442" style="flex:1;min-width:0" /></div>
          <div class="trow-sub" id="pfCellCiRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_cell_ci">Cell Identity (CI)</span>
            <input class="field__input" id="pfCellCi" type="text" inputmode="numeric" maxlength="14" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="4312477737" style="flex:1;min-width:0" /></div>
          <div class="trow-sub" id="pfCellMccRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_cell_mcc">MCC (optional)</span>
            <input class="field__input" id="pfCellMcc" type="text" inputmode="numeric" maxlength="3" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="310" style="flex:1;min-width:0" /></div>
          <div class="trow-sub" id="pfCellMncRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_cell_mnc">MNC (optional)</span>
            <input class="field__input" id="pfCellMnc" type="text" inputmode="numeric" maxlength="3" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="260" style="flex:1;min-width:0" /></div>
          <label class="trow" id="pfUptimeRow">
            <span class="trow__label"><span data-i18n="pkg_t_uptime">Fake Uptime</span>
              <span class="trow__pro" id="pfUptimePro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="uptime" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfUptime"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfUptimeValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_uptime_value">Uptime (days)</span>
            <input class="field__input" id="pfUptimeD" type="number" min="0" max="3650" step="0.1" inputmode="decimal" placeholder="e.g. 5" style="flex:1;min-width:0" />
          </div>
          <label class="trow" id="pfBootidRow">
            <span class="trow__label"><span data-i18n="pkg_t_bootid">Spoof Boot ID</span>
              <span class="trow__pro" id="pfBootidPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" id="pfBootidInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfBootid"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow-sub" id="pfBootidChurnRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_bootid_churn">New ID each launch</span>
            <span class="toggle"><input type="checkbox" id="pfBootidChurn"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfBootidValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_bootid_value">Boot ID</span>
            <input class="field__input" id="pfBootidVal" type="text" maxlength="36" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from app)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfBootidGen" data-i18n-attr="aria-label:pkg_bootid_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfMacRow">
            <span class="trow__label"><span data-i18n="pkg_t_mac">Spoof WiFi MAC</span>
              <span class="trow__pro" id="pfMacPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" id="pfMacInfo" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfMac"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow-sub" id="pfMacChurnRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_mac_churn">New MAC each launch</span>
            <span class="toggle"><input type="checkbox" id="pfMacChurn"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfMacValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_mac_value">MAC address</span>
            <input class="field__input" id="pfMacVal" type="text" maxlength="17" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from app)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfMacGen" data-i18n-attr="aria-label:pkg_mac_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfBtmacRow">
            <span class="trow__label"><span data-i18n="pkg_t_btmac">Spoof Bluetooth MAC</span>
              <span class="trow__pro" id="pfBtmacPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="btmac" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfBtmac"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow-sub" id="pfBtmacChurnRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_btmac_churn">New BT MAC each launch</span>
            <span class="toggle"><input type="checkbox" id="pfBtmacChurn"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfBtmacValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_btmac_value">BT MAC address</span>
            <input class="field__input" id="pfBtmacVal" type="text" maxlength="17" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from app)" style="flex:1;min-width:0;text-transform:uppercase" />
            <button type="button" class="field__input field__picker" id="pfBtmacGen" data-i18n-attr="aria-label:pkg_mac_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfRrRow">
            <span class="trow__label"><span data-i18n="pkg_t_rr">Spoof Refresh Rate</span>
              <span class="trow__info trow__info--warn" data-info="rr" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfRr"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfRrValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_rr_value">Refresh rate (Hz)</span>
            <input class="field__input" id="pfRrHz" type="number" min="24" max="480" step="1" inputmode="numeric" placeholder="e.g. 120" style="flex:1;min-width:0" />
          </div>
          <label class="trow" id="pfDrmRow">
            <span class="trow__label"><span data-i18n="pkg_t_drm">Spoof DRM (Widevine)</span>
              <span class="trow__pro" id="pfDrmPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="drm" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfDrm"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfDrmLevelRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_drm_level">Security Level</span>
            <div class="seg" id="pfDrmLevel" data-level="L1">
              <button type="button" class="seg__opt active" data-level="L1">L1</button>
              <button type="button" class="seg__opt" data-level="L2">L2</button>
              <button type="button" class="seg__opt" data-level="L3">L3</button>
            </div>
          </div>
          <div class="trow-sub" id="pfDrmIdRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_drmid_value">Device Unique ID</span>
            <input class="field__input" id="pfDrmIdVal" type="text" maxlength="44" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfDrmIdFmt" data-i18n-attr="aria-label:pkg_drmid_fmt" aria-label="Toggle format" style="flex:none;width:auto;padding:0 10px;justify-content:center;font-size:11px;font-weight:600">B64</button>
            <button type="button" class="field__input field__picker" id="pfDrmIdGen" data-i18n-attr="aria-label:pkg_drmid_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <div class="trow-sub" id="pfDrmSysRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_drmsys_value">System ID</span>
            <input class="field__input" id="pfDrmSysVal" type="text" inputmode="numeric" maxlength="10" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfDrmSysGen" data-i18n-attr="aria-label:pkg_drmsys_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <div class="trow-subhead" data-i18n="pkg_grp_ids">Phone identifiers</div>
          <label class="trow" id="pfImeiRow">
            <span class="trow__label"><span data-i18n="pkg_t_imei">Spoof IMEI</span>
              <span class="trow__pro" id="pfImeiPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="imei" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfImei"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfImeiValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_imei_value">IMEI (SIM 1)</span>
            <input class="field__input" id="pfImeiVal" type="text" inputmode="numeric" maxlength="15" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfImeiGen" data-i18n-attr="aria-label:pkg_imei_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <div class="trow-sub" id="pfImei2ValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_imei2_value">IMEI (SIM 2)</span>
            <input class="field__input" id="pfImei2Val" type="text" inputmode="numeric" maxlength="15" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfImei2Gen" data-i18n-attr="aria-label:pkg_imei2_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfImsiRow">
            <span class="trow__label"><span data-i18n="pkg_t_imsi">Spoof IMSI</span>
              <span class="trow__pro" id="pfImsiPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="imsi" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfImsi"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfImsiValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_imsi_value">IMSI (SIM 1)</span>
            <input class="field__input" id="pfImsiVal" type="text" inputmode="numeric" maxlength="15" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfImsiGen" data-i18n-attr="aria-label:pkg_imsi_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <div class="trow-sub" id="pfImsi2ValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_imsi2_value">IMSI (SIM 2)</span>
            <input class="field__input" id="pfImsi2Val" type="text" inputmode="numeric" maxlength="15" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfImsi2Gen" data-i18n-attr="aria-label:pkg_imsi2_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfIccidRow">
            <span class="trow__label"><span data-i18n="pkg_t_iccid">Spoof ICCID</span>
              <span class="trow__pro" id="pfIccidPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="iccid" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfIccid"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfIccidValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_iccid_value">ICCID (SIM 1)</span>
            <input class="field__input" id="pfIccidVal" type="text" inputmode="numeric" maxlength="20" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfIccidGen" data-i18n-attr="aria-label:pkg_iccid_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <div class="trow-sub" id="pfIccid2ValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_iccid2_value">ICCID (SIM 2)</span>
            <input class="field__input" id="pfIccid2Val" type="text" inputmode="numeric" maxlength="20" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (per-app)" style="flex:1;min-width:0" />
            <button type="button" class="field__input field__picker" id="pfIccid2Gen" data-i18n-attr="aria-label:pkg_iccid2_gen" aria-label="Generate" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfWebuaRow">
            <span class="trow__label"><span data-i18n="pkg_t_webua">WebView User-Agent</span>
              <span class="trow__pro" id="pfWebuaPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="webua" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfWebua"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfWebuaValRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_webua_value">User-Agent</span>
            <button type="button" class="field__input field__picker" id="pfWebuaPick" data-i18n-attr="aria-label:pkg_t_webua" aria-label="User-Agent" style="flex:1;min-width:0">
              <span class="field__picker-text" id="pfWebuaPickText" data-i18n="pkg_webua_off">Choose…</span>
            </button>
          </div>
          <label class="trow" id="pfWebfpRow">
            <span class="trow__label"><span data-i18n="pkg_t_webfp">Canvas Spoof</span>
              <span class="trow__pro" id="pfWebfpPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info trow__info--warn" data-info="webfp" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfWebfp"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfWebfpValRow" style="display:none">
            <input class="field__input" id="pfWebfpVal" type="text" maxlength="10" spellcheck="false" autocapitalize="none" autocomplete="off" placeholder="auto (from package)" style="flex:1;min-width:0;text-transform:lowercase" />
            <button type="button" class="field__input field__picker" id="pfWebfpGen" data-i18n-attr="aria-label:pkg_webfp_gen" aria-label="Randomize" style="flex:none;width:auto;padding:0 12px;justify-content:center">↻</button>
          </div>
          <label class="trow" id="pfVpnRow">
            <span class="trow__label"><span data-i18n="pkg_t_vpn">Hide VPN</span>
              <span class="trow__info" data-info="vpns" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfVpn"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow-sub trow-sub--warn trow-sub--toggle" id="pfVpnAggRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_t_vpn_agg">Aggressive (in-app)</span>
            <span class="trow__info trow__info--warn" data-info="vpn" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${WARN_SVG}</span>
            <span class="toggle"><input type="checkbox" id="pfVpnAgg"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfPlaysrcRow">
            <span class="trow__label"><span data-i18n="pkg_t_playsrc">Fake Play Install Source</span>
              <span class="trow__pro" id="pfPlaysrcPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="playsrc" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfPlaysrc"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow-sub trow-sub--toggle" id="pfPlaysrcPairipRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_t_playsrc_pairip">Also block PairIP check</span>
            <span class="trow__info" data-info="pairip" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span>
            <span class="toggle"><input type="checkbox" id="pfPlaysrcPairip"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfPairipRow">
            <span class="trow__label"><span data-i18n="pkg_t_pairip">Block PairIP License Check</span>
              <span class="trow__pro" id="pfPairipPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="pairip" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfPairip"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfTensorfeatRow">
            <span class="trow__label"><span data-i18n="pkg_t_tensorfeat">Tensor Feature Targets</span>
              <span class="trow__pro" id="pfTensorfeatPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="tensorfeat" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfTensorfeat"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfSimRow">
            <span class="trow__label"><span data-i18n="pkg_t_sim">SIM / Carrier Spoof</span>
              <span class="trow__pro" id="pfSimPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="sim" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfSim"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfSimModeRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_sim_mode">Mode</span>
            <div class="seg" id="pfSimMode" data-mode="safe">
              <button type="button" class="seg__opt active" data-mode="safe" data-i18n="pkg_sim_safe">Safe</button>
              <button type="button" class="seg__opt" data-mode="aggressive"><span data-i18n="pkg_sim_aggr">Aggressive</span><span class="seg__unsafe" aria-hidden="true" title="Unsafe — resident/detectable">${WARN_SVG}</span></button>
              <button type="button" class="seg__opt" data-mode="phantom"><span data-i18n="pkg_sim_phantom">Add SIM</span><span class="seg__unsafe" aria-hidden="true" title="Unsafe — resident/detectable">${WARN_SVG}</span></button>
            </div></div>
          <div class="trow-sub" id="pfSim1Row" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_sim_slot1">SIM 1</span>
            <button type="button" class="field__input field__picker" id="pfSim1Model" data-i18n-attr="aria-label:pkg_sim_slot1" aria-label="SIM 1">
              <span class="field__picker-text" id="pfSim1Text" data-i18n="pkg_sim_none">None (real)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <div class="trow-sub" id="pfSim2Row" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_sim_slot2">SIM 2</span>
            <button type="button" class="field__input field__picker" id="pfSim2Model" data-i18n-attr="aria-label:pkg_sim_slot2" aria-label="SIM 2">
              <span class="field__picker-text" id="pfSim2Text" data-i18n="pkg_sim_none">None (real)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <div class="trow-sub" id="pfSimFakeRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_sim_added">Added SIM</span>
            <button type="button" class="field__input field__picker" id="pfSimFakeModel" data-i18n-attr="aria-label:pkg_sim_added" aria-label="Added SIM">
              <span class="field__picker-text" id="pfSimFakeText" data-i18n="pkg_sim_none">None (real)</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          <label class="trow" id="pfProxyRow">
            <span class="trow__label"><span data-i18n="pkg_t_proxy">Per-App Proxy</span>
              <span class="trow__pro" id="pfProxyPro" hidden><svg class="trow__pro-lock" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg><span data-i18n="lic_pro_tag">PRO</span></span>
              <span class="trow__info" data-info="proxy" role="button" tabindex="0" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="trow__lockico" aria-hidden="true"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg></span>
            <span class="toggle"><input type="checkbox" id="pfProxy"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <div class="trow-sub" id="pfProxyPickRow" style="display:none">
            <span class="trow-sub__label" data-i18n="pkg_proxy_pick">Proxy</span>
            <button type="button" class="field__input field__picker" id="pfProxyModel" data-i18n-attr="aria-label:pkg_proxy_pick" aria-label="Proxy">
              <span class="field__picker-text" id="pfProxyText" data-i18n="proxy_none">None</span>
              <svg class="field__picker-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
            </button></div>
          </div>
         </div>

         <div class="toggles toggle-group" id="pfTweakGroup">
          <button type="button" class="toggles__head toggles__head--btn" id="pfTweakHead" aria-expanded="false">
            <span data-i18n="pkg_sec_tweaks">Tweaks</span>
            <svg class="adv-caret" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          <div class="toggle-body" id="pfTweakBody" hidden>
          <label class="trow" id="pfDndRow">
            <span class="trow__label"><span data-i18n="pkg_t_dnd">Do Not Disturb</span>
              <span class="trow__info" role="button" tabindex="0" data-info="dnd" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfDnd"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfDabRow">
            <span class="trow__label"><span data-i18n="pkg_t_dab">Disable Auto-Brightness</span>
              <span class="trow__info" role="button" tabindex="0" data-info="dab" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfDab"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfKsoRow">
            <span class="trow__label"><span data-i18n="pkg_t_kso">Keep Screen On</span>
              <span class="trow__info" role="button" tabindex="0" data-info="kso" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfKso"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfNologRow">
            <span class="trow__label"><span data-i18n="pkg_t_nolog">Disable Logging</span>
              <span class="trow__info" role="button" tabindex="0" data-info="nolog" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <span class="toggle"><input type="checkbox" id="pfNolog"><span class="toggle__track"><span class="toggle__thumb"></span></span></span></label>
          <label class="trow" id="pfDpiRow">
            <span class="trow__label"><span data-i18n="pkg_t_dpi">Screen DPI</span>
              <span class="trow__info" role="button" tabindex="0" data-info="dpi" data-i18n-attr="aria-label:info_aria" aria-label="What's this?">${INFO_SVG}</span></span>
            <input class="field__input" id="pfDpi" type="number" min="120" max="640" step="1" inputmode="numeric" placeholder="e.g. 480" style="width:92px;flex:0 0 auto;text-align:center" /></label>
          </div>
         </div>
        </div>

        <div class="form-error" id="pfError" hidden></div>
        <div class="form-buttons">
          <button type="button" class="btn btn--ghost" data-close="packageModal" data-i18n="btn_cancel">Cancel</button>
          <button type="submit" class="btn btn--primary" data-i18n="btn_save">Save</button>
        </div>
      </form>
    </div>

    <!-- CONFIRM DELETE -->
    <div class="bottom-sheet sheet--confirm" id="confirmModal" role="dialog" aria-modal="true">
      <div class="sheet-handle"></div>
      <div class="sheet-header">
        <span class="sheet-header__title" id="confirmTitle">Delete</span>
      </div>
      <div class="sheet-body">
        <p class="confirm-msg" id="confirmMsg"></p>
        <a class="confirm-link" id="confirmLink" target="_blank" rel="noopener noreferrer" hidden></a>
        <label class="confirm-dsa" id="confirmDsaRow" hidden>
          <input type="checkbox" id="confirmDsa" />
          <span data-i18n="risk_dsa">Don't show this warning again</span>
        </label>
        <div class="form-buttons" id="confirmBtns">
          <button type="button" class="btn btn--ghost" id="confirmCancel" data-close="confirmModal" data-i18n="btn_cancel">Cancel</button>
          <button type="button" class="btn btn--ghost" id="confirmAlt" hidden></button>
          <button type="button" class="btn btn--danger" id="confirmOk" data-i18n="btn_delete">Delete</button>
        </div>
      </div>
    </div>
  `);

  const $m = s => document.querySelector(s);
  const overlay = () => document.getElementById('sheetOverlay');

  // Clearing any inline transform/transition is essential: a drag-to-dismiss
  // gesture (gestures.js) leaves an inline `transform:translateY()` that BEATS
  // the `.bottom-sheet.open` rule, so a sheet could otherwise never visually
  // reopen after being dragged (no error, no log — just "nothing happens").
  function clearDrag(el) { if (el) { el.style.transition = ''; el.style.transform = ''; } }

  function openModal(id) {
    overlay()?.classList.add('visible');
    const sheet = document.getElementById(id);
    clearDrag(sheet);
    requestAnimationFrame(() => sheet?.classList.add('open'));
  }
  function closeModal(id) {
    const sheet = document.getElementById(id);
    sheet?.classList.remove('open');
    clearDrag(sheet);
    if (![...document.querySelectorAll('.bottom-sheet.open')].length) overlay()?.classList.remove('visible');
  }
  function closeAll() {
    document.querySelectorAll('.bottom-sheet').forEach(s => { s.classList.remove('open'); clearDrag(s); });
    overlay()?.classList.remove('visible');
  }

  // close buttons + overlay
  document.querySelectorAll('[data-close]').forEach(b =>
    b.addEventListener('click', () => closeModal(b.dataset.close)));
  overlay()?.addEventListener('click', closeAll);

  /* field error helpers */
  function clearErrors(form) {
    form.querySelectorAll('.field--error').forEach(f => f.classList.remove('field--error'));
    form.querySelectorAll('.field-msg').forEach(m => m.remove());
  }
  function markError(inputEl, msg) {
    const field = inputEl.closest('.field') || inputEl.parentNode;
    field.classList.add('field--error');
    const span = document.createElement('span');
    span.className = 'field-msg';
    span.textContent = msg;
    field.appendChild(span);
  }
  function clearFieldError(inputEl) {
    const field = inputEl.closest('.field') || inputEl.parentNode;
    if (!field) return;
    field.classList.remove('field--error');
    field.querySelectorAll('.field-msg').forEach(m => m.remove());
  }

  /* ════════════ DEVICE MODAL ════════════ */
  let editingDeviceKey = null;

  /* Optional extra Build.* / Build$VERSION.* fields — [inputId, COPG.json key].
     Order/keys mirror EXTRA_BUILD_FIELDS in spoof_module.cpp. */
  const ADV_FIELDS = [
    ['dfBoard','BOARD'], ['dfHardware','HARDWARE'], ['dfId','ID'], ['dfDisplay','DISPLAY'],
    ['dfBootloader','BOOTLOADER'], ['dfTags','TAGS'], ['dfType','TYPE'],
    ['dfIncremental','INCREMENTAL'], ['dfSecPatch','SECURITY_PATCH'], ['dfCodename','CODENAME'],
    ['dfSocMan','SOC_MANUFACTURER'], ['dfSocModel','SOC_MODEL'], ['dfRadio','RADIO'],
    ['dfSku','SKU'], ['dfOdmSku','ODM_SKU'], ['dfBaseOs','BASE_OS'],
  ];
  function advValues() {
    const o = {};
    ADV_FIELDS.forEach(([id, key]) => { o[key] = $m('#' + id).value; });
    return o;
  }
  /* Custom-props editor: repeating key→value rows written to the profile's PROPS:{} (COW-forged
     for a `cow`-tagged app). Covers vendor props with no Build field — ril.*, ro.omc.*, etc. */
  function addPropRow(k, v) {
    const list = $m('#dfPropsList'); if (!list) return;
    const row = document.createElement('div');
    row.className = 'props-row';
    row.innerHTML =
      '<input class="field__input field__input--mono props-row__k" type="text" placeholder="ril.official_cscver" autocapitalize="off" autocorrect="off" spellcheck="false" />' +
      '<input class="field__input field__input--mono props-row__v" type="text" placeholder="G985FOWONHXL1" autocapitalize="off" autocorrect="off" spellcheck="false" />' +
      '<button type="button" class="props-row__del" aria-label="Remove">✕</button>';
    row.querySelector('.props-row__k').value = k || '';
    row.querySelector('.props-row__v').value = v || '';
    row.querySelector('.props-row__del').addEventListener('click', () => row.remove());
    list.appendChild(row);
  }
  function fillProps(obj) {
    const list = $m('#dfPropsList'); if (!list) return;
    list.innerHTML = '';
    if (obj && typeof obj === 'object') Object.keys(obj).forEach(k => addPropRow(k, obj[k]));
  }
  function propsValues() {
    const o = {};
    $$('.props-row', $m('#dfPropsList')).forEach(r => {
      const k = r.querySelector('.props-row__k').value.trim();
      const v = r.querySelector('.props-row__v').value.trim();
      if (k && v) o[k] = v;
    });
    return o;
  }
  function setAdvOpen(open) {
    const g = $m('#dfAdvGroup'), b = $m('#dfAdvBody'), t = $m('#dfAdvToggle');
    if (!g || !b) return;
    g.classList.toggle('is-open', !!open);
    b.hidden = !open;
    if (t) t.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  function wireAdvToggle() {
    const t = $m('#dfAdvToggle');
    if (!t) return;
    t.addEventListener('click', e => {
      if (e.target.closest('#dfAdvInfo')) return;   // ⓘ has its own handler
      setAdvOpen($m('#dfAdvBody').hidden);
    });
  }
  // Collapsible package-modal sections (Spoofing / Tweaks) — same accordion pattern
  // as the device modal's adv-group: header button toggles the body, caret rotates.
  function setGroupOpen(groupId, bodyId, open) {
    const g = $m('#' + groupId), b = $m('#' + bodyId);
    if (!g || !b) return;
    g.classList.toggle('is-open', !!open);
    b.hidden = !open;
    const btn = g.querySelector('.toggles__head--btn');
    if (btn) btn.setAttribute('aria-expanded', open ? 'true' : 'false');
  }
  function wireToggleGroups() {
    [['pfSpoofHead', 'pfSpoofGroup', 'pfSpoofBody'],
     ['pfTweakHead', 'pfTweakGroup', 'pfTweakBody']].forEach(([head, group, body]) => {
      const h = $m('#' + head);
      if (h) h.addEventListener('click', () => setGroupOpen(group, body, $m('#' + body).hidden));
    });
  }
  // Sections default collapsed; auto-expand one only if it already has an active
  // toggle (editing a package that uses it). Fresh add → both stay closed.
  function syncGroupsAutoOpen() {
    const on = ids => ids.some(id => { const e = $m('#' + id); return e && e.checked; });
    // tz + lang are value-pickers (dataset, not a checkbox) — count them as active too so the
    // Spoofing group auto-opens when only a Timezone/Language is set.
    const pickerOn = () => {
      const tz = $m('#pfTzPick'), lang = $m('#pfLangPick');
      return !!((tz && tz.dataset.tz) || (lang && lang.dataset.lang));
    };
    setGroupOpen('pfSpoofGroup', 'pfSpoofBody', on(SPOOF_CB) || pickerOn());
    // DPI is a value-input (not a checkbox) — count it as active so Tweaks auto-opens on edit.
    const dpiOn = () => { const d = $m('#pfDpi'); return !!(d && d.value); };
    setGroupOpen('pfTweakGroup', 'pfTweakBody', on(TWEAK_CB) || dpiOn());
  }
  function wireAdvInfo() {
    const w = $m('#dfAdvInfo');
    if (!w) return;
    const open = e => { if (e) { e.preventDefault(); e.stopPropagation(); }
      info({ title: I18N.t('info_adv_title'), message: I18N.t('info_adv_msg') }); };
    w.addEventListener('click', open);
    w.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') open(e); });
  }
  function wirePropsEditor() {
    const add = $m('#dfPropsAdd');
    if (add) add.addEventListener('click', () => addPropRow('', ''));
    const w = $m('#dfPropsInfo');
    if (w) {
      const open = e => { if (e) { e.preventDefault(); e.stopPropagation(); }
        info({ title: I18N.t('info_props_title'), message: I18N.t('info_props_msg') }); };
      w.addEventListener('click', open);
      w.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') open(e); });
    }
  }

  /* ─── Generators for the synthesizable advanced fields ─── */
  const _p2 = n => String(n).padStart(2, '0');
  function _randRecentDate() {                      // random instant within ~last 6 months
    const back = Math.floor(Math.random() * 180) * 86400000 + Math.floor(Math.random() * 86400000);
    return new Date(Date.now() - back);
  }
  function genSecPatch() {                          // YYYY-MM-05 (patch level, day is conventionally 01/05)
    const d = _randRecentDate();
    return `${d.getUTCFullYear()}-${_p2(d.getUTCMonth() + 1)}-05`;
  }
  function genIncremental() {                       // YYYYMMDD.HHMMSS — matches the build-number style in fingerprints
    const d = _randRecentDate();
    return `${d.getUTCFullYear()}${_p2(d.getUTCMonth() + 1)}${_p2(d.getUTCDate())}.${_p2(d.getUTCHours())}${_p2(d.getUTCMinutes())}${_p2(d.getUTCSeconds())}`;
  }
  function genRadio() {                              // Qualcomm MPSS baseband (the common Snapdragon format):
    // MPSS.HI.a.b.cN-BUILD-SOC_GEN_PACK-1 . Uses the profile's SOC Model (e.g. SM8650) when it looks
    // like an SMxxxx so the baseband is coherent with the chipset, else a random SMxxxx. Baseband
    // strings are chipset-specific — for an exact match, paste a real one from the target device.
    const soc  = ($m('#dfSocModel') && $m('#dfSocModel').value.trim()) || '';
    const chip = /^SM\w+$/i.test(soc) ? soc.toUpperCase() : ('SM' + (8350 + Math.floor(Math.random() * 400)));
    const a = 1 + Math.floor(Math.random() * 4), b = Math.floor(Math.random() * 3), c = 1 + Math.floor(Math.random() * 9);
    const build = _p2(Math.floor(Math.random() * 100)) + String(100 + Math.floor(Math.random() * 900));
    return `MPSS.HI.${a}.${b}.c${c}-${build}-${chip}_GEN_PACK-1`;
  }
  /* Generic Generate-button wiring (non-labelable span → never steals the row tap). */
  function wireGen(btnId, inputId, genFn) {
    const g = $m('#' + btnId), s = $m('#' + inputId);
    if (!g || !s) return;
    const gen = e => { if (e) { e.preventDefault(); e.stopPropagation(); } s.value = genFn(); flash(s); };
    g.addEventListener('click', gen);
    g.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') gen(e); });
  }

  function openDevice(deviceKey) {
    const form = $m('#deviceForm');
    clearErrors(form);
    editingDeviceKey = deviceKey || null;
    $m('#deviceModalTitle').textContent = I18N.t(deviceKey ? 'dev_edit_title' : 'dev_add_title');

    const identity = !!deviceKey && COPG.isThisDeviceKey(deviceKey);
    const nameEl = $m('#dfName');
    nameEl.readOnly = identity;                  // identity name is fixed ("This Device")
    if (deviceKey) {
      const d = COPG.config[deviceKey] || {};
      nameEl.value = identity ? I18N.t('dev_this_device') : (d.NAME || d.DEVICE || '');
      $m('#dfBrand').value = d.BRAND || '';
      $m('#dfModel').value = d.MODEL || '';
      $m('#dfProduct').value = d.PRODUCT || '';
      // DEVICE shown only when it was overridden (differs from the fingerprint's device segment);
      // blank keeps the safe auto-derive = fingerprint[2] on save (see upsertDevice).
      (function () {
        const seg = (d.FINGERPRINT || '').split('/');
        const fpDev = seg.length >= 3 ? seg[2].split(':')[0].trim() : '';
        $m('#dfDevice').value = (d.DEVICE && d.DEVICE !== fpDev) ? d.DEVICE : '';
      })();
      $m('#dfManufacturer').value = d.MANUFACTURER || '';
      $m('#dfFingerprint').value = d.FINGERPRINT || '';
      $m('#dfAndroid').value = d.ANDROID_VERSION || '';
      $m('#dfSdk').value = d.SDK_INT || '';
      $m('#dfSerial').value = d.SERIAL || '';
      let hasAdv = !!d.SERIAL;                   // Serial lives in Advanced → expand if set
      ADV_FIELDS.forEach(([id, key]) => { const v = d[key] || ''; $m('#' + id).value = v; if (v) hasAdv = true; });
      fillProps(d.PROPS);                        // custom props live in Advanced too
      if (d.PROPS && Object.keys(d.PROPS).length) hasAdv = true;
      setAdvOpen(hasAdv);                       // auto-expand if this profile uses any
    } else {
      form.reset();
      ADV_FIELDS.forEach(([id]) => { $m('#' + id).value = ''; });
      fillProps({});
      setAdvOpen(false);
    }
    openModal('deviceModal');
  }

  /* SDK is derived from + LOCKED to the Android version: typing 16 → 36,
     clearing/unknown Android → SDK clears too (prevents stale-SDK mistakes). */
  function wireAndroidSdk() {
    const a = $m('#dfAndroid'), s = $m('#dfSdk');
    if (!a || !s) return;
    // SDK auto-fills from a known Android version. An unknown/future version (not
    // in the map) or an empty field CLEARS SDK — so a stale value from a previous
    // android can't linger (e.g. 17→37 then 19→ must blank, not keep 37). SDK stays
    // editable: after the clear you can still type a future SDK by hand (sync only
    // fires on android input, so it won't wipe what you then type).
    const sync = () => {
      const v = a.value.trim();
      const sug = v ? COPG.sdkFromAndroid(v) : null;
      const next = sug != null ? String(sug) : '';
      if (next !== s.value) { s.value = next; if (next) flash(s); }
    };
    a.addEventListener('input', sync);
  }
  // Random plausible serial (uppercase alphanumeric) — typing 24 chars by hand is painful.
  function genSerial(n = 16) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let out = '';
    try {
      const buf = new Uint32Array(n);
      crypto.getRandomValues(buf);
      for (let i = 0; i < n; i++) out += chars[buf[i] % chars.length];
    } catch (_) {
      for (let i = 0; i < n; i++) out += chars[Math.floor(Math.random() * chars.length)];
    }
    return out;
  }
  function wireSerialGen() {
    const g = $m('#dfSerialGen'), s = $m('#dfSerial');
    if (!g || !s) return;
    const gen = e => { if (e) { e.preventDefault(); e.stopPropagation(); } s.value = genSerial(); flash(s); };
    g.addEventListener('click', gen);
    g.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') gen(e); });
  }
  // ANDROID_ID is exactly 16 lowercase hex chars (a 64-bit value). Used for the ↻ that
  // fills the per-app aid=<hex> literal field (the device seed field was removed).
  function genAndroidId() {
    const hex = '0123456789abcdef';
    let out = '';
    try {
      const buf = new Uint32Array(16);
      crypto.getRandomValues(buf);
      for (let i = 0; i < 16; i++) out += hex[buf[i] % 16];
    } catch (_) {
      for (let i = 0; i < 16; i++) out += hex[Math.floor(Math.random() * 16)];
    }
    return out;
  }
  // GAID is a random RFC-4122 v4 UUID (8-4-4-4-12 lowercase hex). Used for the ↻ that
  // fills the per-app gaid=<uuid> literal field.
  function genGaid() {
    const b = new Uint8Array(16);
    try { crypto.getRandomValues(b); }
    catch (_) { for (let i = 0; i < 16; i++) b[i] = Math.floor(Math.random() * 256); }
    b[6] = (b[6] & 0x0f) | 0x40;                 // version 4
    b[8] = (b[8] & 0x3f) | 0x80;                 // variant 10xx
    const h = [...b].map(x => x.toString(16).padStart(2, '0'));
    return `${h[0]}${h[1]}${h[2]}${h[3]}-${h[4]}${h[5]}-${h[6]}${h[7]}-${h[8]}${h[9]}-${h[10]}${h[11]}${h[12]}${h[13]}${h[14]}${h[15]}`;
  }
  // Widevine deviceUniqueId = 32 raw bytes. DRM-info apps DISPLAY it Base64-encoded (44 chars,
  // ends '='), so the WebUI field + ↻ generator are Base64 to match what the user sees in the app.
  // The drmid= TAG stays hex (native hex_to_bytes) — we convert at the save/load boundary below.
  function hexToB64(hex) {
    if (!hex || !/^[0-9a-fA-F]+$/.test(hex) || hex.length % 2) return '';
    let s = '';
    for (let i = 0; i < hex.length; i += 2) s += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    try { return btoa(s); } catch (_) { return ''; }
  }
  function b64ToHex(b64) {
    let s;
    try { s = atob((b64 || '').trim()); } catch (_) { return ''; }
    let h = '';
    for (let i = 0; i < s.length; i++) { const c = s.charCodeAt(i) & 0xff; h += (c < 16 ? '0' : '') + c.toString(16); }
    return h;
  }
  // Device Unique ID field FORMAT toggle. The drmid= TAG is always raw hex (native hex_to_bytes),
  // but the field can DISPLAY either Base64 (how DRM-info apps show deviceUniqueId — default) or the
  // raw hex, via the B64/HEX switch. drmIdHex tracks the current mode; conversions below normalize
  // to/from hex so the tag is format-agnostic.
  let drmIdHex = false;                                   // false = Base64 (app display), true = raw hex
  const drmIdHexToField = hex => drmIdHex ? (hex || '') : hexToB64(hex);   // hex → what the field shows
  function drmIdFieldToHex(v) {                           // what the field holds → canonical hex (for the tag)
    v = (v || '').trim();
    if (!v) return '';
    return drmIdHex ? ((/^[0-9a-fA-F]+$/.test(v) && v.length % 2 === 0) ? v.toLowerCase() : '') : b64ToHex(v);
  }
  function applyDrmIdFmt() {                              // reflect the current mode in the button + field
    const btn = $m('#pfDrmIdFmt'); if (btn) btn.textContent = drmIdHex ? 'HEX' : 'B64';
    const inp = $m('#pfDrmIdVal'); if (inp) inp.maxLength = drmIdHex ? 64 : 44;
    syncDrmVal();                                         // re-render the placeholder in the new format
  }
  function toggleDrmIdFmt() {                             // flip B64⇄HEX, converting the live value
    const inp = $m('#pfDrmIdVal'); if (!inp) return;
    const hex = drmIdFieldToHex(inp.value);               // interpret current value under the OLD mode → hex
    drmIdHex = !drmIdHex;
    inp.value = hex ? drmIdHexToField(hex) : '';          // re-render under the NEW mode
    applyDrmIdFmt();
  }
  // ↻ generate = 32 random bytes, shown in the CURRENT field format (Base64 like the app, or hex).
  function genDrmId() {
    const buf = new Uint8Array(32);
    try { crypto.getRandomValues(buf); } catch (_) { for (let i = 0; i < 32; i++) buf[i] = Math.floor(Math.random() * 256); }
    let hex = '';
    for (let i = 0; i < 32; i++) { const c = buf[i] & 0xff; hex += (c < 16 ? '0' : '') + c.toString(16); }
    return drmIdHexToField(hex);
  }
  function genDrmSys() {
    let n;
    try { const b = new Uint32Array(1); crypto.getRandomValues(b); n = b[0] % 90000 + 10000; }
    catch (_) { n = Math.floor(Math.random() * 90000) + 10000; }
    return String(n);
  }
  // IMEI = 14 random digits + Luhn check = 15, always Luhn-valid. Used for the ↻ that
  // fills the per-app imei=<15> literal field.
  function genImei() {
    let p14 = '';
    try {
      const b = new Uint8Array(14);
      crypto.getRandomValues(b);
      for (let i = 0; i < 14; i++) p14 += String(b[i] % 10);
    } catch (_) {
      for (let i = 0; i < 14; i++) p14 += String(Math.floor(Math.random() * 10));
    }
    let sum = 0, dbl = true;
    for (let i = 13; i >= 0; i--) { let d = p14.charCodeAt(i) - 48; if (dbl) { d *= 2; if (d > 9) d -= 9; } sum += d; dbl = !dbl; }
    return p14 + String((10 - (sum % 10)) % 10);
  }
  // n random decimal digits (crypto if available).
  function randDigits(n) {
    let s = '';
    try { const b = new Uint8Array(n); crypto.getRandomValues(b); for (let i = 0; i < n; i++) s += String(b[i] % 10); }
    catch (_) { for (let i = 0; i < n; i++) s += String(Math.floor(Math.random() * 10)); }
    return s;
  }
  function luhnCheck(payload) {
    let sum = 0, dbl = true;
    for (let i = payload.length - 1; i >= 0; i--) { let d = payload.charCodeAt(i) - 48; if (dbl) { d *= 2; if (d > 9) d -= 9; } sum += d; dbl = !dbl; }
    return String((10 - (sum % 10)) % 10);
  }
  // IMSI = 15 numeric digits (no check digit). ICCID = "89" + 16 digits + Luhn = 19 digits.
  function genImsi()  { return randDigits(15); }
  function genIccid() { const body = '89' + randDigits(16); return body + luhnCheck(body); }
  function wireSdkWarn() {
    const w = $m('#dfSdkWarn');
    if (!w) return;
    const open = e => { if (e) { e.preventDefault(); e.stopPropagation(); }
      info({ title: I18N.t('info_sdk_title'), message: I18N.t('info_sdk_msg') }); };
    w.addEventListener('click', open);
    w.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') open(e); });
  }
  function flash(el) {
    el.classList.add('field__input--suggested');
    setTimeout(() => el.classList.remove('field__input--suggested'), 2500);
  }

  $m('#deviceForm').addEventListener('submit', async e => {
    e.preventDefault();
    const form = e.currentTarget;
    clearErrors(form);
    const identity = COPG.isThisDeviceKey(editingDeviceKey);
    const fields = {
      name: $m('#dfName'), model: $m('#dfModel'),
      manufacturer: $m('#dfManufacturer'), fingerprint: $m('#dfFingerprint'),
    };
    // Identity profile is allowed to be fully empty (no-spoof) → skip required + dup checks.
    if (!identity) {
      let bad = false;
      for (const [, el] of Object.entries(fields)) {
        if (!el.value.trim()) { markError(el, I18N.t('msg_required')); bad = true; }
      }
      if (bad) return;
    }

    const name = fields.name.value.trim();
    const model = fields.model.value.trim();
    const dup = identity ? null : COPG.deviceNameExists(name, model, editingDeviceKey);
    if (dup && !editingDeviceKey) {
      if (dup.field === 'name')  markError(fields.name,  I18N.t('msg_dup_device'));
      if (dup.field === 'model') markError(fields.model, I18N.t('msg_dup_model'));
      return;
    }

    COPG.upsertDevice({
      name, brand: $m('#dfBrand').value, model, product: $m('#dfProduct').value, device: $m('#dfDevice').value,
      manufacturer: fields.manufacturer.value, fingerprint: fields.fingerprint.value,
      android: $m('#dfAndroid').value, sdk: $m('#dfSdk').value, serial: $m('#dfSerial').value,
      adv: advValues(), props: propsValues(),
    }, editingDeviceKey);

    closeAll();
    await w.LibRefresh(I18N.t(editingDeviceKey ? 'toast_device_saved' : 'toast_device_added'));
  });

  /* ─── Device share sheet: export (show+copy a code) / import (paste a code) ─── */
  let shareMode = 'export';   // 'export' | 'import'
  let shareKey  = null;       // device key being exported (for re-gen on toggle)

  function copyToClipboard(text) {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text); return true; }
    } catch (_) {}
    // WebView fallback: select the visible textarea + execCommand
    try { const ta = $m('#dsCode'); ta.focus(); ta.select(); return document.execCommand('copy'); }
    catch (_) { return false; }
  }

  // (re)build the export code from shareKey + the "include packages" checkbox
  function regenShareCode(copy) {
    const ta = $m('#dsCode');
    const code = COPG.exportDevice(shareKey, $m('#dsInclPkgs').checked);
    if (!code) { showToast(I18N.t('toast_dev_export_fail')); return false; }
    ta.value = code;
    if (copy) copyToClipboard(code);
    return true;
  }

  function openDeviceShare(mode, deviceKey) {
    shareMode = mode === 'import' ? 'import' : 'export';
    shareKey  = deviceKey || null;
    const ta = $m('#dsCode'), action = $m('#dsAction'), pkgRow = $m('#dsPkgRow');
    clearFieldError(ta);
    $m('#dsTitle').textContent = I18N.t(shareMode === 'import' ? 'dev_import_title' : 'dev_share_title');
    $m('#dsHint').textContent  = I18N.t(shareMode === 'import' ? 'dev_import_hint' : 'dev_export_hint');
    action.textContent = I18N.t(shareMode === 'import' ? 'act_import' : 'act_copy');
    if (shareMode === 'export') {
      // "include packages" row appears only when this device carries packages
      const n = COPG.devicePackageCount(deviceKey);
      pkgRow.hidden = n === 0;
      $m('#dsInclPkgs').checked = n > 0;                 // default: include when present
      $m('#dsPkgLabel').textContent = `${I18N.t('dev_incl_pkgs')} (${n})`;
      ta.readOnly = true;
      if (!regenShareCode(false)) return;                 // nothing shareable → toast, don't open
      openModal('deviceShareModal');
      copyToClipboard(ta.value);                          // auto-copy on open (footer re-copies)
    } else {
      pkgRow.hidden = true;
      ta.value = ''; ta.readOnly = false;
      openModal('deviceShareModal');
      setTimeout(() => ta.focus(), 120);
    }
  }
  // toggling "include packages" rebuilds + re-copies the code live
  $m('#dsInclPkgs').addEventListener('change', () => regenShareCode(true));

  $m('#dsAction').addEventListener('click', async () => {
    const ta = $m('#dsCode');
    if (shareMode === 'export') {
      showToast(I18N.t(copyToClipboard(ta.value) ? 'toast_dev_copied' : 'toast_dev_copy_fail'));
      return;
    }
    clearFieldError(ta);
    const res = COPG.importDevice(ta.value);
    if (res.needsConflict) {
      // a package may live on only one device — ask move-here vs keep-on-old
      const owners = [...new Set(res.conflicts.map(c => c.ownerName))].slice(0, 4).join(', ');
      confirm({
        title:    I18N.t('dev_conflict_title'),
        message:  I18N.t('dev_conflict_msg').replace('%n', res.conflictCount).replace('%d', owners),
        okLabel:  I18N.t('dev_conflict_move'),
        altLabel: I18N.t('dev_conflict_keep'),
        onOk:  () => finishImport(COPG.importDevice(ta.value, { conflictMode: 'move' })),
        onAlt: () => finishImport(COPG.importDevice(ta.value, { conflictMode: 'keep' })),
      });
      return;
    }
    await finishImport(res);
  });

  async function finishImport(res) {
    const ta = $m('#dsCode');
    if (!res.ok) {
      if (res.error === 'empty') { showToast(I18N.t('toast_dev_import_empty')); return; }
      markError(ta, I18N.t('msg_import_bad')); return;
    }
    closeAll();
    const parts = [];
    if (res.packageCount) parts.push(`+${res.packageCount}`);
    if (res.moved) parts.push(`${I18N.t('dev_moved')} ${res.moved}`);
    if (res.kept)  parts.push(`${I18N.t('dev_kept')} ${res.kept}`);
    const suffix = parts.length ? ` (${parts.join(', ')})` : '';
    await w.LibRefresh(`${res.name} — ${I18N.t('toast_dev_imported')}${suffix}`);
  }

  /* ════════════ PACKAGE MODAL ════════════ */
  let editingPkg = null;  // { clean, type, deviceKey } | null
  let batchItems = null, batchBaseline = null;  // bulk-edit ("Edit together") state

  // The device field is now a picker button (opens DevicePicker). The chosen
  // device's package-array key (PACKAGES_X — what listPackages tags onto
  // pkg.deviceKey) is stashed in the button's dataset; the label shows its name.
  function setDeviceSelection(pkgKey) {
    const btn  = $m('#pfDevice');
    const text = $m('#pfDeviceText');
    const devs = COPG.listDevices();
    const dev  = pkgKey ? devs.find(d => d.pkgKey === pkgKey) : null;
    if (dev) {
      btn.dataset.deviceKey = dev.pkgKey;
      text.textContent = dev.name;
      text.classList.remove('is-placeholder');
      text.removeAttribute('data-i18n');          // a real value, not the placeholder
    } else {
      delete btn.dataset.deviceKey;
      text.textContent = I18N.t(devs.length ? 'pkg_pick_device' : 'pkg_no_devices');
      text.classList.add('is-placeholder');
      text.dataset.i18n = devs.length ? 'pkg_pick_device' : 'pkg_no_devices';
    }
    clearFieldError(btn);
    syncAidAvail();                              // AID toggle PRO lock
    syncSerialLock();                            // grey per-app serial if the new profile sets SERIAL
  }
  // kept name for the existing call sites (preselect on open/edit)
  function populateDeviceSelect(selectedKey) { setDeviceSelection(selectedKey); }

  // "Spoof Android ID" is a PRO feature (no profile seed — the id derives from the package).
  // Free tier → PRO lock (grey row + chip + lock, tap → upsell); licensed → enabled.
  function syncAidAvail() {
    const row = $m('#pfAidRow'), cb = $m('#pfAid'), tag = $m('#pfAidPro');
    if (!row || !cb) return;
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    row.classList.toggle('trow--locked', locked);
    cb.disabled = false;                          // always enabled (a free-tier tap fires the upsell)
    if (locked) cb.checked = false;
    row.classList.remove('trow--disabled');
  }
  // Per-user scope of the package being edited/added ('' = all users). It is NOT chosen in
  // this modal — the picker's "Apps for" drawer sets it (or it comes from the saved entry);
  // here it's shown as a small READ-ONLY tag next to the name. This keeps the common case
  // (all users) clean and makes the rare per-user case obvious without a fiddly chooser.
  let pkgUserScope = '';
  function setPkgUserScope(scope) {
    pkgUserScope = (scope == null) ? '' : String(scope);
    const tag = $m('#pfUserTag'); if (!tag) return;
    if (pkgUserScope === '') { tag.hidden = true; tag.textContent = ''; return; }
    tag.hidden = false;
    tag.textContent = (pkgUserScope === '0')
      ? I18N.t('pkg_user_owner')
      : I18N.t('tag_user').replace('%s', pkgUserScope);
  }
  // The package name the NATIVE derive*() hashes for the current user-scope selection:
  // idPackage() appends "@u<id>" for a non-owner user (>0) so the preview matches what the
  // app will actually read; owner / all-users derive from the bare package (byte-identical).
  function previewPkg() {
    const c = COPG.clean(($m('#pfPackage').value || '').trim());
    return c ? COPG.idPackage(c, pkgUserScope) : '';
  }
  // The AID literal/preview sub-row: shown only when Spoof Android ID is on. BLANK = derive the
  // id from the package (bare 'aid' tag); a typed/generated 16-hex value pins aid=<hex>. The
  // derived id shows as the PLACEHOLDER so the user sees what a blank field will produce.
  function syncAidVal() {
    const row = $m('#pfAidValRow'); if (!row) return;
    const on = $m('#pfAid').checked && !$m('#pfAidRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveAndroidId) ? COPG.deriveAndroidId(pkg) : '';
      $m('#pfAidVal').placeholder = derived || 'a1b2c3d4e5f60718';
    }
  }
  // Per-app serial (FREE) is mutually exclusive with the device profile's own SERIAL: if the chosen
  // profile already sets a serial, this row is greyed + hard-DISABLED (not a PRO upsell — the two set
  // the same field). The native `serial` tag would still override the profile if both were set; the
  // grey-out just keeps the user from configuring the serial in two places.
  function syncSerialLock() {
    const row = $m('#pfSerialRow'), cb = $m('#pfSerial'), hint = $m('#pfSerialHint');
    if (!row || !cb) return;
    const dk  = $m('#pfDevice') ? ($m('#pfDevice').dataset.deviceKey || '') : '';
    const dev = dk ? COPG.listDevices().find(d => d.pkgKey === dk) : null;
    const deviceHasSerial = !!(dev && dev.props && dev.props.SERIAL);
    row.classList.toggle('trow--locked', deviceHasSerial);
    cb.disabled = deviceHasSerial;                 // hard disable (mutual exclusion, not a license gate)
    if (deviceHasSerial) cb.checked = false;
    if (hint) hint.hidden = !deviceHasSerial;
    syncSerialVal();
  }
  // The serial literal/preview sub-row: shown only when Custom serial is on + not greyed. BLANK =
  // derive per-app from the package (bare 'serial' tag); a typed value pins serial=<value>.
  function syncSerialVal() {
    const row = $m('#pfSerialValRow'); if (!row) return;
    const on = $m('#pfSerial').checked && !$m('#pfSerialRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
  }

  // Unified model: every package is a device package on a profile, so the profile
  // picker + both toggle groups are always shown. (with_cpu mount and block unmount
  // stay mutually exclusive — see wiring below.)
  const SPOOF_CB = ['pfWithCpu', 'pfBlock', 'pfCow', 'pfAid', 'pfSerial', 'pfGaid', 'pfAppSet', 'pfRandid', 'pfHideDev', 'pfGpu', 'pfMock', 'pfSensor', 'pfGetprop', 'pfGps', 'pfCell', 'pfUptime', 'pfBootid', 'pfMac', 'pfBtmac', 'pfRr', 'pfDrm', 'pfImei', 'pfImsi', 'pfIccid', 'pfWebua', 'pfWebfp', 'pfVpn', 'pfVpnAgg', 'pfWallpaper', 'pfAccount', 'pfPlaysrc', 'pfPairip', 'pfPlaysrcPairip', 'pfTensorfeat', 'pfSim', 'pfProxy'];
  const TWEAK_CB = ['pfDnd', 'pfDab', 'pfKso', 'pfNolog'];
  function setType() {
    $m('#pfDeviceField').style.display = '';
    $m('#pfToggles').style.display     = '';
    syncAidAvail();                             // re-evaluate the AID toggle's PRO lock
    // GPU spoof unsupported on x86_64-only → hide the toggle + clear it
    if (!gpuSupported()) { $m('#pfGpuRow').style.display = 'none'; $m('#pfGpu').checked = false; }
    if (!mockSupported()) { $m('#pfMockRow').style.display = 'none'; $m('#pfMock').checked = false; }
    if (!mockSupported()) { $m('#pfGpsRow').style.display = 'none'; $m('#pfGps').checked = false; }   // gps hook = same arm-only v7878 backend as mock
    if (!mockSupported()) { $m('#pfCellRow').style.display = 'none'; $m('#pfCell').checked = false; } // cell hook = same arm-only v7878 backend
    if (!drmSupported())  { $m('#pfDrmRow').style.display  = 'none'; $m('#pfDrm').checked  = false; }
    if (!imeiSupported()) { $m('#pfImeiRow').style.display = 'none'; $m('#pfImei').checked = false; }
    if (!imeiSupported()) { $m('#pfImsiRow').style.display = 'none'; $m('#pfImsi').checked = false; }    // same arm-only v7878 TelephonyManager backend
    if (!imeiSupported()) { $m('#pfIccidRow').style.display = 'none'; $m('#pfIccid').checked = false; }  // same arm-only v7878 TelephonyManager backend
    if (!gaidSupported()) { $m('#pfGaidRow').style.display = 'none'; $m('#pfGaid').checked = false; }
    if (!gaidSupported()) { $m('#pfAppSetRow').style.display = 'none'; $m('#pfAppSet').checked = false; }   // same arm-only backend
    // Main "Hide VPN" (:vpns, system_server) works on ALL arches → never hidden. Only the nested
    // Aggressive (:vpnn, in-app ART/native) needs the arm-only backend → hidden on x86 (syncVpn).
    if (!vpnSupported()) { $m('#pfVpnAgg').checked = false; }
    if (!rrSupported())  { $m('#pfRrRow').style.display  = 'none'; $m('#pfRr').checked  = false; }
    syncCpuPick();                              // CPU model row visibility (with_cpu)
    syncGpuPick();                              // GPU model row visibility (gpu)
    syncRrVal();                                // Hz input row visibility (rr)
    syncVpn();                                  // Aggressive sub-row visibility
    syncPlaysrc();                              // "Also block PairIP" helper sub-row visibility
  }
  function currentType() { return 'device'; }   // retained for call sites; always device now

  // Device field → open the searchable/sortable device picker sheet.
  $m('#pfDevice').addEventListener('click', () => {
    if (!w.DevicePicker) return;
    const current = $m('#pfDevice').dataset.deviceKey || null;
    DevicePicker.open(current, sel => setDeviceSelection(sel.pkgKey));
  });
  // CPU spoof is opt-in (v4.7.3): "With CPU Spoofing" mounts the fake cpuinfo,
  // "Block CPU" unmounts it (forces real cpuinfo even if a global mount leaked).
  // They're opposite actions → mutually exclusive: turning one on clears the other.
  $m('#pfWithCpu').addEventListener('change', () => { if ($m('#pfWithCpu').checked) $m('#pfBlock').checked = false; syncCpuPick(); });
  $m('#pfBlock').addEventListener('change',   () => { if ($m('#pfBlock').checked)   $m('#pfWithCpu').checked = false; syncCpuPick(); });
  // One delegated listener keeps the randid hint live as ANY spoof checkbox (incl. randid itself)
  // toggles — cheaper than wiring syncRandidHint into every id row's own change handler.
  $m('#pfToggles').addEventListener('change', e => { if (e.target && e.target.type === 'checkbox') syncRandidHint(); });

  // CPU model sub-row (revealed only when "With CPU Spoofing" is on, device type).
  // Selected key lives on #pfCpuModel.dataset.cpuKey; '' = legacy default (Snapdragon).
  function setCpuSelection(key) {
    const btn = $m('#pfCpuModel'); if (!btn) return;
    btn.dataset.cpuKey = key || '';
    $m('#pfCpuModelText').textContent = (key && COPG.cpuModelName(key)) || I18N.t('pkg_cpu_default');
  }
  function syncCpuPick() {
    const row = $m('#pfCpuRow'); if (!row) return;
    row.style.display = $m('#pfWithCpu').checked ? '' : 'none';   // CPU model row only when CPU spoof on
  }
  $m('#pfCpuModel').addEventListener('click', () => {
    if (!w.CpuPicker) return;
    CpuPicker.open($m('#pfCpuModel').dataset.cpuKey || '', sel => setCpuSelection(sel.key));
  });
  // Timezone (tz=<IANA zone>) — FREE, stealth. Selected zone lives on #pfTzPick.dataset.tz
  // ('' = off/real). Opens the TzPicker sheet (searchable built-ins + add-custom), like CPU/GPU.
  function setTzSelection(zone) {
    const btn = $m('#pfTzPick'); if (!btn) return;
    btn.dataset.tz = zone || '';
    $m('#pfTzText').textContent = (zone && COPG.tzName ? COPG.tzName(zone) : zone) || I18N.t('pkg_tz_off');
  }
  $m('#pfTzPick').addEventListener('click', () => {
    if (!w.TzPicker) return;
    TzPicker.open($m('#pfTzPick').dataset.tz || '', sel => setTzSelection(sel.key));
  });
  // Language (lang=<BCP-47>) — FREE, stealth, applied system-side. Selected locale lives on
  // #pfLangPick.dataset.lang ('' = off/real). Opens the LangPicker sheet, like tz.
  function setLangSelection(loc) {
    const btn = $m('#pfLangPick'); if (!btn) return;
    btn.dataset.lang = loc || '';
    $m('#pfLangText').textContent = (loc && COPG.langName ? COPG.langName(loc) : loc) || I18N.t('pkg_lang_off');
  }
  $m('#pfLangPick').addEventListener('click', () => {
    if (!w.LangPicker) return;
    if (!$m('#pfLangPick').dataset.lang && ssBlocked(null)) return;   // master off + not yet set → guide (allow clearing an existing one)
    LangPicker.open($m('#pfLangPick').dataset.lang || '', sel => setLangSelection(sel.key));
  });
  // ↻ generate a fresh random 16-hex ANDROID_ID into the literal field.
  $m('#pfAidGen').addEventListener('click', () => { $m('#pfAidVal').value = genAndroidId(); });
  // Per-app serial (FREE): toggle reveals the literal sub-row; ↻ fills a random serial-shaped value.
  $m('#pfSerial').addEventListener('change', syncSerialVal);
  $m('#pfSerialGen').addEventListener('click', () => { $m('#pfSerialVal').value = genSerial(); });

  // Canvas-fp seed: Randomize → a new random hex seed (re-roll the fingerprint); blank = derive from package.
  if ($m('#pfWebfpGen')) $m('#pfWebfpGen').addEventListener('click', () => {
    $m('#pfWebfpVal').value = (Math.floor(Math.random() * 0xffffffff) >>> 0).toString(16).padStart(8, '0');
  });

  // GPU spoof (reference): RESIDENT inline hooks = detectable, so enabling it is
  // gated behind an explicit "use at own risk" confirm. We require acceptance before
  // the toggle flips on (uncheck-then-confirm), so dismissing leaves it OFF — no
  // onCancel needed. Selected key on #pfGpuModel.dataset.gpuKey; default adreno830.
  function setGpuSelection(key) {
    const btn = $m('#pfGpuModel'); if (!btn) return;
    btn.dataset.gpuKey = key || '';
    $m('#pfGpuModelText').textContent = (key && COPG.gpuModelName(key)) || I18N.t('pkg_gpu_default');
  }
  function syncGpuPick() {
    const row = $m('#pfGpuModelRow'); if (!row) return;
    row.style.display = $m('#pfGpu').checked ? '' : 'none';
  }
  // The nested "Aggressive (in-app)" sub-row shows when the main Hide VPN is on (or the aggressive
  // toggle itself is already on — preserve a legacy :vpn/:vpnn that had no :vpns). arm-only backend.
  function syncVpn() {
    const row = $m('#pfVpnAggRow'); if (!row) return;
    const show = (typeof vpnSupported !== 'function' || vpnSupported()) &&
                 ($m('#pfVpn').checked || $m('#pfVpnAgg').checked);
    row.style.display = show ? '' : 'none';
  }
  // The "Also block PairIP check" HELPER sub-row under playsrc — a convenience for users who don't know
  // they also need the PairIP block; it mirrors the independent #pfPairip row (same :pairip tag). Shown
  // only when playsrc is on. #pfPairip stays the source of truth (save reads it).
  function syncPlaysrc() {
    const row = $m('#pfPlaysrcPairipRow'); if (!row) return;
    row.style.display = $m('#pfPlaysrc').checked ? '' : 'none';
    const h = $m('#pfPlaysrcPairip'); if (h) h.checked = $m('#pfPairip').checked;   // mirror current state
  }
  // GPU spoof AND per-app Android ID are PRO-only features. If a licensing layer is
  // present and the license isn't VALID/GRACE, they stay locked (native gates them too;
  // this makes the lock visible in the UI instead of a tag that silently does nothing).
  // No licensing layer (window.License absent) → not gated.
  function proLicensed() {
    if (!w.License) return true;
    const s = (w._licState && w._licState.state) || 'FREE';
    return s === 'VALID' || s === 'GRACE';
  }
  // Free-tier upsell for a PRO-only control (used to gate the GPU / SIM profile pickers, which
  // otherwise let free users open, browse, add and edit PRO profiles even though native re-gates).
  function proUpsell(titleKey, msgKey) {
    confirm({
      title: I18N.t(titleKey),
      message: I18N.t(msgKey),
      okLabel: I18N.t('gpu_pro_ok'),
      onOk: () => { closeAll(); if (w.License) License.reveal(); },
    });
  }
  function syncGpuLock() {
    const tag = $m('#pfGpuPro'), row = $m('#pfGpuRow');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;              // show "PRO" chip only on free tier
    if (row) row.classList.toggle('trow--locked', locked);   // grey the row when free
  }
  // COW prop spoof is a PRO feature too (stealth, but gated). Same lock pattern as GPU/AID:
  // grey the row + PRO chip + lock on free tier (native gates it in pre too). Checkbox stays
  // enabled so a tap fires the upsell.
  function syncCowLock() {
    const tag = $m('#pfCowPro'), row = $m('#pfCowRow'), cb = $m('#pfCow');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function syncRandidLock() {
    const tag = $m('#pfRandidPro'), row = $m('#pfRandidRow'), cb = $m('#pfRandid');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // randid is a pure MODIFIER — it only rotates the derived IDs whose own spoof is enabled
  // (android_id/serial/gaid/appset/drm/imei/imsi/iccid/wallpaper/boot_id). On its own it's a no-op, so
  // when it's on we show an inline hint describing what it rotates, flagged amber when none are on
  // yet (the "dead toggle" case). Not a dependency-gate — the user can still turn it on freely.
  const RANDID_IDS = ['pfAid', 'pfSerial', 'pfGaid', 'pfAppSet', 'pfDrm', 'pfImei', 'pfImsi', 'pfIccid', 'pfWallpaper', 'pfBootid', 'pfMac', 'pfBtmac'];
  function syncRandidHint() {
    const hint = $m('#pfRandidHint'), cb = $m('#pfRandid');
    if (!hint || !cb) return;
    if (!cb.checked) { hint.hidden = true; return; }
    const anyOn = RANDID_IDS.some(id => { const el = $m('#' + id); return el && el.checked; });
    hint.hidden = false;
    hint.classList.toggle('trow-hint--none', !anyOn);
    const base = I18N.t('pkg_randid_hint');
    hint.textContent = anyOn ? base : base + ' ' + I18N.t('pkg_randid_hint_none');
  }
  // Fake Play install source (:playsrc) + Block PairIP (:pairip) are INDEPENDENT PRO rows — same lock
  // pattern each (grey row + PRO chip on free tier; native re-gates in preServerSpecialize).
  // ── System-server master gate (Settings "System-Server Spoofs", global.json sysserver) ──
  // Hide VPN / Play install-source / Block PairIP / per-app Language run INSIDE system_server, so
  // they only arm when the master is ON — and only after a reboot. When the master is OFF we grey
  // these rows and block enabling with a "turn it on + reboot first" heads-up, so a user never flips
  // one and wonders why nothing changed. Each also shows a persistent ⟳ marker (enabling → reboot).
  const SS_ROWS = ['pfVpnRow', 'pfPlaysrcRow', 'pfPairipRow', 'pfTensorfeatRow', 'pfLangRow', 'pfWallpaperRow', 'pfAccountRow'];
  function sysServerOn() { try { return !!(w.COPG && COPG.getGlobalConfig && COPG.getGlobalConfig().sysserver); } catch (_) { return false; } }
  // ARMED = master on AND a real reboot has happened since it was toggled. Grey these rows until the
  // layer is actually armed — not the moment the master intent flips on (it only installs at boot),
  // matching the Device tab. sysserverArmed() lives in copg-data.js.
  function sysServerArmed() { try { return !!(w.COPG && COPG.sysserverArmed && COPG.sysserverArmed()); } catch (_) { return sysServerOn(); } }
  function syncSysServerGate() {
    const armed = sysServerArmed();
    SS_ROWS.forEach(id => {
      const row = $m('#' + id); if (!row) return;
      row.classList.toggle('trow--ssoff', !armed);
      if (!row.querySelector('.trow__reboot')) {
        const mark = document.createElement('span');
        mark.className = 'trow__reboot';
        mark.textContent = '⟳';
        try { mark.title = I18N.t('sysserver_reboot_mark'); } catch (_) {}
        const lbl = row.querySelector('.trow__label');
        if (lbl) lbl.appendChild(mark);
      }
    });
  }
  // Turning a system_server spoof ON before the master is armed → revert + heads-up. Master OFF →
  // "turn it on + reboot"; master ON but not yet rebooted → "reboot to apply". Returns true if it
  // blocked (caller should stop). cb null = a non-checkbox control (the language picker).
  function ssBlocked(cb) {
    if (sysServerArmed()) return false;
    if (cb) cb.checked = false;
    const pending = sysServerOn();   // master on, awaiting the reboot that arms it
    confirm({ title:   I18N.t(pending ? 'sysserver_pending_title' : 'sysserver_needed_title'),
              message: I18N.t(pending ? 'sysserver_pending_msg'   : 'sysserver_needed_msg'),
              okLabel: I18N.t('sysserver_warn_ok'), infoOnly: true });
    return true;
  }

  function syncPlaysrcLock() {
    const tag = $m('#pfPlaysrcPro'), row = $m('#pfPlaysrcRow'), cb = $m('#pfPlaysrc');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function syncPairipLock() {
    const tag = $m('#pfPairipPro'), row = $m('#pfPairipRow'), cb = $m('#pfPairip');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function syncTensorfeatLock() {
    const tag = $m('#pfTensorfeatPro'), row = $m('#pfTensorfeatRow'), cb = $m('#pfTensorfeat');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // Wallpaper-id + Google-account spoof: PRO (system-side, license-gated in preServerSpecialize) AND
  // stealth (out-of-process → gray info, no risk gate). Same PRO chip/lock pattern as tensorfeat.
  function syncWallpaperLock() {
    const tag = $m('#pfWallpaperPro'), row = $m('#pfWallpaperRow'), cb = $m('#pfWallpaper');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function syncAccountLock() {
    const tag = $m('#pfAccountPro'), row = $m('#pfAccountRow'), cb = $m('#pfAccount');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // Mock-location hide is PRO too, AND resident/detectable like GPU → same lock pattern
  // (grey row + PRO chip on free tier; native re-gates in post). Checkbox stays enabled so
  // a free-tier tap fires the upsell path in the change handler below.
  function syncMockLock() {
    const tag = $m('#pfMockPro'), row = $m('#pfMockRow'), cb = $m('#pfMock');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // Sensor-roster spoof: PRO + resident (v7878, same as mock) → same lock pattern.
  function syncSensorLock() {
    const tag = $m('#pfSensorPro'), row = $m('#pfSensorRow'), cb = $m('#pfSensor');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // getprop consistency: PRO + resident/detectable (libc execve inline hook) → same lock pattern.
  function syncGetpropLock() {
    const tag = $m('#pfGetpropPro'), row = $m('#pfGetpropRow'), cb = $m('#pfGetprop');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // GPS spoof: PRO + resident/detectable (v7878, same as mock) → same lock pattern.
  function syncGpsLock() {
    const tag = $m('#pfGpsPro'), row = $m('#pfGpsRow'), cb = $m('#pfGps');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncGpsPick(); }
  }
  // Reveal the place-picker sub-row only when GPS spoof is on.
  function syncGpsPick() {
    const row = $m('#pfGpsLocRow'); if (!row) return;
    row.style.display = $m('#pfGps').checked ? '' : 'none';
  }
  // Cell-tower spoof: PRO + resident/detectable (v7878, same as GPS) → same lock pattern.
  function syncCellLock() {
    const tag = $m('#pfCellPro'), row = $m('#pfCellRow'), cb = $m('#pfCell');
    if (!row) return;
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncCellInputs(); }
  }
  // Reveal the LAC/CI input sub-rows only when Cell-tower spoof is on.
  function syncCellInputs() {
    const on = $m('#pfCell').checked;
    const r1 = $m('#pfCellLacRow'), r2 = $m('#pfCellCiRow'), r3 = $m('#pfCellMccRow'), r4 = $m('#pfCellMncRow');
    if (r1) r1.style.display = on ? '' : 'none';
    if (r2) r2.style.display = on ? '' : 'none';
    if (r3) r3.style.display = on ? '' : 'none';
    if (r4) r4.style.display = on ? '' : 'none';
  }
  // GPS place selection — lat/lng on #pfGpsPick.dataset (the gps=<lat>,<lng> tag), text = the nearest
  // known place name (COPG.nearestPlace) or the raw coords; empty = "Choose a place…".
  function setGpsSelection(lat, lng) {
    const btn = $m('#pfGpsPick'); if (!btn) return;
    const la = String(lat || '').trim(), lo = String(lng || '').trim();
    btn.dataset.gpsLat = la;
    btn.dataset.gpsLng = lo;
    let text = I18N.t('pkg_gps_choose');
    if (la !== '' && lo !== '') {
      const near = (COPG.nearestPlace && COPG.nearestPlace(la, lo)) || null;
      text = near ? (near.country ? near.name + ', ' + near.country : near.name) : (la + ', ' + lo);
    }
    $m('#pfGpsPickText').textContent = text;
  }
  // Uptime spoof: PRO + resident/detectable like Mock/GPU → same lock pattern.
  function syncUptimeLock() {
    const tag = $m('#pfUptimePro'), row = $m('#pfUptimeRow'), cb = $m('#pfUptime');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncUptimeVal(); }
  }
  // Reveal the days input only when Fake Uptime is on.
  function syncUptimeVal() {
    const row = $m('#pfUptimeValRow'); if (!row) return;
    row.style.display = $m('#pfUptime').checked ? '' : 'none';
  }
  // boot_id spoof is RESIDENT: a per-app inline open/openat hook (every VFS backend — NoMount
  // per-uid/global, mount --bind — crashed apps, so inline is the only backend). It stays mapped →
  // memory-scan DETECTABLE → always the warn marker + red risk gate, like Mock / GPU / DRM.
  function applyBootidMarker() {
    const el = $m('#pfBootidInfo'); if (!el) return;
    el.classList.add('trow__info--warn');
    el.innerHTML = WARN_SVG;
  }
  // boot_id: PRO + resident → PRO lock like Mock/GPU.
  function syncBootidLock() {
    const tag = $m('#pfBootidPro'), row = $m('#pfBootidRow'), cb = $m('#pfBootid');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncBootidVal(); }
  }
  // Reveal the churn switch when boot_id is on; the pinned-UUID field only when churn is OFF (a
  // "new each launch" id can't also be a fixed value). Blank field = derive from the app (shown as
  // the placeholder so the user sees what a blank field will produce).
  function syncBootidVal() {
    const on = $m('#pfBootid').checked && !$m('#pfBootidRow').classList.contains('trow--locked');
    const churn = $m('#pfBootidChurn').checked;
    const cr = $m('#pfBootidChurnRow'), vr = $m('#pfBootidValRow');
    if (cr) cr.style.display = on ? '' : 'none';
    if (vr) vr.style.display = (on && !churn) ? '' : 'none';
    if (on && !churn) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveBootId) ? COPG.deriveBootId(pkg) : '';
      $m('#pfBootidVal').placeholder = derived || '38400000-8cf0-11bd-b23e-10b96e40000d';
    }
  }
  // WiFi MAC spoof is RESIDENT: a per-app inline open/openat redirect of /sys/class/net/wlanN/address.
  // It stays mapped → memory-scan DETECTABLE → always the warn marker + red risk gate, like boot_id.
  function applyMacMarker() {
    const el = $m('#pfMacInfo'); if (!el) return;
    el.classList.add('trow__info--warn');
    el.innerHTML = WARN_SVG;
  }
  // MAC: PRO + resident → PRO lock like boot_id/Mock/GPU.
  function syncMacLock() {
    const tag = $m('#pfMacPro'), row = $m('#pfMacRow'), cb = $m('#pfMac');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncMacVal(); }
  }
  // Reveal the churn switch when MAC is on; the pinned-MAC field only when churn is OFF. Blank field
  // = derive from the app (shown as the placeholder). Preview is the exact MAC the app will read.
  function syncMacVal() {
    const on = $m('#pfMac').checked && !$m('#pfMacRow').classList.contains('trow--locked');
    const churn = $m('#pfMacChurn').checked;
    const cr = $m('#pfMacChurnRow'), vr = $m('#pfMacValRow');
    if (cr) cr.style.display = on ? '' : 'none';
    if (vr) vr.style.display = (on && !churn) ? '' : 'none';
    if (on && !churn) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveMac) ? COPG.deriveMac(pkg) : '';
      $m('#pfMacVal').placeholder = derived || '02:1a:11:ff:2c:9b';
    }
  }
  // Bluetooth MAC (STEALTH, PRO): same PRO-lock + churn/pinned reveal as WiFi MAC, no risk gate.
  function syncBtmacLock() {
    const tag = $m('#pfBtmacPro'), row = $m('#pfBtmacRow'), cb = $m('#pfBtmac');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncBtmacVal(); }
  }
  function syncBtmacVal() {
    const on = $m('#pfBtmac').checked && !$m('#pfBtmacRow').classList.contains('trow--locked');
    const churn = $m('#pfBtmacChurn').checked;
    const cr = $m('#pfBtmacChurnRow'), vr = $m('#pfBtmacValRow');
    if (cr) cr.style.display = on ? '' : 'none';
    if (vr) vr.style.display = (on && !churn) ? '' : 'none';
    if (on && !churn) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveBtmac) ? COPG.deriveBtmac(pkg) : '';
      $m('#pfBtmacVal').placeholder = derived || 'FF:DD:1D:22:60:44';
    }
  }
  // Reveal the Hz input only when Spoof Refresh Rate is on.
  function syncRrVal() {
    const row = $m('#pfRrValRow'); if (!row) return;
    row.style.display = $m('#pfRr').checked ? '' : 'none';
  }
  // Refresh-rate hook is arm-only (rr_hook install() stubs on x86/x86_64). Same rule as gpu/vpn.
  function rrSupported() { return true; }   // shown on every ABI (v7878 + inline hooks cover arm64/arm32/x86_64)
  // DRM (Widevine securityLevel) spoof is PRO + resident/detectable like GPU/mock → same
  // lock pattern (grey row + PRO chip on free tier; native re-gates in post).
  function syncDrmLock() {
    const tag = $m('#pfDrmPro'), row = $m('#pfDrmRow'), cb = $m('#pfDrm');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
    syncDrmVal();
  }
  // WebView User-Agent spoof: PRO + resident/detectable → same lock pattern as DRM/Mock.
  function syncWebuaLock() {
    const tag = $m('#pfWebuaPro'), row = $m('#pfWebuaRow'), cb = $m('#pfWebua');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) { cb.checked = false; syncWebuaVal(); }
  }
  // Canvas fingerprint spoof: PRO + resident (v7878 JS-shim) → same lock pattern as mock/webua.
  function syncWebfpLock() {
    const tag = $m('#pfWebfpPro'), row = $m('#pfWebfpRow'), cb = $m('#pfWebfp');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
    syncWebfpVal();
  }
  // Reveal the seed field (Randomize / pin a value) only when Canvas spoof is on + unlocked.
  function syncWebfpVal() {
    const row = $m('#pfWebfpValRow'); if (!row) return;
    row.style.display = ($m('#pfWebfp').checked && !$m('#pfWebfpRow').classList.contains('trow--locked')) ? '' : 'none';
  }
  // Reveal the UA text field only when WebView User-Agent spoof is on + unlocked.
  function syncWebuaVal() {
    const row = $m('#pfWebuaValRow'); if (!row) return;
    row.style.display = ($m('#pfWebua').checked && !$m('#pfWebuaRow').classList.contains('trow--locked')) ? '' : 'none';
  }
  // UA profile picker selection — key on #pfWebuaPick.dataset.uaKey (same pattern as GPU model).
  function setWebuaSelection(key) {
    const btn = $m('#pfWebuaPick'); if (!btn) return;
    btn.dataset.uaKey = key || '';
    $m('#pfWebuaPickText').textContent = (key && COPG.uaProfileName(key)) || I18N.t('pkg_webua_off');
  }
  // Widevine security level — a segmented control (same pattern as the SIM Mode row) rather than
  // a native <select>, whose WebView dropdown is the one control that doesn't follow the app's
  // visual language. Three short values also read better laid out than hidden behind a menu.
  function drmLevel() { return $m('#pfDrmLevel').dataset.level || 'L1'; }
  function setDrmLevel(lvl) {
    const seg = $m('#pfDrmLevel'); if (!seg) return;
    lvl = (['L1', 'L2', 'L3'].indexOf(String(lvl || '').toUpperCase()) >= 0)
      ? String(lvl).toUpperCase() : 'L1';
    seg.dataset.level = lvl;
    seg.querySelectorAll('.seg__opt').forEach(b =>
      b.classList.toggle('active', b.dataset.level === lvl));
  }
  // The DRM literal/preview sub-rows (deviceUniqueId + systemId): shown only when DRM spoof
  // is on + unlocked. BLANK = derive per-app from the package; a typed/generated value pins
  // drmid=<hex> / drmsys=<int>. Derived values show as PLACEHOLDERS so the user sees what a
  // blank field produces (byte-identical to the native derive via COPG.deriveDrm*).
  function syncDrmVal() {
    const idRow = $m('#pfDrmIdRow'), sysRow = $m('#pfDrmSysRow'), lvlRow = $m('#pfDrmLevelRow');
    if (!idRow || !sysRow || !lvlRow) return;
    const on = $m('#pfDrm').checked && !$m('#pfDrmRow').classList.contains('trow--locked');
    lvlRow.style.display = on ? '' : 'none';
    idRow.style.display = on ? '' : 'none';
    sysRow.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      $m('#pfDrmIdVal').placeholder  = (pkg && COPG.deriveDrmDeviceId) ? drmIdHexToField(COPG.deriveDrmDeviceId(pkg)) : 'auto (per-app)';
      $m('#pfDrmSysVal').placeholder = (pkg && COPG.deriveDrmSystemId) ? COPG.deriveDrmSystemId(pkg) : 'auto (per-app)';
    }
  }
  // IMEI spoof is PRO + resident/detectable → same lock pattern as GPU/mock/drm.
  function syncImeiLock() {
    const tag = $m('#pfImeiPro'), row = $m('#pfImeiRow'), cb = $m('#pfImei');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
    syncImeiVal();
  }
  // IMEI backend is arm-only (LSPlant). Hide the toggle only when the ABI is KNOWN + no arm.
  function imeiSupported() { return true; }   // shown on every ABI (v7878 TelephonyManager hook covers arm64/arm32/x86_64)
  // The IMEI literal/preview sub-row: shown only when IMEI spoof is on + unlocked. BLANK =
  // derive per-app; a typed/generated 15-digit value pins imei=<15>. The derived IMEI shows
  // as the placeholder (byte-identical to the native derive via COPG.deriveImei).
  function syncImeiVal() {
    const row = $m('#pfImeiValRow'), row2 = $m('#pfImei2ValRow'); if (!row) return;
    const on = $m('#pfImei').checked && !$m('#pfImeiRow').classList.contains('trow--locked');
    row.style.display  = on ? '' : 'none';
    if (row2) row2.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      $m('#pfImeiVal').placeholder  = (pkg && COPG.deriveImei) ? COPG.deriveImei(pkg, 0) : 'auto (per-app)';
      $m('#pfImei2Val').placeholder = (pkg && COPG.deriveImei) ? COPG.deriveImei(pkg, 1) : 'auto (per-app)';
    }
  }
  // IMSI / ICCID spoof — PRO + resident (same v7878 TelephonyManager hook as IMEI). Same lock +
  // literal/preview pattern; single value each (blank = derive per-app, byte-identical to native).
  function syncImsiLock() {
    const tag = $m('#pfImsiPro'), row = $m('#pfImsiRow'), cb = $m('#pfImsi');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
    syncImsiVal();
  }
  function syncImsiVal() {
    const row = $m('#pfImsiValRow'), row2 = $m('#pfImsi2ValRow'); if (!row) return;
    const on = $m('#pfImsi').checked && !$m('#pfImsiRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
    if (row2) row2.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      $m('#pfImsiVal').placeholder  = (pkg && COPG.deriveImsi) ? COPG.deriveImsi(pkg, 0) : 'auto (per-app)';
      $m('#pfImsi2Val').placeholder = (pkg && COPG.deriveImsi) ? COPG.deriveImsi(pkg, 1) : 'auto (per-app)';
    }
  }
  function syncIccidLock() {
    const tag = $m('#pfIccidPro'), row = $m('#pfIccidRow'), cb = $m('#pfIccid');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
    syncIccidVal();
  }
  function syncIccidVal() {
    const row = $m('#pfIccidValRow'), row2 = $m('#pfIccid2ValRow'); if (!row) return;
    const on = $m('#pfIccid').checked && !$m('#pfIccidRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
    if (row2) row2.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      $m('#pfIccidVal').placeholder  = (pkg && COPG.deriveIccid) ? COPG.deriveIccid(pkg, 0) : 'auto (per-app)';
      $m('#pfIccid2Val').placeholder = (pkg && COPG.deriveIccid) ? COPG.deriveIccid(pkg, 1) : 'auto (per-app)';
    }
  }
  // DRM backend is arm-only (LSPlant + And64/ele7enxxh; x86/x86_64 stub). Same rule as
  // mockSupported: hide the toggle only when the ABI is KNOWN and has no arm.
  function drmSupported() { return true; }   // shown on every ABI (v7878 MediaDrm hook covers arm64/arm32/x86_64)
  // GPU spoof backend is arm-only (And64 arm64 + ele7enxxh arm32; x86/x86_64 = no-op
  // stub). Hide the whole GPU toggle on an x86_64-only device. Only hide when we KNOW
  // the ABI and it has no arm — unknown/preview ('—') keeps it visible.
  function gpuSupported() { return true; }   // shown on every ABI (inline GL/Vulkan hook covers arm64/arm32/x86_64)
  // Mock-location hide backend is arm-only (LSPlant + And64 on arm64, ele7enxxh on arm32;
  // x86/x86_64 = no inline backend → native stubs). Same rule as gpuSupported: hide the
  // toggle only when we KNOW the ABI and it has no arm. Unknown/preview ('—') keeps it visible.
  function mockSupported() { return true; }   // shown on every ABI (v7878 Location hook covers arm64/arm32/x86_64)
  // VPN hide backend is arm-only (in-app LSPlant ART layer + native getifaddrs inline on
  // arm64/arm32; x86/x86_64 = vpn_hook install() stub → returns false). Same rule as
  // gpu/mockSupported: hide only when the ABI is KNOWN and has no arm; '—'/preview keeps it.
  function vpnSupported() { return true; }   // shown on every ABI (inline getifaddrs + ART layer cover arm64/arm32/x86_64)
  $m('#pfGpu').addEventListener('change', () => {
    if ($m('#pfGpu').checked) {
      $m('#pfGpu').checked = false;                       // require explicit risk acceptance
      if (!proLicensed()) {                               // PRO-only → block on free tier
        confirm({
          title: I18N.t('gpu_pro_title'),
          message: I18N.t('gpu_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('gpu', {
        title: I18N.t('gpu_risk_title'),
        message: I18N.t('gpu_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,                            // 5s cooldown on OK → forces a read
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => {
          $m('#pfGpu').checked = true;
          if (!$m('#pfGpuModel').dataset.gpuKey) setGpuSelection('adreno830');
          syncGpuPick();
        },
      });
    } else {
      syncGpuPick();
    }
  });
  // Refresh-rate spoof: RESIDENT v7878 hook = detectable → red-warn gate like GPU, but FREE
  // (no PRO lock). Uncheck-then-confirm so dismissing leaves it OFF. Number input (Hz).
  $m('#pfRr').addEventListener('change', () => {
    if ($m('#pfRr').checked) {
      $m('#pfRr').checked = false;                       // require explicit risk acceptance
      riskConfirm('rr', {
        title: I18N.t('rr_risk_title'),
        message: I18N.t('rr_risk_msg'),
        okLabel: I18N.t('rr_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => {
          $m('#pfRr').checked = true;
          if (!$m('#pfRrHz').value) $m('#pfRrHz').value = 120;
          syncRrVal();
        },
      });
    } else {
      syncRrVal();
    }
  });
  // Mock-location hide: RESIDENT LSPlant hook = detectable, so — like GPU — enabling it is
  // gated behind an explicit "use at own risk" confirm (uncheck-then-confirm, so dismissing
  // leaves it OFF). PRO-only: free tier gets the upsell instead. No model sub-row (boolean).
  $m('#pfMock').addEventListener('change', () => {
    if ($m('#pfMock').checked) {
      $m('#pfMock').checked = false;                       // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('mock_pro_title'),
          message: I18N.t('mock_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('mock', {
        title: I18N.t('mock_risk_title'),
        message: I18N.t('mock_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,                            // 5s cooldown on OK → forces a read
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfMock').checked = true; },
      });
    }
  });
  // Sensor-roster spoof: RESIDENT v7878 hook = detectable → same PRO-block + red risk gate as Mock. Boolean.
  $m('#pfSensor').addEventListener('change', () => {
    if ($m('#pfSensor').checked) {
      $m('#pfSensor').checked = false;
      if (!proLicensed()) {
        confirm({ title: I18N.t('sensor_pro_title'), message: I18N.t('sensor_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'), onOk: () => { closeAll(); if (w.License) License.reveal(); } });
        return;
      }
      riskConfirm('sensor', {
        title: I18N.t('sensor_risk_title'), message: I18N.t('sensor_risk_msg'), okLabel: I18N.t('gpu_risk_ok'),
        danger: true, countdown: 5, link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfSensor').checked = true; },
      });
    }
  });
  // getprop consistency: RESIDENT libc execve inline hook = detectable → same PRO-block + red risk gate as Mock. Boolean.
  $m('#pfGetprop').addEventListener('change', () => {
    if ($m('#pfGetprop').checked) {
      $m('#pfGetprop').checked = false;
      if (!proLicensed()) {
        confirm({ title: I18N.t('getprop_pro_title'), message: I18N.t('getprop_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'), onOk: () => { closeAll(); if (w.License) License.reveal(); } });
        return;
      }
      riskConfirm('getprop', {
        title: I18N.t('getprop_risk_title'), message: I18N.t('getprop_risk_msg'), okLabel: I18N.t('gpu_risk_ok'),
        danger: true, countdown: 5, link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfGetprop').checked = true; },
      });
    }
  });
  // GPS Location spoof: PRO + resident/detectable (v7878) → same PRO-block + red risk gate as Mock.
  // On accept, reveal the place-picker sub-row and default the picker open if no place is chosen yet.
  $m('#pfGps').addEventListener('change', () => {
    if ($m('#pfGps').checked) {
      $m('#pfGps').checked = false;                        // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('gps_pro_title'),
          message: I18N.t('gps_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('gps', {
        title: I18N.t('gps_risk_title'),
        message: I18N.t('gps_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,                            // 5s cooldown on OK → forces a read
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => {
          $m('#pfGps').checked = true;
          syncGpsPick();
          if (!$m('#pfGpsPick').dataset.gpsLat && w.GpsPicker)   // no place yet → open the picker
            GpsPicker.open('', '', sel => setGpsSelection(sel.lat, sel.lng));
        },
      });
    } else {
      syncGpsPick();
    }
  });

  $m('#pfCell').addEventListener('change', () => {
    if ($m('#pfCell').checked) {
      $m('#pfCell').checked = false;                       // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('cell_pro_title'),
          message: I18N.t('cell_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('cell', {
        title: I18N.t('cell_risk_title'),
        message: I18N.t('cell_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfCell').checked = true; syncCellInputs(); },
      });
    } else {
      syncCellInputs();
    }
  });
  // Fake Uptime: PRO + resident/detectable → same PRO-block + red risk gate as Mock.
  $m('#pfUptime').addEventListener('change', () => {
    if ($m('#pfUptime').checked) {
      $m('#pfUptime').checked = false;                     // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('uptime_pro_title'),
          message: I18N.t('uptime_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('uptime', {
        title: I18N.t('uptime_risk_title'),
        message: I18N.t('uptime_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfUptime').checked = true; syncUptimeVal(); },
      });
    } else { syncUptimeVal(); }
  });
  // boot_id ⓘ — preview the exact id the app reads. Churn (its own switch OR the Randomize-IDs
  // switch, which covers boot_id too) → a fresh id each launch, so no fixed preview. Always resident.
  function openBootidInfo(e) {
    e.preventDefault(); e.stopPropagation();
    const pkg = previewPkg();
    const lit = ($m('#pfBootidVal') ? $m('#pfBootidVal').value.trim().toLowerCase() : '');
    const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
    const churn = $m('#pfBootidChurn').checked || $m('#pfRandid').checked;
    let v;
    if (churn)               v = I18N.t('info_pkgbootid_churn');
    else if (UUID.test(lit)) v = I18N.t('info_pkgbootid_value') + ' ' + lit;
    else if (!pkg)           v = I18N.t('info_pkgbootid_nopkg');
    else                     v = I18N.t('info_pkgbootid_value') + ' ' + COPG.deriveBootId(pkg);
    info({ title: I18N.t('info_pkgbootid_title'),
           message: I18N.t('info_pkgbootid_msg') + ' ' + v + ' ' + I18N.t('info_pkgbootid_resident') });
  }
  { const el = $m('#pfBootidInfo');
    if (el) {
      el.addEventListener('click', openBootidInfo);
      el.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openBootidInfo(e); });
    } }
  // ↻ fill the field with a fresh random v4 UUID (reuses the GAID generator).
  $m('#pfBootidGen').addEventListener('click', () => { $m('#pfBootidVal').value = genGaid(); });
  // Churn switch just re-syncs (hides the pinned-UUID field when on).
  $m('#pfBootidChurn').addEventListener('change', syncBootidVal);
  // Spoof Boot ID: PRO + RESIDENT (inline hook, only backend) → always the red 5s risk gate.
  $m('#pfBootid').addEventListener('change', () => {
    if ($m('#pfBootid').checked) {
      $m('#pfBootid').checked = false;                     // require explicit acceptance
      if (!proLicensed()) { proUpsell('bootid_pro_title', 'bootid_pro_msg'); return; }  // PRO-only
      riskConfirm('bootid', {
        title: I18N.t('bootid_risk_title'),
        message: I18N.t('bootid_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfBootid').checked = true; syncBootidVal(); },
      });
    } else { syncBootidVal(); }
  });
  function openMacInfo(e) {
    e.preventDefault(); e.stopPropagation();
    const pkg = previewPkg();
    const lit = ($m('#pfMacVal') ? $m('#pfMacVal').value.trim().toLowerCase().replace(/[^0-9a-f]/g, '') : '');
    const churn = $m('#pfMacChurn').checked || $m('#pfRandid').checked;
    let v;
    if (churn)                           v = I18N.t('info_pkgmac_churn');
    else if (/^[0-9a-f]{12}$/.test(lit)) v = I18N.t('info_pkgmac_value') + ' ' + lit.replace(/(..)(?=.)/g, '$1:');
    else if (!pkg)                       v = I18N.t('info_pkgmac_nopkg');
    else                                 v = I18N.t('info_pkgmac_value') + ' ' + COPG.deriveMac(pkg);
    info({ title: I18N.t('info_pkgmac_title'),
           message: I18N.t('info_pkgmac_msg') + ' ' + v + ' ' + I18N.t('info_pkgmac_resident') });
  }
  { const el = $m('#pfMacInfo');
    if (el) {
      el.addEventListener('click', openMacInfo);
      el.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openMacInfo(e); });
    } }
  // ↻ fill the field with a fresh random locally-administered MAC (same shape the derive produces).
  $m('#pfMacGen').addEventListener('click', () => {
    const b = Array.from({ length: 6 }, () => Math.floor(Math.random() * 256));
    b[0] = (b[0] & 0xFE) | 0x02;                           // locally-administered + unicast
    $m('#pfMacVal').value = b.map(x => x.toString(16).padStart(2, '0')).join(':');
  });
  $m('#pfMacChurn').addEventListener('change', syncMacVal);
  // Spoof WiFi MAC: PRO + RESIDENT (inline hook) → always the red 5s risk gate.
  $m('#pfMac').addEventListener('change', () => {
    if ($m('#pfMac').checked) {
      $m('#pfMac').checked = false;                         // require explicit acceptance
      if (!proLicensed()) { proUpsell('mac_pro_title', 'mac_pro_msg'); return; }   // PRO-only
      riskConfirm('mac', {
        title: I18N.t('mac_risk_title'),
        message: I18N.t('mac_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfMac').checked = true; syncMacVal(); },
      });
    } else { syncMacVal(); }
  });
  // ↻ fill with a fresh random unicast BT MAC (uppercase, same shape the derive produces).
  $m('#pfBtmacGen').addEventListener('click', () => {
    const b = Array.from({ length: 6 }, () => Math.floor(Math.random() * 256));
    b[0] = b[0] & 0xFE;                                    // unicast
    $m('#pfBtmacVal').value = b.map(x => x.toString(16).padStart(2, '0')).join(':').toUpperCase();
  });
  $m('#pfBtmacChurn').addEventListener('change', syncBtmacVal);
  // Spoof Bluetooth MAC: PRO + STEALTH (zero residency) → PRO upsell only, no risk gate.
  $m('#pfBtmac').addEventListener('change', () => {
    if ($m('#pfBtmac').checked && !proLicensed()) {
      $m('#pfBtmac').checked = false;
      proUpsell('btmac_pro_title', 'btmac_pro_msg');
      return;
    }
    syncBtmacVal();
  });
  $m('#pfDrmLevel').addEventListener('click', e => {
    const btn = e.target.closest('.seg__opt'); if (!btn) return;
    setDrmLevel(btn.dataset.level);
  });
  // ↻ generate a fresh random deviceUniqueId / systemId literal into the sub-row fields.
  $m('#pfDrmIdFmt').addEventListener('click', toggleDrmIdFmt);
  $m('#pfDrmIdGen').addEventListener('click', () => { $m('#pfDrmIdVal').value = genDrmId(); });
  $m('#pfDrmSysGen').addEventListener('click', () => { $m('#pfDrmSysVal').value = genDrmSys(); });
  // DRM (Widevine identity) spoof: RESIDENT LSPlant MediaDrm hook = detectable, so — like
  // GPU/mock — enabling it is gated behind an explicit "use at own risk" confirm (uncheck-
  // then-confirm). PRO-only: free tier gets the upsell. Sub-rows let the user pin/generate a
  // deviceUniqueId + systemId (blank = per-app derive).
  $m('#pfDrm').addEventListener('change', () => {
    if ($m('#pfDrm').checked) {
      $m('#pfDrm').checked = false;                        // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('drm_pro_title'),
          message: I18N.t('drm_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('drm', {
        title: I18N.t('drm_risk_title'),
        message: I18N.t('drm_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,                            // 5s cooldown on OK → forces a read
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfDrm').checked = true; syncDrmVal(); },
      });
    } else {
      syncDrmVal();                              // toggled off → hide the sub-rows
    }
  });
  // ↻ generate a fresh Luhn-valid IMEI literal into each SIM slot's field.
  $m('#pfImeiGen').addEventListener('click', () => { $m('#pfImeiVal').value = genImei(); });
  $m('#pfImei2Gen').addEventListener('click', () => { $m('#pfImei2Val').value = genImei(); });
  // IMEI spoof: RESIDENT LSPlant TelephonyManager hook = detectable → PRO + risk-gate
  // (uncheck-then-confirm), same class as GPU/mock/drm.
  $m('#pfImei').addEventListener('change', () => {
    if ($m('#pfImei').checked) {
      $m('#pfImei').checked = false;                       // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('imei_pro_title'),
          message: I18N.t('imei_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('imei', {
        title: I18N.t('imei_risk_title'),
        message: I18N.t('imei_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfImei').checked = true; syncImeiVal(); },
      });
    } else {
      syncImeiVal();                             // toggled off → hide the sub-row
    }
  });
  // ↻ generate a fresh IMSI / ICCID literal (per slot).
  $m('#pfImsiGen').addEventListener('click', () => { $m('#pfImsiVal').value = genImsi(); });
  $m('#pfImsi2Gen').addEventListener('click', () => { $m('#pfImsi2Val').value = genImsi(); });
  $m('#pfIccidGen').addEventListener('click', () => { $m('#pfIccidVal').value = genIccid(); });
  $m('#pfIccid2Gen').addEventListener('click', () => { $m('#pfIccid2Val').value = genIccid(); });
  // IMSI spoof: RESIDENT v7878 TelephonyManager.getSubscriberId hook = detectable → PRO + risk-gate,
  // same class as IMEI (shares the hook/dex).
  $m('#pfImsi').addEventListener('change', () => {
    if ($m('#pfImsi').checked) {
      $m('#pfImsi').checked = false;
      if (!proLicensed()) {
        confirm({
          title: I18N.t('imei_pro_title'),
          message: I18N.t('imei_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('imsi', {
        title: I18N.t('imsi_risk_title'),
        message: I18N.t('imsi_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfImsi').checked = true; syncImsiVal(); },
      });
    } else {
      syncImsiVal();
    }
  });
  // ICCID spoof: RESIDENT v7878 TelephonyManager.getSimSerialNumber hook = detectable → PRO + risk-gate.
  $m('#pfIccid').addEventListener('change', () => {
    if ($m('#pfIccid').checked) {
      $m('#pfIccid').checked = false;
      if (!proLicensed()) {
        confirm({
          title: I18N.t('imei_pro_title'),
          message: I18N.t('imei_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('iccid', {
        title: I18N.t('iccid_risk_title'),
        message: I18N.t('iccid_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfIccid').checked = true; syncIccidVal(); },
      });
    } else {
      syncIccidVal();
    }
  });
  // WebView User-Agent spoof: RESIDENT v7878 WebSettings hook = detectable → PRO + risk-gate
  // (uncheck-then-confirm), same class as DRM/IMEI. Targets are web apps (no anti-cheat).
  $m('#pfWebua').addEventListener('change', () => {
    if ($m('#pfWebua').checked) {
      $m('#pfWebua').checked = false;                      // require explicit risk acceptance
      if (!proLicensed()) {                                // PRO-only → block on free tier
        confirm({
          title: I18N.t('webua_pro_title'),
          message: I18N.t('webua_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('webua', {
        title: I18N.t('webua_risk_title'),
        message: I18N.t('webua_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfWebua').checked = true; syncWebuaVal(); },
      });
    } else {
      syncWebuaVal();                            // toggled off → hide the UA field
    }
  });
  // ── Canvas / WebGL fingerprint spoof — PRO + RESIDENT (v7878 WebView JS-shim) ──
  // Same PRO lock + risk-gate class as WebUA: greyed on free tier (tap → upsell), and enabling
  // pops the "use at own risk" red confirm (the hook stays mapped = detectable). WebView-only.
  $m('#pfWebfp').addEventListener('change', () => {
    if ($m('#pfWebfp').checked) {
      $m('#pfWebfp').checked = false;                     // require explicit risk acceptance
      if (!proLicensed()) {                               // PRO-only → block on free tier
        confirm({
          title: I18N.t('webfp_pro_title'),
          message: I18N.t('webfp_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      riskConfirm('webfp', {
        title: I18N.t('webfp_risk_title'),
        message: I18N.t('webfp_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfWebfp').checked = true; syncWebfpVal(); },
      });
    } else {
      syncWebfpVal();                                       // toggled off → hide the seed field
    }
  });
  // ── GAID (Google Advertising ID) spoof — PRO + RESIDENT (LSPlant GMS bindService hook) ──
  // Same PRO lock + risk-gate class as Mock: greyed on free tier (tap → upsell), and enabling
  // pops the "use at own risk" red confirm (LSPlant stays mapped = detectable). No profile seed
  // — the literal/preview sub-row lets the user PIN an exact UUID (gaid=<uuid>) or leave it BLANK
  // to derive a distinct per-app id from the package (bare 'gaid' tag).
  // GAID backend is arm-only (LSPlant + And64 arm64 / ele7enxxh arm32; x86 = install() stub → false).
  function gaidSupported() { return true; }   // shown on every ABI (v7878 bindService hook covers arm64/arm32/x86_64)
  function syncGaidLock() {
    const tag = $m('#pfGaidPro'), row = $m('#pfGaidRow'), cb = $m('#pfGaid');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  // The GAID literal/preview sub-row: shown only when Spoof Advertising ID is on. BLANK = derive
  // from the package (bare 'gaid' tag); a typed/generated UUID pins gaid=<uuid>. The derived id
  // shows as the PLACEHOLDER so the user sees what a blank field will produce.
  function syncGaidVal() {
    const row = $m('#pfGaidValRow'); if (!row) return;
    const on = $m('#pfGaid').checked && !$m('#pfGaidRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveGaid) ? COPG.deriveGaid(pkg) : '';
      $m('#pfGaidVal').placeholder = derived || '38400000-8cf0-11bd-b23e-10b96e40000d';
    }
  }
  // GAID ⓘ — preview the exact id the app reads (literal typed, else package-derived).
  function openGaidInfo(e) {
    e.preventDefault(); e.stopPropagation();
    const pkg  = previewPkg();
    const lit  = ($m('#pfGaidVal') ? $m('#pfGaidVal').value.trim().toLowerCase() : '');
    const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
    let msg;
    if (UUID.test(lit))  msg = I18N.t('info_pkggaid_msg') + ' ' + I18N.t('info_pkggaid_value') + ' ' + lit;
    else if (!pkg)       msg = I18N.t('info_pkggaid_msg') + ' ' + I18N.t('info_pkggaid_nopkg');
    else                 msg = I18N.t('info_pkggaid_msg') + ' ' + I18N.t('info_pkggaid_value') + ' ' + COPG.deriveGaid(pkg);
    info({ title: I18N.t('info_pkggaid_title'), message: msg });
  }
  { const el = $m('#pfGaidInfo');
    if (el) {
      el.addEventListener('click', openGaidInfo);
      el.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openGaidInfo(e); });
    } }
  // ↻ generate a fresh random UUID literal into the sub-row field.
  $m('#pfGaidGen').addEventListener('click', () => { $m('#pfGaidVal').value = genGaid(); });
  // GAID is PRO + RESIDENT → uncheck-then-confirm: free tier upsell, else red risk warn (5s).
  $m('#pfGaid').addEventListener('change', () => {
    if ($m('#pfGaid').checked) {
      $m('#pfGaid').checked = false;                       // require explicit risk acceptance
      if (!proLicensed()) { proUpsell('gaid_pro_title', 'gaid_pro_msg'); return; }  // PRO-only
      riskConfirm('gaid', {
        title: I18N.t('gaid_risk_title'),
        message: I18N.t('gaid_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,                            // 5s cooldown on OK → forces a read
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfGaid').checked = true; syncGaidVal(); },
      });
    } else {
      syncGaidVal();
    }
  });

  // ── App Set ID (PRO, resident) — mirrors the GAID row exactly (same arm-only backend) ──
  function syncAppSetLock() {
    const tag = $m('#pfAppSetPro'), row = $m('#pfAppSetRow'), cb = $m('#pfAppSet');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function syncAppSetVal() {
    const row = $m('#pfAppSetValRow'); if (!row) return;
    const on = $m('#pfAppSet').checked && !$m('#pfAppSetRow').classList.contains('trow--locked');
    row.style.display = on ? '' : 'none';
    if (on) {
      const pkg = previewPkg();
      const derived = (pkg && COPG.deriveAppSetId) ? COPG.deriveAppSetId(pkg) : '';
      $m('#pfAppSetVal').placeholder = derived || 'auto (per-app)';
    }
  }
  function openAppSetInfo(e) {
    e.preventDefault(); e.stopPropagation();
    const pkg  = previewPkg();
    const lit  = ($m('#pfAppSetVal') ? $m('#pfAppSetVal').value.trim().toLowerCase() : '');
    const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
    let msg;
    if (UUID.test(lit))  msg = I18N.t('info_pkgappset_msg') + ' ' + I18N.t('info_pkgappset_value') + ' ' + lit;
    else if (!pkg)       msg = I18N.t('info_pkgappset_msg') + ' ' + I18N.t('info_pkgappset_nopkg');
    else                 msg = I18N.t('info_pkgappset_msg') + ' ' + I18N.t('info_pkgappset_value') + ' ' + COPG.deriveAppSetId(pkg);
    info({ title: I18N.t('info_pkgappset_title'), message: msg });
  }
  { const el = $m('#pfAppSetInfo');
    if (el) {
      el.addEventListener('click', openAppSetInfo);
      el.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openAppSetInfo(e); });
    } }
  $m('#pfAppSetGen').addEventListener('click', () => { $m('#pfAppSetVal').value = genGaid(); });   // App Set ID is a UUID too
  $m('#pfAppSet').addEventListener('change', () => {
    if ($m('#pfAppSet').checked) {
      $m('#pfAppSet').checked = false;                     // require explicit risk acceptance
      if (!proLicensed()) { proUpsell('appset_pro_title', 'appset_pro_msg'); return; }  // PRO-only
      riskConfirm('appset', {
        title: I18N.t('appset_risk_title'),
        message: I18N.t('appset_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfAppSet').checked = true; syncAppSetVal(); },
      });
    } else {
      syncAppSetVal();
    }
  });
  // Hide VPN — base tag ':vpn' = the in-app LSPlant ART layer (NetworkCapabilities/
  // NetworkInfo/LinkProperties/ConnectivityManager/NetworkInterface). The old
  // system_server path was removed (it collided with LSPosed's LSPlant), so this is
  // now a RESIDENT in-app hook → detectable, and pairip's integrity scan CRASHES
  // banking/pairip apps under it. So the toggle is itself risk-gated (uncheck-then-confirm
  // red warn), same as GPU/mock. (The old "Native hooks" sub-toggle was removed — the base
  // hook is already resident/risky, so the extra layer was the same risk class.)
  // Main "Hide VPN" = the SAFE system_server :vpns hook (pairip-safe, no residency) → NO risk gate,
  // gray (!) info. Toggling it just reveals/hides the nested Aggressive sub.
  $m('#pfVpn').addEventListener('change', () => {
    if ($m('#pfVpn').checked && ssBlocked($m('#pfVpn'))) { syncVpn(); return; }   // master off → block + guide
    syncVpn();
  });
  // Nested "Aggressive (in-app)" = the RESIDENT :vpnn hook (ART + native getifaddrs) → detectable,
  // crashes pairip/banking → red ⚠ risk-gated (uncheck-then-confirm), same as GPU/mock.
  $m('#pfVpnAgg').addEventListener('change', () => {
    if ($m('#pfVpnAgg').checked) {
      $m('#pfVpnAgg').checked = false;          // gate the ON transition behind the warn
      riskConfirm('vpn', {
        title: I18N.t('vpn_risk_title'),
        message: I18N.t('vpn_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => { $m('#pfVpnAgg').checked = true; },
      });
    }
  });
  // Wallpaper-id spoof (wallpaper[=<id>], system_server) — FREE + stealth, pairip-safe (out-of-process),
  // so NO risk gate, gray (!) info like :vpns. Toggling reveals/hides the Fake-ID sub-input; a blank id
  // means the bare 'wallpaper' tag → the hook forges -1 ("no wallpaper", mainly the verify test), a
  // number pins a plausible per-persona id. ssBlocked gates it behind the master System-Server switch.
  function syncWallpaperId() {
    const row = $m('#pfWallpaperIdRow'); if (!row) return;
    row.style.display = $m('#pfWallpaper').checked ? '' : 'none';
  }
  $m('#pfWallpaper').addEventListener('change', () => {
    if ($m('#pfWallpaper').checked && ssBlocked($m('#pfWallpaper'))) { syncWallpaperId(); return; }  // master off → block + guide
    if ($m('#pfWallpaper').checked && !proLicensed()) { $m('#pfWallpaper').checked = false; proUpsell('wallpaper_pro_title', 'wallpaper_pro_msg'); syncWallpaperId(); return; }  // PRO-only
    syncWallpaperId();
  });
  // Google-account spoof (account[=<email>], system_server) — FREE + stealth (out-of-process),
  // pairip-safe → NO risk gate, gray (!) info like :vpns/wallpaper. Toggling reveals/hides the fake-
  // email input; blank email = the bare 'account' tag → the hook HIDES (empty account list), an email
  // returns one forged com.google account. ssBlocked gates it behind the master System-Server switch.
  // Mode segment: Hide (bare 'account' → empty list) vs Fake email ('account=<email>'). Reveal the
  // Mode row whenever the toggle is on; reveal the email field only in Fake mode.
  function accountMode() { return ($m('#pfAccountMode') || {}).dataset ? $m('#pfAccountMode').dataset.mode || 'hide' : 'hide'; }
  function setAccountMode(mode) {
    const seg = $m('#pfAccountMode'); if (!seg) return;
    if (mode !== 'fake') mode = 'hide';
    seg.dataset.mode = mode;
    seg.querySelectorAll('.seg__opt').forEach(b => b.classList.toggle('active', b.dataset.mode === mode));
    syncAccountEmail();
  }
  function syncAccountEmail() {
    const on = $m('#pfAccount').checked;
    const modeRow = $m('#pfAccountModeRow'), emailRow = $m('#pfAccountEmailRow');
    if (modeRow)  modeRow.style.display  = on ? '' : 'none';
    if (emailRow) emailRow.style.display = (on && accountMode() === 'fake') ? '' : 'none';
  }
  $m('#pfAccount').addEventListener('change', () => {
    if ($m('#pfAccount').checked && ssBlocked($m('#pfAccount'))) { syncAccountEmail(); return; }  // master off → block + guide
    if ($m('#pfAccount').checked && !proLicensed()) { $m('#pfAccount').checked = false; proUpsell('account_pro_title', 'account_pro_msg'); syncAccountEmail(); return; }  // PRO-only
    syncAccountEmail();
  });
  $m('#pfAccountMode').addEventListener('click', e => {
    const btn = e.target.closest('.seg__opt'); if (!btn) return;
    setAccountMode(btn.dataset.mode);
  });
  $m('#pfGpuModel').addEventListener('click', () => {
    if (!w.GpuPicker) return;
    if (!proLicensed()) { proUpsell('gpu_pro_title', 'gpu_pro_msg'); return; }  // PRO-gate the picker
    GpuPicker.open($m('#pfGpuModel').dataset.gpuKey || '', sel => setGpuSelection(sel.key));
  });
  $m('#pfWebuaPick').addEventListener('click', () => {
    if (!w.UaPicker) return;
    if (!proLicensed()) { proUpsell('webua_pro_title', 'webua_pro_msg'); return; }
    UaPicker.open($m('#pfWebuaPick').dataset.uaKey || '', sel => setWebuaSelection(sel.key));
  });
  $m('#pfGpsPick').addEventListener('click', () => {
    if (!w.GpsPicker) return;
    if (!proLicensed()) { proUpsell('gps_pro_title', 'gps_pro_msg'); return; }   // PRO-gate the picker
    GpsPicker.open($m('#pfGpsPick').dataset.gpsLat || '', $m('#pfGpsPick').dataset.gpsLng || '',
      sel => setGpsSelection(sel.lat, sel.lng));
  });

  // ── SIM / Carrier spoof (PRO) ──────────────────────────────────────────
  // Two modes via the Safe|Aggressive segment: SAFE (sim=) = COW gsm.* props only
  // (stealth); AGGRESSIVE (simx=) = COW + resident ISub-proxy dex (detectable → red
  // risk-warning, never anti-cheat). Carrier picked from SIM/manifest.json.
  // Per-slot carrier pickers: SIM 1 (#pfSim1Model) + SIM 2 (#pfSim2Model). Each holds a
  // carrier key on its dataset ('' = that slot left real). Both same → written as the bare
  // sim=<key> shorthand; different → sim=A@0 + sim=B@1 (see setCarrierModel).
  function setSim(n, key) {
    const btn = $m('#pfSim' + n + 'Model'); if (!btn) return;
    btn.dataset.simKey = key || '';
    $m('#pfSim' + n + 'Text').textContent = (key && COPG.carrierModelName(key)) || I18N.t('pkg_sim_none');
  }
  // Phantom (Add-SIM) mode carries ONE carrier on its own picker (#pfSimFakeModel).
  function setSimFake(key) {
    const btn = $m('#pfSimFakeModel'); if (!btn) return;
    btn.dataset.simKey = key || '';
    $m('#pfSimFakeText').textContent = (key && COPG.carrierModelName(key)) || I18N.t('pkg_sim_none');
  }
  function simMode() { return $m('#pfSimMode').dataset.mode || 'safe'; }
  function setSimMode(mode) {
    const seg = $m('#pfSimMode'); if (!seg) return;
    if (mode !== 'aggressive' && mode !== 'phantom') mode = 'safe';
    seg.dataset.mode = mode;
    seg.querySelectorAll('.seg__opt').forEach(b =>
      b.classList.toggle('active', b.dataset.mode === mode));
    syncSimPick();
  }
  function syncSimPick() {
    const on = $m('#pfSim').checked;
    const phantom = on && simMode() === 'phantom';   // Add-SIM: one picker, per-slot hidden
    $m('#pfSimModeRow').style.display = on ? '' : 'none';
    $m('#pfSim1Row').style.display    = (on && !phantom) ? '' : 'none';
    $m('#pfSim2Row').style.display    = (on && !phantom) ? '' : 'none';
    $m('#pfSimFakeRow').style.display = phantom ? '' : 'none';
  }
  // SIM/carrier is PRO (native gates it in pre/post). Same lock pattern as COW/AID/GPU.
  function syncSimLock() {
    const tag = $m('#pfSimPro'), row = $m('#pfSimRow'), cb = $m('#pfSim');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  $m('#pfSim').addEventListener('change', () => {
    if ($m('#pfSim').checked) {
      if (!proLicensed()) {                                // PRO-only → block + upsell
        $m('#pfSim').checked = false;
        confirm({
          title: I18N.t('sim_pro_title'),
          message: I18N.t('sim_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'),
          onOk: () => { closeAll(); if (w.License) License.reveal(); },
        });
        return;
      }
      setSimMode('safe');                                  // default to the stealth mode
      // default: fake both SIMs with the same carrier (bare shorthand → single-SIM safe)
      if (!$m('#pfSim1Model').dataset.simKey && !$m('#pfSim2Model').dataset.simKey) {
        setSim(1, 'tmobile_us'); setSim(2, 'tmobile_us');
      }
    }
    syncSimPick();
  });
  // Segment: Safe ↔ Aggressive. Switching TO aggressive pops the resident-risk warning
  // (same class as GPU); dismissing reverts to Safe.
  $m('#pfSimMode').addEventListener('click', e => {
    const btn = e.target.closest('.seg__opt'); if (!btn) return;
    const want = btn.dataset.mode;
    if (want === simMode()) return;
    if (want === 'aggressive') {
      riskConfirm('sim', {
        title: I18N.t('sim_risk_title'),
        message: I18N.t('sim_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => setSimMode('aggressive'),
      });
    } else if (want === 'phantom') {
      // Add-SIM = resident (ISub-inject + presence-gate proxies stay mapped → detectable).
      riskConfirm('sim', {
        title: I18N.t('simfake_risk_title'),
        message: I18N.t('simfake_risk_msg'),
        okLabel: I18N.t('gpu_risk_ok'),
        danger: true,
        countdown: 5,
        link: { href: 'https://t.me/COPG_chat', label: I18N.t('gpu_risk_link') },
        onOk: () => {
          setSimMode('phantom');
          if (!$m('#pfSimFakeModel').dataset.simKey) setSimFake('tmobile_us');
        },
      });
    } else {
      setSimMode('safe');
    }
  });
  // Phantom carrier picker (the added SIM's identity).
  $m('#pfSimFakeModel').addEventListener('click', () => {
    if (!w.CarrierPicker) return;
    if (!proLicensed()) { proUpsell('sim_pro_title', 'sim_pro_msg'); return; }
    CarrierPicker.open($m('#pfSimFakeModel').dataset.simKey || '', sel => setSimFake(sel.key));
  });
  // Each SIM slot opens the carrier picker; picking "None (real)" clears that slot.
  $m('#pfSim1Model').addEventListener('click', () => {
    if (!w.CarrierPicker) return;
    if (!proLicensed()) { proUpsell('sim_pro_title', 'sim_pro_msg'); return; }  // PRO-gate the picker
    CarrierPicker.open($m('#pfSim1Model').dataset.simKey || '', sel => setSim(1, sel.key));
  });
  $m('#pfSim2Model').addEventListener('click', () => {
    if (!w.CarrierPicker) return;
    if (!proLicensed()) { proUpsell('sim_pro_title', 'sim_pro_msg'); return; }  // PRO-gate the picker
    CarrierPicker.open($m('#pfSim2Model').dataset.simKey || '', sel => setSim(2, sel.key));
  });

  // ── Per-App Proxy (proxy=<key>, PRO, kernel TPROXY → remote SOCKS5) ───────
  // Not resident in the app's memory (kernel routing, no interface), but the proxy IP is
  // server-side detectable → same red risk gate as the resident features, never anti-cheat.
  function setProxyModel(key) {
    const btn = $m('#pfProxyModel'); if (!btn) return;
    btn.dataset.proxyKey = key || '';
    $m('#pfProxyText').textContent = (key && COPG.proxyModelName(key)) || I18N.t('proxy_none');
  }
  function syncProxyPick() { const r = $m('#pfProxyPickRow'); if (r) r.style.display = $m('#pfProxy').checked ? '' : 'none'; }
  function syncProxyLock() {
    const tag = $m('#pfProxyPro'), row = $m('#pfProxyRow'), cb = $m('#pfProxy');
    const locked = !proLicensed();
    if (tag) tag.hidden = !locked;
    if (row) row.classList.toggle('trow--locked', locked);
    if (locked && cb) cb.checked = false;
  }
  function openProxyPicker() {
    if (!w.ProxyPicker) return;
    if (!proLicensed()) { proUpsell('proxy_pro_title', 'proxy_pro_msg'); return; }
    ProxyPicker.open($m('#pfProxyModel').dataset.proxyKey || '', sel => setProxyModel(sel.key));
  }
  $m('#pfProxy').addEventListener('change', () => {
    // Stealth: kernel TPROXY, no residency/interface → no red risk gate (unlike the resident hooks).
    // Just PRO-gate + reveal the picker. The ⓘ explains the only caveat (proxy-IP reputation).
    if ($m('#pfProxy').checked) {
      if (!proLicensed()) {
        $m('#pfProxy').checked = false;
        confirm({ title: I18N.t('proxy_pro_title'), message: I18N.t('proxy_pro_msg'),
          okLabel: I18N.t('gpu_pro_ok'), onOk: () => { closeAll(); if (w.License) License.reveal(); } });
        return;
      }
      syncProxyPick();
      if (!$m('#pfProxyModel').dataset.proxyKey) openProxyPicker();
    } else {
      syncProxyPick();
    }
  });
  $m('#pfProxyModel').addEventListener('click', openProxyPicker);

  // COW Prop Spoof is stealth (per-process copy-on-write prop edit, module
  // unloads before the app runs → zero memory residency), so no risk confirm —
  // it just toggles like any other spoof option.

  // ⓘ on the cpu/cow rows → plain-language explanation. preventDefault +
  // stopPropagation so tapping the icon doesn't toggle the row's checkbox.
  const INFO_KEYS = {
    user:     ['info_user_title',     'info_user_msg'],
    withcpu:  ['info_withcpu_title',  'info_withcpu_msg'],
    block:    ['info_block_title',    'info_block_msg'],
    cow:      ['info_cow_title',      'info_cow_msg'],
    btmac:    ['info_btmac_title',    'info_btmac_msg'],
    serial:   ['info_serial_title',   'info_serial_msg'],
    hidedev:  ['info_hidedev_title',  'info_hidedev_msg'],
    randid:   ['info_randid_title',   'info_randid_msg'],
    tz:       ['info_tz_title',       'info_tz_msg'],
    lang:     ['info_lang_title',     'info_lang_msg'],
    gpu:      ['info_gpu_title',      'info_gpu_msg'],
    wallpaper:['info_wallpaper_title','info_wallpaper_msg'],
    account:  ['info_account_title',  'info_account_msg'],
    mock:     ['info_mock_title',     'info_mock_msg'],
    sensor:   ['info_sensor_title',   'info_sensor_msg'],
    getprop:  ['info_getprop_title',  'info_getprop_msg'],
    gps:      ['info_gps_title',      'info_gps_msg'],
    cell:     ['info_cell_title',     'info_cell_msg'],
    uptime:   ['info_uptime_title',   'info_uptime_msg'],
    drm:      ['info_drm_title',      'info_drm_msg'],
    imei:     ['info_imei_title',     'info_imei_msg'],
    imsi:     ['info_imsi_title',     'info_imsi_msg'],
    iccid:    ['info_iccid_title',    'info_iccid_msg'],
    webua:    ['info_webua_title',    'info_webua_msg'],
    webfp:    ['info_webfp_title',    'info_webfp_msg'],
    vpn:      ['info_vpn_title',      'info_vpn_msg'],
    vpns:     ['info_vpns_title',     'info_vpns_msg'],
    playsrc:  ['info_playsrc_title',  'info_playsrc_msg'],
    pairip:   ['info_pairip_title',   'info_pairip_msg'],
    tensorfeat: ['info_tensorfeat_title', 'info_tensorfeat_msg'],
    sim:      ['info_sim_title',      'info_sim_msg'],
    proxy:    ['info_proxy_title',    'info_proxy_msg'],
    dnd:      ['info_dnd_title',      'info_dnd_msg'],
    dab:      ['info_dab_title',      'info_dab_msg'],
    kso:      ['info_kso_title',      'info_kso_msg'],
    nolog:    ['info_nolog_title',    'info_nolog_msg'],
    dpi:      ['info_dpi_title',      'info_dpi_msg'],
  };
  function openInfo(el, e) {
    e.preventDefault(); e.stopPropagation();    // don't let the row's label toggle the checkbox
    const k = INFO_KEYS[el.dataset.info]; if (!k) return;
    info({ title: I18N.t(k[0]), message: I18N.t(k[1]) });
  }
  $$('#pfToggles .trow__info').forEach(el => {
    el.addEventListener('click', e => openInfo(el, e));
    el.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openInfo(el, e); });
  });

  // AID ⓘ is DYNAMIC: it previews the exact id this app will read (derived from the
  // package via COPG.deriveAndroidId — byte-identical to the native derive), or the
  // literal if one is typed. (Its plain .trow__info also got the generic no-op handler
  // above, which just stops the row toggle — harmless; we add the popup here.)
  function openAidInfo(e) {
    e.preventDefault(); e.stopPropagation();
    const pkg  = previewPkg();
    const lit  = ($m('#pfAidVal') ? $m('#pfAidVal').value.trim().toLowerCase() : '');
    let msg;
    if (/^[0-9a-f]{16}$/.test(lit))                  // exact id typed/generated → preview it as-is
      msg = I18N.t('info_pkgaid_msg') + ' ' + I18N.t('info_pkgaid_value') + ' ' + lit;
    else if (!pkg)  msg = I18N.t('info_pkgaid_msg') + ' ' + I18N.t('info_pkgaid_nopkg');
    else            msg = I18N.t('info_pkgaid_msg') + ' ' + I18N.t('info_pkgaid_value') + ' ' + COPG.deriveAndroidId(pkg);
    msg += ' ' + I18N.t('info_pkgaid_consistency');
    info({ title: I18N.t('info_pkgaid_title'), message: msg });
  }
  const aidInfoEl = $m('#pfAidInfo');
  if (aidInfoEl) {
    aidInfoEl.addEventListener('click', openAidInfo);
    aidInfoEl.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') openAidInfo(e); });
  }
  // Android ID is PRO — block the toggle on free tier and upsell (native re-gates too).
  $m('#pfAid').addEventListener('change', () => {
    if ($m('#pfAid').checked && !proLicensed()) {
      $m('#pfAid').checked = false;
      confirm({
        title: I18N.t('aid_pro_title'),
        message: I18N.t('aid_pro_msg'),
        okLabel: I18N.t('gpu_pro_ok'),
        onOk: () => { closeAll(); if (w.License) License.reveal(); },
      });
    }
    syncAidVal();   // reveal/hide the literal id sub-row
  });
  // COW prop spoof is PRO too — block on free tier and upsell (native re-gates in pre).
  $m('#pfCow').addEventListener('change', () => {
    if ($m('#pfCow').checked && !proLicensed()) {
      $m('#pfCow').checked = false;
      confirm({
        title: I18N.t('cow_pro_title'),
        message: I18N.t('cow_pro_msg'),
        okLabel: I18N.t('gpu_pro_ok'),
        onOk: () => { closeAll(); if (w.License) License.reveal(); },
      });
    }
  });
  // Fake Play install source (:playsrc, PRO, system_server stealth-from-app): block on free tier + upsell.
  // Reveal the "Also block PairIP check" helper sub-row when on.
  $m('#pfPlaysrc').addEventListener('change', () => {
    if ($m('#pfPlaysrc').checked && ssBlocked($m('#pfPlaysrc'))) { syncPlaysrc(); return; }   // master off
    if ($m('#pfPlaysrc').checked && !proLicensed()) {
      $m('#pfPlaysrc').checked = false;
      proUpsell('playsrc_pro_title', 'playsrc_pro_msg');
    }
    syncPlaysrc();
  });
  // Block PairIP license check (:pairip, PRO) — INDEPENDENT of playsrc. Stealth-from-app (out-of-process
  // in system_server), so NOT risk-gated — just PRO. #pfPairip is the source of truth; keep the helper
  // sub-row under playsrc mirrored.
  $m('#pfPairip').addEventListener('change', () => {
    if ($m('#pfPairip').checked && ssBlocked($m('#pfPairip'))) { const h0 = $m('#pfPlaysrcPairip'); if (h0) h0.checked = false; return; }
    if ($m('#pfPairip').checked && !proLicensed()) {
      $m('#pfPairip').checked = false;
      proUpsell('pairip_pro_title', 'pairip_pro_msg');
    }
    const h = $m('#pfPlaysrcPairip'); if (h) h.checked = $m('#pfPairip').checked;
  });
  // Helper: "Also block PairIP check" under playsrc — drives the same :pairip tag via #pfPairip.
  $m('#pfPlaysrcPairip').addEventListener('change', () => {
    if ($m('#pfPlaysrcPairip').checked && !proLicensed()) {
      $m('#pfPlaysrcPairip').checked = false;
      proUpsell('pairip_pro_title', 'pairip_pro_msg');
      return;
    }
    $m('#pfPairip').checked = $m('#pfPlaysrcPairip').checked;
  });
  // Tensor feature targets (:tensorfeat, PRO) — INDEPENDENT. Stealth-from-app (out-of-process in
  // system_server), so NOT risk-gated — just PRO.
  $m('#pfTensorfeat').addEventListener('change', () => {
    if ($m('#pfTensorfeat').checked && ssBlocked($m('#pfTensorfeat'))) return;
    if ($m('#pfTensorfeat').checked && !proLicensed()) {
      $m('#pfTensorfeat').checked = false;
      proUpsell('tensorfeat_pro_title', 'tensorfeat_pro_msg');
    }
  });

  function openPackage(pkg) {
    const form = $m('#packageForm');
    batchItems = null;                                  // a normal open is never a batch
    form.classList.remove('is-batch');
    { const pf = $m('#pfPackage').closest('.field'), nf = $m('#pfName').closest('.field');
      if (pf) pf.style.display = ''; if (nf) nf.style.display = ''; }
    { const h = $m('#pfBatchHint'); if (h) h.hidden = true; }
    clearErrors(form);
    $m('#pfError').hidden = true;
    drmIdHex = false; applyDrmIdFmt();   // always open the Device Unique ID field in Base64 (app format)
    editingPkg = pkg ? { clean: pkg.clean, type: pkg.type, deviceKey: pkg.deviceKey, userScope: pkg.userScope || '' } : null;
    $m('#packageModalTitle').textContent = I18N.t(pkg ? 'pkg_edit_title' : 'pkg_add_title');

    if (pkg) {
      $m('#pfPackage').value = pkg.clean;
      $m('#pfName').value = pkg.name && pkg.name !== pkg.clean ? pkg.name : (COPG.names[pkg.clean] || '');
      setType();
      populateDeviceSelect(pkg.deviceKey || COPG.THIS_DEVICE_PKEY);
      setPkgUserScope(pkg.userScope || '');      // u=<id> per-user scope ('' = all users), read-only tag
      $m('#pfWithCpu').checked  = !!pkg.with_cpu;
      $m('#pfBlock').checked    = !!pkg.blocked;
      $m('#pfCow').checked      = !!pkg.cow;
      $m('#pfAid').checked      = !!pkg.aid;
      $m('#pfAidVal').value     = pkg.aidLiteral || '';   // literal aid=<hex>, else '' (derive/preview)
      $m('#pfSerial').checked   = !!pkg.serial;
      $m('#pfSerialVal').value  = pkg.serialLiteral || '';  // literal serial=<value>, else '' (derive from package)
      $m('#pfGaid').checked     = !!pkg.gaid;
      $m('#pfGaidVal').value    = pkg.gaidLiteral || '';  // literal gaid=<uuid>, else '' (derive/preview)
      $m('#pfAppSet').checked   = !!pkg.appset;
      $m('#pfAppSetVal').value  = pkg.appsetLiteral || ''; // literal appset=<uuid>, else '' (derive/preview)
      $m('#pfHideDev').checked  = !!pkg.hideDev;
      $m('#pfRandid').checked   = !!pkg.randid;   // churn derived IDs each launch (PRO, stealth)
      $m('#pfDnd').checked      = !!pkg.dnd;
      $m('#pfDab').checked      = !!pkg.dab;
      $m('#pfKso').checked      = !!pkg.kso;
      $m('#pfNolog').checked    = !!pkg.nolog;
      $m('#pfDpi').value        = pkg.dpi || '';           // dpi=<n> per-app screen density (controller tweak)
      $m('#pfGpu').checked      = !!pkg.gpu;
      $m('#pfMock').checked     = !!pkg.mock;
      $m('#pfSensor').checked   = !!pkg.sensor;   // sensor-roster spoof (PRO, resident)
      $m('#pfGetprop').checked  = !!pkg.getprop;  // getprop-child consistency (PRO, resident)
      $m('#pfGps').checked      = !!pkg.gps;             // gps=<lat>,<lng> resident Location coord overwrite (PRO)
      setGpsSelection(pkg.gpsLat || '', pkg.gpsLng || '');
      $m('#pfCell').checked     = !!pkg.cell;            // cell=<lac>,<ci> resident cell-tower overwrite (PRO)
      $m('#pfCellLac').value    = pkg.cellLac || '';
      $m('#pfCellCi').value     = pkg.cellCi || '';
      $m('#pfCellMcc').value    = pkg.cellMcc || '';
      $m('#pfCellMnc').value    = pkg.cellMnc || '';
      $m('#pfUptime').checked   = !!pkg.uptime;
      $m('#pfUptimeD').value    = pkg.uptimeSeconds ? +(pkg.uptimeSeconds / 86400).toFixed(2) : '';  // seconds → days
      $m('#pfBootid').checked      = !!pkg.bootid;          // bootid / bootid=rnd / bootid=<uuid> (PRO)
      $m('#pfBootidChurn').checked = !!pkg.bootidChurn;     // bootid=rnd → fresh each launch
      $m('#pfBootidVal').value     = pkg.bootidLiteral || '';   // pinned UUID, '' = derive/churn
      $m('#pfMac').checked      = !!pkg.mac;                // mac / mac=rnd / mac=<12hex> (PRO)
      $m('#pfMacChurn').checked = !!pkg.macChurn;           // mac=rnd → fresh each launch
      $m('#pfMacVal').value     = (pkg.macLiteral || '').replace(/(..)(?=.)/g, '$1:');   // 12hex → xx:xx:… display
      $m('#pfBtmac').checked      = !!pkg.btmac;            // btmac / btmac=rnd / btmac=<12hex> (PRO, stealth)
      $m('#pfBtmacChurn').checked = !!pkg.btmacChurn;       // btmac=rnd → fresh each launch
      $m('#pfBtmacVal').value     = (pkg.btmacLiteral || '').replace(/(..)(?=.)/g, '$1:').toUpperCase();  // 12hex → XX:XX:… display
      $m('#pfRr').checked       = !!pkg.rr;                // rr=<hz> resident refresh-rate hook (FREE)
      $m('#pfRrHz').value       = pkg.refreshHz || '';    // spoofed Hz, '' = off
      $m('#pfDrm').checked      = !!pkg.drm;
      setDrmLevel(pkg.drmLevel);                          // drm=<level>, default L1
      $m('#pfDrmIdVal').value   = drmIdHexToField(pkg.drmId);  // drmid=<hex> tag → shown in the current B64/HEX format; '' = derive/preview
      $m('#pfDrmSysVal').value  = pkg.drmSys || '';       // literal drmsys=<int>, else ''
      $m('#pfImei').checked     = !!pkg.imei;
      $m('#pfImeiVal').value    = pkg.imeiLiteral || '';  // literal imei=<15> slot 0, else '' (derive/preview)
      $m('#pfImei2Val').value   = pkg.imei2Literal || ''; // literal imei2=<15> slot 1, else ''
      $m('#pfImsi').checked     = !!pkg.imsi;
      $m('#pfImsiVal').value    = pkg.imsiLiteral || '';  // literal imsi=<14-15> slot 0, else '' (derive/preview)
      $m('#pfImsi2Val').value   = pkg.imsi2Literal || ''; // literal imsi2=<14-15> slot 1, else ''
      $m('#pfIccid').checked    = !!pkg.iccid;
      $m('#pfIccidVal').value   = pkg.iccidLiteral || ''; // literal iccid=<19-20> slot 0, else '' (derive/preview)
      $m('#pfIccid2Val').value  = pkg.iccid2Literal || '';// literal iccid2=<19-20> slot 1, else ''
      $m('#pfWebua').checked    = !!pkg.webua;            // ua=<key> WebView User-Agent (PRO, resident)
      $m('#pfWebfp').checked    = !!pkg.webfp;            // webfp canvas fp spoof (PRO, resident)
      if ($m('#pfWebfpVal')) $m('#pfWebfpVal').value = pkg.webfpSeed || '';   // webfp=<seed> pinned value, '' = derive
      setWebuaSelection(pkg.webua || '');                 // ua=<key> UA profile, '' = off
      $m('#pfVpn').checked      = !!pkg.vpns;     // main "Hide VPN" = safe :vpns (system_server)
      $m('#pfVpnAgg').checked   = !!pkg.vpnAgg;   // nested aggressive = resident in-app :vpn/:vpnn
      $m('#pfPlaysrc').checked  = !!pkg.playsrc;  // main "Fake Play install source" = :playsrc (system_server, PRO)
      $m('#pfPairip').checked   = !!pkg.pairip;   // nested "Block PairIP license check" = :pairip (system_server, PRO)
      $m('#pfTensorfeat').checked = !!pkg.tensorfeat; // "Tensor feature targets" = :tensorfeat (system_server, PRO)
      $m('#pfWallpaper').checked = !!pkg.wallpaper;   // wallpaper[=<id>] = system-side wallpaper-id spoof (FREE)
      $m('#pfWallpaperId').value = pkg.wallpaperId || '';  // '' = bare tag → -1
      $m('#pfAccount').checked = !!pkg.account;       // account[=<email>] = system-side google-account spoof (FREE)
      $m('#pfAccountEmail').value = pkg.accountEmail || '';  // '' = bare tag → hide (empty list)
      setAccountMode(pkg.account && pkg.accountEmail ? 'fake' : 'hide');  // email present → Fake, else Hide
      $m('#pfSim').checked      = !!pkg.sim;
      $m('#pfProxy').checked    = !!pkg.proxy;    // proxy=<key> per-app SOCKS5 (PRO)
      setProxyModel(pkg.proxy || '');
      setCpuSelection(pkg.cpuModel || '');
      setGpuSelection(pkg.gpuModel || '');
      setTzSelection(pkg.timezone || '');
      setLangSelection(pkg.locale || '');
      setSimMode(pkg.sim || 'safe');
      if (pkg.sim === 'phantom') {                          // Add-SIM: carrier on the phantom picker
        setSimFake(pkg.simS0 || '');
        setSim(1, ''); setSim(2, '');
      } else {
        setSimFake('');
        setSim(1, pkg.simS0 || '');
        setSim(2, pkg.simS1 || '');
      }
    } else {
      form.reset();
      setType();
      populateDeviceSelect(COPG.THIS_DEVICE_PKEY);  // default new packages to "This Device" (no identity spoof)
      setPkgUserScope('');                           // default new packages to All users (no tag)

      setCpuSelection('');
      setGpuSelection('');
      setGpsSelection('', '');
      setTzSelection('');
      setLangSelection('');
      setDrmLevel('L1');   // the seg is not a form control, so form.reset() can't clear it
      setAccountMode('hide');   // default new packages to Hide (segment is not a form control)
      setSimMode('safe');
      setSim(1, '');
      setSim(2, '');
      setSimFake('');
      setProxyModel('');
    }
    if (COPG.getCpuModels) COPG.getCpuModels();  // warm the model-name cache for display
    if (COPG.getProxyModels) COPG.getProxyModels();  // warm proxy-name cache too
    if (COPG.getGpuModels) COPG.getGpuModels();  // warm GPU model-name cache too
    if (COPG.getPlaces) COPG.getPlaces().then(() => setGpsSelection($m('#pfGpsPick').dataset.gpsLat || '', $m('#pfGpsPick').dataset.gpsLng || ''));  // warm cities.json → re-render the GPS chip name
    if (COPG.getTzZones) COPG.getTzZones();      // warm timezone list (custom zones) for display
    if (COPG.getLangLocales) COPG.getLangLocales();  // warm locale list (custom locales) for display
    if (COPG.getCarrierModels) COPG.getCarrierModels();  // warm carrier-name cache too
    if (w.License && License.refresh) License.refresh();   // refresh _licState for the PRO locks
    syncAidAvail();                             // final gate + AID PRO lock after checkboxes are set
    syncAidVal();                               // reveal the AID literal/preview sub-row if AID on
    syncSerialLock();                           // grey per-app serial if the profile sets SERIAL (+ reveal sub-row)
    syncCpuPick();
    syncGpuPick();
    syncVpn();                                 // reveal the Aggressive sub-row if VPN hide is already on
    syncWallpaperId();                          // reveal the Fake-ID input if wallpaper spoof is on
    syncAccountEmail();                         // reveal the fake-email input if account spoof is on
    syncGpuLock();                             // show/hide the PRO chip on the GPU row
    syncCowLock();                              // show/hide the PRO chip on the COW row
    syncRandidLock();                           // show/hide the PRO chip on the Randomize-IDs row
    syncRandidHint();                           // show the "rotates the IDs you enable below" hint when randid is on
    syncPlaysrcLock();                          // show/hide the PRO chip on the Play-source-spoof row
    syncPairipLock();                           // show/hide the PRO chip on the Block-PairIP row
    syncTensorfeatLock();                       // show/hide the PRO chip on the Tensor-feature row
    syncWallpaperLock();                        // show/hide the PRO chip on the Wallpaper-id row
    syncAccountLock();                          // show/hide the PRO chip on the Google-account row
    syncSysServerGate();                         // grey + reboot-mark the system_server rows per the master switch
    syncPlaysrc();                             // reveal the "Also block PairIP" helper + mirror final state (after locks)
    syncMockLock();                             // show/hide the PRO chip on the Mock-hide row
    syncSensorLock();                           // show/hide the PRO chip on the Sensor row
    syncGetpropLock();                          // show/hide the PRO chip on the getprop row
    syncGpsLock();                              // show/hide the PRO chip on the GPS-spoof row
    syncCellLock(); syncCellInputs();           // cell-tower row lock + LAC/CI reveal
    syncGpsPick();                              // reveal the place-picker sub-row if GPS spoof is on
    syncUptimeLock();                            // show/hide the PRO chip on the Fake-Uptime row
    syncUptimeVal();                             // reveal the days input if Fake Uptime is on
    syncBootidLock();                            // show/hide the PRO chip on the Spoof-Boot-ID row
    syncBootidVal();                             // reveal churn switch + pinned-UUID field
    applyBootidMarker();                         // always the resident warn marker (inline hook only)
    syncMacLock();                               // Spoof-WiFi-MAC PRO chip
    syncMacVal();                                // reveal churn switch + pinned-MAC field
    syncBtmacLock();                             // Spoof-Bluetooth-MAC PRO chip
    syncBtmacVal();                              // reveal churn switch + pinned-BT-MAC field
    applyMacMarker();                            // always the resident warn marker (inline hook only)
    syncDrmLock();                              // show/hide the PRO chip on the DRM-spoof row
    syncImeiLock();                             // show/hide the PRO chip on the IMEI row + sub-row
    syncImsiLock();                             // show/hide the PRO chip on the IMSI row + sub-row
    syncIccidLock();                            // show/hide the PRO chip on the ICCID row + sub-row
    syncWebuaLock();                            // show/hide the PRO chip on the WebView-UA row
    syncWebfpLock();                            // show/hide the PRO chip on the canvas/WebGL row
    syncWebuaVal();                             // reveal the UA field if WebView-UA is on
    syncGaidLock();                             // show/hide the PRO chip on the GAID row
    syncGaidVal();                              // reveal the GAID literal/preview sub-row if GAID on
    syncAppSetLock();                           // show/hide the PRO chip on the App Set ID row
    syncAppSetVal();                            // reveal the App Set ID literal/preview sub-row if on
    syncSimLock();                              // show/hide the PRO chip on the SIM row
    syncSimPick();                              // reveal the mode+carrier sub-rows if SIM on
    syncProxyLock();                            // show/hide the PRO chip on the Per-App Proxy row
    syncProxyPick();                            // reveal the proxy picker sub-row if proxy on
    syncGroupsAutoOpen();                        // expand a section only if it has an active toggle
    openModal('packageModal');
  }

  function readPackageForm() {
    const raw = $m('#pfPackage').value.trim();
    const type = currentType();
    const deviceKey = $m('#pfDevice').dataset.deviceKey || null;
    return {
      pkg: raw, name: $m('#pfName').value, type, deviceKey,
      with_cpu: $m('#pfWithCpu').checked,
      blocked: $m('#pfBlock').checked,
      cow: $m('#pfCow').checked,
      aid: $m('#pfAid').checked,
      aidLiteral: $m('#pfAidVal').value.trim(),           // aid=<hex> literal (blank = derive from seed)
      serial: $m('#pfSerial').checked && !$m('#pfSerial').disabled,   // per-app serial (FREE); disabled = profile owns it
      serialLiteral: $m('#pfSerialVal').value.trim(),     // serial=<value> literal (blank = derive from package)
      gaid: $m('#pfGaid').checked,
      gaidLiteral: $m('#pfGaidVal').value.trim(),         // gaid=<uuid> literal (blank = derive from seed; PRO, resident)
      appset: $m('#pfAppSet').checked,
      appsetLiteral: $m('#pfAppSetVal').value.trim(),     // appset=<uuid> literal (blank = derive from package; PRO, resident)
      hideDev: $m('#pfHideDev').checked,                  // hidedev tag → hide USB-debugging + dev-options (PRO, stealth)
      randid: $m('#pfRandid').checked,                    // randid tag → fresh derived IDs (android_id/serial/gaid/appset/imei…) each launch (PRO, stealth)
      cpuModel: $m('#pfCpuModel').dataset.cpuKey || '',   // upsertPackage gates by type/with_cpu
      gpu: $m('#pfGpu').checked,
      gpuModel: $m('#pfGpuModel').dataset.gpuKey || '',   // upsertPackage gates by type (device)
      timezone: $m('#pfTzPick').dataset.tz || '',         // tz=<IANA zone> per-app timezone (FREE, stealth)
      locale: $m('#pfLangPick').dataset.lang || '',       // lang=<BCP-47> per-app locale (FREE, stealth, system-side)
      wallpaper: $m('#pfWallpaper').checked,              // wallpaper[=<id>] per-app wallpaper-id spoof (FREE, stealth, system-side)
      wallpaperId: ($m('#pfWallpaperId').value || '').replace(/[^0-9-]/g, ''),  // pinned id (int, '' = bare → -1)
      account: $m('#pfAccount').checked,                  // account[=<email>] per-app google-account spoof (FREE, stealth, system-side)
      accountEmail: (accountMode() === 'fake' ? ($m('#pfAccountEmail').value || '').trim() : ''),  // Hide → '' (bare tag); Fake → the email
      mock: $m('#pfMock').checked,                        // mock tag → resident Location.isMock hook (PRO)
      sensor: $m('#pfSensor').checked,                    // sensor tag → resident SystemSensorManager hook (PRO)
      getprop: $m('#pfGetprop').checked,                  // getprop tag → resident libc execve hook, child getprop returns spoofed props (PRO)
      gps: $m('#pfGps').checked,                          // gps=<lat>,<lng> tag → resident Location coord overwrite (PRO)
      gpsLat: $m('#pfGpsPick').dataset.gpsLat || '',      // fake latitude (decimal degrees), '' = off
      gpsLng: $m('#pfGpsPick').dataset.gpsLng || '',      // fake longitude (decimal degrees), '' = off
      cell: $m('#pfCell').checked,                        // cell=<lac>,<ci> tag → resident cell-tower overwrite (PRO)
      cellLac: ($m('#pfCellLac').value || '').replace(/[^0-9]/g, ''),  // area code (LAC/TAC), decimal only
      cellCi: ($m('#pfCellCi').value || '').replace(/[^0-9]/g, ''),    // cell id (CID/CI/NCI), decimal only
      cellMcc: ($m('#pfCellMcc').value || '').replace(/[^0-9]/g, ''),  // optional MCC (3 digits)
      cellMnc: ($m('#pfCellMnc').value || '').replace(/[^0-9]/g, ''),  // optional MNC (2-3 digits)
      uptime: $m('#pfUptime').checked,                    // uptime=<sec> tag → resident clock_gettime/sysinfo hook (PRO)
      uptimeSeconds: Math.round((parseFloat($m('#pfUptimeD').value) || 0) * 86400),   // days → seconds
      bootid: $m('#pfBootid').checked,                    // bootid / bootid=rnd / bootid=<uuid> → per-app boot_id spoof (PRO)
      bootidChurn: $m('#pfBootidChurn').checked,          // fresh UUID each launch (fake reboot)
      bootidLiteral: ($m('#pfBootidVal').value || '').trim(),   // pinned UUID, '' = derive/churn
      mac: $m('#pfMac').checked,                          // mac / mac=rnd / mac=<12hex> → per-app WiFi MAC spoof (PRO)
      macChurn: $m('#pfMacChurn').checked,                // fresh MAC each launch
      macLiteral: ($m('#pfMacVal').value || '').trim(),   // pinned MAC (setMac strips ':'/'-' → 12hex), '' = derive/churn
      btmac: $m('#pfBtmac').checked,                      // btmac / btmac=rnd / btmac=<12hex> → per-app Bluetooth MAC spoof (PRO, stealth)
      btmacChurn: $m('#pfBtmacChurn').checked,            // fresh BT MAC each launch
      btmacLiteral: ($m('#pfBtmacVal').value || '').trim(), // pinned BT MAC (setBtmac strips ':'/'-' → 12hex), '' = derive/churn
      rr: $m('#pfRr').checked,                            // rr=<hz> tag → resident DisplayManagerGlobal hook (FREE)
      refreshHz: parseFloat($m('#pfRrHz').value) || 0,    // spoofed display refresh rate (Hz)
      drm: $m('#pfDrm').checked,                          // drm tag → resident MediaDrm identity spoof (PRO)
      drmLevel: drmLevel(),                               // drm=<level> securityLevel L1/L2/L3 (L1 = bare :drm)
      drmId: drmIdFieldToHex($m('#pfDrmIdVal').value),    // field (B64 or hex per toggle) → canonical hex for the drmid= tag (blank/invalid = derive)
      drmSys: $m('#pfDrmSysVal').value.trim(),            // drmsys=<int> literal systemId (blank = derive)
      imei: $m('#pfImei').checked,                        // imei tag → resident TelephonyManager IMEI spoof (PRO)
      imeiLiteral: $m('#pfImeiVal').value.trim(),         // imei=<15> literal slot 0 (blank = derive per-app)
      imei2Literal: $m('#pfImei2Val').value.trim(),       // imei2=<15> literal slot 1 / dual-SIM (blank = derive)
      imsi: $m('#pfImsi').checked,                        // imsi tag → resident getSubscriberId spoof (PRO)
      imsiLiteral: $m('#pfImsiVal').value.trim(),         // imsi=<14-15> literal slot 0 (blank = derive)
      imsi2Literal: $m('#pfImsi2Val').value.trim(),       // imsi2=<14-15> literal slot 1 / dual-SIM (blank = derive)
      iccid: $m('#pfIccid').checked,                      // iccid tag → resident getSimSerialNumber spoof (PRO)
      iccidLiteral: $m('#pfIccidVal').value.trim(),       // iccid=<19-20> literal slot 0 (blank = derive)
      iccid2Literal: $m('#pfIccid2Val').value.trim(),     // iccid2=<19-20> literal slot 1 / dual-SIM (blank = derive)
      webua: $m('#pfWebua').checked ? ($m('#pfWebuaPick').dataset.uaKey || '') : '',  // ua=<key> WebView UA profile
      webfp: $m('#pfWebfp').checked,                      // webfp → canvas fp spoof (WebView JS-shim)
      webfpSeed: $m('#pfWebfpVal') ? $m('#pfWebfpVal').value : '',  // webfp=<seed> pinned/randomized, '' = derive
      vpns: $m('#pfVpn').checked,                         // main Hide VPN → :vpns (system_server, pairip-safe, reboot to apply)
      vpnAgg: $m('#pfVpnAgg').checked,                    // aggressive → :vpnn (resident in-app ART + native; risk-gated)
      playsrc: $m('#pfPlaysrc').checked,                 // Fake Play install source → :playsrc (system_server, PRO, stealth-from-app)
      pairip: $m('#pfPairip').checked,                   // Block PairIP license check → :pairip (independent; system_server, PRO, stealth-from-app, NOT detectable)
      tensorfeat: $m('#pfTensorfeat').checked,           // Tensor feature targets → :tensorfeat (system_server, PRO, stealth-from-app)
      sim: $m('#pfSim').checked ? simMode() : '',          // '' | 'safe' | 'aggressive' | 'phantom'
      // phantom (Add-SIM) carries its single carrier on #pfSimFakeModel → simS0; else per-slot.
      simS0: ($m('#pfSim').checked && simMode() === 'phantom')
               ? ($m('#pfSimFakeModel').dataset.simKey || '')
               : ($m('#pfSim1Model').dataset.simKey || ''),  // SIM1 / added-SIM carrier key ('' = real)
      simS1: ($m('#pfSim').checked && simMode() === 'phantom')
               ? ''
               : ($m('#pfSim2Model').dataset.simKey || ''),  // SIM2 carrier key ('' = real)
      proxy: $m('#pfProxy').checked ? ($m('#pfProxyModel').dataset.proxyKey || '') : '',  // proxy=<key>, '' = off
      dnd: $m('#pfDnd').checked,
      dab: $m('#pfDab').checked,
      kso: $m('#pfKso').checked,
      nolog: $m('#pfNolog').checked,
      dpi: $m('#pfDpi').value.trim(),                     // dpi=<n> per-app screen density (controller tweak)
      userScope: pkgUserScope,                            // u=<id> per-user scope ('' = all users), from the picker
    };
  }

  // ─── Bulk edit ("Edit together") ───────────────────────────────────────────
  // Open the package modal over a set of selected packages. It opens blank; a snapshot
  // of that blank form is the baseline. On save, only the fields the user actually changed
  // from the baseline are overlaid onto each selected package (every untouched tag is kept),
  // so one sweep can e.g. turn cow+aid on for a whole set of clones without disturbing the rest.
  function openPackageBatch(items) {
    if (!items || !items.length) return;
    openPackage(null);                                  // full reset + open (clears batch state first)
    batchItems = items.slice();
    $m('#packageModalTitle').textContent = I18N.t('pkg_batch_title').replace('%d', items.length);
    $m('#packageForm').classList.add('is-batch');
    const pf = $m('#pfPackage').closest('.field'), nf = $m('#pfName').closest('.field');
    if (pf) pf.style.display = 'none';
    if (nf) nf.style.display = 'none';
    const hint = $m('#pfBatchHint'); if (hint) hint.hidden = false;
    setPkgUserScope('');                                // identity stays per-app; hidden in batch
    setDeviceSelection(null);                           // no default device → only an explicit pick counts as a change
    batchBaseline = readPackageForm();                  // snapshot the blank form
  }

  async function batchSave(cur) {
    const items = batchItems; batchItems = null;
    const base = batchBaseline || {};
    const changed = {};
    Object.keys(cur).forEach(k => { if (cur[k] !== base[k]) changed[k] = cur[k]; });
    // package/display-name/user-scope are per-app identity — never bulk-applied
    delete changed.pkg; delete changed.name; delete changed.userScope;
    // CPU spoof vs Block CPU are mutually exclusive (native: with_cpu wins). Keep the one the
    // user actually touched authoritative across the batch.
    if (changed.blocked === true) { changed.with_cpu = false; changed.cpuModel = ''; }
    if (changed.with_cpu === true || 'cpuModel' in changed) { changed.blocked = false; }
    if (!Object.keys(changed).length) { closeAll(); showToast(I18N.t('sel_batch_none')); return; }
    // Each row already carries its full current settings (listPackages shape == upsertPackage form),
    // so {...row, pkg:clean} round-trips it; the overlay changes only the touched keys.
    items.forEach(it => {
      const merged = { ...it, pkg: it.clean, ...changed };
      COPG.upsertPackage(merged, { clean: it.clean, type: it.type, deviceKey: it.deviceKey, userScope: it.userScope });
    });
    closeAll();
    await w.LibRefresh(I18N.t('sel_batch_done').replace('%d', items.length));
  }

  $m('#packageForm').addEventListener('submit', async e => {
    e.preventDefault();
    const form = e.currentTarget;
    clearErrors(form);
    const errBox = $m('#pfError'); errBox.hidden = true; errBox.textContent = '';

    const pkgEl = $m('#pfPackage');
    const raw = pkgEl.value.trim();
    const type = currentType();
    const devSel = $m('#pfDevice');
    const deviceKey = devSel.dataset.deviceKey || null;

    let bad = false;
    if (!raw) { markError(pkgEl, I18N.t('msg_required')); bad = true; }
    if (type === 'device' && !deviceKey) { markError(devSel, I18N.t('msg_pick_device')); bad = true; }
    if (bad) return;

    const cleanName = COPG.clean(raw);
    const userScope = pkgUserScope;
    // cross-config duplicate — scoped to THIS user, so the same package for a DIFFERENT user
    // on another device is allowed (the whole point of per-user spoof), not a clash.
    const where = COPG.locate(cleanName, editingPkg ? editingPkg.clean : null, userScope);
    if (where) {
      errBox.textContent = I18N.t('msg_dup_pkg') + ' ' + where;
      errBox.hidden = false;
      markError(pkgEl, I18N.t('msg_duplicate'));
      return;
    }

    const formData = readPackageForm();
    if (batchItems) { await batchSave(formData); return; }
    COPG.upsertPackage(formData, editingPkg);

    closeAll();
    await w.LibRefresh(I18N.t(editingPkg ? 'toast_package_saved' : 'toast_package_added'));
  });

  /* ════════════ CONFIRM DELETE ════════════ */
  let confirmCb = null;
  let confirmAltCb = null;       // optional third-button callback (e.g. import Keep vs Move)
  let confirmTimer = null;       // GPU-risk-style cooldown ticker on the OK button
  let confirmRiskKey = null;     // when set, the "don't show again" row is shown for this spoof

  // Per-spoof "don't show this risk warning again" acknowledgements, persisted like the other
  // copg-* prefs. Keyed by spoof (gpu/mock/drm/imei/gaid/appset/vpn/sim); once acked, that
  // spoof's red risk gate is skipped and its onOk runs straight away.
  const RISK_ACK_KEY = 'copg-risk-ack';
  function riskAckMap() { try { return JSON.parse(localStorage.getItem(RISK_ACK_KEY) || '{}') || {}; } catch (_) { return {}; } }
  function riskAcked(k) { return !!riskAckMap()[k]; }
  function setRiskAck(k) { try { const m = riskAckMap(); m[k] = 1; localStorage.setItem(RISK_ACK_KEY, JSON.stringify(m)); } catch (_) {} }
  // Risk gate that remembers acceptance: if the user already ticked "don't show again" for this
  // spoof, run the accept path immediately; otherwise show the normal confirm carrying riskKey so
  // the confirm sheet offers the checkbox.
  function riskConfirm(key, opts) {
    if (riskAcked(key)) { if (opts.onOk) opts.onOk(); return; }
    confirm(Object.assign({}, opts, { riskKey: key }));
  }
  /* Generic confirm sheet. `danger` paints the OK button red (delete);
     otherwise it's the primary accent (restore, and any future confirm). */
  function confirm(opts) {
    $m('#confirmTitle').textContent = opts.title || '';
    $m('#confirmMsg').textContent   = opts.message || '';
    // optional clickable link (e.g. the community group for the GOT warning) —
    // confirmMsg is textContent so it can't hold an anchor; this row carries it.
    const link = $m('#confirmLink');
    if (opts.link && opts.link.href) {
      link.href = opts.link.href;
      link.textContent = opts.link.label || opts.link.href;
      link.hidden = false;
      // On device, route through an Android VIEW intent so a t.me link opens in the
      // Telegram app instead of the in-WebView browser. (onclick = idempotent.)
      link.onclick = e => {
        if (COPG.hasBridge && COPG.hasBridge()) { e.preventDefault(); COPG.openExternal(link.href); }
      };
    } else { link.hidden = true; link.removeAttribute('href'); link.onclick = null; }
    // "Don't show again" row — only for risk gates (opts.riskKey), reset unticked each open.
    confirmRiskKey = opts.riskKey || null;
    const dsaRow = $m('#confirmDsaRow'), dsa = $m('#confirmDsa');
    if (dsaRow) { dsaRow.hidden = !confirmRiskKey; if (dsa) dsa.checked = false; }
    // info-only: a single acknowledge button, no Cancel (pure explanation sheet)
    $m('#confirmCancel').hidden = !!opts.infoOnly;
    const ok = $m('#confirmOk');
    ok.textContent = opts.okLabel || I18N.t('btn_save');
    ok.removeAttribute('data-i18n');           // set explicitly each open → no stale i18n revert
    ok.classList.toggle('btn--danger',  !!opts.danger);
    ok.classList.toggle('btn--primary', !opts.danger);
    confirmCb = opts.onOk || null;
    // optional third button (a second affordance beside OK/Cancel) — e.g. Move here
    // vs Keep on existing. Stacks the buttons vertically so three fit on a phone.
    const alt = $m('#confirmAlt');
    confirmAltCb = opts.onAlt || null;
    if (opts.altLabel) { alt.hidden = false; alt.textContent = opts.altLabel; }
    else { alt.hidden = true; alt.textContent = ''; }
    $m('#confirmBtns').classList.toggle('form-buttons--stack', !!opts.altLabel);
    // optional cooldown: disable OK and count down on its label so the user is forced
    // to sit with the warning before accepting (e.g. 5s on the GPU risk gate).
    clearConfirmCountdown();
    const secs = parseInt(opts.countdown, 10) || 0;
    if (secs > 0) {
      const base = ok.textContent;
      let n = secs;
      ok.disabled = true;
      ok.textContent = `${base} (${n})`;
      confirmTimer = setInterval(() => {
        n--;
        if (n <= 0) { clearConfirmCountdown(); ok.disabled = false; ok.textContent = base; }
        else ok.textContent = `${base} (${n})`;
      }, 1000);
    } else {
      ok.disabled = false;
    }
    openModal('confirmModal');
  }
  function clearConfirmCountdown() {
    if (confirmTimer) { clearInterval(confirmTimer); confirmTimer = null; }
  }
  function confirmDelete(title, message, cb) {
    confirm({ title, message, okLabel: I18N.t('btn_delete'), danger: true, onOk: cb });
  }
  /* Plain-language explanation sheet (reuses the confirm sheet, no Cancel). */
  function info(opts) {
    confirm({ title: opts.title, message: opts.message, okLabel: opts.okLabel || I18N.t('info_ok'), infoOnly: true });
  }
  $m('#confirmOk').addEventListener('click', async () => {
    if ($m('#confirmOk').disabled) return;       // cooldown still running
    clearConfirmCountdown();
    // Persist "don't show again" before running the callback, so this spoof's gate is skipped next time.
    if (confirmRiskKey && $m('#confirmDsa') && $m('#confirmDsa').checked) setRiskAck(confirmRiskKey);
    confirmRiskKey = null;
    const cb = confirmCb; confirmCb = null;
    closeModal('confirmModal');                 // close only the confirm, not the sheet underneath
    if (cb) await cb();                          // callback decides whether to close the rest
  });
  $m('#confirmAlt').addEventListener('click', async () => {
    clearConfirmCountdown();
    const cb = confirmAltCb; confirmAltCb = null; confirmCb = null;
    closeModal('confirmModal');
    if (cb) await cb();
  });
  // Cancel / dismiss → stop any running cooldown and re-enable OK for the next open.
  $m('#confirmCancel').addEventListener('click', () => { clearConfirmCountdown(); confirmRiskKey = null; $m('#confirmOk').disabled = false; });

  /* Pick from installed apps → fills package + display name */
  $m('#pfPickBtn').addEventListener('click', () => {
    if (!w.AppPicker || typeof w.AppPicker.open !== 'function') {
      // warn level → visible even with Debug Logs off, so a missing picker is obvious
      if (w.logEvent) logEvent('App picker unavailable (AppPicker=' + (typeof w.AppPicker) + ').', 'warn');
      showToast(I18N.t('toast_picker_unavailable')); return;
    }
    w.AppPicker.open(app => {
      $m('#pfPackage').value = app.pkg;
      if (app.label) $m('#pfName').value = app.label;
      $m('#pfPackage').closest('.field')?.classList.remove('field--error');
      // Pre-scope to the user the app was picked under (owner/all → '', a clone → its id) and
      // show the read-only tag, then refresh the per-user id previews so the ⓘ placeholders match.
      if (app.userScope !== undefined) {
        setPkgUserScope(app.userScope);
        syncAidVal(); syncSerialLock(); syncGaidVal(); syncAppSetVal(); syncDrmVal(); syncImeiVal(); syncImsiVal(); syncIccidVal();
      }
    });
  });

  /* Keep the focused field above the on-screen keyboard.
     The sheet is position:fixed; bottom:0, so by default the soft keyboard
     covers the lower fields (the WebView uses resizes-visual — layout viewport
     doesn't shrink). Track visualViewport: lift the sheet by the keyboard height
     (--kb-inset) and cap the form sheet to the visible area (--sheet-vh) so its
     header stays on-screen; then scrollIntoView can reach any field. */
  (function setupKeyboardInset() {
    const vv = window.visualViewport;
    if (!vv) return;
    const root = document.documentElement.style;
    const apply = () => {
      const kb = Math.max(0, Math.round(window.innerHeight - vv.height - vv.offsetTop));
      root.setProperty('--kb-inset', kb + 'px');
      if (kb > 80) root.setProperty('--sheet-vh', Math.round(vv.height - 12) + 'px');
      else root.removeProperty('--sheet-vh');
    };
    vv.addEventListener('resize', apply);
    vv.addEventListener('scroll', apply);
    apply();
  })();
  ['#deviceForm', '#packageForm'].forEach(sel => {
    const form = $m(sel);
    form?.addEventListener('focusin', e => {
      const t = e.target;
      if (t.matches('input, select, textarea')) {
        setTimeout(() => t.scrollIntoView({ block: 'center', behavior: 'smooth' }), 300);
      }
    });
  });
  // Picker add/edit panels (GPU/CPU) are injected by their own scripts AFTER this
  // file, so delegate on document: any input inside a .picker-import__panel gets
  // centered in its scroll area once the keyboard is up (mirrors the form sheets).
  document.addEventListener('focusin', e => {
    const t = e.target;
    if (t.matches && t.matches('.picker-import__panel input')) {
      setTimeout(() => t.scrollIntoView({ block: 'center', behavior: 'smooth' }), 300);
    }
  });

  /* init */
  wireAndroidSdk();
  wireSerialGen();
  wireSdkWarn();
  wireAdvToggle();
  wireToggleGroups();
  wireAdvInfo();
  wirePropsEditor();
  wireGen('dfSecPatchGen',    'dfSecPatch',    genSecPatch);
  wireGen('dfIncrementalGen', 'dfIncremental', genIncremental);
  wireGen('dfRadioGen',       'dfRadio',       genRadio);

  w.Modals = { openDevice, openDeviceShare, openPackage, openPackageBatch, confirm, confirmDelete, info, closeAll };

})(window);
