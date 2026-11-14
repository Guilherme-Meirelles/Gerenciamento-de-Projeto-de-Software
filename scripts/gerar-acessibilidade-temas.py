#!/usr/bin/env python3
"""Gera src/main/resources/static/css/acessibilidade-temas.css (tema claro, alto contraste e correção
de vh/vw com o texto ampliado) a partir dos demais .css do projeto.

Rode de novo sempre que um .css do projeto mudar:

    python scripts/gerar-acessibilidade-temas.py src/main/resources/static/css src/main/resources/static/css/acessibilidade-temas.css
"""
import re, sys, glob, os, colorsys

CSS_DIR = sys.argv[1]
OUT = sys.argv[2]

# ---------- parser ----------
def parse(css):
    css = re.sub(r'/\*.*?\*/', '', css, flags=re.S)
    i = 0; n = len(css)
    def parse_block(i, top=True):
        nodes = []
        while i < n:
            # skip ws
            while i < n and css[i].isspace(): i += 1
            if i >= n: break
            if css[i] == '}':
                return nodes, i + 1
            # read prelude until { or ;
            start = i; depth = 0; q = None
            while i < n:
                c = css[i]
                if q:
                    if c == '\\': i += 1
                    elif c == q: q = None
                elif c in '"\'': q = c
                elif c == '(': depth += 1
                elif c == ')': depth -= 1
                elif depth == 0 and c in '{;': break
                i += 1
            prelude = css[start:i].strip()
            if i >= n: break
            if css[i] == ';':
                i += 1; continue
            # '{'
            i += 1
            if prelude.startswith('@'):
                name = prelude.split()[0] if prelude.split() else prelude
                if name in ('@media', '@supports'):
                    children, i = parse_block(i, False)
                    nodes.append(('at', prelude, children))
                else:
                    # skip balanced block
                    d = 1
                    q = None
                    while i < n and d:
                        c = css[i]
                        if q:
                            if c == '\\': i += 1
                            elif c == q: q = None
                        elif c in '"\'': q = c
                        elif c == '{': d += 1
                        elif c == '}': d -= 1
                        i += 1
            else:
                # declarations until matching }
                s = i; q = None; depth = 0
                while i < n:
                    c = css[i]
                    if q:
                        if c == '\\': i += 1
                        elif c == q: q = None
                    elif c in '"\'': q = c
                    elif c == '(': depth += 1
                    elif c == ')': depth -= 1
                    elif c == '}' and depth == 0: break
                    i += 1
                body = css[s:i]; i += 1
                nodes.append(('rule', prelude, body))
        return nodes, i
    nodes, _ = parse_block(0)
    return nodes

def split_decls(body):
    out = []; cur = ''; q = None; depth = 0
    for c in body:
        if q:
            cur += c
            if c == q: q = None
            continue
        if c in '"\'': q = c; cur += c; continue
        if c == '(': depth += 1
        if c == ')': depth -= 1
        if c == ';' and depth == 0:
            out.append(cur); cur = ''
        else:
            cur += c
    if cur.strip(): out.append(cur)
    res = []
    for d in out:
        if ':' not in d: continue
        p, v = d.split(':', 1)
        res.append((p.strip().lower(), v.strip()))
    return res

def split_selectors(sel):
    out = []; cur = ''; depth = 0; q = None
    for c in sel:
        if q:
            cur += c
            if c == q: q = None
            continue
        if c in '"\'': q = c; cur += c; continue
        if c in '([': depth += 1
        if c in ')]': depth -= 1
        if c == ',' and depth == 0:
            out.append(cur.strip()); cur = ''
        else: cur += c
    if cur.strip(): out.append(cur.strip())
    return out

def prefix(sel, cls):
    p = 'html.' + cls
    if sel.startswith('html'): return p + sel[4:]
    if sel.startswith(':root'): return p + sel[5:]
    return p + ' ' + sel

# ---------- colors ----------
HEX = re.compile(r'#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})\b')
RGBA = re.compile(r'rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)')

