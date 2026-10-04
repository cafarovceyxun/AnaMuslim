// Line-art illustration engine (style B, v2). Organic contours, thick outer lines, thin details,
// faint tints. Faces carry only very soft hints (no eyes or mouth). Returns SVG strings only.
var ILLX = (function () {
  // ---- Hand: side view, pointing +x, wrist at origin, thumb toward -y. Separate fingers. ----
  var HAND =
    '<path class="sk" d="M18 -11 C22 -20 30 -27 39 -28 C45 -28.6 48 -25 46 -21 C44 -17.5 38 -15 32 -12.5"/>' +
    '<path class="dt" d="M41 -26.5 C44 -25.5 45 -23.5 44.5 -21.5"/>' +
    '<path class="sk" d="M40 9 C50 10 60 12 66 14.5 C70 16.5 69 19.5 64.5 19.5 C56 19.5 47 18 38 15.5"/>' +
    '<path class="sk" d="M44 3 C56 4 68 5.5 75 8 C79.5 9.8 78.5 13.4 73.5 13.4 C64 13.4 53 12 42 10"/>' +
    '<path class="sk" d="M0 -14 C12 -16.5 26 -18 38 -17 C52 -16 66 -13.5 77 -10.5 C83 -8.8 85.5 -5.5 83.5 -2.6 C81.5 0.2 76 0.6 70 0.2 L56 0 C62 1.2 70 2.4 76.5 4 C81 5.3 81 8.8 76 9 C66 9.4 54 8.8 44 8 C34 11 22 13.5 0 14 Z"/>' +
    '<path class="dt" d="M38 -16 C40 -13.5 40.5 -11 39.5 -8.5 M78 -9.2 C80.5 -8.4 82 -6.6 81.6 -4.4 M22 6 C30 6.5 38 6 44 4.5"/>';
  var SLEEVE =
    '<path class="cl" d="M-56 -21 C-38 -24 -16 -22 2 -17.5 C4.5 -6 4.5 6 2 17.5 C-16 21.5 -38 23 -56 21"/>' +
    '<path class="cdt" d="M-4 -16.5 C-2.6 -6 -2.6 6 -4 16.5 M-30 -21.5 C-27 -10 -29 6 -26 21.5 M-44 -22 C-42 -14 -43 -6 -41 0"/>';
  function hand(x, y, rot, s, flip, sleeve) {
    s = s || 1;
    return '<g transform="translate(' + x + ' ' + y + ') rotate(' + rot + ') scale(' + s + ' ' + (flip ? -s : s) + ')">' +
      (sleeve === false ? '' : SLEEVE) + HAND + '</g>';
  }
  function drop(x, y, r) {
    r = r || 3;
    return '<path class="wa" d="M' + x + ' ' + (y - r * 1.9) + ' C' + (x + r * .4) + ' ' + (y - r) + ' ' + (x + r) + ' ' + (y - r * .2) + ' ' + (x + r) + ' ' + (y + r * .4) +
      ' C' + (x + r) + ' ' + (y + r * 1.1) + ' ' + (x + r * .5) + ' ' + (y + r * 1.4) + ' ' + x + ' ' + (y + r * 1.4) +
      ' C' + (x - r * .5) + ' ' + (y + r * 1.4) + ' ' + (x - r) + ' ' + (y + r * 1.1) + ' ' + (x - r) + ' ' + (y + r * .4) +
      ' C' + (x - r) + ' ' + (y - r * .2) + ' ' + (x - r * .4) + ' ' + (y - r) + ' ' + x + ' ' + (y - r * 1.9) + ' Z"/>';
  }
  function drops(pts) { return pts.map(function (p) { return drop(p[0], p[1], p[2]); }).join(''); }
  function dust(pts) { return pts.map(function (p) { return '<circle class="du" cx="' + p[0] + '" cy="' + p[1] + '" r="' + (p[2] || 2.2) + '"/>'; }).join(''); }
  function stream(d, w) {
    return '<path class="wb" d="' + d + '" stroke-width="' + (w || 6) + '"/><path class="wh" d="' + d + '" stroke-width="1.2"/>';
  }
  function head(x, y, ang) {
    var a = ang * Math.PI / 180, l = 8;
    return '<path class="mv" d="M' + (x - l * Math.cos(a - .5)).toFixed(1) + ' ' + (y - l * Math.sin(a - .5)).toFixed(1) + ' L' + x + ' ' + y +
      ' L' + (x - l * Math.cos(a + .5)).toFixed(1) + ' ' + (y - l * Math.sin(a + .5)).toFixed(1) + '"/>';
  }
  function svg(inner, h) {
    h = h || 170;
    return '<svg class="ill" viewBox="0 0 240 ' + h + '" aria-hidden="true"><rect class="bgc" width="240" height="' + h + '" rx="16"/>' + inner + '</svg>';
  }
  function ewer(x, y, s) {
    return '<g transform="translate(' + x + ' ' + y + ') scale(' + (s || 1) + ')">' +
      '<path class="mtd" d="M-11 -33 C-31 -32 -31 -7 -13 -6"/>' +
      '<path class="mt" d="M12 -20 C25 -24 37 -36 47 -51 C49 -53 52 -52 52 -49 C46 -31 33 -14 14 -8"/>' +
      '<path class="mt" d="M0 -50 C9 -50 12 -43 10.5 -37 C22 -29 25 -11 14 -2.5 C6 0 -6 0 -14 -2.5 C-25 -11 -22 -29 -10.5 -37 C-12 -43 -9 -50 0 -50 Z"/>' +
      '<path class="mtl" d="M-17 -26 C-8 -22 8 -22 17 -26 M-19 -16 C-8 -12 8 -12 19 -16 M-6 -24 L-4 -18 M0 -23 L0 -17 M6 -24 L4 -18"/>' +
      '<path class="mt" d="M-9 -51 C-6 -54 6 -54 9 -51 C6 -49 -6 -49 -9 -51 Z"/><circle class="mt" cx="0" cy="-57" r="3"/></g>';
  }

  var THOBE_SIDE = '<path class="cl" d="M40 170 C45 153 62 145 84 144 C96 147 108 148 122 145 C146 146 165 155 174 170"/>' +
    '<path class="cdt" d="M86 145 C93 153 112 154 121 146 M104 152 L104 170 M60 156 C62 162 62 166 61 170 M150 154 C152 160 152 165 151 170"/>';
  var PROFILE_HEAD =
    '<path class="sk" d="M86 116 C88 126 88 136 86 146 L110 146 C108 140 108 135 110 130"/>' +
    '<path class="sk" d="M86 118 C72 106 68 86 72 68 C76 46 92 30 114 28 C136 27 150 40 154 56 C155 62 154 66 156 70 C158 74 160 78 163 84 C164 87 162 89 158 89.5 C157.5 92 157.5 94 158.5 96.5 C160 100 157 102 156 103 C158 112 154 122 148 128 C140 135 128 139 116 138 C108 137 100 132 96 126 C92 122 89 120 86 118 Z"/>' +
    '<path class="hr" d="M156 103 C158 112 154 122 148 128 C140 135 128 139 116 138 C108 137 100 132 96 126 C100 118 102 112 101 104 C110 108 122 110 134 108 C142 106 150 104 156 103 Z"/>' +
    '<path class="dt" d="M114 116 C116 121 117 125 116 130 M126 116 C128 122 128 126 127 132 M138 114 C140 119 140 124 138 129 M106 112 C107 117 107 121 105 125"/>' +
    '<path class="sk" d="M98 74 C91 73.5 89 82 91 88 C93 94 97 96.5 101 93"/><path class="dt" d="M96 79 C93.5 83 94 88 97.5 90.5"/>' +
    '<path class="soft" d="M148 66 C151 64.5 154 65 156 67"/>';
  var KUFI = '<path class="cl" d="M73 66 C72 42 93 26 117 26 C137 26 151 38 155 54 C136 49 98 50 73 66 Z"/>' +
    '<path class="stitch" d="M80 58 C98 46 128 42 150 48 M86 46 C102 37 126 34 144 40 M98 34 C110 30 124 30 134 32"/>';
  var HAIR = '<path class="hr" d="M72 70 C70 44 92 27 116 26 C136 26 150 37 155 53 C147 52 141 51 134 52 C118 50 100 56 88 66 C84 70 82 76 82 82 C78 80 74 76 72 70 Z"/>' +
    '<path class="dt" d="M96 36 C108 31 122 30 134 34 M90 46 C104 39 122 37 140 42"/>';
  var PROFILE = THOBE_SIDE + PROFILE_HEAD;

  var FRONT_BODY = '<path class="cl" d="M40 170 C46 140 78 128 104 126 C112 130 128 130 136 126 C162 128 194 140 200 170"/>' +
    '<path class="cdt" d="M104 126 C108 138 132 138 136 126 M120 136 L120 170 M64 146 C66 156 66 164 64 170 M176 146 C174 156 174 164 176 170"/>' +
    '<path class="sk" d="M108 104 C109 114 108 122 104 128 C112 132 128 132 136 128 C132 122 131 114 132 104"/>' +
    '<path class="sk" d="M89 70 C83 68 82 78 84 84 C86 90 90 92 92 88"/><path class="sk" d="M151 70 C157 68 158 78 156 84 C154 90 150 92 148 88"/>' +
    '<path class="dt" d="M87 75 C85.5 79 86.5 84 89 86.5 M153 75 C154.5 79 153.5 84 151 86.5"/>' +
    '<path class="sk" d="M88 68 C88 42 102 28 120 28 C138 28 152 42 152 68 C152 86 146 100 136 108 C130 113 125 115 120 115 C115 115 110 113 104 108 C94 100 88 86 88 68 Z"/>' +
    '<path class="hr" d="M90 80 C92 98 102 114 120 118 C138 114 148 98 150 80 C146 94 136 104 120 105 C104 104 94 94 90 80 Z"/>' +
    '<path class="soft" d="M108 66 C112 64.5 116 65 118 66.5 M122 66.5 C124 65 128 64.5 132 66 M120 72 C119.5 78 118.5 83 121 85"/>';
  var FRONT_KUFI = '<path class="cl" d="M88 56 C87 34 102 23 120 23 C138 23 153 34 152 56 C140 48 100 48 88 56 Z"/>' +
    '<path class="stitch" d="M93 47 C108 40 132 40 147 47 M98 37 C110 32 130 32 142 37"/>';
  var FRONT_HAIR = '<path class="hr" d="M88 60 C86 36 102 24 120 24 C138 24 154 36 152 60 C144 50 132 46 120 46 C108 46 96 50 88 60 Z"/>';

  var I = {};
  I.hands = function () {
    return svg('<path class="shl" d="M44 160 C90 157 160 157 204 160"/>' + ewer(40, 76, .95) +
      stream('M88 28 C100 40 110 62 116 92') +
      hand(44, 116, -6, 1) + hand(206, 96, 186, 1, true) +
      drops([[110, 134], [124, 146, 2.6], [98, 148, 2.4], [140, 132, 2.2], [150, 146, 2]]) +
      '<path class="mv" d="M92 76 C104 66 120 64 132 68"/>' + head(132, 68, 15) +
      '<path class="mv" d="M168 128 C156 138 140 140 128 136"/>' + head(128, 136, 195));
  };
  I.mouth = function () {
    return svg(PROFILE + KUFI + hand(208, 164, -131, .84, true) +
      '<path class="wa" d="M160 122 C164 116 172 111 178 110 C177 116 170 121 162 124 Z"/>' +
      drops([[170, 98, 2.2], [176, 106, 1.8]]) +
      '<circle class="hi" cx="152" cy="110" r="10"/><circle class="hi" cx="161" cy="86" r="8"/>' +
      '<text x="172" y="82" class="p">burun</text><text x="140" y="99" class="p" text-anchor="end">ağız</text>');
  };
  I.face = function () {
    return svg(FRONT_BODY + FRONT_KUFI + '<path class="hi" d="M96 66 C96 48 106 40 120 40 C134 40 144 48 144 66 C144 86 134 100 120 100 C106 100 96 86 96 66 Z"/>' +
      '<path class="cl" d="M64 170 C68 162 74 156 82 152 L100 158 C98 162 96 166 95 170"/><path class="cl" d="M176 170 C172 162 166 156 158 152 L140 158 C142 162 144 166 145 170"/>' +
      '<path class="sk" d="M82 156 C78 144 84 136 94 133 C102 131 112 132 119 137 L120 151 C112 158 96 162 82 156 Z"/>' +
      '<path class="sk" d="M158 156 C162 144 156 136 146 133 C138 131 128 132 121 137 L120 151 C128 158 144 162 158 156 Z"/>' +
      '<path class="dt" d="M88 138 C88 135 91 134 93 136 C93 133 97 132 99 134.5 C100 132 104 131.5 106 134 M152 138 C152 135 149 134 147 136 C147 133 143 132 141 134.5 C140 132 136 131.5 134 134"/>' +
      '<path class="wa" d="M104 139 C110 136 130 136 136 139 C130 143 110 143 104 139 Z"/>' +
      '<path class="mv" d="M98 126 C93 116 93 106 96 96"/>' + head(96, 96, -75) +
      '<path class="mv" d="M142 126 C147 116 147 106 144 96"/>' + head(144, 96, -105) +
      drops([[106, 90, 2], [134, 92, 2], [120, 124, 1.8]]));
  };
  I.faceWipe = function () {
    return svg(FRONT_BODY + FRONT_KUFI +
      '<g transform="translate(101 112) rotate(-96) scale(.52)">' + HAND + '</g>' +
      '<g transform="translate(139 112) rotate(-84) scale(.52 -.52)">' + HAND + '</g>' +
      '<path class="mv" d="M76 52 C74 70 74 88 76 104"/>' + head(76, 104, 92) + '<path class="mv" d="M164 52 C166 70 166 88 164 104"/>' + head(164, 104, 88) +
      dust([[80, 40], [160, 38], [120, 122, 1.8], [102, 120, 1.6], [138, 120, 1.6]]));
  };
  I.arm = function () {
    return svg('<path class="sk" d="M10 24 C20 16 30 10 38 6 C52 26 70 58 86 90 C80 98 70 106 60 110 C44 82 26 50 10 24 Z"/>' +
      '<path class="cl" d="M4 22 C16 14 28 6 40 0 C45 7 50 15 54 22 C42 29 30 36 18 44 C13 37 8 29 4 22 Z"/>' +
      '<path class="cdt" d="M10 31 C22 24 34 16 46 9 M14 38 C26 31 38 23 50 16"/>' +
      '<path class="sk" d="M62 112 C54 106 56 92 66 88 C84 84 112 88 140 92 C152 94 162 95 172 96 C173 102 173 108 172 113 C160 113 148 113 136 114 C112 115 82 118 62 112 Z"/>' +
      '<path class="dt" d="M70 98 C74 96 77 97 79 100 M96 92 C112 92 128 94 140 96"/>' +
      hand(172, 104.5, 2, .78, false, false) +
      hand(222, 62, 158, .76, true) +
      '<circle class="hi" cx="71" cy="102" r="24"/><text x="44" y="146" class="p">dirsək daxil</text>' +
      stream('M120 0 C122 30 124 60 126 86', 5) +
      drops([[110, 124, 2.2], [100, 136, 2], [232, 120, 2], [226, 132, 1.8]]));
  };
  I.head = function () {
    return svg(THOBE_SIDE + PROFILE_HEAD + HAIR + hand(40, 42, 14, .9) +
      '<path class="mv" d="M66 12 C94 0 128 2 152 18"/>' + head(152, 18, 30) + head(66, 12, 200) +
      '<text x="164" y="36" class="p">önə · arxaya</text>');
  };
  I.foot = function () {
    return svg('<path class="shl" d="M50 168 C100 166 170 166 220 168"/>' +
      '<path class="cl" d="M40 0 C70 2 104 2 132 0 C133 18 134 36 134 52 C104 56 70 56 38 52 C38 36 39 18 40 0 Z"/><path class="cdt" d="M40 44 C70 47 104 47 133 44 M70 0 C71 18 70 34 72 52 M104 0 C103 18 104 34 102 54"/>' +
      '<path class="sk" d="M70 52 C70 76 69 96 66 112 C62 128 58 142 60 154 C62 164 70 168 80 168 L150 168 C164 168 176 168 186 168 C196 168 205 167 206 160 C207 154 200 150 190 147 C176 143 156 139 140 134 C124 129 112 122 107 110 C104 100 104 76 104 52"/>' +
      '<path class="sk" d="M188 150 C194 149 198 152 198 157 M178 147 C184 146 188 149 188 154 M168 145 C173 144 177 147 177 151"/>' +
      '<path class="dt" d="M95 106 C99 102 104 103 105 108 M120 150 C140 156 160 158 180 158"/>' +
      '<circle class="hi" cx="70" cy="150" r="22"/><text x="12" y="124" class="p">daban</text>' +
      stream('M18 64 C32 86 46 110 58 136', 5) + drops([[44, 150, 2.2], [34, 160, 1.8], [150, 126, 2]]));
  };
  I.strike = function () {
    return svg('<path class="ea" d="M-4 134 C40 130 80 136 120 132 C160 128 200 134 244 131 L244 176 L-4 176 Z"/>' +
      '<path class="eat" d="M14 148 h7 M42 158 h5 M88 152 h8 M170 160 h7 M206 146 h5 M128 164 h6 M60 146 l3 -1 M190 154 l4 1"/>' +
      hand(46, 106, 9, 1) + hand(26, 120, 6, 1) +
      dust([[102, 128], [114, 124], [124, 130], [132, 126, 1.8], [92, 130, 1.8], [140, 130, 1.6], [118, 118, 1.5]]) +
      '<path class="mv" d="M74 62 L76 82 M100 56 L100 78 M126 62 L124 84"/>');
  };
  I.blow = function () {
    return svg(PROFILE + KUFI + hand(212, 166, -98, .82, false, true) + hand(232, 160, -104, .78, true, true) +
      '<path class="air" d="M164 106 C176 104 186 100 196 96 M162 114 C174 114 184 112 196 108"/>' +
      dust([[214, 66], [224, 58, 1.8], [230, 72], [210, 54, 1.6]]));
  };
  I.wipeHands = function () {
    return svg(hand(28, 112, 0, 1.05) + hand(226, 94, 180, .95, true) +
      '<path class="mv" d="M206 62 C170 56 130 58 92 68"/>' + head(92, 68, 168) +
      dust([[140, 104], [152, 96, 1.8], [120, 100, 1.6]]) + '<text x="62" y="150" class="p">barmaqlardan biləyə</text>');
  };
  I.pour = function () {
    return svg(THOBE_SIDE + PROFILE_HEAD + HAIR + hand(216, 8, 166, .72, true) +
      '<path class="wa" d="M150 26 C156 21 164 20 170 22 C166 27 158 29 151 29 Z"/>' +
      stream('M150 30 C140 32 130 30 122 30', 4) + drops([[92, 58, 2.2], [80, 82, 2], [150, 58, 1.8]]) +
      '<text x="20" y="30" class="p">3 ovuc</text>');
  };
  I.sides = function () {
    return svg(FRONT_BODY + FRONT_HAIR +
      stream('M100 0 C99 16 99 28 101 42', 4) + stream('M140 0 C141 16 141 28 139 42', 4) + stream('M120 0 C120 10 120 18 120 28', 4) +
      '<circle class="num" cx="80" cy="18" r="10"/><text x="80" y="22" text-anchor="middle" class="w">1</text>' +
      '<circle class="num" cx="160" cy="18" r="10"/><text x="160" y="22" text-anchor="middle" class="w">2</text>' +
      '<circle class="num2" cx="134" cy="10" r="8"/><text x="134" y="14" text-anchor="middle" class="w">3</text>' +
      '<text x="26" y="58">sağ</text><text x="188" y="58">sol</text>');
  };
  I.body = function () {
    return svg(ewer(196, 70, .9) +
      '<path class="si" d="M104 26 C116 26 124 34 124 46 C124 58 116 66 104 66 C92 66 84 58 84 46 C84 34 92 26 104 26 Z"/>' +
      '<path class="si" d="M62 170 C62 128 72 98 86 84 C92 78 98 76 104 76 C110 76 116 78 122 84 C136 98 146 128 146 170"/>' +
      stream('M142 24 C130 22 118 22 110 26', 4) +
      '<path class="wl" d="M86 40 C76 70 72 110 70 160 M122 40 C134 70 138 110 140 160 M98 70 C96 100 96 130 98 160" stroke-width="1.6"/>' +
      drops([[58, 120], [152, 110], [54, 150, 2.2], [156, 146, 2.2], [104, 112, 2]]));
  };
  I.khuff = function () {
    return svg('<path class="shl" d="M50 168 C100 166 170 166 220 168"/>' +
      '<path class="lt" d="M66 4 C80 2 94 2 106 4 C104 50 104 92 110 114 C118 130 140 136 168 142 C192 147 206 152 207 160 C208 166 200 168 186 168 L76 168 C64 168 56 160 58 146 C60 120 64 80 66 4 Z"/>' +
      '<path class="ltd" d="M67 16 C80 14 94 14 106 16 M112 120 C130 132 160 138 190 146 M86 30 C86 70 86 110 90 150"/>' +
      hand(214, 104, 198, .7, true) +
      '<path class="mv" d="M196 128 C172 124 144 114 120 68"/>' + head(120, 68, 240) +
      '<path class="wl" d="M150 132 C160 136 170 138 180 140" stroke-width="1.6"/>');
  };
  I.qibla = function () {
    return svg('<circle cx="120" cy="92" r="62" fill="none" class="ring"/>' +
      '<path class="bad" d="M120 92 L94 34 A62 62 0 0 1 146 34 Z"/><path class="bad" d="M120 92 L146 150 A62 62 0 0 1 94 150 Z"/>' +
      '<path class="ok" d="M120 92 L62 66 A62 62 0 0 0 62 118 Z"/><path class="ok" d="M120 92 L178 118 A62 62 0 0 0 178 66 Z"/>' +
      '<g transform="translate(120 16)"><path class="kaaba" d="M0 -9 L10 -4 V8 L0 13 L-10 8 V-4 Z"/><path class="kiswa" d="M-10 -1 L0 4 L10 -1"/></g>' +
      '<circle class="you" cx="120" cy="92" r="7"/>' +
      '<text x="146" y="20">qiblə</text><text x="24" y="96">yan</text><text x="198" y="96">yan</text>' +
      '<text x="120" y="58" text-anchor="middle" class="x">✕</text><text x="120" y="138" text-anchor="middle" class="x">✕</text>');
  };
  I.ruku = function () {
    return '<svg class="ill" viewBox="0 0 240 200" aria-hidden="true"><rect class="bgc" width="240" height="200" rx="16"/>' +
      '<path class="mat" d="M26 184 C80 182 160 182 214 184 L214 191 C160 189 80 189 26 191 Z"/>' +
      '<path class="sk" d="M80 171 C88 170 96 170 102 171 C110 173 118 176 121 179 C122 182 118 183 112 183 L82 183 C77 183 76 175 80 171 Z"/>' +
      '<path class="cl" d="M76 102 C86 101 98 100 108 100 C110 126 112 150 114 175 C100 177 86 177 72 175 C73 150 75 126 76 102 Z"/>' +
      '<path class="cdt" d="M92 112 C91 132 90 152 90 174 M102 112 C103 132 104 152 106 174 M80 150 C81 158 80 166 79 174"/>' +
      '<path class="cl" d="M78 84 C96 78 138 76 166 79 C181 81 184 103 170 107 C150 110 128 112 110 114 C100 115 90 116 84 116 C70 114 66 92 78 84 Z"/>' +
      '<path class="cdt" d="M90 107 C112 103 142 99 168 97 M120 82 C126 86 134 88 142 88"/>' +
      '<path class="sk" d="M186 82 C196 82 205 90 205 100 C205 110 196 117 186 117 C178 117 172 110 172 100 C172 90 178 82 186 82 Z"/>' +
      '<path class="hr" d="M174 104 C178 112 184 118 192 118 C198 118 202 112 202 106 C196 110 186 110 174 104 Z"/>' +
      '<path class="cl" d="M190 82.5 C200 84 206 92 205.5 100 C205 106 202 111 198 114 C201 104 198 92 190 82.5 Z"/>' +
            '<path class="cl" d="M156 96 C162 93 170 97 170 104 C152 118 136 132 120 145 C116 143 112 140 108 136 C124 122 140 109 156 96 Z"/><path class="cdt" d="M118 140 C114 138 112 136 110 133"/>' +
      '<g transform="translate(112 140) rotate(124) scale(.42)">' + HAND + '</g>' +
      '</svg>';
  };

  return I;
})();
if (typeof module !== 'undefined') module.exports = ILLX;