def hex_to_rgb(h):
    h = h.lstrip('#')
    if len(h) in (3, 4): h = ''.join(c*2 for c in h[:3])
    return int(h[0:2],16), int(h[2:4],16), int(h[4:6],16)

def is_grey(r,g,b): return max(r,g,b) - min(r,g,b) <= 6

def escurece_para_claro(r, g, b):
    """Textos coloridos claros (amarelo, azul claro, verde claro...) ficam ilegíveis no fundo claro: escurece mantendo o matiz."""
    luz = (0.299 * r + 0.587 * g + 0.114 * b) / 255
    if luz <= 0.42: return r, g, b
    h, l, sat = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    l = min(l, 0.27)
    sat = min(1.0, sat + 0.1)
    nr, ng, nb = colorsys.hls_to_rgb(h, l, sat)
    return round(nr * 255), round(ng * 255), round(nb * 255)

def fmt(v):
    v = max(0, min(255, int(v)))
    return '#%02x%02x%02x' % (v, v, v)

# value mappers: (grey v, kind) -> new v ; kind in 'text','bg','border'
def light_map(v, kind):
    if kind == 'text':
        out = 255 - v
        if v >= 200: return max(out, 26)      # branco e cinzas claros -> quase preto
        return min(out, 110)                  # cinzas "apagados" ficam num cinza ainda legível
    if kind == 'bg':
        return 255 - round(v * 0.55)          # superfícies claras, sem ficar tudo no mesmo cinza
    return 255 - v                            # bordas

def hc_map(v, kind):
    if kind == 'text':
        return 255 if v >= 100 else v
    if kind == 'bg':
        return 0 if v < 112 else v
    return 255 if 40 <= v <= 200 else v

def kind_of(prop):
    if prop == 'color' or prop in ('caret-color',): return 'text'
    if prop.startswith('background'): return 'bg'
    if prop.startswith('border') or prop.startswith('outline'): return 'border'
    return None

def is_colored(value):
    for m in HEX.finditer(value):
        r,g,b = hex_to_rgb(m.group(0))
        if not is_grey(r,g,b) and max(r,g,b) > 60: return True
    for m in RGBA.finditer(value):
        r,g,b = int(m.group(1)),int(m.group(2)),int(m.group(3))
        a = float(m.group(4)) if m.group(4) else 1
        if not is_grey(r,g,b) and a >= 0.9: return True
    return False

def map_value(prop, value, mapper, theme, rule_has_colored_bg):
    kind = kind_of(prop)
    if prop == 'color-scheme':
        return 'light' if theme == 'claro' else value
    if kind is None: return value
    if kind == 'text' and rule_has_colored_bg: return value
    def hx(m):
        r,g,b = hex_to_rgb(m.group(0))
        if not is_grey(r,g,b):
            if theme == 'claro' and kind == 'text':
                nr, ng, nb = escurece_para_claro(r, g, b)
                return '#%02x%02x%02x' % (nr, ng, nb)
            return m.group(0)
        return fmt(mapper(r, kind))
    def rg(m):
        r,g,b = int(m.group(1)),int(m.group(2)),int(m.group(3))
        a = m.group(4)
        if not is_grey(r,g,b):
            if theme == 'claro' and kind == 'text' and (a is None or float(a) >= 0.9):
                nr, ng, nb = escurece_para_claro(r, g, b)
                return 'rgb(%d, %d, %d)' % (nr, ng, nb)
            return m.group(0)
        if a is not None and float(a) < 1:
            # translúcidos: brancos viram pretos no claro (realces de hover)
            if theme == 'claro' and r > 128: return 'rgba(0, 0, 0, %s)' % a
            return m.group(0)
        return fmt(mapper(r, kind))
    # não mexe dentro de url(...)
    parts = re.split(r'(url\((?:"[^"]*"|\'[^\']*\'|[^)]*)\))', value)
    outp = []
    for p in parts:
        if p.startswith('url('):
            if theme == 'claro' and kind == 'bg':
                p = p.replace("stroke='white'", "stroke='%231a1a1a'").replace("stroke='%23d6d6d6'", "stroke='%23333'")
            outp.append(p)
        else:
            p = HEX.sub(hx, p); p = RGBA.sub(rg, p)
            outp.append(p)
    return ''.join(outp)

def gen_theme(nodes, cls, theme, mapper, out, media=None):
    for nd in nodes:
        if nd[0] == 'at':
            sub = []
            gen_theme(nd[2], cls, theme, mapper, sub)
            if sub:
                out.append(nd[1] + ' {\n' + '\n'.join(sub) + '\n}')
            continue
        _, sel, body = nd
        decls = split_decls(body)
        colored_bg = any(kind_of(p) == 'bg' and is_colored(v) for p, v in decls)
        changed = []
        for p, v in decls:
            nv = map_value(p, v, mapper, theme, colored_bg)
            if nv != v: changed.append('%s: %s !important' % (p, nv))
        if changed:
            sels = ', '.join(prefix(s, cls) for s in split_selectors(sel))
            out.append('%s { %s; }' % (sels, '; '.join(changed)))

# ---------- zoom (vh/vw) ----------
VU = re.compile(r'(-?\d*\.?\d+)(vh|vw|dvh|svh|lvh|vmin|vmax)\b')
def gen_zoom(nodes, out):
    for nd in nodes:
        if nd[0] == 'at':
            sub = []; gen_zoom(nd[2], sub)
            if sub: out.append(nd[1] + ' {\n' + '\n'.join(sub) + '\n}')
            continue
        _, sel, body = nd
        changed = []
        for p, v in split_decls(body):
            if VU.search(v) and not v.startswith('url'):
                nv = VU.sub(lambda m: 'calc(%s%s / var(--az, 1))' % (m.group(1), m.group(2)), v)
                changed.append('%s: %s !important' % (p, nv))
        if changed:
            sels = ', '.join(prefix(s, 'acess-zoom') for s in split_selectors(sel))
            out.append('%s { %s; }' % (sels, '; '.join(changed)))

claro = []; hc = []; zoom = []
for f in sorted(glob.glob(os.path.join(CSS_DIR, '*.css'))):
    base = os.path.basename(f)
    if base.startswith('acessibilidade'): continue
    nodes = parse(open(f, encoding='utf-8').read())
    c1 = []; gen_theme(nodes, 'tema-claro', 'claro', light_map, c1)
    c2 = []; gen_theme(nodes, 'acess-contraste', 'hc', hc_map, c2)
    c3 = []; gen_zoom(nodes, c3)
    claro.append('/* %s */' % base); claro += c1
    hc.append('/* %s */' % base); hc += c2
    zoom.append('/* %s */' % base); zoom += c3

header = '''/* ARQUIVO GERADO a partir dos demais .css (não editar à mão).
 * Contém só ajustes aplicados quando o usuário liga uma opção do menu de acessibilidade:
 *   html.acess-zoom      -> corrige unidades vh/vw quando o texto é ampliado (--az = fator de zoom)
 *   html.tema-claro      -> tema claro (as cores escuras viram claras)
 *   html.acess-contraste -> alto contraste (fundos pretos, textos e bordas brancos)
 * Se algum .css do projeto mudar, o arquivo precisa ser gerado de novo.
 */
'''
texto = (header + '\n/* ===== ZOOM ===== */\n' + '\n'.join(zoom) +
    '\n\n/* ===== TEMA CLARO ===== */\n' + '\n'.join(claro) +
    '\n\n/* ===== ALTO CONTRASTE ===== */\n' + '\n'.join(hc) + '\n')
# valores que já vinham com !important no CSS original não podem ficar com dois (a regra seria ignorada)
texto = texto.replace('!important !important', '!important')
open(OUT, 'w', encoding='utf-8').write(texto)
print('ok', sum(1 for l in claro if l.startswith('html')), sum(1 for l in hc if l.startswith('html')), sum(1 for l in zoom if l.startswith('html')))
